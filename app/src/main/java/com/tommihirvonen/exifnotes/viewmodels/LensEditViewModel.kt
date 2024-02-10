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
import android.view.View
import android.widget.AdapterView
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.tommihirvonen.exifnotes.BR
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.entities.Increment
import com.tommihirvonen.exifnotes.entities.Lens
import com.tommihirvonen.exifnotes.utilities.validate

class LensEditViewModel(application: Application, val fixedLens: Boolean, val lens: Lens)
    : AndroidViewModel(application) {

    val context get() = getApplication<Application>()

    val observable = Observable()

    fun validate(): Boolean {
        val makeValidation = { l: Lens ->
            if (l.make?.isNotEmpty() == true || fixedLens) {
                true
            } else {
                observable.makeError = context.getString(R.string.Required)
                false
            }
        }
        val modelValidation = { l: Lens ->
            if (l.model?.isNotEmpty() == true || fixedLens) {
                true
            } else {
                observable.modelError = context.getString(R.string.Required)
                false
            }
        }

        val focalLengthValidation = { l: Lens ->
            if (l.minFocalLength > l.maxFocalLength && l.minFocalLength >= 0 && l.maxFocalLength >= 0) {
                observable.minFocalLengthError = context.getString(R.string.MinFocalLengthGreaterThanMax)
                false
            } else if (l.minFocalLength < 0 && l.maxFocalLength < 0) {
                observable.minFocalLengthError = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                observable.maxFocalLengthError = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                false
            } else if (l.minFocalLength < 0) {
                observable.minFocalLengthError = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                false
            } else if (l.maxFocalLength < 0) {
                observable.maxFocalLengthError = context.getString(R.string.FocalLengthValuesZeroOrGreater)
                false
            } else {
                true
            }
        }
        return lens.validate(makeValidation, modelValidation, focalLengthValidation,
            ::apertureRangeValidation)
    }

    private fun apertureRangeValidation(lens: Lens): Boolean {
        // Both ends of the aperture range must be provided.
        if (lens.minAperture == null && lens.maxAperture != null ||
            lens.minAperture != null && lens.maxAperture == null) {
            observable.apertureRangeError = context.getString(R.string.NoMinOrMaxAperture)
            return false
        }
        val min = lens.minAperture?.toDoubleOrNull() ?: 0.0
        val max = lens.maxAperture?.toDoubleOrNull() ?: 0.0
        // Note, that the minimum aperture should actually be smaller in numeric value than the max.
        // Small aperture values mean a large opening and vice versa.
        if (min < max) {
            observable.apertureRangeError = context.getString(R.string.MinApertureGreaterThanMax)
            return false
        }
        return true
    }

    inner class Observable : BaseObservable() {

        // Used to check in XML whether some of the layouts should be hidden for fixed lens editing.
        val interchangeableLensLayoutVisibility = if (fixedLens) View.GONE else View.VISIBLE

        @Bindable
        fun getMake() = lens.make
        fun setMake(value: String?) {
            if (lens.make != value) {
                lens.make = value
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
        fun getModel() = lens.model
        fun setModel(value: String?) {
            if (lens.model != value) {
                lens.model = value
                notifyPropertyChanged(BR.model)
            }
        }

        @Bindable
        var modelError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.modelError)
            }

        @Bindable
        fun getSerialNumber() = lens.serialNumber
        fun setSerialNumber(value: String?) {
            if (lens.serialNumber != value) {
                lens.serialNumber = value
                notifyPropertyChanged(BR.serialNumber)
            }
        }

        @Bindable
        fun getMinAperture() = lens.minAperture
        private fun setMinAperture(value: String?) {
            if (lens.minAperture != value) {
                lens.minAperture = value
                notifyPropertyChanged(BR.minAperture)
                apertureRangeError = null
            }
        }

        @Bindable
        fun getMaxAperture() = lens.maxAperture
        private fun setMaxAperture(value: String?) {
            if (lens.maxAperture != value) {
                lens.maxAperture = value
                notifyPropertyChanged(BR.maxAperture)
                apertureRangeError = null
            }
        }

        @Bindable
        var apertureRangeError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.apertureRangeError)
            }

        @Bindable
        fun getMinFocalLength() =
            if (lens.minFocalLength >= 0) lens.minFocalLength.toString() else ""
        fun setMinFocalLength(value: String) {
            if (lens.minFocalLength != value.toIntOrNull()) {
                lens.minFocalLength = value.toIntOrNull() ?: -1
                notifyPropertyChanged(BR.minFocalLength)
            }
        }

        @Bindable
        fun getMaxFocalLength() =
            if (lens.maxFocalLength >= 0) lens.maxFocalLength.toString() else ""
        fun setMaxFocalLength(value: String) {
            if (lens.maxFocalLength != value.toIntOrNull()) {
                lens.maxFocalLength = value.toIntOrNull() ?: -1
                notifyPropertyChanged(BR.maxFocalLength)
            }
        }

        @Bindable
        var minFocalLengthError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.minFocalLengthError)
            }

        @Bindable
        var maxFocalLengthError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.maxFocalLengthError)
            }

        @get:Bindable
        var apertureValueOptions: Array<String> = apertureValues

        val apertureIncrement: String = context.resources
            .getStringArray(R.array.StopIncrements)[lens.apertureIncrements.ordinal]

        val apertureIncrementOnItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                lens.apertureIncrements = Increment.from(position)
                apertureValueOptions = apertureValues
                notifyPropertyChanged(BR.apertureValueOptions)
                val minFound = apertureValueOptions.contains(lens.minAperture)
                val maxFound = apertureValueOptions.contains(lens.maxAperture)
                // If either one wasn't found in the new values array, null them.
                if (!minFound || !maxFound) {
                    setMinAperture(null)
                    setMaxAperture(null)
                }
            }

        val minApertureOnItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Handle empty item selection (first in list)
            if (position > 0) {
                setMinAperture(apertureValueOptions[position])
            } else {
                setMinAperture(null)
            }
        }

        val maxApertureOnItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Handle empty item selection (first in list)
            if (position > 0) {
                setMaxAperture(apertureValueOptions[position])
            } else {
                setMaxAperture(null)
            }
        }

        // Handle list item preselection and scrolling when opened.
        val minApertureOnClickListener = View.OnClickListener { view ->
            val index = apertureValueOptions.indexOf(lens.minAperture)
            if (index >= 0) (view as MaterialAutoCompleteTextView).listSelection = index
        }

        // Handle list item preselection and scrolling when opened.
        val maxApertureOnClickListener = View.OnClickListener { view ->
            val index = apertureValueOptions.indexOf(lens.maxAperture)
            if (index >= 0) (view as MaterialAutoCompleteTextView).listSelection = index
        }

        fun clearApertureRange() {
            setMinAperture(null)
            setMaxAperture(null)
        }

        private val apertureValues get() = when (lens.apertureIncrements) {
            Increment.THIRD -> context.resources.getStringArray(R.array.ApertureValuesThird)
            Increment.HALF -> context.resources.getStringArray(R.array.ApertureValuesHalf)
            Increment.FULL -> context.resources.getStringArray(R.array.ApertureValuesFull)
        }

        @get:Bindable
        val customApertureValuesText get() =
            lens.customApertureValues.sorted().distinct().joinToString()

        fun setCustomApertureValues(values: List<Float>) {
            lens.customApertureValues = values.filter { it >= 0f }.sorted().distinct()
            notifyPropertyChanged(BR.customApertureValuesText)
        }

        val focalLengthInputFilter = object : InputFilter {
            override fun filter(
                source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int,
                dend: Int
            ): CharSequence? {
                try {
                    val input = (dest.toString() + source.toString()).toInt()
                    if (input in 0..1500) return null
                } catch (ignored: NumberFormatException) { }
                return ""
            }
        }
    }
}

class LensEditViewModelFactory(private val application: Application,
                               private val fixedLens: Boolean,
                               private val lens: Lens) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(LensEditViewModel::class.java)) {
            return LensEditViewModel(application, fixedLens, lens) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}