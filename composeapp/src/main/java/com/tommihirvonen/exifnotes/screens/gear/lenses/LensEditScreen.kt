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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FilmProcess
import com.tommihirvonen.exifnotes.screens.DropdownButton
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.util.copy
import com.tommihirvonen.exifnotes.util.description

@Composable
fun LensEditScreen(
    lensId: Long,
    fixedLens: Boolean,
    onNavigateUp: () -> Unit,
    gearViewModel: GearViewModel,
    lensViewModel: LensViewModel = hiltViewModel { factory: LensViewModel.Factory ->
        factory.create(lensId, fixedLens)
    }
) {
    val make = lensViewModel.make.collectAsState()
    val model = lensViewModel.model.collectAsState()
    val serialNumber = lensViewModel.serialNumber.collectAsState()
    val minFocalLength = lensViewModel.minFocalLength.collectAsState()
    val maxFocalLength = lensViewModel.maxFocalLength.collectAsState()
    val minAperture = lensViewModel.minAperture.collectAsState()
    val maxAperture = lensViewModel.maxAperture.collectAsState()
    val customApertureValues = lensViewModel.customApertureValues.collectAsState()
    val makeError = lensViewModel.makeError.collectAsState()
    val modelError = lensViewModel.modelError.collectAsState()
    val apertureRangeError = lensViewModel.apertureRangeError.collectAsState()
    val minFocalLengthError = lensViewModel.minFocalLengthError.collectAsState()
    val maxFocalLengthError = lensViewModel.maxFocalLengthError.collectAsState()
    LensEditContent(
        isNewLens = lensId <= 0,
        make = make.value ?: "",
        model = model.value ?: "",
        serialNumber = serialNumber.value ?: "",
        minFocalLength = minFocalLength.value,
        maxFocalLength = maxFocalLength.value,
        minAperture = minAperture.value ?: "",
        maxAperture = maxAperture.value ?: "",
        customApertureValues = customApertureValues.value,
        makeError = makeError.value,
        modelError = modelError.value,
        apertureRangeError = apertureRangeError.value,
        minFocalLengthError = minFocalLengthError.value,
        maxFocalLengthError = maxFocalLengthError.value,
        onMakeChange = lensViewModel::setMake,
        onModelChange = lensViewModel::setModel,
        onSerialNumberChange = lensViewModel::setSerialNumber,
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
        isNewLens = true,
        make = "Canon",
        model = "FD 28mm f/2.8",
        serialNumber = "123ASD456",
        minFocalLength = 28,
        maxFocalLength = 28,
        minAperture = "22",
        maxAperture = "2.8",
        customApertureValues = "3.5, 4.6",
        makeError = false,
        modelError = false,
        apertureRangeError = "Sample error",
        minFocalLengthError = "Sample error",
        maxFocalLengthError = "Sample error",
        onMakeChange = {},
        onModelChange = {},
        onSerialNumberChange = {},
        onNavigateUp = {},
        onSubmit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensEditContent(
    isNewLens: Boolean,
    make: String,
    model: String,
    serialNumber: String,
    minFocalLength: Int,
    maxFocalLength: Int,
    minAperture: String,
    maxAperture: String,
    customApertureValues: String,
    makeError: Boolean,
    modelError: Boolean,
    apertureRangeError: String,
    minFocalLengthError: String,
    maxFocalLengthError: String,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSerialNumberChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSubmit: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val title = stringResource(if (isNewLens) R.string.AddNewLens else R.string.EditLens)
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
                    label = { Text(stringResource(R.string.SerialNumber)) },
                    isError = modelError
                )
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
                        expanded = false,
                        onExpandedChange = {  }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(),
                            readOnly = true,
                            value = maxAperture,
                            isError = apertureRangeError.isNotEmpty(),
                            onValueChange = { /*TODO*/ },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = {}
                        ) {
                            FilmProcess.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.description ?: "") },
                                    onClick = {

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
                        expanded = false,
                        onExpandedChange = {  }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(),
                            readOnly = true,
                            value = minAperture,
                            isError = apertureRangeError.isNotEmpty(),
                            onValueChange = { /*TODO*/ },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = {}
                        ) {
                            FilmProcess.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.description ?: "") },
                                    onClick = {

                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    FilledTonalIconButton(onClick = { /*TODO*/ }) {
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
                    text = customApertureValues,
                    onClick = { /*TODO*/ }
                )
                Box(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    FilledTonalIconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Add, "")
                    }
                }
                Box(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    FilledTonalIconButton(onClick = { /*TODO*/ }) {
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
                        onValueChange = { /*TODO*/ }
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
                        onValueChange = { /*TODO*/ }
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
}