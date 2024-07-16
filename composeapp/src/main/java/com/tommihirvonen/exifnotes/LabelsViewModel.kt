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

package com.tommihirvonen.exifnotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.data.repositories.LabelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class LabelsViewModel @Inject constructor(private val labelRepository: LabelRepository)
    : ViewModel() {

    val labels: StateFlow<List<Label>> get() = mLabels
    private val mLabels = MutableStateFlow(emptyList<Label>())

    init {
        viewModelScope.launch {
            loadLabels()
        }
    }

    fun submitLabel(label: Label) {
        if (labelRepository.updateLabel(label) == 0) {
            labelRepository.addLabel(label)
        }
        val labels = labels.value
            .filter { it.id != label.id }
            .plus(label)
            .sortedBy { it.name }
        mLabels.value = labels
    }

    fun deleteLabel(label: Label) {
        labelRepository.deleteLabel(label)
        mLabels.value = labels.value.minus(label)
    }

    private suspend fun loadLabels() {
        withContext(Dispatchers.IO) {
            mLabels.value = labelRepository.labels
        }
    }
}