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

package com.tommihirvonen.exifnotes.screens.frames

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FrameSortMode
import com.tommihirvonen.exifnotes.di.export.RollExportOption
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog

@Composable
fun FramesBottomAppBar(
    sortMode: FrameSortMode,
    onFabClick: () -> Unit,
    onSortModeChange: (FrameSortMode) -> Unit,
    onRollShare: (List<RollExportOption>) -> Unit,
    onRollExport: (List<RollExportOption>) -> Unit,
    onNavigateToMap: () -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var shareMenuExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    BottomAppBar(
        actions = {
            IconButton(onClick = { sortMenuExpanded = !sortMenuExpanded }) {
                Icon(Icons.AutoMirrored.Outlined.Sort, "")
            }
            IconButton(onClick = onNavigateToMap) {
                Icon(Icons.Outlined.Map, "")
            }
            IconButton(onClick = { shareMenuExpanded = true }) {
                Icon(Icons.Outlined.Share, "")
            }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                Text(stringResource(R.string.SortBy), modifier = Modifier.padding(horizontal = 8.dp))
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.FrameCount)) },
                    onClick = {
                        onSortModeChange(FrameSortMode.FrameCount)
                        sortMenuExpanded = false
                    },
                    trailingIcon = {
                        RadioButton(
                            selected = sortMode == FrameSortMode.FrameCount,
                            onClick = {
                                onSortModeChange(FrameSortMode.FrameCount)
                                sortMenuExpanded = false
                            }
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.Date)) },
                    onClick = {
                        onSortModeChange(FrameSortMode.Date)
                        sortMenuExpanded = false
                    },
                    trailingIcon = {
                        RadioButton(
                            selected = sortMode == FrameSortMode.Date,
                            onClick = {
                                onSortModeChange(FrameSortMode.Date)
                                sortMenuExpanded = false
                            }
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.FStop)) },
                    onClick = {
                        onSortModeChange(FrameSortMode.FStop)
                        sortMenuExpanded = false
                    },
                    trailingIcon = {
                        RadioButton(
                            selected = sortMode == FrameSortMode.FStop,
                            onClick = {
                                onSortModeChange(FrameSortMode.FStop)
                                sortMenuExpanded = false
                            }
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ShutterSpeed)) },
                    onClick = {
                        onSortModeChange(FrameSortMode.ShutterSpeed)
                        sortMenuExpanded = false
                    },
                    trailingIcon = {
                        RadioButton(
                            selected = sortMode == FrameSortMode.ShutterSpeed,
                            onClick = {
                                onSortModeChange(FrameSortMode.ShutterSpeed)
                                sortMenuExpanded = false
                            }
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.Lens)) },
                    onClick = {
                        onSortModeChange(FrameSortMode.Lens)
                        sortMenuExpanded = false
                    },
                    trailingIcon = {
                        RadioButton(
                            selected = sortMode == FrameSortMode.Lens,
                            onClick = {
                                onSortModeChange(FrameSortMode.Lens)
                                sortMenuExpanded = false
                            }
                        )
                    }
                )
            }
            DropdownMenu(expanded = shareMenuExpanded, onDismissRequest = { shareMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ExportOrShare)) },
                    onClick = {
                        shareMenuExpanded = false
                        showShareDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Share, "")
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ExportToDevice)) },
                    onClick = {
                        shareMenuExpanded = false
                        showExportDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.SdStorage, "")
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "")
            }
        }
    )
    if (showExportDialog) {
        val title = stringResource(R.string.FilesToExport)
        val items = RollExportOption.entries.associateWith { false }
        MultiChoiceDialog(
            title = title,
            initialItems = items,
            itemText = { it.toString() },
            sortItemsBy = { it.ordinal },
            onDismiss = { showExportDialog = false },
            onConfirm = onRollExport
        )
    }
    if (showShareDialog) {
        val title = stringResource(R.string.FilesToShare)
        val items = RollExportOption.entries.associateWith { false }
        MultiChoiceDialog(
            title = title,
            initialItems = items,
            itemText = { it.toString() },
            sortItemsBy = { it.ordinal },
            onDismiss = { showShareDialog = false },
            onConfirm = onRollShare
        )
    }
}