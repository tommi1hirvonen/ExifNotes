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
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.core.entities.RollSortMode
import com.tommihirvonen.exifnotes.core.entities.sorted
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.data.repositories.RollCounts
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RollsViewModel @Inject constructor(application: Application,
                                         private val rollRepository: RollRepository,
                                         private val cameraRepository: CameraRepository)
    : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(context)

    val cameras: LiveData<List<Camera>> get() = mCameras
    val rolls: LiveData<State<List<Roll>>> get() = mRolls
    val rollFilterMode: LiveData<RollFilterMode> get() = mRollFilterMode
    val rollSortMode: LiveData<RollSortMode> get() = mRollSortMode
    val rollCounts: LiveData<RollCounts> get() = mRollCounts
    val toolbarSubtitle: LiveData<String> get() = mToolbarSubtitle

    val selectedRolls = HashSet<Roll>()

    var gearRefreshPending = false

    private val mToolbarSubtitle: MutableLiveData<String> by lazy {
        val subtitleResId = when(rollFilterMode.value) {
            RollFilterMode.ACTIVE -> R.string.ActiveRolls
            RollFilterMode.ARCHIVED -> R.string.ArchivedRolls
            RollFilterMode.ALL -> R.string.AllRolls
            RollFilterMode.FAVORITES -> R.string.Favorites
            null -> R.string.ActiveRolls
        }
        val subtitle = context.resources.getString(subtitleResId)
        MutableLiveData<String>(subtitle)
    }

    private val mRollFilterMode: MutableLiveData<RollFilterMode> by lazy {
        MutableLiveData<RollFilterMode>().apply {
            value = RollFilterMode.fromValue(
                sharedPreferences.getInt(
                    PreferenceConstants.KEY_VISIBLE_ROLLS, RollFilterMode.ACTIVE.value))
        }
    }

    private val mRollSortMode: MutableLiveData<RollSortMode> by lazy {
        MutableLiveData<RollSortMode>().apply {
            value = RollSortMode.fromValue(
                sharedPreferences.getInt(
                    PreferenceConstants.KEY_ROLL_SORT_ORDER, RollSortMode.DATE.value))
        }
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

    fun setRollFilterMode(rollFilterMode: RollFilterMode) {
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_VISIBLE_ROLLS, rollFilterMode.value)
        editor.apply()
        mRollFilterMode.value = rollFilterMode
        val subtitleResId = when(rollFilterMode) {
            RollFilterMode.ACTIVE -> R.string.ActiveRolls
            RollFilterMode.ARCHIVED -> R.string.ArchivedRolls
            RollFilterMode.ALL -> R.string.AllRolls
            RollFilterMode.FAVORITES -> R.string.Favorites
        }
        mToolbarSubtitle.value = context.resources.getString(subtitleResId)
        viewModelScope.launch { loadRolls() }
    }

    fun setRollSortMode(rollSortMode: RollSortMode) {
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_ROLL_SORT_ORDER, rollSortMode.value)
        editor.apply()
        mRollSortMode.value = rollSortMode
        rollList = rollList.sorted(rollSortMode)
        mRolls.value = State.Success(rollList)
    }

    fun loadAll() {
        viewModelScope.launch {
            loadCameras()
            loadRolls()
            loadRollCounts()
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
        if (mRollFilterMode.value == RollFilterMode.ACTIVE && roll.archived
            || mRollFilterMode.value == RollFilterMode.ARCHIVED && !roll.archived
            || mRollFilterMode.value == RollFilterMode.FAVORITES && !roll.favorite) {
            rollList = rollList.minus(roll)
            mRolls.value = State.Success(rollList)
        } else {
            replaceRoll(roll)
        }
        viewModelScope.launch { loadRollCounts() }
    }

    fun deleteRoll(roll: Roll) {
        rollRepository.deleteRoll(roll)
        rollList = rollList.minus(roll)
        mRolls.value = State.Success(rollList)
        viewModelScope.launch { loadRollCounts() }
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
            val filterMode = rollFilterMode.value ?: RollFilterMode.ACTIVE
            val sortMode = rollSortMode.value ?: RollSortMode.DATE
            rollList = rollRepository.getRolls(filterMode).sorted(sortMode)
            mRolls.postValue(State.Success(rollList))
        }
    }

    private suspend fun loadRollCounts() {
        withContext(Dispatchers.IO) {
            mRollCounts.postValue(rollRepository.rollCounts)
        }
    }
}