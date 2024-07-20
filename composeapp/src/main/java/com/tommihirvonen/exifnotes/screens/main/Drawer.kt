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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.data.repositories.RollCounts

@Preview
@Composable
private fun DrawerContentPreview() {
    val rollCounts = RollCounts(
        active = 4,
        archived = 2,
        favorites = 1
    )
    val labels = listOf(
        Label(name = "test-label", rollCount = 2)
    )
    DrawerContent(
        rollCounts = rollCounts,
        labels = labels,
        rollFilterMode = RollFilterMode.Active,
        onRollFilterModeSet = {},
        onNavigateToMap = {},
        onNavigateToGear = {},
        onNavigateToLabels = {},
        onNavigateToSettings = {}
    )
}

@Composable
fun DrawerContent(
    rollCounts: RollCounts,
    labels: List<Label>,
    rollFilterMode: RollFilterMode,
    onRollFilterModeSet: (RollFilterMode) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToGear: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val all = rollCounts.active + rollCounts.archived
    ModalDrawerSheet {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = stringResource(R.string.VisibleRolls),
                    modifier = Modifier.padding(
                        start = 18.dp,
                        top = 8.dp,
                        end = 18.dp,
                        bottom = 8.dp
                    ),
                    fontWeight = FontWeight.SemiBold
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Active)) },
                    icon = { Icon(Icons.Outlined.CameraAlt, "") },
                    selected = rollFilterMode is RollFilterMode.Active,
                    badge = { Text(rollCounts.active.toString()) },
                    onClick = { onRollFilterModeSet(RollFilterMode.Active) }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Archived)) },
                    icon = { Icon(Icons.Outlined.Inventory2, "") },
                    selected = rollFilterMode is RollFilterMode.Archived,
                    badge = { Text(rollCounts.archived.toString()) },
                    onClick = { onRollFilterModeSet(RollFilterMode.Archived) }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Favorites)) },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, "") },
                    selected = rollFilterMode is RollFilterMode.Favorites,
                    badge = { Text(rollCounts.favorites.toString()) },
                    onClick = { onRollFilterModeSet(RollFilterMode.Favorites) }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.All)) },
                    icon = { Icon(Icons.Outlined.AllInclusive, "") },
                    selected = rollFilterMode is RollFilterMode.All,
                    badge = { Text(all.toString()) },
                    onClick = { onRollFilterModeSet(RollFilterMode.All) }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            Column(modifier = Modifier.padding(8.dp)) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Map)) },
                    icon = { Icon(Icons.Outlined.Map, "") },
                    selected = false,
                    onClick = onNavigateToMap
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Gear)) },
                    icon = { Icon(Icons.Outlined.Camera, "") },
                    selected = false,
                    onClick = onNavigateToGear
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Settings)) },
                    icon = { Icon(Icons.Outlined.Settings, "") },
                    selected = false,
                    onClick = onNavigateToSettings
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.ManageLabels)) },
                    icon = { Icon(Icons.Outlined.NewLabel, "") },
                    selected = false,
                    onClick = onNavigateToLabels
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = stringResource(R.string.Labels),
                    modifier = Modifier.padding(
                        start = 18.dp,
                        top = 8.dp,
                        end = 18.dp,
                        bottom = 8.dp
                    ),
                    fontWeight = FontWeight.SemiBold
                )
                if (labels.isEmpty()) {
                    val textColor =
                        NavigationDrawerItemDefaults.colors().textColor(selected = false).value
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                        Text(
                            color = textColor,
                            text = stringResource(R.string.NoLabels)
                        )
                    }

                }
                labels.forEach { label ->
                    NavigationDrawerItem(
                        label = { Text(label.name) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Label, "") },
                        selected = rollFilterMode is RollFilterMode.HasLabel && rollFilterMode.label.id == label.id,
                        badge = { Text(label.rollCount.toString()) },
                        onClick = { onRollFilterModeSet(RollFilterMode.HasLabel(label)) }
                    )
                }
            }
        }
    }
}