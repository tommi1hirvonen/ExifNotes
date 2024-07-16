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
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    companion object {
        const val KEY_APP_THEME = "AppTheme"
        const val KEY_GPS_UPDATE = "GPSUpdate"
        const val KEY_IGNORE_WARNINGS = "IgnoreWarnings"
        const val KEY_FILE_ENDING = "FileEnding"
        const val KEY_PATH_TO_PICTURES = "PicturesPath"
        const val KEY_EXIFTOOL_PATH = "ExiftoolPath"
        const val KEY_COPYRIGHT_INFO = "CopyrightInformation"
        const val KEY_ARTIST_NAME = "ArtistName"
    }

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
}