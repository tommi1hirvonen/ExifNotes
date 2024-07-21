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
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tommihirvonen.exifnotes.screens.GpsCheckDialog
import com.tommihirvonen.exifnotes.screens.TermsOfUseDialog
import com.tommihirvonen.exifnotes.screens.frames.FramesScreen
import com.tommihirvonen.exifnotes.screens.gear.GearScreen
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStockEditScreen
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStocksViewModel
import com.tommihirvonen.exifnotes.screens.gear.filters.FilterEditScreen
import com.tommihirvonen.exifnotes.screens.labels.LabelEditScreen
import com.tommihirvonen.exifnotes.screens.labels.LabelsScreen
import com.tommihirvonen.exifnotes.screens.main.MainScreen
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.screens.rollsmap.RollsMapScreen
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
        GpsCheckDialog(
            settingsViewModel = settingsViewModel
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
                    onNavigateToRoll = { roll ->
                        navController.navigate(route = Frames(roll.id))
                    },
                    onNavigateToMap = { navController.navigate(route = RollsMap) },
                    onNavigateToGear = { navController.navigate(route = Gear) },
                    onNavigateToLabels = { navController.navigate(route = Labels) },
                    onNavigateToSettings = { navController.navigate(route = Settings) }
                )
            }
            composable<Frames> { backStackEntry ->
                val frames = backStackEntry.toRoute<Frames>()
                FramesScreen(
                    rollId = frames.rollId,
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            composable<RollsMap> {
                RollsMapScreen(
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            composable<Gear> {
                val gearViewModel = hiltViewModel<GearViewModel>()
                val filmStocksViewModel = hiltViewModel<FilmStocksViewModel>()
                GearScreen(
                    gearViewModel = gearViewModel,
                    filmStocksViewModel = filmStocksViewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onEditCamera = { /*TODO*/ },
                    onEditLens = { /*TODO*/ },
                    onEditFilter = { filter ->
                        navController.navigate(route = FilterEdit(filter?.id ?: -1))
                    },
                    onEditFilmStock = { filmStock ->
                        navController.navigate(route = FilmStockEdit(filmStock?.id ?: -1))
                    }
                )
            }
            dialog<FilterEdit> { backStackEntry ->
                val gearEntry = remember(backStackEntry) { navController.getBackStackEntry<Gear>() }
                val gearViewModel = hiltViewModel<GearViewModel>(gearEntry)
                val filter = backStackEntry.toRoute<FilterEdit>()
                FilterEditScreen(
                    filterId = filter.filterId,
                    onDismiss = { navController.navigateUp() },
                    gearViewModel = gearViewModel
                )
            }
            dialog<FilmStockEdit> { backStackEntry ->
                val gearEntry = remember(backStackEntry) { navController.getBackStackEntry<Gear>() }
                val filmStocksViewModel = hiltViewModel<FilmStocksViewModel>(gearEntry)
                val filmStock = backStackEntry.toRoute<FilmStockEdit>()
                FilmStockEditScreen(
                    filmStockId = filmStock.filmStockId,
                    onDismiss = { navController.navigateUp() },
                    filmStocksViewModel = filmStocksViewModel
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
private object RollsMap

@Serializable
private object Gear

@Serializable
private data class FilterEdit(val filterId: Long)

@Serializable
private data class FilmStockEdit(val filmStockId: Long)

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

@Serializable
private data class Frames(val rollId: Long)