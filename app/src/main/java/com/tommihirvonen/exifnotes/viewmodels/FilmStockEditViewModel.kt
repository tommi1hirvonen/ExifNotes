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
import android.text.InputFilter
import android.text.Spanned
import android.widget.AdapterView
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tommihirvonen.exifnotes.BR
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.entities.FilmProcess
import com.tommihirvonen.exifnotes.entities.FilmStock
import com.tommihirvonen.exifnotes.entities.FilmType
import com.tommihirvonen.exifnotes.utilities.validate

class FilmStockEditViewModel(application: Application, val filmStock: FilmStock)
    : AndroidViewModel(application) {

    val context get() = getApplication<Application>()

    val observable = Observable()

    fun validate(): Boolean {
        val makeValidation = { f: FilmStock ->
            if (f.make?.isNotEmpty() == true) {
                true
            } else {
                observable.makeError = context.getString(R.string.Required)
                false
            }
        }
        val modelValidation = { f: FilmStock ->
            if (f.model?.isNotEmpty() == true) {
                true
            } else {
                observable.modelError = context.getString(R.string.Required)
                false
            }
        }
        return filmStock.validate(makeValidation, modelValidation)
    }

    inner class Observable : BaseObservable() {
        @Bindable
        fun getMake() = filmStock.make

        fun setMake(value: String?) {
            if (value != filmStock.make) {
                filmStock.make = value
                notifyPropertyChanged(BR.make)
            }
        }

        @Bindable
        fun getModel() = filmStock.model

        fun setModel(value: String?) {
            if (value != filmStock.model) {
                filmStock.model = value
                notifyPropertyChanged(BR.model)
            }
        }

        @Bindable
        fun getIso() = filmStock.iso.toString()

        fun setIso(value: String) {
            if (value.toIntOrNull() != filmStock.iso) {
                filmStock.iso = value.toIntOrNull() ?: 0
                notifyPropertyChanged(BR.iso)
            }
        }

        val filmType = filmStock.type.description(context)
        val filmProcess = filmStock.process.description(context)

        val filmTypeOnClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                filmStock.type = FilmType.from(position)
            }

        val filmProcessOnClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                filmStock.process = FilmProcess.from(position)
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

        val isoInputFilter = object : InputFilter {
            override fun filter(
                source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int,
                dend: Int
            ): CharSequence? {
                try {
                    val input = (dest.toString() + source.toString()).toInt()
                    if (input in 0..1000000) return null
                } catch (ignored: NumberFormatException) {
                }
                return ""
            }
        }
    }
}

class FilmStockEditViewModelFactory(private val application: Application,
                                    private val filmStock: FilmStock) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(FilmStockEditViewModel::class.java)) {
            return FilmStockEditViewModel(application, filmStock) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}