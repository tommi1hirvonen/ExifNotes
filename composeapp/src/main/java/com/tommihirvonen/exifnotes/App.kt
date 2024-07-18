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

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tommihirvonen.exifnotes.screens.TermsOfUseDialog
import com.tommihirvonen.exifnotes.screens.labeledit.LabelEditScreen
import com.tommihirvonen.exifnotes.screens.labelslist.LabelsScreen
import com.tommihirvonen.exifnotes.screens.main.MainScreen
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.screens.settings.LicenseScreen
import com.tommihirvonen.exifnotes.screens.settings.SettingsScreen
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel
import com.tommihirvonen.exifnotes.screens.settings.ThirdPartyLicensesScreen
import com.tommihirvonen.exifnotes.theme.ExifNotesTheme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel
import kotlinx.serialization.Serializable

@Composable
fun App(onFinish: () -> Unit) {
    val themeModel = hiltViewModel<ThemeViewModel>()
    ExifNotesTheme(themeModel) {
        val navController = rememberNavController()
        val mainViewModel = hiltViewModel<MainViewModel>()
        val settingsViewModel = hiltViewModel<SettingsViewModel>()
        TermsOfUseDialog(
            settingsViewModel = settingsViewModel,
            onFinish = onFinish
        )
        NavHost(
            navController = navController,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally() },
            popEnterTransition = { slideInHorizontally() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
            startDestination = Main
        ) {
            composable<Main> {
                MainScreen(
                    mainViewModel = mainViewModel,
                    onNavigateToMap = { /*TODO*/ },
                    onNavigateToGear = { /*TODO*/ },
                    onNavigateToLabels = { navController.navigate(route = Labels) },
                    onNavigateToSettings = { navController.navigate(route = Settings) }
                )
            }
            composable<Labels> {
                LabelsScreen(
                    mainViewModel = mainViewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onEditLabel = { label ->
                        navController.navigate(route = LabelEdit(label?.id ?: -1))
                    }
                )
            }
            composable<Settings> {
                SettingsScreen(
                    themeViewModel = themeModel,
                    settingsViewModel = settingsViewModel,
                    mainViewModel = mainViewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToLicense = { navController.navigate(route = License) },
                    onNavigateToThirdPartyLicenses = {
                        navController.navigate(route = ThirdPartyLicenses)
                    }
                )
            }
            composable<License> {
                LicenseScreen(
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            composable<ThirdPartyLicenses> {
                ThirdPartyLicensesScreen(
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            dialog<LabelEdit> { backStackEntry ->
                val labelEdit = backStackEntry.toRoute<LabelEdit>()
                LabelEditScreen(
                    labelId = labelEdit.labelId,
                    onDismiss = { navController.navigateUp() },
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}

@Serializable
private object Main

@Serializable
private object Labels

@Serializable
private data class LabelEdit(val labelId: Long)

@Serializable
private object Settings

@Serializable
private object License

@Serializable
private object ThirdPartyLicenses