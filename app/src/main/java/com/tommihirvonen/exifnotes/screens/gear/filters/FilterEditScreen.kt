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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.AttachmentType
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.screens.DialogContent

@Composable
fun FilterEditScreen(
    filterId: Long,
    isAccessory: Boolean = false,
    onDismiss: () -> Unit,
    submitHandler: (Filter) -> Unit,
    filterViewModel: FilterViewModel = hiltViewModel { factory: FilterViewModel.Factory ->
        factory.create(filterId, isAccessory)
    }
) {
    val filter = filterViewModel.filter.collectAsState()
    val makeError = filterViewModel.makeError.collectAsState()
    val modelError = filterViewModel.modelError.collectAsState()
    val factor = filterViewModel.factor.collectAsState()
    val modifierError = filterViewModel.modifierError.collectAsState()
    FilterEditForm(
        isNewFilter = filter.value.id <= 0,
        filter = filter.value,
        makeError = makeError.value,
        modelError = modelError.value,
        factor = factor.value,
        modifierError = modifierError.value,
        onMakeChange = filterViewModel::setMake,
        onModelChange = filterViewModel::setModel,
        onTypeChange = filterViewModel::setType,
        onFactorChange = filterViewModel::setFactor,
        onDismiss = onDismiss,
        onSubmit = {
            val result = filterViewModel.validate()
            if (result) {
                submitHandler(filterViewModel.filter.value)
                onDismiss()
            }
        }
    )
}

@Preview
@Composable
private fun FilterEditFormPreview() {
    val filter = Filter(make = "Exif Notes Labs", model = "ND x64")
    FilterEditForm(
        isNewFilter = false,
        filter = filter,
        makeError = false,
        modelError = false,
        factor = "1.0",
        modifierError = false,
        onMakeChange = {},
        onModelChange = {},
        onTypeChange = {},
        onFactorChange = {},
        onDismiss = {},
        onSubmit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterEditForm(
    isNewFilter: Boolean,
    filter: Filter,
    makeError: Boolean,
    modelError: Boolean,
    factor: String,
    modifierError: Boolean,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTypeChange: (AttachmentType) -> Unit,
    onFactorChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }
    DialogContent {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            val title = if (!isNewFilter) {
                stringResource(id = if (filter.isAccessory) R.string.EditAccessory else R.string.EditFilter)
            } else {
                stringResource(id = if (filter.isAccessory) R.string.AddNewAccessory else R.string.AddNewFilter)
            }
            Text(title, fontSize = 24.sp)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = filter.make ?: "",
                    onValueChange = onMakeChange,
                    label = { Text(stringResource(R.string.Make)) },
                    supportingText = { Text(stringResource(R.string.Required)) },
                    isError = makeError
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = filter.model ?: "",
                    onValueChange = onModelChange,
                    label = { Text(stringResource(R.string.Model)) },
                    supportingText = { Text(stringResource(R.string.Required)) },
                    isError = modelError
                )
            }
            if (filter.isAccessory) {
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = attachmentTypeName(filter.type),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.AccessoryType)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            AttachmentType.entries.drop(1).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(attachmentTypeName(type)) },
                                    onClick = {
                                        onTypeChange(type)
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (filter.type.usesFactor) {
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = factor,
                            onValueChange = onFactorChange,
                            label = { Text(stringResource(
                                if (filter.type == AttachmentType.ExtensionTube)
                                    R.string.ExtensionLength else R.string.OpticalFactor
                            )) },
                            supportingText = { Text(stringResource(
                                if (filter.type == AttachmentType.ExtensionTube)
                                    R.string.ExtensionLengthHelp else R.string.OpticalFactorHelp
                            )) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = modifierError
                        )
                    }
                }
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

@Composable
internal fun attachmentTypeName(type: AttachmentType) = stringResource(when (type) {
    AttachmentType.Filter -> R.string.FilterOrFilters
    AttachmentType.Accessory -> R.string.Accessory
    AttachmentType.Teleconverter -> R.string.Teleconverter
    AttachmentType.FocalReducer -> R.string.FocalReducer
    AttachmentType.ExtensionTube -> R.string.ExtensionTube
})
