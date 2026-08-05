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
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class Format(
    private val selectionOrder: Int,
    private val frameWidthMm: Double? = null,
    private val frameHeightMm: Double? = null
) {
    // Keep the first four entries in their original order. Their ordinals are stored in the database.
    MM35(0, 36.0, 24.0),
    MediumFormat120(2),
    APS110(10, 17.0, 13.0),
    Sheet(11),
    MediumFormat645(4, 56.0, 41.5),
    MediumFormat66(5, 56.0, 56.0),
    MediumFormat67(6, 56.0, 70.0),
    MediumFormat69(7, 56.0, 84.0),
    MediumFormat612(8, 56.0, 112.0),
    MediumFormat617(9, 56.0, 168.0),
    XPan(1, 65.0, 24.0),
    Sheet4x5(12, 96.0, 120.0),
    Sheet8x10(13, 196.0, 246.0),
    MediumFormatGFX(3, 43.8, 32.9);

    companion object {
        private val mm35Diagonal = hypot(36.0, 24.0)

        val selectableEntries = entries.sortedBy(Format::selectionOrder)

        fun from(value: Int) =
            entries.firstOrNull { it.ordinal == value } ?: MM35
    }

    fun focalLengthIn35mmFormat(focalLength: Int): Int? {
        if (focalLength <= 0) return null
        val width = frameWidthMm ?: return null
        val height = frameHeightMm ?: return null
        return (focalLength * mm35Diagonal / hypot(width, height)).roundToInt()
    }

    fun description(context: Context) = when (this) {
        MM35 -> "35 mm"
        MediumFormat120 -> "120"
        APS110 -> "110"
        Sheet -> context.getString(R.string.Sheet)
        MediumFormat645 -> "120 (6 × 4.5)"
        MediumFormat66 -> "120 (6 × 6)"
        MediumFormat67 -> "120 (6 × 7)"
        MediumFormat69 -> "120 (6 × 9)"
        MediumFormat612 -> "120 (6 × 12)"
        MediumFormat617 -> "120 (6 × 17)"
        XPan -> "XPan (24 × 65 mm)"
        Sheet4x5 -> "${context.getString(R.string.Sheet)} (4 × 5 in)"
        Sheet8x10 -> "${context.getString(R.string.Sheet)} (8 × 10 in)"
        MediumFormatGFX -> "GFX (43.8 × 32.9 mm)"
    }
}
