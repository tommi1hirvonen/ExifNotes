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
    private val lensFilterRepository: LensFilterRepository) : ViewModel() {

    val cameras: StateFlow<State<List<Camera>>> get() = mCameras
    val lenses: StateFlow<List<Lens>> get() = mLenses
    val filters: StateFlow<List<Filter>> get() = mFilters

    private val mCameras = MutableStateFlow<State<List<Camera>>>(State.InProgress())
    private val mLenses = MutableStateFlow(emptyList<Lens>())
    private val mFilters = MutableStateFlow(emptyList<Filter>())
    private var cameraList = emptyList<Camera>()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cameraList = cameraRepository.cameras.sorted()
                mCameras.value = State.Success(cameraList)
                mLenses.value = lensRepository.lenses.sorted()
                mFilters.value = filterRepository.filters.sorted()
            }
        }
    }

    fun submitCamera(camera: Camera) {
        if (cameraRepository.updateCamera(camera) == 0) {
            cameraRepository.addCamera(camera)
        }
        replaceCamera(camera)
    }

    private fun replaceCamera(camera: Camera) {
        cameraList = cameraList
            .filterNot { it.id == camera.id }
            .plus(camera)
            .sorted()
        mCameras.value = State.Success(cameraList)
    }

    fun deleteCamera(camera: Camera) {
        cameraRepository.deleteCamera(camera)
        cameraList = cameraList.minus(camera)
        mCameras.value = State.Success(cameraList)
    }

    fun submitLens(lens: Lens) {
        if (lensRepository.updateLens(lens) == 0) {
            lensRepository.addLens(lens)
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
        mLenses.value = mLenses.value.minus(lens)
    }

    fun submitFilter(filter: Filter) {
        if (filterRepository.updateFilter(filter) == 0) {
            filterRepository.addFilter(filter)
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
        mFilters.value = mFilters.value.minus(filter)
    }

    fun addCameraLensLink(camera: Camera, lens: Lens) {
        cameraLensRepository.addCameraLensLink(camera, lens)
        camera.lensIds = camera.lensIds.plus(lens.id).toHashSet()
        lens.cameraIds = lens.cameraIds.plus(camera.id).toHashSet()
        replaceCamera(camera)
        replaceLens(lens)
    }

    fun deleteCameraLensLink(camera: Camera, lens: Lens) {
        cameraLensRepository.deleteCameraLensLink(camera, lens)
        camera.lensIds = camera.lensIds.minus(lens.id).toHashSet()
        lens.cameraIds = lens.cameraIds.minus(camera.id).toHashSet()
        replaceCamera(camera)
        replaceLens(lens)
    }

    fun addLensFilterLink(filter: Filter, lens: Lens, isFixedLens: Boolean) {
        lensFilterRepository.addLensFilterLink(filter, lens)
        filter.lensIds = filter.lensIds.plus(lens.id).toHashSet()
        lens.filterIds = lens.filterIds.plus(filter.id).toHashSet()
        replaceFilter(filter)
        if (!isFixedLens) {
            replaceLens(lens)
        }
    }

    fun deleteLensFilterLink(filter: Filter, lens: Lens, isFixedLens: Boolean) {
        lensFilterRepository.deleteLensFilterLink(filter, lens)
        filter.lensIds = filter.lensIds.minus(lens.id).toHashSet()
        lens.filterIds = lens.filterIds.minus(filter.id).toHashSet()
        replaceFilter(filter)
        if (!isFixedLens) {
            replaceLens(lens)
        }
    }
}