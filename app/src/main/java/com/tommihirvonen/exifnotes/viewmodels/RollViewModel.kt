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

class RollViewModel(application: Application) : AndroidViewModel(application) {
    private val database = application.database

    private val sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(application.baseContext)

    val rolls: LiveData<List<Roll>> get() = mRolls
    val rollFilterMode: LiveData<RollFilterMode> get() = mRollFilterMode
    val rollSortMode: LiveData<RollSortMode> get() = mRollSortMode

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

    private val mRolls: MutableLiveData<List<Roll>> by lazy {
        MutableLiveData<List<Roll>>().also {
            loadRolls()
        }
    }

    fun setRollFilterMode(rollFilterMode: RollFilterMode) {
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_VISIBLE_ROLLS, rollFilterMode.value)
        editor.commit()
        mRollFilterMode.value = rollFilterMode
        loadRolls()
    }

    fun setRollSortMode(rollSortMode: RollSortMode) {
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_ROLL_SORT_ORDER, rollSortMode.value)
        editor.commit()
        mRollSortMode.value = rollSortMode
        mRolls.value = mRolls.value?.sorted(rollSortMode)
    }

    fun loadGear() {
        // TODO: To be implemented
    }

    fun loadAll() {
        loadGear()
        loadRolls()
    }

    fun addRoll(roll: Roll) {
        database.addRoll(roll)
        replaceRoll(roll)
    }

    fun updateRoll(roll: Roll): Int {
        val rows = database.updateRoll(roll)
        if (mRollFilterMode.value == RollFilterMode.ACTIVE && roll.archived
            || mRollFilterMode.value == RollFilterMode.ARCHIVED && !roll.archived) {
            mRolls.value = mRolls.value?.minus(roll)
        }
        return rows
    }

    fun deleteRoll(roll: Roll) {
        database.deleteRoll(roll)
        mRolls.value = mRolls.value?.minus(roll)
    }

    private fun replaceRoll(roll: Roll) {
        val sortMode = mRollSortMode.value ?: RollSortMode.DATE
        mRolls.value = mRolls.value?.filterNot { it.id == roll.id }?.plus(roll)?.sorted(sortMode)
    }

    private fun loadRolls() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val filterMode = rollFilterMode.value ?: RollFilterMode.ACTIVE
                val sortMode = rollSortMode.value ?: RollSortMode.DATE
                val rolls = database.getRolls(filterMode).sorted(sortMode)
                mRolls.postValue(rolls)
            }
        }
    }
}