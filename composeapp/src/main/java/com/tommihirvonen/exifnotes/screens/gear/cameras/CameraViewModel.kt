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
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.PartialIncrement
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

    val camera = cameraRepository.getCamera(cameraId) ?: Camera()

    private val _make = MutableStateFlow(camera.make)
    private val _model = MutableStateFlow(camera.model)
    private val _serialNumber = MutableStateFlow(camera.serialNumber)
    private val _shutterIncrements = MutableStateFlow(camera.shutterIncrements)
    private val _minShutter = MutableStateFlow(camera.minShutter)
    private val _maxShutter = MutableStateFlow(camera.maxShutter)
    private val _exposureCompIncrements = MutableStateFlow(camera.exposureCompIncrements)
    private val _makeError = MutableStateFlow(false)
    private val _modelError = MutableStateFlow(false)
    private val _shutterRangeError = MutableStateFlow("")
    private val _shutterValues = MutableStateFlow(getShutterValues())

    val make = _make.asStateFlow()
    val model = _model.asStateFlow()
    val serialNumber = _serialNumber.asStateFlow()
    val shutterIncrements = _shutterIncrements.asStateFlow()
    val minShutter = _minShutter.asStateFlow()
    val maxShutter = _maxShutter.asStateFlow()
    val exposureCompIncrements = _exposureCompIncrements.asStateFlow()
    val makeError = _makeError.asStateFlow()
    val modelError = _modelError.asStateFlow()
    val shutterRangeError = _shutterRangeError.asStateFlow()
    val shutterValues = _shutterValues.asStateFlow()

    fun setMake(value: String) {
        camera.make = value
        _make.value = value
        _makeError.value = false
    }

    fun setModel(value: String) {
        camera.model = value
        _model.value = value
        _modelError.value = false
    }

    fun setSerialNumber(value: String) {
        camera.serialNumber = value.ifEmpty { null }
        _serialNumber.value = value
    }

    fun setShutterIncrements(value: Increment) {
        camera.shutterIncrements = value
        _shutterIncrements.value = value
        _shutterValues.value = getShutterValues()
        val options = getShutterValues()
        val minFound = options.contains(camera.minShutter)
        val maxFound = options.contains(camera.maxShutter)
        // If either one wasn't found in the new values array, null them.
        if (!minFound || !maxFound) {
            setMinShutter(null)
            setMaxShutter(null)
        }
    }

    fun setMinShutter(value: String?) {
        // TODO regex validation
        camera.minShutter = value
        _minShutter.value = value
        _shutterRangeError.value = ""
    }

    fun setMaxShutter(value: String?) {
        // TODO regex validation
        camera.maxShutter = value
        _maxShutter.value = value
        _shutterRangeError.value = ""
    }

    fun clearShutterRange() {
        setMinShutter(null)
        setMaxShutter(null)
    }

    fun setExposureCompIncrements(value: PartialIncrement) {
        camera.exposureCompIncrements = value
        _exposureCompIncrements.value = value
    }

    fun validate(): Boolean = camera.validate(
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

    private fun getShutterValues() = when (camera.shutterIncrements) {
        Increment.THIRD -> context.resources.getStringArray(R.array.ShutterValuesThird)
        Increment.HALF -> context.resources.getStringArray(R.array.ShutterValuesHalf)
        Increment.FULL -> context.resources.getStringArray(R.array.ShutterValuesFull)
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