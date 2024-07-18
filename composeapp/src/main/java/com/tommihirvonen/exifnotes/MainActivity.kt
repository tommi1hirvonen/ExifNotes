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
import com.tommihirvonen.exifnotes.di.pictures.ComplementaryPicturesManager
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
import com.tommihirvonen.exifnotes.util.purgeDirectory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var complementaryPicturesManager: ComplementaryPicturesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Delete all complementary pictures, which are not linked to any frame.
        // Do this each time the app is launched to keep the storage consumption to a minimum.
        // If savedInstanceState is not null, then the activity is being recreated. In this case,
        // don't delete pictures.
        if (savedInstanceState == null) {
            complementaryPicturesManager.deleteUnusedPictures()
        }

        enableEdgeToEdge()
        setContent {
            val themeModel = hiltViewModel<ThemeViewModel>()
            ExifNotesTheme(themeModel) {
                val navController = rememberNavController()
                val rollsModel = hiltViewModel<MainViewModel>()
                val settingsModel = hiltViewModel<SettingsViewModel>()
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
                            mainViewModel = rollsModel,
                            onNavigateToMap = { /*TODO*/ },
                            onNavigateToGear = { /*TODO*/ },
                            onNavigateToLabels = { navController.navigate(route = Labels) },
                            onNavigateToSettings = { navController.navigate(route = Settings) }
                        )
                    }
                    composable<Labels> {
                        LabelsScreen(
                            mainViewModel = rollsModel,
                            onNavigateUp = { navController.navigateUp() },
                            onEditLabel = { label ->
                                navController.navigate(route = LabelEdit(label?.id ?: -1))
                            }
                        )
                    }
                    composable<Settings> {
                        SettingsScreen(
                            themeViewModel = themeModel,
                            settingsViewModel = settingsModel,
                            mainViewModel = rollsModel,
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
                            mainViewModel = rollsModel
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        // This method is called when MainActivity is started, in other words when the
        // application is started. All the files created in FramesFragment.setShareIntentExportRoll
        // in the application's external storage directory are deleted. This way we can keep
        // the number of files stored to a minimum.

        // Delete all temporary files created when exporting rolls.
        val externalStorageDir = getExternalFilesDir(null)
        externalStorageDir?.purgeDirectory()
        externalCacheDir?.purgeDirectory()
        super.onStart()
    }
}

@Serializable
object Main

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