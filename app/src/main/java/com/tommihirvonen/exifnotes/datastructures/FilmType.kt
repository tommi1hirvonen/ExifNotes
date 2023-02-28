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

package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import com.tommihirvonen.exifnotes.R

enum class FilmType {
    UNKNOWN,
    BW_NEGATIVE,
    BW_REVERSAL,
    BW_INSTANT,
    COLOR_NEGATIVE,
    COLOR_REVERSAL,
    COLOR_INSTANT,
    MOTION_PIC_BW_NEGATIVE,
    MOTION_PIC_COLOR_NEGATIVE;

    fun description(context: Context) =
        context.resources.getStringArray(R.array.FilmTypes).getOrNull(ordinal)

    companion object {
        fun from(value: Int) = values().firstOrNull { it.ordinal == value } ?: BW_NEGATIVE
    }
}