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

package com.tommihirvonen.exifnotes.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.HdrAuto
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.screens.StyledText
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.theme.Theme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel
import com.tommihirvonen.exifnotes.util.linkify
import com.tommihirvonen.exifnotes.util.packageInfo
import com.tommihirvonen.exifnotes.util.sortableDate
import com.tommihirvonen.exifnotes.util.textResource
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToThirdPartyLicenses: () -> Unit
) {
    val locationUpdatesEnabled = settingsViewModel.locationUpdatesEnabled.collectAsState()
    val artistName = settingsViewModel.artistName.collectAsState()
    val copyrightInfo = settingsViewModel.copyrightInfo.collectAsState()
    val exiftoolPath = settingsViewModel.exiftoolPath.collectAsState()
    val pathToPictures = settingsViewModel.pathToPictures.collectAsState()
    val fileEnding = settingsViewModel.fileEnding.collectAsState()
    val ignoreWarnings = settingsViewModel.ignoreWarnings.collectAsState()
    val theme = themeViewModel.theme.collectAsState()

    SettingsList(
        locationUpdatesEnabled = locationUpdatesEnabled.value,
        onLocationUpdatesSet = settingsViewModel::setLocationUpdatesEnabled,
        artistName = artistName.value,
        onArtistNameSet = settingsViewModel::setArtistName,
        copyrightInfo = copyrightInfo.value,
        onCopyrightInfoSet = settingsViewModel::setCopyrightInfo,
        exiftoolPath = exiftoolPath.value,
        onExiftoolPathSet = settingsViewModel::setExiftoolPath,
        pathToPictures = pathToPictures.value,
        onPathToPicturesSet = settingsViewModel::setPathToPictures,
        fileEnding = fileEnding.value,
        onFileEndingSet = settingsViewModel::setFileEnding,
        ignoreWarnings = ignoreWarnings.value,
        onIgnoreWarningsSet = settingsViewModel::setIgnoreWarnings,
        theme = theme.value,
        onThemeSet = themeViewModel::setTheme,
        onPicturesExportRequested = settingsViewModel::exportComplementaryPictures,
        onPicturesImportRequested = settingsViewModel::importComplementaryPictures,
        onDatabaseExportRequested = { uri, callback ->
            settingsViewModel.exportDatabase(
                destinationUri = uri,
                onSuccess = callback::onSuccess,
                onError = callback::onError
            )
        },
        onDatabaseImportRequested = { uri, callback ->
            settingsViewModel.importDatabase(
                sourceUri = uri,
                onSuccess = { message ->
                    callback.onSuccess(message)
                    mainViewModel.loadAll()
                },
                onError = callback::onError
            )
        },
        onNavigateUp = onNavigateUp,
        onNavigateToLicense = onNavigateToLicense,
        onNavigateToThirdPartyLicenses = onNavigateToThirdPartyLicenses
    )
}

@Preview(heightDp = 1600)
@Composable
private fun SettingsListPreview() {
    SettingsList(
        locationUpdatesEnabled = true,
        onLocationUpdatesSet = {},
        artistName = "",
        onArtistNameSet = {},
        copyrightInfo = "",
        onCopyrightInfoSet = {},
        exiftoolPath = "",
        onExiftoolPathSet = {},
        pathToPictures = "",
        onPathToPicturesSet = {},
        fileEnding = "",
        onFileEndingSet = {},
        ignoreWarnings = true,
        onIgnoreWarningsSet = {},
        theme = Theme.Auto,
        onThemeSet = {},
        onPicturesExportRequested = {},
        onPicturesImportRequested = {},
        onDatabaseExportRequested = { _, _ -> },
        onDatabaseImportRequested = { _, _ -> },
        onNavigateUp = {},
        onNavigateToLicense = {},
        onNavigateToThirdPartyLicenses = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsList(
    locationUpdatesEnabled: Boolean,
    onLocationUpdatesSet: (Boolean) -> Unit,
    artistName: String,
    onArtistNameSet: (String) -> Unit,
    copyrightInfo: String,
    onCopyrightInfoSet: (String) -> Unit,
    exiftoolPath: String,
    onExiftoolPathSet: (String) -> Unit,
    pathToPictures: String,
    onPathToPicturesSet: (String) -> Unit,
    fileEnding: String,
    onFileEndingSet: (String) -> Unit,
    ignoreWarnings: Boolean,
    onIgnoreWarningsSet: (Boolean) -> Unit,
    theme: Theme,
    onThemeSet: (Theme) -> Unit,
    onPicturesExportRequested: (Uri) -> Unit,
    onPicturesImportRequested: (Uri) -> Unit,
    onDatabaseExportRequested: (Uri, DatabaseExportCallback) -> Unit,
    onDatabaseImportRequested: (Uri, DatabaseImportCallback) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToThirdPartyLicenses: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showVersionHistoryDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showArtistNameDialog by remember { mutableStateOf(false) }
    var showCopyrightInfoDialog by remember { mutableStateOf(false) }
    var showExiftoolPathDialog by remember { mutableStateOf(false) }
    var showPathToPicturesDialog by remember { mutableStateOf(false) }
    var showFileEndingDialog by remember { mutableStateOf(false) }
    var showImportPicturesDialog by remember { mutableStateOf(false) }
    var showExportDatabaseDialog by remember { mutableStateOf(false) }
    var showImportDatabaseDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.Settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(stringResource(R.string.Information))
            SettingsItem(
                title = stringResource(R.string.About),
                icon = Icons.Outlined.Info,
                onClick = { showAboutDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.Help),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = { showHelpDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.VersionHistory),
                icon = Icons.Outlined.History,
                onClick = { showVersionHistoryDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.PrivacyPolicy),
                icon = Icons.Outlined.Policy,
                onClick = { showPrivacyPolicyDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.License),
                icon = Icons.AutoMirrored.Outlined.Article,
                onClick = onNavigateToLicense
            )
            SettingsItem(
                title = stringResource(R.string.ThirdPartyOpenSourceLicenses),
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                onClick = onNavigateToThirdPartyLicenses
            )
            SettingsHeader(stringResource(R.string.General))
            SettingsItem(
                title = stringResource(R.string.GPSUpdateTitle),
                subtitle = stringResource(R.string.GPSUpdateSummary),
                icon = Icons.Outlined.GpsNotFixed,
                onClick = {
                    val value = !locationUpdatesEnabled
                    onLocationUpdatesSet(value)
                }
            ) {
                Switch(
                    checked = locationUpdatesEnabled,
                    onCheckedChange = onLocationUpdatesSet
                )
            }
            val (themeSubtitle, themeIcon) = when (theme) {
                is Theme.Light -> stringResource(R.string.Light) to Icons.Outlined.LightMode
                is Theme.Dark -> stringResource(R.string.Dark) to Icons.Outlined.DarkMode
                is Theme.Auto -> stringResource(R.string.SystemDefault) to Icons.Outlined.HdrAuto
            }
            SettingsItem(
                title = stringResource(R.string.Theme),
                subtitle = themeSubtitle,
                icon = themeIcon,
                onClick = { showThemeDialog = true }
            )
            SettingsHeader(stringResource(R.string.ExiftoolIntegration))
            SettingsItem(
                title = stringResource(R.string.ArtistName),
                subtitle = stringResource(R.string.ArtistNameSummary),
                onClick = { showArtistNameDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.CopyrightInformationTitle),
                subtitle = stringResource(R.string.CopyrightInformationSummary),
                onClick = { showCopyrightInfoDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.ExiftoolPathTitle),
                subtitle = stringResource(R.string.ExiftoolPathSummary),
                onClick = { showExiftoolPathDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.PicturesPathTitle),
                subtitle = stringResource(R.string.PicturesPathSummary),
                onClick = { showPathToPicturesDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.FileEndingTitle),
                subtitle = stringResource(R.string.FileEndingSummary),
                onClick = { showFileEndingDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.IgnoreWarningsTitle),
                subtitle = stringResource(R.string.IgnoreWarningsSummary),
                onClick = {
                    val value = !ignoreWarnings
                    onIgnoreWarningsSet(value)
                }
            ) {
                Checkbox(
                    checked = ignoreWarnings,
                    onCheckedChange = onIgnoreWarningsSet
                )
            }
            SettingsHeader(stringResource(R.string.ComplementaryPictures))

            val exportingPicturesText = stringResource(R.string.StartedExportingComplementaryPictures)
            val createPicturesExportFile = rememberLauncherForActivityResult(
                CreatePicturesExportFile()
            ) { resultUri ->
                if (resultUri == null) {
                    return@rememberLauncherForActivityResult
                }
                onPicturesExportRequested(resultUri)
                scope.launch { snackbarHostState.showSnackbar(exportingPicturesText) }
            }
            SettingsItem(
                title = stringResource(R.string.ExportComplementaryPicturesTitle),
                subtitle = stringResource(R.string.ExportComplementaryPicturesSummary),
                onClick = {
                    val date = LocalDateTime.now().sortableDate
                    val title = "Exif_Notes_Complementary_Pictures_$date.zip"
                    createPicturesExportFile.launch(title)
                }
            )
            SettingsItem(
                title = stringResource(R.string.ImportComplementaryPicturesTitle),
                subtitle = stringResource(R.string.ImportComplementaryPicturesSummary),
                onClick = { showImportPicturesDialog = true }
            )
            SettingsHeader(stringResource(R.string.Database))
            SettingsItem(
                title = stringResource(R.string.ExportDatabaseTitle),
                subtitle = stringResource(R.string.ExportDatabaseSummary),
                onClick = { showExportDatabaseDialog = true }
            )
            SettingsItem(
                title = stringResource(R.string.ImportDatabaseTitle),
                subtitle = stringResource(R.string.ImportDatabaseSummary),
                onClick = { showImportDatabaseDialog = true }
            )
        }
    }

    if (showAboutDialog) {
        val versionName = LocalContext.current.packageInfo?.versionName ?: ""
        val text = stringResource(R.string.AboutAndTermsOfUse, versionName).linkify()
        InfoDialog(
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(text) },
            onDismiss = { showAboutDialog = false }
        )
    }
    if (showHelpDialog) {
        val text = stringResource(R.string.main_help).linkify()
        InfoDialog(
            title = { Text(stringResource(R.string.Help)) },
            text = { Text(text) },
            onDismiss = { showHelpDialog = false }
        )
    }
    if (showVersionHistoryDialog) {
        val text = stringResource(R.string.VersionHistoryStatement)
        InfoDialog(
            title = { Text(stringResource(R.string.VersionHistory)) },
            text = { Text(text) },
            onDismiss = { showVersionHistoryDialog = false }
        )
    }
    if (showPrivacyPolicyDialog) {
        val text = textResource(R.string.PrivacyPolicyStatement)
        InfoDialog(
            title = { Text(stringResource(R.string.PrivacyPolicy)) },
            text = { StyledText(text) },
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }
    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = theme,
            onDismiss = { showThemeDialog = false },
            onThemeSet = onThemeSet
        )
    }
    if (showArtistNameDialog) {
        EditSettingDialog(
            initialValue = artistName,
            title = { Text(stringResource(R.string.ArtistName)) },
            onDismiss = { showArtistNameDialog = false },
            onValueSet = onArtistNameSet
        )
    }
    if (showCopyrightInfoDialog) {
        EditSettingDialog(
            initialValue = copyrightInfo,
            title = { Text(stringResource(R.string.CopyrightInformationTitle)) },
            onDismiss = { showCopyrightInfoDialog = false },
            onValueSet = onCopyrightInfoSet
        )
    }
    if (showExiftoolPathDialog) {
        EditSettingDialog(
            initialValue = exiftoolPath,
            title = { Text(stringResource(R.string.ExiftoolPathTitle)) },
            onDismiss = { showExiftoolPathDialog = false },
            onValueSet = onExiftoolPathSet
        )
    }
    if (showPathToPicturesDialog) {
        EditSettingDialog(
            initialValue = pathToPictures,
            title = { Text(stringResource(R.string.PicturesPathTitle)) },
            onDismiss = { showPathToPicturesDialog = false },
            onValueSet = onPathToPicturesSet
        )
    }
    if (showFileEndingDialog) {
        EditSettingDialog(
            initialValue = fileEnding,
            title = { Text(stringResource(R.string.FileEndingTitle)) },
            onDismiss = { showFileEndingDialog = false },
            onValueSet = onFileEndingSet
        )
    }

    val importingPicturesText = stringResource(R.string.StartedImportingComplementaryPictures)
    val pickPicturesImportFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
        if (resultUri == null) {
            return@rememberLauncherForActivityResult
        }
        onPicturesImportRequested(resultUri)
        scope.launch { snackbarHostState.showSnackbar(importingPicturesText) }
    }
    if (showImportPicturesDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.ImportComplementaryPicturesTitle)) },
            text = { Text(stringResource(R.string.ImportComplementaryPicturesVerification)) },
            onDismissRequest = { showImportPicturesDialog = false },
            dismissButton = {
                TextButton(onClick = { showImportPicturesDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportPicturesDialog = false
                        pickPicturesImportFile.launch(arrayOf("application/zip"))
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            }
        )
    }

    val snackTextDbCopiedSuccessfully = stringResource(R.string.DatabaseCopiedSuccessfully)
    val snackTextDbCopyFailed = stringResource(R.string.ErrorExportingDatabase)
    val createDatabaseExportFile = rememberLauncherForActivityResult(CreateDatabaseExportFile()) { resultUri ->
        if (resultUri == null) {
            return@rememberLauncherForActivityResult
        }
        onDatabaseExportRequested(resultUri, object : DatabaseExportCallback {
            override fun onSuccess() {
                scope.launch { snackbarHostState.showSnackbar(snackTextDbCopiedSuccessfully) }
            }
            override fun onError() {
                scope.launch { snackbarHostState.showSnackbar(snackTextDbCopyFailed) }
            }
        })
    }
    if (showExportDatabaseDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.ExportDatabaseTitle)) },
            text = { Text(stringResource(R.string.ExportDatabaseVerification)) },
            onDismissRequest = { showExportDatabaseDialog = false },
            dismissButton = {
                TextButton(onClick = { showExportDatabaseDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportDatabaseDialog = false
                        val date = LocalDateTime.now().sortableDate
                        val filename = "Exif_Notes_Database_$date.db"
                        createDatabaseExportFile.launch(filename)
                    }
                ) {
                    Text(stringResource(R.string.OK))
                }
            }
        )
    }

    val pickDatabaseImportFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
        if (resultUri == null) {
            return@rememberLauncherForActivityResult
        }
        onDatabaseImportRequested(resultUri, object : DatabaseImportCallback {
            override fun onSuccess(message: String) {
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
            override fun onError(message: String) {
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        })
    }
    if (showImportDatabaseDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.ImportDatabaseTitle)) },
            text = { Text(stringResource(R.string.ImportDatabaseVerification)) },
            onDismissRequest = { showImportDatabaseDialog = false },
            dismissButton = {
                TextButton(onClick = { showImportDatabaseDialog = false }) {
                    Text(stringResource(R.string.No))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDatabaseDialog = false
                        pickDatabaseImportFile.launch(arrayOf("*/*"))
                    }
                ) {
                    Text(stringResource(R.string.Continue))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsHeaderPreview() {
    SettingsHeader("Test header")
}

@Composable
private fun SettingsHeader(header: String) {
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)) {
        Text(header, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsItemPreview() {
    SettingsItem(
        title = "Test setting title",
        subtitle = "Test setting summary",
        icon = Icons.Outlined.LocationSearching,
        onClick = {},
        content = {
            Switch(checked = true, onCheckedChange = {})
        }
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    Box(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier
                .weight(0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            Icon(
                                imageVector = icon,
                                contentDescription = ""
                            )
                        }
                    }
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            if (content != null) {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Preview
@Composable
private fun EditSettingsDialogPreview() {
    EditSettingDialog(
        initialValue = "Test value",
        title = { Text("Edit test value") },
        onDismiss = {},
        onValueSet = {}
    )
}

@Composable
private fun EditSettingDialog(
    initialValue: String,
    title: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onValueSet: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        title = { title() },
        text = {
            Column {
                TextField(
                    value = value,
                    onValueChange = { text -> value = text }
                )
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.Cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onValueSet(value)
                }
            ) {
                Text(stringResource(R.string.OK))
            }
        }
    )
}

@Preview
@Composable
private fun InfoDialogPreview() {
    InfoDialog(
        title = { Text("Title") },
        text = { Text("Text") },
        onDismiss = {}
    )
}

@Composable
private fun InfoDialog(
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = { title() },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                text()
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Close))
            }
        }
    )
}

@Preview
@Composable
private fun ThemeDialogPreview() {
    ThemeDialog(currentTheme = Theme.Auto, onDismiss = {}, onThemeSet = {})
}

@Composable
private fun ThemeDialog(
    currentTheme: Theme,
    onDismiss: () -> Unit,
    onThemeSet: (Theme) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.Cancel))
            }
        },
        title = { Text(stringResource(R.string.Theme)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onThemeSet(Theme.Auto)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme is Theme.Auto,
                        onClick = {
                            onDismiss()
                            onThemeSet(Theme.Auto)
                        }
                    )
                    Text(stringResource(R.string.SystemDefault))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onThemeSet(Theme.Light)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme is Theme.Light,
                        onClick = {
                            onDismiss()
                            onThemeSet(Theme.Light)
                        }
                    )
                    Text(stringResource(R.string.Light))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onThemeSet(Theme.Dark)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme is Theme.Dark,
                        onClick = {
                            onDismiss()
                            onThemeSet(Theme.Dark)
                        }
                    )
                    Text(stringResource(R.string.Dark))
                }
            }
        }
    )
}

private interface DatabaseExportCallback {
    fun onSuccess()
    fun onError()
}

private interface DatabaseImportCallback {
    fun onSuccess(message: String)
    fun onError(message: String)
}

private class CreatePicturesExportFile : CreateDocument("application/zip") {
    override fun createIntent(context: Context, input: String): Intent {
        val intent = super.createIntent(context, input)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/zip"
        return intent
    }
}

private class CreateDatabaseExportFile : CreateDocument("*/*") {
    override fun createIntent(context: Context, input: String): Intent {
        val intent = super.createIntent(context, input)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        return intent
    }
}