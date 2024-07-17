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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tommihirvonen.exifnotes.screens.labeledit.LabelForm
import com.tommihirvonen.exifnotes.screens.labelslist.LabelsList
import com.tommihirvonen.exifnotes.screens.main.RollsList
import com.tommihirvonen.exifnotes.screens.main.RollsViewModel
import com.tommihirvonen.exifnotes.screens.settings.License
import com.tommihirvonen.exifnotes.screens.settings.Settings
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel
import com.tommihirvonen.exifnotes.screens.settings.ThirdPartyLicenses
import com.tommihirvonen.exifnotes.theme.ExifNotesTheme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeModel = hiltViewModel<ThemeViewModel>()
            ExifNotesTheme(themeModel) {
                val navController = rememberNavController()
                val rollsModel = hiltViewModel<RollsViewModel>()
                val settingsModel = hiltViewModel<SettingsViewModel>()
                NavHost(
                    navController = navController,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                    exitTransition = { slideOutHorizontally() },
                    popEnterTransition = { slideInHorizontally() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                    startDestination = Rolls
                ) {
                    composable<Rolls> {
                        RollsList(
                            rollsModel = rollsModel,
                            onNavigateToLabels = { navController.navigate(route = Labels) },
                            onNavigateToSettings = { navController.navigate(route = Settings) }
                        )
                    }
                    composable<Labels> {
                        LabelsList(
                            rollsModel = rollsModel,
                            onNavigateUp = { navController.navigateUp() },
                            onEditLabel = { label ->
                                navController.navigate(route = LabelEdit(label?.id ?: -1))
                            }
                        )
                    }
                    composable<Settings> {
                        Settings(
                            themeViewModel = themeModel,
                            settingsViewModel = settingsModel,
                            rollsViewModel = rollsModel,
                            onNavigateUp = { navController.navigateUp() },
                            onNavigateToLicense = { navController.navigate(route = License) },
                            onNavigateToThirdPartyLicenses = {
                                navController.navigate(route = ThirdPartyLicenses)
                            }
                        )
                    }
                    composable<License> {
                        License(
                            onNavigateUp = { navController.navigateUp() }
                        )
                    }
                    composable<ThirdPartyLicenses> {
                        ThirdPartyLicenses(
                            onNavigateUp = { navController.navigateUp() }
                        )
                    }
                    dialog<LabelEdit> { backStackEntry ->
                        val labelEdit = backStackEntry.toRoute<LabelEdit>()
                        LabelForm(
                            labelId = labelEdit.labelId,
                            onDismiss = { navController.navigateUp() },
                            rollsModel = rollsModel
                        )
                    }
                }
            }
        }
    }
}

@Serializable
object Rolls

@Serializable
object Labels

@Serializable
data class LabelEdit(val labelId: Long)

@Serializable
object Settings

@Serializable
object License

@Serializable
object ThirdPartyLicenses