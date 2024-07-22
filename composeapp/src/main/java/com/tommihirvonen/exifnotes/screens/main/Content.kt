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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollSortMode
import com.tommihirvonen.exifnotes.util.State
import com.tommihirvonen.exifnotes.util.isScrollingUp
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Preview
@Composable
private fun MainContentPreview() {
    val filmStock = FilmStock(make = "Tommi's Lab", model = "Rainbow 400", iso = 400)
    val camera = Camera(make = "TomCam Factory", model = "Pocket 9000")
    val roll = Roll(
        name = "Placeholder roll",
        date = LocalDateTime.of(2024, 1, 1, 0, 0),
        unloaded = LocalDateTime.of(2024, 2, 1, 0, 0),
        developed = LocalDateTime.of(2024, 3, 1, 0, 0),
        camera = camera,
        filmStock = filmStock,
        note = "Test note ".repeat(10),
        frameCount = 2
    )
    val rolls = State.Success(listOf(roll))
    MainContent(
        subtitle = "Active rolls",
        rolls = rolls,
        selectedRolls = hashSetOf(),
        rollSortMode = RollSortMode.DATE,
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
        onRemoveLabels = {},
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Menu, "")
            }
        }
    )
}

@Preview
@Composable
private fun MainContentActionModePreview() {
    val filmStock = FilmStock(make = "Tommi's Lab", model = "Rainbow 400", iso = 400)
    val camera = Camera(make = "TomCam Factory", model = "Pocket 9000")
    val roll = Roll(
        name = "Placeholder roll",
        date = LocalDateTime.of(2024, 1, 1, 0, 0),
        unloaded = LocalDateTime.of(2024, 2, 1, 0, 0),
        developed = LocalDateTime.of(2024, 3, 1, 0, 0),
        camera = camera,
        filmStock = filmStock,
        note = "Test note ".repeat(10),
        frameCount = 2
    )
    val rolls = State.Success(listOf(roll))
    val selectedRolls = hashSetOf(roll)
    MainContent(
        subtitle = "Active rolls",
        rolls = rolls,
        selectedRolls = selectedRolls,
        rollSortMode = RollSortMode.DATE,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    subtitle: String,
    rolls: State<List<Roll>>,
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
    onRemoveLabels: () -> Unit,
    navigationIcon: @Composable () -> Unit = {}
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val actionModeEnabled = selectedRolls.isNotEmpty()

    val allRollsCount = when (rolls) {
        is State.Success -> rolls.data.size
        else -> 0
    }

    val messageRollsArchived = stringResource(R.string.RollsArchived)
    val messageRollsUnarchived = stringResource(R.string.RollsActivated)
    val messageRollsAddedToFavorites = stringResource(R.string.RollsAddedToFavorites)
    val messageRollsRemovedFromFavorites = stringResource(R.string.RollsRemovedFromFavorites)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                subtitle = subtitle,
                rollSortMode = rollSortMode,
                onRollSortModeSet = onRollSortModeSet
            )
            AnimatedVisibility(
                visible = actionModeEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ActionModeAppBar(
                    selectedRollsCount = selectedRolls.size,
                    allRollsCount = allRollsCount,
                    onDeselectAll = toggleRollSelectionNone,
                    onSelectAll = toggleRollSelectionAll,
                    onDelete = onDelete,
                    onEdit = onEdit,
                    onArchive = {
                        onArchive()
                        scope.launch {
                            snackBarHostState.showSnackbar(messageRollsArchived)
                        }
                    },
                    onUnarchive = {
                        onUnarchive()
                        scope.launch {
                            snackBarHostState.showSnackbar(messageRollsUnarchived)
                        }
                    },
                    onFavorite = {
                        onFavorite()
                        scope.launch {
                            snackBarHostState.showSnackbar(messageRollsAddedToFavorites)
                        }
                    },
                    onUnfavorite = {
                        onUnfavorite()
                        scope.launch {
                            snackBarHostState.showSnackbar(messageRollsRemovedFromFavorites)
                        }
                    },
                    onAddLabels = onAddLabels,
                    onRemoveLabels = onRemoveLabels
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Outlined.Add, "") },
                text = { Text(stringResource(R.string.NewRoll)) },
                expanded = listState.isScrollingUp(),
                onClick = onFabClick
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { innerPadding ->
        if (rolls is State.InProgress) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(vertical = 48.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else if (rolls is State.Success) {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp),
                state = listState
            ) {
                if (rolls.data.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier.alpha(0.6f),
                                text = stringResource(R.string.NoRolls),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
                items(
                    items = rolls.data,
                    key = { roll -> roll.id }
                ) { roll ->
                    RollCard(
                        roll = roll,
                        selected = selectedRolls.contains(roll),
                        onClick = {
                            if (actionModeEnabled) {
                                toggleRollSelection(roll)
                                return@RollCard
                            }
                            onRollClick(roll)
                        },
                        onLongClick = { toggleRollSelection(roll) }
                    )
                }
            }

        }
    }
}