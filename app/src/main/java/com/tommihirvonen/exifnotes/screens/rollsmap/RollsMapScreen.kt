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

package com.tommihirvonen.exifnotes.screens.rollsmap

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import com.tommihirvonen.exifnotes.screens.DialogContent
import com.tommihirvonen.exifnotes.screens.MapTypeDropdownMenu
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.theme.Theme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel
import com.tommihirvonen.exifnotes.util.LoadState

@Composable
fun RollsMapScreen(
    onNavigateUp: () -> Unit,
    onFrameEdit: (Frame) -> Unit,
    themeViewModel: ThemeViewModel,
    mainViewModel: MainViewModel,
    rollsMapViewModel: RollsMapViewModel = hiltViewModel { factory: RollsMapViewModel.Factory ->
        val rolls = when (val state = mainViewModel.rolls.value) {
            is LoadState.Success -> state.data
            else -> emptyList()
        }
        factory.create(rolls)
    }
) {
    val theme = themeViewModel.theme.collectAsState()
    val darkTheme = when (theme.value) {
        is Theme.Light -> false
        is Theme.Dark -> true
        is Theme.Auto -> isSystemInDarkTheme()
    }

    val title by mainViewModel.toolbarSubtitle.collectAsState()
    val mapType by rollsMapViewModel.mapType.collectAsState()
    val allRolls by rollsMapViewModel.allRolls.collectAsState()
    val selectedRolls by rollsMapViewModel.selectedRolls.collectAsState()
    RollsMapContent(
        title = title,
        rolls = allRolls,
        selectedRolls = selectedRolls,
        isDarkTheme = darkTheme,
        myLocationEnabled = rollsMapViewModel.myLocationEnabled,
        mapType = mapType,
        onNavigateUp = onNavigateUp,
        onMapTypeChange = rollsMapViewModel::setMapType,
        onFrameEdit = onFrameEdit,
        onSelectedRollsChange = rollsMapViewModel::setSelectedRolls
    )
}

@Preview
@Composable
private fun RollsMapContentPreview() {
    RollsMapContent(
        title = "Title placeholder",
        rolls = emptyList(),
        selectedRolls = emptyList(),
        isDarkTheme = false,
        myLocationEnabled = false,
        mapType = MapType.NORMAL,
        onNavigateUp = {},
        onMapTypeChange = {},
        onFrameEdit = {},
        onSelectedRollsChange = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
private fun RollsMapContent(
    title: String,
    rolls: List<Roll>,
    selectedRolls: List<Pair<Roll, Bitmap>>,
    isDarkTheme: Boolean,
    myLocationEnabled: Boolean,
    mapType: MapType,
    onNavigateUp: () -> Unit,
    onMapTypeChange: (MapType) -> Unit,
    onFrameEdit: (Frame) -> Unit,
    onSelectedRollsChange: (List<Roll>) -> Unit
) {
    val context = LocalContext.current
    var mapTypeExpanded by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
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
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.size(40.dp),
                    shape = FloatingActionButtonDefaults.smallShape,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Outlined.FilterAlt, "")
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                for (pair in selectedRolls) {
                    val (roll, marker) = pair
                    val frames = roll.frames
                    for (frame in frames) {
                        val location = frame.location ?: continue
                        val markerState = remember(frames) {
                            MarkerState(location)
                        }
                        MarkerInfoWindowContent(
                            icon = BitmapDescriptorFactory.fromBitmap(marker),
                            state = markerState,
                            title = roll.name,
                            snippet = "#${frame.count}",
                            onInfoWindowClick = { _ ->
                                onFrameEdit(frame)
                            }
                        ) { _ ->
                            Column {
                                Text(roll.name ?: "", style = MaterialTheme.typography.titleMedium)
                                Text("#${frame.count}")
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
                }
                MapEffect(selectedRolls) { map ->
                    val frames = selectedRolls.flatMap { it.first.frames }
                    if (frames.none()) return@MapEffect
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
    if (showFilterDialog) {
        RollsFilterDialog(
            rolls = rolls,
            selectedRolls = selectedRolls,
            onDismiss = { showFilterDialog = false },
            onConfirm = onSelectedRollsChange
        )
    }
}

@Preview
@Composable
private fun RollsFilterDialogPreview() {
    val drawable = ContextCompat.getDrawable(LocalContext.current, R.drawable.ic_marker_red)!!
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val roll = Roll(name = "Test roll")
    RollsFilterDialog(
        rolls = listOf(roll),
        selectedRolls = listOf(roll to bitmap),
        onDismiss = {},
        onConfirm = {}
    )
}

@Composable
private fun RollsFilterDialog(
    rolls: List<Roll>,
    selectedRolls: List<Pair<Roll, Bitmap>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Roll>) -> Unit
) {
    val items = remember(selectedRolls) {
        rolls.associateWith { roll ->
                selectedRolls.any { it.first == roll }
            }
            .toList()
            .toMutableStateMap()
    }
    val list = remember(selectedRolls) {
        val nonSelected = rolls.filter { roll ->
            selectedRolls.none { it.first.id == roll.id }
        }
        selectedRolls
            .sortedBy { it.first.name?.lowercase() }
            .plus(
                nonSelected.sortedBy { it.name?.lowercase() }
                    .map { it to null }
            )
    }
    Dialog(onDismissRequest = onDismiss) {
        DialogContent {
            Column(modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(list) { pair ->
                        val (roll, marker) = pair
                        val selected = items[roll] ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    items[roll] = !selected
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        items[roll] = !selected
                                    }
                                )
                                Text(
                                    text = roll.name ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (marker != null) {
                                Image(
                                    modifier = Modifier.size(24.dp),
                                    bitmap = marker.asImageBitmap(),
                                    contentDescription = ""
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (items.none { it.value }) {
                        TextButton(
                            onClick = {
                                for (key in items.keys) {
                                    items[key] = true
                                }
                            }
                        ) {
                            Text(stringResource(R.string.SelectAll))
                        }
                    } else {
                        TextButton(
                            onClick = {
                                for (key in items.keys) {
                                    items[key] = false
                                }
                            }
                        ) {
                            Text(stringResource(R.string.DeselectAll))
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.Close))
                        }
                        TextButton(onClick = {
                            val selected = items.filter { it.value }.map { it.key }
                            onConfirm(selected)
                        }) {
                            Text(stringResource(R.string.Apply))
                        }
                    }
                }
            }
        }
    }
}