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

import android.webkit.WebView
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
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tommihirvonen.exifnotes.theme.Theme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateUp: () -> Unit = {},
    onNavigateToLicense: () -> Unit = {},
    onNavigateToThirdPartyLicenses: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val showAboutDialog = remember { mutableStateOf(false) }
    val showHelpDialog = remember { mutableStateOf(false) }
    val showVersionHistoryDialog = remember { mutableStateOf(false) }
    val showPrivacyPolicyDialog = remember { mutableStateOf(false) }
    val showThemeDialog = remember { mutableStateOf(false) }

    val locationUpdatesEnabled = settingsViewModel.locationUpdatesEnabled.collectAsState()
    val ignoreWarnings = settingsViewModel.ignoreWarnings.collectAsState()
    val theme = themeViewModel.theme.collectAsState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.Settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
            )
        }
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
                onClick = { showAboutDialog.value = true }
            )
            SettingsItem(
                title = stringResource(R.string.Help),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = { showHelpDialog.value = true }
            )
            SettingsItem(
                title = stringResource(R.string.VersionHistory),
                icon = Icons.Outlined.History,
                onClick = { showVersionHistoryDialog.value = true }
            )
            SettingsItem(
                title = stringResource(R.string.PrivacyPolicy),
                icon = Icons.Outlined.Policy,
                onClick = { showPrivacyPolicyDialog.value = true }
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
            HorizontalDivider()
            SettingsHeader(stringResource(R.string.General))
            SettingsItem(
                title = stringResource(R.string.GPSUpdateTitle),
                subtitle = stringResource(R.string.GPSUpdateSummary),
                icon = Icons.Outlined.GpsNotFixed,
                onClick = {
                    val value = !locationUpdatesEnabled.value
                    settingsViewModel.setLocationUpdatesEnabled(value)
                }
            ) {
                Switch(
                    checked = locationUpdatesEnabled.value,
                    onCheckedChange = { value ->
                        settingsViewModel.setLocationUpdatesEnabled(value)
                    }
                )
            }
            val (themeSubtitle, themeIcon) = when (theme.value) {
                is Theme.Light -> stringResource(R.string.Light) to Icons.Outlined.LightMode
                is Theme.Dark -> stringResource(R.string.Dark) to Icons.Outlined.DarkMode
                is Theme.Auto -> stringResource(R.string.SystemDefault) to Icons.Outlined.HdrAuto
            }
            SettingsItem(
                title = stringResource(R.string.Theme),
                subtitle = themeSubtitle,
                icon = themeIcon,
                onClick = { showThemeDialog.value = true }
            )
            HorizontalDivider()
            SettingsHeader(stringResource(R.string.ExiftoolIntegration))
            SettingsItem(
                title = stringResource(R.string.ArtistName),
                subtitle = stringResource(R.string.ArtistNameSummary)
            )
            SettingsItem(
                title = stringResource(R.string.CopyrightInformationTitle),
                subtitle = stringResource(R.string.CopyrightInformationSummary)
            )
            SettingsItem(
                title = stringResource(R.string.ExiftoolPathTitle),
                subtitle = stringResource(R.string.ExiftoolPathSummary)
            )
            SettingsItem(
                title = stringResource(R.string.PicturesPathTitle),
                subtitle = stringResource(R.string.PicturesPathSummary)
            )
            SettingsItem(
                title = stringResource(R.string.FileEndingTitle),
                subtitle = stringResource(R.string.FileEndingSummary)
            )
            SettingsItem(
                title = stringResource(R.string.IgnoreWarningsTitle),
                subtitle = stringResource(R.string.IgnoreWarningsSummary),
                onClick = {
                    val value = !ignoreWarnings.value
                    settingsViewModel.setIgnoreWarnings(value)
                }
            ) {
                Checkbox(
                    checked = ignoreWarnings.value,
                    onCheckedChange = { value ->
                        settingsViewModel.setIgnoreWarnings(value)
                    }
                )
            }
            HorizontalDivider()
            SettingsHeader(stringResource(R.string.ComplementaryPictures))
            SettingsItem(
                title = stringResource(R.string.ExportComplementaryPicturesTitle),
                subtitle = stringResource(R.string.ExportComplementaryPicturesSummary)
            )
            SettingsItem(
                title = stringResource(R.string.ImportComplementaryPicturesTitle),
                subtitle = stringResource(R.string.ImportComplementaryPicturesSummary)
            )
            HorizontalDivider()
            SettingsHeader(stringResource(R.string.Database))
            SettingsItem(
                title = stringResource(R.string.ExportDatabaseTitle),
                subtitle = stringResource(R.string.ExportDatabaseSummary)
            )
            SettingsItem(
                title = stringResource(R.string.ImportDatabaseTitle),
                subtitle = stringResource(R.string.ImportDatabaseSummary)
            )
        }
    }

    if (showAboutDialog.value) {
        val versionName = LocalContext.current.packageInfo?.versionName ?: ""
        val text = stringResource(R.string.AboutAndTermsOfUse, versionName).linkify()
        InfoDialog(
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(text) },
            onDismiss = { showAboutDialog.value = false }
        )
    }
    if (showHelpDialog.value) {
        val text = stringResource(R.string.main_help).linkify()
        InfoDialog(
            title = { Text(stringResource(R.string.Help)) },
            text = { Text(text) },
            onDismiss = { showHelpDialog.value = false }
        )
    }
    if (showVersionHistoryDialog.value) {
        val text = stringResource(R.string.VersionHistoryStatement)
        InfoDialog(
            title = { Text(stringResource(R.string.VersionHistory)) },
            text = { Text(text) },
            onDismiss = { showVersionHistoryDialog.value = false }
        )
    }
    if (showPrivacyPolicyDialog.value) {
        val text = textResource(R.string.PrivacyPolicyStatement)
        InfoDialog(
            title = { Text(stringResource(R.string.PrivacyPolicy)) },
            text = { StyledText(text) },
            onDismiss = { showPrivacyPolicyDialog.value = false }
        )
    }
    if (showThemeDialog.value) {
        ThemeDialog(
            currentTheme = theme.value,
            onDismiss = { showThemeDialog.value = false },
            onThemeSet = { t -> themeViewModel.setTheme(t) }
        )
    }
}

@Composable
private fun SettingsHeader(header: String) {
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
        Text(header, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
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
                        Text(title, fontWeight = FontWeight.Bold)
                        if (subtitle != null) {
                            Text(subtitle, color = MaterialTheme.colorScheme.secondary)
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
private fun ThemeDialog(
    currentTheme: Theme = Theme.Auto,
    onDismiss: () -> Unit = {},
    onThemeSet: (Theme) -> Unit = {}
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun License(onNavigateUp: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.License)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())) {
            AndroidView(
                factory = { context -> WebView(context) },
                update = { webView ->
                    webView.loadUrl("file:///android_asset/license.html")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdPartyLicenses(onNavigateUp: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.ThirdPartyOpenSourceLicenses)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())) {
            AndroidView(
                factory = { context -> WebView(context) },
                update = { webView ->
                    webView.loadUrl("file:///android_asset/open_source_licenses.html")
                }
            )
        }
    }
}