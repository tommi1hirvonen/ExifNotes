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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RollsViewModel @Inject constructor(application: Application,
                                         private val rollRepository: RollRepository,
                                         private val cameraRepository: CameraRepository,
                                         private val labelRepository: LabelRepository)
    : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    val cameras: LiveData<List<Camera>> get() = mCameras
    val rolls: LiveData<State<List<Roll>>> get() = mRolls
    val rollFilterMode: LiveData<RollFilterMode> get() = mRollFilterMode
    val rollSortMode: LiveData<RollSortMode> get() = mRollSortMode
    val rollCounts: LiveData<RollCounts> get() = mRollCounts
    val toolbarSubtitle: LiveData<String> get() = mToolbarSubtitle
    val labels: LiveData<List<Label>> get() = mLabels

    val selectedRolls = HashSet<Roll>()

    var gearRefreshPending = false

    private val mToolbarSubtitle: MutableLiveData<String> by lazy {
        val text = when(val filter = rollFilterMode.value) {
            is RollFilterMode.Active -> context.resources.getString(R.string.ActiveRolls)
            is RollFilterMode.Archived -> context.resources.getString(R.string.ArchivedRolls)
            is RollFilterMode.All -> context.resources.getString(R.string.AllRolls)
            is RollFilterMode.Favorites -> context.resources.getString(R.string.Favorites)
            is RollFilterMode.HasLabel -> filter.label.name
            null -> context.resources.getString(R.string.ActiveRolls)
        }
        MutableLiveData<String>(text)
    }

    private val mRollFilterMode = MutableLiveData<RollFilterMode>().apply {
        value = RollFilterMode.Active
    }

    private val mRollSortMode = MutableLiveData<RollSortMode>().apply {
        value = RollSortMode.DATE
    }

    private val mRolls: MutableLiveData<State<List<Roll>>> by lazy {
        MutableLiveData<State<List<Roll>>>().also {
            viewModelScope.launch { loadRolls() }
        }
    }

    private val mRollCounts: MutableLiveData<RollCounts> by lazy {
        MutableLiveData<RollCounts>().also {
            viewModelScope.launch { loadRollCounts() }
        }
    }

    private var rollList = emptyList<Roll>()

    private val mCameras: MutableLiveData<List<Camera>> by lazy {
        MutableLiveData<List<Camera>>().also {
            viewModelScope.launch { loadCameras() }
        }
    }

    private val mLabels: MutableLiveData<List<Label>> by lazy {
        MutableLiveData<List<Label>>().also {
            viewModelScope.launch { loadLabels() }
        }
    }

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
        mCameras.value = mCameras.value?.filterNot { it.id == camera.id }?.plus(camera)?.sorted()
    }

    fun submitRoll(roll: Roll) {
        if (rollRepository.updateRoll(roll) == 0) {
            rollRepository.addRoll(roll)
        }
        if (mRollFilterMode.value == RollFilterMode.Active && roll.archived
            || mRollFilterMode.value == RollFilterMode.Archived && !roll.archived
            || mRollFilterMode.value == RollFilterMode.Favorites && !roll.favorite) {
            rollList = rollList.minus(roll)
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
        rollList = rollList.minus(roll)
        mRolls.value = State.Success(rollList)
        viewModelScope.launch {
            loadRollCounts()
            loadLabels()
        }
    }

    private fun replaceRoll(roll: Roll) {
        val sortMode = mRollSortMode.value ?: RollSortMode.DATE
        rollList = rollList.filterNot { it.id == roll.id }.plus(roll).sorted(sortMode)
        mRolls.value = State.Success(rollList)
    }

    private suspend fun loadCameras() {
        withContext(Dispatchers.IO) {
            mCameras.postValue(cameraRepository.cameras)
        }
    }

    private suspend fun loadRolls() {
        withContext(Dispatchers.IO) {
            mRolls.postValue(State.InProgress())
            val filterMode = rollFilterMode.value ?: RollFilterMode.Active
            val sortMode = rollSortMode.value ?: RollSortMode.DATE
            rollList = rollRepository.getRolls(filterMode).sorted(sortMode)
            mRolls.postValue(State.Success(rollList))
        }
    }

    private suspend fun loadLabels() {
        withContext(Dispatchers.IO) {
            mLabels.postValue(labelRepository.labels)
        }
    }

    private suspend fun loadRollCounts() {
        withContext(Dispatchers.IO) {
            mRollCounts.postValue(rollRepository.rollCounts)
        }
    }
}