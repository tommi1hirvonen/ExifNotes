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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.FrameSortMode
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.di.export.RollExportOption
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.util.LoadState
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun FramesScreen(
    rollId: Long,
    onEditRoll: (Roll) -> Unit,
    onEditFrame: (Frame?) -> Unit,
    onNavigateToMap: (Roll) -> Unit,
    mainViewModel: MainViewModel,
    framesViewModel: FramesViewModel = hiltViewModel { factory: FramesViewModel.Factory ->
        factory.create(rollId)
    },
    onNavigateUp: () -> Unit
) {
    val labels = mainViewModel.labels.collectAsState()
    val roll = framesViewModel.roll.collectAsState()
    val frames = framesViewModel.frames.collectAsState()
    val selectedFrames = framesViewModel.selectedFrames.collectAsState()
    val sortMode = framesViewModel.frameSortMode.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showLabels by remember { mutableStateOf(false) }
    var exportOptions by remember { mutableStateOf(emptyList<RollExportOption>()) }

    val exportSuccessText = stringResource(R.string.ExportedFilesSuccessfully)
    val exportFailureText = stringResource(R.string.ErrorExporting)
    val pickExportLocation = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            framesViewModel.exportFiles(uri, exportOptions)
            scope.launch { snackbarHostState.showSnackbar(exportSuccessText) }
        } catch (e: IOException) {
            e.printStackTrace()
            scope.launch { snackbarHostState.showSnackbar(exportFailureText) }
        }
    }

    FramesContent(
        roll = roll.value,
        frames = frames.value,
        selectedFrames = selectedFrames.value,
        sortMode = sortMode.value,
        onFrameClick = onEditFrame,
        onFabClick = { onEditFrame(null) },
        toggleFrameSelection = framesViewModel::toggleFrameSelection,
        toggleFrameSelectionAll = framesViewModel::toggleFrameSelectionAll,
        toggleFrameSelectionNone = framesViewModel::toggleFrameSelectionNone,
        onSortModeChange = framesViewModel::setSortMode,
        onRollShare = { options ->
            val intent = framesViewModel.createShareFilesIntent(options)
            context.startActivity(intent)
        },
        onRollExport = { options ->
            exportOptions = options
            pickExportLocation.launch(null)
        },
        onNavigateToMap = { onNavigateToMap(roll.value) },
        onEditRoll = { onEditRoll(roll.value) },
        onToggleFavorite = {
            val favorite = roll.value.favorite
            val updated = roll.value.copy(favorite = !favorite)
            mainViewModel.submitRoll(updated)
            framesViewModel.setRoll(updated)
        },
        onEditLabels = { showLabels = true },
        onDelete = {
            selectedFrames.value.forEach(framesViewModel::deleteFrame)
        },
        onEdit = { /*TODO*/ },
        onCopy = { /*TODO*/ },
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbarHostState
    )
    if (showLabels) {
        val initialItems = labels.value.associateWith { label ->
            roll.value.labels.any { it.id == label.id }
        }
        val title = stringResource(R.string.ManageLabels)
        MultiChoiceDialog(
            title = title,
            initialItems = initialItems,
            itemText = { it.name },
            sortItemsBy = { it.name },
            onDismiss = { showLabels = false },
            onConfirm = { selectedLabels ->
                val updated = roll.value.copy(labels = selectedLabels)
                mainViewModel.submitRoll(updated)
                framesViewModel.setRoll(updated)
            }
        )
    }
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
        sortMode = FrameSortMode.FRAME_COUNT,
        onFrameClick = {},
        onFabClick = {},
        toggleFrameSelection = {},
        toggleFrameSelectionAll = {},
        toggleFrameSelectionNone = {},
        onSortModeChange = {},
        onRollShare = {},
        onRollExport = {},
        onNavigateToMap = {},
        onEditRoll = {},
        onNavigateUp = {},
        onToggleFavorite = {},
        onEditLabels = {},
        onDelete = {},
        onEdit = {},
        onCopy = {},
        snackbarHostState = SnackbarHostState()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FramesContent(
    roll: Roll,
    frames: LoadState<List<Frame>>,
    selectedFrames: HashSet<Frame>,
    sortMode: FrameSortMode,
    onFrameClick: (Frame) -> Unit,
    onFabClick: () -> Unit,
    toggleFrameSelection: (Frame) -> Unit,
    toggleFrameSelectionAll: () -> Unit,
    toggleFrameSelectionNone: () -> Unit,
    onSortModeChange: (FrameSortMode) -> Unit,
    onRollShare: (List<RollExportOption>) -> Unit,
    onRollExport: (List<RollExportOption>) -> Unit,
    onNavigateToMap: () -> Unit,
    onEditRoll: () -> Unit,
    onNavigateUp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditLabels: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    snackbarHostState: SnackbarHostState
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
            FramesBottomAppBar(
                sortMode = sortMode,
                onFabClick = onFabClick,
                onSortModeChange = onSortModeChange,
                onRollShare = onRollShare,
                onRollExport = onRollExport,
                onNavigateToMap = onNavigateToMap
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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