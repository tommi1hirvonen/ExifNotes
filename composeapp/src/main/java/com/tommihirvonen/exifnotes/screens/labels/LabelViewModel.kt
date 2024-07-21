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

package com.tommihirvonen.exifnotes.screens.labels

import androidx.lifecycle.ViewModel
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.data.repositories.LabelRepository
import com.tommihirvonen.exifnotes.util.validate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel(assistedFactory = LabelViewModel.Factory::class)
class LabelViewModel @AssistedInject constructor (
    @Assisted labelId: Long,
    labelRepository: LabelRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(labelId: Long): LabelViewModel
    }

    val label: Label = labelRepository.getLabel(labelId) ?: Label()

    val labelName get() = _labelName as StateFlow<String>
    private val _labelName = MutableStateFlow(label.name)

    val labelNameError get() = _labelNameError as StateFlow<Boolean>
    private val _labelNameError = MutableStateFlow(false)

    fun setLabelName(value: String) {
        label.name = value
        _labelName.value = value
        _labelNameError.value = false
    }

    fun validate(): Boolean {
        val nameValidation = { l: Label ->
            if (l.name.isNotEmpty()) {
                true
            } else {
                _labelNameError.value = true
                false
            }
        }
        return label.validate(nameValidation)
    }
}