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

//Legacy table names for onUpgrade() statements.
//These table names were used in pre 19 versions of the database.
private const val LEGACY_TABLE_LINK_CAMERA_LENS = "mountables"
private const val LEGACY_TABLE_LINK_LENS_FILTER = "mountable_filters_lenses"
internal val ON_UPGRADE_CREATE_FILTER_TABLE = """
    |create table $TABLE_FILTERS(
    |   $KEY_FILTER_ID integer primary key autoincrement,
    |   $KEY_FILTER_MAKE text not null,
    |   $KEY_FILTER_MODEL text not null
    |);
    """.trimMargin()
internal val ON_UPGRADE_CREATE_LINK_LENS_FILTER_TABLE = """
    |create table $LEGACY_TABLE_LINK_LENS_FILTER(
    |   $KEY_LENS_ID integer not null,
    |   $KEY_FILTER_ID integer not null
    |);
    """.trimMargin()
internal const val ALTER_TABLE_FRAMES_1 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_FOCAL_LENGTH integer;"
internal const val ALTER_TABLE_FRAMES_2 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_EXPOSURE_COMP text;"
internal const val ALTER_TABLE_FRAMES_3 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_NO_OF_EXPOSURES integer;"
internal const val ALTER_TABLE_FRAMES_4 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_FLASH_USED integer;"
internal const val ALTER_TABLE_FRAMES_5 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_FLASH_POWER text;"
internal const val ALTER_TABLE_FRAMES_6 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_FLASH_COMP text;"
internal const val ALTER_TABLE_FRAMES_7 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_FRAME_SIZE text;"
internal const val ALTER_TABLE_FRAMES_8 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_FILTER_ID integer;"
internal const val ALTER_TABLE_FRAMES_9 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_METERING_MODE integer;"
internal const val ALTER_TABLE_FRAMES_10 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_FORMATTED_ADDRESS text;"
internal const val ALTER_TABLE_FRAMES_11 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_PICTURE_FILENAME text;"
internal const val ALTER_TABLE_FRAMES_12 =
    "ALTER TABLE $TABLE_FRAMES ADD COLUMN $KEY_LIGHT_SOURCE integer;"
internal const val ALTER_TABLE_LENSES_1 =
    "ALTER TABLE $TABLE_LENSES ADD COLUMN $KEY_LENS_MAX_APERTURE text;"
internal const val ALTER_TABLE_LENSES_2 =
    "ALTER TABLE $TABLE_LENSES ADD COLUMN $KEY_LENS_MIN_APERTURE text;"
internal const val ALTER_TABLE_LENSES_3 =
    "ALTER TABLE $TABLE_LENSES ADD COLUMN $KEY_LENS_MAX_FOCAL_LENGTH integer;"
internal const val ALTER_TABLE_LENSES_4 =
    "ALTER TABLE $TABLE_LENSES ADD COLUMN $KEY_LENS_MIN_FOCAL_LENGTH integer;"
internal const val ALTER_TABLE_LENSES_5 =
    "ALTER TABLE $TABLE_LENSES ADD COLUMN $KEY_LENS_SERIAL_NO text;"
internal const val ALTER_TABLE_LENSES_6 =
    "ALTER TABLE $TABLE_LENSES ADD COLUMN $KEY_LENS_APERTURE_INCREMENTS integer not null default 0;"
internal const val ALTER_TABLE_CAMERAS_1 =
    "ALTER TABLE $TABLE_CAMERAS ADD COLUMN $KEY_CAMERA_MAX_SHUTTER text;"
internal const val ALTER_TABLE_CAMERAS_2 =
    "ALTER TABLE $TABLE_CAMERAS ADD COLUMN $KEY_CAMERA_MIN_SHUTTER text;"
internal const val ALTER_TABLE_CAMERAS_3 =
    "ALTER TABLE $TABLE_CAMERAS ADD COLUMN $KEY_CAMERA_SERIAL_NO text;"
internal const val ALTER_TABLE_CAMERAS_4 =
    "ALTER TABLE $TABLE_CAMERAS ADD COLUMN $KEY_CAMERA_SHUTTER_INCREMENTS integer not null default 0;"
internal const val ALTER_TABLE_CAMERAS_5 =
    "ALTER TABLE $TABLE_CAMERAS ADD COLUMN $KEY_CAMERA_EXPOSURE_COMP_INCREMENTS integer not null default 0;"
internal const val ALTER_TABLE_CAMERAS_6 =
    "ALTER TABLE $TABLE_CAMERAS ADD COLUMN $KEY_LENS_ID integer references $TABLE_LENSES on delete set null;"
internal const val ALTER_TABLE_ROLLS_1 =
    "ALTER TABLE $TABLE_ROLLS ADD COLUMN $KEY_ROLL_ISO integer;"
internal const val ALTER_TABLE_ROLLS_2 =
    "ALTER TABLE $TABLE_ROLLS ADD COLUMN $KEY_ROLL_PUSH text;"
internal const val ALTER_TABLE_ROLLS_3 =
    "ALTER TABLE $TABLE_ROLLS ADD COLUMN $KEY_ROLL_FORMAT integer;"
internal const val ALTER_TABLE_ROLLS_4 =
    "ALTER TABLE $TABLE_ROLLS ADD COLUMN $KEY_ROLL_ARCHIVED integer not null default 0;"
internal const val ALTER_TABLE_ROLLS_5 =
    "ALTER TABLE $TABLE_ROLLS ADD COLUMN $KEY_FILM_STOCK_ID integer references $TABLE_FILM_STOCKS on delete set null;"
internal const val ALTER_TABLE_ROLLS_6 =
    "ALTER TABLE $TABLE_ROLLS ADD COLUMN $KEY_ROLL_UNLOADED text;"
internal const val ALTER_TABLE_ROLLS_7 =
    "ALTER TABLE $TABLE_ROLLS ADD COLUMN $KEY_ROLL_DEVELOPED text;"
internal const val REPLACE_QUOTE_CHARS =
    """UPDATE $TABLE_FRAMES SET $KEY_SHUTTER = REPLACE($KEY_SHUTTER, 'q', '"') WHERE $KEY_SHUTTER LIKE '%q';"""
internal const val ALTER_TABLE_CAMERAS_7 =
    "ALTER TABLE $TABLE_CAMERAS ADD COLUMN $KEY_CAMERA_FORMAT integer;"
internal const val ALTER_TABLE_LENSES_7 =
    "ALTER TABLE $TABLE_LENSES ADD COLUMN $KEY_LENS_CUSTOM_APERTURE_VALUES text;"

// (1) Rename the table, (2) create a new table with new structure,
// (3) insert data from the renamed table to the new table and (4) drop the renamed table.
internal const val ROLLS_TABLE_REVISION_1 = "alter table $TABLE_ROLLS rename to temp_rolls;"
internal val ROLLS_TABLE_REVISION_2 = """
    |create table $TABLE_ROLLS(
    |   $KEY_ROLL_ID integer primary key autoincrement,
    |   $KEY_ROLLNAME text not null,
    |   $KEY_ROLL_DATE text not null,
    |   $KEY_ROLL_NOTE text,
    |   $KEY_CAMERA_ID integer references $TABLE_CAMERAS on delete set null,
    |   $KEY_ROLL_ISO integer,
    |   $KEY_ROLL_PUSH text,
    |   $KEY_ROLL_FORMAT integer,
    |   $KEY_ROLL_ARCHIVED integer not null default 0
    |);
    """.trimMargin()
internal val ROLLS_TABLE_REVISION_3 = """
    |insert into $TABLE_ROLLS (
    |   $KEY_ROLL_ID,
    |   $KEY_ROLLNAME,
    |   $KEY_ROLL_DATE,
    |   $KEY_ROLL_NOTE,
    |   $KEY_CAMERA_ID,
    |   $KEY_ROLL_ISO,
    |   $KEY_ROLL_PUSH,
    |   $KEY_ROLL_FORMAT,
    |   $KEY_ROLL_ARCHIVED
    |)
    |select 
    |   $KEY_ROLL_ID,
    |   $KEY_ROLLNAME,
    |   $KEY_ROLL_DATE,
    |   $KEY_ROLL_NOTE,
    |   case when $KEY_CAMERA_ID not in (select $KEY_CAMERA_ID from $TABLE_CAMERAS) then null else $KEY_CAMERA_ID end,
    |   $KEY_ROLL_ISO,
    |   $KEY_ROLL_PUSH,
    |   $KEY_ROLL_FORMAT,
    |   $KEY_ROLL_ARCHIVED
    |from temp_rolls;
    """.trimMargin()
internal const val ROLLS_TABLE_REVISION_4 = "drop table temp_rolls;"

// (1) Rename the table, (2) create a new table with new structure,
// (3) insert data from the renamed table to the new table,
// (4) create a new link table between frames and filters,
// (5) insert data from the renamed table to the new link table and
// (6) drop the renamed table.
internal const val FRAMES_TABLE_REVISION_1 = "alter table $TABLE_FRAMES rename to temp_frames;"
internal val FRAMES_TABLE_REVISION_2 = """
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
    |   $KEY_PICTURE_FILENAME text
    |);
    """.trimMargin()
internal val FRAMES_TABLE_REVISION_3 = """
    |insert into $TABLE_FRAMES (
    |   $KEY_FRAME_ID,
    |   $KEY_ROLL_ID,
    |   $KEY_COUNT,
    |   $KEY_DATE,
    |   $KEY_LENS_ID,
    |   $KEY_SHUTTER,
    |   $KEY_APERTURE,
    |   $KEY_FRAME_NOTE,
    |   $KEY_LOCATION,
    |   $KEY_FOCAL_LENGTH,
    |   $KEY_EXPOSURE_COMP,
    |   $KEY_NO_OF_EXPOSURES,
    |   $KEY_FLASH_USED,
    |   $KEY_FLASH_POWER,
    |   $KEY_FLASH_COMP,
    |   $KEY_FRAME_SIZE,
    |   $KEY_METERING_MODE,
    |   $KEY_FORMATTED_ADDRESS,
    |   $KEY_PICTURE_FILENAME
    |)
    |select 
    |   $KEY_FRAME_ID,
    |   $KEY_ROLL_ID,
    |   $KEY_COUNT,
    |   $KEY_DATE,
    |   case when $KEY_LENS_ID not in (select $KEY_LENS_ID from $TABLE_LENSES) then null else $KEY_LENS_ID end,
    |   nullif($KEY_SHUTTER, '<empty>'),
    |   nullif($KEY_APERTURE, '<empty>'),
    |   $KEY_FRAME_NOTE,
    |   $KEY_LOCATION,
    |   $KEY_FOCAL_LENGTH,
    |   $KEY_EXPOSURE_COMP,
    |   $KEY_NO_OF_EXPOSURES,
    |   $KEY_FLASH_USED,
    |   $KEY_FLASH_POWER,
    |   $KEY_FLASH_COMP,
    |   $KEY_FRAME_SIZE,
    |   $KEY_METERING_MODE,
    |   $KEY_FORMATTED_ADDRESS,
    |   $KEY_PICTURE_FILENAME
    |from temp_frames 
    |where $KEY_ROLL_ID in (select $KEY_ROLL_ID from $TABLE_ROLLS);
    """.trimMargin()
internal val ON_UPGRADE_CREATE_LINK_FRAME_FILTER_TABLE = """
    |create table $TABLE_LINK_FRAME_FILTER(
    |   $KEY_FRAME_ID integer not null references $TABLE_FRAMES on delete cascade,
    |   $KEY_FILTER_ID integer not null references $TABLE_FILTERS on delete cascade,
    |   primary key($KEY_FRAME_ID, $KEY_FILTER_ID)
    |);
    """.trimMargin()
internal val FRAMES_TABLE_REVISION_4 = """
    |insert into $TABLE_LINK_FRAME_FILTER (
    |   $KEY_FRAME_ID, $KEY_FILTER_ID
    |)
    |select $KEY_FRAME_ID, $KEY_FILTER_ID 
    |from temp_frames 
    |where $KEY_FILTER_ID in (select $KEY_FILTER_ID from $TABLE_FILTERS)
    |   and $KEY_ROLL_ID in (select $KEY_ROLL_ID from $TABLE_ROLLS);
    """.trimMargin()
internal const val FRAMES_TABLE_REVISION_5 = "drop table temp_frames;"

// (1) Rename the table (in pre database 19 versions called "mountables"),
// (2) create a new table with new structure,
// (3) insert data from the renamed table to the new table and (4) drop the renamed table.
internal const val CAMERA_LENS_LINK_TABLE_REVISION_1 = "alter table $LEGACY_TABLE_LINK_CAMERA_LENS rename to temp_mountables;"
internal val CAMERA_LENS_LINK_TABLE_REVISION_2 = """
    |create table $TABLE_LINK_CAMERA_LENS(
    |   $KEY_CAMERA_ID integer not null references $TABLE_CAMERAS on delete cascade,
    |   $KEY_LENS_ID integer not null references $TABLE_LENSES on delete cascade,
    |   primary key($KEY_CAMERA_ID, $KEY_LENS_ID)
    |);
    """.trimMargin()
internal val CAMERA_LENS_LINK_TABLE_REVISION_3 = """
    |insert into $TABLE_LINK_CAMERA_LENS (
    |   $KEY_CAMERA_ID, $KEY_LENS_ID
    |)
    |select $KEY_CAMERA_ID, $KEY_LENS_ID 
    |from temp_mountables 
    |where $KEY_CAMERA_ID in (select $KEY_CAMERA_ID from $TABLE_CAMERAS) 
    |   and $KEY_LENS_ID in (select $KEY_LENS_ID from $TABLE_LENSES);
    """.trimMargin()
internal const val CAMERA_LENS_LINK_TABLE_REVISION_4 = "drop table temp_mountables;"

// (1) Rename the table (in pre database 19 versions called "mountable_filters_lenses"),
// (2) create a new table with new structure,
// (3) insert data from the renamed table to the new table and (4) drop the renamed table.
internal const val LENS_FILTER_LINK_TABLE_REVISION_1 =
    "alter table $LEGACY_TABLE_LINK_LENS_FILTER rename to temp_mountable_filters_lenses;"
internal val LENS_FILTER_LINK_TABLE_REVISION_2 = """
    |create table $TABLE_LINK_LENS_FILTER(
    |   $KEY_LENS_ID integer not null references $TABLE_LENSES on delete cascade,
    |   $KEY_FILTER_ID integer not null references $TABLE_FILTERS on delete cascade,
    |   primary key($KEY_LENS_ID, $KEY_FILTER_ID)
    |);
    """.trimMargin()
internal val LENS_FILTER_LINK_TABLE_REVISION_3 = """
    |insert into $TABLE_LINK_LENS_FILTER ($KEY_LENS_ID, $KEY_FILTER_ID)
    |select $KEY_LENS_ID, $KEY_FILTER_ID
    |from temp_mountable_filters_lenses 
    |where $KEY_LENS_ID in (
    |   select $KEY_LENS_ID 
    |   from $TABLE_LENSES
    |) and $KEY_FILTER_ID in (
    |   select $KEY_FILTER_ID
    |   from $TABLE_FILTERS
    |);
    """.trimMargin()
internal const val LENS_FILTER_LINK_TABLE_REVISION_4 = "drop table temp_mountable_filters_lenses;"

internal const val ALTER_TABLE_ROLLS_ADD_FAVORITE =
    "alter table $TABLE_ROLLS add column $KEY_ROLL_FAVORITE integer not null default 0;"