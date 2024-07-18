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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CameraRoll
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.gear.cameras.CamerasScreen
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStocksScreen
import com.tommihirvonen.exifnotes.screens.gear.filters.FiltersScreen
import com.tommihirvonen.exifnotes.screens.gear.lenses.LensesScreen
import com.tommihirvonen.exifnotes.util.State
import kotlinx.coroutines.launch

@Composable
fun GearScreen(
    gearViewModel: GearViewModel,
    onNavigateUp: () -> Unit,
    onEditFilmStock: (FilmStock) -> Unit
) {
    val cameras = gearViewModel.cameras.collectAsState()
    val lenses = gearViewModel.lenses.collectAsState()
    val filters = gearViewModel.filters.collectAsState()
    GearContent(
        cameras = cameras.value,
        lenses = lenses.value,
        filters = filters.value,
        onNavigateUp = onNavigateUp,
        onEditFilmStock = onEditFilmStock
    )
}

@Preview(widthDp = 600)
@Composable
private fun GearScreenLargePreview() {
    GearContent(
        cameras = State.Success(emptyList()),
        lenses = emptyList(),
        filters = emptyList(),
        onNavigateUp = {},
        onEditFilmStock = {}
    )
}

@Preview
@Composable
private fun GearScreenPreview() {
    GearContent(
        cameras = State.Success(emptyList()),
        lenses = emptyList(),
        filters = emptyList(),
        onNavigateUp = {},
        onEditFilmStock = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GearContent(
    cameras: State<List<Camera>>,
    lenses: List<Lens>,
    filters: List<Filter>,
    onNavigateUp: () -> Unit,
    onEditFilmStock: (FilmStock) -> Unit
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
                    TopAppBar(
                        title = { Text(stringResource(R.string.Gear)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { /*TODO*/ }) {
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
                                cameras = cameras
                            )
                            1 -> LensesScreen(
                                lenses = lenses
                            )
                            2 -> FiltersScreen(
                                filters = filters
                            )
                            3 -> FilmStocksScreen(
                                onFilmStockClick = onEditFilmStock
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