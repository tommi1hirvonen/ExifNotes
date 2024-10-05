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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.util.LoadState
import com.tommihirvonen.exifnotes.util.mapNonUniqueToNameWithSerial

@Preview
@Composable
fun CamerasScreen(
    cameras: LoadState<List<Camera>> = LoadState.InProgress(),
    compatibleLensesProvider: (Camera) -> (List<Lens>) = { _ -> emptyList() },
    compatibleFiltersProvider: (Camera) -> (List<Filter>) = { _ -> emptyList() },
    onEdit: (Camera) -> Unit = {},
    onDelete: (Camera) -> Unit = {},
    onEditCompatibleLenses: (Camera) -> Unit = {},
    onEditCompatibleFilters: (Camera) -> Unit = {}
) {
    if (cameras is LoadState.InProgress) {
        Column(
            modifier = Modifier
                .padding(vertical = 48.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
    } else if (cameras is LoadState.Success) {
        val camerasWithUniqueNames = remember(cameras) {
            cameras.data.mapNonUniqueToNameWithSerial()
        }
        val state = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = state
        ) {
            if (cameras.data.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            modifier = Modifier.alpha(0.6f),
                            text = stringResource(R.string.NoCameras),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            items(
                items = camerasWithUniqueNames,
                key = { it.first.id }
            ) { (camera, uniqueName) ->
                CameraCard(
                    modifier = Modifier.animateItem(),
                    camera = camera,
                    uniqueName = uniqueName,
                    compatibleLenses = compatibleLensesProvider(camera),
                    compatibleFilters = compatibleFiltersProvider(camera),
                    onEdit = { onEdit(camera) },
                    onDelete = {onDelete(camera) },
                    onEditCompatibleLenses = { onEditCompatibleLenses(camera) },
                    onEditCompatibleFilters = { onEditCompatibleFilters(camera) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun CameraCardInterchangeablePreview() {
    val camera = Camera(make = "Canon", model = "A-1")
    val lens1 = Lens(make = "Canon", model = "FD 28mm f/2.8")
    val lens2 = Lens(make = "Canon", model = "FD 50mm f/1.8")
    val lens3 = Lens(make = "Canon", model = "FD 100mm f/2.8")
    CameraCard(
        camera = camera,
        uniqueName = camera.name,
        compatibleLenses = listOf(lens1, lens2, lens3),
        compatibleFilters = emptyList(),
        onEdit = {},
        onDelete = {},
        onEditCompatibleLenses = {},
        onEditCompatibleFilters = {}
    )
}

@Preview
@Composable
private fun CameraCardFixedLensPreview() {
    val camera = Camera(
        make = "Contax",
        model = "T2",
        serialNumber = "123321",
        lens = Lens(minFocalLength = 38, maxFocalLength = 38, minAperture = "2.8", maxAperture = "22")
    )
    val filter1 = Filter(make = "Haida", model = "C-POL PRO II")
    val filter2 = Filter(make = "Hoya", model = "ND x64")
    CameraCard(
        camera = camera,
        uniqueName = camera.name,
        compatibleLenses = emptyList(),
        compatibleFilters = listOf(filter1, filter2),
        onEdit = {},
        onDelete = {},
        onEditCompatibleLenses = {},
        onEditCompatibleFilters = {}
    )
}

@Composable
private fun CameraCard(
    modifier: Modifier = Modifier,
    camera: Camera,
    uniqueName: String,
    compatibleLenses: List<Lens>,
    compatibleFilters: List<Filter>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditCompatibleLenses: () -> Unit,
    onEditCompatibleFilters: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().then(modifier)) {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            onClick = { showDropdown = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                Text(uniqueName, style = MaterialTheme.typography.titleMedium)
                if (camera.lens != null) {
                    Row {
                        Text(
                            text = stringResource(R.string.FixedLens),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                if (compatibleLenses.isNotEmpty()) {
                    Row {
                        Text(
                            text = stringResource(R.string.LensesNoCap) + ":"
                        )
                    }
                    for (lens in compatibleLenses) {
                        Row {
                            Text(
                                text = "- ${lens.name}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (compatibleFilters.isNotEmpty()) {
                    Row {
                        Text(
                            text = stringResource(R.string.FiltersNoCap) + ":"
                        )
                    }
                    for (filter in compatibleFilters) {
                        Row {
                            Text(
                                text = "- ${filter.name}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            if (camera.isFixedLens) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.SelectCompatibleFilters)) },
                    leadingIcon = { Icon(Icons.Outlined.Circle, "") },
                    onClick = {
                        showDropdown = false
                        onEditCompatibleFilters()
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.SelectCompatibleLenses)) },
                    leadingIcon = { Icon(Icons.Outlined.Camera, "") },
                    onClick = {
                        showDropdown = false
                        onEditCompatibleLenses()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Edit)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, "") },
                onClick = {
                    showDropdown = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Delete)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, "") },
                onClick = {
                    showDropdown = false
                    onDelete()
                }
            )
        }
    }
}