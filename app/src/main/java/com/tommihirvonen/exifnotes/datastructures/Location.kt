package com.tommihirvonen.exifnotes.datastructures

import android.location.Location
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Location(val decimalLocation: String) : Parcelable {

    companion object {
        private fun Location.customString(): String = this.let {
            return (Location.convert(it.latitude, Location.FORMAT_DEGREES) + " " +
                    Location.convert(it.longitude, Location.FORMAT_DEGREES)).replace(",", ".")
        }
        private fun LatLng.customString(): String = this.let {
            return "${it.latitude} ${it.longitude}"
        }
    }

    constructor(latLng: LatLng) : this(latLng.customString())

    constructor(location: Location) : this(location.customString())

    override fun toString(): String {
        return decimalLocation
    }

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
            val latStringDegrees = Location.convert(latString.toDouble(), Location.FORMAT_SECONDS)
            val lngStringDegrees = Location.convert(lngString.toDouble(), Location.FORMAT_SECONDS)
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

    private data class Components(
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