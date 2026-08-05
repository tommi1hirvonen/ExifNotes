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

package com.tommihirvonen.exifnotes.screens.gear.filters

import androidx.lifecycle.ViewModel
import com.tommihirvonen.exifnotes.core.entities.AttachmentType
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.data.repositories.FilterRepository
import com.tommihirvonen.exifnotes.util.validate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel(assistedFactory = FilterViewModel.Factory::class)
class FilterViewModel @AssistedInject constructor (
    @Assisted filterId: Long,
    @Assisted isAccessory: Boolean,
    filterRepository: FilterRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(filterId: Long, isAccessory: Boolean): FilterViewModel
    }

    private val _filter = MutableStateFlow(
        filterRepository.getFilter(filterId) ?: Filter(
            type = if (isAccessory) AttachmentType.Accessory else AttachmentType.Filter
        )
    )
    private val _makeError = MutableStateFlow(false)
    private val _modelError = MutableStateFlow(false)
    private val _factor = MutableStateFlow(_filter.value.factor.toString())
    private val _modifierError = MutableStateFlow(false)

    val filter = _filter.asStateFlow()
    val makeError = _makeError.asStateFlow()
    val modelError = _modelError.asStateFlow()
    val factor = _factor.asStateFlow()
    val modifierError = _modifierError.asStateFlow()

    fun setMake(value: String) {
        _filter.value = _filter.value.copy(make = value)
        _makeError.value = false
    }

    fun setModel(value: String) {
        _filter.value = _filter.value.copy(model = value)
        _modelError.value = false
    }

    fun setType(value: AttachmentType) {
        val factor = when (value) {
            AttachmentType.Teleconverter -> 1.4
            AttachmentType.FocalReducer -> 0.71
            AttachmentType.ExtensionTube -> 12.0
            else -> 1.0
        }
        _filter.value = _filter.value.copy(type = value, factor = factor)
        _factor.value = factor.toString()
        _modifierError.value = false
    }

    fun setFactor(value: String) {
        _factor.value = value
        value.toDoubleOrNull()?.let { _filter.value = _filter.value.copy(factor = it) }
        _modifierError.value = false
    }

    fun validate(): Boolean {
        val makeValidation = { f: Filter ->
            if (f.make?.isNotEmpty() == true) {
                true
            } else {
                _makeError.value = true
                false
            }
        }
        val modelValidation = { f: Filter ->
            if (f.model?.isNotEmpty() == true) {
                true
            } else {
                _modelError.value = true
                false
            }
        }
        val modifierValidation = { f: Filter ->
            val factor = _factor.value.toDoubleOrNull()
            val isValid = factor?.isFinite() == true && when (f.type) {
                AttachmentType.Teleconverter -> factor > 1
                AttachmentType.FocalReducer -> factor > 0 && factor < 1
                AttachmentType.ExtensionTube -> factor > 0
                else -> true
            }
            if (!f.type.usesFactor || isValid) {
                true
            } else {
                _modifierError.value = true
                false
            }
        }
        return _filter.value.validate(makeValidation, modelValidation, modifierValidation)
    }
}
