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
import com.tommihirvonen.exifnotes.screens.frameedit.FrameEditScreen
import com.tommihirvonen.exifnotes.screens.frameedit.FrameViewModel
import com.tommihirvonen.exifnotes.screens.frames.FramesScreen
import com.tommihirvonen.exifnotes.screens.frames.FramesViewModel
import com.tommihirvonen.exifnotes.screens.framesmap.FramesMapScreen
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
import com.tommihirvonen.exifnotes.screens.location.LocationPickScreen
import com.tommihirvonen.exifnotes.screens.main.MainScreen
import com.tommihirvonen.exifnotes.screens.main.MainViewModel
import com.tommihirvonen.exifnotes.screens.rolledit.RollEditScreen
import com.tommihirvonen.exifnotes.screens.rolledit.RollViewModel
import com.tommihirvonen.exifnotes.screens.rollsmap.RollsMapScreen
import com.tommihirvonen.exifnotes.screens.rollsmap.RollsMapViewModel
import com.tommihirvonen.exifnotes.screens.settings.LicenseScreen
import com.tommihirvonen.exifnotes.screens.settings.SettingsScreen
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel
import com.tommihirvonen.exifnotes.screens.settings.ThirdPartyLicensesScreen
import com.tommihirvonen.exifnotes.theme.ExifNotesTheme
import com.tommihirvonen.exifnotes.theme.ThemeViewModel
import kotlinx.serialization.Serializable

@Composable
fun App(onFinish: () -> Unit) {
    val themeViewModel = hiltViewModel<ThemeViewModel>()
    ExifNotesTheme(themeViewModel) {
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
                    submitHandler = mainViewModel::submitRoll
                )
            }
            composable<Frames> { backStackEntry ->
                val frames = backStackEntry.toRoute<Frames>()
                FramesScreen(
                    rollId = frames.rollId,
                    mainViewModel = mainViewModel,
                    onEditRoll = {
                        navController.navigate(route = FramesRollEdit(rollId = frames.rollId))
                    },
                    onEditFrame = { frame, previousFrame, frameCount ->
                        val route = FrameEdit(
                            rollId = frames.rollId,
                            frameId = frame?.id ?: -1,
                            previousFrameId = previousFrame?.id ?: -1,
                            frameCount = frameCount
                        )
                        navController.navigate(route = route)
                    },
                    onNavigateToMap = { _ ->
                        navController.navigate(route = FramesMap)
                    },
                    onNavigateToLocationPick = {
                        navController.navigate(route = BatchLocationPick)
                    },
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            composable<FrameEdit> { backStackEntry ->
                val frameEdit = backStackEntry.toRoute<FrameEdit>()
                val framesEntry = remember(backStackEntry) { navController.getBackStackEntry<Frames>() }
                val framesViewModel = hiltViewModel<FramesViewModel>(framesEntry)
                FrameEditScreen(
                    rollId = frameEdit.rollId,
                    frameId = frameEdit.frameId,
                    previousFrameId = frameEdit.previousFrameId,
                    frameCount = frameEdit.frameCount,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToLocationPick = { navController.navigate(route = LocationPick) },
                    onNavigateToFilterEdit = {
                        navController.navigate(route = FrameFilterEdit(filterId = -1))
                    },
                    onNavigateToLensEdit = {
                        navController.navigate(route = FrameLensEdit(lensId = -1))
                    },
                    submitHandler = framesViewModel::submitFrame
                )
            }
            composable<LocationPick> { backStackEntry ->
                val frameEditEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry<FrameEdit>()
                    } catch (e: IllegalArgumentException) {
                        navController.getBackStackEntry<RollsMapFrameEdit>()
                    }
                }
                val frameViewModel = hiltViewModel<FrameViewModel>(frameEditEntry)
                LocationPickScreen(
                    frame = frameViewModel.frame.value,
                    onNavigateUp = { navController.navigateUp() },
                    onLocationConfirm = { latLng, address ->
                        frameViewModel.setLocation(latLng, address)
                        navController.navigateUp()
                    },
                    themeViewModel = themeViewModel
                )
            }
            composable<BatchLocationPick> { backStackEntry ->
                val framesEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Frames>()
                }
                val framesViewModel = hiltViewModel<FramesViewModel>(framesEntry)
                LocationPickScreen(
                    frame = null,
                    onNavigateUp = { navController.navigateUp() },
                    onLocationConfirm = { latLng, address ->
                        framesViewModel.selectedFrames.value.forEach { frame ->
                            framesViewModel.submitFrame(
                                frame.copy(
                                    location = latLng,
                                    formattedAddress = address
                                )
                            )
                        }
                        navController.navigateUp()
                    },
                    themeViewModel = themeViewModel
                ) }
            composable<FramesMap> { backStackEntry ->
                val framesEntry = remember(backStackEntry) { navController.getBackStackEntry<Frames>() }
                val framesViewModel = hiltViewModel<FramesViewModel>(framesEntry)
                FramesMapScreen(
                    themeViewModel = themeViewModel,
                    framesViewModel = framesViewModel,
                    onNavigateUp = { navController.navigateUp() },
                    onFrameEdit = { frame ->
                        val route = FrameEdit(
                            rollId = frame.rollId,
                            frameId = frame.id,
                            previousFrameId = -1,
                            frameCount = 0
                        )
                        navController.navigate(route = route)
                    }
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
                    submitHandler = { roll ->
                        mainViewModel.submitRoll(roll)
                        framesViewModel.setRoll(roll)
                    }
                )
            }
            composable<RollsMap> {
                RollsMapScreen(
                    onNavigateUp = { navController.navigateUp() },
                    onFrameEdit = { frame ->
                        navController.navigate(
                            route = RollsMapFrameEdit(rollId = frame.rollId, frameId = frame.id)
                        )
                    },
                    themeViewModel = themeViewModel,
                    mainViewModel = mainViewModel
                )
            }
            composable<RollsMapFrameEdit> { backStackEntry ->
                val frameEdit = backStackEntry.toRoute<RollsMapFrameEdit>()
                val rollsMapEntry = remember(backStackEntry) { navController.getBackStackEntry<RollsMap>() }
                val rollsMapViewModel = hiltViewModel<RollsMapViewModel>(rollsMapEntry)
                FrameEditScreen(
                    rollId = frameEdit.rollId,
                    frameId = frameEdit.frameId,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToLocationPick = { navController.navigate(route = LocationPick) },
                    onNavigateToFilterEdit = {
                        navController.navigate(route = FrameFilterEdit(filterId = -1))
                    },
                    onNavigateToLensEdit = {
                        navController.navigate(route = FrameLensEdit(lensId = -1))
                    },
                    submitHandler = rollsMapViewModel::submitFrame
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
                    submitHandler = gearViewModel::submitCamera
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
                    submitHandler = { c ->
                        rollViewModel.submitCamera(c)
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
                    submitHandler = gearViewModel::submitLens
                )
            }
            composable<FrameLensEdit> { backStackEntry ->
                val lensEdit = backStackEntry.toRoute<FrameLensEdit>()
                val frameEditEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry<FrameEdit>()
                    } catch (e: IllegalArgumentException) {
                        navController.getBackStackEntry<RollsMapFrameEdit>()
                    }
                }
                val frameViewModel = hiltViewModel<FrameViewModel>(frameEditEntry)
                InterchangeableLensEditScreen(
                    lensId = lensEdit.lensId,
                    onNavigateUp = { navController.navigateUp() },
                    submitHandler = frameViewModel::submitLens
                )
            }
            dialog<FilterEdit> { backStackEntry ->
                val gearEntry = remember(backStackEntry) { navController.getBackStackEntry<Gear>() }
                val gearViewModel = hiltViewModel<GearViewModel>(gearEntry)
                val filter = backStackEntry.toRoute<FilterEdit>()
                FilterEditScreen(
                    filterId = filter.filterId,
                    onDismiss = { navController.navigateUp() },
                    submitHandler = gearViewModel::submitFilter
                )
            }
            dialog<FrameFilterEdit> { backStackEntry ->
                val filter = backStackEntry.toRoute<FrameFilterEdit>()
                val frameEditEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry<FrameEdit>()
                    } catch (e: IllegalArgumentException) {
                        navController.getBackStackEntry<RollsMapFrameEdit>()
                    }
                }
                val frameViewModel = hiltViewModel<FrameViewModel>(frameEditEntry)
                FilterEditScreen(
                    filterId = filter.filterId,
                    onDismiss = { navController.navigateUp() },
                    submitHandler = frameViewModel::submitFilter
                )
            }
            dialog<FilmStockEdit> { backStackEntry ->
                val gearEntry = remember(backStackEntry) { navController.getBackStackEntry<Gear>() }
                val filmStocksViewModel = hiltViewModel<FilmStocksViewModel>(gearEntry)
                val filmStock = backStackEntry.toRoute<FilmStockEdit>()
                FilmStockEditScreen(
                    filmStockId = filmStock.filmStockId,
                    onNavigateUp = { navController.navigateUp() },
                    submitHandler = filmStocksViewModel::submitFilmStock
                )
            }
            dialog<RollFilmStockEdit> { backStackEntry ->
                val rollEditEntry = remember(backStackEntry) { navController.getBackStackEntry<RollEdit>() }
                val filmStock = backStackEntry.toRoute<RollFilmStockEdit>()
                val rollViewModel = hiltViewModel<RollViewModel>(rollEditEntry)
                FilmStockEditScreen(
                    filmStockId = filmStock.filmStockId,
                    onNavigateUp = { navController.navigateUp() },
                    submitHandler = { stock ->
                        rollViewModel.submitFilmStock(stock)
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
                    themeViewModel = themeViewModel,
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
private object BatchLocationPick

@Serializable
private data class FramesRollEdit(val rollId: Long)

@Serializable
private data class FrameEdit(
    val rollId: Long, val frameId: Long, val previousFrameId: Long, val frameCount: Int
)

@Serializable
private data class FrameLensEdit(val lensId: Long)

@Serializable
private data class FrameFilterEdit(val filterId: Long)

@Serializable
private object LocationPick

@Serializable
private object FramesMap

@Serializable
private object RollsMap

@Serializable
private data class RollsMapFrameEdit(val rollId: Long, val frameId: Long)

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