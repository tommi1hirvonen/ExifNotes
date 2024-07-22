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

package com.tommihirvonen.exifnotes.screens

import android.widget.TextView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FilmStockFilterMode

@Composable
fun DialogContent(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        content()
    }
}

@Preview
@Composable
private fun DialogContentPreview() {
    DialogContent {
        Box(modifier = Modifier.padding(16.dp)) {
            Text("Hello world!")
        }
    }
}

@Composable
fun StyledText(text: CharSequence, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context) },
        update = {
            it.text = text
        }
    )
}

@Preview
@Composable
private fun MultiChoiceDialogPreview() {
    MultiChoiceDialog(
        title = "Select manufacturers",
        initialItems = mapOf(
            "Fujifilm" to true,
            "Ilford" to false,
            "Kodak" to true
        ),
        onDismiss = {},
        onConfirm = {}
    )
}

@Composable
fun <TValue, TSort : Comparable<TSort>> MultiChoiceDialog(
    title: String? = null,
    initialItems: Map<TValue, Boolean>,
    itemText: (TValue) -> String,
    sortItemsBy: (TValue) -> (TSort),
    onDismiss: () -> Unit,
    onConfirm: (List<TValue>) -> Unit
) {
    val items = remember(initialItems) { initialItems.toList().toMutableStateMap() }
    val list = remember(initialItems) { items.map { it.key }.sortedBy(sortItemsBy).toList() }
    Dialog(onDismissRequest = onDismiss) {
        DialogContent {
            Column(modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
            ) {
                if (title != null) {
                    Row(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(text = title, style = MaterialTheme.typography.titleLarge)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .weight(1f, fill = false)
                ) {
                    items(items = list) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val prev = items[item] ?: false
                                    items[item] = !prev
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = items[item] ?: false,
                                onCheckedChange = { items[item] = it }
                            )
                            Text(itemText(item))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            for (key in items.keys) {
                                items[key] = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.DeselectAll))
                    }
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.Cancel))
                        }
                        TextButton(
                            onClick = {
                                onDismiss()
                                val selectedItems = items
                                    .filter { it.value }
                                    .map { it.key }
                                    .toList()
                                onConfirm(selectedItems)
                            }
                        ) {
                            Text(stringResource(R.string.OK))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiChoiceDialog(
    title: String,
    initialItems: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    MultiChoiceDialog(
        title = title,
        initialItems = initialItems,
        itemText = { it },
        sortItemsBy = { it },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Preview
@Composable
private fun SingleChoiceDialogPreview() {
    SingleChoiceDialog(
        items = FilmStockFilterMode.entries,
        initialSelection = FilmStockFilterMode.ALL,
        itemText = { it.toString() },
        sortItemsBy = { it.ordinal },
        onDismiss = { },
        onConfirm = { }
    )
}

@Composable
fun <TValue, TSort : Comparable<TSort>> SingleChoiceDialog(
    title: String? = null,
    items: List<TValue>,
    initialSelection: (TValue?),
    itemText: (TValue) -> String,
    sortItemsBy: (TValue) -> (TSort),
    onDismiss: () -> Unit,
    onConfirm: (TValue) -> Unit
) {
    var selectedValue by remember { mutableStateOf(initialSelection) }
    Dialog(onDismissRequest = onDismiss) {
        DialogContent {
            Column(modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
            ) {
                if (title != null) {
                    Row(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(text = title, style = MaterialTheme.typography.titleLarge)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .weight(1f, fill = false)
                ) {
                    items(items = items.sortedBy(sortItemsBy)) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedValue = item
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = item == selectedValue,
                                onClick = { selectedValue = item }
                            )
                            Text(itemText(item))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.Cancel))
                    }
                    TextButton(
                        enabled = selectedValue != null,
                        onClick = {
                            val value = selectedValue
                            if (value != null) {
                                onDismiss()
                                onConfirm(value)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.OK))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun DropdownButtonPreview() {
    Row {
        DropdownButton(
            modifier = Modifier.weight(0.5f),
            text = "Dropdown button",
            onClick = {}
        )
        ExposedDropdownMenuBox(
            modifier = Modifier.weight(0.5f),
            expanded = false,
            onExpandedChange = {  }
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(),
                readOnly = true,
                value = "Default dropdown",
                onValueChange = {},
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                }
            )
        }
    }
}

@Composable
fun DropdownButton(
    modifier: Modifier = Modifier,
    text: String = "",
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = shape
            )
            .clip(shape)
            .clickable { onClick() }
            .then(modifier)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}