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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.util.copy

@Composable
fun CameraEditScreen(
    cameraId: Long,
    onNavigateUp: () -> Unit,
    gearViewModel: GearViewModel,
    cameraViewModel: CameraViewModel = hiltViewModel { factory: CameraViewModel.Factory ->
        factory.create(cameraId)
    }
) {
    val make = cameraViewModel.make.collectAsState()
    val model = cameraViewModel.model.collectAsState()
    val serialNumber = cameraViewModel.serialNumber.collectAsState()
    val makeError = cameraViewModel.makeError.collectAsState()
    val modelError = cameraViewModel.modelError.collectAsState()
    CameraEditContent(
        isNewCamera = cameraId <= 0,
        make = make.value ?: "",
        model = model.value ?: "",
        serialNumber = serialNumber.value ?: "",
        makeError = makeError.value,
        modelError = modelError.value,
        onMakeChange = cameraViewModel::setMake,
        onModelChange = cameraViewModel::setModel,
        onSerialNumberChange = cameraViewModel::setSerialNumber,
        onNavigateUp = onNavigateUp,
        onSubmit = {
            if (cameraViewModel.validate()) {
                gearViewModel.submitCamera(cameraViewModel.camera)
                onNavigateUp()
            }
        }
    )
}

@Preview
@Composable
private fun CameraEditContentPreview() {
    CameraEditContent(
        isNewCamera = true,
        make = "Canon",
        model = "A-1",
        serialNumber = "123ASD456",
        makeError = false,
        modelError = false,
        onMakeChange = {},
        onModelChange = {},
        onSerialNumberChange = {},
        onNavigateUp = {},
        onSubmit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraEditContent(
    isNewCamera: Boolean,
    make: String,
    model: String,
    serialNumber: String,
    makeError: Boolean,
    modelError: Boolean,
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
                    label = { Text(stringResource(R.string.SerialNumber)) }
                )
            }
        }
    }
}