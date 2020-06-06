package com.tommihirvonen.exifnotes.datastructures

import android.location.Location
import com.google.android.gms.maps.model.LatLng

data class Location(val decimalLocation: String) {

    val latLng: LatLng? get() = decimalLocation.let {
        try {
            val latString = decimalLocation.substring(0, decimalLocation.indexOf(" "))
            val lngString = decimalLocation.substring(decimalLocation.indexOf(" ") + 1, decimalLocation.length - 1)
            val lat = latString.replace(",", ".").toDouble()
            val lng = lngString.replace(",", ".").toDouble()
            LatLng(lat, lng)
        } catch (e: Exception) {
            null
        }
    }

    val readableLocation: String get() = decimalLocation.let { location ->
        val stringBuilder = StringBuilder()

        var latString = location.substring(0, location.indexOf(" "))
        var lngString = location.substring(location.indexOf(" ") + 1)

        val latRef: String
        if (latString.substring(0, 1) == "-") {
            latRef = "S"
            latString = latString.substring(1)
        } else latRef = "N"

        val lngRef: String
        if (lngString.substring(0, 1) == "-") {
            lngRef = "W"
            lngString = lngString.substring(1)
        } else lngRef = "E"

        latString = Location.convert(latString.toDouble(), Location.FORMAT_SECONDS)
        val latStringList = latString.split(":".toRegex())

        lngString = Location.convert(lngString.toDouble(), Location.FORMAT_SECONDS)
        val lngStringList = lngString.split(":".toRegex())

        val space = " "

        stringBuilder.append(latStringList[0]).append("°").append(space)
                .append(latStringList[1]).append("'").append(space)
                .append(latStringList[2].replace(',', '.'))
                .append("\"").append(space)
        stringBuilder.append(latRef).append(space)
        stringBuilder.append(lngStringList[0]).append("°").append(space)
                .append(lngStringList[1]).append("'").append(space)
                .append(lngStringList[2].replace(',', '.'))
                .append("\"").append(space)
        stringBuilder.append(lngRef)

        return stringBuilder.toString()
    }

}