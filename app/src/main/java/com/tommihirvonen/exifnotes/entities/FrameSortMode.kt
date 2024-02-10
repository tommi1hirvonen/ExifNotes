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

package com.tommihirvonen.exifnotes.entities

import android.content.Context
import com.tommihirvonen.exifnotes.R

enum class FrameSortMode constructor(value: Int) {
    FRAME_COUNT(0),
    DATE(1),
    F_STOP(2),
    SHUTTER_SPEED(3),
    LENS(4);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    fun getComparator(context: Context): Comparator<Frame> = when (this) {
        FRAME_COUNT -> compareByDescending { it.count }
        DATE -> compareByDescending { it.date }
        LENS -> compareBy { it.lens?.name }
        F_STOP -> {
            val allApertureValues = context.resources.getStringArray(R.array.AllApertureValues)
            compareBy { allApertureValues.indexOf(it.aperture) }
        }
        SHUTTER_SPEED -> {
            val allShutterValues = context.resources.getStringArray(R.array.AllShutterValues)
            compareByDescending { allShutterValues.indexOf(it.shutter) }
        }
    }

    companion object {

        fun fromValue(value: Int): FrameSortMode {
            return when (value) {
                0 -> FRAME_COUNT
                1 -> DATE
                2 -> F_STOP
                3 -> SHUTTER_SPEED
                4 -> LENS
                else -> FRAME_COUNT
            }
        }
    }
}
