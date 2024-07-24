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

package com.tommihirvonen.exifnotes.screens.gear.lenses

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.repositories.LensRepository
import com.tommihirvonen.exifnotes.util.validate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel(assistedFactory = InterchangeableLensViewModel.Factory::class)
class InterchangeableLensViewModel @AssistedInject constructor(
    application: Application,
    @Assisted lensId: Long,
    lensRepository: LensRepository
) : LensViewModel(
    application,
    lensRepository.getLens(lensId) ?: Lens(),
    false
) {
    @AssistedFactory
    interface Factory {
        fun create(lensId: Long): InterchangeableLensViewModel
    }
}

@HiltViewModel(assistedFactory = FixedLensViewModel.Factory::class)
class FixedLensViewModel @AssistedInject constructor(
    application: Application,
    @Assisted lens: Lens
) : LensViewModel(application, lens, true) {
    @AssistedFactory
    interface Factory {
        fun create(lens: Lens): FixedLensViewModel
    }
}

abstract class LensViewModel(
    private val application: Application,
    initialLens: Lens,
    private val fixedLens: Boolean
) : AndroidViewModel(application) {

    val context: Context get() = application.applicationContext

    private val _lens = MutableStateFlow(initialLens)
    private val _makeError = MutableStateFlow(false)
    private val _modelError = MutableStateFlow(false)
    private val _apertureRangeError = MutableStateFlow("")
    private val _minFocalLengthError = MutableStateFlow("")
    private val _maxFocalLengthError = MutableStateFlow("")
    private val _apertureValues = MutableStateFlow(getApertureValues(initialLens.apertureIncrements))

    val lens = _lens.asStateFlow()
    val makeError = _makeError.asStateFlow()
    val modelError = _modelError.asStateFlow()
    val apertureRangeError = _apertureRangeError.asStateFlow()
    val minFocalLengthError = _minFocalLengthError.asStateFlow()
    val maxFocalLengthError = _maxFocalLengthError.asStateFlow()
    val apertureValues = _apertureValues.asStateFlow()

    fun setMake(value: String) {
        _lens.value = _lens.value.copy(make = value)
        _makeError.value = false
    }

    fun setModel(value: String) {
        _lens.value = _lens.value.copy(model = value)
        _modelError.value = false
    }

    fun setSerialNumber(value: String) {
        val serialNumber = value.ifEmpty { null }
        _lens.value = _lens.value.copy(serialNumber = serialNumber)
    }

    fun setApertureIncrements(value: Increment) {
        _lens.value = _lens.value.copy(apertureIncrements = value)
        val values = getApertureValues(value)
        _apertureValues.value = values
        val minFound = values.contains(lens.value.minAperture)
        val maxFound = values.contains(lens.value.maxAperture)
        // If either one wasn't found in the new values array, null them.
        if (!minFound || !maxFound) {
            setMinAperture(null)
            setMaxAperture(null)
        }
    }

    fun clearApertureRange() {
        setMinAperture(null)
        setMaxAperture(null)
    }

    fun setCustomApertureValues(values: List<Float>) {
        _lens.value = _lens.value.copy(
            customApertureValues = values.filter { it >= 0f }.sorted().distinct()
        )
    }

    private fun getApertureValues(increment: Increment) = when (increment) {
        Increment.THIRD -> context.resources.getStringArray(R.array.ApertureValuesThird).toList()
        Increment.HALF -> context.resources.getStringArray(R.array.ApertureValuesHalf).toList()
        Increment.FULL -> context.resources.getStringArray(R.array.ApertureValuesFull).toList()
    }

    fun setMinAperture(value: String?) {
        if (value?.toDoubleOrNull() != null) {
            _lens.value = _lens.value.copy(minAperture = value)
        } else {
            _lens.value = _lens.value.copy(minAperture = null)
        }
        _apertureRangeError.value = ""
    }

    fun setMaxAperture(value: String?) {
        if (value?.toDoubleOrNull() != null) {
            _lens.value = _lens.value.copy(maxAperture = value)
        } else {
            _lens.value = _lens.value.copy(maxAperture = null)
        }
        _apertureRangeError.value = ""
    }

    fun setMinFocalLength(value: String) {
        val v = value.toIntOrNull() ?: return
        if (v in 0..1_000_000) {
            _lens.value = _lens.value.copy(minFocalLength = v)
            _minFocalLengthError.value = ""
        }
    }

    fun setMaxFocalLength(value: String) {
        val v = value.toIntOrNull() ?: return
        if (v in 0..1_000_000) {
            _lens.value = _lens.value.copy(maxFocalLength = v)
            _maxFocalLengthError.value = ""
        }
    }

    fun validate(): Boolean {
        val makeValidation = { l: Lens ->
            if (l.make?.isNotEmpty() == true || fixedLens) {
                true
            } else {
                _makeError.value = true
                false
            }
        }
        val modelValidation = { l: Lens ->
            if (l.model?.isNotEmpty() == true || fixedLens) {
                true
            } else {
                _modelError.value = true
                false
            }
        }

        val focalLengthValidation = { l: Lens ->
            if (l.minFocalLength > l.maxFocalLength && l.minFocalLength >= 0 && l.maxFocalLength >= 0) {
                _minFocalLengthError.value = context.getString(R.string.MinFocalLengthGreaterThanMax)
                false
            } else if (l.minFocalLength < 0 && l.maxFocalLength < 0) {
                _minFocalLengthError.value = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                _maxFocalLengthError.value = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                false
            } else if (l.minFocalLength < 0) {
                _minFocalLengthError.value = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                false
            } else if (l.maxFocalLength < 0) {
                _maxFocalLengthError.value = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                false
            } else {
                true
            }
        }
        return _lens.value.validate(makeValidation, modelValidation, focalLengthValidation,
            ::apertureRangeValidation)
    }

    private fun apertureRangeValidation(lens: Lens): Boolean {
        // Both ends of the aperture range must be provided.
        if (lens.minAperture == null && lens.maxAperture != null ||
            lens.minAperture != null && lens.maxAperture == null) {
            _apertureRangeError.value = context.getString(R.string.NoMinOrMaxAperture)
            return false
        }
        val min = lens.minAperture?.toDoubleOrNull() ?: 0.0
        val max = lens.maxAperture?.toDoubleOrNull() ?: 0.0
        // Note, that the minimum aperture should actually be smaller in numeric value than the max.
        // Small aperture values mean a large opening and vice versa.
        if (min < max) {
            _apertureRangeError.value = context.getString(R.string.MinApertureGreaterThanMax)
            return false
        }
        return true
    }
}