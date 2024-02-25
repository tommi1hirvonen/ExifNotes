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
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.FrameSortMode
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.sorted
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FramesViewModel(application: Application,
                      private val repository: FrameRepository,
                      roll: Roll) : AndroidViewModel(application) {

    private val sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(application.baseContext)

    val roll: StateFlow<Roll> get() = mRoll
    val frames: StateFlow<List<Frame>> get() = mFrames
    val frameSortMode: StateFlow<FrameSortMode> get() = mFrameSortMode

    private val mRoll = MutableStateFlow(roll)
    private val mFrames = MutableStateFlow(emptyList<Frame>())
    private val mFrameSortMode = MutableStateFlow(
        FrameSortMode.fromValue(
            sharedPreferences.getInt(
                PreferenceConstants.KEY_FRAME_SORT_ORDER, FrameSortMode.FRAME_COUNT.value)))

    init {
        loadFrames(roll)
    }

    val selectedFrames = HashSet<Frame>()

    fun toggleFavorite(isFavorite: Boolean) {
        val roll = mRoll.value
        roll.favorite = isFavorite
        mRoll.value = roll
    }

    fun setRoll(roll: Roll) {
        mRoll.value = roll
    }

    fun setFrameSortMode(mode: FrameSortMode) {
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_FRAME_SORT_ORDER, mode.value)
        editor.apply()
        mFrameSortMode.value = mode
        mFrames.value = mFrames.value.sorted(getApplication(), mode)
    }

    fun submitFrame(frame: Frame) {
        if (repository.updateFrame(frame) == 0) {
            repository.addFrame(frame)
        }
        val sortMode = mFrameSortMode.value
        mFrames.value = mFrames.value
            .filterNot { it.id == frame.id }
            .plus(frame)
            .sorted(getApplication(), sortMode)
    }

    fun deleteFrame(frame: Frame) {
        repository.deleteFrame(frame)
        mFrames.value = mFrames.value.filterNot { it.id == frame.id }
    }

    private fun loadFrames(roll: Roll) {
        val sortMode = mFrameSortMode.value
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mFrames.value = repository.getFrames(roll).sorted(getApplication(), sortMode)
            }
        }
    }
}

class FramesViewModelFactory(
    private val application: Application,
    private val repository: FrameRepository,
    private val roll: Roll
)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(FramesViewModel::class.java)) {
            return FramesViewModel(application, repository, roll) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}