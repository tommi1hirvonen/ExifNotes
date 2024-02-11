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
import com.tommihirvonen.exifnotes.data.integrity.Datatype.*
import com.tommihirvonen.exifnotes.data.integrity.OnDelete.*

internal class ExifNotesIntegrityCheck(db: SQLiteDatabase) : IntegrityCheck(db) {
    init {
        table(TABLE_CAMERAS) {
            col(KEY_CAMERA_ID, INT).primaryKey().autoIncrement()
            col(KEY_CAMERA_MAKE, TEXT).notNull()
            col(KEY_CAMERA_MODEL, TEXT).notNull()
            col(KEY_CAMERA_MAX_SHUTTER, TEXT)
            col(KEY_CAMERA_MIN_SHUTTER, TEXT)
            col(KEY_CAMERA_SERIAL_NO, TEXT)
            col(KEY_CAMERA_SHUTTER_INCREMENTS, INT).notNull()
            col(KEY_CAMERA_EXPOSURE_COMP_INCREMENTS, INT).notNull()
            col(KEY_CAMERA_FORMAT, INT)
            col(KEY_LENS_ID, INT).foreignKey(TABLE_LENSES, SET_NULL)
        }
        table(TABLE_LENSES) {
            col(KEY_LENS_ID, INT).primaryKey().autoIncrement()
            col(KEY_LENS_MAKE, TEXT).notNull()
            col(KEY_LENS_MODEL, TEXT).notNull()
            col(KEY_LENS_MAX_APERTURE, TEXT)
            col(KEY_LENS_MIN_APERTURE, TEXT)
            col(KEY_LENS_MAX_FOCAL_LENGTH, INT)
            col(KEY_LENS_MIN_FOCAL_LENGTH, INT)
            col(KEY_LENS_SERIAL_NO, TEXT)
            col(KEY_LENS_APERTURE_INCREMENTS, INT).notNull()
            col(KEY_LENS_CUSTOM_APERTURE_VALUES, TEXT)
        }
        table(TABLE_FILTERS) {
            col(KEY_FILTER_ID, INT).primaryKey().autoIncrement()
            col(KEY_FILTER_MAKE, TEXT).notNull()
            col(KEY_FILTER_MODEL, TEXT).notNull()
        }
        table(TABLE_LINK_CAMERA_LENS) {
            col(KEY_CAMERA_ID, INT).notNull().primaryKey().foreignKey(TABLE_CAMERAS, CASCADE)
            col(KEY_LENS_ID, INT).notNull().primaryKey().foreignKey(TABLE_LENSES, CASCADE)
        }
        table(TABLE_LINK_LENS_FILTER) {
            col(KEY_LENS_ID, INT).notNull().primaryKey().foreignKey(TABLE_LENSES, CASCADE)
            col(KEY_FILTER_ID, INT).notNull().primaryKey().foreignKey(TABLE_FILTERS, CASCADE)
        }
        table(TABLE_LINK_FRAME_FILTER) {
            col(KEY_FRAME_ID, INT).notNull().primaryKey().foreignKey(TABLE_FRAMES, CASCADE)
            col(KEY_FILTER_ID, INT).notNull().primaryKey().foreignKey(TABLE_FILTERS, CASCADE)
        }
        table(TABLE_ROLLS) {
            col(KEY_ROLL_ID, INT).primaryKey().autoIncrement()
            col(KEY_ROLLNAME, TEXT).notNull()
            col(KEY_ROLL_DATE, TEXT).notNull()
            col(KEY_ROLL_NOTE, TEXT)
            col(KEY_CAMERA_ID, INT).foreignKey(TABLE_CAMERAS, SET_NULL)
            col(KEY_ROLL_ISO, INT)
            col(KEY_ROLL_PUSH, TEXT)
            col(KEY_ROLL_FORMAT, INT)
            col(KEY_ROLL_ARCHIVED, INT).notNull()
            col(KEY_FILM_STOCK_ID, INT).foreignKey(TABLE_FILM_STOCKS, SET_NULL)
            col(KEY_ROLL_UNLOADED, TEXT)
            col(KEY_ROLL_DEVELOPED, TEXT)
            col(KEY_ROLL_FAVORITE, INT).notNull()
        }
        table(TABLE_FRAMES) {
            col(KEY_FRAME_ID, INT).primaryKey().autoIncrement()
            col(KEY_ROLL_ID, INT).notNull().foreignKey(TABLE_ROLLS, CASCADE)
            col(KEY_COUNT, INT).notNull()
            col(KEY_DATE, TEXT).notNull()
            col(KEY_LENS_ID, INT).foreignKey(TABLE_LENSES, SET_NULL)
            col(KEY_SHUTTER, TEXT)
            col(KEY_APERTURE, TEXT)
            col(KEY_FRAME_NOTE, TEXT)
            col(KEY_LOCATION, TEXT)
            col(KEY_FOCAL_LENGTH, INT)
            col(KEY_EXPOSURE_COMP, TEXT)
            col(KEY_NO_OF_EXPOSURES, INT)
            col(KEY_FLASH_USED, INT)
            col(KEY_FLASH_POWER, TEXT)
            col(KEY_FLASH_COMP, TEXT)
            col(KEY_FRAME_SIZE, TEXT)
            col(KEY_METERING_MODE, INT)
            col(KEY_FORMATTED_ADDRESS, TEXT)
            col(KEY_PICTURE_FILENAME, TEXT)
            col(KEY_LIGHT_SOURCE, INT)
        }
        table(TABLE_FILM_STOCKS) {
            col(KEY_FILM_STOCK_ID, INT).primaryKey().autoIncrement()
            col(KEY_FILM_STOCK_NAME, TEXT).notNull()
            col(KEY_FILM_MANUFACTURER_NAME, TEXT).notNull()
            col(KEY_FILM_ISO, INT)
            col(KEY_FILM_TYPE, INT)
            col(KEY_FILM_PROCESS, INT)
            col(KEY_FILM_IS_PREADDED, INT)
        }
        table(TABLE_LABELS) {
            col(KEY_LABEL_ID, INT).notNull().primaryKey().autoIncrement()
            col(KEY_LABEL_NAME, TEXT).notNull()
        }
        table(TABLE_LINK_ROLL_LABEL) {
            col(KEY_ROLL_ID, INT).notNull().primaryKey().foreignKey(TABLE_ROLLS, CASCADE)
            col(KEY_LABEL_ID, INT).notNull().primaryKey().foreignKey(TABLE_LABELS, CASCADE)
        }
    }
}