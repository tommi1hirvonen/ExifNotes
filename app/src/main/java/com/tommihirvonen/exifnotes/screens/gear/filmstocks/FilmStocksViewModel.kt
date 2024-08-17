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

package com.tommihirvonen.exifnotes.screens.gear.filmstocks;

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tommihirvonen.exifnotes.core.entities.FilmProcess
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.FilmStockFilterMode
import com.tommihirvonen.exifnotes.core.entities.FilmStockSortMode
import com.tommihirvonen.exifnotes.core.entities.FilmType
import com.tommihirvonen.exifnotes.core.entities.sorted
import com.tommihirvonen.exifnotes.data.repositories.FilmStockRepository
import com.tommihirvonen.exifnotes.util.applyPredicates
import com.tommihirvonen.exifnotes.util.isEmptyOrContains
import com.tommihirvonen.exifnotes.util.mapDistinct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilmStocksViewModel @Inject constructor(private val repository: FilmStockRepository) : ViewModel() {

    val filmStocks: StateFlow<List<FilmStock>> get() = _filmStocks
    val sortMode: StateFlow<FilmStockSortMode> get() = _sortMode

    private val _filmStocks = MutableStateFlow(emptyList<FilmStock>())
    private val _sortMode = MutableStateFlow(FilmStockSortMode.NAME)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allFilmStocks = repository.filmStocks
            filteredFilmStocks = allFilmStocks
            _filmStocks.value = filteredFilmStocks
        }
    }

    var filterSet = FilmStockFilterSet()
        set(value) {
            field = value
            applyFilters()
        }

    private var allFilmStocks = emptyList<FilmStock>()
    private var filteredFilmStocks = emptyList<FilmStock>()

    fun setSortMode(sortMode: FilmStockSortMode) {
        _sortMode.value = sortMode
        filteredFilmStocks = filteredFilmStocks.sorted(sortMode)
        _filmStocks.value = filteredFilmStocks
    }

    fun submitFilmStock(filmStock: FilmStock) {
        if (repository.updateFilmStock(filmStock) == 0) {
            repository.addFilmStock(filmStock)
        }
        replaceFilmStock(filmStock)
    }

    fun isFilmStockInUse(filmStock: FilmStock) = repository.isFilmStockBeingUsed(filmStock)

    fun deleteFilmStock(filmStock: FilmStock) {
        repository.deleteFilmStock(filmStock)
        allFilmStocks = allFilmStocks.minus(filmStock)
        filteredFilmStocks = filteredFilmStocks.minus(filmStock)
        _filmStocks.value = filteredFilmStocks
    }

    private fun replaceFilmStock(filmStock: FilmStock) {
        val sortMode = _sortMode.value
        filteredFilmStocks = filteredFilmStocks
            .filterNot { it.id == filmStock.id }
            .plus(filmStock)
            .sorted(sortMode)
        _filmStocks.value = filteredFilmStocks
    }

    private fun applyFilters() {
        filteredFilmStocks = allFilmStocks
            .applyPredicates(manufacturerFilter, typeFilter, processFilter,
                isoFilter, addedByFilter)
            .sorted(_sortMode.value)
        _filmStocks.value = filteredFilmStocks
    }

    // Apply all filters except for the iso filter.
    val filteredIsoValues get() = allFilmStocks
        .applyPredicates(manufacturerFilter, typeFilter, processFilter, addedByFilter)
        .mapDistinct { it.iso }

    // Apply all filters except for the manufacturer filter.
    val filteredManufacturers get() = allFilmStocks
        .applyPredicates(typeFilter, processFilter, isoFilter, addedByFilter)
        .mapDistinct { it.make ?: "" }

    private val manufacturerFilter = { fs: FilmStock ->
        filterSet.manufacturers.isEmptyOrContains(fs.make)
    }

    private val typeFilter = { fs: FilmStock ->
        filterSet.types.isEmptyOrContains(fs.type)
    }

    private val processFilter = { fs: FilmStock ->
        filterSet.processes.isEmptyOrContains(fs.process)
    }

    private val isoFilter = { fs: FilmStock ->
        filterSet.isoValues.isEmptyOrContains(fs.iso)
    }

    private val addedByFilter = { fs: FilmStock ->
        when (filterSet.filterMode) {
            FilmStockFilterMode.ALL -> true
            FilmStockFilterMode.PREADDED -> fs.isPreadded
            FilmStockFilterMode.USER_ADDED -> !fs.isPreadded
        }
    }
}

data class FilmStockFilterSet(
    val filterMode: FilmStockFilterMode = FilmStockFilterMode.ALL,
    val manufacturers: List<String> = emptyList(),
    val isoValues: List<Int> = emptyList(),
    val types: List<FilmType> = emptyList(),
    val processes: List<FilmProcess> = emptyList()
)