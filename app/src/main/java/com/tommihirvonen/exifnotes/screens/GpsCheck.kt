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

package com.tommihirvonen.exifnotes.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel

@Composable
fun GpsCheckDialog(
    settingsViewModel: SettingsViewModel
) {
    val isLocationUpdatesEnabled = remember { settingsViewModel.locationUpdatesEnabled.value }
    val isGpsEnabled = remember { settingsViewModel.isGpsEnabled() }
    var showDialog by remember {
        mutableStateOf(!isGpsEnabled && isLocationUpdatesEnabled)
    }
    if (showDialog) {
        val context = LocalContext.current
        GpsCheckDialogContent(
            onDismiss = { showDialog = false },
            onDisableLocationUpdates = {
                showDialog = false
                settingsViewModel.setLocationUpdatesEnabled(false)
            },
            onGoToSettings = {
                showDialog = false
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
        )
    }
}

@Preview
@Composable
private fun GpsCheckDialogPreview() {
    GpsCheckDialogContent(
        onDismiss = {},
        onDisableLocationUpdates = {},
        onGoToSettings = {}
    )
}

@Composable
private fun GpsCheckDialogContent(
    onDismiss: () -> Unit,
    onDisableLocationUpdates: () -> Unit,
    onGoToSettings: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        DialogContent {
            Column(modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.GPSSettings),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.GPSNotEnabled)
                )
                Spacer(modifier = Modifier.height(14.dp))
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.Cancel))
                }
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onGoToSettings
                ) {
                    Text(stringResource(R.string.GoToSettings))
                }
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onDisableLocationUpdates
                ) {
                    Text(stringResource(R.string.DisableInApp))
                }
            }
        }
    }
}