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

package com.tommihirvonen.exifnotes.screens.location

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame

@Preview
@Composable
private fun LocationPickScreenPreview() {
    LocationPickScreen(
        frame = Frame(),
        onNavigateUp = {},
        onLocationConfirm = { _, _ -> }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickScreen(
    frame: Frame,
    onNavigateUp: () -> Unit,
    onLocationConfirm: (LatLng?, String?) -> Unit,
    locationPickViewModel: LocationPickViewModel = hiltViewModel()
) {
    val singapore = LatLng(1.35, 103.87)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }
    val markerState = rememberMarkerState(position = singapore)
    val snackbarState = remember { SnackbarHostState() }

    val searchText by locationPickViewModel.searchText.collectAsState()
    val expanded by locationPickViewModel.expanded.collectAsState()
    val countries by locationPickViewModel.countriesList.collectAsState()

    Scaffold(
        topBar = {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.SearchWEllipsis)) },
                leadingIcon = {
                    IconButton(
                        onClick = {
                            if (!expanded) {
                                onNavigateUp()
                            } else {
                                locationPickViewModel.onToggleExpanded()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                locationPickViewModel.onSearchTextChange("")
                            }
                        ) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    }
                },
                query = searchText,
                onQueryChange = locationPickViewModel::onSearchTextChange,
                onSearch = locationPickViewModel::onSearchTextChange,
                active = expanded,
                onActiveChange = { locationPickViewModel.onToggleExpanded() }
            ) {
                LazyColumn {
                    items(countries) { country ->
                        Text(
                            text = country,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Outlined.Check, "")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { innerPadding ->
        GoogleMap(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp, start = 0.dp, end = 0.dp)
        ) {
            Marker(
                state = markerState,
                title = "Singapore",
                snippet = "Marker in Singapore"
            )
        }
    }
    val snackMessage = stringResource(R.string.TapOnMap)
    LaunchedEffect(key1 = "") {
        snackbarState.showSnackbar(snackMessage)
    }
}