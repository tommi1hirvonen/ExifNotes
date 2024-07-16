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
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tommihirvonen.exifnotes.theme.ExifNotesTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExifNotesTheme {
                val navController = rememberNavController()
                val rollsModel = hiltViewModel<RollsViewModel>()
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
                            onNavigateToLabels = { navController.navigate(route = Labels) }
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
                    dialog<LabelEdit> { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(Labels)
                        }
                        val parentViewModel = hiltViewModel<LabelsViewModel>(parentEntry)
                        val labelEdit = backStackEntry.toRoute<LabelEdit>()
                        LabelForm(
                            labelId = labelEdit.labelId,
                            onDismiss = { navController.navigateUp() },
                            rollsModel = rollsModel,
                            labelsModel = parentViewModel
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