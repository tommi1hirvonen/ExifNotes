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

package com.tommihirvonen.exifnotes.viewmodels

import android.app.Application
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tommihirvonen.exifnotes.BR
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.utilities.validate

class LabelEditViewModelFactory(private val application: Application, private val label: Label)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(LabelEditViewModel::class.java)) {
            return LabelEditViewModel(application, label) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LabelEditViewModel(application: Application, val label: Label)
    : AndroidViewModel(application) {

    val observable = Observable()

    val context get() = getApplication<Application>()

    fun validate(): Boolean {
        val nameValidation = { l: Label ->
            if (l.name.isNotEmpty()) {
                true
            } else {
                observable.nameError = context.getString(R.string.Required)
                false
            }
        }
        return label.validate(nameValidation)
    }

    inner class Observable : BaseObservable() {
        @Bindable
        fun getName() = label.name

        fun setName(value: String?) {
            if (value != label.name) {
                label.name = value ?: ""
                notifyPropertyChanged(BR.name)
            }
        }

        @get:Bindable
        var nameError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.nameError)
            }
    }
}