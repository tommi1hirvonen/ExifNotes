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

package com.tommihirvonen.exifnotes.screens.gear.filmstocks

import androidx.lifecycle.ViewModel
import com.tommihirvonen.exifnotes.core.entities.FilmProcess
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.FilmType
import com.tommihirvonen.exifnotes.data.repositories.FilmStockRepository
import com.tommihirvonen.exifnotes.util.validate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel(assistedFactory = FilmStockViewModel.Factory::class)
class FilmStockViewModel @AssistedInject constructor (
    @Assisted filmStockId: Long,
    filmStockRepository: FilmStockRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(filmStockId: Long): FilmStockViewModel
    }

    val filmStock = filmStockRepository.getFilmStock(filmStockId) ?: FilmStock()

    private val _make = MutableStateFlow(filmStock.make)
    private val _model = MutableStateFlow(filmStock.model)
    private val _iso = MutableStateFlow(filmStock.iso)
    private val _type = MutableStateFlow(filmStock.type)
    private val _process = MutableStateFlow(filmStock.process)
    private val _makeError = MutableStateFlow(false)
    private val _modelError = MutableStateFlow(false)

    val make = _make.asStateFlow()
    val model = _model.asStateFlow()
    val iso = _iso.asStateFlow()
    val type = _type.asStateFlow()
    val process = _process.asStateFlow()
    val makeError = _makeError.asStateFlow()
    val modelError = _modelError.asStateFlow()

    fun setMake(value: String) {
        filmStock.make = value
        _make.value = value
        _makeError.value = false
    }

    fun setModel(value: String) {
        filmStock.model = value
        _model.value = value
        _modelError.value = false
    }

    fun setIso(value: String) {
        val v = value.toIntOrNull() ?: return
        if (v in 0..1_000_000) {
            filmStock.iso = v
            _iso.value = v
        }
    }

    fun setType(value: FilmType) {
        filmStock.type = value
        _type.value = value
    }

    fun setProcess(value: FilmProcess) {
        filmStock.process = value
        _process.value = value
    }

    fun validate(): Boolean {
        val makeValidation = { f: FilmStock ->
            if (f.make?.isNotEmpty() == true) {
                true
            } else {
                _makeError.value = true
                false
            }
        }
        val modelValidation = { f: FilmStock ->
            if (f.model?.isNotEmpty() == true) {
                true
            } else {
                _modelError.value = true
                false
            }
        }
        return filmStock.validate(makeValidation, modelValidation)
    }
}