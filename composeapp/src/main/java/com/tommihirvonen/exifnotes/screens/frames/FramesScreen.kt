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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.util.State

@Composable
fun FramesScreen(
    rollId: Long,
    framesViewModel: FramesViewModel = hiltViewModel { factory: FramesViewModel.Factory ->
        factory.create(rollId)
    },
    onNavigateUp: () -> Unit
) {
    val rollFromModel = framesViewModel.roll.collectAsState()
    val frames = framesViewModel.frames.collectAsState()
    val selectedFrames = framesViewModel.selectedFrames.collectAsState()
    FramesContent(
        roll = rollFromModel.value,
        frames = frames.value,
        selectedFrames = selectedFrames.value,
        onFrameClick = { /*TODO*/ },
        toggleFrameSelection = framesViewModel::toggleFrameSelection,
        toggleFrameSelectionAll = framesViewModel::toggleFrameSelectionAll,
        toggleFrameSelectionNone = framesViewModel::toggleFrameSelectionNone,
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
        frames = State.Success(listOf(frame1, frame2)),
        selectedFrames = hashSetOf(),
        onFrameClick = {},
        toggleFrameSelection = {},
        toggleFrameSelectionAll = {},
        toggleFrameSelectionNone = {},
        onNavigateUp = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FramesContent(
    roll: Roll,
    frames: State<List<Frame>>,
    selectedFrames: HashSet<Frame>,
    onFrameClick: (Frame) -> Unit,
    toggleFrameSelection: (Frame) -> Unit,
    toggleFrameSelectionAll: () -> Unit,
    toggleFrameSelectionNone: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val actionModeEnabled = selectedFrames.isNotEmpty()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(roll.name ?: "")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Edit, "")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.AutoMirrored.Outlined.Label, "")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.FavoriteBorder, "")
                    }
                }
            )
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
                        onClick = { /*TODO*/ },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (frames is State.InProgress) {
            Column(
                modifier = Modifier
                    .padding(vertical = 48.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else if (frames is State.Success) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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