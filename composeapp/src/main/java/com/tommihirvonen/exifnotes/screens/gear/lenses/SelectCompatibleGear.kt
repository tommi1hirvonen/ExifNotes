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
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel

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
                gearViewModel.addLensFilterLink(filter, lens, isFixedLens = false)
            }
            removed.forEach { filter ->
                gearViewModel.deleteLensFilterLink(filter, lens, isFixedLens = false)
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