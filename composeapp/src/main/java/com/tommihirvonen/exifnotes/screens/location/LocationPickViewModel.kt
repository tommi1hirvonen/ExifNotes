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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.di.geocoder.GeocoderRequestBuilder
import com.tommihirvonen.exifnotes.di.geocoder.GeocoderResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = LocationPickViewModel.Factory::class)
class LocationPickViewModel @AssistedInject constructor(
    @Assisted frame: Frame,
    private val application: Application,
    private val geocoderRequestBuilder: GeocoderRequestBuilder
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(frame: Frame): LocationPickViewModel
    }

    private val _countries = listOf(
        "Finland", "Sweden", "Norway", "Denmark", "Iceland"
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _location = MutableStateFlow(frame.location)
    val location = _location.asStateFlow()

    private val _address = MutableStateFlow(frame.formattedAddress)
    val address = _address.asStateFlow()

    private val _expanded = MutableStateFlow(false)
    val expanded = _expanded.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText = _errorText.asStateFlow()

    private val _countriesList = MutableStateFlow(_countries)
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

    suspend fun setLocation(latLng: LatLng) {
        _isLoading.value = true
        _address.value = null
        _location.value = latLng
        val result = geocoderRequestBuilder.fromLatLng(latLng).getResponse()
        val (formattedAddress, errorText) =
            when (result) {
                is GeocoderResponse.Success -> {
                    result.formattedAddress to null
                }
                is GeocoderResponse.NotFound -> {
                    null to application.resources.getString(R.string.AddressNotFound)
                }
                is GeocoderResponse.Timeout -> {
                    null to application.resources.getString(R.string.TimeoutGettingAddress)
                }
                is GeocoderResponse.Error -> {
                    null to application.resources.getString(R.string.ErrorGettingAddress)
                }
            }
        _address.value = formattedAddress
        _errorText.value = errorText
        _isLoading.value = false
    }

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