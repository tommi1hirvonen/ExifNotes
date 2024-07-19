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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens

@Preview
@Composable
fun FiltersScreen(
    filters: List<Filter> = emptyList(),
    onFilterClick: (Filter) -> Unit = {}
) {
    val state = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state
    ) {
        items(
            items = filters,
            key = { it.id }
        ) { filter ->
            FilterCard(
                filter = filter,
                compatibleCameras = emptyList(), // TODO
                compatibleLenses = emptyList(), // TODO
                onClick = { onFilterClick(filter) }
            )
        }
    }
}

@Preview
@Composable
private fun FilterCardPreview() {
    val filter = Filter(make = "Hoya", model = "ND x64")
    val lens = Lens(make = "Canon", model = "FD 50mm f/1.8")
    val camera = Camera(make = "Contax", model = "T2")
    FilterCard(
        filter = filter,
        compatibleCameras = listOf(camera),
        compatibleLenses = listOf(lens),
        onClick = {}
    )
}

@Composable
private fun FilterCard(
    filter: Filter,
    compatibleCameras: List<Camera>,
    compatibleLenses: List<Lens>,
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
                Text(filter.name, style = MaterialTheme.typography.titleMedium)
                if (compatibleCameras.isNotEmpty()) {
                    Row {
                        Text(
                            text = stringResource(R.string.CamerasNoCap) + ":"
                        )
                    }
                    for (camera in compatibleCameras) {
                        Row {
                            Text(
                                text = "- ${camera.name}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (compatibleLenses.isNotEmpty()) {
                    Row {
                        Text(
                            text = stringResource(R.string.FiltersNoCap) + ":"
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
            }
        }
    }
}