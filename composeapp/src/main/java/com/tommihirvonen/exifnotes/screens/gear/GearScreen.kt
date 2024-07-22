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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CameraRoll
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.FilmStockSortMode
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.gear.cameras.CamerasScreen
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStockFilterSet
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStocksScreen
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStocksViewModel
import com.tommihirvonen.exifnotes.screens.gear.filters.FilterSelectCompatibleCamerasDialog
import com.tommihirvonen.exifnotes.screens.gear.filters.FilterSelectCompatibleLensesDialog
import com.tommihirvonen.exifnotes.screens.gear.filters.FiltersScreen
import com.tommihirvonen.exifnotes.screens.gear.lenses.LensSelectCompatibleCamerasDialog
import com.tommihirvonen.exifnotes.screens.gear.lenses.LensSelectCompatibleFiltersDialog
import com.tommihirvonen.exifnotes.screens.gear.lenses.LensesScreen
import com.tommihirvonen.exifnotes.util.State
import kotlinx.coroutines.launch

@Composable
fun GearScreen(
    gearViewModel: GearViewModel,
    filmStocksViewModel: FilmStocksViewModel,
    onNavigateUp: () -> Unit,
    onEditCamera: (Camera?) -> Unit,
    onEditLens: (Lens?) -> Unit,
    onEditFilter: (Filter?) -> Unit,
    onEditFilmStock: (FilmStock?) -> Unit
) {
    val cameras = gearViewModel.cameras.collectAsState()
    val lenses = gearViewModel.lenses.collectAsState()
    val filters = gearViewModel.filters.collectAsState()
    val filmStocks = filmStocksViewModel.filmStocks.collectAsState()
    val filmStockSortMode = filmStocksViewModel.sortMode.collectAsState()
    var confirmDeleteCamera by remember { mutableStateOf<Camera?>(null) }
    var confirmDeleteLens by remember { mutableStateOf<Lens?>(null) }
    var confirmDeleteFilter by remember { mutableStateOf<Filter?>(null) }
    var confirmDeleteFilmStock by remember { mutableStateOf<FilmStock?>(null) }
    var showLensCompatibleCamerasDialog by remember { mutableStateOf<Lens?>(null) }
    var showLensCompatibleFiltersDialog by remember { mutableStateOf<Lens?>(null) }
    var showFilterCompatibleLensesDialog by remember { mutableStateOf<Filter?>(null) }
    var showFilterCompatibleCamerasDialog by remember { mutableStateOf<Filter?>(null) }
    GearContent(
        cameras = cameras.value,
        lenses = lenses.value,
        filters = filters.value,
        filmStocks = filmStocks.value,
        cameraCompatibleLensesProvider = { camera ->
            lenses.value.filter { lens -> camera.lensIds.contains(lens.id) }
        },
        cameraCompatibleFiltersProvider = { camera ->
            camera.lens?.let { fixedLens ->
                filters.value.filter { filter ->
                    fixedLens.filterIds.contains(filter.id)
                }
            } ?: emptyList()
        },
        lensCompatibleCamerasProvider = { lens ->
            when (val c = cameras.value) {
                is State.Success -> c.data.filter { camera -> lens.cameraIds.contains(camera.id) }
                else -> emptyList()
            }
        },
        lensCompatibleFiltersProvider = { lens ->
            filters.value.filter { filter -> lens.filterIds.contains(filter.id) }
        },
        filterCompatibleCamerasProvider = { filter ->
            when (val c = cameras.value) {
                is State.Success -> c.data.filter { camera ->
                    val lens = camera.lens
                    lens != null && filter.lensIds.contains(lens.id)
                }
                else -> emptyList()
            }
        },
        filterCompatibleLensesProvider = { filter ->
            lenses.value.filter { lens -> filter.lensIds.contains(lens.id) }
        },
        onNavigateUp = onNavigateUp,
        onEditCamera = onEditCamera,
        onDeleteCamera = { camera ->
            confirmDeleteCamera = camera
        },
        onEditCameraCompatibleLenses = { camera ->
            // TODO
        },
        onEditCameraCompatibleFilters = { camera ->
            // TODO
        },
        onEditLens = onEditLens,
        onDeleteLens = { lens ->
            confirmDeleteLens = lens
        },
        onEditLensCompatibleCameras = { lens ->
            showLensCompatibleCamerasDialog = lens
        },
        onEditLensCompatibleFilters = { lens ->
            showLensCompatibleFiltersDialog = lens
        },
        onEditFilter = onEditFilter,
        onDeleteFilter = { filter ->
            confirmDeleteFilter = filter
        },
        onEditFilterCompatibleLenses = { filter ->
            showFilterCompatibleLensesDialog = filter
        },
        onEditFilterCompatibleCameras = { filter ->
            showFilterCompatibleCamerasDialog = filter
        },
        onEditFilmStock = onEditFilmStock,
        onDeleteFilmStock = { filmStock ->
            confirmDeleteFilmStock = filmStock
        },
        filmStockSortMode = filmStockSortMode.value,
        onFilmStockSort = filmStocksViewModel::setSortMode,
        manufacturers = filmStocksViewModel.filteredManufacturers,
        isoValues = filmStocksViewModel.filteredIsoValues,
        filmStockFilters = filmStocksViewModel.filterSet,
        onFilmStockFiltersChanged = { filmStocksViewModel.filterSet = it }
    )
    when (val camera = confirmDeleteCamera) {
        is Camera -> AlertDialog(
            title = { Text(stringResource(R.string.ConfirmCameraDelete)) },
            text = {
                Column {
                    Text(camera.name)
                    val inUse = gearViewModel.isCameraInUse(camera)
                    if (inUse) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.CameraIsInUseConfirmation))
                    }
                }
            },
            onDismissRequest = { confirmDeleteCamera = null },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCamera = null }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteCamera = null
                        gearViewModel.deleteCamera(camera)
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            }
        )
    }
    when (val lens = confirmDeleteLens) {
        is Lens -> AlertDialog(
            title = { Text(stringResource(R.string.ConfirmLensDelete)) },
            text = {
                Column {
                    Text(lens.name)
                    val inUse = gearViewModel.isLensInUse(lens)
                    if (inUse) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.LensIsInUseConfirmation))
                    }
                }
            },
            onDismissRequest = { confirmDeleteLens = null },
            dismissButton = {
                TextButton(onClick = { confirmDeleteLens = null }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteLens = null
                        gearViewModel.deleteLens(lens)
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            }
        )
    }
    when (val filmStock = confirmDeleteFilmStock) {
        is FilmStock -> AlertDialog(
            title = { Text(stringResource(R.string.DeleteFilmStock)) },
            text = {
                Column {
                    Text(filmStock.name)
                    val inUse = filmStocksViewModel.isFilmStockInUse(filmStock)
                    if (inUse) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.FilmStockIsInUseConfirmation))
                    }
                }
            },
            onDismissRequest = { confirmDeleteFilmStock = null },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFilmStock = null }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteFilmStock = null
                        filmStocksViewModel.deleteFilmStock(filmStock)
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            }
        )
    }
    when (val filter = confirmDeleteFilter) {
        is Filter -> AlertDialog(
            title = { Text(stringResource(R.string.ConfirmFilterDelete)) },
            text = {
                Column {
                    Text(confirmDeleteFilter?.name ?: "")
                    val inUse = gearViewModel.isFilterInUse(filter)
                    if (inUse) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.FilterIsInUseDeleteAnyway))
                    }
                }
            },
            onDismissRequest = { confirmDeleteFilter = null },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFilter = null }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteFilter = null
                        gearViewModel.deleteFilter(filter)
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            }
        )
    }
    when (val lens = showLensCompatibleCamerasDialog) {
        is Lens -> LensSelectCompatibleCamerasDialog(
            gearViewModel = gearViewModel,
            lens = lens,
            onDismiss = { showLensCompatibleCamerasDialog = null }
        )
    }
    when (val lens = showLensCompatibleFiltersDialog) {
        is Lens -> LensSelectCompatibleFiltersDialog(
            gearViewModel = gearViewModel,
            lens = lens,
            onDismiss = { showLensCompatibleFiltersDialog = null }
        )
    }
    when (val filter = showFilterCompatibleLensesDialog) {
        is Filter -> FilterSelectCompatibleLensesDialog(
            gearViewModel = gearViewModel,
            filter = filter,
            onDismiss = { showFilterCompatibleLensesDialog = null }
        )
    }
    when (val filter = showFilterCompatibleCamerasDialog) {
        is Filter -> FilterSelectCompatibleCamerasDialog(
            gearViewModel = gearViewModel,
            filter = filter,
            onDismiss = { showFilterCompatibleCamerasDialog = null }
        )
    }
}

@Preview(widthDp = 600)
@Composable
private fun GearScreenLargePreview() {
    GearContent(
        cameras = State.Success(emptyList()),
        lenses = emptyList(),
        filters = emptyList(),
        filmStocks = emptyList(),
        cameraCompatibleLensesProvider = { _ -> emptyList() },
        cameraCompatibleFiltersProvider = { _ -> emptyList() },
        lensCompatibleCamerasProvider = { _ -> emptyList() },
        lensCompatibleFiltersProvider = { _ -> emptyList() },
        filterCompatibleCamerasProvider = { _ -> emptyList() },
        filterCompatibleLensesProvider = { _ -> emptyList() },
        onNavigateUp = {},
        onEditCamera = {},
        onDeleteCamera = {},
        onEditCameraCompatibleLenses = {},
        onEditCameraCompatibleFilters = {},
        onEditLens = {},
        onDeleteLens = {},
        onEditLensCompatibleCameras = {},
        onEditLensCompatibleFilters = {},
        onEditFilter = {},
        onDeleteFilter = {},
        onEditFilterCompatibleLenses = {},
        onEditFilterCompatibleCameras = {},
        onEditFilmStock = {},
        onDeleteFilmStock = {},
        filmStockSortMode = FilmStockSortMode.NAME,
        onFilmStockSort = {},
        manufacturers = emptyList(),
        isoValues = emptyList(),
        filmStockFilters = FilmStockFilterSet(),
        onFilmStockFiltersChanged = {}
    )
}

@Preview
@Composable
private fun GearScreenPreview() {
    GearContent(
        cameras = State.Success(emptyList()),
        lenses = emptyList(),
        filters = emptyList(),
        filmStocks = emptyList(),
        cameraCompatibleLensesProvider = { _ -> emptyList() },
        cameraCompatibleFiltersProvider = { _ -> emptyList() },
        lensCompatibleCamerasProvider = { _ -> emptyList() },
        lensCompatibleFiltersProvider = { _ -> emptyList() },
        filterCompatibleCamerasProvider = { _ -> emptyList() },
        filterCompatibleLensesProvider = { _ -> emptyList() },
        onNavigateUp = {},
        onEditCamera = {},
        onDeleteCamera = {},
        onEditCameraCompatibleLenses = {},
        onEditCameraCompatibleFilters = {},
        onEditLens = {},
        onDeleteLens = {},
        onEditLensCompatibleCameras = {},
        onEditLensCompatibleFilters = {},
        onEditFilter = {},
        onDeleteFilter = {},
        onEditFilterCompatibleLenses = {},
        onEditFilterCompatibleCameras = {},
        onEditFilmStock = {},
        onDeleteFilmStock = {},
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
private fun GearContent(
    cameras: State<List<Camera>>,
    lenses: List<Lens>,
    filters: List<Filter>,
    filmStocks: List<FilmStock>,
    cameraCompatibleLensesProvider: (Camera) -> (List<Lens>),
    cameraCompatibleFiltersProvider: (Camera) -> (List<Filter>),
    lensCompatibleCamerasProvider: (Lens) -> (List<Camera>),
    lensCompatibleFiltersProvider: (Lens) -> (List<Filter>),
    filterCompatibleCamerasProvider: (Filter) -> (List<Camera>),
    filterCompatibleLensesProvider: (Filter) -> (List<Lens>),
    onNavigateUp: () -> Unit,
    onEditCamera: (Camera?) -> Unit,
    onDeleteCamera: (Camera) -> Unit,
    onEditCameraCompatibleLenses: (Camera) -> Unit,
    onEditCameraCompatibleFilters: (Camera) -> Unit,
    onEditLens: (Lens?) -> Unit,
    onDeleteLens: (Lens) -> Unit,
    onEditLensCompatibleCameras: (Lens) -> Unit,
    onEditLensCompatibleFilters: (Lens) -> Unit,
    onEditFilter: (Filter?) -> Unit,
    onDeleteFilter: (Filter) -> Unit,
    onEditFilterCompatibleLenses: (Filter) -> Unit,
    onEditFilterCompatibleCameras: (Filter) -> Unit,
    onEditFilmStock: (FilmStock?) -> Unit,
    onDeleteFilmStock: (FilmStock) -> Unit,
    filmStockSortMode: FilmStockSortMode,
    onFilmStockSort: (FilmStockSortMode) -> Unit,
    manufacturers: List<String>,
    isoValues: List<Int>,
    filmStockFilters: FilmStockFilterSet,
    onFilmStockFiltersChanged: (FilmStockFilterSet) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val navigationItems = listOf(
        NavigationItem(
            selected = pagerState.currentPage == 0,
            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
            label = { Text(stringResource(R.string.CamerasNoCap)) },
            icon = { Icon(Icons.Outlined.CameraAlt, "") }
        ),
        NavigationItem(
            selected = pagerState.currentPage == 1,
            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
            label = { Text(stringResource(R.string.LensesNoCap)) },
            icon = { Icon(Icons.Outlined.Camera, "") }
        ),
        NavigationItem(
            selected = pagerState.currentPage == 2,
            onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
            label = { Text(stringResource(R.string.FiltersNoCap)) },
            icon = { Icon(Icons.Outlined.Circle, "") }
        ),
        NavigationItem(
            selected = pagerState.currentPage == 3,
            onClick = { scope.launch { pagerState.animateScrollToPage(3) } },
            label = { Text(stringResource(R.string.FilmStocksNoCap)) },
            icon = { Icon(Icons.Outlined.CameraRoll, "") }
        )
    )

    BoxWithConstraints {
        val maxWidth = maxWidth
        Row {
            if (maxWidth >= 600.dp) {
                NavigationRail {
                    for (item in navigationItems) {
                        NavigationRailItem(
                            selected = item.selected,
                            onClick = item.onClick,
                            icon = item.icon,
                            label = item.label
                        )
                    }
                }
            }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    GearTopAppBar(
                        scrollBehavior = scrollBehavior,
                        currentPagerPage = pagerState.currentPage,
                        onNavigateUp = onNavigateUp,
                        filmStockSortMode = filmStockSortMode,
                        onFilmStockSort = onFilmStockSort,
                        manufacturers = manufacturers,
                        isoValues = isoValues,
                        filmStockFilters = filmStockFilters,
                        onFilmStockFiltersChanged = onFilmStockFiltersChanged
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            when (pagerState.currentPage) {
                                0 -> onEditCamera(null)
                                1 -> onEditLens(null)
                                2 -> onEditFilter(null)
                                3 -> onEditFilmStock(null)
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Add, "")
                    }
                },
                bottomBar = {
                    if (maxWidth < 600.dp) {
                        NavigationBar {
                            for (item in navigationItems) {
                                NavigationBarItem(
                                    selected = item.selected,
                                    onClick = item.onClick,
                                    icon = item.icon,
                                    label = item.label
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                ) {
                    HorizontalPager(state = pagerState) { page ->
                        when (page) {
                            0 -> CamerasScreen(
                                cameras = cameras,
                                compatibleLensesProvider = cameraCompatibleLensesProvider,
                                compatibleFiltersProvider = cameraCompatibleFiltersProvider,
                                onEdit = onEditCamera,
                                onDelete = onDeleteCamera,
                                onEditCompatibleLenses = onEditCameraCompatibleLenses,
                                onEditCompatibleFilters = onEditCameraCompatibleFilters
                            )
                            1 -> LensesScreen(
                                lenses = lenses,
                                compatibleCamerasProvider = lensCompatibleCamerasProvider,
                                compatibleFiltersProvider = lensCompatibleFiltersProvider,
                                onEdit = onEditLens,
                                onDelete = onDeleteLens,
                                onEditCompatibleCameras = onEditLensCompatibleCameras,
                                onEditCompatibleFilters = onEditLensCompatibleFilters
                            )
                            2 -> FiltersScreen(
                                filters = filters,
                                compatibleCamerasProvider = filterCompatibleCamerasProvider,
                                compatibleLensesProvider = filterCompatibleLensesProvider,
                                onEdit = onEditFilter,
                                onDelete = onDeleteFilter,
                                onEditCompatibleLenses = onEditFilterCompatibleLenses,
                                onEditCompatibleCameras = onEditFilterCompatibleCameras
                            )
                            3 -> FilmStocksScreen(
                                filmStocks = filmStocks,
                                onEdit = onEditFilmStock,
                                onDelete = onDeleteFilmStock
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavigationItem(
    val label: @Composable () -> Unit,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit
)