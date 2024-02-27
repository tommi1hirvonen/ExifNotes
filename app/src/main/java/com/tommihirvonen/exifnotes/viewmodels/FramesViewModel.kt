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
    val frames: StateFlow<State<List<Frame>>> get() = mFrames
    val frameSortMode: StateFlow<FrameSortMode> get() = mFrameSortMode

    private val mRoll = MutableStateFlow(roll)
    private val mFrames = MutableStateFlow<State<List<Frame>>>(State.InProgress())
    private val mFrameSortMode = MutableStateFlow(
        FrameSortMode.fromValue(
            sharedPreferences.getInt(
                PreferenceConstants.KEY_FRAME_SORT_ORDER, FrameSortMode.FRAME_COUNT.value)))

    init {
        loadFrames()
    }

    val selectedFrames = HashSet<Frame>()

    private var framesList = emptyList<Frame>()

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
        framesList = framesList.sorted(getApplication(), mode)
        mFrames.value = State.Success(framesList)
    }

    fun submitFrame(frame: Frame) {
        if (repository.updateFrame(frame) == 0) {
            repository.addFrame(frame)
        }
        val sortMode = mFrameSortMode.value
        framesList = framesList
            .filterNot { it.id == frame.id }
            .plus(frame)
            .sorted(getApplication(), sortMode)
        mFrames.value = State.Success(framesList)
    }

    fun deleteFrame(frame: Frame) {
        repository.deleteFrame(frame)
        framesList = framesList.filterNot { it.id == frame.id }
        mFrames.value = State.Success(framesList)
    }

    private fun loadFrames() {
        val sortMode = mFrameSortMode.value
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                framesList = repository.getFrames(roll.value).sorted(getApplication(), sortMode)
                mFrames.value = State.Success(framesList)
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