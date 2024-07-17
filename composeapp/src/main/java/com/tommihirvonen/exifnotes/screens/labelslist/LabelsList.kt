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

package com.tommihirvonen.exifnotes.screens.labelslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.screens.main.RollsViewModel
import com.tommihirvonen.exifnotes.core.entities.Label

@Composable
fun LabelsList(
    rollsModel: RollsViewModel,
    onNavigateUp: () -> Unit,
    onEditLabel: (Label?) -> Unit
) {
    val labels = rollsModel.labels.collectAsState()
    LabelsContent(
        labels = labels.value,
        onDeleteLabel = { label ->
            rollsModel.deleteLabel(label)
        },
        onEditLabel = onEditLabel,
        onNavigateUp = onNavigateUp
    )
}

@Preview
@Composable
private fun LabelsContentPreview() {
    val labels = listOf(
        Label(0, "label 1", 1),
        Label(1, "label 2", 2),
        Label(2, "label 3", 3)
    )
    LabelsContent(labels)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelsContent(
    labels: List<Label>,
    onDeleteLabel: (Label) -> Unit = {},
    onEditLabel: (Label?) -> Unit = {},
    onNavigateUp: () -> Unit = {}
) {
    val confirmDeleteLabel = remember { mutableStateOf<Label?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.Labels)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Outlined.NewLabel, "") },
                text = { Text(stringResource(R.string.NewLabel)) },
                onClick = { onEditLabel(null) }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(labels, key = { label -> label.id }) { label ->
                LabelListItem(
                    label = label,
                    onLabelDelete = { confirmDeleteLabel.value = label },
                    onLabelEdit = { onEditLabel(label) }
                )
            }
        }
    }
    when (val deleteLabel = confirmDeleteLabel.value) {
        is Label -> {
            AlertDialog(
                onDismissRequest = { confirmDeleteLabel.value = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteLabel(deleteLabel)
                            confirmDeleteLabel.value = null
                        }
                    ) {
                        Text(stringResource(R.string.Yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteLabel.value = null }) {
                        Text(stringResource(R.string.Cancel))
                    }
                },
                title = { Text(stringResource(R.string.DeleteLabel)) },
                text = { Text(deleteLabel.name) }
            )
        }
    }
}

@Preview
@Composable
private fun LabelListItemPreview() {
    val label = Label(name = "test-label", rollCount = 5)
    LabelListItem(label = label)
}

@Composable
private fun LabelListItem(
    label: Label,
    onLabelDelete: () -> Unit = {},
    onLabelEdit: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable { onLabelEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(label.name, fontSize = 20.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.Label, "")
                    Text(
                        text = pluralStringResource(R.plurals.RollsAmount, label.rollCount, label.rollCount),
                        fontSize = 16.sp
                    )
                }
            }
            Column {
                IconButton(onClick = onLabelDelete) {
                    Icon(Icons.Outlined.Delete, "")
                }
            }
        }
    }
}