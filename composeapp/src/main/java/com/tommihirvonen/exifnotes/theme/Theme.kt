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

package com.tommihirvonen.exifnotes.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7C5800),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA7),
    onPrimaryContainer = Color(0xFF271900),
    secondary = Color(0xFF6D5C3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DFBB),
    onSecondaryContainer = Color(0xFF251A04),
    tertiary = Color(0xFF4D6544),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFEBC1),
    onTertiaryContainer = Color(0xFF0B2007),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFEEE1CF),
    onSurfaceVariant = Color(0xFF4E4639),
    outline = Color(0xFF807667),
    inverseOnSurface = Color(0xFFF8EFE7),
    inverseSurface = Color(0xFF34302A),
    inversePrimary = Color(0xFFFFBB1E),
    surfaceTint = Color(0xFF7C5800)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFBB1E),
    onPrimary = Color(0xFF412D00),
    primaryContainer = Color(0xFF5E4200),
    onPrimaryContainer = Color(0xFFFFDEA7),
    secondary = Color(0xFFDAC3A0),
    onSecondary = Color(0xFF3C2E15),
    secondaryContainer = Color(0xFF54452A),
    onSecondaryContainer = Color(0xFFF7DFBB),
    tertiary = Color(0xFFB3CEA6),
    onTertiary = Color(0xFF203619),
    tertiaryContainer = Color(0xFF364D2E),
    onTertiaryContainer = Color(0xFFCFEBC1),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1E1B16),
    onBackground = Color(0xFFE9E1D9),
    surface = Color(0xFF1E1B16),
    onSurface = Color(0xFFE9E1D9),
    surfaceVariant = Color(0xFF4E4639),
    onSurfaceVariant = Color(0xFFD1C5B4),
    outline = Color(0xFF9A8F80),
    inverseOnSurface = Color(0xFF1E1B16),
    inverseSurface = Color(0xFFE9E1D9),
    inversePrimary = Color(0xFF7C5800),
    surfaceTint = Color(0xFFFFBB1E)
)

@Composable
fun ExifNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            }
            else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}