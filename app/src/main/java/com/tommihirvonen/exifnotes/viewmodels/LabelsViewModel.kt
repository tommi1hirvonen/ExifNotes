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

package com.tommihirvonen.exifnotes.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.data.repositories.LabelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LabelsViewModel @Inject constructor(private val labelRepository: LabelRepository)
    : ViewModel() {
    val labels: LiveData<List<Label>> get() = mLabels

    private val mLabels: MutableLiveData<List<Label>> by lazy {
        MutableLiveData<List<Label>>().also {
            viewModelScope.launch { loadLabels() }
        }
    }

    private val labelsList = mutableListOf<Label>()

    fun submitLabel(label: Label) {
        if (labelRepository.updateLabel(label) == 0) {
            labelRepository.addLabel(label)
            labelsList.add(label)
            labelsList.sortBy { it.name }
        }
    }

    fun deleteLabel(label: Label) {
        labelRepository.deleteLabel(label)
        labelsList.remove(label)
        mLabels.postValue(labelsList)
    }

    private suspend fun loadLabels() {
        withContext(Dispatchers.IO) {
            labelsList.clear()
            labelsList.addAll(labelRepository.labels)
            mLabels.postValue(labelsList)
        }
    }
}