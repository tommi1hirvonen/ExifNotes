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

package com.tommihirvonen.exifnotes.screens.gear.cameras

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Format
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.PartialIncrement
import com.tommihirvonen.exifnotes.core.toShutterSpeedOrNull
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.util.validate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel(assistedFactory = CameraViewModel.Factory::class)
class CameraViewModel @AssistedInject constructor(
    @Assisted cameraId: Long,
    private val application: Application,
    cameraRepository: CameraRepository
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(cameraId: Long): CameraViewModel
    }

    val context: Context get() = application.applicationContext

    private val _camera = MutableStateFlow(
        cameraRepository.getCamera(cameraId) ?: Camera()
    )
    private val _makeError = MutableStateFlow(false)
    private val _modelError = MutableStateFlow(false)
    private val _shutterRangeError = MutableStateFlow("")
    private val _fixedLensSummary by lazy {
        MutableStateFlow(getFixedLensSummary())
    }
    private val _shutterValues by lazy {
        MutableStateFlow(getShutterValues(_camera.value.shutterIncrements))
    }

    val camera = _camera.asStateFlow()
    val fixedLensSummary = _fixedLensSummary.asStateFlow()
    val makeError = _makeError.asStateFlow()
    val modelError = _modelError.asStateFlow()
    val shutterRangeError = _shutterRangeError.asStateFlow()
    val shutterValues = _shutterValues.asStateFlow()

    fun setMake(value: String) {
        _camera.value = _camera.value.copy(make = value)
        _makeError.value = false
    }

    fun setModel(value: String) {
        _camera.value = _camera.value.copy(model = value)
        _modelError.value = false
    }

    fun setSerialNumber(value: String) {
        val serialNumber = value.ifEmpty { null }
        _camera.value = _camera.value.copy(serialNumber = serialNumber)
    }

    fun setShutterIncrements(value: Increment) {
        _camera.value = _camera.value.copy(shutterIncrements = value)
        val values = getShutterValues(value)
        _shutterValues.value = values
        val minFound = values.contains(camera.value.minShutter)
        val maxFound = values.contains(camera.value.maxShutter)
        // If either one wasn't found in the new values array, null them.
        if (!minFound || !maxFound) {
            setMinShutter(null)
            setMaxShutter(null)
        }
    }

    fun setMinShutter(value: String?) {
        val actualValue = value?.toShutterSpeedOrNull()
        _camera.value = _camera.value.copy(minShutter = actualValue)
        _shutterRangeError.value = ""
    }

    fun setMaxShutter(value: String?) {
        val actualValue = value?.toShutterSpeedOrNull()
        _camera.value = _camera.value.copy(maxShutter = actualValue)
        _shutterRangeError.value = ""
    }

    fun clearShutterRange() {
        setMinShutter(null)
        setMaxShutter(null)
    }

    fun setExposureCompIncrements(value: PartialIncrement) {
        _camera.value = _camera.value.copy(exposureCompIncrements = value)
    }

    fun setFormat(value: Format) {
        _camera.value = _camera.value.copy(format = value)
    }

    fun setLens(lens: Lens?) {
        _camera.value = _camera.value.copy(lens = lens)
        _fixedLensSummary.value = getFixedLensSummary()
    }

    fun validate(): Boolean = camera.value.validate(
        makeValidation,
        modelValidation,
        ::shutterRangeValidation
    )

    private val makeValidation = { c: Camera ->
        if (c.make.isNullOrEmpty()) {
            _makeError.value = true
            false
        } else {
            true
        }
    }

    private val modelValidation = { c: Camera ->
        if (c.model.isNullOrEmpty()) {
            _modelError.value = true
            false
        } else {
            true
        }
    }

    private fun getFixedLensSummary(): String {
        val lens = camera.value.lens
        return if (lens == null) {
            context.resources.getString(R.string.ClickToSet)
        } else {
            val (minFocal, maxFocal) = lens.minFocalLength to lens.maxFocalLength
            val focalText = when {
                minFocal != maxFocal -> "$minFocal-${maxFocal}mm"
                minFocal > 0 -> "${minFocal}mm"
                else -> null
            }
            val maxAperture = lens.maxAperture
            val apertureText = if (maxAperture == null) {
                null
            } else {
                "f/$maxAperture"
            }
            val text = listOfNotNull(focalText, apertureText).joinToString(" ").ifEmpty {
                context.resources.getString(R.string.ClickToEdit)
            }
            text
        }
    }

    private fun getShutterValues(increment: Increment) = when (increment) {
        Increment.THIRD -> context.resources.getStringArray(com.tommihirvonen.exifnotes.core.R.array.ShutterValuesThird)
        Increment.HALF -> context.resources.getStringArray(com.tommihirvonen.exifnotes.core.R.array.ShutterValuesHalf)
        Increment.FULL -> context.resources.getStringArray(com.tommihirvonen.exifnotes.core.R.array.ShutterValuesFull)
    }.reversed()

    private fun shutterRangeValidation(camera: Camera): Boolean {
        // Both ends of the shutter range must be provided.
        if (camera.minShutter == null && camera.maxShutter != null ||
            camera.minShutter != null && camera.maxShutter == null) {
            _shutterRangeError.value = context.getString(R.string.NoMinOrMaxShutter)
            return false
        }
        val minIndex = shutterValues.value.indexOf(camera.minShutter)
        val maxIndex = shutterValues.value.indexOf(camera.maxShutter)
        if (minIndex > maxIndex) {
            _shutterRangeError.value = context.getString(R.string.MinShutterSpeedGreaterThanMax)
            return false
        }
        return true
    }
}