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

import android.content.Context
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.tommihirvonen.exifnotes.BR
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.utilities.validate

class FilterEditViewModel(val filter: Filter) : BaseObservable() {

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

    @Bindable
    var makeError: String? = null
    set(value) {
        field = value?.ifEmpty { null }
        notifyPropertyChanged(BR.makeError)
    }

    @Bindable
    var modelError: String? = null
    set(value) {
        field = value?.ifEmpty { null }
        notifyPropertyChanged(BR.modelError)
    }

    fun validate(context: Context): Boolean {
        val makeValidation = { f: Filter ->
            if (f.make?.isNotEmpty() == true) {
                true
            } else {
                makeError = context.getString(R.string.MakeIsRequired)
                false
            }
        }
        val modelValidation = { f: Filter ->
            if (f.model?.isNotEmpty() == true) {
                true
            } else {
                modelError = context.getString(R.string.ModelIsRequired)
                false
            }
        }
        return filter.validate(makeValidation, modelValidation)
    }
}