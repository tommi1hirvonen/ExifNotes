/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.core.entities

import android.content.Context
import com.tommihirvonen.exifnotes.core.R

enum class FrameSortMode(value: Int) {
    FrameCount(0),
    Date(1),
    FStop(2),
    ShutterSpeed(3),
    Lens(4);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    fun getComparator(context: Context): Comparator<Frame> = when (this) {
        FrameCount -> compareByDescending { it.count }
        Date -> compareByDescending { it.date }
        Lens -> compareBy { it.lens?.name }
        FStop -> {
            val allApertureValues = context.resources.getStringArray(R.array.AllApertureValues)
            compareBy { allApertureValues.indexOf(it.aperture) }
        }
        ShutterSpeed -> {
            val allShutterValues = context.resources.getStringArray(R.array.AllShutterValues)
            compareByDescending { allShutterValues.indexOf(it.shutter) }
        }
    }

    companion object {

        fun fromValue(value: Int): FrameSortMode {
            return when (value) {
                0 -> FrameCount
                1 -> Date
                2 -> FStop
                3 -> ShutterSpeed
                4 -> Lens
                else -> FrameCount
            }
        }
    }
}
