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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToGear: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val subtitle = mainViewModel.toolbarSubtitle.collectAsState()
    val rolls = mainViewModel.rolls.collectAsState()
    val selectedRolls = mainViewModel.selectedRolls.collectAsState()
    val rollSortMode = mainViewModel.rollSortMode.collectAsState()
    val rollFilterMode = mainViewModel.rollFilterMode.collectAsState()
    val rollCounts = mainViewModel.rollCounts.collectAsState()
    val labels = mainViewModel.labels.collectAsState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerContent = @Composable {
            DrawerContent(
                rollCounts = rollCounts.value,
                labels = labels.value,
                rollFilterMode = rollFilterMode.value,
                onRollFilterModeSet = { rollFilterMode ->
                    mainViewModel.setRollFilterMode(rollFilterMode)
                    scope.launch { drawerState.close() }
                },
                onNavigateToMap = onNavigateToMap,
                onNavigateToGear = onNavigateToGear,
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
                subtitle = subtitle.value,
                rolls = rolls.value,
                selectedRolls = selectedRolls.value,
                rollSortMode = rollSortMode.value,
                onRollSortModeSet = mainViewModel::setRollSortMode,
                onFabClick = { /*TODO*/ },
                onRollClick = { /*TODO*/ },
                toggleRollSelection = mainViewModel::toggleRollSelection,
                toggleRollSelectionAll = mainViewModel::toggleRollSelectionAll,
                toggleRollSelectionNone = mainViewModel::toggleRollSelectionNone,
                onEdit = { /*TODO*/ },
                onDelete = {
                    mainViewModel.selectedRolls.value.forEach(mainViewModel::deleteRoll)
                    mainViewModel.toggleRollSelectionNone()
                },
                onArchive = {
                    mainViewModel.selectedRolls.value.forEach { roll ->
                        roll.archived = true
                        mainViewModel.submitRoll(roll)
                    }
                    mainViewModel.toggleRollSelectionNone()
                },
                onUnarchive = {
                    mainViewModel.selectedRolls.value.forEach { roll ->
                        roll.archived = false
                        mainViewModel.submitRoll(roll)
                    }
                    mainViewModel.toggleRollSelectionNone()
                },
                onFavorite = {
                    mainViewModel.selectedRolls.value.forEach { roll ->
                        roll.favorite = true
                        mainViewModel.submitRoll(roll)
                    }
                    mainViewModel.toggleRollSelectionNone()
                },
                onUnfavorite = {
                    mainViewModel.selectedRolls.value.forEach { roll ->
                        roll.favorite = false
                        mainViewModel.submitRoll(roll)
                    }
                    mainViewModel.toggleRollSelectionNone()
                },
                onAddLabels = { /*TODO*/ },
                onRemoveLabels = { /*TODO*/ },
                navigationIcon = {
                    if (maxWidth < 600.dp) {
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

        if (maxWidth < 600.dp) {
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