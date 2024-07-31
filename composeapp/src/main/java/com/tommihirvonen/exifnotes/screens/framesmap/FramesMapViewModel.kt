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

package com.tommihirvonen.exifnotes.screens.framesmap

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.google.maps.android.compose.MapType
import com.tommihirvonen.exifnotes.screens.location.LocationPickViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FramesMapViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val _mapType: MutableStateFlow<MapType>

    val myLocationEnabled: Boolean

    init {
        val fineLocationPermissions = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        myLocationEnabled = fineLocationPermissions == PackageManager.PERMISSION_GRANTED
                || coarseLocationPermission == PackageManager.PERMISSION_GRANTED

        val mapTypePref = sharedPreferences.getInt(LocationPickViewModel.KEY_MAP_TYPE, MapType.NORMAL.value)
        val type = MapType.entries.firstOrNull { it.ordinal == mapTypePref } ?: MapType.NORMAL
        _mapType = MutableStateFlow(type)
    }

    val mapType = _mapType.asStateFlow()

    fun setMapType(mapType: MapType) {
        _mapType.value = mapType
        sharedPreferences.edit {
            putInt(LocationPickViewModel.KEY_MAP_TYPE, mapType.ordinal)
        }
    }
}