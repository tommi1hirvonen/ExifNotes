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

package com.tommihirvonen.exifnotes.screens.gear.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.util.LoadState

@Composable
fun FilterSelectCompatibleLensesDialog(
    gearViewModel: GearViewModel,
    filter: Filter,
    onDismiss: () -> Unit
) {
    val lenses = gearViewModel.lenses.collectAsState()
    MultiChoiceDialog(
        title = stringResource(R.string.SelectCompatibleLenses),
        initialItems = lenses.value.associateWith { filter.lensIds.contains(it.id) },
        itemText = { it.name },
        sortItemsBy = { it.name },
        onDismiss = onDismiss,
        onConfirm = { selectedLenses ->
            val added = selectedLenses.filterNot { filter.lensIds.contains(it.id) }
            val removed = filter.lensIds
                .filter { id ->
                    selectedLenses.none { lens -> lens.id == id }
                }
                .mapNotNull { id ->
                    lenses.value.firstOrNull { lens -> lens.id == id }
                }
            val foldResult = added.fold(filter) { filter, lens ->
                val (f, _) = gearViewModel.addLensFilterLink(filter, lens, fixedLensCamera = null)
                f
            }
            removed.fold(foldResult) { filter, lens ->
                val (f, _) = gearViewModel.deleteLensFilterLink(filter, lens, fixedLensCamera = null)
                f
            }
        }
    )
}

@Composable
fun FilterSelectCompatibleCamerasDialog(
    gearViewModel: GearViewModel,
    filter: Filter,
    onDismiss: () -> Unit
) {
    val camerasState = gearViewModel.cameras.collectAsState()

    // Handle fixed lens cameras.
    // In truth, we are mapping the filter to the lenses of the cameras.
    val cameraLenses = when (val cameras = camerasState.value) {
        is LoadState.Success -> {
            cameras.data.mapNotNull { camera ->
                val lens = camera.lens
                if (lens != null) {
                    camera to lens
                } else {
                    null
                }
            }
        }
        else -> emptyList()
    }
    MultiChoiceDialog(
        title = stringResource(R.string.SelectCompatibleCameras),
        initialItems = cameraLenses.associateWith { filter.lensIds.contains(it.second.id) },
        itemText = { it.first.name },
        sortItemsBy = { it.first.name },
        onDismiss = onDismiss,
        onConfirm = { selectedCameras ->
            val added = selectedCameras.filterNot { filter.lensIds.contains(it.second.id) }
            val removed = filter.lensIds
                .filter { id ->
                    selectedCameras.none { pair -> pair.second.id == id }
                }
                .mapNotNull { id ->
                    cameraLenses.firstOrNull { pair -> pair.second.id == id }
                }
            // Fold lenses so that the correct list of filter ids
            // contained in the lens is accumulated.
            val foldResult = added.fold(filter) { filter, cameraLens ->
                val (f, _) = gearViewModel.addLensFilterLink(
                    filter,
                    cameraLens.second,
                    cameraLens.first
                )
                f
            }
            // Use the fold operation result as the starting point for deletion.
            removed.fold(foldResult) { filter, cameraLens ->
                val (f, _) = gearViewModel.deleteLensFilterLink(
                    filter,
                    cameraLens.second,
                    cameraLens.first
                )
                f
            }
        }
    )
}