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
import android.content.Context.NOTIFICATION_SERVICE
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ComplementaryPicturesExportWorker(private val context: Context, parameters: WorkerParameters)
    : CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private val channelId = "exif_notes_complementary_pictures_export"

    private val progressNotificationId = 0
    private val resultNotificationId = 1

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            return Result.failure()
        }

        val complementaryPictureFilenames =
            applicationContext.database.complementaryPictureFilenames
        val picturesDirectory =
            ComplementaryPicturesManager.getComplementaryPicturesDirectory(applicationContext)
            ?: return Result.failure()
        val filenameFilter = FilenameFilter { _: File?, s: String? ->
            complementaryPictureFilenames.contains(s)
        }
        val pictureFiles = picturesDirectory.listFiles(filenameFilter)
        if (pictureFiles.isNullOrEmpty()) {
            return Result.success()
        }

        setForeground(createProgressForegroundInfo(0, pictureFiles.size))

        val (result, succeeded) = withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("complementary_pictures", ".zip",
                applicationContext.externalCacheDir)
            val tempOutputStream = FileOutputStream(tempFile)
            ZipOutputStream(tempOutputStream).use { outputStream ->
                pictureFiles.forEachIndexed { index, file ->
                    if (isStopped) {
                        return@withContext Result.success() to true
                    }
                    BufferedInputStream(FileInputStream(file)).use { inputStream ->
                        val entry = ZipEntry(file.name)
                        outputStream.putNextEntry(entry)
                        inputStream.copyTo(outputStream)
                    }
                    setForeground(createProgressForegroundInfo(index + 1, pictureFiles.size))
                }
            }

            val targetUri = inputData.getString(ExtraKeys.TARGET_URI)?.toUri()
                ?: return@withContext Result.failure() to false
            val targetOutputStream = applicationContext.contentResolver.openOutputStream(targetUri)

            val tempInputStream = FileInputStream(tempFile)
            tempInputStream.copyTo(targetOutputStream!!)
            tempInputStream.close()
            targetOutputStream.close()
            return@withContext Result.success() to true
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
            .getString(R.string.NotificationComplementaryPicturesExportTitle)
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
            applicationContext.getString(R.string.NotificationComplementaryPicturesExportSuccessTitle) to
                    applicationContext.getString(R.string.NotificationComplementaryPicturesExportSuccessMessage)
        } else {
            applicationContext.getString(R.string.NotificationComplementaryPicturesExportFailTitle) to
                    applicationContext.getString(R.string.NotificationComplementaryPicturesExportFailMessage)
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
            .getString(R.string.NotificationComplementaryPicturesExportName)
        val descriptionText = applicationContext
            .getString(R.string.NotificationComplementaryPicturesExportDescription)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelId, name, importance)
        channel.description = descriptionText
        notificationManager.createNotificationChannel(channel)
    }
}