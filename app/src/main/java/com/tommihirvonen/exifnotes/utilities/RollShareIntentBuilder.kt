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
import com.tommihirvonen.exifnotes.datastructures.RollExportOption
import java.io.File

/**
 * Creates an Intent to share exiftool commands and a csv
 * for the frames of the roll in question.
 */
class RollShareIntentBuilder(
    private val context: Context,
    roll: Roll,
    private val options: List<RollExportOption>) : RollExport(context, roll) {

    fun create(): Intent? {

        if (options.isEmpty()) {
            return null
        }

        //Create the Intent to be shared, no initialization yet
        val shareIntent: Intent

        //Get the external storage path (not the same as SD card)
        val externalStorageDir = context.getExternalFilesDir(null)

        val files: List<File>
        try {
            files = options.map { option ->
                val filename = fileNameMapping(option)
                val content = contentMapping(option)
                val file = File(externalStorageDir, filename)
                file.writeText(content)
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating text files", Toast.LENGTH_SHORT).show()
            return null
        }

        if (files.count() == 1) {
            // Only one file is being shared

            shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val file = files.first()

            //Android Nougat requires that the file is given via FileProvider
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, context.applicationContext
                    .packageName + ".provider", file)
            } else {
                Uri.fromFile(file)
            }
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        } else {
            // Multiple files are being shared

            shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            shareIntent.type = "text/plain"

            val uris = files.map { file ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, context.applicationContext
                        .packageName + ".provider", file)
                } else {
                    Uri.fromFile(file)
                }
            }
            val arrayList = arrayListOf<Uri>().apply {
                addAll(uris)
            }
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList)
        }
        return shareIntent
    }
}