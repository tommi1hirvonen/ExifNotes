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

package com.tommihirvonen.exifnotes.screens.labels

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
import com.tommihirvonen.exifnotes.screens.DialogContent
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.screens.main.MainViewModel

@Composable
fun LabelEditScreen(
    labelId: Long,
    onDismiss: () -> Unit = {},
    mainViewModel: MainViewModel,
    model: LabelViewModel = hiltViewModel<LabelViewModel, LabelViewModel.Factory> { factory ->
        factory.create(labelId)
    }
) {
    val label = model.label.collectAsState()
    val error = model.labelNameError.collectAsState()
    LabelEditForm(
        isNewLabel = label.value.id <= 0,
        label = label.value,
        labelNameError = error.value,
        onLabelNameChange = model::setLabelName,
        onDismiss = onDismiss,
        onSubmit = {
            val result = model.validate()
            if (result) {
                mainViewModel.submitLabel(model.label.value)
                onDismiss()
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun LabelEditFormPreview() {
    LabelEditForm(
        isNewLabel = false,
        label = Label(name = "Test label"),
        labelNameError = false,
        onLabelNameChange = {},
        onDismiss = {},
        onSubmit = {}
    )
}

@Composable
private fun LabelEditForm(
    isNewLabel: Boolean,
    label: Label,
    labelNameError: Boolean,
    onLabelNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    DialogContent {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            val title = if (!isNewLabel) {
                stringResource(id = R.string.EditLabel)
            } else {
                stringResource(id = R.string.AddNewLabel)
            }
            Text(title, fontSize = 24.sp)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = label.name,
                    onValueChange = onLabelNameChange,
                    label = { Text(stringResource(R.string.Name)) },
                    supportingText = { Text(stringResource(R.string.Required)) },
                    isError = labelNameError
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
                    val confirmText = if (!isNewLabel) {
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