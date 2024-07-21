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
    filterRepository: FilterRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(filterId: Long): FilterViewModel
    }

    val filter = filterRepository.getFilter(filterId) ?: Filter()

    private val _make = MutableStateFlow(filter.make)
    private val _model = MutableStateFlow(filter.model)
    private val _makeError = MutableStateFlow(false)
    private val _modelError = MutableStateFlow(false)

    val make = _make.asStateFlow()
    val model = _model.asStateFlow()
    val makeError = _makeError.asStateFlow()
    val modelError = _modelError.asStateFlow()

    fun setMake(value: String) {
        filter.make = value
        _make.value = value
        _makeError.value = false
    }

    fun setModel(value: String) {
        filter.model = value
        _model.value = value
        _modelError.value = false
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
        return filter.validate(makeValidation, modelValidation)
    }
}