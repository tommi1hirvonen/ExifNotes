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

package com.tommihirvonen.exifnotes.theme

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
class ThemeViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    companion object {
        const val KEY_APP_THEME = "AppTheme"
    }

    val theme get() = _theme as StateFlow<Theme>

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)
    private val _theme: MutableStateFlow<Theme>

    init {
        val preferenceValue = sharedPreferences.getString(KEY_APP_THEME, "DEFAULT")
            ?: "DEFAULT"
        val initialValue = when (preferenceValue) {
            "LIGHT" -> Theme.Light
            "DARK" -> Theme.Dark
            else -> Theme.Auto
        }
        _theme = MutableStateFlow(initialValue)
    }

    fun setTheme(theme: Theme) {
        val preferenceValue = when (theme) {
            is Theme.Light -> "LIGHT"
            is Theme.Dark -> "DARK"
            is Theme.Auto -> "DEFAULT"
        }
        sharedPreferences.edit {
            putString(KEY_APP_THEME, preferenceValue)
        }
        _theme.value = theme
    }
}

sealed class Theme {
    data object Auto : Theme()
    data object Light : Theme()
    data object Dark : Theme()
}