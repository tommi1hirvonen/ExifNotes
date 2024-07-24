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

package com.tommihirvonen.exifnotes.screens.gear.filmstocks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FilmStock

@Preview
@Composable
private fun SelectFilmStockDialogPreview() {
    val filmStocks = listOf(
        FilmStock(make = "Agfaphoto", model = "APX 400"),
        FilmStock(make = "Fujifilm", model = "NEOPAN ACROS 100"),
        FilmStock(make = "ILFORD", model = "HP5 PLUS"),
        FilmStock(make = "Kodak", model = "Professional Tri-X 400"),
        FilmStock(make = "Rollei", model = "RPX 100")
    )
    SelectFilmStockDialog(
        filmStocks = filmStocks,
        onDismiss = {},
        onSelect = {}
    )
}

@Composable
fun SelectFilmStockDialog(
    onDismiss: () -> Unit,
    onSelect: (FilmStock) -> Unit,
    filmStocksViewModel: FilmStocksViewModel = hiltViewModel()
) {
    val filmStocks = filmStocksViewModel.filmStocks.collectAsState()
    SelectFilmStockDialog(
        filmStocks = filmStocks.value,
        onDismiss = onDismiss,
        onSelect = onSelect
    )
}

@Composable
private fun SelectFilmStockDialog(
    filmStocks: List<FilmStock>,
    onDismiss: () -> Unit,
    onSelect: (FilmStock) -> Unit
) {
    val manufacturers = remember(filmStocks) {
        filmStocks.mapNotNull(FilmStock::make).distinct().sortedBy { it.lowercase() }
    }
    val manufacturerFilmStocks = remember(filmStocks) {
        filmStocks.groupBy(FilmStock::make)
    }
    var selectedManufacturer by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        title = { Text(stringResource(R.string.SelectFilmStock)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(
                    items = manufacturers,
                    key = { _, item -> item }
                ) { index, manufacturer ->
                    Column(
                        modifier = Modifier
                            .clickable {
                                selectedManufacturer = if (selectedManufacturer == manufacturer) {
                                    null
                                } else {
                                    manufacturer
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(manufacturer, style = MaterialTheme.typography.titleSmall)
                            val targetRotation by animateFloatAsState(
                                targetValue = if (selectedManufacturer == manufacturer) -180f else 0f,
                                animationSpec = tween(300),
                                label = "rotation",
                            )
                            Icon(
                                modifier = Modifier.rotate(targetRotation),
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index < manufacturers.size - 1) {
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (selectedManufacturer == manufacturer) {
                        if (index == manufacturers.size - 1) {
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        }
                        val stocks = manufacturerFilmStocks[manufacturer] ?: emptyList()
                        for (filmStock in stocks) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(filmStock) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(top = 14.dp, bottom = 14.dp, start = 30.dp)
                                ) {
                                    Text(
                                        text = filmStock.model ?: "",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        }
    )
}