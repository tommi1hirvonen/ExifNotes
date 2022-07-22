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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GearViewModel(application: Application) : AndroidViewModel(application) {
    private val database = application.database

    val cameras: LiveData<List<Camera>> get() = mCameras
    val lenses: LiveData<List<Lens>> get() = mLenses
    val filters: LiveData<List<Filter>> get() = mFilters

    private val mCameras: MutableLiveData<List<Camera>> by lazy {
        MutableLiveData<List<Camera>>().apply {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    postValue(database.cameras.sorted())
                }
            }
        }
    }

    private val mLenses: MutableLiveData<List<Lens>> by lazy {
        MutableLiveData<List<Lens>>().apply {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    postValue(database.lenses.sorted())
                }
            }
        }
    }

    private val mFilters: MutableLiveData<List<Filter>> by lazy {
        MutableLiveData<List<Filter>>().apply {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    postValue(database.filters.sorted())
                }
            }
        }
    }

    fun addCamera(camera: Camera) {
        database.addCamera(camera)
        replaceCamera(camera)
    }

    fun updateCamera(camera: Camera) = database.updateCamera(camera).also { replaceCamera(camera) }

    private fun replaceCamera(camera: Camera) {
        mCameras.value = mCameras.value?.filterNot { it.id == camera.id }?.plus(camera)?.sorted()
    }

    fun deleteCamera(camera: Camera) {
        database.deleteCamera(camera)
        mCameras.value = mCameras.value?.minus(camera)
    }

    fun addLens(lens: Lens, isFixedLens: Boolean) {
        database.addLens(lens)
        if (!isFixedLens) replaceLens(lens)
    }

    fun updateLens(lens: Lens, isFixedLens: Boolean) = database.updateLens(lens).also {
        if (!isFixedLens) {
            replaceLens(lens)
        }
    }

    private fun replaceLens(lens: Lens) {
        mLenses.value = mLenses.value?.filterNot { it.id == lens.id }?.plus(lens)?.sorted()
    }

    fun deleteLens(lens: Lens) {
        database.deleteLens(lens)
        mLenses.value = mLenses.value?.minus(lens)
    }

    fun addFilter(filter: Filter) {
        database.addFilter(filter)
        replaceFilter(filter)
    }

    fun updateFilter(filter: Filter) = database.updateFilter(filter).also { replaceFilter(filter) }

    private fun replaceFilter(filter: Filter) {
        mFilters.value = mFilters.value?.filterNot { it.id == filter.id }?.plus(filter)?.sorted()
    }

    fun deleteFilter(filter: Filter) {
        database.deleteFilter(filter)
        mFilters.value = mFilters.value?.minus(filter)
    }

    fun addCameraLensLink(camera: Camera, lens: Lens) {
        database.addCameraLensLink(camera, lens)
        camera.lensIds = camera.lensIds.plus(lens.id).toHashSet()
        lens.cameraIds = lens.cameraIds.plus(camera.id).toHashSet()
        replaceCamera(camera)
        replaceLens(lens)
    }

    fun deleteCameraLensLink(camera: Camera, lens: Lens) {
        database.deleteCameraLensLink(camera, lens)
        camera.lensIds = camera.lensIds.minus(lens.id).toHashSet()
        lens.cameraIds = lens.cameraIds.minus(camera.id).toHashSet()
        replaceCamera(camera)
        replaceLens(lens)
    }

    fun addLensFilterLink(filter: Filter, lens: Lens, isFixedLens: Boolean) {
        database.addLensFilterLink(filter, lens)
        filter.lensIds = filter.lensIds.plus(lens.id).toHashSet()
        lens.filterIds = lens.filterIds.plus(filter.id).toHashSet()
        replaceFilter(filter)
        if (!isFixedLens) {
            replaceLens(lens)
        }
    }

    fun deleteLensFilterLink(filter: Filter, lens: Lens, isFixedLens: Boolean) {
        database.deleteLensFilterLink(filter, lens)
        filter.lensIds = filter.lensIds.minus(lens.id).toHashSet()
        lens.filterIds = lens.filterIds.minus(filter.id).toHashSet()
        replaceFilter(filter)
        if (!isFixedLens) {
            replaceLens(lens)
        }
    }
}