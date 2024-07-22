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

package com.tommihirvonen.exifnotes.screens.gear.lenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.DropdownButton
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.util.copy

@Composable
fun FixedLensEditScreen(
    lens: Lens,
    onCancel: () -> Unit,
    onSubmit: (Lens) -> Unit,
    lensViewModel: FixedLensViewModel = hiltViewModel { factory: FixedLensViewModel.Factory ->
        factory.create(lens.copy())
    }
) {
    val make = lensViewModel.make.collectAsState()
    val model = lensViewModel.model.collectAsState()
    val serialNumber = lensViewModel.serialNumber.collectAsState()
    val minFocalLength = lensViewModel.minFocalLength.collectAsState()
    val maxFocalLength = lensViewModel.maxFocalLength.collectAsState()
    val apertureIncrements = lensViewModel.apertureIncrements.collectAsState()
    val minAperture = lensViewModel.minAperture.collectAsState()
    val maxAperture = lensViewModel.maxAperture.collectAsState()
    val customApertureValues = lensViewModel.customApertureValues.collectAsState()
    val apertureValues = lensViewModel.apertureValues.collectAsState()
    val makeError = lensViewModel.makeError.collectAsState()
    val modelError = lensViewModel.modelError.collectAsState()
    val apertureRangeError = lensViewModel.apertureRangeError.collectAsState()
    val minFocalLengthError = lensViewModel.minFocalLengthError.collectAsState()
    val maxFocalLengthError = lensViewModel.maxFocalLengthError.collectAsState()
    val title = stringResource(R.string.FixedLens)
    LensEditContent(
        title = title,
        isFixedLens = true,
        make = make.value ?: "",
        model = model.value ?: "",
        serialNumber = serialNumber.value ?: "",
        minFocalLength = minFocalLength.value,
        maxFocalLength = maxFocalLength.value,
        apertureIncrements = apertureIncrements.value,
        minAperture = minAperture.value ?: "",
        maxAperture = maxAperture.value ?: "",
        customApertureValues = customApertureValues.value,
        apertureValues = apertureValues.value,
        makeError = makeError.value,
        modelError = modelError.value,
        apertureRangeError = apertureRangeError.value,
        minFocalLengthError = minFocalLengthError.value,
        maxFocalLengthError = maxFocalLengthError.value,
        onMakeChange = lensViewModel::setMake,
        onModelChange = lensViewModel::setModel,
        onSerialNumberChange = lensViewModel::setSerialNumber,
        onApertureIncrementsChange = lensViewModel::setApertureIncrements,
        onSetMaxAperture = lensViewModel::setMaxAperture,
        onSetMinAperture = lensViewModel::setMinAperture,
        onClearApertureRange = lensViewModel::clearApertureRange,
        onSetMinFocalLength = lensViewModel::setMinFocalLength,
        onSetMaxFocalLength = lensViewModel::setMaxFocalLength,
        onSetCustomApertureValues = lensViewModel::setCustomApertureValues,
        onNavigateUp = onCancel,
        onSubmit = {
            if (lensViewModel.validate()) {
                onSubmit(lensViewModel.lens)
            }
        }
    )
}

@Composable
fun InterchangeableLensEditScreen(
    lensId: Long,
    onNavigateUp: () -> Unit,
    gearViewModel: GearViewModel,
    lensViewModel: InterchangeableLensViewModel = hiltViewModel { factory: InterchangeableLensViewModel.Factory ->
        factory.create(lensId)
    }
) {
    val make = lensViewModel.make.collectAsState()
    val model = lensViewModel.model.collectAsState()
    val serialNumber = lensViewModel.serialNumber.collectAsState()
    val minFocalLength = lensViewModel.minFocalLength.collectAsState()
    val maxFocalLength = lensViewModel.maxFocalLength.collectAsState()
    val apertureIncrements = lensViewModel.apertureIncrements.collectAsState()
    val minAperture = lensViewModel.minAperture.collectAsState()
    val maxAperture = lensViewModel.maxAperture.collectAsState()
    val customApertureValues = lensViewModel.customApertureValues.collectAsState()
    val apertureValues = lensViewModel.apertureValues.collectAsState()
    val makeError = lensViewModel.makeError.collectAsState()
    val modelError = lensViewModel.modelError.collectAsState()
    val apertureRangeError = lensViewModel.apertureRangeError.collectAsState()
    val minFocalLengthError = lensViewModel.minFocalLengthError.collectAsState()
    val maxFocalLengthError = lensViewModel.maxFocalLengthError.collectAsState()
    val title = stringResource(if (lensId <= 0) R.string.AddNewLens else R.string.EditLens)
    LensEditContent(
        title = title,
        isFixedLens = false,
        make = make.value ?: "",
        model = model.value ?: "",
        serialNumber = serialNumber.value ?: "",
        minFocalLength = minFocalLength.value,
        maxFocalLength = maxFocalLength.value,
        apertureIncrements = apertureIncrements.value,
        minAperture = minAperture.value ?: "",
        maxAperture = maxAperture.value ?: "",
        customApertureValues = customApertureValues.value,
        apertureValues = apertureValues.value,
        makeError = makeError.value,
        modelError = modelError.value,
        apertureRangeError = apertureRangeError.value,
        minFocalLengthError = minFocalLengthError.value,
        maxFocalLengthError = maxFocalLengthError.value,
        onMakeChange = lensViewModel::setMake,
        onModelChange = lensViewModel::setModel,
        onSerialNumberChange = lensViewModel::setSerialNumber,
        onApertureIncrementsChange = lensViewModel::setApertureIncrements,
        onSetMaxAperture = lensViewModel::setMaxAperture,
        onSetMinAperture = lensViewModel::setMinAperture,
        onClearApertureRange = lensViewModel::clearApertureRange,
        onSetMinFocalLength = lensViewModel::setMinFocalLength,
        onSetMaxFocalLength = lensViewModel::setMaxFocalLength,
        onSetCustomApertureValues = lensViewModel::setCustomApertureValues,
        onNavigateUp = onNavigateUp,
        onSubmit = {
            if (lensViewModel.validate()) {
                gearViewModel.submitLens(lensViewModel.lens)
                onNavigateUp()
            }
        }
    )
}

@Preview
@Composable
private fun LensEditContentPreview() {
    LensEditContent(
        title = "Add new lens",
        isFixedLens = false,
        make = "Canon",
        model = "FD 28mm f/2.8",
        serialNumber = "123ASD456",
        minFocalLength = 28,
        maxFocalLength = 28,
        apertureIncrements = Increment.THIRD,
        minAperture = "22",
        maxAperture = "2.8",
        customApertureValues = listOf(2.8f, 5.6f),
        apertureValues = emptyList(),
        makeError = false,
        modelError = false,
        apertureRangeError = "Sample error",
        minFocalLengthError = "Sample error",
        maxFocalLengthError = "Sample error",
        onMakeChange = {},
        onModelChange = {},
        onSerialNumberChange = {},
        onApertureIncrementsChange = {},
        onSetMaxAperture = {},
        onSetMinAperture = {},
        onClearApertureRange = {},
        onSetMinFocalLength = {},
        onSetMaxFocalLength = {},
        onSetCustomApertureValues = {},
        onNavigateUp = {},
        onSubmit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensEditContent(
    title: String,
    isFixedLens: Boolean,
    make: String,
    model: String,
    serialNumber: String,
    minFocalLength: Int,
    maxFocalLength: Int,
    apertureIncrements: Increment,
    minAperture: String,
    maxAperture: String,
    customApertureValues: List<Float>,
    apertureValues: List<String>,
    makeError: Boolean,
    modelError: Boolean,
    apertureRangeError: String,
    minFocalLengthError: String,
    maxFocalLengthError: String,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSerialNumberChange: (String) -> Unit,
    onApertureIncrementsChange: (Increment) -> Unit,
    onSetMaxAperture: (String) -> Unit,
    onSetMinAperture: (String) -> Unit,
    onClearApertureRange: () -> Unit,
    onSetMinFocalLength: (String) -> Unit,
    onSetMaxFocalLength: (String) -> Unit,
    onSetCustomApertureValues: (List<Float>) -> Unit,
    onNavigateUp: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var apertureIncrementsExpanded by remember { mutableStateOf(false) }
    var maxApertureExpanded by remember { mutableStateOf(false) }
    var minApertureExpanded by remember { mutableStateOf(false) }
    var showCustomApertureValuesInfo by remember { mutableStateOf(false) }
    var showAddCustomApertureValueDialog by remember { mutableStateOf(false) }
    var showRemoveCustomApertureValuesDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
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
            if (!isFixedLens) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = make,
                        onValueChange = onMakeChange,
                        label = { Text(stringResource(R.string.Make)) },
                        supportingText = { Text(stringResource(R.string.Required)) },
                        isError = makeError
                    )
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = model,
                        onValueChange = onModelChange,
                        label = { Text(stringResource(R.string.Model)) },
                        supportingText = { Text(stringResource(R.string.Required)) },
                        isError = modelError
                    )
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = serialNumber,
                        onValueChange = onSerialNumberChange,
                        label = { Text(stringResource(R.string.SerialNumber)) }
                    )
                }
            }
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.ApertureIncrements),
                    style = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenuBox(
                    expanded = apertureIncrementsExpanded,
                    onExpandedChange = { apertureIncrementsExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = apertureIncrements.description(context),
                        onValueChange = {},
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = apertureIncrementsExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = apertureIncrementsExpanded,
                        onDismissRequest = { apertureIncrementsExpanded = false }
                    ) {
                        Increment.entries.forEach { increment ->
                            DropdownMenuItem(
                                text = { Text(increment.description(context)) },
                                onClick = {
                                    onApertureIncrementsChange(increment)
                                    apertureIncrementsExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.ApertureRange),
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
                        expanded = maxApertureExpanded,
                        onExpandedChange = { maxApertureExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(),
                            readOnly = true,
                            value = maxAperture,
                            isError = apertureRangeError.isNotEmpty(),
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = maxApertureExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = maxApertureExpanded,
                            onDismissRequest = { maxApertureExpanded = false }
                        ) {
                            apertureValues.forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        onSetMaxAperture(value)
                                        maxApertureExpanded = false
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
                        expanded = minApertureExpanded,
                        onExpandedChange = { minApertureExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(),
                            readOnly = true,
                            value = minAperture,
                            isError = apertureRangeError.isNotEmpty(),
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = minApertureExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = minApertureExpanded,
                            onDismissRequest = { minApertureExpanded = false }
                        ) {
                            apertureValues.forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        onSetMinAperture(value)
                                        minApertureExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    FilledTonalIconButton(onClick = onClearApertureRange) {
                        Icon(Icons.Outlined.Clear, "")
                    }
                }
            }
            if (apertureRangeError.isNotEmpty()) {
                Text(
                    text = apertureRangeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.CustomApertureValues),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropdownButton(
                    modifier = Modifier.weight(1f),
                    text = customApertureValues.sorted().distinct().joinToString(),
                    onClick = { showRemoveCustomApertureValuesDialog = true }
                )
                Box(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    FilledTonalIconButton(onClick = { showAddCustomApertureValueDialog = true }) {
                        Icon(Icons.Outlined.Add, "")
                    }
                }
                Box(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilledTonalIconButton(onClick = { showCustomApertureValuesInfo = true }) {
                        Icon(Icons.Outlined.Info, "")
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.FocalLengthRange),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Column(modifier = Modifier.weight(0.5f)) {
                    Text(
                        text = stringResource(R.string.From),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = minFocalLength.toString(),
                        isError = minFocalLengthError.isNotEmpty(),
                        onValueChange = onSetMinFocalLength,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    if (minFocalLengthError.isNotEmpty()) {
                        Text(
                            text = minFocalLengthError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(0.5f)) {
                    Text(
                        text = stringResource(R.string.To),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = maxFocalLength.toString(),
                        isError = maxFocalLengthError.isNotEmpty(),
                        onValueChange = onSetMaxFocalLength,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    if (maxFocalLengthError.isNotEmpty()) {
                        Text(
                            text = maxFocalLengthError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    if (showCustomApertureValuesInfo) {
        AlertDialog(
            text = { Text(stringResource(R.string.CustomApertureValuesHelp)) },
            onDismissRequest = { showCustomApertureValuesInfo = false },
            confirmButton = {
                TextButton(onClick = { showCustomApertureValuesInfo = false }) {
                    Text(stringResource(R.string.Close))
                }
            }
        )
    }
    if (showAddCustomApertureValueDialog) {
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomApertureValueDialog = false },
            dismissButton = {
                TextButton(onClick = { showAddCustomApertureValueDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = value.toFloatOrNull() != null,
                    onClick = {
                        showAddCustomApertureValueDialog = false
                        when (val v = value.toFloatOrNull()) {
                            is Float -> {
                                onSetCustomApertureValues(
                                    customApertureValues.plus(v)
                                )
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.Add))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.EnterCustomerApertureValue))
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        modifier = Modifier.width(100.dp),
                        value = value,
                        onValueChange = {
                            value = if (it.isEmpty()) {
                                it
                            } else {
                                when (it.toFloatOrNull()) {
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
    if (showRemoveCustomApertureValuesDialog) {
        var values by remember(customApertureValues) { mutableStateOf(customApertureValues) }
        AlertDialog(
            onDismissRequest = { showRemoveCustomApertureValuesDialog = false },
            dismissButton = {
                TextButton(onClick = { showRemoveCustomApertureValuesDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveCustomApertureValuesDialog = false
                        onSetCustomApertureValues(values)
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    for (value in values) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(value.toString())
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedIconButton(onClick = { values = values.minus(value) }) {
                                Icon(Icons.Outlined.DeleteOutline, "")
                            }
                        }
                    }
                }
            }
        )
    }
}