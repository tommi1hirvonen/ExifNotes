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

package com.tommihirvonen.exifnotes.screens.gear.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.screens.DialogContent
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel

@Composable
fun FilterEditScreen(
    filterId: Long,
    onDismiss: () -> Unit,
    gearViewModel: GearViewModel,
    filterViewModel: FilterViewModel = hiltViewModel { factory: FilterViewModel.Factory ->
        factory.create(filterId)
    }
) {
    val make = filterViewModel.make.collectAsState()
    val model = filterViewModel.model.collectAsState()
    val makeError = filterViewModel.makeError.collectAsState()
    val modelError = filterViewModel.modelError.collectAsState()
    FilterEditForm(
        isNewFilter = filterViewModel.filter.id <= 0,
        make = make.value ?: "",
        model = model.value ?: "",
        makeError = makeError.value,
        modelError = modelError.value,
        onMakeChange = filterViewModel::setMake,
        onModelChange = filterViewModel::setModel,
        onDismiss = onDismiss,
        onSubmit = {
            val result = filterViewModel.validate()
            if (result) {
                gearViewModel.submitFilter(filterViewModel.filter)
                onDismiss()
            }
        }
    )
}

@Preview
@Composable
private fun FilterEditFormPreview() {
    FilterEditForm(
        isNewFilter = false,
        make = "Exif Notes Labs",
        model = "ND x64",
        makeError = false,
        modelError = false,
        onMakeChange = {},
        onModelChange = {},
        onDismiss = {},
        onSubmit = {}
    )
}

@Composable
private fun FilterEditForm(
    isNewFilter: Boolean,
    make: String,
    model: String,
    makeError: Boolean,
    modelError: Boolean,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    DialogContent {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            val title = if (!isNewFilter) {
                stringResource(id = R.string.EditFilter)
            } else {
                stringResource(id = R.string.AddNewFilter)
            }
            Text(title, fontSize = 24.sp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(stringResource(R.string.Cancel))
                }
                TextButton(
                    onClick = onSubmit,
                    modifier = Modifier.padding(8.dp),
                ) {
                    val confirmText = if (!isNewFilter) {
                        stringResource(id = R.string.OK)
                    } else {
                        stringResource(id = R.string.Add)
                    }
                    Text(confirmText)
                }
            }
        }
    }
}