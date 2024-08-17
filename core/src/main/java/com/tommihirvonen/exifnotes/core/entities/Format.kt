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

enum class Format {
    MM35, MediumFormat120, APS110, Sheet;

    companion object {
        fun from(value: Int) =
            entries.firstOrNull { it.ordinal == value } ?: MM35
    }

    fun description(context: Context) = when (this) {
        MM35 -> "35 mm"
        MediumFormat120 -> "120"
        APS110 -> "110"
        Sheet -> context.getString(R.string.Sheet)
    }
}