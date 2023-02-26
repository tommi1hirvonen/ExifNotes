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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tommihirvonen.exifnotes.datastructures.Roll
import java.io.File

/**
 * Creates an Intent to share exiftool commands and a csv
 * for the frames of the roll in question.
 */
class RollShareIntentBuilder(
    private val context: Context,
    private val roll: Roll,
    private val exportCsv: Boolean,
    private val exportExifToolCommands: Boolean) {

    fun create(): Intent? {
        if (!exportCsv && !exportExifToolCommands) {
            return null
        }

        //Replace illegal characters from the roll name to make it a valid file name.
        val rollName = roll.name?.illegalCharsRemoved()

        //Create the Intent to be shared, no initialization yet
        val shareIntent: Intent

        //Create the files

        //Get the external storage path (not the same as SD card)
        val externalStorageDir = context.getExternalFilesDir(null)

        //Create the file names for the two files to be put in that intent
        val fileNameCsv = rollName + "_csv" + ".txt"
        val fileNameExifToolCmds = rollName + "_ExifToolCmds" + ".txt"

        //Create the strings to be written on those two files
        val csvString = CsvBuilder(context, roll).create()
        val exifToolCmds = ExifToolCommandsBuilder(context, roll).create()

        //Create the files in external storage
        val fileCsv = File(externalStorageDir, fileNameCsv)
        val fileExifToolCmds = File(externalStorageDir, fileNameExifToolCmds)

        try {
            //Write the csv file
            fileCsv.writeText(csvString)

            //Write the ExifTool commands file
            fileExifToolCmds.writeText(exifToolCmds)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating text files", Toast.LENGTH_SHORT).show()
            return null
        }

        //If the user has chosen to export both files
        if (exportCsv && exportExifToolCommands) {
            //Create the intent to be shared
            shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            shareIntent.type = "text/plain"

            //Create an array with the file names
            val filesToSend: MutableList<String> = ArrayList()
            filesToSend.add(externalStorageDir.toString() + "/" + fileNameCsv)
            filesToSend.add(externalStorageDir.toString() + "/" + fileNameExifToolCmds)

            //Create an ArrayList of files.
            //NOTE: putParcelableArrayListExtra requires an ArrayList as its argument
            val files = ArrayList<Uri>()
            for (path in filesToSend) {
                val file = File(path)
                //Android Nougat requires that the file is given via FileProvider
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, context.applicationContext
                        .packageName + ".provider", file)
                } else {
                    Uri.fromFile(file)
                }
                files.add(uri)
            }

            //Add the two files to the Intent as extras
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        } else {
            shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            //The user has chosen to export only the csv
            if (exportCsv) {
                //Android Nougat requires that the file is given via FileProvider
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, context.applicationContext
                        .packageName + ".provider", fileCsv)
                } else {
                    Uri.fromFile(fileCsv)
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            } else { // Only ExifTool commands were selected
                //Android Nougat requires that the file is given via FileProvider
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, context.applicationContext
                        .packageName + ".provider", fileExifToolCmds)
                } else {
                    Uri.fromFile(fileExifToolCmds)
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        return shareIntent
    }
}