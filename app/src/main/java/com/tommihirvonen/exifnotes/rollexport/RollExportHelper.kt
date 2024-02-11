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

package com.tommihirvonen.exifnotes.rollexport

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.tommihirvonen.exifnotes.core.entities.Roll
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RollExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val builder: RollExportBuilder) {

    fun export(roll: Roll, options: List<RollExportOption>, targetDirectory: DocumentFile) {
        val exports = builder.create(roll, options)
        exports.forEach { export ->
            val (option, filename, content) = export
            val file = when (option) {
                RollExportOption.CSV -> targetDirectory.createFile("text/plain", filename)
                RollExportOption.EXIFTOOL -> targetDirectory.createFile("text/plain", filename)
                RollExportOption.JSON -> targetDirectory.createFile("application/json", filename)
            } ?: return@forEach
            val stream = context.contentResolver.openOutputStream(file.uri) ?: return@forEach
            val writer = OutputStreamWriter(stream)
            writer.write(content)
            writer.flush()
            writer.close()
            stream.close()
        }
    }
}