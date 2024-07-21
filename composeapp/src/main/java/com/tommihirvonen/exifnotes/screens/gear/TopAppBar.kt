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

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FilmStockSortMode
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
    onFilmStockFiltersChanged: (FilmStockFilterSet) -> Unit
) {
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
                // TODO
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.ISO)) },
            onClick = {
                onDismiss()
                // TODO
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