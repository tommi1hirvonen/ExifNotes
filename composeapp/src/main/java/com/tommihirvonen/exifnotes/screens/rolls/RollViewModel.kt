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
    val roll = rollRepository.getRoll(rollId) ?: Roll()

    private val _cameras = MutableStateFlow(cameraRepository.cameras)
    private val _name = MutableStateFlow(roll.name)
    private val _filmStock = MutableStateFlow(roll.filmStock)
    private val _camera = MutableStateFlow(roll.camera)
    private val _date = MutableStateFlow(roll.date)
    private val _unloaded = MutableStateFlow(roll.unloaded)
    private val _developed = MutableStateFlow(roll.developed)
    private val _iso = MutableStateFlow(roll.iso)
    private val _pushPull = MutableStateFlow(roll.pushPull)
    private val _format = MutableStateFlow(roll.format)
    private val _note = MutableStateFlow(roll.note)
    private val _nameError = MutableStateFlow(false)

    val cameras = _cameras.asStateFlow()
    val name = _name.asStateFlow()
    val filmStock = _filmStock.asStateFlow()
    val camera = _camera.asStateFlow()
    val date = _date.asStateFlow()
    val unloaded = _unloaded.asStateFlow()
    val developed = _developed.asStateFlow()
    val iso = _iso.asStateFlow()
    val format = _format.asStateFlow()
    val pushPull = _pushPull.asStateFlow()
    val note = _note.asStateFlow()
    val nameError = _nameError.asStateFlow()

    fun setName(value: String) {
        roll.name = value
        _name.value = value
        _nameError.value = false
    }

    fun setFilmStock(filmStock: FilmStock?) {
        roll.filmStock = filmStock
        _filmStock.value = filmStock
        if (filmStock != null && filmStock.iso != 0) {
            setIso(filmStock.iso.toString())
            if (roll.name.isNullOrEmpty()) {
                roll.name = filmStock.name
                _name.value = filmStock.name
            }
        }
    }

    fun setCamera(camera: Camera?) {
        val cameras = _cameras.value
        if (camera != null && cameras.none { it.id == camera.id }) {
            _cameras.value = cameras.plus(camera).sorted()
        }
        roll.camera = camera
        _camera.value = camera
        roll.format = camera?.format ?: roll.format
        _format.value = roll.format
    }

    fun setDate(value: LocalDateTime) {
        roll.date = value
        _date.value = value
    }

    fun setUnloaded(value: LocalDateTime?) {
        roll.unloaded = value
        _unloaded.value = value
    }

    fun setDeveloped(value: LocalDateTime?) {
        roll.developed = value
        _developed.value = value
    }

    fun setIso(value: String) {
        val iso = value.toIntOrNull() ?: 0
        if (iso >= 0) {
            roll.iso = iso
            _iso.value = iso
        }
    }

    fun setPushPull(value: String) {
        val actualValue = if (value == "0") null else value
        roll.pushPull = actualValue
        _pushPull.value = actualValue
    }

    fun setFormat(value: Format) {
        roll.format = value
        _format.value = value
    }

    fun setNote(value: String) {
        roll.note = value.ifEmpty { null }
        _note.value = value.ifEmpty { null }
    }

    fun validate(): Boolean = roll.validate(nameValidation)

    private val nameValidation = { roll: Roll ->
        if (roll.name.isNullOrEmpty()) {
            _nameError.value = true
            false
        } else {
            true
        }
    }
}