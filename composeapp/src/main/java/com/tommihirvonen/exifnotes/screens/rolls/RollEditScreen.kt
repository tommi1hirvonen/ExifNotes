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

package com.tommihirvonen.exifnotes.screens.rolls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Format
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.screens.DateTimeButtonCombo
import com.tommihirvonen.exifnotes.screens.DropdownButton
import com.tommihirvonen.exifnotes.screens.frames.FramesViewModel
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.SelectFilmStockDialog
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.util.copy
import com.tommihirvonen.exifnotes.util.mapNonUniqueToNameWithSerial
import java.time.LocalDateTime

@Composable
fun RollEditScreen(
    rollId: Long,
    onNavigateUp: () -> Unit,
    onEditFilmStock: (FilmStock?) -> Unit,
    onEditCamera: (Camera?) -> Unit,
    mainViewModel: MainViewModel,
    framesViewModel: FramesViewModel,
    rollViewModel: RollViewModel = hiltViewModel { factory: RollViewModel.Factory ->
        factory.create(rollId)
    }
) {
    RollEditScreen(
        rollId = rollId,
        onNavigateUp = onNavigateUp,
        onEditFilmStock = onEditFilmStock,
        onEditCamera = onEditCamera,
        mainViewModel = mainViewModel,
        rollViewModel = rollViewModel,
        afterSubmit = {
            framesViewModel.setRoll(rollViewModel.roll.value)
        }
    )
}

@Composable
fun RollEditScreen(
    rollId: Long,
    onNavigateUp: () -> Unit,
    onEditFilmStock: (FilmStock?) -> Unit,
    onEditCamera: (Camera?) -> Unit,
    mainViewModel: MainViewModel,
    rollViewModel: RollViewModel = hiltViewModel { factory: RollViewModel.Factory ->
        factory.create(rollId)
    },
    afterSubmit: () -> Unit = {}
) {
    val cameras = rollViewModel.cameras.collectAsState()
    val roll = rollViewModel.roll.collectAsState()
    val nameError = rollViewModel.nameError.collectAsState()
    RollEditContent(
        isNewRoll = rollId <= 0,
        roll = roll.value,
        cameras = cameras.value,
        pushPullValues = rollViewModel.pushPullValues,
        nameError = nameError.value,
        onNameChange = rollViewModel::setName,
        onDateChange = rollViewModel::setDate,
        onUnloadedChange = rollViewModel::setUnloaded,
        onDevelopedChange = rollViewModel::setDeveloped,
        onFilmStockChange = rollViewModel::setFilmStock,
        onCameraChange = rollViewModel::setCamera,
        onIsoChange = rollViewModel::setIso,
        onPushPullChange = rollViewModel::setPushPull,
        onFormatChange = rollViewModel::setFormat,
        onNoteChange = rollViewModel::setNote,
        onNavigateUp = onNavigateUp,
        onEditFilmStock = onEditFilmStock,
        onEditCamera = onEditCamera,
        onSubmit = {
            if (rollViewModel.validate()) {
                mainViewModel.submitRoll(rollViewModel.roll.value)
                afterSubmit()
                onNavigateUp()
            }
        }
    )
}

@Preview
@Composable
private fun RollEditContentPreview() {
    val roll = Roll(
        name = "Test roll",
        date = LocalDateTime.now(),
        iso = 400,
        format = Format.MM35
    )
    RollEditContent(
        isNewRoll = true,
        roll = roll,
        cameras = emptyList(),
        pushPullValues = emptyList(),
        nameError = false,
        onNameChange = {},
        onDateChange = {},
        onUnloadedChange = {},
        onDevelopedChange = {},
        onFilmStockChange = {},
        onCameraChange = {},
        onIsoChange = {},
        onPushPullChange = {},
        onFormatChange = {},
        onNoteChange = {},
        onNavigateUp = {},
        onEditFilmStock = {},
        onEditCamera = {},
        onSubmit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RollEditContent(
    isNewRoll: Boolean,
    roll: Roll,
    cameras: List<Camera>,
    pushPullValues: List<String>,
    nameError: Boolean,
    onNameChange: (String) -> Unit,
    onDateChange: (LocalDateTime) -> Unit,
    onUnloadedChange: (LocalDateTime?) -> Unit,
    onDevelopedChange: (LocalDateTime?) -> Unit,
    onFilmStockChange: (FilmStock?) -> Unit,
    onCameraChange: (Camera?) -> Unit,
    onIsoChange: (String) -> Unit,
    onPushPullChange: (String) -> Unit,
    onFormatChange: (Format) -> Unit,
    onNoteChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onEditFilmStock: (FilmStock?) -> Unit,
    onEditCamera: (Camera?) -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val camerasWithUniqueNames = remember(cameras) {
        cameras.mapNonUniqueToNameWithSerial()
    }
    val cameraName = remember(roll.camera) {
        camerasWithUniqueNames.firstOrNull { it.first.id == roll.camera?.id }?.second
            ?: roll.camera?.name
            ?: ""
    }
    var showFilmStockDialog by remember { mutableStateOf(false) }
    var camerasExpanded by remember { mutableStateOf(false) }
    var pushPullExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val title = stringResource(if (isNewRoll) R.string.AddNewRoll else R.string.EditRoll)
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
                actions = {
                    val padding = ButtonDefaults.ContentPadding.copy(start = 18.dp)
                    Button(
                        contentPadding = padding,
                        onClick = onSubmit
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.Check,
                            contentDescription = ""
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.Save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = roll.name ?: "",
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.Name)) },
                    supportingText = { Text(stringResource(R.string.Required)) },
                    isError = nameError
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.FilmStock),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filmStockText = roll.filmStock?.name ?: stringResource(R.string.ClickToSet)
                DropdownButton(
                    modifier = Modifier.weight(1f),
                    text = filmStockText,
                    onClick = { showFilmStockDialog = true }
                )
                Box(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    if (roll.filmStock != null) {
                        FilledTonalIconButton(onClick = { onFilmStockChange(null) }) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    } else {
                        FilledTonalIconButton(onClick = { onEditFilmStock(null) }) {
                            Icon(Icons.Outlined.Add, "")
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.LoadedOn),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateTimeButtonCombo(
                    modifier = Modifier.weight(1f, fill = false),
                    dateTime = roll.date,
                    onDateTimeSet = onDateChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier
                    .padding(vertical = 4.dp)
                    .alpha(0f)) {
                    IconButton(onClick = { }) { }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.UnloadedOn),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateTimeButtonCombo(
                    modifier = Modifier.weight(1f, fill = false),
                    dateTime = roll.unloaded,
                    onDateTimeSet = onUnloadedChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    FilledTonalIconButton(onClick = { onUnloadedChange(null) }) {
                        Icon(Icons.Outlined.Clear, "")
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.DevelopedOn),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateTimeButtonCombo(
                    modifier = Modifier.weight(1f, fill = false),
                    dateTime = roll.developed,
                    onDateTimeSet = onDevelopedChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    FilledTonalIconButton(onClick = { onDevelopedChange(null) }) {
                        Icon(Icons.Outlined.Clear, "")
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.Camera),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1f),
                    expanded = camerasExpanded,
                    onExpandedChange = { camerasExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = cameraName,
                        onValueChange = {},
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = camerasExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = camerasExpanded,
                        onDismissRequest = { camerasExpanded = false }
                    ) {
                        camerasWithUniqueNames.forEach { (cam, cameraName) ->
                            DropdownMenuItem(
                                text = { Text(cameraName) },
                                onClick = {
                                    onCameraChange(cam)
                                    camerasExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    if (roll.camera != null) {
                        FilledTonalIconButton(onClick = { onCameraChange(null) }) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    } else {
                        FilledTonalIconButton(onClick = { onEditCamera(null) }) {
                            Icon(Icons.Outlined.Add, "")
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ISO),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = roll.iso.toString(),
                        onValueChange = onIsoChange,
                        supportingText = { Text(stringResource(R.string.Required)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.PushPull),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenuBox(
                        expanded = pushPullExpanded,
                        onExpandedChange = { pushPullExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            value = roll.pushPull ?: "",
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = pushPullExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = pushPullExpanded,
                            onDismissRequest = { pushPullExpanded = false }
                        ) {
                            pushPullValues.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v) },
                                    onClick = {
                                        onPushPullChange(v)
                                        pushPullExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.Format),
                    style = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenuBox(
                    expanded = formatExpanded,
                    onExpandedChange = { formatExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = roll.format.description(context),
                        onValueChange = {},
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = formatExpanded,
                        onDismissRequest = { formatExpanded = false }
                    ) {
                        Format.entries.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.description(context)) },
                                onClick = {
                                    onFormatChange(format)
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = roll.note ?: "",
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.DescriptionOrNote)) }
                )
            }
        }
    }
    if (showFilmStockDialog) {
        SelectFilmStockDialog(
            onDismiss = { showFilmStockDialog = false },
            onSelect = { selected ->
                showFilmStockDialog = false
                onFilmStockChange(selected)
            }
        )
    }
}