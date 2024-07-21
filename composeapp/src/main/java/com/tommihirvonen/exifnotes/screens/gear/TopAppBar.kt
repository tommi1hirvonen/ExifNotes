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

package com.tommihirvonen.exifnotes.screens.gear

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FilmStockSortMode
import com.tommihirvonen.exifnotes.screens.DialogContent
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStockFilterSet

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun TopAppBarPreview() {
    AppBar(
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
        currentPagerPage = 3,
        onNavigateUp = {},
        filmStockSortMode = FilmStockSortMode.NAME,
        onFilmStockSort = {},
        manufacturers = emptyList(),
        isoValues = emptyList(),
        filmStockFilters = FilmStockFilterSet(),
        onFilmStockFiltersChanged = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    currentPagerPage: Int,
    onNavigateUp: () -> Unit,
    filmStockSortMode: FilmStockSortMode,
    onFilmStockSort: (FilmStockSortMode) -> Unit,
    manufacturers: List<String>,
    isoValues: List<Int>,
    filmStockFilters: FilmStockFilterSet,
    onFilmStockFiltersChanged: (FilmStockFilterSet) -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.Gear)) },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
            }
        },
        scrollBehavior = scrollBehavior,
        actions = {
            if (currentPagerPage == 3) {
                var showSortDropdown by remember { mutableStateOf(false) }
                var showFilterDropdown by remember { mutableStateOf(false) }
                IconButton(onClick = { showFilterDropdown = true }) {
                    Icon(Icons.Outlined.FilterList, "")
                }
                IconButton(onClick = { showSortDropdown = true }) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, "")
                }
                SortDropdownMenu(
                    expanded = showSortDropdown,
                    onDismiss = { showSortDropdown = false },
                    filmStockSortMode = filmStockSortMode,
                    onFilmStockSort = onFilmStockSort
                )
                FilterDropdownMenu(
                    expanded = showFilterDropdown,
                    onDismiss = { showFilterDropdown = false },
                    manufacturers = manufacturers,
                    isoValues = isoValues,
                    filmStockFilters = filmStockFilters,
                    onFilmStockFiltersChanged = onFilmStockFiltersChanged
                )
            }
        }
    )
}

@Composable
private fun FilterDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    manufacturers: List<String>,
    isoValues: List<Int>,
    filmStockFilters: FilmStockFilterSet,
    onFilmStockFiltersChanged: (FilmStockFilterSet) -> Unit
) {
    var showManufacturersDialog by remember { mutableStateOf(false) }
    var showIsoDialog by remember { mutableStateOf(false) }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = stringResource(R.string.FilterFilmStocksBy),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Manufacturer)) },
            onClick = {
                onDismiss()
                showManufacturersDialog = true
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ISO)) },
            onClick = {
                onDismiss()
                showIsoDialog = true
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.FilmType)) },
            onClick = {
                onDismiss()
                // TODO
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.FilmProcess)) },
            onClick = {
                onDismiss()
                // TODO
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.AddedBy)) },
            onClick = {
                onDismiss()
                // TODO
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Reset)) },
            onClick = {
                onDismiss()
                onFilmStockFiltersChanged(FilmStockFilterSet())
            }
        )
    }
    if (showManufacturersDialog) {
        MultiChoiceDialog(
            title = stringResource(R.string.Manufacturer),
            initialItems = manufacturers.map { it to filmStockFilters.manufacturers.contains(it) },
            onDismiss = { showManufacturersDialog = false },
            onConfirm = { values ->
                showManufacturersDialog = false
                onFilmStockFiltersChanged(filmStockFilters.copy(manufacturers = values))
            }
        )
    }
    if (showIsoDialog) {
        MultiChoiceDialog(
            title = stringResource(R.string.ISO),
            initialItems = isoValues.map { it.toString() to filmStockFilters.isoValues.contains(it) },
            onDismiss = { showIsoDialog = false },
            onConfirm = { values ->
                showIsoDialog = false
                val v = values.mapNotNull { it.toIntOrNull() }
                onFilmStockFiltersChanged(filmStockFilters.copy(isoValues = v))
            }
        )
    }
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    filmStockSortMode: FilmStockSortMode,
    onFilmStockSort: (FilmStockSortMode) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = stringResource(R.string.SortFilmStocksBy),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = {
                onDismiss()
                onFilmStockSort(FilmStockSortMode.NAME)
            },
            trailingIcon = {
                RadioButton(
                    selected = filmStockSortMode == FilmStockSortMode.NAME,
                    onClick = {
                        onFilmStockSort(FilmStockSortMode.NAME)
                        onDismiss()
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ISO)) },
            onClick = {
                onDismiss()
                onFilmStockSort(FilmStockSortMode.ISO)
            },
            trailingIcon = {
                RadioButton(
                    selected = filmStockSortMode == FilmStockSortMode.ISO,
                    onClick = {
                        onFilmStockSort(FilmStockSortMode.ISO)
                        onDismiss()
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun MultiChoiceDialogPreview() {
    MultiChoiceDialog(
        title = "Select manufacturers",
        initialItems = listOf(
            "Fujifilm" to true,
            "Ilford" to false,
            "Kodak" to true
        ),
        onDismiss = {},
        onConfirm = {}
    )
}

@Composable
private fun MultiChoiceDialog(
    title: String,
    initialItems: List<Pair<String, Boolean>>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val items = remember(initialItems) {
        initialItems.toMutableStateMap()
    }
    val list = items.map { it.key }.sorted().toList()
    Dialog(onDismissRequest = { /*TODO*/ }) {
        DialogContent {
            Column(modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
            ) {
                Row {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .weight(1f, fill = false)
                ) {
                    items(items = list) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val prev = items[item] ?: false
                                    items[item] = !prev
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = items[item] ?: false,
                                onCheckedChange = { items[item] = it }
                            )
                            Text(item)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            for (key in items.keys) {
                                items[key] = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.DeselectAll))
                    }
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.Cancel))
                        }
                        TextButton(
                            onClick = {
                                onDismiss()
                                val selectedItems = items
                                    .filter { it.value }
                                    .map { it.key }
                                    .toList()
                                onConfirm(selectedItems)
                            }
                        ) {
                            Text(stringResource(R.string.OK))
                        }
                    }
                }
            }
        }
    }
}