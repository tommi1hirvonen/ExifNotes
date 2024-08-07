/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.core.entities.RollSortMode
import com.tommihirvonen.exifnotes.core.entities.sorted
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.data.repositories.LabelRepository
import com.tommihirvonen.exifnotes.data.repositories.RollCounts
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RollsViewModel @Inject constructor(
    application: Application,
    private val rollRepository: RollRepository,
    private val cameraRepository: CameraRepository,
    private val labelRepository: LabelRepository) : AndroidViewModel(application) {

    val rolls: StateFlow<State<List<Roll>>> get() = mRolls
    val cameras: StateFlow<List<Camera>> get() = mCameras
    val labels: StateFlow<List<Label>> get() = mLabels
    val rollCounts: StateFlow<RollCounts> get() = mRollCounts
    val toolbarSubtitle: StateFlow<String> get() = mToolbarSubtitle
    val rollFilterMode: StateFlow<RollFilterMode> get() = mRollFilterMode
    val rollSortMode: StateFlow<RollSortMode> get() = mRollSortMode

    private val mRolls = MutableStateFlow<State<List<Roll>>>(State.InProgress())
    private val mCameras = MutableStateFlow(emptyList<Camera>())
    private val mLabels = MutableStateFlow(emptyList<Label>())
    private val mRollCounts = MutableStateFlow(RollCounts(0, 0, 0))
    private val mToolbarSubtitle = MutableStateFlow(context.resources.getString(R.string.ActiveRolls))
    private val mRollFilterMode = MutableStateFlow<RollFilterMode>(RollFilterMode.Active)
    private val mRollSortMode = MutableStateFlow(RollSortMode.DATE)

    init {
        loadAll()
    }

    val selectedRolls = HashSet<Roll>()
    var refreshPending = false

    private val context get() = getApplication<Application>()
    private var rollList = emptyList<Roll>()

    fun setRollFilterMode(rollFilterMode: RollFilterMode) {
        mRollFilterMode.value = rollFilterMode
        val text = when(rollFilterMode) {
            is RollFilterMode.Active -> context.resources.getString(R.string.ActiveRolls)
            is RollFilterMode.Archived -> context.resources.getString(R.string.ArchivedRolls)
            is RollFilterMode.All -> context.resources.getString(R.string.AllRolls)
            is RollFilterMode.Favorites -> context.resources.getString(R.string.Favorites)
            is RollFilterMode.HasLabel -> rollFilterMode.label.name
        }
        mToolbarSubtitle.value = text
        viewModelScope.launch { loadRolls() }
    }

    fun setRollSortMode(rollSortMode: RollSortMode) {
        mRollSortMode.value = rollSortMode
        rollList = rollList.sorted(rollSortMode)
        mRolls.value = State.Success(rollList)
    }

    fun loadAll() {
        viewModelScope.launch {
            loadCameras()
            loadRolls()
            loadRollCounts()
            loadLabels()
        }
    }

    fun addCamera(camera: Camera) {
        cameraRepository.addCamera(camera)
        mCameras.value = mCameras.value
            .filterNot { it.id == camera.id }
            .plus(camera)
            .sorted()
    }

    fun submitRoll(roll: Roll) {
        if (rollRepository.updateRoll(roll) == 0) {
            rollRepository.addRoll(roll)
        }
        val filterMode = mRollFilterMode.value
        val removeRollFromList = when {
            filterMode == RollFilterMode.Active && roll.archived -> true
            filterMode == RollFilterMode.Archived && !roll.archived -> true
            filterMode == RollFilterMode.Favorites && !roll.favorite -> true
            filterMode is RollFilterMode.HasLabel -> {
                !roll.labels.any { filterMode.label.id == it.id }
            }
            else -> false
        }
        if (removeRollFromList) {
            rollList = rollList.filterNot { it.id == roll.id }
            mRolls.value = State.Success(rollList)
        } else {
            replaceRoll(roll)
        }
        viewModelScope.launch {
            loadRollCounts()
            loadLabels()
        }
    }

    fun deleteRoll(roll: Roll) {
        rollRepository.deleteRoll(roll)
        rollList = rollList.filterNot { it.id == roll.id }
        mRolls.value = State.Success(rollList)
        viewModelScope.launch {
            loadRollCounts()
            loadLabels()
        }
    }

    private fun replaceRoll(roll: Roll) {
        val sortMode = mRollSortMode.value
        rollList = rollList.filterNot { it.id == roll.id }.plus(roll).sorted(sortMode)
        mRolls.value = State.Success(rollList)
    }

    private suspend fun loadCameras() {
        withContext(Dispatchers.IO) {
            mCameras.value = cameraRepository.cameras
        }
    }

    private suspend fun loadRolls() {
        withContext(Dispatchers.IO) {
            mRolls.value = State.InProgress()
            val filterMode = rollFilterMode.value
            val sortMode = rollSortMode.value
            rollList = rollRepository.getRolls(filterMode).sorted(sortMode)
            mRolls.value = State.Success(rollList)
        }
    }

    private suspend fun loadLabels() {
        withContext(Dispatchers.IO) {
            mLabels.value = labelRepository.labels
        }
    }

    private suspend fun loadRollCounts() {
        withContext(Dispatchers.IO) {
            mRollCounts.value = rollRepository.rollCounts
        }
    }
}