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

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.CameraPositionState
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.di.geocoder.GeocoderRequestBuilder
import com.tommihirvonen.exifnotes.di.geocoder.GeocoderResponse
import com.tommihirvonen.exifnotes.di.location.LocationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = LocationPickViewModel.Factory::class)
class LocationPickViewModel @AssistedInject constructor(
    @Assisted frame: Frame,
    private val application: Application,
    private val geocoderRequestBuilder: GeocoderRequestBuilder,
    private val locationService: LocationService
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(frame: Frame): LocationPickViewModel
    }

    val myLocationEnabled: Boolean

    init {
        val fineLocationPermissions = ActivityCompat.checkSelfPermission(
            application.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            application.applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        myLocationEnabled = fineLocationPermissions == PackageManager.PERMISSION_GRANTED
                || coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private val suggestionsHandler = Handler(Looper.getMainLooper())
    private val suggestionsSessionToken by lazy { AutocompleteSessionToken.newInstance() }
    private val placesClient by lazy { Places.createClient(application.applicationContext) }

    private val _isLoadingAddress = MutableStateFlow(false)
    val isLoadingAddress = _isLoadingAddress.asStateFlow()

    private val _location = MutableStateFlow(frame.location)
    val location = _location.asStateFlow()

    private val _address = MutableStateFlow(frame.formattedAddress)
    val address = _address.asStateFlow()

    private val _searchExpanded = MutableStateFlow(false)
    val searchExpanded = _searchExpanded.asStateFlow()

    private val _isQueryingSuggestions = MutableStateFlow(false)
    val isQueryingSuggestions = _isQueryingSuggestions.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText = _errorText.asStateFlow()

    private val _suggestions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private fun updateSuggestions(query: String, cameraPositionState: CameraPositionState?) {
        suggestionsHandler.removeCallbacksAndMessages(null)
        if (query.isBlank()) {
            return
        }
        _isQueryingSuggestions.update { true }
        suggestionsHandler.postDelayed({
            val bounds = cameraPositionState?.projection?.visibleRegion?.latLngBounds
            val builder = FindAutocompletePredictionsRequest
                .builder()
                .setSessionToken(suggestionsSessionToken)
                .setQuery(query)
            if (bounds != null) {
                val bias = RectangularBounds.newInstance(bounds)
                builder.locationBias = bias
            }
            val request = builder.build()
            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                _isQueryingSuggestions.update { false }
                _suggestions.update { response.autocompletePredictions }
            }.addOnFailureListener { exception ->
                _isQueryingSuggestions.update { false }
                if (exception is ApiException) {
                    Toast.makeText(
                        application.applicationContext,
                        "Place not found: ${exception.statusCode}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, 350)
    }

    fun onSearchTextChange(text: String, cameraPositionState: CameraPositionState?) {
        _searchText.value = text
        updateSuggestions(text, cameraPositionState)
    }

    fun onToggleExpanded() {
        _searchExpanded.value = !_searchExpanded.value
        if (!_searchExpanded.value) {
            onSearchTextChange("", null)
        }
    }

    fun setMyLocation(cameraPositionState: CameraPositionState) {
        val location = locationService.lastLocation
        val latLng = location?.let { LatLng(it.latitude, it.longitude) }
        if (latLng != null) {
            viewModelScope.launch { setLocation(latLng) }
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
            cameraPositionState.move(cameraUpdate)
        }
    }

    suspend fun setLocation(latLng: LatLng) {
        _isLoadingAddress.value = true
        _address.value = null
        _location.value = latLng
        _errorText.value = null
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
        _isLoadingAddress.value = false
    }

    suspend fun submitPlaceId(placeId: String, cameraPositionState: CameraPositionState) {
        _isLoadingAddress.value = true
        _address.value = null
        _errorText.value = null
        when (val result = geocoderRequestBuilder.fromPlaceId(placeId).getResponse()) {
            is GeocoderResponse.Success -> {
                _address.value = result.formattedAddress
                _location.value = result.location
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(result.location, 12f)
                cameraPositionState.move(cameraUpdate)
            }
            is GeocoderResponse.NotFound -> {
                _errorText.value = application.resources.getString(R.string.AddressNotFound)
            }
            is GeocoderResponse.Timeout -> {
                _errorText.value = application.resources.getString(R.string.TimeoutGettingAddress)
            }
            is GeocoderResponse.Error -> {
                _errorText.value = application.resources.getString(R.string.ErrorGettingAddress)
            }
        }
        _isLoadingAddress.value = false
    }

    suspend fun submitQuery(query: String, cameraPositionState: CameraPositionState) {
        _isLoadingAddress.value = true
        _address.value = null
        _errorText.value = null
        when (val result = geocoderRequestBuilder.fromQuery(query).getResponse()) {
            is GeocoderResponse.Success -> {
                _address.value = result.formattedAddress
                _location.value = result.location
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(result.location, 12f)
                cameraPositionState.move(cameraUpdate)
            }
            is GeocoderResponse.NotFound -> {
                _errorText.value = application.resources.getString(R.string.AddressNotFound)
            }
            is GeocoderResponse.Timeout -> {
                _errorText.value = application.resources.getString(R.string.TimeoutGettingAddress)
            }
            is GeocoderResponse.Error -> {
                _errorText.value = application.resources.getString(R.string.ErrorGettingAddress)
            }
        }
        _isLoadingAddress.value = false
    }
}