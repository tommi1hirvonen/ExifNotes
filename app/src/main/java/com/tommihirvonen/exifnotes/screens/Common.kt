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

import android.text.format.DateFormat
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.maps.android.compose.MapType
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FilmStockFilterMode
import com.tommihirvonen.exifnotes.core.localDateTimeOrNull
import com.tommihirvonen.exifnotes.util.epochMilliseconds
import com.tommihirvonen.exifnotes.util.sortableDate
import com.tommihirvonen.exifnotes.util.sortableTime
import java.time.LocalDateTime

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
                                    val prev = items[item] == true
                                    items[item] = !prev
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = items[item] == true,
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
        initialSelection = FilmStockFilterMode.All,
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

@Composable
fun <TValue> SimpleItemsDialog(
    title: @Composable () -> Unit = {},
    items: List<TValue>,
    itemText: @Composable (TValue) -> Unit,
    onDismiss: () -> Unit,
    onSelected: (TValue) -> Unit
) {
    AlertDialog(
        title = title,
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(items) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemText(item)
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        }
    )
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
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
    maxLines: Int = 1,
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
                maxLines = maxLines,
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

@Preview(showBackground = true)
@Composable
private fun DateTimeButtonComboPreview() {
    DateTimeButtonCombo(
        dateTime = LocalDateTime.now(),
        onDateTimeSet = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeButtonCombo(
    modifier: Modifier = Modifier,
    dateTime: LocalDateTime?,
    onDateTimeSet: (LocalDateTime) -> Unit
) {
    val dateText = dateTime?.sortableDate ?: stringResource(R.string.Date)
    val timeText = dateTime?.sortableTime ?: stringResource(R.string.Time)
    val currentDateTime = LocalDateTime.now()
    val dateState = remember(dateTime) {
        DatePickerState(
            locale = CalendarLocale.getDefault(),
            initialSelectedDateMillis = dateTime?.epochMilliseconds
                ?: currentDateTime.epochMilliseconds
        )
    }
    val is24HourFormat = DateFormat.is24HourFormat(LocalContext.current)
    val timeState = remember(dateTime) {
        TimePickerState(
            initialHour = dateTime?.hour ?: currentDateTime.hour,
            initialMinute = dateTime?.minute ?: currentDateTime.minute,
            is24Hour = is24HourFormat
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    Row(modifier = modifier) {
        DropdownButton(
            modifier = Modifier.weight(0.55f),
            text = dateText,
            onClick = { showDatePicker = true }
        )
        Spacer(modifier = Modifier.width(14.dp))
        DropdownButton(
            modifier = Modifier.weight(0.45f),
            text = timeText,
            onClick = { showTimePicker = true }
        )
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        val date = dateState.selectedDateMillis?.let(::localDateTimeOrNull)
                        if (date != null) {
                            val time = dateTime ?: LocalDateTime.now()
                            val newDate = LocalDateTime.of(
                                date.year,
                                date.monthValue,
                                date.dayOfMonth,
                                time.hour,
                                time.minute
                            )
                            onDateTimeSet(newDate)
                        }

                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
    if (showTimePicker) {
        TimePickerDialog(
            timePickerState = timeState,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                showTimePicker = false
                val date = dateTime ?: LocalDateTime.now()
                val newTime = LocalDateTime.of(
                    date.year,
                    date.monthValue,
                    date.dayOfMonth,
                    timeState.hour,
                    timeState.minute
                )
                onDateTimeSet(newTime)
            }
        )
    }
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String = "Select time",
    timePickerState: TimePickerState? = null,
    onConfirm: (TimePickerState) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val actualTimePickerState = if (timePickerState != null) {
        timePickerState
    } else {
        val currentTime = LocalDateTime.now()
        rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute,
            is24Hour = true
        )
    }
    var showDial by remember { mutableStateOf(true) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                if (showDial) {
                    TimePicker(
                        state = actualTimePickerState,
                    )
                } else {
                    TimeInput(
                        state = actualTimePickerState,
                    )
                }
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    val toggleIcon = if (showDial) {
                        Icons.Outlined.EditCalendar
                    } else {
                        Icons.Outlined.AccessTime
                    }
                    IconButton(onClick = { showDial = !showDial }) {
                        Icon(
                            imageVector = toggleIcon,
                            contentDescription = "Time picker type toggle",
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.Cancel))
                    }
                    TextButton(
                        onClick = {
                            onConfirm(actualTimePickerState)
                        }
                    ) {
                        Text(stringResource(R.string.OK))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MapTypeDropdownMenuPreview() {
    MapTypeDropdownMenu(
        expanded = true,
        onDismiss = {},
        selectedMapType = MapType.NORMAL,
        onMapTypeSelected = {}
    )
}

@Composable
fun MapTypeDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedMapType: MapType,
    onMapTypeSelected: (MapType) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Normal)) },
            onClick = {
                onMapTypeSelected(MapType.NORMAL)
            },
            trailingIcon = {
                RadioButton(
                    selected = selectedMapType == MapType.NORMAL,
                    onClick = {
                        onMapTypeSelected(MapType.NORMAL)
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Hybrid)) },
            onClick = {
                onMapTypeSelected(MapType.HYBRID)
            },
            trailingIcon = {
                RadioButton(
                    selected = selectedMapType == MapType.HYBRID,
                    onClick = {
                        onMapTypeSelected(MapType.HYBRID)
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Satellite)) },
            onClick = {
                onMapTypeSelected(MapType.SATELLITE)
            },
            trailingIcon = {
                RadioButton(
                    selected = selectedMapType == MapType.SATELLITE,
                    onClick = {
                        onMapTypeSelected(MapType.SATELLITE)
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Terrain)) },
            onClick = {
                onMapTypeSelected(MapType.TERRAIN)
            },
            trailingIcon = {
                RadioButton(
                    selected = selectedMapType == MapType.TERRAIN,
                    onClick = {
                        onMapTypeSelected(MapType.TERRAIN)
                    }
                )
            }
        )
    }
}