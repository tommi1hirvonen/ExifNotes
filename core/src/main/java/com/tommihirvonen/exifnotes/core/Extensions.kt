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

package com.tommihirvonen.exifnotes.core

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

val LatLng.decimalString: String get() = "$latitude $longitude"

fun latLngOrNull(value: String): LatLng? =
    try {
        val (latString, lngString) = value.split(" ")
        val lat = latString.replace(",", ".").toDouble()
        val lng = lngString.replace(",", ".").toDouble()
        LatLng(lat, lng)
    } catch (e: Exception) {
        Log.e("EXIF_NOTES", e.toString())
        null
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
        Log.e("EXIF_NOTES", e.toString())
        null
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
            Log.e("EXIF_NOTES", e.toString())
            LocalDateTime.parse(value, dateTimeFormatter2)
        } catch (e: DateTimeParseException) {
            Log.e("EXIF_NOTES", e.toString())
            null
        }
    }

val LocalDateTime.sortableDateTime: String get() = format(dateTimeFormatter1)

fun String.toShutterSpeedOrNull(): String? {
    return when {
        regexInteger.matches(this) && this.endsWith('"') -> this
        regexInteger.matches(this) -> "$this\""
        regexDecimal.matches(this) && this.endsWith('"') -> this
        regexDecimal.matches(this) -> "$this\""
        regexFraction.matches(this) -> this
        this == "B" -> this
        else -> null
    }
}

private val regexInteger = "[1-9]+[0-9]*\"?".toRegex()
private val regexDecimal = "(?:[1-9]+[0-9]*|[0-9])\\.[0-9]*[1-9]\"?".toRegex()
private val regexFraction = "1/[1-9]+[0-9]*".toRegex()
