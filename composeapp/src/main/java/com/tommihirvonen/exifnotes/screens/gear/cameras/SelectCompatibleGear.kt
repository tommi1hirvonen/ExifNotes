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

package com.tommihirvonen.exifnotes.screens.gear.cameras

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel

@Composable
fun CameraSelectCompatibleLensesDialog(
    gearViewModel: GearViewModel,
    camera: Camera,
    onDismiss: () -> Unit
) {
    val lenses = gearViewModel.lenses.collectAsState()
    MultiChoiceDialog(
        title = stringResource(R.string.SelectCompatibleLenses),
        initialItems = lenses.value.associateWith { camera.lensIds.contains(it.id) },
        itemText = { it.name },
        sortItemsBy = { it.name },
        onDismiss = onDismiss,
        onConfirm = { selectedLenses ->
            val added = selectedLenses.filterNot { camera.lensIds.contains(it.id) }
            val removed = camera.lensIds
                .filter { id ->
                    selectedLenses.none { lens -> lens.id == id }
                }
                .mapNotNull { id ->
                    lenses.value.firstOrNull { lens -> lens.id == id }
                }
            added.forEach { lens ->
                gearViewModel.addCameraLensLink(camera, lens)
            }
            removed.forEach { lens ->
                gearViewModel.deleteCameraLensLink(camera, lens)
            }
        }
    )
}

@Composable
fun CameraSelectCompatibleFiltersDialog(
    gearViewModel: GearViewModel,
    camera: Camera,
    cameraLens: Lens,
    onDismiss: () -> Unit
) {
    val filters = gearViewModel.filters.collectAsState()
    MultiChoiceDialog(
        title = stringResource(R.string.SelectCompatibleFilters),
        initialItems = filters.value.associateWith { cameraLens.filterIds.contains(it.id) },
        itemText = { it.name },
        sortItemsBy = { it.name },
        onDismiss = onDismiss,
        onConfirm = { selectedFilters ->
            val added = selectedFilters.filterNot { cameraLens.filterIds.contains(it.id) }
            val removed = cameraLens.filterIds
                .filter { id ->
                    selectedFilters.none { filter -> filter.id == id }
                }
                .mapNotNull { id ->
                    filters.value.firstOrNull { filter -> filter.id == id }
                }
            added.forEach { filter ->
                gearViewModel.addLensFilterLink(filter, cameraLens, camera)
            }
            removed.forEach { filter ->
                gearViewModel.deleteLensFilterLink(filter, cameraLens, camera)
            }
        }
    )
}