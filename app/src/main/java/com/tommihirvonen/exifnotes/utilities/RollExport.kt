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

import android.content.Context
import com.tommihirvonen.exifnotes.data.database
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.datastructures.RollExportOption

abstract class RollExport(private val context: Context,
                          private val roll: Roll
) {
    private val rollName = roll.name?.illegalCharsRemoved()

    private val frames = lazy { context.database.getFrames(roll) }

    protected val fileNameMapping = { option: RollExportOption ->
        when (option) {
            RollExportOption.CSV -> "${rollName}_csv.txt"
            RollExportOption.EXIFTOOL -> "${rollName}_ExifToolCmds.txt"
            RollExportOption.JSON -> "$rollName.json"
        }
    }

    protected val contentMapping = { option: RollExportOption ->
        when (option) {
            RollExportOption.CSV -> CsvBuilder(context, roll, frames.value).create()
            RollExportOption.EXIFTOOL -> ExifToolCommandsBuilder(context, roll, frames.value).create()
            RollExportOption.JSON -> JsonBuilder(roll, frames.value).create()
        }
    }
}