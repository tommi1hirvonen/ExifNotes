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

package com.tommihirvonen.exifnotes.screens.gear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.repositories.CameraLensRepository
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.data.repositories.FilterRepository
import com.tommihirvonen.exifnotes.data.repositories.LensFilterRepository
import com.tommihirvonen.exifnotes.data.repositories.LensRepository
import com.tommihirvonen.exifnotes.util.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GearViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val lensRepository: LensRepository,
    private val filterRepository: FilterRepository,
    private val cameraLensRepository: CameraLensRepository,
    private val lensFilterRepository: LensFilterRepository
) : ViewModel() {

    val cameras: StateFlow<LoadState<List<Camera>>> get() = mCameras
    val lenses: StateFlow<List<Lens>> get() = mLenses
    val filters: StateFlow<List<Filter>> get() = mFilters

    private val mCameras = MutableStateFlow<LoadState<List<Camera>>>(LoadState.InProgress())
    private val mLenses = MutableStateFlow(emptyList<Lens>())
    private val mFilters = MutableStateFlow(emptyList<Filter>())
    private var cameraList = emptyList<Camera>()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cameraList = cameraRepository.cameras.sorted()
                mCameras.value = LoadState.Success(cameraList)
                mLenses.value = lensRepository.lenses.sorted()
                mFilters.value = filterRepository.filters.sorted()
            }
        }
    }

    fun isCameraInUse(camera: Camera) = cameraRepository.isCameraBeingUsed(camera)

    fun isLensInUse(lens: Lens) = lensRepository.isLensInUse(lens)

    fun isFilterInUse(filter: Filter) = filterRepository.isFilterBeingUsed(filter)

    fun submitCamera(value: Camera) {
        val (count, existingCamera) = cameraRepository.updateCamera(value)
        val camera = if (count == 0) {
            cameraRepository.addCamera(value)
        } else {
            existingCamera
        }
        replaceCamera(camera)
    }

    private fun replaceCamera(camera: Camera) {
        cameraList = cameraList
            .filterNot { it.id == camera.id }
            .plus(camera)
            .sorted()
        mCameras.value = LoadState.Success(cameraList)
    }

    fun deleteCamera(camera: Camera) {
        cameraRepository.deleteCamera(camera)
        cameraList = cameraList.filterNot { it.id == camera.id }
        mCameras.value = LoadState.Success(cameraList)
    }

    fun submitLens(value: Lens) {
        val lens = if (lensRepository.updateLens(value) == 0) {
            lensRepository.addLens(value)
        } else {
            value
        }
        replaceLens(lens)
    }

    private fun replaceLens(lens: Lens) {
        mLenses.value = mLenses.value
            .filterNot { it.id == lens.id }
            .plus(lens)
            .sorted()
    }

    fun deleteLens(lens: Lens) {
        lensRepository.deleteLens(lens)
        mLenses.value = mLenses.value.filterNot { it.id == lens.id }
    }

    fun submitFilter(value: Filter) {
        val filter = if (filterRepository.updateFilter(value) == 0) {
            filterRepository.addFilter(value)
        } else {
            value
        }
        replaceFilter(filter)
    }

    private fun replaceFilter(filter: Filter) {
        mFilters.value = mFilters.value
            .filterNot { it.id == filter.id }
            .plus(filter)
            .sorted()
    }

    fun deleteFilter(filter: Filter) {
        filterRepository.deleteFilter(filter)
        mFilters.value = mFilters.value.filterNot { it.id == filter.id }
    }

    fun addCameraLensLink(camera: Camera, lens: Lens): Pair<Camera, Lens> {
        cameraLensRepository.addCameraLensLink(camera, lens)
        val lensIds = camera.lensIds.plus(lens.id).toHashSet()
        val cameraReplacement = camera.copy(lensIds = lensIds)
        replaceCamera(cameraReplacement)
        val cameraIds = lens.cameraIds.plus(camera.id).toHashSet()
        val lensReplacement = lens.copy(cameraIds = cameraIds)
        replaceLens(lensReplacement)
        return cameraReplacement to lensReplacement
    }

    fun deleteCameraLensLink(camera: Camera, lens: Lens): Pair<Camera, Lens> {
        cameraLensRepository.deleteCameraLensLink(camera, lens)
        val lensIds = camera.lensIds.minus(lens.id).toHashSet()
        val cameraReplacement = camera.copy(lensIds = lensIds)
        replaceCamera(cameraReplacement)
        val cameraIds = lens.cameraIds.minus(camera.id).toHashSet()
        val lensReplacement = lens.copy(cameraIds = cameraIds)
        replaceLens(lensReplacement)
        return cameraReplacement to lensReplacement
    }

    fun addLensFilterLink(filter: Filter, lens: Lens, fixedLensCamera: Camera?): Pair<Filter, Lens> {
        lensFilterRepository.addLensFilterLink(filter, lens)
        val lensIds = filter.lensIds.plus(lens.id).toHashSet()
        val filterReplacement = filter.copy(lensIds = lensIds)
        replaceFilter(filterReplacement)
        if (fixedLensCamera != null) {
            val filterIds = lens.filterIds.plus(filter.id).toHashSet()
            val lensReplacement = fixedLensCamera.lens?.copy(filterIds = filterIds)
                ?: lens.copy(filterIds = filterIds)
            val cameraReplacement = fixedLensCamera.copy(lens = lensReplacement)
            replaceCamera(cameraReplacement)
            return filterReplacement to lensReplacement
        } else {
            val filterIds = lens.filterIds.plus(filter.id).toHashSet()
            val lensReplacement = lens.copy(filterIds = filterIds)
            replaceLens(lensReplacement)
            return filterReplacement to lensReplacement
        }
    }

    fun deleteLensFilterLink(filter: Filter, lens: Lens, fixedLensCamera: Camera?): Pair<Filter, Lens> {
        lensFilterRepository.deleteLensFilterLink(filter, lens)
        val lensIds = filter.lensIds.minus(lens.id).toHashSet()
        val filterReplacement = filter.copy(lensIds = lensIds)
        replaceFilter(filterReplacement)
        if (fixedLensCamera != null) {
            val filterIds = lens.filterIds.minus(filter.id).toHashSet()
            val lensReplacement = fixedLensCamera.lens?.copy(filterIds = filterIds)
                ?: lens.copy(filterIds = filterIds)
            val cameraReplacement = fixedLensCamera.copy(lens = lensReplacement)
            replaceCamera(cameraReplacement)
            return filterReplacement to lensReplacement
        } else {
            val filterIds = lens.filterIds.minus(filter.id).toHashSet()
            val lensReplacement = lens.copy(filterIds = filterIds)
            replaceLens(lensReplacement)
            return filterReplacement to lensReplacement
        }
    }
}