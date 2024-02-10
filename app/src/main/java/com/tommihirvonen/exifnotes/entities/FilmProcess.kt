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

package com.tommihirvonen.exifnotes.entities

import android.content.Context
import com.tommihirvonen.exifnotes.R

enum class FilmProcess {
    UNKNOWN,
    BW_NEGATIVE,
    BW_REVERSAL,
    C41,
    E6,
    ECN2,
    INTEGRAL;

    fun description(context: Context) =
        context.resources.getStringArray(R.array.FilmProcesses).getOrNull(ordinal)

    companion object {
        fun from(value: Int) = entries.firstOrNull { it.ordinal == value } ?: UNKNOWN
    }
}