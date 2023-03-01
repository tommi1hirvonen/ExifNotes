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

package com.tommihirvonen.exifnotes.datastructures

import android.location.Location as AndroidLocation
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Parcelize
@Serializable(with = Location.Serializer::class)
class Location(val decimalLocation: String) : Parcelable {

    object Serializer : KSerializer<Location> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("location") {
                element<Double>("latitude")
                element<Double>("longitude")
            }
        override fun deserialize(decoder: Decoder): Location =
            decoder.decodeStructure(descriptor) {
                var latitude = 0.0
                var longitude = 0.0
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> latitude = decodeDoubleElement(descriptor, 0)
                        1 -> longitude = decodeDoubleElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                val latLng = LatLng(latitude, longitude)
                Location(latLng)
            }
        override fun serialize(encoder: Encoder, value: Location) {
            val latLng = value.latLng
            encoder.encodeStructure(descriptor) {
                encodeDoubleElement(descriptor, 0, latLng?.latitude ?: 0.0)
                encodeDoubleElement(descriptor, 1, latLng?.longitude ?: 0.0)
            }
        }
    }

    companion object {
        private fun AndroidLocation.customString(): String =
            (AndroidLocation.convert(latitude, AndroidLocation.FORMAT_DEGREES) + " "
                    + AndroidLocation.convert(longitude, AndroidLocation.FORMAT_DEGREES))
                .replace(",", ".")

        private fun LatLng.customString(): String = "$latitude $longitude"
    }

    constructor(latLng: LatLng) : this(latLng.customString())

    constructor(location: AndroidLocation) : this(location.customString())

    override fun toString(): String = decimalLocation

    val latLng: LatLng? get() = decimalLocation.let {
        try {
            val (latString, lngString) = decimalLocation.split(" ")
            val lat = latString.replace(",", ".").toDouble()
            val lng = lngString.replace(",", ".").toDouble()
            LatLng(lat, lng)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    val readableLocation: String? get() = components?.let { components ->
        try {
            val stringBuilder = StringBuilder()
            val space = " "
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
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val exifToolLocation: String? get() = components?.let { components ->
        try {
            val stringBuilder = StringBuilder()
            val quote = "\""
            val space = " "
            val gpsLatTag = "-GPSLatitude="
            val gpsLatRefTag = "-GPSLatitudeRef="
            val gpsLngTag = "-GPSLongitude="
            val gpsLngRefTag = "-GPSLongitudeRef="

            stringBuilder.append(gpsLatTag).append(quote).append(components.latitudeDegrees)
                    .append(space).append(components.latitudeMinutes).append(space)
                    .append(components.latitudeSeconds).append(quote).append(space)
            stringBuilder.append(gpsLatRefTag).append(quote).append(components.latitudeRef).append(quote).append(space)

            stringBuilder.append(gpsLngTag).append(quote).append(components.longitudeDegrees)
                    .append(space).append(components.longitudeMinutes).append(space)
                    .append(components.longitudeSeconds).append(quote).append(space)
            stringBuilder.append(gpsLngRefTag).append(quote).append(components.longitudeRef).append(quote).append(space)
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val components: Components? get() = decimalLocation.let { location ->
        try {
            var (latString, lngString) = location.split(" ")
            val latRef = if (latString.firstOrNull() == '-') {
                latString = latString.removePrefix("-")
                "S"
            } else "N"
            val lngRef = if (lngString.firstOrNull() == '-') {
                lngString = lngString.removePrefix("-")
                "W"
            } else "E"
            val latStringDegrees = AndroidLocation.convert(latString.toDouble(), AndroidLocation.FORMAT_SECONDS)
            val lngStringDegrees = AndroidLocation.convert(lngString.toDouble(), AndroidLocation.FORMAT_SECONDS)
            val latList = latStringDegrees.split(":")
            val lngList = lngStringDegrees.split(":")
            return Components(
                    latRef, latList[0], latList[1], latList[2],
                    lngRef, lngList[0], lngList[1], lngList[2]
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private class Components(
            val latitudeRef: String,
            val latitudeDegrees: String,
            val latitudeMinutes: String,
            val latitudeSeconds: String,
            val longitudeRef: String,
            val longitudeDegrees: String,
            val longitudeMinutes: String,
            val longitudeSeconds: String
    )

}