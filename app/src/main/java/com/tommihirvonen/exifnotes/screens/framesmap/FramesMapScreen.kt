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

package com.tommihirvonen.exifnotes.screens.framesmap

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.DefaultMapUiSettings
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.MarkerState
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.sortableDateTime
import com.tommihirvonen.exifnotes.screens.MapTypeDropdownMenu
import com.tommihirvonen.exifnotes.screens.frames.FramesViewModel
import com.tommihirvonen.exifnotes.theme.Theme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel
import com.tommihirvonen.exifnotes.util.LoadState

@Composable
fun FramesMapScreen(
    onNavigateUp: () -> Unit,
    onFrameEdit: (Frame) -> Unit,
    themeViewModel: ThemeViewModel,
    framesViewModel: FramesViewModel,
    framesMapViewModel: FramesMapViewModel = hiltViewModel()
) {
    val theme = themeViewModel.theme.collectAsState()
    val darkTheme = when (theme.value) {
        is Theme.Light -> false
        is Theme.Dark -> true
        is Theme.Auto -> isSystemInDarkTheme()
    }

    val roll by framesViewModel.roll.collectAsState()
    val framesLoadState by framesViewModel.frames.collectAsState()
    val frames = when (val f = framesLoadState) {
        is LoadState.Success -> f.data
        else -> emptyList()
    }
    val mapType by framesMapViewModel.mapType.collectAsState()
    FramesMapContent(
        roll = roll,
        frames = frames,
        isDarkTheme = darkTheme,
        myLocationEnabled = framesMapViewModel.myLocationEnabled,
        mapType = mapType,
        onNavigateUp = onNavigateUp,
        onMapTypeChange = framesMapViewModel::setMapType,
        onFrameEdit = onFrameEdit
    )
}

@Preview
@Composable
private fun FramesMapContentPreview() {
    FramesMapContent(
        roll = Roll(name = "Test roll"),
        frames = emptyList(),
        isDarkTheme = false,
        myLocationEnabled = false,
        mapType = MapType.NORMAL,
        onNavigateUp = {},
        onMapTypeChange = {},
        onFrameEdit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
private fun FramesMapContent(
    roll: Roll,
    frames: List<Frame>,
    isDarkTheme: Boolean,
    myLocationEnabled: Boolean,
    mapType: MapType,
    onNavigateUp: () -> Unit,
    onMapTypeChange: (MapType) -> Unit,
    onFrameEdit: (Frame) -> Unit
) {
    val context = LocalContext.current
    var mapTypeExpanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = roll.name ?: "",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = 60.dp)
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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val mapStyle = if (isDarkTheme) {
                MapStyleOptions(stringResource(R.string.style_json))
            } else {
                null
            }
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize(),
                properties = MapProperties(
                    isMyLocationEnabled = myLocationEnabled,
                    mapStyleOptions = mapStyle,
                    mapType = mapType
                ),
                uiSettings = DefaultMapUiSettings.copy(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                )
            ) {
                for (frame in frames) {
                    val location = frame.location ?: continue
                    val markerState = remember(frames) {
                        MarkerState(location)
                    }
                    MarkerInfoWindowContent(
                        state = markerState,
                        title = "#${frame.count}",
                        onInfoWindowClick = { _ ->
                            onFrameEdit(frame)
                        }
                    ) { _ ->
                        Column {
                            Text("#${frame.count}", style = MaterialTheme.typography.titleMedium)
                            Text(frame.date.sortableDateTime)
                            Text(frame.lens?.name ?: "")
                            Text(
                                text = frame.note ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontStyle = FontStyle.Italic,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(stringResource(R.string.ClickToEdit))
                        }
                    }
                }
                MapEffect { map ->
                    val builder = LatLngBounds.Builder()
                    frames.mapNotNull { it.location }.forEach(builder::include)
                    val bounds = builder.build()
                    val width = context.resources.displayMetrics.widthPixels
                    val height = context.resources.displayMetrics.heightPixels
                    val padding = (width * 0.12).toInt() // offset from edges of the map 12% of screen
                    // We use this command where the map's dimensions are specified.
                    // This is because on some devices, the map's layout may not have yet occurred
                    // (map size is 0).
                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)
                    map.moveCamera(cameraUpdate)
                }
            }
        }
    }
}