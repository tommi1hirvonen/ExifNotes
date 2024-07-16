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

package com.tommihirvonen.exifnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LabelForm(
    labelId: Long,
    onDismiss: () -> Unit = {},
    rollsModel: RollsViewModel,
    model: LabelViewModel = hiltViewModel<LabelViewModel, LabelViewModel.Factory> { factory ->
        factory.create(labelId)
    }
) {
    val name = model.labelName.collectAsState()
    val error = model.labelNameError.collectAsState()
    Card {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            val title = if (model.label.id > 0) {
                stringResource(id = R.string.EditLabel)
            } else {
                stringResource(id = R.string.AddNewLabel)
            }
            Text(title, fontSize = 24.sp)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name.value,
                    onValueChange = model::setLabelName,
                    label = { Text(stringResource(R.string.Name)) },
                    supportingText = { Text(stringResource(R.string.Required)) },
                    isError = error.value
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
                    onClick = {
                        val result = model.validate()
                        if (result) {
                            rollsModel.submitLabel(model.label)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    val confirmText = if (model.label.id > 0) {
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