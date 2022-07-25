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

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Chronometer
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.tommihirvonen.exifnotes.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class ZipFileWriter(activity: Activity) {

    private val progressBar: ProgressBar
    private val progressTextView: TextView
    private val messageTextView: TextView
    private val chronometer: Chronometer
    private val dialog: AlertDialog

    init {
        val builder = AlertDialog.Builder(activity)
        @SuppressLint("InflateParams")
        val view = activity.layoutInflater.inflate(R.layout.dialog_progress, null)
        builder.setView(view)
        messageTextView = view.findViewById(R.id.textview_1)
        messageTextView.text = ""
        progressTextView = view.findViewById(R.id.textview_2)
        progressTextView.text = ""
        chronometer = view.findViewById(R.id.elapsed_time)
        progressBar = view.findViewById(R.id.progress_bar)
        progressBar.max = 100
        progressBar.progress = 0
        dialog = builder.create()
        dialog.setCancelable(false)
    }

    fun setMessage(message: String) = apply { messageTextView.text = message }

    suspend fun export(files: Array<File>, zipFile: File): Pair<Boolean, Int> {
        dialog.show()
        chronometer.start()

        @Suppress("BlockingMethodInNonBlockingContext")
        val result = withContext(Dispatchers.IO) {
            try {
                // If the files array is empty, return true and end here.
                if (files.isEmpty()){
                    return@withContext false to 0
                }
                var completedEntries = 0
                // Publish empty progress to tell the interface, that the process has begun.
                updateProgress(0, files.size)

                ZipOutputStream(FileOutputStream(zipFile)).use { outputStream ->
                    files.forEach { file ->
                        BufferedInputStream(FileInputStream(file)).use { inputStream ->
                            val entry = ZipEntry(file.name)
                            outputStream.putNextEntry(entry)
                            inputStream.copyTo(outputStream)
                        }
                        ++completedEntries
                        updateProgress(completedEntries, files.size)
                    }
                }
                return@withContext true to completedEntries
            } catch (e: Exception) {
                return@withContext false to 0
            }
        }

        chronometer.stop()
        dialog.dismiss()

        return result
    }

    private suspend fun updateProgress(completed: Int, total: Int) {
        withContext(Dispatchers.Main) {
            val progressPercentage = (completed.toFloat() / total.toFloat() * 100f).toInt()
            progressBar.progress = progressPercentage
            val progressText = "$completed/$total"
            progressTextView.text = progressText
        }
    }
}