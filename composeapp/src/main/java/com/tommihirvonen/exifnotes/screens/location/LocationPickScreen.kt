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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.util.readableCoordinates
import kotlinx.coroutines.launch

@Composable
fun LocationPickScreen(
    frame: Frame,
    onNavigateUp: () -> Unit,
    onLocationConfirm: (LatLng?, String?) -> Unit,
    locationPickViewModel: LocationPickViewModel =
        hiltViewModel { factory: LocationPickViewModel.Factory ->
            factory.create(frame)
        }
) {
    val scope = rememberCoroutineScope()
    val location by locationPickViewModel.location.collectAsState()
    val markerState by remember {
        derivedStateOf { location?.let { MarkerState(it) } }
    }
    val cameraPositionState = rememberCameraPositionState {
        val loc = location
        if (loc != null) {
            position = CameraPosition.fromLatLngZoom(loc, 15f)
        }
    }
    val address by locationPickViewModel.address.collectAsState()
    val errorText by locationPickViewModel.errorText.collectAsState()
    val isLoading by locationPickViewModel.isLoading.collectAsState()
    val searchText by locationPickViewModel.searchText.collectAsState()
    val expanded by locationPickViewModel.expanded.collectAsState()
    val countries by locationPickViewModel.countriesList.collectAsState()
    LocationPickScreenContent(
        cameraPositionState = cameraPositionState,
        markerState = markerState,
        address = address,
        errorText = errorText,
        isLoading = isLoading,
        searchText = searchText,
        expanded = expanded,
        countries = countries,
        onNavigateUp = onNavigateUp,
        onConfirm = {
            onLocationConfirm(location, address)
        },
        onLocationChange = { value ->
            scope.launch { locationPickViewModel.setLocation(value) }
        },
        onSearchTextChange = locationPickViewModel::onSearchTextChange,
        onSearchRequested = { query ->
            scope.launch {
                locationPickViewModel.submitQuery(query, cameraPositionState)
            }
            locationPickViewModel.onToggleExpanded()
        },
        onToggleSearchExpanded = {
            locationPickViewModel.onToggleExpanded()
        }
    )
}

@Preview
@Composable
private fun LocationPickScreenPreview() {
    LocationPickScreenContent(
        cameraPositionState = rememberCameraPositionState(),
        markerState = null,
        address = "",
        errorText = null,
        isLoading = true,
        searchText = "",
        expanded = false,
        countries = emptyList(),
        onNavigateUp = {},
        onConfirm = {},
        onLocationChange = {},
        onSearchTextChange = {},
        onSearchRequested = {},
        onToggleSearchExpanded = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickScreenContent(
    cameraPositionState: CameraPositionState,
    markerState: MarkerState?,
    address: String?,
    errorText: String?,
    isLoading: Boolean,
    searchText: String,
    expanded: Boolean,
    countries: List<String>,
    onNavigateUp: () -> Unit,
    onConfirm: () -> Unit,
    onLocationChange: (LatLng) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onSearchRequested: (String) -> Unit,
    onToggleSearchExpanded: () -> Unit,
) {

    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                                onToggleSearchExpanded()
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
                                onSearchTextChange("")
                            }
                        ) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    }
                },
                query = searchText,
                onQueryChange = onSearchTextChange,
                onSearch = onSearchRequested,
                active = expanded,
                onActiveChange = { onToggleSearchExpanded() }
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
            Box(
                modifier = Modifier.padding(bottom = 180.dp)
            ) {
                FloatingActionButton(onClick = onConfirm) {
                    Icon(Icons.Outlined.Check, "")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp, start = 0.dp, end = 0.dp),
                onMapClick = { location ->
                    onLocationChange(location)
                    scope.launch {
                        snackbarState.showSnackbar(location.readableCoordinates)
                    }
                }
            ) {
                if (markerState != null) {
                    Marker(
                        state = markerState
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .height(60.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val text = if (errorText.isNullOrEmpty()) {
                        address
                    } else {
                        errorText
                    }
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = text ?: "",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                        )
                    }
                }
            }
        }
    }
    val snackMessage = stringResource(R.string.TapOnMap)
    LaunchedEffect(key1 = "") {
        snackbarState.showSnackbar(snackMessage)
    }
}