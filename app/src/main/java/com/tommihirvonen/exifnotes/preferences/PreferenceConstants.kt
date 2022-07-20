/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.preferences

/**
 * Class containing global constants used as keys and values when
 * reading or editing SharedPreferences.
 */
object PreferenceConstants {
    const val KEY_DARK_THEME = "DarkTheme"
    const val KEY_MAP_TYPE = "MAP_TYPE"
    const val KEY_UI_COLOR = "UIColor"
    const val KEY_GPS_UPDATE = "GPSUpdate"
    const val KEY_FRAME_SORT_ORDER = "FrameSortOrder"
    const val KEY_ROLL_SORT_ORDER = "RollSortOrder"
    const val KEY_VISIBLE_ROLLS = "VisibleRolls"
    const val KEY_FILES_TO_EXPORT = "FilesToExport"
    const val VALUE_BOTH = "BOTH"
    const val VALUE_CSV = "CSV"
    const val VALUE_EXIFTOOL = "EXIFTOOL"
    const val KEY_EXPORT_COMPLEMENTARY_PICTURES = "ExportComplementaryPictures"
    const val KEY_IMPORT_COMPLEMENTARY_PICTURES = "ImportComplementaryPictures"
    const val KEY_EXPORT_DATABASE = "ExportDatabase"
    const val KEY_IMPORT_DATABASE = "ImportDatabase"
}