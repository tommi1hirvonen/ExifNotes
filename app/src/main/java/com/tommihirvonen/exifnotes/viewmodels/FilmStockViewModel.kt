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
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.FilmStockFilterMode
import com.tommihirvonen.exifnotes.datastructures.FilmStockSortMode
import com.tommihirvonen.exifnotes.datastructures.sorted
import com.tommihirvonen.exifnotes.utilities.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FilmStockViewModel(application: Application) : AndroidViewModel(application) {

    val filmStocks get() = filmStocksData as LiveData<List<FilmStock>>
    val sortMode get() = _sortMode as LiveData<FilmStockSortMode>

    var filterSet = FilmStockFilterSet()
    set(value) {
        field = value
        applyFilters()
    }

    private val database = application.database

    private val filmStocksData: MutableLiveData<List<FilmStock>> by lazy {
        MutableLiveData<List<FilmStock>>().apply {
            viewModelScope.launch(Dispatchers.IO) {
                allFilmStocks = database.filmStocks
                filteredFilmStocks = allFilmStocks
                postValue(filteredFilmStocks)
            }
        }
    }

    private val _sortMode = MutableLiveData<FilmStockSortMode>().apply {
        value = FilmStockSortMode.NAME
    }

    private var allFilmStocks = emptyList<FilmStock>()
    private var filteredFilmStocks = emptyList<FilmStock>()

    fun setSortMode(sortMode: FilmStockSortMode) {
        _sortMode.value = sortMode
        filteredFilmStocks = filteredFilmStocks.sorted(sortMode)
        filmStocksData.value = filteredFilmStocks
    }

    fun addFilmStock(filmStock: FilmStock) {
        database.addFilmStock(filmStock)
        replaceFilmStock(filmStock)
    }

    fun updateFilmStock(filmStock: FilmStock): Int {
        val rows = database.updateFilmStock(filmStock)
        val sortMode = _sortMode.value ?: FilmStockSortMode.NAME
        filteredFilmStocks = filteredFilmStocks.sorted(sortMode)
        filmStocksData.value = filteredFilmStocks
        return rows
    }

    fun deleteFilmStock(filmStock: FilmStock) {
        database.deleteFilmStock(filmStock)
        allFilmStocks = allFilmStocks.minus(filmStock)
        filteredFilmStocks = filteredFilmStocks.minus(filmStock)
        filmStocksData.value = filteredFilmStocks
    }

    private fun replaceFilmStock(filmStock: FilmStock) {
        val sortMode = _sortMode.value ?: FilmStockSortMode.NAME
        filteredFilmStocks = filteredFilmStocks
            .filterNot { it.id == filmStock.id }
            .plus(filmStock)
            .sorted(sortMode)
        filmStocksData.value = filteredFilmStocks
    }

    private fun applyFilters() {
        filteredFilmStocks = allFilmStocks
            .applyPredicates(manufacturerFilter, typeFilter, processFilter,
                isoFilter, addedByFilter)
            .sorted(_sortMode.value ?: FilmStockSortMode.NAME)
        filmStocksData.value = filteredFilmStocks
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

fun <T> List<T>.isEmptyOrContains(value: T): Boolean = contains(value) || isEmpty()

fun <T> List<T>.applyPredicates(vararg predicates: ((T) -> (Boolean))): List<T> =
    filter { item -> predicates.all { p -> p(item) } }

fun <T, U : Comparable<U>> List<T>.mapDistinct(transform: (T) -> U): List<U> =
    map(transform).distinct().sorted()

data class FilmStockFilterSet(
    val filterMode: FilmStockFilterMode = FilmStockFilterMode.ALL,
    val manufacturers: List<String> = emptyList(),
    val isoValues: List<Int> = emptyList(),
    val types: List<Int> = emptyList(),
    val processes: List<Int> = emptyList()
)