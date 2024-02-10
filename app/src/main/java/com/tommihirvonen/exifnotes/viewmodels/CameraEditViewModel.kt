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
import com.tommihirvonen.exifnotes.entities.Camera
import com.tommihirvonen.exifnotes.entities.Format
import com.tommihirvonen.exifnotes.entities.Increment
import com.tommihirvonen.exifnotes.entities.Lens
import com.tommihirvonen.exifnotes.entities.PartialIncrement
import com.tommihirvonen.exifnotes.utilities.validate

class CameraEditViewModel(application: Application, val camera: Camera)
    : AndroidViewModel(application) {

    val context get() = getApplication<Application>()

    val observable = Observable()

    fun validate(): Boolean =
        camera.validate(makeValidation, modelValidation, ::shutterRangeValidation)

    private val makeValidation = { c: Camera ->
        if (c.make.isNullOrEmpty()) {
            observable.makeError = context.getString(R.string.Required)
            false
        } else {
            true
        }
    }

    private val modelValidation = { c: Camera ->
        if (c.model.isNullOrEmpty()) {
            observable.modelError = context.getString(R.string.Required)
            false
        } else {
            true
        }
    }

    private fun shutterRangeValidation(camera: Camera): Boolean {
        // Both ends of the shutter range must be provided.
        if (camera.minShutter == null && camera.maxShutter != null ||
            camera.minShutter != null && camera.maxShutter == null) {
            observable.shutterRangeError = context.getString(R.string.NoMinOrMaxShutter)
            return false
        }
        val minIndex = observable.shutterValueOptions.indexOf(camera.minShutter)
        val maxIndex = observable.shutterValueOptions.indexOf(camera.maxShutter)
        if (minIndex > maxIndex) {
            observable.shutterRangeError = context.getString(R.string.MinShutterSpeedGreaterThanMax)
            return false
        }
        return true
    }

    inner class Observable : BaseObservable() {
        @Bindable
        fun getMake() = camera.make
        fun setMake(value: String?) {
            if (camera.make != value) {
                camera.make = value
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
        fun getModel() = camera.model
        fun setModel(value: String?) {
            if (camera.model != value) {
                camera.model = value
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
        fun getSerialNumber() = camera.serialNumber
        fun setSerialNumber(value: String?) {
            if (camera.serialNumber != value) {
                camera.serialNumber = value
                notifyPropertyChanged(BR.serialNumber)
            }
        }

        @Bindable
        fun getMinShutter() = camera.minShutter
        private fun setMinShutter(value: String?) {
            if (camera.minShutter != value) {
                camera.minShutter = value
                notifyPropertyChanged(BR.minShutter)
                shutterRangeError = null
            }
        }

        @Bindable
        fun getMaxShutter() = camera.maxShutter
        private fun setMaxShutter(value: String?) {
            if (camera.maxShutter != value) {
                camera.maxShutter = value
                notifyPropertyChanged(BR.maxShutter)
                shutterRangeError = null
            }
        }

        @Bindable
        var shutterRangeError: String? = null
            set(value) {
                field = value?.ifEmpty { null }
                notifyPropertyChanged(BR.shutterRangeError)
            }

        @get:Bindable
        var shutterValueOptions: Array<String> = shutterValues

        val shutterIncrement: String = context.resources
            .getStringArray(R.array.StopIncrements)[camera.shutterIncrements.ordinal]

        val shutterIncrementOnItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                camera.shutterIncrements = Increment.from(position)
                shutterValueOptions = shutterValues
                notifyPropertyChanged(BR.shutterValueOptions)
                val minFound = shutterValueOptions.contains(camera.minShutter)
                val maxFound = shutterValueOptions.contains(camera.maxShutter)
                // If either one wasn't found in the new values array, null them.
                if (!minFound || !maxFound) {
                    setMinShutter(null)
                    setMaxShutter(null)
                }
            }

        val minShutterOnItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Handle empty item selection (last in list)
            if (position == shutterValueOptions.lastIndex) {
                setMinShutter(null)
            } else {
                setMinShutter(shutterValueOptions[position])
            }
        }

        val maxShutterOnItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Handle empty item selection (last in list)
            if (position == shutterValueOptions.lastIndex) {
                setMaxShutter(null)
            } else {
                setMaxShutter(shutterValueOptions[position])
            }
        }

        // Handle list item preselection and scrolling when opened.
        val minShutterOnClickListener = View.OnClickListener { view ->
            val index = shutterValueOptions.indexOf(camera.minShutter)
            if (index >= 0) (view as MaterialAutoCompleteTextView).listSelection = index
        }

        // Handle list item preselection and scrolling when opened.
        val maxShutterOnClickListener = View.OnClickListener { view ->
            val index = shutterValueOptions.indexOf(camera.maxShutter)
            if (index >= 0) (view as MaterialAutoCompleteTextView).listSelection = index
        }

        fun clearShutterRange() {
            setMinShutter(null)
            setMaxShutter(null)
        }

        private val shutterValues get() = when (camera.shutterIncrements) {
            Increment.THIRD -> context.resources.getStringArray(R.array.ShutterValuesThird)
            Increment.HALF -> context.resources.getStringArray(R.array.ShutterValuesHalf)
            Increment.FULL -> context.resources.getStringArray(R.array.ShutterValuesFull)
        }.reversedArray()

        val exposureCompensationIncrement: String = context.resources
            .getStringArray(R.array.ExposureCompIncrements)[camera.exposureCompIncrements.ordinal]

        val exposureCompensationIncrementOnItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                camera.exposureCompIncrements = PartialIncrement.from(position)
            }

        @get:Bindable
        val fixedLensSummary: String get() {
            val lens = camera.lens
            return if (lens == null) {
                context.resources.getString(R.string.ClickToSet)
            } else {
                val (minFocal, maxFocal) = lens.minFocalLength to lens.maxFocalLength
                val focalText = when {
                    minFocal != maxFocal -> "$minFocal-${maxFocal}mm"
                    minFocal > 0 -> "${minFocal}mm"
                    else -> null
                }
                val maxAperture = lens.maxAperture
                val apertureText = if (maxAperture == null) {
                    null
                } else {
                    "f/$maxAperture"
                }
                val text = listOfNotNull(focalText, apertureText).joinToString(" ").ifEmpty {
                    context.resources.getString(R.string.ClickToEdit)
                }
                text
            }
        }

        @get:Bindable
        val clearLensVisibility get() = if (camera.lens == null) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        fun setLens(lens: Lens) {
            camera.lens = lens
            notifyPropertyChanged(BR.fixedLensSummary)
            notifyPropertyChanged(BR.clearLensVisibility)
        }

        fun clearLens() {
            camera.lens = null
            notifyPropertyChanged(BR.fixedLensSummary)
            notifyPropertyChanged(BR.clearLensVisibility)
        }

        @get:Bindable
        val format: String get() =
            camera.format.description(context) ?: "Unrecognized"

        val onFormatItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            camera.format = Format.from(position)
        }
    }
}

class CameraEditViewModelFactory(private val application: Application, private val camera: Camera)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(CameraEditViewModel::class.java)) {
            return CameraEditViewModel(application, camera) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}