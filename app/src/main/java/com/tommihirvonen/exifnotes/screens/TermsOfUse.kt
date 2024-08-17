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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel
import com.tommihirvonen.exifnotes.util.linkify
import com.tommihirvonen.exifnotes.util.packageInfo

@Composable
fun TermsOfUseDialog(
    settingsViewModel: SettingsViewModel,
    onFinish: () -> Unit
) {
    val accepted = settingsViewModel.termsOfUseAccepted.collectAsState()
    if (!accepted.value) {
        val versionName = LocalContext.current.packageInfo?.versionName ?: ""
        TermsOfUseDialogContent(
            versionName = versionName,
            onAgree = settingsViewModel::acceptTermsOfUse,
            onDecline = onFinish
        )
    }
}

@Preview
@Composable
private fun TermsOfUseDialogContentPreview() {
    TermsOfUseDialogContent(
        versionName = "v123.321",
        onAgree = {},
        onDecline = {}
    )
}

@Composable
private fun TermsOfUseDialogContent(
    versionName: String,
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    val about = stringResource(R.string.AboutAndTermsOfUse, versionName)
    val updates = stringResource(R.string.Updates)
    val message = "$about\n\n\n$updates"
    val annotated = message.linkify()
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(stringResource(R.string.Agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.Decline))
            }
        },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(annotated)
            }
        }
    )
}