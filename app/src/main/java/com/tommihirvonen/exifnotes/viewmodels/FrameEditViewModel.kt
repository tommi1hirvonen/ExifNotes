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
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.tommihirvonen.exifnotes.BR
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.entities.*
import com.tommihirvonen.exifnotes.geocoder.GeocoderRequestBuilder
import com.tommihirvonen.exifnotes.geocoder.GeocoderResponse
import com.tommihirvonen.exifnotes.data.database
import com.tommihirvonen.exifnotes.utilities.readableCoordinates
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class FrameEditViewModel(application: Application,
                         private val geocoderRequestBuilder: GeocoderRequestBuilder,
                         val frame: Frame)
    : AndroidViewModel(application) {

    init {
        if (frame.roll.camera?.isFixedLens == true) {
            frame.lens = null
        }
    }

    private val context get() = getApplication<Application>()
    private val database get() = context.database

    val lens get() = frame.roll.camera?.lens ?: frame.lens

    val observable = Observable().apply {
        val location = frame.location
        if (location != null && frame.formattedAddress.isNullOrEmpty()) {
            // Make the ProgressBar visible to indicate that a query is being executed
            locationProgressBarVisibility = View.VISIBLE
            // Start a coroutine to asynchronously fetch the formatted address.
            viewModelScope.launch {
                val response = geocoderRequestBuilder.fromLatLng(location).getResponse()
                if (response is GeocoderResponse.Success) {
                    setLocation(location, response.formattedAddress.ifEmpty { null })
                }
                locationProgressBarVisibility = View.INVISIBLE
            }
        }
    }

    var pictureFilename: String? = null

    private var lenses: List<Lens> =
        frame.roll.camera?.let(database::getLinkedLenses) ?: database.lenses
        set(value) {
            field = value
            observable.notifyPropertyChanged(BR.lensItems)
        }

    var filters: List<Filter> = lens?.let(database::getLinkedFilters) ?: database.filters

    fun addLens(lens: Lens) {
        database.addLens(lens)
        frame.roll.camera?.let {
            database.addCameraLensLink(it, lens)
        }
        lenses = lenses.plus(lens).sorted()
        observable.setLens(lens)
    }

    fun addFilter(filter: Filter) {
        database.addFilter(filter)
        lens?.let { database.addLensFilterLink(filter, it) }
        filters = filters.plus(filter).sorted()
        observable.setFilters(frame.filters.plus(filter))
    }

    fun validate(): Boolean = true

    inner class Observable : BaseObservable() {

        @get:Bindable
        val frameCountItems = (-3..100).map(Int::toString).toTypedArray()

        @get:Bindable
        val noOfExposuresItems = (1..10).map(Int::toString).toTypedArray()

        @get:Bindable
        val exposureCompItems = frame.roll.camera?.exposureCompValues(context)
            ?: Camera.defaultExposureCompValues(context)

        @get:Bindable
        val lensItems get() = listOf(context.resources.getString(R.string.NoLens))
            .plus(lenses.map { l ->
                val nameIsNotDistinct = lenses.any { it.id != l.id && it.name == l.name }
                if (nameIsNotDistinct && !l.serialNumber.isNullOrEmpty())
                    "${l.name} (${l.serialNumber})"
                else
                    l.name
            })
            .toTypedArray()

        @get:Bindable
        val shutterSpeedItems = frame.roll.camera?.shutterSpeedValues(context)
            ?: Camera.defaultShutterSpeedValues(context)

        @get:Bindable
        val apertureItems get() = lens?.apertureValues(context)
            ?: Lens.defaultApertureValues(context)

        @get:Bindable
        var locationProgressBarVisibility = View.INVISIBLE
            set(value) {
                field = value
                notifyPropertyChanged(BR.locationProgressBarVisibility)
            }

        @Bindable
        fun getFrameCount() = frame.count.toString()
        fun setFrameCount(value: String?) {
            val count = value?.toIntOrNull() ?: 1
            if (frame.count != count) {
                frame.count = count
                notifyPropertyChanged(BR.frameCount)
            }
        }

        @Bindable
        fun getDate() = frame.date
        fun setDate(value: LocalDateTime) {
            frame.date = value
            notifyPropertyChanged(BR.date)
        }

        @Bindable
        fun getAperture() = frame.aperture
        fun setAperture(value: String?) {
            if (frame.aperture != value) {
                frame.aperture = value
                notifyPropertyChanged(BR.aperture)
            }
        }

        @Bindable
        fun getShutterSpeed() = frame.shutter
        fun setShutterSpeed(value: String?) {
            if (frame.shutter != value) {
                frame.shutter = value
                notifyPropertyChanged(BR.shutterSpeed)
            }
        }

        @Bindable
        fun getLens() = frame.lens?.name
        internal fun setLens(value: Lens?) {
            frame.lens = value
            notifyPropertyChanged(BR.lens)

            // Check that filters only contain compatible filters with the current lens.
            filters = value?.let(database::getLinkedFilters) ?: database.filters
            setFilters(frame.filters.filter(filters::contains))

            // Check that the aperture value is compatible with the current lens.
            if (!apertureItems.contains(frame.aperture)) {
                setAperture(null)
            }
            notifyPropertyChanged(BR.apertureItems)

            // Check that the focal length is compatible with the current lens.
            if (value != null) {
                if (frame.focalLength > value.maxFocalLength) {
                    setFocalLength(value.maxFocalLength)
                } else if (frame.focalLength < value.minFocalLength){
                    setFocalLength(value.minFocalLength)
                }
            }
        }

        @Bindable
        fun getFilters() = frame.filters.joinToString(separator = "\n") { "-${it.name}" }
        fun setFilters(value: List<Filter>) {
            frame.filters = value
            notifyPropertyChanged(BR.filters)
        }

        @Bindable
        fun getFocalLength() = frame.focalLength.toString()
        fun setFocalLength(value: Int) {
            frame.focalLength = value
            notifyPropertyChanged(BR.focalLength)
        }

        @Bindable
        fun getLocation() = frame.formattedAddress?.ifEmpty { null }
            ?: frame.location?.readableCoordinates
                ?.replace("N ", "N\n")?.replace("S ", "S\n")
        fun setLocation(location: LatLng?, formattedAddress: String?) {
            frame.location = location
            frame.formattedAddress = formattedAddress
            notifyPropertyChanged(BR.location)
        }
        fun resetLocation() = setLocation(null, null)

        @Bindable
        fun getExposureComp() = frame.exposureComp
        fun setExposureComp(value: String?) {
            if (frame.exposureComp != value) {
                frame.exposureComp = value
                notifyPropertyChanged(BR.exposureComp)
            }
        }

        @Bindable
        fun getNoOfExposures() = frame.noOfExposures.toString()
        fun setNoOfExposures(value: String?) {
            val noOfExp = value?.toIntOrNull() ?: 1
            if (frame.noOfExposures != noOfExp) {
                frame.noOfExposures = noOfExp
                notifyPropertyChanged(BR.noOfExposures)
            }
        }

        @Bindable
        fun getFlashUsed() = frame.flashUsed
        fun setFlashUsed(value: Boolean) {
            if (frame.flashUsed != value) {
                frame.flashUsed = value
                notifyPropertyChanged(BR.flashUsed)
            }
        }

        @Bindable
        fun getNote() = frame.note
        fun setNote(value: String?) {
            if (frame.note != value) {
                frame.note = value
                notifyPropertyChanged(BR.note)
            }
        }

        @get:Bindable
        val lightSource: String get() = frame.lightSource.description(context) ?: "Unknown"

        val lensLayoutVisibility get() = if (frame.roll.camera?.isFixedLens == true) {
            View.GONE
        } else {
            View.VISIBLE
        }

        val lensItemOnClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Account for the "No lens" option
            if (position > 0) {
                setLens(lenses[position - 1])
            } else {
                setLens(null)
            }
        }

        val frameCountOnClickListener = View.OnClickListener { view ->
            val currentIndex = frameCountItems.indexOf(frame.count.toString())
            if (currentIndex >= 0) {
                (view as MaterialAutoCompleteTextView).listSelection = currentIndex
            }
        }

        val lightSourceOnItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            frame.lightSource = LightSource.from(position)
        }

        val exposureCompOnClickListener = View.OnClickListener { view ->
            val currentIndex = exposureCompItems.indexOf(frame.exposureComp)
            if (currentIndex >= 0) {
                (view as MaterialAutoCompleteTextView).listSelection = currentIndex
            }
        }

        val apertureOnItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                setAperture(apertureItems[position])
            } else {
                setAperture(null)
            }
        }

        val apertureOnClickListener = View.OnClickListener { view ->
            val index = apertureItems.indexOf(frame.aperture)
            if (index > 0) (view as MaterialAutoCompleteTextView).listSelection = index
        }

        val shutterOnItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position < shutterSpeedItems.lastIndex) {
                setShutterSpeed(shutterSpeedItems[position])
            } else {
                setShutterSpeed(null)
            }
        }

        val shutterOnClickListener = View.OnClickListener { view ->
            val index = shutterSpeedItems.indexOf(frame.shutter)
            if (index > 0) (view as MaterialAutoCompleteTextView).listSelection = index
        }
    }
}

class FrameEditViewModelFactory(val application: Application,
                                private val geocoderRequestBuilder: GeocoderRequestBuilder,
                                val frame: Frame)
    : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FrameEditViewModel::class.java)) {
            return FrameEditViewModel(application, geocoderRequestBuilder, frame) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}