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
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import java.io.OutputStreamWriter

class RollExportHelper(
    private val context: Context,
    private val roll: Roll,
    private val targetDirectory: DocumentFile) {

    fun export() {
        val rollName = roll.name?.illegalCharsRemoved()
        //Get the user setting about which files to export. By default, share both files.
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val filesToExport = prefs.getString(
            PreferenceConstants.KEY_FILES_TO_EXPORT,
            PreferenceConstants.VALUE_BOTH)
        if (filesToExport == PreferenceConstants.VALUE_BOTH
            || filesToExport == PreferenceConstants.VALUE_CSV) {
            val csvDocumentFile = targetDirectory.createFile("text/plain",
                rollName + "_csv.txt") ?: return
            val csvOutputStream = context.contentResolver
                .openOutputStream(csvDocumentFile.uri) ?: return
            val csvString = CsvBuilder(context, roll).create()
            val csvOutputStreamWriter = OutputStreamWriter(csvOutputStream)
            csvOutputStreamWriter.write(csvString)
            csvOutputStreamWriter.flush()
            csvOutputStreamWriter.close()
            csvOutputStream.close()
        }
        if (filesToExport == PreferenceConstants.VALUE_BOTH
            || filesToExport == PreferenceConstants.VALUE_EXIFTOOL) {
            val cmdDocumentFile = targetDirectory.createFile("text/plain",
                rollName + "_ExifToolCmds.txt") ?: return
            val cmdOutputStream = context.contentResolver
                .openOutputStream(cmdDocumentFile.uri) ?: return
            val cmdString = ExifToolCommandsBuilder(context, roll).create()
            val cmdOutputStreamWriter = OutputStreamWriter(cmdOutputStream)
            cmdOutputStreamWriter.write(cmdString)
            cmdOutputStreamWriter.flush()
            cmdOutputStreamWriter.close()
            cmdOutputStream.close()
        }
    }
}