/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes

import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.datastructures.Location
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class LocationTest {
    @Test
    fun location_serialize() {
        val latLng = LatLng(35.123, 45.123)
        val location = Location(latLng)
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(location)
        println(json)
        val location2 = Json.decodeFromString(Location.serializer(), json)
        println(location2)
    }
}