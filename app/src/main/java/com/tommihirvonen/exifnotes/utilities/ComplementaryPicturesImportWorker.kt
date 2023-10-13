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

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tommihirvonen.exifnotes.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ComplementaryPicturesImportWorker(private val context: Context, parameters: WorkerParameters)
    : CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val channelId = "exif_notes_complementary_pictures_import"

    private val progressNotificationId = 10
    private val resultNotificationId = 11

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            return Result.failure()
        }

        val picturesUri = inputData.getString(ExtraKeys.TARGET_URI)?.toUri()
            ?: return Result.failure()

        val (result, succeeded) = withContext(Dispatchers.IO) {

            val filePath: String = try {
                val inputStream = applicationContext.contentResolver.openInputStream(picturesUri)
                val outputDir = applicationContext.externalCacheDir
                val outputFile = File.createTempFile("pictures", ".zip", outputDir)
                val outputStream: OutputStream = FileOutputStream(outputFile)
                inputStream!!.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                outputFile.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext Result.failure() to false
            }

            val zipFile = File(filePath)
            val targetDirectory = ComplementaryPicturesManager
                .getComplementaryPicturesDirectory(applicationContext)
                ?: return@withContext Result.failure() to false

            try {
                val totalEntries = ZipFile(zipFile).size()
                // If the zip file was empty, end here.
                if (totalEntries == 0) {
                    return@withContext Result.failure() to false
                }
                // Publish empty progress to tell the interface, that the process has begun.
                setForeground(createProgressForegroundInfo(0, totalEntries))
                // Create target directory if it does not exists
                targetDirectory.makeDirsIfNotExists()
                ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
                    generateSequence { zipInputStream.nextEntry }.forEachIndexed { index, zipEntry ->
                        if (isStopped) {
                            return@withContext Result.success() to true
                        }
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
                            setForeground(createProgressForegroundInfo(index + 1, totalEntries))
                        }
                    }
                }
                return@withContext Result.success() to true
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure() to false
            }
        }

        with(NotificationManagerCompat.from(applicationContext)) {
            val notification = createResultNotification(succeeded)
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return@with
            }
            notify(resultNotificationId, notification)
        }

        return result
    }

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createProgressForegroundInfo(progress: Int, total: Int): ForegroundInfo {
        val title = applicationContext
            .getString(R.string.NotificationComplementaryPicturesImportTitle)
        val cancel = applicationContext.getString(R.string.Cancel)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val message = "$progress/$total"
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(message)
            .setProgress(total, progress, false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()
        return ForegroundInfo(progressNotificationId, notification)
    }

    private fun createResultNotification(success: Boolean): Notification {
        val (title, message) = if (success) {
            applicationContext.getString(R.string.NotificationComplementaryPicturesImportSuccessTitle) to
                    applicationContext.getString(R.string.NotificationComplementaryPicturesImportSuccessMessage)
        } else {
            applicationContext.getString(R.string.NotificationComplementaryPicturesImportFailTitle) to
                    applicationContext.getString(R.string.NotificationComplementaryPicturesImportFailMessage)
        }
        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        return NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val name = applicationContext
            .getString(R.string.NotificationComplementaryPicturesImportName)
        val descriptionText = applicationContext
            .getString(R.string.NotificationComplementaryPicturesImportDescription)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelId, name, importance)
        channel.description = descriptionText
        notificationManager.createNotificationChannel(channel)
    }
}