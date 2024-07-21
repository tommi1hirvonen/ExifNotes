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
import androidx.compose.ui.tooling.preview.Preview
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.util.State

@Composable
fun FilterSelectCompatibleLensesDialog(
    gearViewModel: GearViewModel,
    filter: Filter,
    onDismiss: () -> Unit
) {
    val lenses = gearViewModel.lenses.collectAsState()
    SelectCompatibleLensesDialogContent(
        title = stringResource(R.string.SelectCompatibleLenses),
        lenses = lenses.value.associateWith { filter.lensIds.contains(it.id) },
        onDismiss = onDismiss,
        onConfirm = { values ->
            onConfirm(
                gearViewModel = gearViewModel,
                filter = filter,
                lenses = lenses.value,
                selectedLenses = values,
                fixedLensCameras = false
            )
        }
    )
}

@Composable
fun FilterSelectCompatibleCamerasDialog(
    gearViewModel: GearViewModel,
    filter: Filter,
    onDismiss: () -> Unit
) {
    val cameras = gearViewModel.cameras.collectAsState()
    // Handle fixed lens cameras.
    // In truth, we are mapping the filter to the lenses of the cameras.
    val lenses = when (val c = cameras.value) {
        is State.Success -> {
            c.data.mapNotNull(Camera::lens)
        }
        else -> emptyList()
    }
    SelectCompatibleLensesDialogContent(
        title = stringResource(R.string.SelectCompatibleCameras),
        lenses = lenses.associateWith { filter.lensIds.contains(it.id) },
        onDismiss = onDismiss,
        onConfirm = { values ->
            onConfirm(
                gearViewModel = gearViewModel,
                filter = filter,
                lenses = lenses,
                selectedLenses = values,
                fixedLensCameras = true
            )
        }
    )
}

private fun onConfirm(
    gearViewModel: GearViewModel,
    filter: Filter,
    lenses: List<Lens>,
    selectedLenses: List<Lens>,
    fixedLensCameras: Boolean
) {
    val added = selectedLenses.filterNot { filter.lensIds.contains(it.id) }
    val removed = filter.lensIds
        .filter { id ->
            selectedLenses.none { lens -> lens.id == id }
        }
        .mapNotNull { id ->
            lenses.firstOrNull { lens -> lens.id == id }
        }
    added.forEach { lens ->
        gearViewModel.addLensFilterLink(filter, lens, isFixedLens = fixedLensCameras)
    }
    removed.forEach { lens ->
        gearViewModel.deleteLensFilterLink(filter, lens, isFixedLens = fixedLensCameras)
    }
}

@Preview
@Composable
private fun FilterSelectCompatibleLensesDialogPreview() {
    val lenses = mapOf(
        Lens(make = "Canon", model = "FD 28mm f/2.8") to false,
        Lens(make = "Canon", model = "FD 50mm f/1.8") to true
    )
    SelectCompatibleLensesDialogContent(
        title = "Select compatible lenses",
        lenses = lenses,
        onDismiss = {},
        onConfirm = {}
    )
}

@Composable
private fun SelectCompatibleLensesDialogContent(
    title: String,
    lenses: Map<Lens, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (List<Lens>) -> Unit
) {
    MultiChoiceDialog(
        title = title,
        initialItems = lenses,
        itemText = { it.name },
        sortItemsBy = { it.name },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}