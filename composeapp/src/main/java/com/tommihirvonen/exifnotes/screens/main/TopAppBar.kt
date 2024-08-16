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

package com.tommihirvonen.exifnotes.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LabelOff
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.RollSortMode

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AppBarPreview() {
    MainTopAppBar(
        subtitle = "Active rolls",
        rollSortMode = RollSortMode.DATE,
        onRollSortModeSet = {},
        scrollBehavior = null,
        navigationIcon = {
            Icon(Icons.Outlined.Menu, "")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    subtitle: String,
    rollSortMode: RollSortMode,
    onRollSortModeSet: (RollSortMode) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    navigationIcon: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name))
                Text(subtitle, fontSize = 16.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
            }
        },
        navigationIcon = {
            navigationIcon()
        },
        actions = {
            TopAppBarMenu(
                sortMode = rollSortMode,
                onRollSortModeSet = onRollSortModeSet
            )
        }
    )
}

@Preview
@Composable
private fun ActionModeAppBarPreview() {
    ActionModeAppBar(
        selectedRollsCount = 3,
        allRollsCount = 10,
        onDeselectAll = {},
        onSelectAll = {},
        onDelete = {},
        onEdit = {},
        onArchive = {},
        onUnarchive = {},
        onFavorite = {},
        onUnfavorite = {},
        onAddLabels = {},
        onRemoveLabels = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionModeAppBar(
    selectedRollsCount: Int,
    allRollsCount: Int,
    onDeselectAll: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onAddLabels: () -> Unit,
    onRemoveLabels: () -> Unit
) {
    TopAppBar(
        title = {
            val text = "$selectedRollsCount/$allRollsCount"
            Text(text)
        },
        navigationIcon = {
            IconButton(onClick = onDeselectAll) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
            }
        },
        actions = {
            TopAppBarActionMenu(
                selectedRollsCount = selectedRollsCount,
                onEdit = onEdit,
                onSelectAll = onSelectAll,
                onDelete = onDelete,
                onArchive = onArchive,
                onUnarchive = onUnarchive,
                onFavorite = onFavorite,
                onUnfavorite = onUnfavorite,
                onAddLabels = onAddLabels,
                onRemoveLabels = onRemoveLabels
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    )
}

@Composable
private fun TopAppBarMenu(
    sortMode: RollSortMode,
    onRollSortModeSet: (RollSortMode) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(Icons.AutoMirrored.Outlined.Sort, "")
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        Text(stringResource(R.string.SortRollsBy), modifier = Modifier.padding(horizontal = 8.dp))
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Date)) },
            onClick = {
                onRollSortModeSet(RollSortMode.DATE)
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Outlined.CalendarToday, "") },
            trailingIcon = {
                RadioButton(
                    selected = sortMode == RollSortMode.DATE,
                    onClick = {
                        onRollSortModeSet(RollSortMode.DATE)
                        showMenu = false
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = {
                onRollSortModeSet(RollSortMode.NAME)
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, "") },
            trailingIcon = {
                RadioButton(
                    selected = sortMode == RollSortMode.NAME,
                    onClick = {
                        onRollSortModeSet(RollSortMode.NAME)
                        showMenu = false
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Camera)) },
            onClick = {
                onRollSortModeSet(RollSortMode.CAMERA)
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Outlined.CameraAlt, "") },
            trailingIcon = {
                RadioButton(
                    selected = sortMode == RollSortMode.CAMERA,
                    onClick = {
                        onRollSortModeSet(RollSortMode.CAMERA)
                        showMenu = false
                    }
                )
            }
        )
    }
}

@Composable
private fun TopAppBarActionMenu(
    selectedRollsCount: Int,
    onEdit: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onAddLabels: () -> Unit,
    onRemoveLabels: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    Row {
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, "")
        }
        IconButton(onClick = onSelectAll) {
            Icon(Icons.Outlined.SelectAll, "")
        }
        IconButton(onClick = { showDeleteConfirmDialog = true }) {
            Icon(Icons.Outlined.DeleteOutline, "")
        }
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Outlined.MoreVert, "")
        }
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Archive)) },
            leadingIcon = { Icon(Icons.Outlined.Archive, "") },
            onClick = {
                showMenu = false
                onArchive()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Unarchive)) },
            leadingIcon = { Icon(Icons.Outlined.Unarchive, "") },
            onClick = {
                showMenu = false
                onUnarchive()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.AddToFavorites)) },
            leadingIcon = { Icon(Icons.Filled.Favorite, "") },
            onClick = {
                showMenu = false
                onFavorite()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.RemoveFromFavorites)) },
            leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, "") },
            onClick = {
                showMenu = false
                onUnfavorite()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.AddLabels)) },
            leadingIcon = { Icon(Icons.Outlined.NewLabel, "") },
            onClick = {
                showMenu = false
                onAddLabels()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.RemoveLabels)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.LabelOff, "") },
            onClick = {
                showMenu = false
                onRemoveLabels()
            }
        )
    }
    if (showDeleteConfirmDialog) {
        val title = pluralStringResource(R.plurals.ConfirmRollsDelete, selectedRollsCount, selectedRollsCount)
        AlertDialog(
            title = { Text(title) },
            onDismissRequest = { showDeleteConfirmDialog = false },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.OK))
                }
            }
        )
    }
}