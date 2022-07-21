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
import android.app.AlertDialog
import android.widget.Chronometer
import android.widget.ProgressBar
import android.widget.TextView
import com.tommihirvonen.exifnotes.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

internal class ZipFileReader(activity: Activity) {

    private val progressBar: ProgressBar
    private val progressTextView: TextView
    private val messageTextView: TextView
    private val chronometer: Chronometer
    private val dialog: AlertDialog

    init {
        // Show a dialog with progress bar, elapsed time, completed zip entries and total zip entries.
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

    suspend fun read(zipFile: File, targetDirectory: File): Pair<Boolean, Int> {
        dialog.show()
        chronometer.start()

        @Suppress("BlockingMethodInNonBlockingContext")
        val result = withContext(Dispatchers.IO) {
            try {
                val totalEntries = ZipFile(zipFile).size()
                // If the zip file was empty, end here.
                if (totalEntries == 0) {
                    return@withContext false to 0
                }
                // Publish empty progress to tell the interface, that the process has begun.
                updateProgress(0, totalEntries)
                // Create target directory if it does not exists
                targetDirectory.makeDirsIfNotExists()
                var completedEntries = 0
                ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
                    generateSequence { zipInputStream.nextEntry }.forEach { zipEntry ->
                        val targetFile = File(targetDirectory, zipEntry.name)
                        if (!targetFile.canonicalPath.startsWith(targetDirectory.canonicalPath)) {
                            throw SecurityException("Possible path traversal characters detected in zip entry path")
                        } else if (zipEntry.isDirectory) {
                            targetFile.makeDirsIfNotExists()
                        } else {
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                            FileOutputStream(targetFile).use { outputStream ->
                                zipInputStream.copyTo(outputStream)
                            }
                            zipInputStream.closeEntry()
                            ++completedEntries
                            updateProgress(completedEntries, totalEntries)
                        }
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