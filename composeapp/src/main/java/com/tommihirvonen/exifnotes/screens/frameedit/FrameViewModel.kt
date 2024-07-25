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

package com.tommihirvonen.exifnotes.screens.frameedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

@HiltViewModel(assistedFactory = FrameViewModel.Factory::class )
class FrameViewModel @AssistedInject constructor(
    @Assisted frameId: Long,
    application: Application,
    frameRepository: FrameRepository
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(frameId: Long): FrameViewModel
    }

    private val _frame = MutableStateFlow(
        frameRepository.getFrame(frameId) ?: Frame()
    )

    val frame = _frame.asStateFlow()

    fun setCount(value: Int) {
        _frame.value = _frame.value.copy(count = value)
    }

    fun setDate(value: LocalDateTime) {
        _frame.value = _frame.value.copy(date = value)
    }

    fun validate(): Boolean = true

}