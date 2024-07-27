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

package com.tommihirvonen.exifnotes.screens.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LocationPickViewModel : ViewModel() {

    private val countries = listOf(
        "Finland", "Sweden", "Norway", "Denmark", "Iceland"
    )

    private val _expanded = MutableStateFlow(false)
    val expanded = _expanded.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _countriesList = MutableStateFlow(countries)
    val countriesList = searchText
        .combine(_countriesList) { text, countries ->
            if (text.isBlank()) {
                countries
            } else {
                countries.filter { country ->
                    country.uppercase().contains(text.trim().uppercase())
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _countriesList.value
        )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun onToggleExpanded() {
        _expanded.value = !_expanded.value
        if (!_expanded.value) {
            onSearchTextChange("")
        }
    }
}