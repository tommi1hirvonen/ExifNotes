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

package com.tommihirvonen.exifnotes.utilities

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.datastructures.RollExportOption
import java.io.OutputStreamWriter

class RollExportHelper(
    private val context: Context,
    roll: Roll,
    private val targetDirectory: DocumentFile,
    private val options: List<RollExportOption>) : RollExport(context, roll) {

    private val fileMapping = { option: RollExportOption ->
        when (option) {
            RollExportOption.CSV ->
                targetDirectory.createFile("text/plain", fileNameMapping(option))
            RollExportOption.EXIFTOOL ->
                targetDirectory.createFile("text/plain", fileNameMapping(option))
            RollExportOption.JSON ->
                targetDirectory.createFile("application/json", fileNameMapping(option))
        }
    }

    fun export() {
        options.forEach { option ->
            val file = fileMapping(option) ?: return@forEach
            val stream = context.contentResolver.openOutputStream(file.uri) ?: return@forEach
            val content = contentMapping(option)
            val writer = OutputStreamWriter(stream)
            writer.write(content)
            writer.flush()
            writer.close()
            stream.close()
        }
    }
}