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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.util.State

@Preview
@Composable
fun CamerasScreen(
    cameras: State<List<Camera>> = State.InProgress(),
    compatibleLensesProvider: (Camera) -> (List<Lens>) = { _ -> emptyList() },
    compatibleFiltersProvider: (Camera) -> (List<Filter>) = { _ -> emptyList() },
    onCameraClick: (Camera) -> Unit = {}
) {
    if (cameras is State.InProgress) {
        Column(
            modifier = Modifier
                .padding(vertical = 48.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
    } else if (cameras is State.Success) {
        val state = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = state
        ) {
            items(
                items = cameras.data,
                key = { it.id }
            ) { camera ->
                CameraCard(
                    camera = camera,
                    compatibleLenses = compatibleLensesProvider(camera),
                    compatibleFilters = compatibleFiltersProvider(camera),
                    onClick = { onCameraClick(camera) }
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
        compatibleLenses = listOf(lens1, lens2, lens3),
        compatibleFilters = emptyList(),
        onClick = {}
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
        compatibleLenses = emptyList(),
        compatibleFilters = listOf(filter1, filter2),
        onClick = {}
    )
}

@Composable
private fun CameraCard(
    camera: Camera,
    compatibleLenses: List<Lens>,
    compatibleFilters: List<Filter>,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                Text(camera.name, style = MaterialTheme.typography.titleMedium)
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
    }
}