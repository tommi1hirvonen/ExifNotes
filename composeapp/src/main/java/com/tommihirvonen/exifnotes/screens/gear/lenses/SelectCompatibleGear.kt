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

package com.tommihirvonen.exifnotes.screens.gear.lenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.util.State

@Composable
fun LensSelectCompatibleCamerasDialog(
    gearViewModel: GearViewModel,
    lens: Lens,
    onDismiss: () -> Unit
) {
    val camerasState = gearViewModel.cameras.collectAsState()
    val cameras = when (val c = camerasState.value) {
        is State.Success -> c.data
        else -> emptyList()
    }
    SelectCompatibleCamerasDialogContent(
        cameras = cameras.associateWith { lens.cameraIds.contains(it.id) },
        onDismiss = onDismiss,
        onConfirm = { selectedCameras ->
            val added = selectedCameras.filterNot { lens.cameraIds.contains(it.id) }
            val removed = lens.cameraIds
                .filter { id ->
                    selectedCameras.none { camera -> camera.id == id }
                }
                .mapNotNull { id ->
                    cameras.firstOrNull { camera -> camera.id == id }
                }
            added.forEach { camera ->
                gearViewModel.addCameraLensLink(camera, lens)
            }
            removed.forEach { camera ->
                gearViewModel.deleteCameraLensLink(camera, lens)
            }
        }
    )
}

@Composable
private fun SelectCompatibleCamerasDialogContent(
    cameras: Map<Camera, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (List<Camera>) -> Unit
) {
    val title = stringResource(R.string.SelectCompatibleCameras)
    MultiChoiceDialog(
        title = title,
        initialItems = cameras,
        itemText = { it.name },
        sortItemsBy = { it.name },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun LensSelectCompatibleFiltersDialog(
    gearViewModel: GearViewModel,
    lens: Lens,
    onDismiss: () -> Unit
) {
    val filters = gearViewModel.filters.collectAsState()
    SelectCompatibleFiltersDialogContent(
        filters = filters.value.associateWith { lens.filterIds.contains(it.id) },
        onDismiss = onDismiss,
        onConfirm = { selectedFilters ->
            val added = selectedFilters.filterNot { lens.filterIds.contains(it.id) }
            val removed = lens.filterIds
                .filter { id ->
                    selectedFilters.none { filter -> filter.id == id }
                }
                .mapNotNull { id ->
                    filters.value.firstOrNull { filter -> filter.id == id }
                }
            added.forEach { filter ->
                gearViewModel.addLensFilterLink(filter, lens, fixedLensCamera = null)
            }
            removed.forEach { filter ->
                gearViewModel.deleteLensFilterLink(filter, lens, fixedLensCamera = null)
            }
        }
    )
}

@Composable
private fun SelectCompatibleFiltersDialogContent(
    filters: Map<Filter, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (List<Filter>) -> Unit
) {
    val title = stringResource(R.string.SelectCompatibleFilters)
    MultiChoiceDialog(
        title = title,
        initialItems = filters,
        itemText = { it.name },
        sortItemsBy = { it.name },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}