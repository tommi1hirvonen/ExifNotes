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

package com.tommihirvonen.exifnotes.screens.rollsmap

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.MapType
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.screens.location.LocationPickViewModel
import com.tommihirvonen.exifnotes.util.LoadState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = RollsMapViewModel.Factory::class)
class RollsMapViewModel @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted private val initialRolls: List<Roll>,
    private val frameRepository: FrameRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(initialRolls: List<Roll>): RollsMapViewModel
    }

    private val markerBitmaps = getMarkerBitmaps(context)

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

        loadData()
    }

    val mapType = _mapType.asStateFlow()

    private val _rolls = MutableStateFlow<LoadState<List<RollData>>>(LoadState.InProgress())
    val rolls = _rolls.asStateFlow()

    fun setMapType(mapType: MapType) {
        _mapType.value = mapType
        sharedPreferences.edit {
            putInt(LocationPickViewModel.KEY_MAP_TYPE, mapType.ordinal)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val data = initialRolls.mapIndexed { index, roll ->
                    val i = index % markerBitmaps.size
                    RollData(roll, true, markerBitmaps[i], frameRepository.getFrames(roll))
                }
                _rolls.value = LoadState.Success(data)
            }
        }
    }
}

data class RollData(
    val roll: Roll,
    val selected: Boolean,
    val marker: Bitmap,
    val frames: List<Frame>
)

private fun getMarkerBitmaps(context: Context): List<Bitmap> = arrayListOf(
    getMarkerBitmap(context),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_AZURE),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_GREEN),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_ORANGE),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_YELLOW),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_BLUE),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_ROSE),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_CYAN),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_VIOLET),
    getMarkerBitmap(context, BitmapDescriptorFactory.HUE_MAGENTA)
)

private fun getMarkerBitmap(context: Context, hue: Float): Bitmap {
    val bitmap = getMarkerBitmap(context)
    return setBitmapHue(bitmap, hue)
}

private fun getMarkerBitmap(context: Context): Bitmap {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_red)!!
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun setBitmapHue(bitmap: Bitmap, hue: Float): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val hvs = FloatArray(3)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, y)
            Color.colorToHSV(pixel, hvs)
            hvs[0] = hue
            bitmap.setPixel(x, y, Color.HSVToColor(Color.alpha(pixel), hvs))
        }
    }
    return bitmap
}