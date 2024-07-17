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

package com.tommihirvonen.exifnotes

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.data.Database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val database: Database
) : AndroidViewModel(application) {

    companion object {
        const val KEY_GPS_UPDATE = "GPSUpdate"
        const val KEY_IGNORE_WARNINGS = "IgnoreWarnings"
        const val KEY_FILE_ENDING = "FileEnding"
        const val KEY_PATH_TO_PICTURES = "PicturesPath"
        const val KEY_EXIFTOOL_PATH = "ExiftoolPath"
        const val KEY_COPYRIGHT_INFO = "CopyrightInformation"
        const val KEY_ARTIST_NAME = "ArtistName"
    }

    private val context get() = application.applicationContext
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)

    val locationUpdatesEnabled get() = _locationUpdatesEnabled as StateFlow<Boolean>
    val artistName get() = _artistName as StateFlow<String>
    val copyrightInfo get() = _copyrightInfo as StateFlow<String>
    val exiftoolPath get() = _exiftoolPath as StateFlow<String>
    val pathToPictures get() = _pathToPictures as StateFlow<String>
    val fileEnding get() = _fileEnding as StateFlow<String>
    val ignoreWarnings get() = _ignoreWarnings as StateFlow<Boolean>

    private val _locationUpdatesEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_GPS_UPDATE, true)
    )
    private val _artistName = MutableStateFlow(
        sharedPreferences.getString(KEY_ARTIST_NAME, "") ?: ""
    )
    private val _copyrightInfo = MutableStateFlow(
        sharedPreferences.getString(KEY_COPYRIGHT_INFO, "")?: ""
    )
    private val _exiftoolPath = MutableStateFlow(
        sharedPreferences.getString(KEY_EXIFTOOL_PATH, "")?: ""
    )
    private val _pathToPictures = MutableStateFlow(
        sharedPreferences.getString(KEY_PATH_TO_PICTURES, "")?: ""
    )
    private val _fileEnding = MutableStateFlow(
        sharedPreferences.getString(KEY_FILE_ENDING, "")?: ""
    )
    private val _ignoreWarnings = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_IGNORE_WARNINGS, false)
    )

    fun setLocationUpdatesEnabled(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_GPS_UPDATE, value)
        }
        _locationUpdatesEnabled.value = value
    }

    fun setArtistName(value: String) {
        sharedPreferences.edit {
            putString(KEY_ARTIST_NAME, value)
        }
        _artistName.value = value
    }

    fun setCopyrightInfo(value: String) {
        sharedPreferences.edit {
            putString(KEY_COPYRIGHT_INFO, value)
        }
        _copyrightInfo.value = value
    }

    fun setExiftoolPath(value: String) {
        sharedPreferences.edit {
            putString(KEY_EXIFTOOL_PATH, value)
        }
        _exiftoolPath.value = value
    }

    fun setPathToPictures(value: String) {
        sharedPreferences.edit {
            putString(KEY_PATH_TO_PICTURES, value)
        }
        _pathToPictures.value = value
    }

    fun setFileEnding(value: String) {
        sharedPreferences.edit {
            putString(KEY_FILE_ENDING, value)
        }
        _fileEnding.value = value
    }

    fun setIgnoreWarnings(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_IGNORE_WARNINGS, value)
        }
        _ignoreWarnings.value = value
    }

    fun exportDatabase(
        destinationUri: Uri,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        try {
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
            val databaseFile = database.getDatabaseFile()
            val inputStream: InputStream = FileInputStream(databaseFile)
            inputStream.copyTo(outputStream!!)
            inputStream.close()
            outputStream.close()
            onSuccess()
        } catch (e: IOException) {
            e.printStackTrace()
            onError()
        }
    }

    fun importDatabase(
        sourceUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try{
            // Copy the content from the Uri to a cached File so it can be read as a File.

            // Check the extension of the given file.
            val cursor = context.contentResolver.query(sourceUri,
                null, null, null, null)
            cursor!!.moveToFirst()
            val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            if (File(name).extension != "db") {
                onError("Not a valid .db file!")
                return
            }
            cursor.close()

            // Copy file for database import.
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val outputDir = context.externalCacheDir
            val outputFile = File.createTempFile("database", ".db", outputDir)
            val outputStream: OutputStream = FileOutputStream(outputFile)
            inputStream!!.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            val filePath = outputFile.absolutePath
            val extension = outputFile.extension

            //If the length of filePath is 0, then the user canceled the import.
            if (filePath.isNotEmpty() && extension == "db") {
                val importSuccess: Boolean = try {
                    database.importDatabase(filePath)
                } catch (e: IOException) {
                    val message = context.resources.getString(R.string.ErrorImportingDatabaseFrom) +
                            filePath
                    onError(message)
                    return
                }
                if (importSuccess) {
                    val message = context.resources.getString(R.string.DatabaseImported)
                    onSuccess(message)
                } else {
                    onError("Import failed")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}