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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun Settings(onNavigateUp: () -> Unit = {}) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val showAboutDialog = remember { mutableStateOf(false) }
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
                title = stringResource(R.string.VersionHistory),
                icon = Icons.Outlined.History
            )
            SettingsItem(
                title = stringResource(R.string.PrivacyPolicy),
                icon = Icons.Outlined.Policy
            )
            SettingsItem(
                title = stringResource(R.string.License),
                icon = Icons.AutoMirrored.Outlined.Article
            )
            SettingsItem(
                title = stringResource(R.string.ThirdPartyOpenSourceLicenses),
                icon = Icons.AutoMirrored.Outlined.LibraryBooks
            )
            HorizontalDivider()
            SettingsHeader(stringResource(R.string.General))
            SettingsItem(
                title = stringResource(R.string.GPSUpdateTitle),
                subtitle = stringResource(R.string.GPSUpdateSummary),
                icon = Icons.Outlined.GpsNotFixed
            ) {
                Switch(checked = true, onCheckedChange = {})
            }
            SettingsItem(
                title = stringResource(R.string.Theme),
                subtitle = stringResource(R.string.SystemDefault),
                icon = Icons.Outlined.Contrast
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
                subtitle = stringResource(R.string.IgnoreWarningsSummary)
            ) {
                Checkbox(checked = false, onCheckedChange = {})
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
        val text = stringResource(R.string.AboutAndTermsOfUse, versionName)
        val annotated = text.linkify()
        AlertDialog(
            title = { Text(stringResource(R.string.app_name)) },
            text = { Text(annotated) },
            onDismissRequest = { showAboutDialog.value = false },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog.value = false }
                ) {
                    Text(stringResource(R.string.Close))
                }
            }
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