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
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tommihirvonen.exifnotes.BR
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.utilities.validate

class FilterEditViewModel(application: Application, val filter: Filter)
    : AndroidViewModel(application) {

    val observable = Observable()

    val context get() = getApplication<Application>()

    fun validate(): Boolean {
        val makeValidation = { f: Filter ->
            if (f.make?.isNotEmpty() == true) {
                true
            } else {
                observable.makeError = context.getString(R.string.Required)
                false
            }
        }
        val modelValidation = { f: Filter ->
            if (f.model?.isNotEmpty() == true) {
                true
            } else {
                observable.modelError = context.getString(R.string.Required)
                false
            }
        }
        return filter.validate(makeValidation, modelValidation)
    }

    inner class Observable : BaseObservable() {
        @Bindable
        fun getMake() = filter.make

        fun setMake(value: String?) {
            if (value != filter.make) {
                filter.make = value
                notifyPropertyChanged(BR.make)
            }
        }

        @Bindable
        fun getModel() = filter.model

        fun setModel(value: String?) {
            if (value != filter.model) {
                filter.model = value
                notifyPropertyChanged(BR.make)
            }
        }

        @get:Bindable
        var makeError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.makeError)
            }

        @get:Bindable
        var modelError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.modelError)
            }
    }
}

class FilterEditViewModelFactory(private val application: Application, private val filter: Filter)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(FilterEditViewModel::class.java)) {
            return FilterEditViewModel(application, filter) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}