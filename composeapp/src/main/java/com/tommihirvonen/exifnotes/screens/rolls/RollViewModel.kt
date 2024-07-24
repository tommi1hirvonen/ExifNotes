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

package com.tommihirvonen.exifnotes.screens.rolls

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Format
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import com.tommihirvonen.exifnotes.util.validate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

@HiltViewModel(assistedFactory = RollViewModel.Factory::class)
class RollViewModel @AssistedInject constructor (
    @Assisted rollId: Long,
    application: Application,
    rollRepository: RollRepository,
    cameraRepository: CameraRepository
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(rollId: Long): RollViewModel
    }
    
    val pushPullValues = application.applicationContext.resources
        .getStringArray(R.array.CompValues)
        .toList()

    private val _roll = MutableStateFlow(
        rollRepository.getRoll(rollId) ?: Roll()
    )
    private val _cameras = MutableStateFlow(cameraRepository.cameras)
    private val _nameError = MutableStateFlow(false)

    val roll = _roll.asStateFlow()
    val cameras = _cameras.asStateFlow()
    val nameError = _nameError.asStateFlow()

    fun setName(value: String) {
        _roll.value = _roll.value.copy(name = value)
        _nameError.value = false
    }

    fun setFilmStock(filmStock: FilmStock?) {
        val roll = _roll.value
        val iso = filmStock?.iso ?: roll.iso
        val name = if (roll.name.isNullOrEmpty()) filmStock?.name else roll.name
        _roll.value = roll.copy(
            filmStock = filmStock,
            iso = iso,
            name = name
        )
    }

    fun setCamera(camera: Camera?) {
        val cameras = _cameras.value
        if (camera != null && cameras.none { it.id == camera.id }) {
            _cameras.value = cameras.plus(camera).sorted()
        }
        val roll = _roll.value
        _roll.value = roll.copy(
            camera = camera,
            format = camera?.format ?: roll.format
        )
    }

    fun setDate(value: LocalDateTime) {
        _roll.value = _roll.value.copy(date = value)
    }

    fun setUnloaded(value: LocalDateTime?) {
        _roll.value = _roll.value.copy(unloaded = value)
    }

    fun setDeveloped(value: LocalDateTime?) {
        _roll.value = _roll.value.copy(developed = value)
    }

    fun setIso(value: String) {
        val iso = value.toIntOrNull() ?: 0
        if (iso >= 0) {
            _roll.value = _roll.value.copy(iso = iso)
        }
    }

    fun setPushPull(value: String) {
        val actualValue = if (value == "0") null else value
        _roll.value = _roll.value.copy(pushPull = actualValue)
    }

    fun setFormat(value: Format) {
        _roll.value = _roll.value.copy(format = value)
    }

    fun setNote(value: String) {
        val actualValue = value.ifEmpty { null }
        _roll.value = _roll.value.copy(note = actualValue)
    }

    fun validate(): Boolean = _roll.value.validate(nameValidation)

    private val nameValidation = { roll: Roll ->
        if (roll.name.isNullOrEmpty()) {
            _nameError.value = true
            false
        } else {
            true
        }
    }
}