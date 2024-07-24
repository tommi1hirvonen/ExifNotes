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

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramesTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    title: String,
    isFavorite: Boolean,
    onNavigateUp: () -> Unit,
    onEditRoll: () -> Unit,
    onEditLabels: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
            }
        },
        actions = {
            IconButton(onClick = onEditRoll) {
                Icon(Icons.Outlined.Edit, "")
            }
            IconButton(onClick = onEditLabels) {
                Icon(Icons.AutoMirrored.Outlined.Label, "")
            }
            val favoriteIcon = if (isFavorite)
                Icons.Outlined.Favorite
            else
                Icons.Outlined.FavoriteBorder
            IconButton(onClick = onToggleFavorite) {
                Icon(favoriteIcon, "")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramesActionModeAppBar(
    selectedFramesCount: Int,
    allFramesCount: Int,
    onDeselectAll: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit
) {
    TopAppBar(
        title = {
            val text = "$selectedFramesCount/$allFramesCount"
            Text(text)
        },
        navigationIcon = {
            IconButton(onClick = onDeselectAll) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
            }
        },
        actions = {
            TopAppBarActionMenu(
                selectedFramesCount = selectedFramesCount,
                onEdit = onEdit,
                onCopy = onCopy,
                onSelectAll = onSelectAll,
                onDelete = onDelete
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    )
}

@Composable
private fun TopAppBarActionMenu(
    selectedFramesCount: Int,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    Row {
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, "")
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Outlined.ContentCopy, "")
        }
        IconButton(onClick = onSelectAll) {
            Icon(Icons.Outlined.SelectAll, "")
        }
        IconButton(onClick = { showDeleteConfirmDialog = true }) {
            Icon(Icons.Outlined.DeleteOutline, "")
        }
    }
    if (showDeleteConfirmDialog) {
        val title = pluralStringResource(R.plurals.ConfirmFramesDelete, selectedFramesCount, selectedFramesCount)
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