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
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.screens.GpsCheckDialog
import com.tommihirvonen.exifnotes.screens.TermsOfUseDialog
import com.tommihirvonen.exifnotes.screens.frames.FramesScreen
import com.tommihirvonen.exifnotes.screens.frames.FramesViewModel
import com.tommihirvonen.exifnotes.screens.gear.GearScreen
import com.tommihirvonen.exifnotes.screens.gear.GearViewModel
import com.tommihirvonen.exifnotes.screens.gear.cameras.CameraEditScreen
import com.tommihirvonen.exifnotes.screens.gear.cameras.CameraViewModel
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStockEditScreen
import com.tommihirvonen.exifnotes.screens.gear.filmstocks.FilmStocksViewModel
import com.tommihirvonen.exifnotes.screens.gear.filters.FilterEditScreen
import com.tommihirvonen.exifnotes.screens.gear.lenses.FixedLensEditScreen
import com.tommihirvonen.exifnotes.screens.gear.lenses.InterchangeableLensEditScreen
import com.tommihirvonen.exifnotes.screens.labels.LabelEditScreen
import com.tommihirvonen.exifnotes.screens.labels.LabelsScreen
import com.tommihirvonen.exifnotes.screens.main.MainScreen
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.screens.rolls.RollEditScreen
import com.tommihirvonen.exifnotes.screens.rolls.RollViewModel
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
                    onEditRoll = { roll ->
                        navController.navigate(route = RollEdit(rollId = roll?.id ?: -1))
                    },
                    onNavigateToRoll = { roll ->
                        navController.navigate(route = Frames(roll.id))
                    },
                    onNavigateToMap = { navController.navigate(route = RollsMap) },
                    onNavigateToGear = { navController.navigate(route = Gear) },
                    onNavigateToLabels = { navController.navigate(route = Labels) },
                    onNavigateToSettings = { navController.navigate(route = Settings) }
                )
            }
            composable<RollEdit> { backStackEntry ->
                val rollEdit = backStackEntry.toRoute<RollEdit>()
                RollEditScreen(
                    rollId = rollEdit.rollId,
                    onNavigateUp = { navController.navigateUp() },
                    onEditFilmStock = { filmStock ->
                        val route = RollFilmStockEdit(filmStockId = filmStock?.id ?: -1)
                        navController.navigate(route = route)
                    },
                    onEditCamera = { camera ->
                        val route = RollCameraEdit(cameraId = camera?.id ?: -1)
                        navController.navigate(route = route)
                    },
                    mainViewModel = mainViewModel
                )
            }
            composable<Frames> { backStackEntry ->
                val frames = backStackEntry.toRoute<Frames>()
                FramesScreen(
                    rollId = frames.rollId,
                    onEditRoll = {
                        navController.navigate(route = FramesRollEdit(rollId = frames.rollId))
                    },
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            composable<FramesRollEdit> { backStackEntry ->
                val rollEdit = backStackEntry.toRoute<FramesRollEdit>()
                val framesEntry = remember(backStackEntry) { navController.getBackStackEntry<Frames>() }
                val framesViewModel = hiltViewModel<FramesViewModel>(framesEntry)
                RollEditScreen(
                    rollId = rollEdit.rollId,
                    onNavigateUp = { navController.navigateUp() },
                    onEditFilmStock = { filmStock ->
                        val route = RollFilmStockEdit(filmStockId = filmStock?.id ?: -1)
                        navController.navigate(route = route)
                    },
                    onEditCamera = { camera ->
                        val route = RollCameraEdit(cameraId = camera?.id ?: -1)
                        navController.navigate(route = route)
                    },
                    mainViewModel = mainViewModel,
                    framesViewModel = framesViewModel
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
                    onNavigateUp = {
                        mainViewModel.loadAll()
                        navController.navigateUp()
                    },
                    onEditCamera = { camera ->
                        navController.navigate(route = CameraEdit(cameraId = camera?.id ?: -1))
                    },
                    onEditLens = { lens ->
                        val route = LensEdit(lensId = lens?.id ?: -1, fixedLens = false)
                        navController.navigate(route = route)
                    },
                    onEditFilter = { filter ->
                        navController.navigate(route = FilterEdit(filter?.id ?: -1))
                    },
                    onEditFilmStock = { filmStock ->
                        navController.navigate(route = FilmStockEdit(filmStock?.id ?: -1))
                    }
                )
            }
            composable<CameraEdit> { backStackEntry ->
                val gearEntry = remember(backStackEntry) { navController.getBackStackEntry<Gear>() }
                val gearViewModel = hiltViewModel<GearViewModel>(gearEntry)
                val camera = backStackEntry.toRoute<CameraEdit>()
                CameraEditScreen(
                    cameraId = camera.cameraId,
                    onNavigateUp = { navController.navigateUp() },
                    onEditFixedLens = { navController.navigate(route = FixedLensEdit) },
                    gearViewModel = gearViewModel
                )
            }
            composable<RollCameraEdit> { backStackEntry ->
                val rollEditEntry = remember(backStackEntry) { navController.getBackStackEntry<RollEdit>() }
                val camera = backStackEntry.toRoute<RollCameraEdit>()
                val rollViewModel = hiltViewModel<RollViewModel>(rollEditEntry)
                CameraEditScreen(
                    cameraId = camera.cameraId,
                    onNavigateUp = { navController.navigateUp() },
                    onEditFixedLens = { navController.navigate(route = FixedLensEdit) },
                    afterSubmit = { c ->
                        rollViewModel.setCamera(c)
                        mainViewModel.loadAll()
                    }
                )
            }
            composable<FixedLensEdit> { backStackEntry ->
                val cameraEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry<CameraEdit>()
                    } catch (e: IllegalArgumentException) {
                        navController.getBackStackEntry<RollCameraEdit>()
                    }
                }
                val cameraViewModel = hiltViewModel<CameraViewModel>(cameraEntry)
                FixedLensEditScreen(
                    initialLens = cameraViewModel.camera.value.lens ?: Lens(),
                    onCancel = { navController.navigateUp() },
                    onSubmit = { lens ->
                        cameraViewModel.setLens(lens)
                        navController.navigateUp()
                    }
                )
            }
            composable<LensEdit> { backStackEntry ->
                val gearEntry = remember(backStackEntry) { navController.getBackStackEntry<Gear>() }
                val gearViewModel = hiltViewModel<GearViewModel>(gearEntry)
                val lens = backStackEntry.toRoute<LensEdit>()
                InterchangeableLensEditScreen(
                    lensId =  lens.lensId,
                    onNavigateUp = { navController.navigateUp() },
                    gearViewModel = gearViewModel
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
                    onNavigateUp = { navController.navigateUp() },
                    filmStocksViewModel = filmStocksViewModel
                )
            }
            dialog<RollFilmStockEdit> { backStackEntry ->
                val rollEditEntry = remember(backStackEntry) { navController.getBackStackEntry<RollEdit>() }
                val filmStock = backStackEntry.toRoute<RollFilmStockEdit>()
                val rollViewModel = hiltViewModel<RollViewModel>(rollEditEntry)
                FilmStockEditScreen(
                    filmStockId = filmStock.filmStockId,
                    onNavigateUp = { navController.navigateUp() },
                    afterSubmit = { stock ->
                        rollViewModel.setFilmStock(stock)
                        mainViewModel.loadAll()
                    }
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
private data class RollEdit(val rollId: Long)

@Serializable
private data class Frames(val rollId: Long)

@Serializable
private data class FramesRollEdit(val rollId: Long)

@Serializable
private object RollsMap

@Serializable
private object Gear

@Serializable
private data class RollCameraEdit(val cameraId: Long)

@Serializable
private data class CameraEdit(val cameraId: Long)

@Serializable
private object FixedLensEdit

@Serializable
private data class LensEdit(val lensId: Long, val fixedLens: Boolean)

@Serializable
private data class FilterEdit(val filterId: Long)

@Serializable
private data class RollFilmStockEdit(val filmStockId: Long)

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