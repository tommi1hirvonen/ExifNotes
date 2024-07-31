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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.PlatformTextStyle

val Typography @Composable get() = Typography(
    bodyMedium = MaterialTheme.typography.bodyMedium.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    bodySmall = MaterialTheme.typography.bodySmall.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    bodyLarge = MaterialTheme.typography.bodyLarge.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
)