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

package com.tommihirvonen.exifnotes.screens.frames

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.FrameSortMode
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.sorted
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import com.tommihirvonen.exifnotes.di.pictures.ComplementaryPicturesManager
import com.tommihirvonen.exifnotes.util.LoadState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = FramesViewModel.Factory::class)
class FramesViewModel @AssistedInject constructor(
    @Assisted private val rollId: Long,
    private val rollRepository: RollRepository,
    private val frameRepository: FrameRepository,
    private val complementaryPicturesManager: ComplementaryPicturesManager,
    application: Application
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(rollId: Long): FramesViewModel
    }

    companion object {
        const val KEY_FRAME_SORT_ORDER = "FrameSortOrder"
    }

    private val sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(application.applicationContext)

    val roll get() = _roll.asStateFlow()
    val frames get() = _frames.asStateFlow()
    val selectedFrames get() = _selectedFrames.asStateFlow()
    val frameSortMode get() = _frameSortMode.asStateFlow()

    private val _roll = MutableStateFlow(Roll())
    private val _frames = MutableStateFlow<LoadState<List<Frame>>>(LoadState.InProgress())
    private val _selectedFrames = MutableStateFlow(hashSetOf<Frame>())
    private val _frameSortMode = MutableStateFlow(
        FrameSortMode.fromValue(
            sharedPreferences.getInt(KEY_FRAME_SORT_ORDER, FrameSortMode.FRAME_COUNT.value)
        )
    )

    private var framesList = emptyList<Frame>()

    init {
        loadFrames()
    }

    fun setRoll(roll: Roll) {
        _roll.value = roll
        for (frame in framesList) {
            frame.roll = roll
        }
    }

    fun toggleFrameSelection(frame: Frame) {
        val frames = _selectedFrames.value.toHashSet()
        if (frames.contains(frame)) {
            frames.remove(frame)
        } else {
            frames.add(frame)
        }
        _selectedFrames.value = frames
    }

    fun toggleFrameSelectionAll() {
        _selectedFrames.value = framesList.toHashSet()
    }

    fun toggleFrameSelectionNone() {
        _selectedFrames.value = hashSetOf()
    }

    private fun loadFrames() {
        val sortMode = _frameSortMode.value
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _roll.value = rollRepository.getRoll(rollId) ?: Roll()
                framesList = frameRepository
                    .getFrames(roll.value)
                    .sorted(getApplication(), sortMode)
                // For frames which have a complementary picture,
                // check whether the picture file actually exists.
                // Then we can display an appropriate icon in the UI.
                for (frame in framesList) {
                    val pictureFilename = frame.pictureFilename ?: continue
                    frame.pictureFileExists = complementaryPicturesManager
                        .getPictureFile(pictureFilename)
                        .exists()
                }
                _frames.value = LoadState.Success(framesList)
            }
        }
    }
}