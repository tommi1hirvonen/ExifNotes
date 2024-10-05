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

package com.tommihirvonen.exifnotes.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.core.entities.RollSortMode
import com.tommihirvonen.exifnotes.data.repositories.RollCounts
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.SelectFilmStockDialog
import com.tommihirvonen.exifnotes.util.LoadState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onEditRoll: (Roll?) -> Unit,
    onNavigateToRoll: (Roll) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToGear: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val subtitle = mainViewModel.toolbarSubtitle.collectAsState()
    val rolls = mainViewModel.rolls.collectAsState()
    val selectedRolls = mainViewModel.selectedRolls.collectAsState()
    val rollSortMode = mainViewModel.rollSortMode.collectAsState()
    val rollFilterMode = mainViewModel.rollFilterMode.collectAsState()
    val rollCounts = mainViewModel.rollCounts.collectAsState()
    val labels = mainViewModel.labels.collectAsState()

    var showAddLabelsDialog by remember { mutableStateOf(false) }
    var showRemoveLabelsDialog by remember { mutableStateOf(false) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchEditFilmStock by remember { mutableStateOf(false) }
    var showBatchEditISODialog by remember { mutableStateOf(false) }

    MainContent(
        snackbarHostState = snackbarHostState,
        rollCounts = rollCounts.value,
        labels = labels.value,
        rollFilterMode = rollFilterMode.value,
        onRollFilterModeSet = mainViewModel::setRollFilterMode,
        onNavigateToMap = onNavigateToMap,
        onNavigateToGear = onNavigateToGear,
        onNavigateToLabels = onNavigateToLabels,
        onNavigateToSettings = onNavigateToSettings,
        subtitle = subtitle.value,
        rolls = rolls.value,
        selectedRolls = selectedRolls.value,
        rollSortMode = rollSortMode.value,
        onRollSortModeSet = mainViewModel::setRollSortMode,
        onFabClick = { onEditRoll(null) },
        onRollClick = onNavigateToRoll,
        toggleRollSelection = mainViewModel::toggleRollSelection,
        toggleRollSelectionAll = mainViewModel::toggleRollSelectionAll,
        toggleRollSelectionNone = mainViewModel::toggleRollSelectionNone,
        onEdit = {
            if (selectedRolls.value.size > 1) {
                showBatchEditDialog = true
            } else {
                val roll = selectedRolls.value.firstOrNull()
                if (roll != null) onEditRoll(roll)
            }
        },
        onDelete = {
            mainViewModel.selectedRolls.value.forEach(mainViewModel::deleteRoll)
            mainViewModel.toggleRollSelectionNone()
        },
        onArchive = {
            mainViewModel.selectedRolls.value.forEach { roll ->
                mainViewModel.submitRoll(roll.copy(archived = true))
            }
            mainViewModel.toggleRollSelectionNone()
        },
        onUnarchive = {
            mainViewModel.selectedRolls.value.forEach { roll ->
                mainViewModel.submitRoll(roll.copy(archived = false))
            }
            mainViewModel.toggleRollSelectionNone()
        },
        onFavorite = {
            mainViewModel.selectedRolls.value.forEach { roll ->
                mainViewModel.submitRoll(roll.copy(favorite = true))
            }
            mainViewModel.toggleRollSelectionNone()
        },
        onUnfavorite = {
            mainViewModel.selectedRolls.value.forEach { roll ->
                mainViewModel.submitRoll(roll.copy(favorite = false))
            }
            mainViewModel.toggleRollSelectionNone()
        },
        onAddLabels = { showAddLabelsDialog = true },
        onRemoveLabels = { showRemoveLabelsDialog = true }
    )
    if (showAddLabelsDialog) {
        val message = stringResource(R.string.LabelsAdded)
        MultiChoiceDialog(
            title = stringResource(R.string.AddLabels),
            initialItems = labels.value.associateWith { false },
            itemText = { it.name },
            sortItemsBy = { it.name },
            onDismiss = { showAddLabelsDialog = false },
            onConfirm = { selectedLabels ->
                showAddLabelsDialog = false
                for (roll in mainViewModel.selectedRolls.value) {
                    val labelsToAdd = selectedLabels
                        .filter { label ->
                            roll.labels.none { it.id == label.id }
                        }
                    mainViewModel.submitRoll(
                        roll.copy(
                            labels = roll.labels.plus(labelsToAdd).sortedBy { it.name }
                        )
                    )
                }
                if (selectedLabels.isNotEmpty()) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }
    if (showRemoveLabelsDialog) {
        val filteredLabels = labels.value.filter { label ->
            mainViewModel.selectedRolls.value.any { roll ->
                roll.labels.any { it.id == label.id }
            }
        }
        val message = stringResource(R.string.LabelsRemoved)
        MultiChoiceDialog(
            title = stringResource(R.string.RemoveLabels),
            initialItems = filteredLabels.associateWith { false },
            itemText = { it.name },
            sortItemsBy = { it.name },
            onDismiss = { showRemoveLabelsDialog = false },
            onConfirm = { selectedLabels ->
                showRemoveLabelsDialog = false
                for (roll in mainViewModel.selectedRolls.value) {
                    mainViewModel.submitRoll(
                        roll.copy(
                            labels = roll.labels.filterNot { label ->
                                selectedLabels.any { it.id == label.id }
                            }
                        )
                    )
                }
                if (selectedLabels.isNotEmpty()) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }
    if (showBatchEditDialog) {
        BatchEditDialog(
            selectedRolls = selectedRolls.value,
            onEditFilmStock = {
                showBatchEditDialog = false
                showBatchEditFilmStock = true
            },
            onClearFilmStock = {
                selectedRolls.value.forEach { roll ->
                    mainViewModel.submitRoll(roll.copy(filmStock = null))
                }
                showBatchEditDialog = false
            },
            onEditISO = {
                showBatchEditDialog = false
                showBatchEditISODialog = true
            },
            onDismiss = { showBatchEditDialog = false }
        )
    }
    if (showBatchEditFilmStock) {
        SelectFilmStockDialog(
            onDismiss = { showBatchEditFilmStock = false },
            onSelect = { filmStock ->
                showBatchEditFilmStock = false
                selectedRolls.value.forEach { roll ->
                    mainViewModel.submitRoll(roll.copy(filmStock = filmStock))
                }
            }
        )
    }
    if (showBatchEditISODialog) {
        IsoDialog(
            onDismiss = { showBatchEditISODialog = false },
            onConfirm = { value ->
                showBatchEditISODialog = false
                selectedRolls.value.forEach { roll ->
                    mainViewModel.submitRoll(roll.copy(iso = value))
                }
            }
        )
    }
}

@Preview
@Composable
private fun BatchEditDialog(
    selectedRolls: HashSet<Roll> = hashSetOf(),
    onEditFilmStock: () -> Unit = {},
    onClearFilmStock: () -> Unit = {},
    onEditISO: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        title = {
            Text(
                pluralStringResource(
                    R.plurals.BatchEditRollsTitle,
                    selectedRolls.size,
                    selectedRolls.size
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditFilmStock() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.EditFilmStock))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearFilmStock() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.ClearFilmStock))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditISO() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.SetISO))
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

@Preview
@Composable
private fun MainContentPreview() {
    val filmStock = FilmStock(make = "Tommi's Lab", model = "Rainbow 400", iso = 400)
    val camera = Camera(make = "TomCam Factory", model = "Pocket 9000")
    val roll = Roll(
        id = 1,
        name = "Placeholder roll",
        date = LocalDateTime.of(2024, 1, 1, 0, 0),
        unloaded = LocalDateTime.of(2024, 2, 1, 0, 0),
        developed = LocalDateTime.of(2024, 3, 1, 0, 0),
        camera = camera,
        filmStock = filmStock,
        note = "Test note ".repeat(10),
        frameCount = 2
    )
    val rolls = LoadState.Success(listOf(roll, roll.copy(id = 2)))
    MainContent(
        snackbarHostState = SnackbarHostState(),
        rollCounts = RollCounts(active = 2, archived = 2, favorites = 1),
        labels = emptyList(),
        rollFilterMode = RollFilterMode.Active,
        onRollFilterModeSet = {},
        onNavigateToMap = {},
        onNavigateToGear = {},
        onNavigateToLabels = {},
        onNavigateToSettings = {},
        subtitle = "Archived rolls",
        rolls = rolls,
        selectedRolls = hashSetOf(),
        rollSortMode = RollSortMode.Date,
        onRollSortModeSet = {},
        onFabClick = {},
        onRollClick = {},
        toggleRollSelection = {},
        toggleRollSelectionAll = {},
        toggleRollSelectionNone = {},
        onEdit = {},
        onDelete = {},
        onArchive = {},
        onUnarchive = {},
        onFavorite = {},
        onUnfavorite = {},
        onAddLabels = {},
        onRemoveLabels = {}
    )
}

@Composable
private fun MainContent(
    snackbarHostState: SnackbarHostState,
    rollCounts: RollCounts,
    labels: List<Label>,
    rollFilterMode: RollFilterMode,
    onRollFilterModeSet: (RollFilterMode) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToGear: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    subtitle: String,
    rolls: LoadState<List<Roll>>,
    selectedRolls: HashSet<Roll>,
    rollSortMode: RollSortMode,
    onRollSortModeSet: (RollSortMode) -> Unit,
    onFabClick: () -> Unit,
    onRollClick: (Roll) -> Unit,
    toggleRollSelection: (Roll) -> Unit,
    toggleRollSelectionAll: () -> Unit,
    toggleRollSelectionNone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onAddLabels: () -> Unit,
    onRemoveLabels: () -> Unit
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerContent = @Composable {
            DrawerContent(
                rollCounts = rollCounts,
                labels = labels,
                rollFilterMode = rollFilterMode,
                onRollFilterModeSet = { rollFilterMode ->
                    onRollFilterModeSet(rollFilterMode)
                    scope.launch { drawerState.close() }
                },
                onNavigateToMap = {
                    scope.launch { drawerState.close() }
                    onNavigateToMap()
                },
                onNavigateToGear = {
                    scope.launch { drawerState.close() }
                    onNavigateToGear()
                },
                onNavigateToLabels = {
                    scope.launch { drawerState.close() }
                    onNavigateToLabels()
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        }
        val mainContent = @Composable {
            MainContent(
                snackbarHostState = snackbarHostState,
                subtitle = subtitle,
                rolls = rolls,
                selectedRolls = selectedRolls,
                rollSortMode = rollSortMode,
                onRollSortModeSet = onRollSortModeSet,
                onFabClick = onFabClick,
                onRollClick = onRollClick,
                toggleRollSelection = toggleRollSelection,
                toggleRollSelectionAll = toggleRollSelectionAll,
                toggleRollSelectionNone = toggleRollSelectionNone,
                onEdit = onEdit,
                onDelete = onDelete,
                onArchive = onArchive,
                onUnarchive = onUnarchive,
                onFavorite = onFavorite,
                onUnfavorite = onUnfavorite,
                onAddLabels = onAddLabels,
                onRemoveLabels = onRemoveLabels,
                navigationIcon = {
                    if (this.maxWidth < 600.dp) {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) { Icon(Icons.Outlined.Menu, "") }
                    }
                }
            )
        }

        if (this.maxWidth < 600.dp) {
            ModalNavigationDrawer(
                drawerContent = { drawerContent() },
                drawerState = drawerState,
                gesturesEnabled = true
            ) {
                mainContent()
            }
        } else {
            PermanentNavigationDrawer(
                drawerContent = { drawerContent() }
            ) {
                mainContent()
            }
        }
    }
}

@Preview
@Composable
private fun IsoDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (Int) -> Unit = {}
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.toIntOrNull() != null,
                onClick = {
                    if (value.toIntOrNull() != null) {
                        onConfirm(value.toIntOrNull() ?: 0)
                    }
                }
            ) {
                Text(stringResource(R.string.OK))
            }
        },
        title = { Text(stringResource(R.string.SetISO)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    modifier = Modifier.width(100.dp),
                    value = value,
                    onValueChange = {
                        value = if (it.isEmpty()) {
                            it
                        } else {
                            when (it.toIntOrNull()) {
                                null -> value
                                else -> it
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        }
    )
}