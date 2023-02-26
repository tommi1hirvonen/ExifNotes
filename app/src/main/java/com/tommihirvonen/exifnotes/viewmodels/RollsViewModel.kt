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
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RollsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = application.database

    private val sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(application.baseContext)

    val cameras: LiveData<List<Camera>> get() = mCameras
    val rolls: LiveData<State<List<Roll>>> get() = mRolls
    val rollFilterMode: LiveData<RollFilterMode> get() = mRollFilterMode
    val rollSortMode: LiveData<RollSortMode> get() = mRollSortMode
    val rollCounts: LiveData<Pair<Int, Int>> get() = mRollCounts

    val selectedRolls = HashSet<Roll>()

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

    private val mRollCounts: MutableLiveData<Pair<Int, Int>> by lazy {
        MutableLiveData<Pair<Int, Int>>().also {
            viewModelScope.launch { loadRollCounts() }
        }
    }

    private var rollList = emptyList<Roll>()

    private val mCameras: MutableLiveData<List<Camera>> by lazy {
        MutableLiveData<List<Camera>>().also {
            viewModelScope.launch { loadCameras() }
        }
    }

    fun requestRollCountsUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            loadRollCounts()
        }
    }

    fun setRollFilterMode(rollFilterMode: RollFilterMode) {
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_VISIBLE_ROLLS, rollFilterMode.value)
        editor.apply()
        mRollFilterMode.value = rollFilterMode
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
        }
    }

    fun addCamera(camera: Camera) {
        database.addCamera(camera)
        mCameras.value = mCameras.value?.filterNot { it.id == camera.id }?.plus(camera)?.sorted()
    }

    fun submitRoll(roll: Roll) {
        if (database.updateRoll(roll) == 0) {
            database.addRoll(roll)
        }
        if (mRollFilterMode.value == RollFilterMode.ACTIVE && roll.archived
            || mRollFilterMode.value == RollFilterMode.ARCHIVED && !roll.archived) {
            rollList = rollList.minus(roll)
            mRolls.value = State.Success(rollList)
        } else {
            replaceRoll(roll)
        }
    }

    fun deleteRoll(roll: Roll) {
        database.deleteRoll(roll)
        rollList = rollList.minus(roll)
        mRolls.value = State.Success(rollList)
    }

    private fun replaceRoll(roll: Roll) {
        val sortMode = mRollSortMode.value ?: RollSortMode.DATE
        rollList = rollList.filterNot { it.id == roll.id }.plus(roll).sorted(sortMode)
        mRolls.value = State.Success(rollList)
    }

    private suspend fun loadCameras() {
        withContext(Dispatchers.IO) {
            mCameras.postValue(database.cameras)
        }
    }

    private suspend fun loadRolls() {
        withContext(Dispatchers.IO) {
            mRolls.postValue(State.InProgress())
            val filterMode = rollFilterMode.value ?: RollFilterMode.ACTIVE
            val sortMode = rollSortMode.value ?: RollSortMode.DATE
            rollList = database.getRolls(filterMode).sorted(sortMode)
            mRolls.postValue(State.Success(rollList))
        }
    }

    private suspend fun loadRollCounts() {
        withContext(Dispatchers.IO) {
            mRollCounts.postValue(database.rollCounts)
        }
    }
}