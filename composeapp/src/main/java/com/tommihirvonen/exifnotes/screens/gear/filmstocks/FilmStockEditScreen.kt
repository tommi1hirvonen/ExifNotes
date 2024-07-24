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

package com.tommihirvonen.exifnotes.screens.gear.filmstocks

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
import com.tommihirvonen.exifnotes.core.entities.FilmProcess
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.FilmType
import com.tommihirvonen.exifnotes.screens.DialogContent
import com.tommihirvonen.exifnotes.util.description

@Composable
fun FilmStockEditScreen(
    filmStockId: Long,
    onNavigateUp: () -> Unit,
    filmStocksViewModel: FilmStocksViewModel = hiltViewModel(),
    filmStockViewModel: FilmStockViewModel = hiltViewModel { factory: FilmStockViewModel.Factory ->
        factory.create(filmStockId)
    },
    afterSubmit: (FilmStock) -> Unit = {}
) {
    val make = filmStockViewModel.make.collectAsState()
    val model = filmStockViewModel.model.collectAsState()
    val iso = filmStockViewModel.iso.collectAsState()
    val type = filmStockViewModel.type.collectAsState()
    val process = filmStockViewModel.process.collectAsState()
    val makeError = filmStockViewModel.makeError.collectAsState()
    val modelError = filmStockViewModel.modelError.collectAsState()
    FilmStockEditForm(
        isNewFilmStock = filmStockViewModel.filmStock.id <= 0,
        make = make.value ?: "",
        model = model.value ?: "",
        iso = iso.value.toString(),
        type = type.value,
        process = process.value,
        makeError = makeError.value,
        modelError = modelError.value,
        onMakeChange = filmStockViewModel::setMake,
        onModelChange = filmStockViewModel::setModel,
        onIsoChange = filmStockViewModel::setIso,
        onTypeChange = filmStockViewModel::setType,
        onProcessChange = filmStockViewModel::setProcess,
        onDismiss = onNavigateUp,
        onSubmit = {
            val result = filmStockViewModel.validate()
            if (result) {
                filmStocksViewModel.submitFilmStock(filmStockViewModel.filmStock)
                afterSubmit(filmStockViewModel.filmStock)
                onNavigateUp()
            }
        }
    )
}

@Preview
@Composable
private fun FilmStockEditFormPreview() {
    FilmStockEditForm(
        isNewFilmStock = false,
        make = "Exif Notes Labs",
        model = "400 Professional",
        iso = "400",
        type = FilmType.BW_NEGATIVE,
        process = FilmProcess.BW_NEGATIVE,
        makeError = false,
        modelError = false,
        onMakeChange = {},
        onModelChange = {},
        onTypeChange = {},
        onProcessChange = {},
        onIsoChange = {},
        onDismiss = {},
        onSubmit = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilmStockEditForm(
    isNewFilmStock: Boolean,
    make: String,
    model: String,
    iso: String,
    type: FilmType,
    process: FilmProcess,
    makeError: Boolean,
    modelError: Boolean,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onIsoChange: (String) -> Unit,
    onTypeChange: (FilmType) -> Unit,
    onProcessChange: (FilmProcess) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }
    var processExpanded by remember { mutableStateOf(false) }
    DialogContent {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            val title = if (!isNewFilmStock) {
                stringResource(id = R.string.EditFilmStock)
            } else {
                stringResource(id = R.string.AddNewFilmStock)
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
            Row(modifier = Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = iso,
                    onValueChange = onIsoChange,
                    label = { Text(stringResource(R.string.ISO)) },
                    supportingText = { Text(stringResource(R.string.Required)) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = type.description ?: "",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.FilmType)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        FilmType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.description ?: "") },
                                onClick = {
                                    onTypeChange(t)
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = processExpanded,
                    onExpandedChange = { processExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = process.description ?: "",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.FilmProcess)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = processExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = processExpanded,
                        onDismissRequest = { processExpanded = false }
                    ) {
                        FilmProcess.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.description ?: "") },
                                onClick = {
                                    onProcessChange(p)
                                    processExpanded = false
                                }
                            )
                        }
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
                    val confirmText = if (!isNewFilmStock) {
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