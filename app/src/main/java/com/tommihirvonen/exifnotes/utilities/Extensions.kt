/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.InsetDrawable
import android.location.Location
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.view.animation.Interpolator
import androidx.annotation.StringRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionSet
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.entities.Coordinates
import com.tommihirvonen.exifnotes.entities.Gear
import java.io.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.absoluteValue

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    // TODO: The new API for getting parcelable extra from an Intent introduced in API 33
    //  (getParcelableExtra(String name, Class<T> clazz)) is buggy (bug report link below).
    //  The workaround is to continue using the old method for API 33 and below.
    //  Change to using possible compat APIs if they are introduced as a part of AndroidX.
    // https://issuetracker.google.com/issues/240585930
    SDK_INT >= 34 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

fun <T> T.validate(vararg validations: (T) -> (Boolean)): Boolean =
    validations.map { it(this) }.all { it }

val Context.packageInfo: PackageInfo? get() {
    try {
        return if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName,
                PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

// New style formatting
private val dateTimeFormatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
// Legacy style formatting for compatibility with older app versions
private val dateTimeFormatter2 = DateTimeFormatter.ofPattern("yyyy-M-d H:mm")

fun localDateTimeOrNull(value: String): LocalDateTime? =
    try {
        LocalDateTime.parse(value, dateTimeFormatter1)
    } catch (e: DateTimeParseException) {
        try {
            LocalDateTime.parse(value, dateTimeFormatter2)
        } catch (e: DateTimeParseException) {
            null
        }
    }

/**
 * @param value Epoch milliseconds
 */
fun localDateTimeOrNull(value: Long): LocalDateTime? =
    try {
        val zone = ZoneId.of(ZoneOffset.UTC.id)
        val instant = Instant.ofEpochMilli(value)
        val zdt = instant.atZone(zone)
        zdt.toLocalDateTime()
    } catch (e: Exception) {
        null
    }

val LocalDateTime.sortableDateTime: String get() = format(dateTimeFormatter1)

val LocalDateTime.sortableDate: String get() =
    format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

val LocalDateTime.sortableTime: String get() =
    format(DateTimeFormatter.ofPattern("HH:mm"))

val LocalDateTime.epochMilliseconds: Long get() {
    val zone = ZoneId.of(ZoneOffset.UTC.id)
    val zdt = ZonedDateTime.of(this, zone)
    val instant = zdt.toInstant()
    return instant.toEpochMilli()
}

fun latLngOrNull(value: String): LatLng? =
    try {
        val (latString, lngString) = value.split(" ")
        val lat = latString.replace(",", ".").toDouble()
        val lng = lngString.replace(",", ".").toDouble()
        LatLng(lat, lng)
    } catch (e: Exception) {
        null
    }

val LatLng.decimalString: String get() = "$latitude $longitude"

val LatLng.coordinates: Coordinates
    get() {
    val latRef = if (latitude < 0) "S" else "N"
    val lngRef = if (longitude < 0) "W" else "E"
    val latComponents = Location.convert(latitude.absoluteValue, Location.FORMAT_SECONDS)
    val lngComponents = Location.convert(longitude.absoluteValue, Location.FORMAT_SECONDS)
    val (latDegrees, latMinutes, latSeconds) = latComponents.split(":")
    val (lngDegrees, lngMinutes, lngSeconds) = lngComponents.split(":")
    return Coordinates(
        latRef, latDegrees, latMinutes, latSeconds,
        lngRef, lngDegrees, lngMinutes, lngSeconds
    )
}

val LatLng.readableCoordinates: String get() {
    val stringBuilder = StringBuilder()
    val space = " "
    val components = coordinates
    stringBuilder.append(components.latitudeDegrees).append("°").append(space)
        .append(components.latitudeMinutes).append("'").append(space)
        .append(components.latitudeSeconds.replace(',', '.'))
        .append("\"").append(space)
    stringBuilder.append(components.latitudeRef).append(space)

    stringBuilder.append(components.longitudeDegrees).append("°").append(space)
        .append(components.longitudeMinutes).append("'").append(space)
        .append(components.longitudeSeconds.replace(',', '.'))
        .append("\"").append(space)
    stringBuilder.append(components.longitudeRef)
    return stringBuilder.toString()
}

val LatLng.exifToolLocation: String get() {
    val stringBuilder = StringBuilder()
    val quote = "\""
    val space = " "
    val gpsLatTag = "-GPSLatitude="
    val gpsLatRefTag = "-GPSLatitudeRef="
    val gpsLngTag = "-GPSLongitude="
    val gpsLngRefTag = "-GPSLongitudeRef="
    val components = coordinates
    stringBuilder.append(gpsLatTag).append(quote).append(components.latitudeDegrees)
        .append(space).append(components.latitudeMinutes).append(space)
        .append(components.latitudeSeconds).append(quote).append(space)
    stringBuilder.append(gpsLatRefTag).append(quote).append(components.latitudeRef).append(quote).append(space)

    stringBuilder.append(gpsLngTag).append(quote).append(components.longitudeDegrees)
        .append(space).append(components.longitudeMinutes).append(space)
        .append(components.longitudeSeconds).append(quote).append(space)
    stringBuilder.append(gpsLngRefTag).append(quote).append(components.longitudeRef).append(quote).append(space)
    return stringBuilder.toString()
}

/**
 * Removes potential illegal characters from a string to make it a valid file name.
 */
fun String.illegalCharsRemoved(): String = replace("[|\\\\?*<\":>/]".toRegex(), "_")

/**
 * Remove all files in a directory. Subdirectories are skipped.
 */
fun File.purgeDirectory() = this.listFiles()?.filterNot(File::isDirectory)?.forEach { it.delete() }

fun File.makeDirsIfNotExists() { if (!isDirectory) mkdirs() }

fun List<Gear>.toStringList(): String = if (this.isEmpty()) {
    ""
} else {
    this.joinToString(separator = "\n-", prefix = "\n-", transform = Gear::name)
}

fun TransitionSet.setCommonInterpolator(interpolator: Interpolator): TransitionSet = apply {
    (0 until transitionCount).map(::getTransitionAt).forEach { transition ->
        transition?.interpolator = interpolator
    }
}

fun View.snackbar(@StringRes resId: Int, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, resId, duration).show()

fun View.snackbar(text: CharSequence, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, text, duration).show()

fun View.snackbar(@StringRes resId: Int, anchorView: View, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, resId, duration).setAnchorView(anchorView).show()

fun View.snackbar(text: CharSequence, anchorView: View, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, text, duration).setAnchorView(anchorView).show()

fun <T> List<T>.isEmptyOrContains(value: T): Boolean = contains(value) || isEmpty()

fun <T> List<T>.applyPredicates(vararg predicates: ((T) -> (Boolean))): List<T> =
    filter { item -> predicates.all { p -> p(item) } }

fun <T, U : Comparable<U>> List<T>.mapDistinct(transform: (T) -> U): List<U> =
    map(transform).distinct().sorted()

@SuppressLint("RestrictedApi")
fun PopupMenu.setIconsVisible(context: Context) {
    val iconMargin = 0f
    if (menu is MenuBuilder) {
        val menuBuilder = menu as MenuBuilder
        menuBuilder.setOptionalIconsVisible(true)
        for (item in menuBuilder.visibleItems) {
            val iconMarginPx =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, iconMargin, context.resources.displayMetrics)
                    .toInt()
            if (item.icon != null) {
                if (SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx,0)
                } else {
                    item.icon =
                        object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                            override fun getIntrinsicWidth(): Int {
                                return intrinsicHeight + iconMarginPx + iconMarginPx
                            }
                        }
                }
            }
        }
    }
}

fun <T : Parcelable?> Fragment.setNavigationResult(result: T, key: String) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
}

fun <T : Parcelable?> Fragment.observeThenClearNavigationResult(key: String, onResult: (T) -> Any?) {
    val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
    val data = savedStateHandle?.getLiveData<T>(key)
    data?.observe(viewLifecycleOwner) { result ->
        onResult(result)
        savedStateHandle.remove<T>(key)
    }
}

/**
 * Helper method for observing Navigation results from dialog destinations.
 * When observing results from a DialogFragment, the correct target needs to
 * be retrieved using getBackStackEntry() and the observer needs to be attached to that entry.
 * https://developer.android.com/guide/navigation/navigation-programmatic#additional_considerations
 */
fun <T : Any?> NavBackStackEntry.observeThenClearNavigationResult(lifecycleOwner: LifecycleOwner, key: String, onResult: (T?) -> Any?) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME && savedStateHandle.contains(key)) {
            val result = savedStateHandle.get<T>(key)
            onResult(result)
            savedStateHandle.remove<T>(key)
        }
    }
    lifecycle.addObserver(observer)
    // As addObserver() does not automatically remove the observer, we
    // call removeObserver() manually when the lifecycle is destroyed
    lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            lifecycle.removeObserver(observer)
        }
    })
}