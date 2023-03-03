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

package com.tommihirvonen.exifnotes.utilities

import com.tommihirvonen.exifnotes.datastructures.Frame
import com.tommihirvonen.exifnotes.datastructures.Roll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonBuilder(private val roll: Roll, private val frames: List<Frame>) {
    fun create(): String {
        val rollCopy = roll.copy(frames = frames)
        val format = Json { prettyPrint = true }
        return format.encodeToString(rollCopy)
    }
}