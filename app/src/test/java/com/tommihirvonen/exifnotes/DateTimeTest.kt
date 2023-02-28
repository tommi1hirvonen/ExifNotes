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

import com.tommihirvonen.exifnotes.datastructures.DateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class DateTimeTest {
    @Test
    fun dateTime_serialize() {
        val dateTime = DateTime(2023, 2, 28, 18, 53)
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(dateTime)
        println(json)
        val dateTime2 = Json.decodeFromString(DateTime.serializer(), json)
        println(dateTime2)
    }
}