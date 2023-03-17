/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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
import android.view.View
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.BR
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.geocoder.GeocoderRequestBuilder
import com.tommihirvonen.exifnotes.geocoder.GeocoderResponse.*

class LocationPickViewModel(private val application: Application,
                            private val geocoderRequestBuilder: GeocoderRequestBuilder,
                            location: LatLng?,
                            var formattedAddress: String?) : AndroidViewModel(application) {

    val location: LiveData<LocationData> get() = mLocation

    private val mLocation: MutableLiveData<LocationData> = MutableLiveData(LocationData(location, Animate.MOVE))

    val observable = Observable(formattedAddress ?: "")

    suspend fun setLocation(latLng: LatLng, animate: Animate = Animate.NONE) {
        observable.progressBarVisibility = View.VISIBLE
        observable.addressText = ""
        mLocation.value = LocationData(latLng, animate)
        val result = geocoderRequestBuilder.fromLatLng(latLng).getResponse()
        val (formattedAddress, showText) =
            when (result) {
                is Success -> {
                    result.formattedAddress to result.formattedAddress
                }
                is NotFound -> {
                    null to application.resources.getString(R.string.AddressNotFound)
                }
                is Timeout -> {
                    null to application.resources.getString(R.string.TimeoutGettingAddress)
                }
                is Error -> {
                    null to application.resources.getString(R.string.ErrorGettingAddress)
                }
            }
        this.formattedAddress = formattedAddress
        observable.addressText = showText
        observable.progressBarVisibility = View.INVISIBLE
    }

    suspend fun submitPlaceId(placeId: String) {
        observable.progressBarVisibility = View.VISIBLE
        observable.addressText = ""

        when (val result = geocoderRequestBuilder.fromPlaceId(placeId).getResponse()) {
            is Success -> {
                formattedAddress = result.formattedAddress
                observable.addressText = result.formattedAddress
                mLocation.value = LocationData(result.location, Animate.ANIMATE)
            }
            is NotFound -> {
                formattedAddress = null
                observable.addressText = application.resources.getString(R.string.AddressNotFound)
            }
            is Timeout -> {
                formattedAddress = null
                observable.addressText = application.resources.getString(R.string.TimeoutGettingAddress)
            }
            is Error -> {
                formattedAddress = null
                observable.addressText = application.resources.getString(R.string.ErrorGettingAddress)
            }
        }
        observable.progressBarVisibility = View.INVISIBLE
    }

    suspend fun submitQuery(query: String) {
        observable.progressBarVisibility = View.VISIBLE
        observable.addressText = ""
        when (val result = geocoderRequestBuilder.fromQuery(query).getResponse()) {
            is Success -> {
                formattedAddress = result.formattedAddress
                observable.addressText = result.formattedAddress
                mLocation.value = LocationData(result.location, Animate.ANIMATE)
            }
            is NotFound -> {
                formattedAddress = null
                observable.addressText = application.resources.getString(R.string.AddressNotFound)
            }
            is Timeout -> {
                formattedAddress = null
                observable.addressText = application.resources.getString(R.string.TimeoutGettingAddress)
            }
            is Error -> {
                formattedAddress = null
                observable.addressText = application.resources.getString(R.string.ErrorGettingAddress)
            }
        }
        observable.progressBarVisibility = View.INVISIBLE
    }

    inner class Observable(addressText: String) : BaseObservable() {
        @get:Bindable
        var progressBarVisibility = View.INVISIBLE
        set(value) {
            field = value
            notifyPropertyChanged(BR.progressBarVisibility)
        }

        @get:Bindable
        var addressText = addressText
            set(value) {
                field = value
                notifyPropertyChanged(BR.addressText)
            }
    }
}

class LocationPickViewModelFactory(private val application: Application,
                                   private val geocoderRequestBuilder: GeocoderRequestBuilder,
                                   val location: LatLng?,
                                   private val formattedAddress: String?)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(LocationPickViewModel::class.java)) {
            return LocationPickViewModel(application, geocoderRequestBuilder, location, formattedAddress) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class LocationData(val location: LatLng?, val animate: Animate)

enum class Animate {
    NONE, MOVE, ANIMATE
}