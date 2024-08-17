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

import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Format
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.PartialIncrement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class CameraTest {
    @Test
    fun camera_serialize() {
        val camera = Camera(
            id = 123,
            make = "Canon",
            model = "A-1",
            serialNumber = "ABC123",
            minShutter = "1/1000",
            maxShutter = "30",
            shutterIncrements = Increment.Half,
            exposureCompIncrements = PartialIncrement.Third,
            format = Format.MM35
        )
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(camera)
        println(json)
    }

    @Test
    fun fixedLens_serialize() {
        val camera = Camera(
            id = 123,
            make = "Contax",
            model = "T2",
            serialNumber = "ABC123",
            minShutter = "1/500",
            maxShutter = "8",
            shutterIncrements = Increment.Half,
            exposureCompIncrements = PartialIncrement.Third,
            format = Format.MM35,
            lens = Lens(
                id = 124,
                minAperture = "2.8",
                maxAperture = "22",
                minFocalLength = 38,
                maxFocalLength = 38,
                apertureIncrements = Increment.Half
            ),
        )
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(camera)
        println(json)
    }
}