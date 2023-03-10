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
import com.tommihirvonen.exifnotes.utilities.Geocoder
import com.tommihirvonen.exifnotes.utilities.decimalString

class LocationPickViewModel(private val application: Application,
                            location: LatLng?,
                            var formattedAddress: String?) : AndroidViewModel(application) {

    val location: LiveData<LocationData> get() = mLocation

    private val mLocation: MutableLiveData<LocationData> = MutableLiveData(LocationData(location, Animate.MOVE))

    val observable = Observable(formattedAddress ?: "")

    suspend fun setLocation(latLng: LatLng, animate: Animate = Animate.NONE) {
        observable.progressBarVisibility = View.VISIBLE
        observable.addressText = ""

        mLocation.value = LocationData(latLng, animate)

        val (_, addressResult) = Geocoder(application.applicationContext).getData(latLng.decimalString)
        formattedAddress = if (addressResult.isNotEmpty()) {
            observable.addressText = addressResult
            addressResult
        } else {
            observable.addressText = application.resources.getString(R.string.AddressNotFound)
            null
        }

        observable.progressBarVisibility = View.INVISIBLE
    }

    suspend fun submitQuery(query: String) {
        observable.progressBarVisibility = View.VISIBLE
        observable.addressText = ""
        val (position, addressResult) = Geocoder(application.applicationContext).getData(query)
        if (position != null) {
            formattedAddress = addressResult
            observable.addressText = addressResult
            mLocation.value = LocationData(position, Animate.ANIMATE)
        } else {
            formattedAddress = null
            observable.addressText = application.resources.getString(R.string.AddressNotFound)
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
                                   val location: LatLng?,
                                   private val formattedAddress: String?)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(LocationPickViewModel::class.java)) {
            return LocationPickViewModel(application, location, formattedAddress) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class LocationData(val location: LatLng?, val animate: Animate)

enum class Animate {
    NONE, MOVE, ANIMATE
}