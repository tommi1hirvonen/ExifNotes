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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommihirvonen.exifnotes.theme.ExifNotesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExifNotesTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                BoxWithConstraints {
                    if (maxWidth < 600.dp) {
                        ModalNavigationDrawer(
                            drawerContent = { DrawerContent() },
                            drawerState = drawerState,
                            gesturesEnabled = true
                        ) {
                            val scope = rememberCoroutineScope()
                            MainContent(navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch {
                                        drawerState.apply {
                                            if (isClosed) open() else close()
                                        }
                                    }
                                }) { Icon(Icons.Outlined.Menu, "") }
                            })
                        }
                    } else {
                        PermanentNavigationDrawer(
                            drawerContent = { DrawerContent() }
                        ) {
                            MainContent()
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TopAppBarMenu() {
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(Icons.AutoMirrored.Outlined.Sort, "")
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        Text(stringResource(R.string.SortRollsBy), modifier = Modifier.padding(horizontal = 8.dp))
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Date)) },
            onClick = { /*TODO*/ },
            leadingIcon = { Icon(Icons.Outlined.CalendarToday, "") },
            trailingIcon = { RadioButton(selected = true, onClick = { /*TODO*/ }) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = { /*TODO*/ },
            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, "") },
            trailingIcon = { RadioButton(selected = false, onClick = { /*TODO*/ }) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Camera)) },
            onClick = { /*TODO*/ },
            leadingIcon = { Icon(Icons.Outlined.CameraAlt, "") },
            trailingIcon = { RadioButton(selected = false, onClick = { /*TODO*/ }) }
        )
    }
}

@Preview
@Composable
fun DrawerContent() {
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
                    selected = true,
                    badge = { Text("123", fontWeight = FontWeight.Normal) },
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Archived)) },
                    icon = { Icon(Icons.Outlined.Inventory2, "") },
                    selected = false,
                    badge = { Text("321", fontWeight = FontWeight.Normal) },
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Favorites)) },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, "") },
                    selected = false,
                    badge = { Text("123", fontWeight = FontWeight.Normal) },
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.All)) },
                    icon = { Icon(Icons.Outlined.AllInclusive, "") },
                    selected = false,
                    badge = { Text("321", fontWeight = FontWeight.Normal) },
                    onClick = { /*TODO*/ }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
            Column(modifier = Modifier.padding(8.dp)) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Map)) },
                    icon = { Icon(Icons.Outlined.Map, "") },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Gear)) },
                    icon = { Icon(Icons.Outlined.Camera, "") },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Settings)) },
                    icon = { Icon(Icons.Outlined.Settings, "") },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.ManageLabels)) },
                    icon = { Icon(Icons.Outlined.NewLabel, "") },
                    selected = false,
                    onClick = { /*TODO*/ }
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
                val labels = (1..20).map { "placeholder-label-$it" }.toList()
                labels.forEach { label ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Label, "") },
                        selected = false,
                        onClick = { /*TODO*/ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(navigationIcon: @Composable () -> Unit = {}) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.app_name))
                        Text(stringResource(R.string.ActiveRolls), fontSize = 16.sp)
                    }
                },
                navigationIcon = navigationIcon,
                actions = {
                    TopAppBarMenu()
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Outlined.Add, "") },
                text = { Text(stringResource(R.string.NewRoll)) },
                onClick = { /*TODO*/ }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Text(
            text = "Android",
            modifier = Modifier.padding(innerPadding)
        )
    }
}