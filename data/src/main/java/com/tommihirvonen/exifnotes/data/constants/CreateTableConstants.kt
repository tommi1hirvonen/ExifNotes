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

package com.tommihirvonen.exifnotes.data.constants

internal val CREATE_FILM_STOCKS_TABLE = """
    |create table $TABLE_FILM_STOCKS(
    |   $KEY_FILM_STOCK_ID integer primary key autoincrement,
    |   $KEY_FILM_MANUFACTURER_NAME text not null,
    |   $KEY_FILM_STOCK_NAME text not null,
    |   $KEY_FILM_ISO integer,
    |   $KEY_FILM_TYPE integer,
    |   $KEY_FILM_PROCESS integer,
    |   $KEY_FILM_IS_PREADDED integer
    |);
    """.trimMargin()

internal val CREATE_LENS_TABLE = """
    |create table $TABLE_LENSES(
    |   $KEY_LENS_ID integer primary key autoincrement,
    |   $KEY_LENS_MAKE text not null,
    |   $KEY_LENS_MODEL text not null,
    |   $KEY_LENS_MAX_APERTURE text,
    |   $KEY_LENS_MIN_APERTURE text,
    |   $KEY_LENS_MAX_FOCAL_LENGTH integer,
    |   $KEY_LENS_MIN_FOCAL_LENGTH integer,
    |   $KEY_LENS_SERIAL_NO text,
    |   $KEY_LENS_APERTURE_INCREMENTS integer not null,
    |   $KEY_LENS_CUSTOM_APERTURE_VALUES text
    |);
    """.trimMargin()

internal val CREATE_CAMERA_TABLE = """
    |create table $TABLE_CAMERAS(
    |   $KEY_CAMERA_ID integer primary key autoincrement,
    |   $KEY_CAMERA_MAKE text not null,
    |   $KEY_CAMERA_MODEL text not null,
    |   $KEY_CAMERA_MAX_SHUTTER text,
    |   $KEY_CAMERA_MIN_SHUTTER text,
    |   $KEY_CAMERA_SERIAL_NO text,
    |   $KEY_CAMERA_SHUTTER_INCREMENTS integer not null,
    |   $KEY_CAMERA_EXPOSURE_COMP_INCREMENTS integer not null default 0,
    |   $KEY_LENS_ID integer references $TABLE_LENSES on delete set null,
    |   $KEY_CAMERA_FORMAT integer
    |);
    """.trimMargin()

internal val CREATE_FILTER_TABLE = """
    |create table $TABLE_FILTERS(
    |   $KEY_FILTER_ID integer primary key autoincrement,
    |   $KEY_FILTER_MAKE text not null,
    |   $KEY_FILTER_MODEL text not null
    |);
    """.trimMargin()

internal val CREATE_ROLL_TABLE = """
    |create table $TABLE_ROLLS(
    |   $KEY_ROLL_ID integer primary key autoincrement,
    |   $KEY_ROLLNAME text not null,
    |   $KEY_ROLL_DATE text not null,
    |   $KEY_ROLL_NOTE text,
    |   $KEY_CAMERA_ID integer references $TABLE_CAMERAS on delete set null,
    |   $KEY_ROLL_ISO integer,
    |   $KEY_ROLL_PUSH text,
    |   $KEY_ROLL_FORMAT integer,
    |   $KEY_ROLL_ARCHIVED integer not null default 0,
    |   $KEY_FILM_STOCK_ID integer references $TABLE_FILM_STOCKS on delete set null,
    |   $KEY_ROLL_UNLOADED text,
    |   $KEY_ROLL_DEVELOPED text,
    |   $KEY_ROLL_FAVORITE integer not null default 0
    |);
    """.trimMargin()

internal val CREATE_FRAME_TABLE = """
    |create table $TABLE_FRAMES(
    |   $KEY_FRAME_ID integer primary key autoincrement,
    |   $KEY_ROLL_ID integer not null references $TABLE_ROLLS on delete cascade,
    |   $KEY_COUNT integer not null,
    |   $KEY_DATE text not null,
    |   $KEY_LENS_ID integer references $TABLE_LENSES on delete set null,
    |   $KEY_SHUTTER text,
    |   $KEY_APERTURE text,
    |   $KEY_FRAME_NOTE text,
    |   $KEY_LOCATION text,
    |   $KEY_FOCAL_LENGTH integer,
    |   $KEY_EXPOSURE_COMP text,
    |   $KEY_NO_OF_EXPOSURES integer,
    |   $KEY_FLASH_USED integer,
    |   $KEY_FLASH_POWER text,
    |   $KEY_FLASH_COMP text,
    |   $KEY_FRAME_SIZE text,
    |   $KEY_METERING_MODE integer,
    |   $KEY_FORMATTED_ADDRESS text,
    |   $KEY_PICTURE_FILENAME text,
    |   $KEY_LIGHT_SOURCE integer
    |);
    """.trimMargin()

internal val CREATE_LINK_CAMERA_LENS_TABLE = """
    |create table $TABLE_LINK_CAMERA_LENS(
    |   $KEY_CAMERA_ID integer not null references $TABLE_CAMERAS on delete cascade,
    |   $KEY_LENS_ID integer not null references $TABLE_LENSES on delete cascade,
    |   primary key($KEY_CAMERA_ID, $KEY_LENS_ID)
    |);
    """.trimMargin()

internal val CREATE_LINK_LENS_FILTER_TABLE = """
    |create table $TABLE_LINK_LENS_FILTER(
    |   $KEY_LENS_ID integer not null references $TABLE_LENSES on delete cascade,
    |   $KEY_FILTER_ID integer not null references $TABLE_FILTERS on delete cascade,
    |   primary key($KEY_LENS_ID, $KEY_FILTER_ID)
    |);
    """.trimMargin()

internal val CREATE_LINK_FRAME_FILTER_TABLE = """
    |create table $TABLE_LINK_FRAME_FILTER(
    |   $KEY_FRAME_ID integer not null references $TABLE_FRAMES on delete cascade,
    |   $KEY_FILTER_ID integer not null references $TABLE_FILTERS on delete cascade,
    |   primary key($KEY_FRAME_ID, $KEY_FILTER_ID)
    |);
    """.trimMargin()

internal val CREATE_LABEL_TABLE = """
    |create table $TABLE_LABELS (
    |    $KEY_LABEL_ID integer not null primary key autoincrement,
    |    $KEY_LABEL_NAME text not null
    |);
    """.trimMargin()

internal val CREATE_ROLL_LABEL_LINK_TABLE = """
    |create table $TABLE_LINK_ROLL_LABEL (
    |    $KEY_ROLL_ID integer not null references $TABLE_ROLLS on delete cascade,
    |    $KEY_LABEL_ID integer not null references $TABLE_LABELS on delete cascade,
    |    primary key ($KEY_ROLL_ID, $KEY_LABEL_ID)
    |);
    """.trimMargin()