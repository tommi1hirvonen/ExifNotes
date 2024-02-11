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

package com.tommihirvonen.exifnotes.data.integrity

import android.database.sqlite.SQLiteDatabase
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.integrity.OnDelete.*

internal class ExifNotesIntegrityCheck(db: SQLiteDatabase) : IntegrityCheck(db) {
    init {
        table(TABLE_CAMERAS) {
            int(KEY_CAMERA_ID).primaryKey().autoIncrement()
            text(KEY_CAMERA_MAKE).notNull()
            text(KEY_CAMERA_MODEL).notNull()
            text(KEY_CAMERA_MAX_SHUTTER)
            text(KEY_CAMERA_MIN_SHUTTER)
            text(KEY_CAMERA_SERIAL_NO)
            int(KEY_CAMERA_SHUTTER_INCREMENTS).notNull()
            int(KEY_CAMERA_EXPOSURE_COMP_INCREMENTS).notNull()
            int(KEY_CAMERA_FORMAT)
            int(KEY_LENS_ID).foreignKey(TABLE_LENSES, SET_NULL)
        }
        table(TABLE_LENSES) {
            int(KEY_LENS_ID).primaryKey().autoIncrement()
            text(KEY_LENS_MAKE).notNull()
            text(KEY_LENS_MODEL).notNull()
            text(KEY_LENS_MAX_APERTURE)
            text(KEY_LENS_MIN_APERTURE)
            int(KEY_LENS_MAX_FOCAL_LENGTH)
            int(KEY_LENS_MIN_FOCAL_LENGTH)
            text(KEY_LENS_SERIAL_NO)
            int(KEY_LENS_APERTURE_INCREMENTS).notNull()
            text(KEY_LENS_CUSTOM_APERTURE_VALUES)
        }
        table(TABLE_FILTERS) {
            int(KEY_FILTER_ID).primaryKey().autoIncrement()
            text(KEY_FILTER_MAKE).notNull()
            text(KEY_FILTER_MODEL).notNull()
        }
        table(TABLE_LINK_CAMERA_LENS) {
            int(KEY_CAMERA_ID).notNull().primaryKey().foreignKey(TABLE_CAMERAS, CASCADE)
            int(KEY_LENS_ID).notNull().primaryKey().foreignKey(TABLE_LENSES, CASCADE)
        }
        table(TABLE_LINK_LENS_FILTER) {
            int(KEY_LENS_ID).notNull().primaryKey().foreignKey(TABLE_LENSES, CASCADE)
            int(KEY_FILTER_ID).notNull().primaryKey().foreignKey(TABLE_FILTERS, CASCADE)
        }
        table(TABLE_LINK_FRAME_FILTER) {
            int(KEY_FRAME_ID).notNull().primaryKey().foreignKey(TABLE_FRAMES, CASCADE)
            int(KEY_FILTER_ID).notNull().primaryKey().foreignKey(TABLE_FILTERS, CASCADE)
        }
        table(TABLE_ROLLS) {
            int(KEY_ROLL_ID).primaryKey().autoIncrement()
            text(KEY_ROLLNAME).notNull()
            text(KEY_ROLL_DATE).notNull()
            text(KEY_ROLL_NOTE)
            int(KEY_CAMERA_ID).foreignKey(TABLE_CAMERAS, SET_NULL)
            int(KEY_ROLL_ISO)
            text(KEY_ROLL_PUSH)
            int(KEY_ROLL_FORMAT)
            int(KEY_ROLL_ARCHIVED).notNull()
            int(KEY_FILM_STOCK_ID).foreignKey(TABLE_FILM_STOCKS, SET_NULL)
            text(KEY_ROLL_UNLOADED)
            text(KEY_ROLL_DEVELOPED)
        }
        table(TABLE_FRAMES) {
            int(KEY_FRAME_ID).primaryKey().autoIncrement()
            int(KEY_ROLL_ID).notNull().foreignKey(TABLE_ROLLS, CASCADE)
            int(KEY_COUNT).notNull()
            text(KEY_DATE).notNull()
            int(KEY_LENS_ID).foreignKey(TABLE_LENSES, SET_NULL)
            text(KEY_SHUTTER)
            text(KEY_APERTURE)
            text(KEY_FRAME_NOTE)
            text(KEY_LOCATION)
            int(KEY_FOCAL_LENGTH)
            text(KEY_EXPOSURE_COMP)
            int(KEY_NO_OF_EXPOSURES)
            int(KEY_FLASH_USED)
            text(KEY_FLASH_POWER)
            text(KEY_FLASH_COMP)
            text(KEY_FRAME_SIZE)
            int(KEY_METERING_MODE)
            text(KEY_FORMATTED_ADDRESS)
            text(KEY_PICTURE_FILENAME)
            int(KEY_LIGHT_SOURCE)
        }
        table(TABLE_FILM_STOCKS) {
            int(KEY_FILM_STOCK_ID).primaryKey().autoIncrement()
            text(KEY_FILM_STOCK_NAME).notNull()
            text(KEY_FILM_MANUFACTURER_NAME).notNull()
            int(KEY_FILM_ISO)
            int(KEY_FILM_TYPE)
            int(KEY_FILM_PROCESS)
            int(KEY_FILM_IS_PREADDED)
        }
    }
}