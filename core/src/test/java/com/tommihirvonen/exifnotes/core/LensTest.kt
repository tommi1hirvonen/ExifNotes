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

import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.Lens
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class LensTest {
    @Test
    fun lens_serialize() {
        val lens = Lens(
            id = 123,
            make = "Canon",
            model = "FD 28mm f/2.8",
            minAperture = "2.8",
            maxAperture = "2.8",
            minFocalLength = 28,
            maxFocalLength = 28,
            serialNumber = "ABC123",
            apertureIncrements = Increment.HALF
        )
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(lens)
        println(json)
    }
}