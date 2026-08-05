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

package com.tommihirvonen.exifnotes.core

import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.AttachmentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class FilterTest {
    @Test
    fun accessory_modifiers_serialize() {
        val accessory = Filter(
            make = "Metabones",
            model = "Speed Booster",
            type = AttachmentType.FocalReducer,
            factor = 0.71
        )

        val json = Json.encodeToString(accessory)

        assertTrue(json.contains("\"type\":\"FocalReducer\""))
        assertTrue(json.contains("\"factor\":0.71"))
        assertEquals(AttachmentType.Filter, AttachmentType.from(99))
    }
    @Test
    fun filter_serialize() {
        val filter = Filter(
            id = 123,
            make = "Hoya",
            model = "C-PRO POL"
        )
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(filter)
        println(json)
    }
}
