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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Theaters
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.core.entities.RollSortMode
import com.tommihirvonen.exifnotes.core.sortableDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun RollsList(
    rollsModel: RollsViewModel,
    onNavigateToLabels: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerContent = {
                    DrawerContent(
                        model = rollsModel,
                        onNavigateToLabels = onNavigateToLabels,
                        onNavigateToSettings = onNavigateToSettings,
                        drawerState = drawerState
                    )
                },
                drawerState = drawerState,
                gesturesEnabled = true
            ) {
                val scope = rememberCoroutineScope()
                MainContent(
                    model = rollsModel,
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) { Icon(Icons.Outlined.Menu, "") }
                    }
                )
            }
        } else {
            PermanentNavigationDrawer(
                drawerContent = {
                    DrawerContent(
                        model = rollsModel,
                        onNavigateToLabels = onNavigateToLabels,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            ) {
                MainContent(model = rollsModel)
            }
        }
    }
}

@Composable
fun DrawerContent(
    model: RollsViewModel,
    onNavigateToLabels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    drawerState: DrawerState? = null,
) {
    val labels = model.labels.collectAsState()
    val filter = model.rollFilterMode.collectAsState()
    val counts = model.rollCounts.collectAsState()
    val all = counts.value.active + counts.value.archived
    val scope = rememberCoroutineScope()
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
                    selected = filter.value is RollFilterMode.Active,
                    badge = { Text(counts.value.active.toString()) },
                    onClick = {
                        model.setRollFilterMode(RollFilterMode.Active)
                        scope.launch { drawerState?.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Archived)) },
                    icon = { Icon(Icons.Outlined.Inventory2, "") },
                    selected = filter.value is RollFilterMode.Archived,
                    badge = { Text(counts.value.archived.toString()) },
                    onClick = {
                        model.setRollFilterMode(RollFilterMode.Archived)
                        scope.launch { drawerState?.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.Favorites)) },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, "") },
                    selected = filter.value is RollFilterMode.Favorites,
                    badge = { Text(counts.value.favorites.toString()) },
                    onClick = {
                        model.setRollFilterMode(RollFilterMode.Favorites)
                        scope.launch { drawerState?.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.All)) },
                    icon = { Icon(Icons.Outlined.AllInclusive, "") },
                    selected = filter.value is RollFilterMode.All,
                    badge = { Text(all.toString()) },
                    onClick = {
                        model.setRollFilterMode(RollFilterMode.All)
                        scope.launch { drawerState?.close() }
                    }
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
                    onClick = {
                        scope.launch { drawerState?.close() }
                        onNavigateToSettings()
                    }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.ManageLabels)) },
                    icon = { Icon(Icons.Outlined.NewLabel, "") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState?.close() }
                        onNavigateToLabels()
                    }
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
                val filterValue = filter.value
                labels.value.forEach { label ->
                    NavigationDrawerItem(
                        label = { Text(label.name) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Label, "") },
                        selected = filterValue is RollFilterMode.HasLabel && filterValue.label.id == label.id,
                        badge = { Text(label.rollCount.toString()) },
                        onClick = {
                            model.setRollFilterMode(RollFilterMode.HasLabel(label))
                            scope.launch { drawerState?.close() }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    model: RollsViewModel,
    navigationIcon: @Composable () -> Unit = {},
) {
    val rolls = model.rolls.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val filter = model.rollFilterMode.collectAsState()
    val subtitle = when (val filterValue = filter.value) {
        RollFilterMode.Active -> stringResource(R.string.ActiveRolls)
        RollFilterMode.All -> stringResource(R.string.AllRolls)
        RollFilterMode.Archived -> stringResource(R.string.ArchivedRolls)
        RollFilterMode.Favorites -> stringResource(R.string.Favorites)
        is RollFilterMode.HasLabel -> filterValue.label.name
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.app_name))
                        Text(subtitle, fontSize = 16.sp)
                    }
                },
                navigationIcon = navigationIcon,
                actions = {
                    TopAppBarMenu(model)
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
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { innerPadding ->
        val state = rolls.value
        if (state is State.InProgress) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(vertical = 48.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else if (state is State.Success) {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = state.data,
                    key = { roll -> roll.id }
                ) { roll ->
                    RollCard(
                        roll = roll,
                        selected = false,
                        scope = scope,
                        snackBarHostState = snackBarHostState
                    )
                }
            }
        }
    }
}

@Composable
fun TopAppBarMenu(model: RollsViewModel) {
    val sort = model.rollSortMode.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(Icons.AutoMirrored.Outlined.Sort, "")
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        Text(stringResource(R.string.SortRollsBy), modifier = Modifier.padding(horizontal = 8.dp))
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Date)) },
            onClick = {
                model.setRollSortMode(RollSortMode.DATE)
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Outlined.CalendarToday, "") },
            trailingIcon = {
                RadioButton(
                    selected = sort.value == RollSortMode.DATE,
                    onClick = {
                        model.setRollSortMode(RollSortMode.DATE)
                        showMenu = false
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = {
                model.setRollSortMode(RollSortMode.NAME)
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, "") },
            trailingIcon = {
                RadioButton(
                    selected = sort.value == RollSortMode.NAME,
                    onClick = {
                        model.setRollSortMode(RollSortMode.NAME)
                        showMenu = false
                    }
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Camera)) },
            onClick = {
                model.setRollSortMode(RollSortMode.CAMERA)
                showMenu = false
            },
            leadingIcon = { Icon(Icons.Outlined.CameraAlt, "") },
            trailingIcon = {
                RadioButton(
                    selected = sort.value == RollSortMode.CAMERA,
                    onClick = {
                        model.setRollSortMode(RollSortMode.CAMERA)
                        showMenu = false
                    }
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RollCardPreview() {
    val filmStock = FilmStock(make = "Tommi's Lab", model = "Rainbow 400", iso = 400)
    val camera = Camera(make = "TomCam Factory", model = "Pocket 9000")
    val roll = Roll(
        name = "Placeholder roll",
        date = LocalDateTime.of(2024, 1, 1, 0, 0),
        unloaded = LocalDateTime.of(2024, 2, 1, 0, 0),
        developed = LocalDateTime.of(2024, 3, 1, 0, 0),
        camera = camera,
        filmStock = filmStock,
        note = "Test note ".repeat(10)
    )
    RollCard(
        roll = roll,
        selected = true,
        scope = null,
        snackBarHostState = null
    )
}

@Composable
fun RollCard(
    roll: Roll,
    selected: Boolean = false,
    scope: CoroutineScope?,
    snackBarHostState: SnackbarHostState?
) {
    val filmStock = roll.filmStock
    val camera = roll.camera
    val note = roll.note ?: ""
    val developed = roll.developed
    val unloaded = roll.unloaded
    val (date, state) = when {
        developed != null -> developed.sortableDateTime to stringResource(R.string.Developed)
        unloaded != null -> unloaded.sortableDateTime to stringResource(R.string.Unloaded)
        else -> roll.date.sortableDateTime to stringResource(R.string.Loaded)
    }

    val cardColor = if (selected) Color.Unspecified else Color.Transparent
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            onClick = {
                scope?.launch {
                    snackBarHostState?.showSnackbar(roll.name ?: "")
                }
            }
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row {
                        Text(
                            text = roll.name ?: "",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (filmStock != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.Filled.CameraRoll,
                                contentDescription = ""
                            )
                            Text(
                                text = filmStock.name,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row {
                        Row(
                            modifier = Modifier.weight(0.7f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = ""
                            )
                            if (camera != null) {
                                Text(camera.name, fontSize = 13.sp)
                            } else {
                                Text(stringResource(R.string.NoCamera), fontSize = 13.sp)
                            }
                        }
                        Row(
                            modifier = Modifier.weight(0.3f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Filled.Theaters,
                                contentDescription = ""
                            )
                            Text(
                                pluralStringResource(
                                    R.plurals.PhotosAmount, roll.frames.size, roll.frames.size
                                ),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Row {
                        Row(
                            modifier = Modifier.weight(0.7f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = ""
                            )
                            Text(date, fontSize = 13.sp)
                        }
                        Box (modifier = Modifier.weight(0.3f)){
                            Text(state, fontSize = 13.sp)
                        }
                    }
                    if (note.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = ""
                            )
                            Text(
                                text = note,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        if (selected) {
            Box(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Box(modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape
                    )
                    .background(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Icon(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp),
                            imageVector = Icons.Filled.Check,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
        }
    }
}