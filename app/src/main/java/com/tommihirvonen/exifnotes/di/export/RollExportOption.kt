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

package com.tommihirvonen.exifnotes.di.export

enum class RollExportOption {
    CSV, EXIFTOOL, JSON;

    override fun toString(): String = when (this) {
        CSV -> "csv"
        EXIFTOOL -> "ExifTool"
        JSON -> "JSON"
    }

    fun toRollExportOptionData(): RollExportOptionData? = when (this) {
        CSV -> RollExportOptionData.CSV
        JSON -> RollExportOptionData.JSON
        else -> null
    }
}

sealed class RollExportOptionData {
    data object CSV : RollExportOptionData()
    data object JSON : RollExportOptionData()
    class ExifTool(val lineSeparatorOs: LineSeparatorOs) : RollExportOptionData()
}

enum class LineSeparatorOs {
    WINDOWS, UNIX;

    override fun toString(): String = when (this) {
        WINDOWS -> "Windows (CRLF)"
        UNIX -> "macOS/Linux (Unix) (LF)"
    }

    val lineSeparator: String get() = when (this) {
        WINDOWS -> "\r\n"
        UNIX -> "\n"
    }
}