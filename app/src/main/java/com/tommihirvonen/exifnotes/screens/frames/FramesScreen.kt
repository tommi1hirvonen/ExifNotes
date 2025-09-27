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

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePickerState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.FrameSortMode
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.LightSource
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.localDateTimeOrNull
import com.tommihirvonen.exifnotes.di.export.RollExportOptionData
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.SimpleItemsDialog
import com.tommihirvonen.exifnotes.screens.TimePickerDialog
import com.tommihirvonen.exifnotes.screens.frameedit.CustomApertureDialog
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.util.LoadState
import com.tommihirvonen.exifnotes.util.description
import com.tommihirvonen.exifnotes.util.epochMilliseconds
import com.tommihirvonen.exifnotes.util.mapNonUniqueToNameWithSerial
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramesScreen(
    rollId: Long,
    onEditRoll: (Roll) -> Unit,
    onEditFrame: (Frame?, Frame?, Int) -> Unit,
    onNavigateToMap: (Roll) -> Unit,
    onNavigateToLocationPick: () -> Unit,
    mainViewModel: MainViewModel,
    framesViewModel: FramesViewModel = hiltViewModel { factory: FramesViewModel.Factory ->
        factory.create(rollId)
    },
    onNavigateUp: () -> Unit
) {
    val labels = mainViewModel.labels.collectAsState()
    val roll = framesViewModel.roll.collectAsState()
    val framesLoadState = framesViewModel.frames.collectAsState()
    val selectedFrames = framesViewModel.selectedFrames.collectAsState()
    val sortMode = framesViewModel.frameSortMode.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showLabels by remember { mutableStateOf(false) }
    var exportOptions by remember { mutableStateOf(emptyList<RollExportOptionData>()) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }

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
        frames = framesLoadState.value,
        selectedFrames = selectedFrames.value,
        sortMode = sortMode.value,
        onFrameClick = { frame ->
            onEditFrame(frame, null, 0)
        },
        onFabClick = {
            val frames = when (val state = framesLoadState.value) {
                is LoadState.Success -> state.data
                else -> emptyList()
            }
            val frameCount = frames.maxOfOrNull(Frame::count)?.plus(1) ?: 1
            val previousFrame = frames.maxByOrNull(Frame::id)
            onEditFrame(null, previousFrame, frameCount)
        },
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
            framesViewModel.deleteFrames(selectedFrames.value)
        },
        onEdit = { showBatchEditDialog = true },
        onCopy = { showCopyDialog = true },
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
    if (showCopyDialog) {
        var value by remember { mutableStateOf(selectedFrames.value.size.toString()) }
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCopyDialog = false
                        selectedFrames.value.forEach { frame ->
                            val copy = frame.copy(
                                id = -1,
                                count = frame.count + (value.toIntOrNull() ?: 0)
                            )
                            framesViewModel.submitFrame(copy)
                        }
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.EditCopiedFramesCountsBy))
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        modifier = Modifier.width(100.dp),
                        value = value,
                        onValueChange = {
                            value = if (it.isEmpty() || it == "-") {
                                it
                            } else {
                                when (it.toIntOrNull()) {
                                    null -> value
                                    else -> it
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
            }
        )
    }

    var showFrameCountsDialog by remember { mutableStateOf(false) }
    var showDateTimeDialog by remember { mutableStateOf(false) }
    var showLensDialog by remember { mutableStateOf(false) }
    var showApertureDialog by remember { mutableStateOf(false) }
    var showShutterDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var showFocalLengthDialog by remember { mutableStateOf(false) }
    var showExposureCompDialog by remember { mutableStateOf(false) }
    var showLightSourceDialog by remember { mutableStateOf(false) }

    if (showBatchEditDialog) {
        SimpleItemsDialog(
            title = {
                Text(
                    pluralStringResource(
                        R.plurals.BatchEditFramesTitle,
                        selectedFrames.value.size,
                        selectedFrames.value.size
                    )
                )
            },
            items = FramesBatchEditOption.entries,
            itemText = { option -> Text(option.description) },
            onDismiss = { showBatchEditDialog = false },
            onSelected = { option ->
                showBatchEditDialog = false
                when (option) {
                    FramesBatchEditOption.FrameCounts -> { showFrameCountsDialog = true }
                    FramesBatchEditOption.DateAndTime -> { showDateTimeDialog = true }
                    FramesBatchEditOption.Lens -> { showLensDialog = true }
                    FramesBatchEditOption.Aperture -> { showApertureDialog = true }
                    FramesBatchEditOption.ShutterSpeed -> { showShutterDialog = true }
                    FramesBatchEditOption.Filters -> { showFiltersDialog = true }
                    FramesBatchEditOption.FocalLength -> { showFocalLengthDialog = true }
                    FramesBatchEditOption.ExposureCompensation -> { showExposureCompDialog = true }
                    FramesBatchEditOption.Location -> onNavigateToLocationPick()
                    FramesBatchEditOption.LightSource -> { showLightSourceDialog = true }
                    FramesBatchEditOption.ReverseFrameCounts -> {
                        // Create a list of frame counts in reversed order
                        val frameCountsReversed = selectedFrames.value
                            .sortedBy { it.count }
                            .map(Frame::count)
                            .reversed()
                        selectedFrames.value
                            .sortedBy { it.count }
                            .zip(frameCountsReversed) { frame, count ->
                                framesViewModel.submitFrame(frame.copy(count = count))
                            }
                    }
                }
            }
        )
    }
    if (showFrameCountsDialog) {
        FrameCountsDialog(
            onDismiss = { showFrameCountsDialog = false },
            onConfirm = { value ->
                showFrameCountsDialog = false
                selectedFrames.value.forEach { frame ->
                    framesViewModel.submitFrame(frame.copy(count = frame.count + value))
                }
            }
        )
    }
    if (showDateTimeDialog) {
        val currentDateTime = LocalDateTime.now()
        val dateState = remember {
            DatePickerState(
                locale = CalendarLocale.getDefault(),
                initialSelectedDateMillis = currentDateTime.epochMilliseconds
            )
        }
        val is24HourFormat = DateFormat.is24HourFormat(LocalContext.current)
        val timeState = remember {
            TimePickerState(
                initialHour = currentDateTime.hour,
                initialMinute = currentDateTime.minute,
                is24Hour = is24HourFormat
            )
        }
        var showDatePicker by remember { mutableStateOf(true) }
        var showTimePicker by remember { mutableStateOf(false) }
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDateTimeDialog = false },
                dismissButton = {
                    TextButton(onClick = { showDateTimeDialog = false }) {
                        Text(stringResource(R.string.Cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                            showTimePicker = true
                        }
                    ) {
                        Text(stringResource(R.string.OK))
                    }
                }
            ) {
                DatePicker(state = dateState)
            }
        }
        if (showTimePicker) {
            TimePickerDialog(
                timePickerState = timeState,
                onDismiss = { showDateTimeDialog = false },
                onConfirm = {
                    showDateTimeDialog = false
                    val date = dateState.selectedDateMillis?.let(::localDateTimeOrNull)
                        ?: LocalDateTime.now()
                    val dateTime = LocalDateTime.of(
                        date.year,
                        date.monthValue,
                        date.dayOfMonth,
                        timeState.hour,
                        timeState.minute
                    )
                    selectedFrames.value.forEach { frame ->
                        framesViewModel.submitFrame(frame.copy(date = dateTime))
                    }
                }
            )
        }
    }
    if (showLensDialog) {
        val noLensText = stringResource(R.string.NoLens)
        val items = remember {
            framesViewModel.lenses
                .mapNonUniqueToNameWithSerial()
                .plus(null  to noLensText)
        }
        SimpleItemsDialog(
            items = items,
            itemText = { Text(it.second) },
            onDismiss = { showLensDialog = false },
            onSelected = { value ->
                showLensDialog = false
                selectedFrames.value.forEach { frame ->
                    framesViewModel.submitFrame(frame.copy(lens = value.first))
                }
            }
        )
    }
    if (showApertureDialog) {
        CustomApertureDialog(
            onDismiss = { showApertureDialog = false },
            onConfirm = { value ->
                showApertureDialog = false
                selectedFrames.value.forEach { frame ->
                    framesViewModel.submitFrame(frame.copy(aperture = value))
                }
            }
        )
    }
    if (showShutterDialog) {
        val items = remember {
            roll.value.camera?.shutterSpeedValues(context)?.withIndex()?.toList()
                ?: Camera.defaultShutterSpeedValues(context).withIndex().toList()
        }
        SimpleItemsDialog(
            items = items,
            itemText = { Text(it.value) },
            onDismiss = { showShutterDialog = false },
            onSelected = { value ->
                showShutterDialog = false
                selectedFrames.value.forEach { frame ->
                    val shutter = if (value.index == 0) null else value.value
                    framesViewModel.submitFrame(frame.copy(shutter = shutter))
                }
            }
        )
    }
    if (showFiltersDialog) {
        val items = remember {
            framesViewModel.filters.associateWith { false }
        }
        MultiChoiceDialog(
            initialItems = items,
            itemText = { it.name },
            sortItemsBy = { it.name },
            onDismiss = { showFiltersDialog = false },
            onConfirm = { filters ->
                showFiltersDialog = false
                selectedFrames.value.forEach { frame ->
                    framesViewModel.submitFrame(frame.copy(filters = filters))
                }
            }
        )
    }
    if (showFocalLengthDialog) {
        FocalLengthDialog(
            onDismiss = { showFocalLengthDialog = false },
            onConfirm = { value ->
                showFocalLengthDialog = false
                selectedFrames.value.forEach { frame ->
                    framesViewModel.submitFrame(frame.copy(focalLength = value))
                }
            }
        )
    }
    if (showExposureCompDialog) {
        val items = remember {
            roll.value.camera?.exposureCompValues(context)?.toList()
                ?: Camera.defaultExposureCompValues(context).toList()
        }
        SimpleItemsDialog(
            items = items,
            itemText = { Text(it) },
            onDismiss = { showExposureCompDialog = false },
            onSelected = { value ->
                showExposureCompDialog = false
                selectedFrames.value.forEach { frame ->
                    framesViewModel.submitFrame(frame.copy(exposureComp = value))
                }
            }
        )
    }
    if (showLightSourceDialog) {
        val items = remember { LightSource.entries }
        SimpleItemsDialog(
            items = items,
            itemText = { Text(it.description) },
            onDismiss = { showLightSourceDialog = false },
            onSelected = { value ->
                showLightSourceDialog = false
                selectedFrames.value.forEach { frame ->
                    framesViewModel.submitFrame(frame.copy(lightSource = value))
                }
            }
        )
    }
}

@Preview(widthDp = 800)
@Composable
private fun FramesContentPreview() {
    val roll = Roll(name = "Test roll")
    val frame1 = Frame(
        rollId = 0,
        id = 1,
        count = 1,
        shutter = "1/250",
        aperture = "2.8",
        lens = Lens(make = "Canon", model = "FD 28mm f/2.8")
    )
    val frame2 = Frame(
        rollId = 0,
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
        sortMode = FrameSortMode.FrameCount,
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
    onRollShare: (List<RollExportOptionData>) -> Unit,
    onRollExport: (List<RollExportOptionData>) -> Unit,
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
    BackHandler(enabled = actionModeEnabled) { toggleFrameSelectionNone() }
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
                    .padding(innerPadding)
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                        modifier = Modifier
                            .widthIn(500.dp, 500.dp)
                            .animateItem(),
                        frame = frame,
                        selected = selectedFrames.any { it.id == frame.id },
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

@Preview
@Composable
fun FocalLengthDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (Int) -> Unit = {}
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.toIntOrNull() != null,
                onClick = {
                    if (value.toIntOrNull() != null) {
                        onConfirm(value.toIntOrNull() ?: 0)
                    }
                }
            ) {
                Text(stringResource(R.string.OK))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.EditFocalLength))
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    modifier = Modifier.width(100.dp),
                    value = value,
                    onValueChange = {
                        value = if (it.isEmpty()) {
                            it
                        } else {
                            when (it.toIntOrNull()) {
                                null -> value
                                else -> it
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        }
    )
}

@Preview
@Composable
fun FrameCountsDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (Int) -> Unit = {}
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.toIntOrNull() != null,
                onClick = {
                    if (value.toIntOrNull() != null) {
                        onConfirm(value.toIntOrNull() ?: 0)
                    }
                }
            ) {
                Text(stringResource(R.string.OK))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.EditFrameCountsBy))
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    modifier = Modifier.width(100.dp),
                    value = value,
                    onValueChange = {
                        value = if (it.isEmpty() || it == "-") {
                            it
                        } else {
                            when (it.toIntOrNull()) {
                                null -> value
                                else -> it
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        }
    )
}