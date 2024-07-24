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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.util.LoadState

@Composable
fun FramesScreen(
    rollId: Long,
    onEditRoll: (Roll) -> Unit,
    mainViewModel: MainViewModel,
    framesViewModel: FramesViewModel = hiltViewModel { factory: FramesViewModel.Factory ->
        factory.create(rollId)
    },
    onNavigateUp: () -> Unit
) {
    val roll = framesViewModel.roll.collectAsState()
    val frames = framesViewModel.frames.collectAsState()
    val selectedFrames = framesViewModel.selectedFrames.collectAsState()
    FramesContent(
        roll = roll.value,
        frames = frames.value,
        selectedFrames = selectedFrames.value,
        onFrameClick = { /*TODO*/ },
        onFabClick = { /*TODO*/ },
        toggleFrameSelection = framesViewModel::toggleFrameSelection,
        toggleFrameSelectionAll = framesViewModel::toggleFrameSelectionAll,
        toggleFrameSelectionNone = framesViewModel::toggleFrameSelectionNone,
        onEditRoll = { onEditRoll(roll.value) },
        onToggleFavorite = {
            val favorite = roll.value.favorite
            val updated = roll.value.copy(favorite = !favorite)
            mainViewModel.submitRoll(updated)
            framesViewModel.setRoll(updated)
        },
        onEditLabels = { /*TODO*/ },
        onDelete = {
            selectedFrames.value.forEach(framesViewModel::deleteFrame)
        },
        onEdit = { /*TODO*/ },
        onCopy = { /*TODO*/ },
        onNavigateUp = onNavigateUp
    )
}

@Preview
@Composable
private fun FramesContentPreview() {
    val roll = Roll(name = "Test roll")
    val frame1 = Frame(
        id = 1,
        count = 1,
        shutter = "1/250",
        aperture = "2.8",
        lens = Lens(make = "Canon", model = "FD 28mm f/2.8")
    )
    val frame2 = Frame(
        id = 2,
        count = 2,
        shutter = "1/1000",
        aperture = "1.8",
        lens = Lens(make = "Canon", model = "FD 50mm f/1.8")
    )
    FramesContent(
        roll = roll,
        frames = LoadState.Success(listOf(frame1, frame2)),
        selectedFrames = hashSetOf(),
        onFrameClick = {},
        onFabClick = {},
        toggleFrameSelection = {},
        toggleFrameSelectionAll = {},
        toggleFrameSelectionNone = {},
        onEditRoll = {},
        onNavigateUp = {},
        onToggleFavorite = {},
        onEditLabels = {},
        onDelete = {},
        onEdit = {},
        onCopy = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FramesContent(
    roll: Roll,
    frames: LoadState<List<Frame>>,
    selectedFrames: HashSet<Frame>,
    onFrameClick: (Frame) -> Unit,
    onFabClick: () -> Unit,
    toggleFrameSelection: (Frame) -> Unit,
    toggleFrameSelectionAll: () -> Unit,
    toggleFrameSelectionNone: () -> Unit,
    onEditRoll: () -> Unit,
    onNavigateUp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditLabels: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit
) {
    val actionModeEnabled = selectedFrames.isNotEmpty()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val allFramesCount = when (frames) {
        is LoadState.Success -> frames.data.size
        else -> 0
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FramesTopAppBar(
                scrollBehavior = scrollBehavior,
                title = roll.name ?: "",
                isFavorite = roll.favorite,
                onNavigateUp = onNavigateUp,
                onEditRoll = onEditRoll,
                onEditLabels = onEditLabels,
                onToggleFavorite = onToggleFavorite
            )
            AnimatedVisibility(
                visible = actionModeEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FramesActionModeAppBar(
                    selectedFramesCount = selectedFrames.size,
                    allFramesCount = allFramesCount,
                    onDeselectAll = toggleFrameSelectionNone,
                    onSelectAll = toggleFrameSelectionAll,
                    onDelete = onDelete,
                    onEdit = onEdit,
                    onCopy = onCopy)
            }
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.AutoMirrored.Outlined.Sort, "")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Map, "")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Share, "")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.MoreVert, "")
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
        }
    ) { innerPadding ->
        if (frames is LoadState.InProgress) {
            Column(
                modifier = Modifier
                    .padding(vertical = 48.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else if (frames is LoadState.Success) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (frames.data.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier.alpha(0.6f),
                                text = stringResource(R.string.NoFrames),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
                items(
                    items = frames.data,
                    key = { it.id }
                ) { frame ->
                    FrameCard(
                        frame = frame,
                        selected = selectedFrames.contains(frame),
                        onClick = {
                            if (actionModeEnabled) {
                                toggleFrameSelection(frame)
                                return@FrameCard
                            }
                            onFrameClick(frame)
                        },
                        onLongClick = { toggleFrameSelection(frame) }
                    )
                }
            }
        }
    }
}