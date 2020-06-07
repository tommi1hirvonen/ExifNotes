package com.tommihirvonen.exifnotes.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.DateTime;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.RollFilterMode;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.datastructures.Location;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FilmDbHelper is the SQL database class that holds all the information
 * the user stores in the app. This class provides all necessary CRUD operations as well as
 * export and import functionality.
 */
public class FilmDbHelper extends SQLiteOpenHelper {

    //=============================================================================================
    //Table names
    private static final String TABLE_FRAMES = "frames";
    private static final String TABLE_LENSES = "lenses";
    private static final String TABLE_ROLLS = "rolls";
    private static final String TABLE_CAMERAS = "cameras";
    private static final String TABLE_LINK_CAMERA_LENS = "link_camera_lens";
    //Added in database version 14
    private static final String TABLE_FILTERS = "filters";
    private static final String TABLE_LINK_LENS_FILTER = "link_lens_filter";
    //Added in database version 19
    private static final String TABLE_LINK_FRAME_FILTER = "link_frame_filter";
    //Added in database version 20
    private static final String TABLE_FILM_STOCKS = "film_stocks";

    //=============================================================================================
    //Column names

    //Frame
    private static final String KEY_FRAME_ID = "frame_id";
    private static final String KEY_COUNT = "count";
    private static final String KEY_DATE = "date";
    private static final String KEY_SHUTTER = "shutter";
    private static final String KEY_APERTURE = "aperture";
    private static final String KEY_FRAME_NOTE = "frame_note";
    private static final String KEY_LOCATION = "location";
    //Added in database version 14
    private static final String KEY_FOCAL_LENGTH = "focal_length";
    private static final String KEY_EXPOSURE_COMP = "exposure_comp";
    private static final String KEY_NO_OF_EXPOSURES = "no_of_exposures";
    private static final String KEY_FLASH_USED = "flash_used";
    private static final String KEY_FLASH_POWER = "flash_power";
    private static final String KEY_FLASH_COMP = "flash_comp";
    private static final String KEY_FRAME_SIZE = "frame_size";
    private static final String KEY_METERING_MODE = "metering_mode";
    //Added in database version 15
    private static final String KEY_FORMATTED_ADDRESS = "formatted_address";
    //Added in database version 17
    private static final String KEY_PICTURE_FILENAME = "picture_filename";
    // Added in database version 20
    private static final String KEY_LIGHT_SOURCE = "light_source";

    //Lens
    private static final String KEY_LENS_ID = "lens_id";
    private static final String KEY_LENS_MAKE = "lens_make";
    private static final String KEY_LENS_MODEL = "lens_model";
    //Added in database version 14
    private static final String KEY_LENS_MAX_APERTURE = "lens_max_aperture";
    private static final String KEY_LENS_MIN_APERTURE = "lens_min_aperture";
    private static final String KEY_LENS_MAX_FOCAL_LENGTH = "lens_max_focal_length";
    private static final String KEY_LENS_MIN_FOCAL_LENGTH = "lens_min_focal_length";
    private static final String KEY_LENS_SERIAL_NO = "lens_serial_no";
    private static final String KEY_LENS_APERTURE_INCREMENTS = "aperture_increments";

    //Camera
    private static final String KEY_CAMERA_ID = "camera_id";
    private static final String KEY_CAMERA_MAKE = "camera_make";
    private static final String KEY_CAMERA_MODEL = "camera_model";
    //Added in database version 14
    private static final String KEY_CAMERA_MAX_SHUTTER = "camera_max_shutter";
    private static final String KEY_CAMERA_MIN_SHUTTER = "camera_min_shutter";
    private static final String KEY_CAMERA_SERIAL_NO = "camera_serial_no";
    private static final String KEY_CAMERA_SHUTTER_INCREMENTS = "shutter_increments";
    //Added in database version 18
    private static final String KEY_CAMERA_EXPOSURE_COMP_INCREMENTS = "exposure_comp_increments";

    //Roll
    private static final String KEY_ROLL_ID = "roll_id";
    private static final String KEY_ROLLNAME = "rollname";
    private static final String KEY_ROLL_DATE = "roll_date";
    private static final String KEY_ROLL_NOTE = "roll_note";
    //Added in database version 14
    private static final String KEY_ROLL_ISO = "roll_iso";
    private static final String KEY_ROLL_PUSH = "roll_push";
    private static final String KEY_ROLL_FORMAT = "roll_format";
    //Added in database version 16
    private static final String KEY_ROLL_ARCHIVED = "roll_archived";
    //Added in database version 21
    private static final String KEY_ROLL_UNLOADED = "roll_unloaded";
    private static final String KEY_ROLL_DEVELOPED = "roll_developed";

    //Filter
    //Added in database version 14
    private static final String KEY_FILTER_ID = "filter_id";
    private static final String KEY_FILTER_MAKE = "filter_make";
    private static final String KEY_FILTER_MODEL = "filter_model";

    //Film stocks
    //Added in database version 20
    private static final String KEY_FILM_STOCK_ID = "film_stock_id";
    private static final String KEY_FILM_MANUFACTURER_NAME = "film_manufacturer_name";
    private static final String KEY_FILM_STOCK_NAME = "film_stock_name";
    private static final String KEY_FILM_ISO = "film_iso";
    private static final String KEY_FILM_TYPE = "film_type";
    private static final String KEY_FILM_PROCESS = "film_process";
    private static final String KEY_FILM_IS_PREADDED = "film_is_preadded";

    //=============================================================================================
    //Database information
    private static final String DATABASE_NAME = "filmnotes.db";

    //Updated version from 13 to 14 - 2016-12-03 - v1.7.0
    //Updated version from 14 to 15 - 2017-04-29 - v1.9.0
    //Updated version from 15 to 16 - 2018-02-17 - v1.9.5
    //Updated version from 16 to 17 - 2018-03-26 - v1.11.0
    //Updated version from 17 to 18 - 2018-07-08 - v1.12.0
    //Updated version from 18 to 19 - 2018-07-17 - awaiting
    private static final int DATABASE_VERSION = 21;

    //=============================================================================================
    //onCreate strings

    private static final String CREATE_FILM_STOCKS_TABLE = "create table " + TABLE_FILM_STOCKS
            + "(" + KEY_FILM_STOCK_ID + " integer primary key autoincrement, "
            + KEY_FILM_MANUFACTURER_NAME + " text not null, "
            + KEY_FILM_STOCK_NAME + " text not null, "
            + KEY_FILM_ISO + " integer,"
            + KEY_FILM_TYPE + " integer,"
            + KEY_FILM_PROCESS + " integer,"
            + KEY_FILM_IS_PREADDED + " integer"
            + ");";

    private static final String CREATE_LENS_TABLE = "create table " + TABLE_LENSES
            + "(" + KEY_LENS_ID + " integer primary key autoincrement, "
            + KEY_LENS_MAKE + " text not null, "
            + KEY_LENS_MODEL + " text not null, "
            + KEY_LENS_MAX_APERTURE + " text, "
            + KEY_LENS_MIN_APERTURE + " text, "
            + KEY_LENS_MAX_FOCAL_LENGTH + " integer, "
            + KEY_LENS_MIN_FOCAL_LENGTH + " integer, "
            + KEY_LENS_SERIAL_NO + " text, "
            + KEY_LENS_APERTURE_INCREMENTS + " integer not null"
            + ");";

    private static final String CREATE_CAMERA_TABLE = "create table " + TABLE_CAMERAS
            + "(" + KEY_CAMERA_ID + " integer primary key autoincrement, "
            + KEY_CAMERA_MAKE + " text not null, "
            + KEY_CAMERA_MODEL + " text not null, "
            + KEY_CAMERA_MAX_SHUTTER + " text, "
            + KEY_CAMERA_MIN_SHUTTER + " text, "
            + KEY_CAMERA_SERIAL_NO + " text, "
            + KEY_CAMERA_SHUTTER_INCREMENTS + " integer not null, "
            + KEY_CAMERA_EXPOSURE_COMP_INCREMENTS + " integer not null default 0"
            + ");";

    private static final String CREATE_FILTER_TABLE = "create table " + TABLE_FILTERS
            + "(" + KEY_FILTER_ID + " integer primary key autoincrement, "
            + KEY_FILTER_MAKE + " text not null, "
            + KEY_FILTER_MODEL + " text not null"
            + ");";

    private static final String CREATE_ROLL_TABLE = "create table " + TABLE_ROLLS
            + "(" + KEY_ROLL_ID + " integer primary key autoincrement, "
            + KEY_ROLLNAME + " text not null, "
            + KEY_ROLL_DATE + " text not null, "
            + KEY_ROLL_NOTE + " text, "
            + KEY_CAMERA_ID + " integer references " + TABLE_CAMERAS + " on delete set null, "
            + KEY_ROLL_ISO + " integer, "
            + KEY_ROLL_PUSH + " text, "
            + KEY_ROLL_FORMAT + " integer, "
            + KEY_ROLL_ARCHIVED + " integer not null default 0,"
            + KEY_FILM_STOCK_ID + " integer references " + TABLE_FILM_STOCKS + " on delete set null, "
            + KEY_ROLL_UNLOADED + " text, "
            + KEY_ROLL_DEVELOPED + " text"
            + ");";

    private static final String CREATE_FRAME_TABLE = "create table " + TABLE_FRAMES
            + "(" + KEY_FRAME_ID + " integer primary key autoincrement, "
            + KEY_ROLL_ID + " integer not null references " + TABLE_ROLLS + " on delete cascade, "
            + KEY_COUNT + " integer not null, "
            + KEY_DATE + " text not null, "
            + KEY_LENS_ID + " integer references " + TABLE_LENSES + " on delete set null, "
            + KEY_SHUTTER + " text, "
            + KEY_APERTURE + " text, "
            + KEY_FRAME_NOTE + " text, "
            + KEY_LOCATION + " text, "
            + KEY_FOCAL_LENGTH + " integer, "
            + KEY_EXPOSURE_COMP + " text, "
            + KEY_NO_OF_EXPOSURES + " integer, "
            + KEY_FLASH_USED + " integer, "
            + KEY_FLASH_POWER + " text, "
            + KEY_FLASH_COMP + " text, "
            + KEY_FRAME_SIZE + " text, "
            + KEY_METERING_MODE + " integer, "
            + KEY_FORMATTED_ADDRESS + " text, "
            + KEY_PICTURE_FILENAME + " text, "
            + KEY_LIGHT_SOURCE + " integer"
            + ");";

    private static final String CREATE_LINK_CAMERA_LENS_TABLE = "create table " + TABLE_LINK_CAMERA_LENS
            + "(" + KEY_CAMERA_ID + " integer not null references " + TABLE_CAMERAS + " on delete cascade, "
            + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
            + "primary key(" + KEY_CAMERA_ID + ", " + KEY_LENS_ID + ")"
            + ");";

    private static final String CREATE_LINK_LENS_FILTER_TABLE = "create table " + TABLE_LINK_LENS_FILTER
            + "(" + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
            + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
            + "primary key(" + KEY_LENS_ID + ", " + KEY_FILTER_ID + ")"
            + ");";

    private static final String CREATE_LINK_FRAME_FILTER_TABLE = "create table " + TABLE_LINK_FRAME_FILTER
            + "(" + KEY_FRAME_ID + " integer not null references " + TABLE_FRAMES + " on delete cascade, "
            + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
            + "primary key(" + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ")"
            + ");";


    //=============================================================================================
    //onUpgrade strings

    //Legacy table names for onUpgrade() statements.
    //These table names were used in pre 19 versions of the database.
    private static final String LEGACY_TABLE_LINK_CAMERA_LENS = "mountables";
    private static final String LEGACY_TABLE_LINK_LENS_FILTER = "mountable_filters_lenses";

    private static final String ON_UPGRADE_CREATE_FILTER_TABLE = "create table " + TABLE_FILTERS
            + "(" + KEY_FILTER_ID + " integer primary key autoincrement, "
            + KEY_FILTER_MAKE + " text not null, "
            + KEY_FILTER_MODEL + " text not null"
            + ");";
    private static final String ON_UPGRADE_CREATE_LINK_LENS_FILTER_TABLE = "create table " + LEGACY_TABLE_LINK_LENS_FILTER
            + "(" + KEY_LENS_ID + " integer not null, "
            + KEY_FILTER_ID + " integer not null"
            + ");";

    private static final String ALTER_TABLE_FRAMES_1 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_FOCAL_LENGTH + " integer;";
    private static final String ALTER_TABLE_FRAMES_2 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_EXPOSURE_COMP + " text;";
    private static final String ALTER_TABLE_FRAMES_3 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_NO_OF_EXPOSURES + " integer;";
    private static final String ALTER_TABLE_FRAMES_4 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_FLASH_USED + " integer;";
    private static final String ALTER_TABLE_FRAMES_5 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_FLASH_POWER + " text;";
    private static final String ALTER_TABLE_FRAMES_6 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_FLASH_COMP + " text;";
    private static final String ALTER_TABLE_FRAMES_7 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_FRAME_SIZE + " text;";
    private static final String ALTER_TABLE_FRAMES_8 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_FILTER_ID + " integer;";
    private static final String ALTER_TABLE_FRAMES_9 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_METERING_MODE + " integer;";
    private static final String ALTER_TABLE_FRAMES_10 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_FORMATTED_ADDRESS + " text;";
    private static final String ALTER_TABLE_FRAMES_11 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_PICTURE_FILENAME + " text;";
    private static final String ALTER_TABLE_FRAMES_12 = "ALTER TABLE " + TABLE_FRAMES
            + " ADD COLUMN " + KEY_LIGHT_SOURCE + " integer;";

    private static final String ALTER_TABLE_LENSES_1 = "ALTER TABLE " + TABLE_LENSES
            + " ADD COLUMN " + KEY_LENS_MAX_APERTURE + " text;";
    private static final String ALTER_TABLE_LENSES_2 = "ALTER TABLE " + TABLE_LENSES
            + " ADD COLUMN " + KEY_LENS_MIN_APERTURE + " text;";
    private static final String ALTER_TABLE_LENSES_3 = "ALTER TABLE " + TABLE_LENSES
            + " ADD COLUMN " + KEY_LENS_MAX_FOCAL_LENGTH + " integer;";
    private static final String ALTER_TABLE_LENSES_4 = "ALTER TABLE " + TABLE_LENSES
            + " ADD COLUMN " + KEY_LENS_MIN_FOCAL_LENGTH + " integer;";
    private static final String ALTER_TABLE_LENSES_5 = "ALTER TABLE " + TABLE_LENSES
            + " ADD COLUMN " + KEY_LENS_SERIAL_NO + " text;";
    private static final String ALTER_TABLE_LENSES_6 = "ALTER TABLE " + TABLE_LENSES
            + " ADD COLUMN " + KEY_LENS_APERTURE_INCREMENTS + " integer not null default 0;";

    private static final String ALTER_TABLE_CAMERAS_1 = "ALTER TABLE " + TABLE_CAMERAS
            + " ADD COLUMN " + KEY_CAMERA_MAX_SHUTTER + " text;";
    private static final String ALTER_TABLE_CAMERAS_2 = "ALTER TABLE " + TABLE_CAMERAS
            + " ADD COLUMN " + KEY_CAMERA_MIN_SHUTTER + " text;";
    private static final String ALTER_TABLE_CAMERAS_3 = "ALTER TABLE " + TABLE_CAMERAS
            + " ADD COLUMN " + KEY_CAMERA_SERIAL_NO + " text;";
    private static final String ALTER_TABLE_CAMERAS_4 = "ALTER TABLE " + TABLE_CAMERAS
            + " ADD COLUMN " + KEY_CAMERA_SHUTTER_INCREMENTS + " integer not null default 0;";
    private static final String ALTER_TABLE_CAMERAS_5 = "ALTER TABLE " + TABLE_CAMERAS
            + " ADD COLUMN " + KEY_CAMERA_EXPOSURE_COMP_INCREMENTS + " integer not null default 0;";

    private static final String ALTER_TABLE_ROLLS_1 = "ALTER TABLE " + TABLE_ROLLS
            + " ADD COLUMN " + KEY_ROLL_ISO + " integer;";
    private static final String ALTER_TABLE_ROLLS_2 = "ALTER TABLE " + TABLE_ROLLS
            + " ADD COLUMN " + KEY_ROLL_PUSH + " text;";
    private static final String ALTER_TABLE_ROLLS_3 = "ALTER TABLE " + TABLE_ROLLS
            + " ADD COLUMN " + KEY_ROLL_FORMAT + " integer;";
    private static final String ALTER_TABLE_ROLLS_4 = "ALTER TABLE " + TABLE_ROLLS
            + " ADD COLUMN " + KEY_ROLL_ARCHIVED + " integer not null default 0;";
    private static final String ALTER_TABLE_ROLLS_5 = "ALTER TABLE " + TABLE_ROLLS
            + " ADD COLUMN " + KEY_FILM_STOCK_ID + " integer references " + TABLE_FILM_STOCKS + " on delete set null;";
    private static final String ALTER_TABLE_ROLLS_6 = "ALTER TABLE " + TABLE_ROLLS
            + " ADD COLUMN " + KEY_ROLL_UNLOADED + " text;";
    private static final String ALTER_TABLE_ROLLS_7 = "ALTER TABLE " + TABLE_ROLLS
            + " ADD COLUMN " + KEY_ROLL_DEVELOPED + " text;";


    @SuppressWarnings("SyntaxError")
    private static final String REPLACE_QUOTE_CHARS = "UPDATE " + TABLE_FRAMES
            + " SET " + KEY_SHUTTER + " = REPLACE(" + KEY_SHUTTER + ", \'q\', \'\"\')"
            + " WHERE " + KEY_SHUTTER + " LIKE \'%q\';";

    // (1) Rename the table, (2) create a new table with new structure,
    // (3) insert data from the renamed table to the new table and (4) drop the renamed table.
    private static final String ROLLS_TABLE_REVISION_1 = "alter table " + TABLE_ROLLS + " rename to temp_rolls;";
    private static final String ROLLS_TABLE_REVISION_2 = "create table " + TABLE_ROLLS
            + "(" + KEY_ROLL_ID + " integer primary key autoincrement, "
            + KEY_ROLLNAME + " text not null, "
            + KEY_ROLL_DATE + " text not null, "
            + KEY_ROLL_NOTE + " text, "
            + KEY_CAMERA_ID + " integer references " + TABLE_CAMERAS + " on delete set null, "
            + KEY_ROLL_ISO + " integer, "
            + KEY_ROLL_PUSH + " text, "
            + KEY_ROLL_FORMAT + " integer, "
            + KEY_ROLL_ARCHIVED + " integer not null default 0"
            + ");";
    private static final String ROLLS_TABLE_REVISION_3 = "insert into " + TABLE_ROLLS + " ("
            + KEY_ROLL_ID + ", " + KEY_ROLLNAME + ", " + KEY_ROLL_DATE + ", " + KEY_ROLL_NOTE + ", "
            + KEY_CAMERA_ID + ", " + KEY_ROLL_ISO + ", " + KEY_ROLL_PUSH + ", "
            + KEY_ROLL_FORMAT + ", " + KEY_ROLL_ARCHIVED + ") "
            + "select "
            + KEY_ROLL_ID + ", " + KEY_ROLLNAME + ", " + KEY_ROLL_DATE + ", " + KEY_ROLL_NOTE + ", "
            + "case when " + KEY_CAMERA_ID + " not in (select " + KEY_CAMERA_ID + " from " + TABLE_CAMERAS + ") then null else " + KEY_CAMERA_ID + " end" + ", "
            + KEY_ROLL_ISO + ", " + KEY_ROLL_PUSH + ", "
            + KEY_ROLL_FORMAT + ", " + KEY_ROLL_ARCHIVED + " "
            + "from temp_rolls;";
    private static final String ROLLS_TABLE_REVISION_4 = "drop table temp_rolls;";

    // (1) Rename the table, (2) create a new table with new structure,
    // (3) insert data from the renamed table to the new table,
    // (4) create a new link table between frames and filters,
    // (5) insert data from the renamed table to the new link table and
    // (6) drop the renamed table.
    private static final String FRAMES_TABLE_REVISION_1 = "alter table " + TABLE_FRAMES + " rename to temp_frames;";
    private static final String FRAMES_TABLE_REVISION_2 = "create table " + TABLE_FRAMES
            + "(" + KEY_FRAME_ID + " integer primary key autoincrement, "
            + KEY_ROLL_ID + " integer not null references " + TABLE_ROLLS + " on delete cascade, "
            + KEY_COUNT + " integer not null, "
            + KEY_DATE + " text not null, "
            + KEY_LENS_ID + " integer references " + TABLE_LENSES + " on delete set null, "
            + KEY_SHUTTER + " text, "
            + KEY_APERTURE + " text, "
            + KEY_FRAME_NOTE + " text, "
            + KEY_LOCATION + " text, "
            + KEY_FOCAL_LENGTH + " integer, "
            + KEY_EXPOSURE_COMP + " text, "
            + KEY_NO_OF_EXPOSURES + " integer, "
            + KEY_FLASH_USED + " integer, "
            + KEY_FLASH_POWER + " text, "
            + KEY_FLASH_COMP + " text, "
            + KEY_FRAME_SIZE + " text, "
            + KEY_METERING_MODE + " integer, "
            + KEY_FORMATTED_ADDRESS + " text, "
            + KEY_PICTURE_FILENAME + " text"
            + ");";
    private static final String FRAMES_TABLE_REVISION_3 = "insert into " + TABLE_FRAMES + " ("
            + KEY_FRAME_ID + ", " + KEY_ROLL_ID + ", " + KEY_COUNT + ", " + KEY_DATE + ", "
            + KEY_LENS_ID + ", " + KEY_SHUTTER + ", " + KEY_APERTURE + ", " + KEY_FRAME_NOTE + ", "
            + KEY_LOCATION + ", " + KEY_FOCAL_LENGTH + ", " + KEY_EXPOSURE_COMP + ", "
            + KEY_NO_OF_EXPOSURES + ", " + KEY_FLASH_USED + ", " + KEY_FLASH_POWER + ", "
            + KEY_FLASH_COMP + ", " + KEY_FRAME_SIZE + ", " + KEY_METERING_MODE + ", "
            + KEY_FORMATTED_ADDRESS + ", " + KEY_PICTURE_FILENAME + ") "
            + "select "
            + KEY_FRAME_ID + ", " + KEY_ROLL_ID + ", " + KEY_COUNT + ", " + KEY_DATE + ", "
            + "case when " + KEY_LENS_ID + " not in (select " + KEY_LENS_ID + " from " + TABLE_LENSES + ") then null else " + KEY_LENS_ID + " end" + ", "
            + "nullif(" + KEY_SHUTTER + ", '<empty>')" + ", "
            + "nullif(" + KEY_APERTURE + ", '<empty>')" + ", "
            + KEY_FRAME_NOTE + ", " + KEY_LOCATION + ", " + KEY_FOCAL_LENGTH + ", "
            + KEY_EXPOSURE_COMP + ", " + KEY_NO_OF_EXPOSURES + ", " + KEY_FLASH_USED + ", "
            + KEY_FLASH_POWER + ", " + KEY_FLASH_COMP + ", " + KEY_FRAME_SIZE + ", "
            + KEY_METERING_MODE + ", " + KEY_FORMATTED_ADDRESS + ", " + KEY_PICTURE_FILENAME + " "
            + "from temp_frames "
            + "where " + KEY_ROLL_ID + " in (select " + KEY_ROLL_ID + " from " + TABLE_ROLLS + ")" + ";";
    private static final String ON_UPGRADE_CREATE_LINK_FRAME_FILTER_TABLE = "create table " + TABLE_LINK_FRAME_FILTER
            + "(" + KEY_FRAME_ID + " integer not null references " + TABLE_FRAMES + " on delete cascade, "
            + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
            + "primary key(" + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ")"
            + ");";
    private static final String FRAMES_TABLE_REVISION_4 = "insert into " + TABLE_LINK_FRAME_FILTER + " ("
            + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ") "
            + "select " + KEY_FRAME_ID + ", " + KEY_FILTER_ID + " "
            + "from temp_frames "
            + "where " + KEY_FILTER_ID + " in (select " + KEY_FILTER_ID + " from " + TABLE_FILTERS + ")"
            + "and " + KEY_ROLL_ID + " in (select " + KEY_ROLL_ID + " from " + TABLE_ROLLS + ")" + ";";
    private static final String FRAMES_TABLE_REVISION_5 = "drop table temp_frames;";

    // (1) Rename the table (in pre database 19 versions called "mountables"),
    // (2) create a new table with new structure,
    // (3) insert data from the renamed table to the new table and (4) drop the renamed table.
    private static final String CAMERA_LENS_LINK_TABLE_REVISION_1 = "alter table " + LEGACY_TABLE_LINK_CAMERA_LENS + " rename to temp_mountables;";
    private static final String CAMERA_LENS_LINK_TABLE_REVISION_2 = "create table " + TABLE_LINK_CAMERA_LENS
            + "(" + KEY_CAMERA_ID + " integer not null references " + TABLE_CAMERAS + " on delete cascade, "
            + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
            + "primary key(" + KEY_CAMERA_ID + ", " + KEY_LENS_ID + ")"
            + ");";
    private static final String CAMERA_LENS_LINK_TABLE_REVISION_3 = "insert into " + TABLE_LINK_CAMERA_LENS + " ("
            + KEY_CAMERA_ID + ", " + KEY_LENS_ID + ") "
            + "select " + KEY_CAMERA_ID + ", " + KEY_LENS_ID + " "
            + "from temp_mountables "
            + "where " + KEY_CAMERA_ID + " in (select " + KEY_CAMERA_ID + " from " + TABLE_CAMERAS + ") "
            + "and " + KEY_LENS_ID + " in (select " + KEY_LENS_ID + " from " + TABLE_LENSES + ")" + ";";
    private static final String CAMERA_LENS_LINK_TABLE_REVISION_4 = "drop table temp_mountables;";

    // (1) Rename the table (in pre database 19 versions called "mountable_filters_lenses"),
    // (2) create a new table with new structure,
    // (3) insert data from the renamed table to the new table and (4) drop the renamed table.
    private static final String LENS_FILTER_LINK_TABLE_REVISION_1 = "alter table " + LEGACY_TABLE_LINK_LENS_FILTER + " rename to temp_mountable_filters_lenses;";
    private static final String LENS_FILTER_LINK_TABLE_REVISION_2 = "create table " + TABLE_LINK_LENS_FILTER
            + "(" + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
            + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
            + "primary key(" + KEY_LENS_ID + ", " + KEY_FILTER_ID + ")"
            + ");";
    private static final String LENS_FILTER_LINK_TABLE_REVISION_3 = "insert into " + TABLE_LINK_LENS_FILTER + " ("
            + KEY_LENS_ID + ", " + KEY_FILTER_ID + ") "
            + "select " + KEY_LENS_ID + ", " + KEY_FILTER_ID + " "
            + "from temp_mountable_filters_lenses "
            + "where " + KEY_LENS_ID + " in (select " + KEY_LENS_ID + " from " + TABLE_LENSES + ") "
            + "and " + KEY_FILTER_ID + " in (select " + KEY_FILTER_ID + " from " + TABLE_FILTERS + ")" + ";";
    private static final String LENS_FILTER_LINK_TABLE_REVISION_4 = "drop table temp_mountable_filters_lenses;";

    /**
     * Store reference to the singleton instance.
     */
    private static FilmDbHelper instance;

    private final Context context;

    /**
     * Singleton method to get reference to the database instance
     *
     * @param context current context
     * @return reference to the database singleton instance
     */
    public static synchronized FilmDbHelper getInstance(final Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (instance == null) {
            instance = new FilmDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Private constructor only to be called from getInstance()
     *
     * @param context the current context
     */
    private FilmDbHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }


    @Override
    public void onOpen(final SQLiteDatabase db) {
        super.onOpen(db);
        // The only time foreign key constraints are enforced, is when something is written
        // to the database. Only enable foreign keys, if the database may be written to.
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase database) {
        // Enable foreign key support, since we aren't overriding onConfigure() (added in API 16).
        database.execSQL("PRAGMA foreign_keys=ON;");
        database.execSQL(CREATE_FILM_STOCKS_TABLE);
        database.execSQL(CREATE_LENS_TABLE);
        database.execSQL(CREATE_CAMERA_TABLE);
        database.execSQL(CREATE_FILTER_TABLE);
        database.execSQL(CREATE_ROLL_TABLE);
        database.execSQL(CREATE_FRAME_TABLE);
        database.execSQL(CREATE_LINK_CAMERA_LENS_TABLE);
        database.execSQL(CREATE_LINK_LENS_FILTER_TABLE);
        database.execSQL(CREATE_LINK_FRAME_FILTER_TABLE);
        populateFilmStocks(database);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // Run all the required upgrade scripts consecutively.
        // New if blocks should be added whenever the database version is raised and new
        // columns and/or tables are added.

        if (oldVersion < 14) {
            //TABLE_FRAMES
            db.execSQL(ALTER_TABLE_FRAMES_1);
            db.execSQL(ALTER_TABLE_FRAMES_2);
            db.execSQL(ALTER_TABLE_FRAMES_3);
            db.execSQL(ALTER_TABLE_FRAMES_4);
            db.execSQL(ALTER_TABLE_FRAMES_5);
            db.execSQL(ALTER_TABLE_FRAMES_6);
            db.execSQL(ALTER_TABLE_FRAMES_7);
            db.execSQL(ALTER_TABLE_FRAMES_8);
            db.execSQL(ALTER_TABLE_FRAMES_9);
            //TABLE_LENSES
            db.execSQL(ALTER_TABLE_LENSES_1);
            db.execSQL(ALTER_TABLE_LENSES_2);
            db.execSQL(ALTER_TABLE_LENSES_3);
            db.execSQL(ALTER_TABLE_LENSES_4);
            db.execSQL(ALTER_TABLE_LENSES_5);
            db.execSQL(ALTER_TABLE_LENSES_6);
            //TABLE_CAMERAS
            db.execSQL(ALTER_TABLE_CAMERAS_1);
            db.execSQL(ALTER_TABLE_CAMERAS_2);
            db.execSQL(ALTER_TABLE_CAMERAS_3);
            db.execSQL(ALTER_TABLE_CAMERAS_4);
            //TABLE_ROLLS
            db.execSQL(ALTER_TABLE_ROLLS_1);
            db.execSQL(ALTER_TABLE_ROLLS_2);
            db.execSQL(ALTER_TABLE_ROLLS_3);
            //In an earlier version special chars were not allowed.
            //Instead quote marks were changed to 'q' when stored in the SQLite database.
            db.execSQL(REPLACE_QUOTE_CHARS);
            //TABLE_FILTERS
            db.execSQL(ON_UPGRADE_CREATE_FILTER_TABLE);
            //TABLE MOUNTABLES
            db.execSQL(ON_UPGRADE_CREATE_LINK_LENS_FILTER_TABLE);
        }
        if (oldVersion < 15) {
            db.execSQL(ALTER_TABLE_FRAMES_10);
        }
        if (oldVersion < 16) {
            db.execSQL(ALTER_TABLE_ROLLS_4);
        }
        if (oldVersion < 17) {
            db.execSQL(ALTER_TABLE_FRAMES_11);
        }
        if (oldVersion < 18) {
            db.execSQL(ALTER_TABLE_CAMERAS_5);
        }
        if (oldVersion < 19) {
            // Enable foreign key support, since we aren't overriding onConfigure() (added in API 16).
            db.execSQL("PRAGMA foreign_keys=ON;");
            // Alter statements
            db.beginTransaction();
            try {
                // execSQL() does not support multiple SQL commands separated with a semi-colon.
                // Separate the upgrade commands into single SQL commands.
                db.execSQL(ROLLS_TABLE_REVISION_1);
                db.execSQL(ROLLS_TABLE_REVISION_2);
                db.execSQL(ROLLS_TABLE_REVISION_3);
                db.execSQL(ROLLS_TABLE_REVISION_4);
                db.execSQL(FRAMES_TABLE_REVISION_1);
                db.execSQL(FRAMES_TABLE_REVISION_2);
                db.execSQL(FRAMES_TABLE_REVISION_3);
                db.execSQL(ON_UPGRADE_CREATE_LINK_FRAME_FILTER_TABLE);
                db.execSQL(FRAMES_TABLE_REVISION_4);
                db.execSQL(FRAMES_TABLE_REVISION_5);
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_1);
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_2);
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_3);
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_4);
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_1);
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_2);
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_3);
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_4);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 20) {
            db.execSQL(CREATE_FILM_STOCKS_TABLE);
            db.execSQL(ALTER_TABLE_FRAMES_12);
            db.execSQL(ALTER_TABLE_ROLLS_5);
            populateFilmStocks(db);
        }
        if (oldVersion < 21) {
            db.execSQL(ALTER_TABLE_ROLLS_6);
            db.execSQL(ALTER_TABLE_ROLLS_7);
        }
    }

    // ******************** CRUD operations for the frames table ********************

    /**
     * Adds a new frame to the database.
     * @param frame the new frame to be added to the database
     */
    public boolean addFrame(@NonNull final Frame frame) {
        // Get reference to writable database
        final SQLiteDatabase db = this.getWritableDatabase();
        // Create ContentValues to add key "column"/value
        final ContentValues values = buildFrameContentValues(frame);
        // Insert
        final long rowId = db.insert(TABLE_FRAMES, // table
                null, // nullColumnHack
                values);
        // Update the frame's id with the insert statement's return value
        frame.setId(rowId);
        // Add the filter links, if the frame was inserted successfully.
        if (rowId != -1) {
            for (final Filter filter : frame.getFilters()) {
                addFrameFilterLink(frame, filter);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets all the frames from a specified roll.
     * @param roll Roll object whose frames should be fetched
     * @return an array of Frames
     */
    public List<Frame> getAllFramesFromRoll(@NonNull final Roll roll){
        final List<Frame> frames = new ArrayList<>();
        // Get reference to readable database
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(
                TABLE_FRAMES, null, KEY_ROLL_ID + "=?",
                new String[]{Long.toString(roll.getId())}, null, null, KEY_COUNT);
        Frame frame;
        // Go over each row, build list
        while (cursor.moveToNext()) {
            frame = getFrameFromCursor(cursor);
            frames.add(frame);
        }
        cursor.close();
        return frames;
    }

    /**
     * Updates the information of a frame.
     * @param frame the frame to be updated.
     */
    public void updateFrame(@NonNull final Frame frame) {
        // Get reference to writable database
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues contentValues = buildFrameContentValues(frame);
        db.update(TABLE_FRAMES, contentValues, KEY_FRAME_ID + "=?",
                new String[]{Long.toString(frame.getId())});
        deleteAllFrameFilterLinks(frame);
        for (final Filter filter : frame.getFilters()) addFrameFilterLink(frame, filter);
    }

    /**
     * Deletes a frame from the database.
     * @param frame the frame to be deleted
     */
    public void deleteFrame(@NonNull final Frame frame) {
        // Get reference to writable database
        final SQLiteDatabase db = this.getWritableDatabase();
        // Delete
        db.delete(TABLE_FRAMES,
                KEY_FRAME_ID + " = ?",
                new String[]{Long.toString(frame.getId())});
    }

    /**
     * Gets all complementary picture filenames from the frames table.
     * @return List of all complementary picture filenames
     */
    public List<String> getAllComplementaryPictureFilenames() {
        final List<String> filenames = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_FRAMES, new String[]{KEY_PICTURE_FILENAME},
                KEY_PICTURE_FILENAME + " IS NOT NULL", null, null, null, null);
        while (cursor.moveToNext()) {
            filenames.add(cursor.getString(cursor.getColumnIndex(KEY_PICTURE_FILENAME)));
        }
        cursor.close();
        return filenames;
    }

    // ******************** CRUD operations for the lenses table ********************

    /**
     * Adds a new lens to the database.
     * @param lens the lens to be added to the database
     */
    public long addLens(@NonNull final Lens lens){
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = buildLensContentValues(lens);
        return db.insert(TABLE_LENSES, null, values);
    }

    /**
     * Gets a lens corresponding to the id.
     * @param lens_id the id of the lens
     * @return a Lens corresponding to the id
     */
    @Nullable
    public Lens getLens(final long lens_id){
        final SQLiteDatabase db = this.getReadableDatabase();
        Lens lens = null;
        final Cursor cursor = db.query(TABLE_LENSES, null, KEY_LENS_ID + "=?",
                new String[]{Long.toString(lens_id)}, null, null, null);
        if (cursor.moveToFirst()) {
            lens = getLensFromCursor(cursor);
            cursor.close();
        }
        return lens;
    }

    /**
     * Gets all the lenses from the database.
     * @return a List of all the lenses in the database.
     */
    public List<Lens> getAllLenses(){
        final List<Lens> lenses = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_LENSES, null, null, null,
                null, null, KEY_LENS_MAKE + " collate nocase," + KEY_LENS_MODEL + " collate nocase");
        Lens lens;
        while (cursor.moveToNext()) {
            lens = getLensFromCursor(cursor);
            lenses.add(lens);
        }
        cursor.close();
        return lenses;
    }

    /**
     * Deletes Lens from the database.
     * @param lens the Lens to be deleted
     */
    public void deleteLens(@NonNull final Lens lens){
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LENSES, KEY_LENS_ID + " = ?", new String[]{Long.toString(lens.getId())});
    }

    /**
     * Checks if the lens is being used in some frame.
     * @param lens Lens to be checked
     * @return true if the lens is in use, false if not
     */
    public boolean isLensInUse(@NonNull final Lens lens){
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_FRAMES, new String[]{KEY_LENS_ID}, KEY_LENS_ID + "=?",
                new String[]{Long.toString(lens.getId())}, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        }
        else {
            cursor.close();
            return false;
        }
    }

    /**
     * Updates the information of a lens
     * @param lens the Lens to be updated
     */
    public void updateLens(@NonNull final Lens lens) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues contentValues = buildLensContentValues(lens);
        db.update(TABLE_LENSES, contentValues, KEY_LENS_ID + "=?",
                new String[]{Long.toString(lens.getId())});
    }

    // ******************** CRUD operations for the cameras table ********************

    /**
     * Adds a new camera to the database.
     * @param camera the camera to be added to the database
     */
    public long addCamera(@NonNull final Camera camera){
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = buildCameraContentValues(camera);
        return db.insert(TABLE_CAMERAS, null, values);
    }

    /**
     * Gets the Camera corresponding to the camera id
     * @param camera_id the id of the Camera
     * @return the Camera corresponding to the given id
     */
    @Nullable
    public Camera getCamera(final long camera_id){
        final SQLiteDatabase db = this.getReadableDatabase();
        Camera camera = null;
        final Cursor cursor = db.query(TABLE_CAMERAS, null, KEY_CAMERA_ID + "=?",
                new String[]{Long.toString(camera_id)}, null, null, null);
        if (cursor.moveToFirst()) {
            camera = getCameraFromCursor(cursor);
            cursor.close();
        }
        return camera;
    }

    /**
     * Gets all the cameras from the database
     * @return a List of all the cameras in the database
     */
    public List<Camera> getAllCameras(){
        final List<Camera> cameras = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_CAMERAS, null, null, null,
                null, null, KEY_CAMERA_MAKE + " collate nocase," + KEY_CAMERA_MODEL + " collate nocase");
        Camera camera;
        while (cursor.moveToNext()) {
            camera = getCameraFromCursor(cursor);
            cameras.add(camera);
        }
        cursor.close();
        return cameras;
    }

    /**
     * Deletes the specified camera from the database
     * @param camera the camera to be deleted
     */
    public void deleteCamera(@NonNull final Camera camera){
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CAMERAS, KEY_CAMERA_ID + " = ?",
                new String[]{Long.toString(camera.getId())});
    }

    /**
     * Checks if a camera is being used in some roll.
     * @param camera the camera to be checked
     * @return true if the camera is in use, false if not
     */
    public boolean isCameraBeingUsed(@NonNull final Camera camera) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_ROLLS, new String[]{KEY_CAMERA_ID}, KEY_CAMERA_ID + "=?",
                new String[]{Long.toString(camera.getId())}, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        }
        else {
            cursor.close();
            return false;
        }
    }

    /**
     * Updates the information of the specified camera.
     * @param camera the camera to be updated
     */
    public void updateCamera(@NonNull final Camera camera) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues contentValues = buildCameraContentValues(camera);
        db.update(TABLE_CAMERAS, contentValues, KEY_CAMERA_ID + "=?",
                new String[]{Long.toString(camera.getId())});
    }

    // ******************** CRUD operations for the camera-lens link table ********************

    /**
     * Adds a mountable combination of camera and lens to the database.
     * @param camera the camera that can be mounted with the lens
     * @param lens the lens that can be mounted with the camera
     */
    public void addCameraLensLink(@NonNull final Camera camera, @NonNull final Lens lens){
        final SQLiteDatabase db = this.getWritableDatabase();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        final String query = "INSERT INTO " + TABLE_LINK_CAMERA_LENS + "(" + KEY_CAMERA_ID + "," + KEY_LENS_ID
                + ") SELECT " + camera.getId() + ", " + lens.getId()
                + " WHERE NOT EXISTS(SELECT 1 FROM " + TABLE_LINK_CAMERA_LENS + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.getId() + " AND " + KEY_LENS_ID + "=" + lens.getId() + ");";
        db.execSQL(query);
    }

    /**
     * Deletes a mountable combination from the database
     * @param camera the camera that can be mounted with the lens
     * @param lens the lens that can be mounted with the camera
     */
    public void deleteCameraLensLink(@NonNull final Camera camera, @NonNull final Lens lens){
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LINK_CAMERA_LENS, KEY_CAMERA_ID + " = ? AND " + KEY_LENS_ID + " = ?",
                    new String[]{Long.toString(camera.getId()), Long.toString(lens.getId())});
    }

    /**
     * Gets all the lenses that can be mounted to the specified camera
     * @param camera the camera whose lenses we want to get
     * @return a List of all linked lenses
     */
    public List<Lens> getLinkedLenses(@NonNull final Camera camera){
        final List<Lens> lenses = new ArrayList<>();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        final String query = "SELECT * FROM " + TABLE_LENSES + " WHERE " + KEY_LENS_ID + " IN "
                + "(" + "SELECT " + KEY_LENS_ID + " FROM " + TABLE_LINK_CAMERA_LENS + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.getId() + ") ORDER BY " + KEY_LENS_MAKE;
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.rawQuery(query, null);
        Lens lens;
        while (cursor.moveToNext()) {
            lens = getLensFromCursor(cursor);
            lenses.add(lens);
        }
        cursor.close();
        return lenses;
    }

    /**
     * Gets all the cameras that can be mounted to the specified lens
     * @param lens the lens whose cameras we want to get
     * @return a List of all linked cameras
     */
    public List<Camera> getLinkedCameras(@NonNull final Lens lens){
        final List<Camera> cameras = new ArrayList<>();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        final String query = "SELECT * FROM " + TABLE_CAMERAS + " WHERE " + KEY_CAMERA_ID + " IN "
                + "(" + "SELECT " + KEY_CAMERA_ID + " FROM " + TABLE_LINK_CAMERA_LENS + " WHERE "
                + KEY_LENS_ID + "=" + lens.getId() + ") ORDER BY " + KEY_CAMERA_MAKE;
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.rawQuery(query, null);
        Camera camera;
        while (cursor.moveToNext()) {
            camera = getCameraFromCursor(cursor);
            cameras.add(camera);
        }
        cursor.close();
        return cameras;
    }

    // ******************** CRUD operations for the rolls table ********************

    /**
     * Adds a new roll to the database.
     * @param roll the roll to be added
     */
    public long addRoll(@NonNull final Roll roll){
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = buildRollContentValues(roll);
        return db.insert(TABLE_ROLLS, null, values);
    }

    /**
     * Gets all the rolls in the database
     * @return a List of all the rolls in the database
     */
    public List<Roll> getRolls(final RollFilterMode filterMode){
        final String selectionArg;
        switch (filterMode) {
            case ACTIVE: default:
                selectionArg = KEY_ROLL_ARCHIVED + "=0";
                break;
            case ARCHIVED:
                selectionArg = KEY_ROLL_ARCHIVED + ">0";
                break;
            case ALL:
                selectionArg = null;
                break;
        }
        final List<Roll> rolls = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_ROLLS, null, selectionArg, null,
                null, null, KEY_ROLL_DATE + " DESC");
        Roll roll;
        while (cursor.moveToNext()) {
            roll = getRollFromCursor(cursor);
            rolls.add(roll);
        }
        cursor.close();
        return rolls;
    }

    /**
     * Gets the roll corresponding to the given id.
     * @param id the id of the roll
     * @return the roll corresponding to the given id
     */
    @Nullable
    public Roll getRoll(final long id){
        final SQLiteDatabase db = this.getReadableDatabase();
        Roll roll = null;
        final Cursor cursor = db.query(TABLE_ROLLS, null, KEY_ROLL_ID + "=?",
                new String[]{Long.toString(id)}, null, null, null);
        if (cursor.moveToFirst()) {
            roll = getRollFromCursor(cursor);
            cursor.close();
        }
        return roll;
    }

    /**
     * Deletes a roll from the database.
     * @param roll the roll to be deleted from the database
     */
    public void deleteRoll(@NonNull final Roll roll){
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ROLLS, KEY_ROLL_ID + " = ?", new String[]{Long.toString(roll.getId())});
    }

    /**
     * Updates the specified roll's information
     * @param roll the roll to be updated
     */
    public void updateRoll(@NonNull final Roll roll){
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues contentValues = buildRollContentValues(roll);
        db.update(TABLE_ROLLS, contentValues, KEY_ROLL_ID + "=?",
                new String[]{Long.toString(roll.getId())});
    }

    /**
     * Gets the number of frames on a specified roll.
     * @param roll the roll whose frame count we want
     * @return an integer of the the number of frames on that roll
     */
    public int getNumberOfFrames(@NonNull final Roll roll){
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_FRAMES, new String[]{"COUNT(" + KEY_FRAME_ID + ")"}, KEY_ROLL_ID + "=?",
                new String[]{Long.toString(roll.getId())}, null, null, null);
        int returnValue = 0;
        if (cursor.moveToFirst()) {
            returnValue = cursor.getInt(0);
            cursor.close();
        }
        return returnValue;
    }


    // ******************** CRUD operations for the filters table ********************

    /**
     * Adds a new filter to the database.
     * @param filter the filter to be added to the database
     */
    public long addFilter(@NonNull final Filter filter){
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = buildFilterContentValues(filter);
        return db.insert(TABLE_FILTERS, null, values);
    }

    /**
     * Gets the Filter corresponding to the filter id
     * @param filter_id the id of the Filter
     * @return the Filter corresponding to the given id
     */
    public Filter getFilter(final long filter_id){
        final SQLiteDatabase db = this.getReadableDatabase();
        final Filter filter = new Filter();
        final Cursor cursor = db.query(TABLE_FILTERS, null, KEY_FILTER_ID + "=?",
                new String[]{Long.toString(filter_id)}, null, null, null);
        if (cursor.moveToFirst()) {
            getFilterFromCursor(cursor, filter);
            cursor.close();
        }
        return filter;
    }

    /**
     * Gets all the filters from the database
     * @return a List of all the filters in the database
     */
    public List<Filter> getAllFilters(){
        final List<Filter> filters = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_FILTERS, null, null, null,
                null, null, KEY_FILTER_MAKE + " collate nocase," + KEY_FILTER_MODEL + " collate nocase");
        Filter filter;
        while (cursor.moveToNext()) {
            filter = getFilterFromCursor(cursor);
            filters.add(filter);
        }
        cursor.close();
        return filters;
    }

    /**
     * Deletes the specified filter from the database
     * @param filter the filter to be deleted
     */
    public void deleteFilter(@NonNull final Filter filter){
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FILTERS, KEY_FILTER_ID + " = ?", new String[]{Long.toString(filter.getId())});
    }

    /**
     * Checks if a filter is being used in some roll.
     * @param filter the filter to be checked
     * @return true if the filter is in use, false if not
     */
    public boolean isFilterBeingUsed(@NonNull final Filter filter) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_LINK_FRAME_FILTER, new String[]{KEY_FILTER_ID}, KEY_FILTER_ID + "=?",
                new String[]{Long.toString(filter.getId())}, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        }
        else {
            cursor.close();
            return false;
        }
    }

    /**
     * Updates the information of the specified filter.
     * @param filter the filter to be updated
     */
    public void updateFilter(@NonNull final Filter filter) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues contentValues = buildFilterContentValues(filter);
        db.update(TABLE_FILTERS, contentValues, KEY_FILTER_ID + "=?",
                new String[]{Long.toString(filter.getId())});
    }

    // ******************** CRUD operations for the lens-filter link table ********************

    /**
     * Adds a mountable combination of filter and lens to the database.
     * @param filter the filter that can be mounted with the lens
     * @param lens the lens that can be mounted with the filter
     */
    public void addLensFilterLink(@NonNull final Filter filter, @NonNull final Lens lens){
        final SQLiteDatabase db = this.getWritableDatabase();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        final String query = "INSERT INTO " + TABLE_LINK_LENS_FILTER + "(" + KEY_FILTER_ID + "," + KEY_LENS_ID
                + ") SELECT " + filter.getId() + ", " + lens.getId()
                + " WHERE NOT EXISTS(SELECT 1 FROM " + TABLE_LINK_LENS_FILTER + " WHERE "
                + KEY_FILTER_ID + "=" + filter.getId() + " AND " + KEY_LENS_ID + "=" + lens.getId() + ");";
        db.execSQL(query);
    }

    /**
     * Deletes a mountable combination from the database
     * @param filter the filter that can be mounted with the lens
     * @param lens the lens that can be mounted with the filter
     */
    public void deleteLensFilterLink(@NonNull final Filter filter, @NonNull final Lens lens){
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LINK_LENS_FILTER, KEY_FILTER_ID + " = ? AND " + KEY_LENS_ID + " = ?",
                new String[]{Long.toString(filter.getId()), Long.toString(lens.getId())});
    }

    /**
     * Gets all the lenses that can be mounted to the specified filter
     * @param filter the filter whose lenses we want to get
     * @return a List of all linked lenses
     */
    public List<Lens> getLinkedLenses(@NonNull final Filter filter){
        final List<Lens> lenses = new ArrayList<>();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        final String query = "SELECT * FROM " + TABLE_LENSES + " WHERE " + KEY_LENS_ID + " IN "
                + "(" + "SELECT " + KEY_LENS_ID + " FROM " + TABLE_LINK_LENS_FILTER + " WHERE "
                + KEY_FILTER_ID + "=" + filter.getId() + ") ORDER BY " + KEY_LENS_MAKE;
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.rawQuery(query, null);
        Lens lens;
        while (cursor.moveToNext()) {
            lens = getLensFromCursor(cursor);
            lenses.add(lens);
        }
        cursor.close();
        return lenses;
    }

    /**
     * Gets all the filters that can be mounted to the specified lens
     * @param lens the lens whose filters we want to get
     * @return a List of all linked filters
     */
    public List<Filter> getLinkedFilters(@NonNull final Lens lens){
        final List<Filter> filters = new ArrayList<>();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        final String query = "SELECT * FROM " + TABLE_FILTERS + " WHERE " + KEY_FILTER_ID + " IN "
                + "(" + "SELECT " + KEY_FILTER_ID + " FROM " + TABLE_LINK_LENS_FILTER + " WHERE "
                + KEY_LENS_ID + "=" + lens.getId() + ") ORDER BY " + KEY_FILTER_MAKE;
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.rawQuery(query, null);
        Filter filter;
        while (cursor.moveToNext()) {
            filter = getFilterFromCursor(cursor);
            filters.add(filter);
        }
        cursor.close();
        return filters;
    }


    // ******************** CRUD operations for the frame-filter link table ********************

    /**
     * Adds a new link between a frame and a filter object
     *
     * @param frame frame that is linked to the filter
     * @param filter filter that is linked to the frame
     */
    private void addFrameFilterLink(@NonNull final Frame frame, @NonNull final Filter filter) {
        final SQLiteDatabase db = this.getWritableDatabase();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        final String query = "insert into " + TABLE_LINK_FRAME_FILTER + " ("
                + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ") "
                + "select " + frame.getId() + ", " + filter.getId() + " "
                + "where not exists (select * from " + TABLE_LINK_FRAME_FILTER + " "
                + "where " + KEY_FRAME_ID + " = " + frame.getId() + " and "
                + KEY_FILTER_ID + " = " + filter.getId() + ");";
        db.execSQL(query);
    }

    /**
     * Deletes all links between a single frame and all its linked filters.
     *
     * @param frame Frame object whose filter links should be deleted
     */
    private void deleteAllFrameFilterLinks(@NonNull final Frame frame) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LINK_FRAME_FILTER, KEY_FRAME_ID + " = ?",
                new String[]{Long.toString(frame.getId())});
    }

    /**
     * Gets all filter objects that are linked to a specific Frame object
     *
     * @param frame the Frame object whose linked filters we want to get
     * @return List object containing all Filter objects linked to the specified Frame object
     */
    private List<Filter> getLinkedFilters(@NonNull final Frame frame) {
        final List<Filter> filters = new ArrayList<>();
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        final String query = "select * from " + TABLE_FILTERS + " where " + KEY_FILTER_ID + " in "
                + "(select " + KEY_FILTER_ID + " from " + TABLE_LINK_FRAME_FILTER + " where "
                + KEY_FRAME_ID + " = " + frame.getId() + ") order by " + KEY_FILTER_MAKE;
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.rawQuery(query, null);
        Filter filter;
        while (cursor.moveToNext()) {
            filter = getFilterFromCursor(cursor);
            filters.add(filter);
        }
        cursor.close();
        return filters;
    }


    // ******************** CRUD operations for the film stock table ********************

    public Long addFilmStock(@NonNull final FilmStock filmStock) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues values = buildFilmStockContentValues(filmStock);
        return db.insert(TABLE_FILM_STOCKS, null, values);
    }

    public FilmStock getFilmStock(final long filmStockId) {
        final SQLiteDatabase db = this.getReadableDatabase();
        FilmStock filmStock = null;
        final Cursor cursor = db.query(TABLE_FILM_STOCKS, null, KEY_FILM_STOCK_ID + "=?",
                new String[]{Long.toString(filmStockId)}, null, null, null);
        if (cursor.moveToFirst()) {
            filmStock = getFilmStockFromCursor(cursor, new FilmStock());
            cursor.close();
        }
        return filmStock;
    }

    public List<FilmStock> getAllFilmStocks() {
        final List<FilmStock> filmStocks = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_FILM_STOCKS, null, null,
                null, null, null,
                KEY_FILM_MANUFACTURER_NAME + " collate nocase," + KEY_FILM_STOCK_NAME + " collate nocase");
        while (cursor.moveToNext()) {
            filmStocks.add(getFilmStockFromCursor(cursor, new FilmStock()));
        }
        cursor.close();
        return filmStocks;
    }

    public List<FilmStock> getAllFilmStocks(final String manufacturerName) {
        final List<FilmStock> filmStocks = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_FILM_STOCKS, null, KEY_FILM_MANUFACTURER_NAME + "=?",
                new String[]{manufacturerName}, null, null, KEY_FILM_STOCK_NAME + " collate nocase");
        while (cursor.moveToNext()) {
            filmStocks.add(getFilmStockFromCursor(cursor, new FilmStock()));
        }
        cursor.close();
        return filmStocks;
    }

    public List<String> getAllFilmManufacturers() {
        final List<String> manufacturers = new ArrayList<>();
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(true, TABLE_FILM_STOCKS, new String[]{KEY_FILM_MANUFACTURER_NAME},
                null, null, null, null, KEY_FILM_MANUFACTURER_NAME + " collate nocase", null);
        while (cursor.moveToNext()) {
            manufacturers.add(cursor.getString(cursor.getColumnIndex(KEY_FILM_MANUFACTURER_NAME)));
        }
        cursor.close();
        return manufacturers;
    }

    public boolean isFilmStockBeingUsed(@NonNull final FilmStock filmStock) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_ROLLS, new String[]{KEY_FILM_STOCK_ID}, KEY_FILM_STOCK_ID + "=?",
                new String[]{Long.toString(filmStock.getId())}, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        }
        return false;
    }

    public void deleteFilmStock(@NonNull final FilmStock filmStock) {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FILM_STOCKS, KEY_FILM_STOCK_ID + "=?",
                new String[]{Long.toString(filmStock.getId())});
    }

    public void updateFilmStock(@NonNull final FilmStock filmStock) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final ContentValues contentValues = buildFilmStockContentValues(filmStock);
        db.update(TABLE_FILM_STOCKS, contentValues, KEY_FILM_STOCK_ID + "=?",
                new String[]{Long.toString(filmStock.getId())});
    }


    //*********************** METHODS TO GET OBJECTS FROM CURSOR **********************************


    /**
     * Returns a Frame object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Frame object
     * @return Frame object generated from cursor
     */
    private Frame getFrameFromCursor (@NonNull final Cursor cursor) {
        final Frame frame = new Frame();
        return getFrameFromCursor(cursor, frame);
    }

    /**
     * Sets the attributes of a Frame object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param frame the Frame whose attributes should be set
     * @return reference to the Frame object given as the parameter
     */
    private Frame getFrameFromCursor (@NonNull final Cursor cursor, @NonNull final Frame frame) {
        frame.setId(cursor.getLong(cursor.getColumnIndex(KEY_FRAME_ID)));
        frame.setRollId(cursor.getLong(cursor.getColumnIndex(KEY_ROLL_ID)));
        frame.setCount(cursor.getInt(cursor.getColumnIndex(KEY_COUNT)));

        final String date = cursor.getString(cursor.getColumnIndex(KEY_DATE));
        if (date != null) frame.setDate(new DateTime(date));

        final long lensId = cursor.getLong(cursor.getColumnIndex(KEY_LENS_ID));
        if (lensId > 0) frame.setLens(getLens(lensId));

        frame.setShutter(cursor.getString(cursor.getColumnIndex(KEY_SHUTTER)));
        frame.setAperture(cursor.getString(cursor.getColumnIndex(KEY_APERTURE)));
        frame.setNote(cursor.getString(cursor.getColumnIndex(KEY_FRAME_NOTE)));

        final String location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION));
        if (location != null) frame.setLocation(new Location(location));

        frame.setFocalLength(cursor.getInt(cursor.getColumnIndex(KEY_FOCAL_LENGTH)));
        frame.setExposureComp(cursor.getString(cursor.getColumnIndex(KEY_EXPOSURE_COMP)));
        frame.setNoOfExposures(cursor.getInt(cursor.getColumnIndex(KEY_NO_OF_EXPOSURES)));

        final int flashUsed = cursor.getInt(cursor.getColumnIndex(KEY_FLASH_USED));
        frame.setFlashUsed(flashUsed > 0);

        frame.setFlashPower(cursor.getString(cursor.getColumnIndex(KEY_FLASH_POWER)));
        frame.setFlashComp(cursor.getString(cursor.getColumnIndex(KEY_FLASH_COMP)));
        frame.setMeteringMode(cursor.getInt(cursor.getColumnIndex(KEY_METERING_MODE)));
        frame.setFormattedAddress(cursor.getString(cursor.getColumnIndex(KEY_FORMATTED_ADDRESS)));
        frame.setPictureFilename(cursor.getString(cursor.getColumnIndex(KEY_PICTURE_FILENAME)));
        frame.setLightSource(cursor.getInt(cursor.getColumnIndex(KEY_LIGHT_SOURCE)));
        frame.setFilters(getLinkedFilters(frame));
        return frame;
    }

    /**
     * Returns a Roll object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Roll object
     * @return Roll object generated from cursor
     */
    private Roll getRollFromCursor (@NonNull final Cursor cursor) {
        final Roll roll = new Roll();
        return getRollFromCursor(cursor, roll);
    }

    /**
     * Sets the attributes of a Roll object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param roll the Roll whose attributes should be set
     * @return reference to the Roll object given as the parameter
     */
    private Roll getRollFromCursor (@NonNull final Cursor cursor, @NonNull final Roll roll) {
        roll.setId(cursor.getLong(cursor.getColumnIndex(KEY_ROLL_ID)));
        roll.setName(cursor.getString(cursor.getColumnIndex(KEY_ROLLNAME)));

        final String date = cursor.getString(cursor.getColumnIndex(KEY_ROLL_DATE));
        if (date != null) roll.setDate(new DateTime(date));

        roll.setNote(cursor.getString(cursor.getColumnIndex(KEY_ROLL_NOTE)));
        roll.setCameraId(cursor.getLong(cursor.getColumnIndex(KEY_CAMERA_ID)));
        roll.setIso(cursor.getInt(cursor.getColumnIndex(KEY_ROLL_ISO)));
        roll.setPushPull(cursor.getString(cursor.getColumnIndex(KEY_ROLL_PUSH)));
        roll.setFormat(cursor.getInt(cursor.getColumnIndex(KEY_ROLL_FORMAT)));
        final int archived = cursor.getInt(cursor.getColumnIndex(KEY_ROLL_ARCHIVED));
        roll.setArchived(archived > 0);
        roll.setFilmStockId(cursor.getInt(cursor.getColumnIndex(KEY_FILM_STOCK_ID)));

        final String unloaded = cursor.getString(cursor.getColumnIndex(KEY_ROLL_UNLOADED));
        if (unloaded != null) roll.setUnloaded(new DateTime(unloaded));

        final String developed = cursor.getString(cursor.getColumnIndex(KEY_ROLL_DEVELOPED));
        if (developed != null) roll.setDeveloped(new DateTime(developed));

        return roll;
    }

    /**
     * Returns a Lens object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Lens object
     * @return Lens object generated from cursor
     */
    private Lens getLensFromCursor (@NonNull final Cursor cursor) {
        final Lens lens = new Lens();
        return getLensFromCursor(cursor, lens);
    }

    /**
     * Sets the attributes of a Lens object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param lens the Lens whose attributes should be set
     * @return reference to the Lens object given as the parameter
     */
    private Lens getLensFromCursor (@NonNull final Cursor cursor, @NonNull final Lens lens) {
        lens.setId(cursor.getLong(cursor.getColumnIndex(KEY_LENS_ID)));
        lens.setMake(cursor.getString(cursor.getColumnIndex(KEY_LENS_MAKE)));
        lens.setModel(cursor.getString(cursor.getColumnIndex(KEY_LENS_MODEL)));
        lens.setSerialNumber(cursor.getString(cursor.getColumnIndex(KEY_LENS_SERIAL_NO)));
        lens.setMinAperture(cursor.getString(cursor.getColumnIndex(KEY_LENS_MIN_APERTURE)));
        lens.setMaxAperture(cursor.getString(cursor.getColumnIndex(KEY_LENS_MAX_APERTURE)));
        lens.setMinFocalLength(cursor.getInt(cursor.getColumnIndex(KEY_LENS_MIN_FOCAL_LENGTH)));
        lens.setMaxFocalLength(cursor.getInt(cursor.getColumnIndex(KEY_LENS_MAX_FOCAL_LENGTH)));
        lens.setApertureIncrements(cursor.getInt(cursor.getColumnIndex(KEY_LENS_APERTURE_INCREMENTS)));
        return lens;
    }

    /**
     * Returns a Camera object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Camera object
     * @return Camera object generated from cursor
     */
    private Camera getCameraFromCursor (@NonNull final Cursor cursor) {
        final Camera camera = new Camera();
        return getCameraFromCursor(cursor, camera);
    }

    /**
     * Sets the attributes of a Camera object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param camera the Camera whose attributes should be set
     * @return reference to the Camera object given as the parameter
     */
    private Camera getCameraFromCursor (@NonNull final Cursor cursor, @NonNull final Camera camera) {
        camera.setId(cursor.getLong(cursor.getColumnIndex(KEY_CAMERA_ID)));
        camera.setMake(cursor.getString(cursor.getColumnIndex(KEY_CAMERA_MAKE)));
        camera.setModel(cursor.getString(cursor.getColumnIndex(KEY_CAMERA_MODEL)));
        camera.setSerialNumber(cursor.getString(cursor.getColumnIndex(KEY_CAMERA_SERIAL_NO)));
        camera.setMinShutter(cursor.getString(cursor.getColumnIndex(KEY_CAMERA_MIN_SHUTTER)));
        camera.setMaxShutter(cursor.getString(cursor.getColumnIndex(KEY_CAMERA_MAX_SHUTTER)));
        camera.setShutterIncrements(cursor.getInt(cursor.getColumnIndex(KEY_CAMERA_SHUTTER_INCREMENTS)));
        camera.setExposureCompIncrements(cursor.getInt(cursor.getColumnIndex(KEY_CAMERA_EXPOSURE_COMP_INCREMENTS)));
        return camera;
    }

    /**
     * Returns a Filter object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Filter object
     * @return Filter object generated from cursor
     */
    private Filter getFilterFromCursor (@NonNull final Cursor cursor) {
        final Filter filter = new Filter();
        return getFilterFromCursor(cursor, filter);
    }

    /**
     * Sets the attributes of a Filter object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param filter the Filter whose attributes should be set
     * @return reference to the Filter object given as the parameter
     */
    private Filter getFilterFromCursor (@NonNull final Cursor cursor, @NonNull final Filter filter) {
        filter.setId(cursor.getLong(cursor.getColumnIndex(KEY_FILTER_ID)));
        filter.setMake(cursor.getString(cursor.getColumnIndex(KEY_FILTER_MAKE)));
        filter.setModel(cursor.getString(cursor.getColumnIndex(KEY_FILTER_MODEL)));
        return filter;
    }

    /**
     * Sets the attributes of a FilmStock object using a Cursor object
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param filmStock the FilmStock whose attributes should be set
     * @return reference to the FilmStock object given as the parameter
     */
    private FilmStock getFilmStockFromCursor(@NonNull final Cursor cursor, @NonNull final FilmStock filmStock) {
        filmStock.setId(cursor.getLong(cursor.getColumnIndex(KEY_FILM_STOCK_ID)));
        filmStock.setMake(cursor.getString(cursor.getColumnIndex(KEY_FILM_MANUFACTURER_NAME)));
        filmStock.setModel(cursor.getString(cursor.getColumnIndex(KEY_FILM_STOCK_NAME)));
        filmStock.setIso(cursor.getInt(cursor.getColumnIndex(KEY_FILM_ISO)));
        filmStock.setType(cursor.getInt(cursor.getColumnIndex(KEY_FILM_TYPE)));
        filmStock.setProcess(cursor.getInt(cursor.getColumnIndex(KEY_FILM_PROCESS)));
        final int preadded = cursor.getInt(cursor.getColumnIndex(KEY_FILM_IS_PREADDED));
        filmStock.setPreadded(preadded > 0);
        return filmStock;
    }


    //*********************** METHODS TO BUILD CONTENT VALUES **********************************


    /**
     * Builds ContentValues container from a Frame object.
     *
     * @param frame Frame object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Frame object.
     */
    private ContentValues buildFrameContentValues(@NonNull final Frame frame){
        final ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ROLL_ID, frame.getRollId());
        contentValues.put(KEY_COUNT, frame.getCount());
        contentValues.put(KEY_DATE, frame.getDate() != null ? frame.getDate().toString() : null);

        if (frame.getLens() != null) contentValues.put(KEY_LENS_ID, frame.getLens().getId());
        else contentValues.putNull(KEY_LENS_ID);

        contentValues.put(KEY_SHUTTER, frame.getShutter());
        contentValues.put(KEY_APERTURE, frame.getAperture());
        contentValues.put(KEY_FRAME_NOTE, frame.getNote());
        contentValues.put(KEY_LOCATION, frame.getLocation() != null ? frame.getLocation().toString() : null);
        contentValues.put(KEY_FOCAL_LENGTH, frame.getFocalLength());
        contentValues.put(KEY_EXPOSURE_COMP, frame.getExposureComp());
        contentValues.put(KEY_NO_OF_EXPOSURES, frame.getNoOfExposures());
        contentValues.put(KEY_FLASH_USED, frame.getFlashUsed());
        contentValues.put(KEY_FLASH_POWER, frame.getFlashPower());
        contentValues.put(KEY_FLASH_COMP, frame.getFlashComp());
        contentValues.put(KEY_METERING_MODE, frame.getMeteringMode());
        contentValues.put(KEY_FORMATTED_ADDRESS, frame.getFormattedAddress());
        contentValues.put(KEY_PICTURE_FILENAME, frame.getPictureFilename());
        contentValues.put(KEY_LIGHT_SOURCE, frame.getLightSource());
        return contentValues;
    }

    /**
     * Builds ContentValues container from a Lens object.
     *
     * @param lens Lens object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the lens object.
     */
    private ContentValues buildLensContentValues(@NonNull final Lens lens){
        final ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_LENS_MAKE, lens.getMake());
        contentValues.put(KEY_LENS_MODEL, lens.getModel());
        contentValues.put(KEY_LENS_SERIAL_NO, lens.getSerialNumber());
        contentValues.put(KEY_LENS_MIN_APERTURE, lens.getMinAperture());
        contentValues.put(KEY_LENS_MAX_APERTURE, lens.getMaxAperture());
        contentValues.put(KEY_LENS_MIN_FOCAL_LENGTH, lens.getMinFocalLength());
        contentValues.put(KEY_LENS_MAX_FOCAL_LENGTH, lens.getMaxFocalLength());
        contentValues.put(KEY_LENS_APERTURE_INCREMENTS, lens.getApertureIncrements());
        return contentValues;
    }

    /**
     * Builds ContentValues container from a Camera object.
     *
     * @param camera Camera object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Camera object.
     */
    private ContentValues buildCameraContentValues(@NonNull final Camera camera){
        final ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_CAMERA_MAKE, camera.getMake());
        contentValues.put(KEY_CAMERA_MODEL, camera.getModel());
        contentValues.put(KEY_CAMERA_SERIAL_NO, camera.getSerialNumber());
        contentValues.put(KEY_CAMERA_MIN_SHUTTER, camera.getMinShutter());
        contentValues.put(KEY_CAMERA_MAX_SHUTTER, camera.getMaxShutter());
        contentValues.put(KEY_CAMERA_SHUTTER_INCREMENTS, camera.getShutterIncrements());
        contentValues.put(KEY_CAMERA_EXPOSURE_COMP_INCREMENTS, camera.getExposureCompIncrements());
        return contentValues;
    }

    /**
     * Builds ContentValues container from a Roll object.
     *
     * @param roll Roll object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Roll object.
     */
    private ContentValues buildRollContentValues(@NonNull final Roll roll){
        final ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ROLLNAME, roll.getName());
        contentValues.put(KEY_ROLL_DATE, roll.getDate() != null ? roll.getDate().toString() : null);
        contentValues.put(KEY_ROLL_NOTE, roll.getNote());
        if (roll.getCameraId() > 0) contentValues.put(KEY_CAMERA_ID, roll.getCameraId());
        else contentValues.putNull(KEY_CAMERA_ID);
        contentValues.put(KEY_ROLL_ISO, roll.getIso());
        contentValues.put(KEY_ROLL_PUSH, roll.getPushPull());
        contentValues.put(KEY_ROLL_FORMAT, roll.getFormat());
        contentValues.put(KEY_ROLL_ARCHIVED, roll.getArchived());
        if (roll.getFilmStockId() > 0) contentValues.put(KEY_FILM_STOCK_ID, roll.getFilmStockId());
        else contentValues.putNull(KEY_FILM_STOCK_ID);
        contentValues.put(KEY_ROLL_UNLOADED, roll.getUnloaded() != null ? roll.getUnloaded().toString() : null);
        contentValues.put(KEY_ROLL_DEVELOPED, roll.getDeveloped() != null ? roll.getDeveloped().toString() : null);
        return contentValues;
    }

    /**
     * Builds ContentValues container from a Filter object.
     *
     * @param filter Filter object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Filter object.
     */
    private ContentValues buildFilterContentValues(@NonNull final Filter filter){
        final ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_FILTER_MAKE, filter.getMake());
        contentValues.put(KEY_FILTER_MODEL, filter.getModel());
        return contentValues;
    }

    /**
     * Builds ContentValues container from a FilmStock object
     *
     * @param filmStock FilmStock object of which the ContentValues is created
     * @return ContentValues containing the attributes of the FilmStock object
     */
    private ContentValues buildFilmStockContentValues(@NonNull final FilmStock filmStock) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_FILM_MANUFACTURER_NAME, filmStock.getMake());
        contentValues.put(KEY_FILM_STOCK_NAME, filmStock.getModel());
        contentValues.put(KEY_FILM_ISO, filmStock.getIso());
        contentValues.put(KEY_FILM_TYPE, filmStock.getType());
        contentValues.put(KEY_FILM_PROCESS, filmStock.getProcess());
        contentValues.put(KEY_FILM_IS_PREADDED, filmStock.isPreadded());
        return contentValues;
    }

    //*********************** METHODS TO EXPORT AND IMPORT DATABASE *******************************

    /**
     * Returns a File object referencing the .db database file used to store this database.
     *
     * @param context the application's context
     * @return File referencing the database file used by this SQLite database
     */
    public static File getDatabaseFile(final Context context){
        final String databasePath = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
        return new File(databasePath);
    }

    /**
     * Imports a .db database file to be used as the database for this app.
     * If the new database file cannot be copied, then throw IOException.
     * If it cannot be opened as an SQLite database, return false and notify.
     * If the file is corrupted, then it is discarded and the old database will be used.
     * Return false and notify.
     *
     * @param context the application's context
     * @param importDatabasePath the path to the file which should be imported
     * @return true if the database was imported successfully
     * @throws IOException is thrown if the new database file cannot be opened
     */
    public boolean importDatabase(final Context context, final String importDatabasePath) throws IOException {
        final File newDb = new File(importDatabasePath);
        final File oldDbBackup = new File(context.getDatabasePath(DATABASE_NAME).getAbsolutePath() + "_backup");
        final File oldDb = getDatabaseFile(context);

        if (newDb.exists()) {

            close();

            //Backup the old database file in case the new file is corrupted.
            Utilities.copyFile(oldDb, oldDbBackup);

            //Replace the old database file with the new one.
            Utilities.copyFile(newDb, oldDb);

            // Access the copied database so SQLiteHelper will cache it and mark
            // it as created.

            final SQLiteDatabase db;
            final boolean[] success = {true};
            try {
                db = SQLiteDatabase.openDatabase(getDatabaseFile(context).getAbsolutePath(), null,
                        SQLiteDatabase.OPEN_READWRITE, dbObj -> {
                    // If the database was corrupt, try to replace with the old backup.
                    try {
                        Utilities.copyFile(oldDbBackup, oldDb);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    success[0] = false;
                });
            } catch (final SQLiteException e) {
                Toast.makeText(context, context.getResources().getString(R.string.CouldNotReadDatabase),
                        Toast.LENGTH_LONG).show();
                return false;
            }

            if (!success[0]) {
                // If the new database file was corrupt, let the user know.
                Toast.makeText(context, context.getResources().getString(R.string.CouldNotReadDatabase),
                        Toast.LENGTH_LONG).show();
                return false;
            }

            if (!runIntegrityCheck()) {
                //If the new database file failed the integrity check, replace it with the backup.
                db.close();
                Utilities.copyFile(oldDbBackup, oldDb);
                Toast.makeText(context, context.getResources().getString(R.string.IntegrityCheckFailed),
                        Toast.LENGTH_LONG).show();
                return false;
            }

            return true;
        }
        return false;
    }

    /**
     * Check the integrity of the current database.
     * The database is valid if it can be used with this app.
     *
     * The integrity of the database is checked AFTER a new database is imported.
     * New columns added during onUpgrade() will be present, which is why
     * checkColumnProperties() should be run for all new columns as well.
     *
     * @return true if the database is a valid database to be used with this app
     */
    private boolean runIntegrityCheck() {

        final String INTEGER = "int";
        final String TEXT = "text";
        final String CASCADE = "CASCADE";
        final String SET_NULL = "SET NULL";
        //Run integrity checks to see if the current database is whole
        return checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_ID, INTEGER, 0, true, true) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MAKE, TEXT, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MODEL, TEXT, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MAX_SHUTTER, TEXT, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MIN_SHUTTER, TEXT, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_SERIAL_NO, TEXT, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_SHUTTER_INCREMENTS, INTEGER, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_EXPOSURE_COMP_INCREMENTS, INTEGER, 1) &&

                checkColumnProperties(TABLE_LENSES, KEY_LENS_ID, INTEGER, 0, true, true) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAKE, TEXT, 1) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MODEL, TEXT, 1) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAX_APERTURE, TEXT, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MIN_APERTURE, TEXT, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAX_FOCAL_LENGTH, INTEGER, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MIN_FOCAL_LENGTH, INTEGER, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_SERIAL_NO, TEXT, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_APERTURE_INCREMENTS, INTEGER, 1) &&

                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_ID, INTEGER, 0, true, true) &&
                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_MAKE, TEXT, 1) &&
                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_MODEL, TEXT, 1) &&

                checkColumnProperties(TABLE_LINK_CAMERA_LENS, KEY_CAMERA_ID, INTEGER, 1, true, false, true, TABLE_CAMERAS, CASCADE) &&
                checkColumnProperties(TABLE_LINK_CAMERA_LENS, KEY_LENS_ID, INTEGER, 1, true, false, true, TABLE_LENSES, CASCADE) &&

                checkColumnProperties(TABLE_LINK_LENS_FILTER, KEY_LENS_ID, INTEGER, 1, true, false, true, TABLE_LENSES, CASCADE) &&
                checkColumnProperties(TABLE_LINK_LENS_FILTER, KEY_FILTER_ID, INTEGER, 1, true, false, true, TABLE_FILTERS, CASCADE) &&

                checkColumnProperties(TABLE_LINK_FRAME_FILTER, KEY_FRAME_ID, INTEGER, 1, true, false, true, TABLE_FRAMES, CASCADE) &&
                checkColumnProperties(TABLE_LINK_FRAME_FILTER, KEY_FILTER_ID, INTEGER, 1, true, false, true, TABLE_FILTERS, CASCADE) &&

                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ID, INTEGER, 0, true, true) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLLNAME, TEXT, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_DATE, TEXT, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_NOTE, TEXT, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_CAMERA_ID, INTEGER, 0, false, false, true, TABLE_CAMERAS, SET_NULL) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ISO, INTEGER, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_PUSH, TEXT, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_FORMAT, INTEGER, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ARCHIVED, INTEGER, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_FILM_STOCK_ID, INTEGER, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_UNLOADED, TEXT, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_DEVELOPED, TEXT, 0) &&

                checkColumnProperties(TABLE_FRAMES, KEY_FRAME_ID, INTEGER, 0, true, true) &&
                checkColumnProperties(TABLE_FRAMES, KEY_ROLL_ID, INTEGER, 1, false, false, true, TABLE_ROLLS, CASCADE) &&
                checkColumnProperties(TABLE_FRAMES, KEY_COUNT, INTEGER, 1) &&
                checkColumnProperties(TABLE_FRAMES, KEY_DATE, TEXT, 1) &&
                checkColumnProperties(TABLE_FRAMES, KEY_LENS_ID, INTEGER, 0, false, false, true, TABLE_LENSES, SET_NULL) &&
                checkColumnProperties(TABLE_FRAMES, KEY_SHUTTER, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_APERTURE, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FRAME_NOTE, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_LOCATION, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FOCAL_LENGTH, INTEGER, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_EXPOSURE_COMP, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_NO_OF_EXPOSURES, INTEGER, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FLASH_USED, INTEGER, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FLASH_POWER, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FLASH_COMP, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FRAME_SIZE, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_METERING_MODE, INTEGER, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FORMATTED_ADDRESS, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_PICTURE_FILENAME, TEXT, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_LIGHT_SOURCE, INTEGER, 0) &&

                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_STOCK_ID, INTEGER, 0, true, true) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_STOCK_NAME, TEXT, 1) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_MANUFACTURER_NAME, TEXT, 1) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_ISO, INTEGER, 0) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_TYPE, INTEGER, 0) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_PROCESS, INTEGER, 0) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_IS_PREADDED, INTEGER, 0)
                ;

    }

    /**
     * Help method to mimic default parameter values in Java style.
     * Full method description found down the method call path.
     */
    private boolean checkColumnProperties(final String tableNameInput, final String columnNameInput, final String columnTypeInput,
                                          final int notNullInput) {
        return checkColumnProperties(tableNameInput, columnNameInput, columnTypeInput, notNullInput, false, false);
    }

    /**
     * Help method to mimic default parameter values in Java style.
     * Full method description found down the method call path.
     */
    private boolean checkColumnProperties(final String tableNameInput, final String columnNameInput, final String columnTypeInput,
                                          final int notNullInput, final boolean primaryKeyInput, final boolean autoIncrementInput) {
        return checkColumnProperties(tableNameInput, columnNameInput, columnTypeInput,
                notNullInput, primaryKeyInput, autoIncrementInput, false, null, null);
    }

    /**
     * Checks the properties of a table column. If all the parameters match to the
     * database's properties, then the method returns true.
     *
     * @param tableNameInput the name of the table
     * @param columnNameInput the name of the column in the table
     * @param columnTypeInput the data type of the column
     * @param notNullInput 1 if the column should be 'not null', 0 if null
     * @param primaryKeyInput 1 if the column should be a primary key, 0 if not
     * @param autoIncrementInput true if the table should be autoincrement
     * @return true if the parameter properties match the database
     */
    private boolean checkColumnProperties(final String tableNameInput, final String columnNameInput, final String columnTypeInput,
                                          final int notNullInput, final boolean primaryKeyInput, final boolean autoIncrementInput,
                                          final boolean foreignKeyInput, final String referenceTableNameInput,
                                          final String onDeleteActionInput) {

        final SQLiteDatabase db = this.getReadableDatabase();

        //Check for possible autoincrement
        if (autoIncrementInput) {
            // We can check that the table is autoincrement from the master tables.
            // Column 'sql' is the query with which the table was created.
            // If a table is autoincrement, then it can only have one primary key.
            // If the primary key matches, then also the autoincrement column is correct.
            // The primary key will be checked later in this method.
            final String incrementQuery = "SELECT * FROM sqlite_master WHERE type = 'table' AND name = '" + tableNameInput +"' AND sql LIKE '%AUTOINCREMENT%'";
            final Cursor incrementCursor = db.rawQuery(incrementQuery, null);
            if (!incrementCursor.moveToFirst()) {
                //No rows were returned. The table has no autoincrement. Integrity check fails.
                incrementCursor.close();
                return false;
            }
            incrementCursor.close();
        }

        //Check for possible foreign key reference
        if (foreignKeyInput) {
            // We can check that the column is a foreign key column using one of the SQLite pragma statements.
            final String foreignKeyQuery = "PRAGMA FOREIGN_KEY_LIST('" + tableNameInput + "')";
            final Cursor foreignKeyCursor = db.rawQuery(foreignKeyQuery, null);
            boolean foreignKeyFound = false;
            //Iterate through the tables foreign key columns and get the properties.
            while (foreignKeyCursor.moveToNext()) {
                final String table = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndex("table"));
                final String from = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndex("from"));
                final String onDelete = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndex("on_delete"));
                final String to = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndex("to"));
                //If the table, from-column and on-delete actions match to those defined
                //by the parameters, the foreign key is correct. The to-column value
                //should be null, because during table creation we have used the shorthand form
                //to reference the parent table's primary key.
                if (table.equals(referenceTableNameInput) && from.equals(columnNameInput)
                        && onDelete.equalsIgnoreCase(onDeleteActionInput) && to == null) {
                    foreignKeyFound = true;
                    break;
                }
            }
            foreignKeyCursor.close();
            //If foreign key was not defined, integrity check fails -> return false.
            if (!foreignKeyFound) return false;
        }

        final String query = "PRAGMA TABLE_INFO('" + tableNameInput + "');";
        final Cursor cursor = db.rawQuery(query, null);

        //Iterate the result rows...
        while (cursor.moveToNext()) {

            final String columnName = cursor.getString(cursor.getColumnIndex("name"));
            // ...until the name checks.
            if (columnName.equals(columnNameInput)) {

                final String columnType = cursor.getString(cursor.getColumnIndex("type"));
                final int notNull = cursor.getInt(cursor.getColumnIndex("notnull"));
                //If the column is defined as primary key, the pk value is 1.
                final boolean primaryKey = cursor.getInt(cursor.getColumnIndex("pk")) > 0;

                cursor.close();

                //Check that the attributes are correct and return the result
                return columnType.startsWith(columnTypeInput) && //type can be int or integer
                        notNull == notNullInput &&
                        primaryKey == primaryKeyInput;

            }
        }
        //We get here if no matching column names were found
        cursor.close();
        return false;
    }

    /**
     * Populate films from resource arrays to the database
     */
    private void populateFilmStocks(final SQLiteDatabase db) {
        final String[] filmStocks = context.getResources().getStringArray(R.array.FilmStocks);
        int counter = 0;
        for (final String s : filmStocks) {
            try {
                final String[] components = s.split(",");
                final FilmStock filmStock = new FilmStock();
                filmStock.setMake(components[0]);
                filmStock.setModel(components[1]);
                filmStock.setIso(Integer.parseInt(components[2]));
                filmStock.setType(Integer.parseInt(components[3]));
                filmStock.setProcess(Integer.parseInt(components[4]));
                filmStock.setPreadded(true);

                final ContentValues values = buildFilmStockContentValues(filmStock);

                // Insert film stocks if they do not already exist.
                final Cursor cursor = db.query(TABLE_FILM_STOCKS, null, KEY_FILM_MANUFACTURER_NAME + "=? AND " + KEY_FILM_STOCK_NAME + "=?",
                        new String[]{filmStock.getMake(), filmStock.getModel()}, null, null, null);
                if (cursor.moveToFirst()) {
                    cursor.close();
                } else {
                    db.insert(TABLE_FILM_STOCKS, null, values);
                }

            } catch (final ArrayIndexOutOfBoundsException | NumberFormatException e) {
                counter++;
            }
        }
        if (counter > 0) {
            Toast.makeText(context, R.string.ErrorAddingFilmStocks, Toast.LENGTH_LONG).show();
        }
    }

}
