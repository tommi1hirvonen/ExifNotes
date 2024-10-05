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

package com.tommihirvonen.exifnotes.screens.gear.cameras

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Format
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.PartialIncrement
import com.tommihirvonen.exifnotes.screens.DropdownButton
import com.tommihirvonen.exifnotes.util.copy

@Composable
fun CameraEditScreen(
    cameraId: Long,
    onNavigateUp: () -> Unit,
    onEditFixedLens: () -> Unit,
    submitHandler: (Camera) -> Unit,
    cameraViewModel: CameraViewModel = hiltViewModel { factory: CameraViewModel.Factory ->
        factory.create(cameraId)
    }
) {
    val camera = cameraViewModel.camera.collectAsState()
    val shutterValues = cameraViewModel.shutterValues.collectAsState()
    val fixedLensSummary = cameraViewModel.fixedLensSummary.collectAsState()
    val makeError = cameraViewModel.makeError.collectAsState()
    val modelError = cameraViewModel.modelError.collectAsState()
    val shutterRangeError = cameraViewModel.shutterRangeError.collectAsState()
    CameraEditContent(
        isNewCamera = camera.value.id <= 0,
        camera = camera.value,
        shutterValues = shutterValues.value,
        fixedLensSummary = fixedLensSummary.value,
        makeError = makeError.value,
        modelError = modelError.value,
        shutterRangeError = shutterRangeError.value,
        onMakeChange = cameraViewModel::setMake,
        onModelChange = cameraViewModel::setModel,
        onSerialNumberChange = cameraViewModel::setSerialNumber,
        onShutterIncrementsChange = cameraViewModel::setShutterIncrements,
        onSetMinShutter = cameraViewModel::setMinShutter,
        onSetMaxShutter = cameraViewModel::setMaxShutter,
        onClearShutterRange = cameraViewModel::clearShutterRange,
        onSetExposureCompIncrements = cameraViewModel::setExposureCompIncrements,
        onSetFormat = cameraViewModel::setFormat,
        onNavigateUp = onNavigateUp,
        onEditFixedLens = onEditFixedLens,
        onClearFixedLens = { cameraViewModel.setLens(null) },
        onSubmit = {
            if (cameraViewModel.validate()) {
                submitHandler(cameraViewModel.camera.value)
                onNavigateUp()
            }
        }
    )
}

@Preview(widthDp = 800)
@Composable
private fun CameraEditContentPreview() {
    val camera = Camera(
        make = "Canon",
        model = "A-1",
        serialNumber = "123ASD456",
        shutterIncrements = Increment.Third,
        minShutter = "1/1000",
        maxShutter = "30\"",
        exposureCompIncrements = PartialIncrement.Third,
        format = Format.MM35
    )
    CameraEditContent(
        isNewCamera = true,
        camera = camera,
        shutterValues = emptyList(),
        fixedLensSummary = "Click to set",
        makeError = false,
        modelError = false,
        shutterRangeError = "Sample error",
        onMakeChange = {},
        onModelChange = {},
        onSerialNumberChange = {},
        onShutterIncrementsChange = {},
        onSetMaxShutter = {},
        onSetMinShutter = {},
        onClearShutterRange = {},
        onSetExposureCompIncrements = {},
        onSetFormat = {},
        onNavigateUp = {},
        onEditFixedLens = {},
        onClearFixedLens = {},
        onSubmit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraEditContent(
    isNewCamera: Boolean,
    camera: Camera,
    shutterValues: List<String>,
    fixedLensSummary: String,
    makeError: Boolean,
    modelError: Boolean,
    shutterRangeError: String,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSerialNumberChange: (String) -> Unit,
    onShutterIncrementsChange: (Increment) -> Unit,
    onSetMaxShutter: (String) -> Unit,
    onSetMinShutter: (String) -> Unit,
    onClearShutterRange: () -> Unit,
    onSetExposureCompIncrements: (PartialIncrement) -> Unit,
    onSetFormat: (Format) -> Unit,
    onNavigateUp: () -> Unit,
    onEditFixedLens: () -> Unit,
    onClearFixedLens: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var shutterIncrementsExpanded by remember { mutableStateOf(false) }
    var maxShutterExpanded by remember { mutableStateOf(false) }
    var minShutterExpanded by remember { mutableStateOf(false) }
    var exposureCompIncrementsExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }
    var showFixedLensInfo by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val title = stringResource(if (isNewCamera) R.string.AddNewCamera else R.string.EditCamera)
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
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .widthIn(min = 0.dp, max = 400.dp)
            ) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = camera.make ?: "",
                        onValueChange = onMakeChange,
                        label = { Text(stringResource(R.string.Make)) },
                        supportingText = { Text(stringResource(R.string.Required)) },
                        isError = makeError
                    )
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = camera.model ?: "",
                        onValueChange = onModelChange,
                        label = { Text(stringResource(R.string.Model)) },
                        supportingText = { Text(stringResource(R.string.Required)) },
                        isError = modelError
                    )
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = camera.serialNumber ?: "",
                        onValueChange = onSerialNumberChange,
                        label = { Text(stringResource(R.string.SerialNumber)) }
                    )
                }
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.ShutterSpeedIncrements),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenuBox(
                        expanded = shutterIncrementsExpanded,
                        onExpandedChange = { shutterIncrementsExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = camera.shutterIncrements.description(context),
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = shutterIncrementsExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = shutterIncrementsExpanded,
                            onDismissRequest = { shutterIncrementsExpanded = false }
                        ) {
                            Increment.entries.forEach { increment ->
                                DropdownMenuItem(
                                    text = { Text(increment.description(context)) },
                                    onClick = {
                                        onShutterIncrementsChange(increment)
                                        shutterIncrementsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.ShutterRange),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(0.5f)) {
                        Text(
                            text = stringResource(R.string.From),
                            style = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenuBox(
                            expanded = minShutterExpanded,
                            onExpandedChange = { minShutterExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                value = camera.minShutter ?: "",
                                isError = shutterRangeError.isNotEmpty(),
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = minShutterExpanded)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = minShutterExpanded,
                                onDismissRequest = { minShutterExpanded = false }
                            ) {
                                shutterValues.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            onSetMinShutter(value)
                                            minShutterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(0.5f)) {
                        Text(
                            text = stringResource(R.string.To),
                            style = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenuBox(
                            expanded = maxShutterExpanded,
                            onExpandedChange = { maxShutterExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                value = camera.maxShutter ?: "",
                                isError = shutterRangeError.isNotEmpty(),
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = maxShutterExpanded)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = maxShutterExpanded,
                                onDismissRequest = { maxShutterExpanded = false }
                            ) {
                                shutterValues.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            onSetMaxShutter(value)
                                            maxShutterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        FilledTonalIconButton(onClick = onClearShutterRange) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    }
                }
                if (shutterRangeError.isNotEmpty()) {
                    Text(
                        text = shutterRangeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.ExposureCompensationIncrements),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenuBox(
                        expanded = exposureCompIncrementsExpanded,
                        onExpandedChange = { exposureCompIncrementsExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = camera.exposureCompIncrements.description(context),
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = exposureCompIncrementsExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = exposureCompIncrementsExpanded,
                            onDismissRequest = { exposureCompIncrementsExpanded = false }
                        ) {
                            PartialIncrement.entries.forEach { increment ->
                                DropdownMenuItem(
                                    text = { Text(increment.description(context)) },
                                    onClick = {
                                        onSetExposureCompIncrements(increment)
                                        exposureCompIncrementsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.FixedLens),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DropdownButton(
                        modifier = Modifier.weight(1f),
                        text = fixedLensSummary,
                        onClick = onEditFixedLens
                    )
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        FilledTonalIconButton(onClick = onClearFixedLens) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    }
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        FilledTonalIconButton(onClick = { showFixedLensInfo = true }) {
                            Icon(Icons.Outlined.Info, "")
                        }
                    }
                }
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.DefaultFormat),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = camera.format.description(context),
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
                                        onSetFormat(format)
                                        formatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (showFixedLensInfo) {
                    AlertDialog(
                        text = { Text(stringResource(R.string.FixedLensHelp)) },
                        onDismissRequest = { showFixedLensInfo = false },
                        confirmButton = {
                            TextButton(onClick = { showFixedLensInfo = false }) {
                                Text(stringResource(R.string.Close))
                            }
                        }
                    )
                }
            }
        }
    }
}