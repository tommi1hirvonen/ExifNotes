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

package com.tommihirvonen.exifnotes.screens.frameedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import com.tommihirvonen.exifnotes.di.location.LocationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

@HiltViewModel(assistedFactory = FrameViewModel.Factory::class )
class FrameViewModel @AssistedInject constructor(
    @Assisted("rollId") rollId: Long,
    @Assisted("frameId") frameId: Long,
    @Assisted("previousFrameId") previousFrameId: Long,
    @Assisted("frameCount") frameCount: Int,
    application: Application,
    frameRepository: FrameRepository,
    rollRepository: RollRepository,
    locationService: LocationService
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("rollId") rollId: Long,
            @Assisted("frameId") frameId: Long,
            @Assisted("previousFrameId") previousFrameId: Long,
            @Assisted("frameCount") frameCount: Int
        ): FrameViewModel
    }

    private val _frame: MutableStateFlow<Frame>

    init {
        val existingFrame = frameRepository.getFrame(frameId)
        val frame = if (existingFrame != null) {
            existingFrame
        } else {
            val date = LocalDateTime.now()
            val noOfExposures = 1
            val location = locationService.lastLocation?.let { LatLng(it.latitude, it.longitude) }
            val previousFrame = frameRepository.getFrame(previousFrameId)
            if (previousFrame != null) {
                Frame(
                    roll = previousFrame.roll,
                    count = frameCount,
                    date = date,
                    noOfExposures = noOfExposures,
                    location = location,
                    lens = previousFrame.lens,
                    shutter = previousFrame.shutter,
                    aperture = previousFrame.aperture,
                    filters = previousFrame.filters,
                    focalLength = previousFrame.focalLength,
                    lightSource = previousFrame.lightSource
                )
            } else {
                val roll = rollRepository.getRoll(rollId)
                Frame(
                    roll = roll ?: Roll(id = rollId),
                    count = frameCount,
                    date = date,
                    noOfExposures = noOfExposures,
                    location = location
                )
            }
        }
        _frame = MutableStateFlow(frame)
    }

    val frame = _frame.asStateFlow()

    fun setCount(value: Int) {
        _frame.value = _frame.value.copy(count = value)
    }

    fun setDate(value: LocalDateTime) {
        _frame.value = _frame.value.copy(date = value)
    }

    fun setNote(value: String) {
        _frame.value = _frame.value.copy(note = value)
    }

    fun validate(): Boolean = true

}