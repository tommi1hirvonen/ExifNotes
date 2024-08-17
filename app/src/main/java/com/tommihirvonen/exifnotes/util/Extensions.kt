/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Coordinates
import com.tommihirvonen.exifnotes.core.entities.FilmProcess
import com.tommihirvonen.exifnotes.core.entities.FilmType
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.LightSource
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

fun <T> T.validate(vararg validations: (T) -> (Boolean)): Boolean =
    validations.map { it(this) }.all { it }

fun <T> List<T>.isEmptyOrContains(value: T): Boolean = contains(value) || isEmpty()

fun <T> List<T>.applyPredicates(vararg predicates: ((T) -> (Boolean))): List<T> =
    filter { item -> predicates.all { p -> p(item) } }

fun <T, U : Comparable<U>> List<T>.mapDistinct(transform: (T) -> U): List<U> =
    map(transform).distinct().sorted()

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

fun File.makeDirsIfNotExists() { if (!isDirectory) mkdirs() }

/**
 * Remove all files in a directory. Subdirectories are skipped.
 */
fun File.purgeDirectory() = this.listFiles()?.filterNot(File::isDirectory)?.forEach { it.delete() }

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

@Composable
fun String.linkify(
    linkStyle: SpanStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline
    )
) = buildAnnotatedString {
    append(this@linkify)

    val spannable = SpannableString(this@linkify)
    Linkify.addLinks(spannable, Linkify.WEB_URLS)

    val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
    for (span in spans) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        addLink(
            url = LinkAnnotation.Url(
                url = span.url,
                styles = TextLinkStyles(linkStyle)
            ),
            start = start,
            end = end
        )
    }
}

@JvmName("mapNonUniqueCamerasToNameWithSerial")
fun Iterable<Camera>.mapNonUniqueToNameWithSerial() = map { camera ->
    val nameIsNotDistinct = any { it.id != camera.id && it.name == camera.name }
    val name = if (nameIsNotDistinct && !camera.serialNumber.isNullOrEmpty()) {
        "${camera.name} (${camera.serialNumber})"
    } else {
        camera.name
    }
    camera to name
}

@JvmName("mapNonUniqueLensesToNameWithSerial")
fun Iterable<Lens>.mapNonUniqueToNameWithSerial() = map { lens ->
    val nameIsNotDistinct = any { it.id != lens.id && it.name == lens.name }
    val name = if (nameIsNotDistinct && !lens.serialNumber.isNullOrEmpty()) {
        "${lens.name} (${lens.serialNumber})"
    } else {
        lens.name
    }
    lens to name
}

@Composable
@ReadOnlyComposable
fun textResource(@StringRes id: Int): CharSequence = LocalContext.current.resources.getText(id)

val FilmType.description: String @Composable get() = description(LocalContext.current)

val FilmProcess.description: String @Composable get() = description(LocalContext.current)

val LightSource.description: String @Composable get() = description(LocalContext.current)

@Composable
fun PaddingValues.copy(
    start: Dp? = null,
    top: Dp? = null,
    end: Dp? = null,
    bottom: Dp? = null
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = start ?: this.calculateStartPadding(layoutDirection),
        top = top ?: this.calculateTopPadding(),
        end = end ?: this.calculateEndPadding(layoutDirection),
        bottom = bottom ?: this.calculateBottomPadding(),
    )
}

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