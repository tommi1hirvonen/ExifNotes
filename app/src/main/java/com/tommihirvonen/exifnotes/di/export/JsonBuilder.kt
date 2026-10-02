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

package com.tommihirvonen.exifnotes.di.export

import android.content.Context
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonBuilder @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val format = Json { prettyPrint = true }

    fun create(roll: Roll, frames: List<Frame>): String {
        val rollData = format.encodeToJsonElement(roll.copy(frames = frames)).jsonObject
        val export = buildJsonObject {
            rollData.forEach { (key, value) -> put(key, value) }
            put("artist", prefs.getString(SettingsViewModel.KEY_ARTIST_NAME, "") ?: "")
            put("copyright", prefs.getString(SettingsViewModel.KEY_COPYRIGHT_INFO, "") ?: "")
        }
        return format.encodeToString(export)
    }
}
