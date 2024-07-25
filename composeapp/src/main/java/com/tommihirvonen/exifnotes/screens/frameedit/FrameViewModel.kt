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
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.LightSource
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.toShutterSpeedOrNull
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.data.repositories.FilterRepository
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.data.repositories.LensRepository
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
    private val application: Application,
    frameRepository: FrameRepository,
    rollRepository: RollRepository,
    lensRepository: LensRepository,
    private val cameraRepository: CameraRepository,
    private val filterRepository: FilterRepository,
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

    private val context: Context get() = application.applicationContext
    private val _frame: MutableStateFlow<Frame>
    private val _lens: MutableStateFlow<Lens?>

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
        _lens = MutableStateFlow(
            frame.roll.camera?.lens ?: frame.lens
        )
    }

    private val _filters = MutableStateFlow(getFilters(_lens.value))
    private val _apertureValues = MutableStateFlow(getApertureValues(_lens.value))

    val frame = _frame.asStateFlow()
    val lens = _lens.asStateFlow()
    val filters = _filters.asStateFlow()
    val apertureValues = _apertureValues.asStateFlow()

    val lenses = _frame.value.roll.camera?.let(cameraRepository::getLinkedLenses)
        ?: lensRepository.lenses
    val shutterValues = _frame.value.roll.camera?.shutterSpeedValues(context)?.toList()
        ?: Camera.defaultShutterSpeedValues(context).toList()
    val exposureCompValues = _frame.value.roll.camera?.exposureCompValues(context)?.toList()
        ?: Camera.defaultExposureCompValues(context).toList()

    fun setCount(value: Int) {
        _frame.value = _frame.value.copy(count = value)
    }

    fun setDate(value: LocalDateTime) {
        _frame.value = _frame.value.copy(date = value)
    }

    fun setNote(value: String) {
        _frame.value = _frame.value.copy(note = value)
    }

    fun setShutter(value: String?) {
        _frame.value = _frame.value.copy(shutter = value?.toShutterSpeedOrNull())
    }

    fun setAperture(value: String?) {
        if (value == null || value.toDoubleOrNull() != null) {
            val actualValue = value?.replace(
                regex = "[^\\d.]".toRegex(),
                replacement = ""
            )
            _frame.value = _frame.value.copy(aperture = actualValue)
        }
    }

    fun setExposureComp(value: String) {
        val actualValue = if (value == "0") null else value
        _frame.value = _frame.value.copy(exposureComp = actualValue)
    }

    fun setNoOfExposures(value: Int) {
        if (value >= 1) {
            _frame.value = _frame.value.copy(noOfExposures = value)
        }
    }

    fun setFlashUsed(value: Boolean) {
        _frame.value = _frame.value.copy(flashUsed = value)
    }

    fun setLightSource(value: LightSource) {
        _frame.value = _frame.value.copy(lightSource = value)
    }

    fun setLens(value: Lens?) {
        val frame = _frame.value
        val filters = getFilters(value)
        val apertureValues = getApertureValues(value)
        _filters.value = filters
        _apertureValues.value = apertureValues
        val aperture = if (!apertureValues.contains(frame.aperture)) null else frame.aperture
        val focalLength = if (value != null && frame.focalLength > value.maxFocalLength)
            value.maxFocalLength
        else if (value != null && frame.focalLength < value.minFocalLength)
            value.minFocalLength
        else
            frame.focalLength
        _lens.value = value
        _frame.value = frame.copy(
            lens = value,
            filters = frame.filters.filter(filters::contains),
            aperture = aperture,
            focalLength = focalLength
        )
    }

    fun setFilters(value: List<Filter>) {
        _frame.value = _frame.value.copy(filters = value)
    }

    fun setFocalLength(value: Int) {
        _frame.value = _frame.value.copy(focalLength = value)
    }

    fun validate(): Boolean = true

    private fun getFilters(lens: Lens?): List<Filter> =
        lens?.let(filterRepository::getLinkedFilters)
            ?: this.lens.value?.let(filterRepository::getLinkedFilters)
            ?: filterRepository.filters

    private fun getApertureValues(lens: Lens?): List<String> =
        lens?.apertureValues(context)?.toList()
            ?: this.lens.value?.apertureValues(context)?.toList()
            ?: Lens.defaultApertureValues(context).toList()
}