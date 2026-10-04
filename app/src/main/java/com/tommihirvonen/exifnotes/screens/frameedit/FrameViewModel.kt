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

package com.tommihirvonen.exifnotes.screens.frameedit

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.LightSource
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.withDate
import com.tommihirvonen.exifnotes.core.entities.withTimeZone
import com.tommihirvonen.exifnotes.core.toShutterSpeedOrNull
import com.tommihirvonen.exifnotes.data.repositories.CameraLensRepository
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.data.repositories.FilterRepository
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.data.repositories.LensFilterRepository
import com.tommihirvonen.exifnotes.data.repositories.LensRepository
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import com.tommihirvonen.exifnotes.di.geocoder.GeocoderRequestBuilder
import com.tommihirvonen.exifnotes.di.geocoder.GeocoderResponse
import com.tommihirvonen.exifnotes.di.location.LocationService
import com.tommihirvonen.exifnotes.di.location.TimeZoneService
import com.tommihirvonen.exifnotes.di.pictures.ComplementaryPicturesManager
import com.tommihirvonen.exifnotes.util.SnackbarMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZonedDateTime

@HiltViewModel(assistedFactory = FrameViewModel.Factory::class )
class FrameViewModel @AssistedInject constructor(
    @Assisted("rollId") rollId: Long,
    @Assisted("frameId") frameId: Long,
    @Assisted("previousFrameId") previousFrameId: Long,
    @Assisted("frameCount") frameCount: Int,
    @Assisted estimateDate: Boolean,
    private val application: Application,
    private val frameRepository: FrameRepository,
    rollRepository: RollRepository,
    private val lensRepository: LensRepository,
    private val cameraRepository: CameraRepository,
    private val cameraLensRepository: CameraLensRepository,
    private val filterRepository: FilterRepository,
    private val lensFilterRepository: LensFilterRepository,
    locationService: LocationService,
    private val timeZoneService: TimeZoneService,
    private val geocoderRequestBuilder: GeocoderRequestBuilder,
    private val complementaryPicturesManager: ComplementaryPicturesManager
) : AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("rollId") rollId: Long,
            @Assisted("frameId") frameId: Long,
            @Assisted("previousFrameId") previousFrameId: Long,
            @Assisted("frameCount") frameCount: Int,
            @Assisted estimateDate: Boolean
        ): FrameViewModel
    }

    val roll: Roll
    private val context: Context get() = application.applicationContext
    private val _frame: MutableStateFlow<Frame>
    private val _lens: MutableStateFlow<Lens?>
    private val _filters: MutableStateFlow<List<Filter>>
    private val _apertureValues: MutableStateFlow<List<String>>
    private val _isResolvingFormattedAddress = MutableStateFlow(false)
    private val _isResolvingTimeZone = MutableStateFlow(false)
    private var timeZoneJob: Job? = null
    private var timeZoneResolved = false
    private val existingFrame = frameRepository.getFrame(frameId)
    private val _pictureBitmap = MutableStateFlow<Bitmap?>(null)
    private val _pictureRotation = MutableStateFlow(0f)
    private val _snackbarMessage = MutableStateFlow(SnackbarMessage())

    private var placeholderPictureFilename: String? = null

    init {
        val frame = if (existingFrame != null) {
            existingFrame.pictureFilename?.let {
                val exists = complementaryPicturesManager
                    .getPictureFile(it)
                    .exists()
                existingFrame.copy(pictureFileExists = exists)
            }
            existingFrame
        } else {
            val noOfExposures = 1
            val location = if (estimateDate) null
                else locationService.lastLocation?.let { LatLng(it.latitude, it.longitude) }
            val previousFrame = frameRepository.getFrame(previousFrameId)
            val now = ZonedDateTime.now()
            val date = if (estimateDate && previousFrame != null)
                previousFrame.date.plusMinutes(5)
            else now.toLocalDateTime()
            if (previousFrame != null) {
                Frame(
                    rollId = previousFrame.rollId,
                    count = frameCount,
                    date = date,
                    timeZoneId = if (estimateDate) previousFrame.timeZoneId else now.zone.id,
                    utcOffsetSeconds = if (estimateDate) previousFrame.utcOffsetSeconds else now.offset.totalSeconds,
                    noOfExposures = noOfExposures,
                    location = location,
                    lens = previousFrame.lens,
                    shutter = previousFrame.shutter,
                    aperture = previousFrame.aperture,
                    filters = previousFrame.filters,
                    focalLength = previousFrame.focalLength,
                    lightSource = previousFrame.lightSource
                ).withDate(date)
            } else {
                Frame(
                    rollId = rollId,
                    count = frameCount,
                    date = date,
                    timeZoneId = now.zone.id,
                    utcOffsetSeconds = now.offset.totalSeconds,
                    noOfExposures = noOfExposures,
                    location = location
                )
            }
        }
        roll = rollRepository.getRoll(rollId) ?: Roll()
        _frame = MutableStateFlow(frame)
        val lens = roll.camera?.lens ?: frame.lens
        _lens = MutableStateFlow(lens)
        _filters = MutableStateFlow(getFilters(lens))
        _apertureValues = MutableStateFlow(getApertureValues(lens))
        val location = frame.location
        if (location != null && frame.formattedAddress.isNullOrEmpty()) {
            // Make the ProgressBar visible to indicate that a query is being executed
            _isResolvingFormattedAddress.value = true
            // Start a coroutine to asynchronously fetch the formatted address.
            viewModelScope.launch {
                val response = geocoderRequestBuilder.fromLatLng(location).getResponse()
                if (response is GeocoderResponse.Success && _frame.value.location == location) {
                    _frame.value = _frame.value.copy(
                        formattedAddress = response.formattedAddress.ifEmpty { null }
                    )
                }
                _isResolvingFormattedAddress.value = false
            }
        }
        if (location != null && (existingFrame == null || frame.timeZoneId == null)) {
            resolveTimeZone()
        }
        loadPictureBitmap()
    }

    private val _lenses = MutableStateFlow(
        roll.camera?.let(cameraRepository::getLinkedLenses)
            ?: lensRepository.lenses
    )

    val frame = _frame.asStateFlow()
    val lens = _lens.asStateFlow()
    val lenses = _lenses.asStateFlow()
    val filters = _filters.asStateFlow()
    val apertureValues = _apertureValues.asStateFlow()
    val isResolvingFormattedAddress = _isResolvingFormattedAddress.asStateFlow()
    val isResolvingTimeZone = _isResolvingTimeZone.asStateFlow()
    val pictureBitmap = _pictureBitmap.asStateFlow()
    val pictureRotation = _pictureRotation.asStateFlow()
    val snackbarMessage = _snackbarMessage.asStateFlow()

    val shutterValues = roll.camera?.shutterSpeedValues(context)?.toList()
        ?: Camera.defaultShutterSpeedValues(context).toList()
    val exposureCompValues = roll.camera?.exposureCompValues(context)?.toList()
        ?: Camera.defaultExposureCompValues(context).toList()

    fun setCount(value: Int) {
        _frame.value = _frame.value.copy(count = value)
    }

    fun setDate(value: LocalDateTime) {
        _frame.value = _frame.value.withDate(value)
    }

    fun setNote(value: String) {
        _frame.value = _frame.value.copy(note = value)
    }

    fun setShutter(value: String?) {
        _frame.value = _frame.value.copy(shutter = value?.toShutterSpeedOrNull())
    }

    fun setAperture(value: String?) {
        if (value == null || value.toDoubleOrNull() != null) {
            val actualValue = value?.replace(
                regex = "[^\\d.]".toRegex(),
                replacement = ""
            )
            _frame.value = _frame.value.copy(aperture = actualValue)
        }
    }

    fun setExposureComp(value: String) {
        val actualValue = if (value == "0") null else value
        _frame.value = _frame.value.copy(exposureComp = actualValue)
    }

    fun setNoOfExposures(value: Int) {
        if (value >= 1) {
            _frame.value = _frame.value.copy(noOfExposures = value)
        }
    }

    fun setFlashUsed(value: Boolean) {
        _frame.value = _frame.value.copy(flashUsed = value)
    }

    fun setLightSource(value: LightSource) {
        _frame.value = _frame.value.copy(lightSource = value)
    }

    fun submitLens(value: Lens) {
        val lens = if (lensRepository.updateLens(value) == 0) {
            lensRepository.addLens(value)
        } else {
            value
        }
        roll.camera?.let { camera ->
            cameraLensRepository.addCameraLensLink(camera, lens)
        }
        _lenses.value = _lenses.value.plus(lens).sorted()
        setLens(lens)
    }

    fun setLens(value: Lens?) {
        val frame = _frame.value
        val filters = getFilters(value)
        val apertureValues = getApertureValues(value)
        _filters.value = filters
        _apertureValues.value = apertureValues
        val aperture = if (!apertureValues.contains(frame.aperture)) null else frame.aperture
        val focalLength = if (value != null && frame.focalLength > value.maxFocalLength)
            value.maxFocalLength
        else if (value != null && frame.focalLength < value.minFocalLength)
            value.minFocalLength
        else
            frame.focalLength
        _lens.value = value
        _frame.value = frame.copy(
            lens = value,
            filters = frame.filters.filter(filters::contains),
            aperture = aperture,
            focalLength = focalLength
        )
    }

    fun submitFilter(value: Filter) {
        val filter = if (filterRepository.updateFilter(value) == 0) {
            filterRepository.addFilter(value)
        } else {
            value
        }
        lens.value?.let {
            lensFilterRepository.addLensFilterLink(filter, it)
        }
        _filters.value = _filters.value.plus(filter).sorted()
        setFilters(_frame.value.filters.plus(filter).sorted())
    }

    fun setFilters(value: List<Filter>) {
        _frame.value = _frame.value.copy(filters = value)
    }

    fun setFocalLength(value: Int) {
        _frame.value = _frame.value.copy(focalLength = value)
    }

    fun setLocation(location: LatLng?, formattedAddress: String?) {
        val locationChanged = _frame.value.location != location
        _frame.value = _frame.value.copy(
            location = location,
            formattedAddress = formattedAddress
        )
        if (locationChanged) {
            timeZoneResolved = false
            timeZoneJob?.cancel()
            _isResolvingTimeZone.value = false
            // Keep the known time zone until a replacement is resolved.
            if (location != null) resolveTimeZone()
        }
    }

    fun resolveTimeZone() {
        val location = _frame.value.location ?: return
        val previousFrame = _frame.value
        timeZoneJob?.cancel()
        _isResolvingTimeZone.value = true
        timeZoneJob = viewModelScope.launch {
            val zoneId = timeZoneService.getTimeZone(location, _frame.value.date)
            val current = _frame.value
            val preferredOffset = previousFrame.utcOffsetSeconds.takeIf {
                previousFrame.timeZoneId == zoneId && previousFrame.date == current.date
            }
            if (zoneId != null) {
                timeZoneResolved = true
                _frame.value = current.copy(timeZoneId = zoneId, utcOffsetSeconds = preferredOffset)
                    .withTimeZone(zoneId)
            }
            if (zoneId == null) {
                _snackbarMessage.value = SnackbarMessage(
                    message = context.getString(R.string.TimeZoneLookupFailed)
                )
            }
            _isResolvingTimeZone.value = false
        }
    }

    fun validate(): Boolean = !_isResolvingTimeZone.value

    fun frameForSave(): Frame {
        val frame = _frame.value
        if (timeZoneResolved || existingFrame == null || frame.location != existingFrame.location) {
            return frame
        }
        val saved = frameRepository.getFrame(frame.id) ?: return frame
        if (saved.location != frame.location ||
            (saved.timeZoneId == existingFrame.timeZoneId &&
                saved.utcOffsetSeconds == existingFrame.utcOffsetSeconds)) {
            return frame
        }
        return frame.copy(timeZoneId = saved.timeZoneId, utcOffsetSeconds = saved.utcOffsetSeconds)
            .withDate(frame.date)
    }

    fun clearComplementaryPicture() {
        _frame.value = _frame.value.copy(pictureFilename = null, pictureFileExists = false)
        _pictureBitmap.value = null
    }

    fun createNewPictureFile(): Uri {
        val pictureFile = complementaryPicturesManager.createNewPictureFile()
        placeholderPictureFilename = pictureFile.name
        //Android Nougat requires that the file is given via FileProvider
        val photoUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider", pictureFile
            )
        } else {
            Uri.fromFile(pictureFile)
        }
        return photoUri
    }

    fun commitPlaceholderPictureFile() {
        val filename = placeholderPictureFilename
        placeholderPictureFilename = null
        if (filename == null) {
            return
        }
        _frame.value = _frame.value.copy(pictureFilename = filename, pictureFileExists = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    complementaryPicturesManager.compressPictureFile(filename)
                    loadPictureBitmap()
                } catch (_: IOException) {
                    _snackbarMessage.value = SnackbarMessage(
                        message = context.resources.getString(R.string.ErrorCompressingComplementaryPicture)
                    )
                }
            }
        }
    }

    fun setPictureFromUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val pictureFile = complementaryPicturesManager.createNewPictureFile()
                try {
                    // Get the compressed bitmap from the Uri.
                    val pictureBitmap = complementaryPicturesManager
                        .getCompressedBitmap(uri) ?: return@withContext
                    try {
                        // Save the compressed bitmap to the placeholder file.
                        complementaryPicturesManager.saveBitmapToFile(pictureBitmap, pictureFile)
                        // Update the member reference and set the complementary picture.
                        _frame.value = _frame.value.copy(
                            pictureFilename = pictureFile.name,
                            pictureFileExists = true
                        )
                        _pictureBitmap.value = pictureBitmap
                        _pictureRotation.value = getPictureRotation(pictureFile)
                    } catch (_: IOException) {
                        _snackbarMessage.value = SnackbarMessage(
                            message = context.resources.getString(R.string.ErrorSavingSelectedPicture)
                        )
                    }
                } catch (_: FileNotFoundException) {
                    _snackbarMessage.value = SnackbarMessage(
                        message = context.resources.getString(R.string.ErrorLocatingSelectedPicture)
                    )
                }
            }
        }
    }

    fun addPictureToGallery() {
        try {
            complementaryPicturesManager.addPictureToGallery(_frame.value.pictureFilename)
            _snackbarMessage.value = SnackbarMessage(
                message = context.resources.getString(R.string.PictureAddedToGallery)
            )
        } catch (_: Exception) {
            _snackbarMessage.value = SnackbarMessage(
                message = context.resources.getString(R.string.ErrorAddingPictureToGallery)
            )
        }
    }

    fun rotatePictureRight() {
        val filename = _frame.value.pictureFilename
        if (filename.isNullOrEmpty()) {
            return
        }
        try {
            complementaryPicturesManager.rotatePictureRight(filename)
            _pictureRotation.value += 90f
        } catch (_: IOException) {
            _snackbarMessage.value = SnackbarMessage(
                message = context.resources.getString(R.string.ErrorWhileEditingPicturesExifData)
            )
        }
    }

    fun rotatePictureLeft() {
        val filename = _frame.value.pictureFilename
        if (filename.isNullOrEmpty()) {
            return
        }
        try {
            complementaryPicturesManager.rotatePictureLeft(filename)
            _pictureRotation.value -= 90f
        } catch (_: IOException) {
            _snackbarMessage.value = SnackbarMessage(
                message = context.resources.getString(R.string.ErrorWhileEditingPicturesExifData)
            )
        }
    }

    private fun getFilters(lens: Lens?): List<Filter> =
        lens?.let(filterRepository::getLinkedFilters)
            ?: _lens.value?.let(filterRepository::getLinkedFilters)
            ?: filterRepository.filters

    private fun getApertureValues(lens: Lens?): List<String> =
        lens?.apertureValues(context)?.toList()
            ?: _lens.value?.apertureValues(context)?.toList()
            ?: Lens.defaultApertureValues(context).toList()

    private fun loadPictureBitmap() {
        val filename = _frame.value.pictureFilename
        if (filename.isNullOrEmpty()) {
            return
        }
        val pictureFile = complementaryPicturesManager.getPictureFile(filename)
        if (!pictureFile.exists()) {
            _snackbarMessage.value = SnackbarMessage(
                message = context.resources.getString(R.string.PictureSetButNotFound)
            )
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _pictureRotation.value = getPictureRotation(pictureFile)
                _pictureBitmap.value = BitmapFactory.decodeFile(pictureFile.absolutePath)
            }
        }
    }

    private fun getPictureRotation(file: File) =
        try {
            val exifInterface = ExifInterface(file.absolutePath)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
}
