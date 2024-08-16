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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.DefaultMapUiSettings
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.screens.MapTypeDropdownMenu
import com.tommihirvonen.exifnotes.theme.Theme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel
import kotlinx.coroutines.launch

@Composable
fun LocationPickScreen(
    frame: Frame?,
    onNavigateUp: () -> Unit,
    onLocationConfirm: (LatLng?, String?) -> Unit,
    themeViewModel: ThemeViewModel,
    locationPickViewModel: LocationPickViewModel =
        hiltViewModel { factory: LocationPickViewModel.Factory ->
            factory.create(frame)
        }
) {
    val theme = themeViewModel.theme.collectAsState()
    val darkTheme = when (theme.value) {
        is Theme.Light -> false
        is Theme.Dark -> true
        is Theme.Auto -> isSystemInDarkTheme()
    }

    val scope = rememberCoroutineScope()
    val location by locationPickViewModel.location.collectAsState()
    val markerState by remember {
        derivedStateOf { location?.let { MarkerState(it) } }
    }
    val cameraPositionState = rememberCameraPositionState {
        val loc = location
        if (loc != null) {
            position = CameraPosition.fromLatLngZoom(loc, 12f)
        }
    }
    val address by locationPickViewModel.address.collectAsState()
    val errorText by locationPickViewModel.errorText.collectAsState()
    val isLoading by locationPickViewModel.isLoadingAddress.collectAsState()
    val searchText by locationPickViewModel.searchText.collectAsState()
    val expanded by locationPickViewModel.searchExpanded.collectAsState()
    val isQuerying by locationPickViewModel.isQueryingSuggestions.collectAsState()
    val suggestions by locationPickViewModel.suggestions.collectAsState()
    val mapType by locationPickViewModel.mapType.collectAsState()
    LocationPickScreenContent(
        isDarkTheme = darkTheme,
        myLocationEnabled = locationPickViewModel.myLocationEnabled,
        mapType = mapType,
        cameraPositionState = cameraPositionState,
        markerState = markerState,
        address = address,
        errorText = errorText,
        isLoadingAddress = isLoading,
        searchText = searchText,
        searchExpanded = expanded,
        isQueryingSuggestions = isQuerying,
        suggestions = suggestions,
        onNavigateUp = onNavigateUp,
        onConfirm = {
            onLocationConfirm(location, address)
        },
        onLocationChange = { value ->
            scope.launch { locationPickViewModel.setLocation(value) }
        },
        onSearchTextChange = { text ->
            locationPickViewModel.onSearchTextChange(text, cameraPositionState)
        },
        onQuerySearchRequested = { query ->
            scope.launch {
                locationPickViewModel.submitQuery(query, cameraPositionState)
            }
            locationPickViewModel.onToggleExpanded()
        },
        onPlacesSearchRequested = { prediction ->
            scope.launch {
                locationPickViewModel.submitPrediction(prediction, cameraPositionState)
            }
            locationPickViewModel.onToggleExpanded()
        },
        onToggleSearchExpanded = {
            locationPickViewModel.onToggleExpanded()
        },
        onMyLocationClick = {
            locationPickViewModel.setMyLocation(cameraPositionState)
        },
        onMapTypeChange = locationPickViewModel::setMapType
    )
}

@Preview
@Composable
private fun LocationPickScreenPreview() {
    LocationPickScreenContent(
        isDarkTheme = false,
        myLocationEnabled = true,
        mapType = MapType.NORMAL,
        cameraPositionState = rememberCameraPositionState(),
        markerState = null,
        address = "Test Address",
        errorText = null,
        isLoadingAddress = true,
        searchText = "",
        searchExpanded = false,
        isQueryingSuggestions = true,
        suggestions = emptyList(),
        onNavigateUp = {},
        onConfirm = {},
        onLocationChange = {},
        onSearchTextChange = {},
        onQuerySearchRequested = {},
        onPlacesSearchRequested = {},
        onToggleSearchExpanded = {},
        onMyLocationClick = {},
        onMapTypeChange = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickScreenContent(
    isDarkTheme: Boolean,
    myLocationEnabled: Boolean,
    mapType: MapType,
    cameraPositionState: CameraPositionState,
    markerState: MarkerState?,
    address: String?,
    errorText: String?,
    isLoadingAddress: Boolean,
    searchText: String,
    searchExpanded: Boolean,
    isQueryingSuggestions: Boolean,
    suggestions: List<AutocompletePrediction>,
    onNavigateUp: () -> Unit,
    onConfirm: () -> Unit,
    onLocationChange: (LatLng) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onQuerySearchRequested: (String) -> Unit,
    onPlacesSearchRequested: (AutocompletePrediction) -> Unit,
    onToggleSearchExpanded: () -> Unit,
    onMyLocationClick: () -> Unit,
    onMapTypeChange: (MapType) -> Unit
) {
    val snackbarState = remember { SnackbarHostState() }
    var mapTypeExpanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            val searchBarPadding by animateDpAsState(
                targetValue = if (searchExpanded) 0.dp else 16.dp,
                label = "Search bar padding"
            )
            val placeholder = searchText.ifEmpty { stringResource(R.string.SearchWEllipsis) }
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(searchBarPadding),
                placeholder = { Text(placeholder) },
                leadingIcon = {
                    IconButton(
                        onClick = {
                            if (!searchExpanded) {
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
                onSearch = onQuerySearchRequested,
                active = searchExpanded,
                onActiveChange = { onToggleSearchExpanded() }
            ) {
                if (isQueryingSuggestions) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = suggestions,
                        key = { it.placeId }
                    ) { suggestion ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlacesSearchRequested(suggestion) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = suggestion.getPrimaryText(null).toString(),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = suggestion.getSecondaryText(null).toString(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = { mapTypeExpanded = true },
                    modifier = Modifier.size(40.dp),
                    shape = FloatingActionButtonDefaults.smallShape,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Outlined.Map, "")
                }
                MapTypeDropdownMenu(
                    expanded = mapTypeExpanded,
                    selectedMapType = mapType,
                    onMapTypeSelected = { type ->
                        mapTypeExpanded = false
                        onMapTypeChange(type)
                    },
                    onDismiss = { mapTypeExpanded = false }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = onMyLocationClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Outlined.GpsFixed, "")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = onConfirm,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Outlined.Check, "")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val mapStyle = if (isDarkTheme) {
                MapStyleOptions(stringResource(R.string.style_json))
            } else {
                null
            }
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = myLocationEnabled,
                    mapStyleOptions = mapStyle,
                    mapType = mapType
                ),
                uiSettings = DefaultMapUiSettings.copy(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                ),
                contentPadding = PaddingValues(
                    top = 0.dp,
                    bottom = 80.dp + padding.calculateBottomPadding(),
                    start = 0.dp,
                    end = 0.dp
                ),
                onMapClick = { location ->
                    onLocationChange(location)
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
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .height(60.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
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
                    if (isLoadingAddress) {
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