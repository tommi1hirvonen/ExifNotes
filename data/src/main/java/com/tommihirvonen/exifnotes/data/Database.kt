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

package com.tommihirvonen.exifnotes.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import com.tommihirvonen.exifnotes.core.entities.*
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.repositories.FilmStockRepository
import com.tommihirvonen.exifnotes.database.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FilmDbHelper is the SQL database class that holds all the information
 * the user stores in the app. This class provides all necessary CRUD operations as well as
 * export and import functionality.
 */
@Singleton
class Database @Inject constructor(@ApplicationContext private val context: Context)
    : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "filmnotes.db"
        private const val DATABASE_VERSION = 24
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // The only time foreign key constraints are enforced, is when something is written
        // to the database. Only enable foreign keys, if the database may be written to.
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    override fun onCreate(database: SQLiteDatabase) {
        // Enable foreign key support, since we aren't overriding onConfigure() (added in API 16).
        database.execSQL("PRAGMA foreign_keys=ON;")
        database.execSQL(CREATE_FILM_STOCKS_TABLE)
        database.execSQL(CREATE_LENS_TABLE)
        database.execSQL(CREATE_CAMERA_TABLE)
        database.execSQL(CREATE_FILTER_TABLE)
        database.execSQL(CREATE_ROLL_TABLE)
        database.execSQL(CREATE_FRAME_TABLE)
        database.execSQL(CREATE_LINK_CAMERA_LENS_TABLE)
        database.execSQL(CREATE_LINK_LENS_FILTER_TABLE)
        database.execSQL(CREATE_LINK_FRAME_FILTER_TABLE)
        populateFilmStocks(database)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Run all the required upgrade scripts consecutively.
        // New if blocks should be added whenever the database version is raised and new
        // columns and/or tables are added.
        if (oldVersion < 14) {
            //TABLE_FRAMES
            db.execSQL(ALTER_TABLE_FRAMES_1)
            db.execSQL(ALTER_TABLE_FRAMES_2)
            db.execSQL(ALTER_TABLE_FRAMES_3)
            db.execSQL(ALTER_TABLE_FRAMES_4)
            db.execSQL(ALTER_TABLE_FRAMES_5)
            db.execSQL(ALTER_TABLE_FRAMES_6)
            db.execSQL(ALTER_TABLE_FRAMES_7)
            db.execSQL(ALTER_TABLE_FRAMES_8)
            db.execSQL(ALTER_TABLE_FRAMES_9)
            //TABLE_LENSES
            db.execSQL(ALTER_TABLE_LENSES_1)
            db.execSQL(ALTER_TABLE_LENSES_2)
            db.execSQL(ALTER_TABLE_LENSES_3)
            db.execSQL(ALTER_TABLE_LENSES_4)
            db.execSQL(ALTER_TABLE_LENSES_5)
            db.execSQL(ALTER_TABLE_LENSES_6)
            //TABLE_CAMERAS
            db.execSQL(ALTER_TABLE_CAMERAS_1)
            db.execSQL(ALTER_TABLE_CAMERAS_2)
            db.execSQL(ALTER_TABLE_CAMERAS_3)
            db.execSQL(ALTER_TABLE_CAMERAS_4)
            //TABLE_ROLLS
            db.execSQL(ALTER_TABLE_ROLLS_1)
            db.execSQL(ALTER_TABLE_ROLLS_2)
            db.execSQL(ALTER_TABLE_ROLLS_3)
            //In an earlier version special chars were not allowed.
            //Instead quote marks were changed to 'q' when stored in the SQLite database.
            db.execSQL(REPLACE_QUOTE_CHARS)
            //TABLE_FILTERS
            db.execSQL(ON_UPGRADE_CREATE_FILTER_TABLE)
            //TABLE MOUNTABLES
            db.execSQL(ON_UPGRADE_CREATE_LINK_LENS_FILTER_TABLE)
        }
        if (oldVersion < 15) {
            db.execSQL(ALTER_TABLE_FRAMES_10)
        }
        if (oldVersion < 16) {
            db.execSQL(ALTER_TABLE_ROLLS_4)
        }
        if (oldVersion < 17) {
            db.execSQL(ALTER_TABLE_FRAMES_11)
        }
        if (oldVersion < 18) {
            db.execSQL(ALTER_TABLE_CAMERAS_5)
        }
        if (oldVersion < 19) {
            // Enable foreign key support, since we aren't overriding onConfigure() (added in API 16).
            db.execSQL("PRAGMA foreign_keys=ON;")
            // Alter statements
            db.beginTransaction()
            try {
                // execSQL() does not support multiple SQL commands separated with a semi-colon.
                // Separate the upgrade commands into single SQL commands.
                db.execSQL(ROLLS_TABLE_REVISION_1)
                db.execSQL(ROLLS_TABLE_REVISION_2)
                db.execSQL(ROLLS_TABLE_REVISION_3)
                db.execSQL(ROLLS_TABLE_REVISION_4)
                db.execSQL(FRAMES_TABLE_REVISION_1)
                db.execSQL(FRAMES_TABLE_REVISION_2)
                db.execSQL(FRAMES_TABLE_REVISION_3)
                db.execSQL(ON_UPGRADE_CREATE_LINK_FRAME_FILTER_TABLE)
                db.execSQL(FRAMES_TABLE_REVISION_4)
                db.execSQL(FRAMES_TABLE_REVISION_5)
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_1)
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_2)
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_3)
                db.execSQL(CAMERA_LENS_LINK_TABLE_REVISION_4)
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_1)
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_2)
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_3)
                db.execSQL(LENS_FILTER_LINK_TABLE_REVISION_4)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        if (oldVersion < 20) {
            db.execSQL(CREATE_FILM_STOCKS_TABLE)
            db.execSQL(ALTER_TABLE_FRAMES_12)
            db.execSQL(ALTER_TABLE_ROLLS_5)
            populateFilmStocks(db)
        }
        if (oldVersion < 21) {
            db.execSQL(ALTER_TABLE_ROLLS_6)
            db.execSQL(ALTER_TABLE_ROLLS_7)
        }
        if (oldVersion < 22) {
            db.execSQL(ALTER_TABLE_CAMERAS_6)
        }
        if (oldVersion < 23) {
            db.execSQL(ALTER_TABLE_CAMERAS_7)
        }
        if (oldVersion < 24) {
            db.execSQL(ALTER_TABLE_LENSES_7)
        }
    }

    /**
     * Returns a File object referencing the .db database file used to store this database.
     *
     * @return File referencing the database file used by this SQLite database
     */
    fun getDatabaseFile(): File {
        val databasePath = context.getDatabasePath(DATABASE_NAME).absolutePath
        return File(databasePath)
    }

    /**
     * Imports a .db database file to be used as the database for this app.
     * If the new database file cannot be copied, then throw IOException.
     * If it cannot be opened as an SQLite database, return false and notify.
     * If the file is corrupted, then it is discarded and the old database will be used.
     * Return false and notify.
     *
     * @param importDatabasePath the path to the file which should be imported
     * @return true if the database was imported successfully
     * @throws IOException is thrown if the new database file cannot be opened
     */
    @Throws(IOException::class)
    fun importDatabase(importDatabasePath: String): Boolean {
        val newDb = File(importDatabasePath)
        val oldDbBackup = File(context.getDatabasePath(DATABASE_NAME).absolutePath + "_backup")
        val oldDb = getDatabaseFile()
        if (newDb.exists()) {
            close()

            //Backup the old database file in case the new file is corrupted.
            oldDb.copyTo(oldDbBackup, overwrite = true)

            //Replace the old database file with the new one.
            newDb.copyTo(oldDb, overwrite = true)

            // Access the copied database so SQLiteHelper will cache it and mark it as created.
            val success = booleanArrayOf(true)
            try {
                SQLiteDatabase.openDatabase(getDatabaseFile().absolutePath, null,
                        SQLiteDatabase.OPEN_READWRITE
                ) {
                    // If the database was corrupt, try to replace with the old backup.
                    try {
                        oldDbBackup.copyTo(oldDb, overwrite = true)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    success[0] = false
                }
            } catch (e: SQLiteException) {
                Toast.makeText(context, context.resources.getString(R.string.CouldNotReadDatabase),
                        Toast.LENGTH_LONG).show()
                return false
            }
            if (!success[0]) {
                // If the new database file was corrupt, let the user know.
                Toast.makeText(context, context.resources.getString(R.string.CouldNotReadDatabase),
                        Toast.LENGTH_LONG).show()
                return false
            }
            if (!runIntegrityCheck()) {
                //If the new database file failed the integrity check, replace it with the backup.
                close()
                oldDbBackup.copyTo(oldDb, overwrite = true)
                Toast.makeText(context, context.resources.getString(R.string.IntegrityCheckFailed),
                        Toast.LENGTH_LONG).show()
                return false
            }
            return true
        }
        return false
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
    private fun runIntegrityCheck(): Boolean {
        val integer = "int"
        val text = "text"
        val cascade = "CASCADE"
        val setNull = "SET NULL"
        //Run integrity checks to see if the current database is whole
        return checkColumnProperties(
            TABLE_CAMERAS, KEY_CAMERA_ID, integer, 0,
                primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MAKE, text, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MODEL, text, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MAX_SHUTTER, text, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MIN_SHUTTER, text, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_SERIAL_NO, text, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_SHUTTER_INCREMENTS, integer, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_EXPOSURE_COMP_INCREMENTS, integer, 1) &&
                checkColumnProperties(
                    TABLE_CAMERAS, KEY_LENS_ID, integer, 0,
                    primaryKeyInput = false, autoIncrementInput = false, foreignKeyInput = true,
                    referenceTableNameInput = TABLE_LENSES, onDeleteActionInput = setNull) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_FORMAT, integer, 0) &&
                checkColumnProperties(
                    TABLE_LENSES, KEY_LENS_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAKE, text, 1) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MODEL, text, 1) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAX_APERTURE, text, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MIN_APERTURE, text, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAX_FOCAL_LENGTH, integer, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MIN_FOCAL_LENGTH, integer, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_SERIAL_NO, text, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_APERTURE_INCREMENTS, integer, 1) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_CUSTOM_APERTURE_VALUES, text, 0) &&
                checkColumnProperties(
                    TABLE_FILTERS, KEY_FILTER_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_MAKE, text, 1) &&
                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_MODEL, text, 1) &&
                checkColumnProperties(
                    TABLE_LINK_CAMERA_LENS, KEY_CAMERA_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_CAMERAS, onDeleteActionInput = cascade) &&
                checkColumnProperties(
                    TABLE_LINK_CAMERA_LENS, KEY_LENS_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_LENSES, onDeleteActionInput = cascade) &&
                checkColumnProperties(
                    TABLE_LINK_LENS_FILTER, KEY_LENS_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_LENSES, onDeleteActionInput = cascade) &&
                checkColumnProperties(
                    TABLE_LINK_LENS_FILTER, KEY_FILTER_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_FILTERS, onDeleteActionInput = cascade) &&
                checkColumnProperties(
                    TABLE_LINK_FRAME_FILTER, KEY_FRAME_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_FRAMES, onDeleteActionInput = cascade) &&
                checkColumnProperties(
                    TABLE_LINK_FRAME_FILTER, KEY_FILTER_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_FILTERS, onDeleteActionInput = cascade) &&
                checkColumnProperties(
                    TABLE_ROLLS, KEY_ROLL_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLLNAME, text, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_DATE, text, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_NOTE, text, 0) &&
                checkColumnProperties(
                    TABLE_ROLLS, KEY_CAMERA_ID, integer, 0,
                        primaryKeyInput = false, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_CAMERAS, onDeleteActionInput = setNull) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ISO, integer, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_PUSH, text, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_FORMAT, integer, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ARCHIVED, integer, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_FILM_STOCK_ID, integer, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_UNLOADED, text, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_DEVELOPED, text, 0) &&
                checkColumnProperties(
                    TABLE_FRAMES, KEY_FRAME_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(
                    TABLE_FRAMES, KEY_ROLL_ID, integer, 1,
                        primaryKeyInput = false, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_ROLLS, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_FRAMES, KEY_COUNT, integer, 1) &&
                checkColumnProperties(TABLE_FRAMES, KEY_DATE, text, 1) &&
                checkColumnProperties(
                    TABLE_FRAMES, KEY_LENS_ID, integer, 0,
                        primaryKeyInput = false, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_LENSES, onDeleteActionInput = setNull) &&
                checkColumnProperties(TABLE_FRAMES, KEY_SHUTTER, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_APERTURE, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FRAME_NOTE, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_LOCATION, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FOCAL_LENGTH, integer, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_EXPOSURE_COMP, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_NO_OF_EXPOSURES, integer, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FLASH_USED, integer, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FLASH_POWER, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FLASH_COMP, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FRAME_SIZE, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_METERING_MODE, integer, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FORMATTED_ADDRESS, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_PICTURE_FILENAME, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_LIGHT_SOURCE, integer, 0) &&
                checkColumnProperties(
                    TABLE_FILM_STOCKS, KEY_FILM_STOCK_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_STOCK_NAME, text, 1) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_MANUFACTURER_NAME, text, 1) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_ISO, integer, 0) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_TYPE, integer, 0) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_PROCESS, integer, 0) &&
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_IS_PREADDED, integer, 0)
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
    private fun checkColumnProperties(tableNameInput: String, columnNameInput: String, columnTypeInput: String,
                                      notNullInput: Int, primaryKeyInput: Boolean = false, autoIncrementInput: Boolean = false,
                                      foreignKeyInput: Boolean = false, referenceTableNameInput: String? = null,
                                      onDeleteActionInput: String? = null): Boolean {
        val db = this.readableDatabase

        //Check for possible autoincrement
        if (autoIncrementInput) {
            // We can check that the table is autoincrement from the master tables.
            // Column 'sql' is the query with which the table was created.
            // If a table is autoincrement, then it can only have one primary key.
            // If the primary key matches, then also the autoincrement column is correct.
            // The primary key will be checked later in this method.
            val incrementQuery = "SELECT * FROM sqlite_master WHERE type = 'table' AND name = '$tableNameInput' AND sql LIKE '%AUTOINCREMENT%'"
            val incrementCursor = db.rawQuery(incrementQuery, null)
            if (!incrementCursor.moveToFirst()) {
                //No rows were returned. The table has no autoincrement. Integrity check fails.
                incrementCursor.close()
                return false
            }
            incrementCursor.close()
        }

        //Check for possible foreign key reference
        if (foreignKeyInput) {
            // We can check that the column is a foreign key column using one of the SQLite pragma statements.
            val foreignKeyQuery = "PRAGMA FOREIGN_KEY_LIST('$tableNameInput')"
            val foreignKeyCursor = db.rawQuery(foreignKeyQuery, null)
            var foreignKeyFound = false
            //Iterate through the tables foreign key columns and get the properties.
            while (foreignKeyCursor.moveToNext()) {
                val table = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("table"))
                val from = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("from"))
                val onDelete = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("on_delete"))
                val to = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("to"))
                //If the table, from-column and on-delete actions match to those defined
                //by the parameters, the foreign key is correct. The to-column value
                //should be null, because during table creation we have used the shorthand form
                //to reference the parent table's primary key.
                if (table == referenceTableNameInput && from == columnNameInput
                        && onDelete.equals(onDeleteActionInput, ignoreCase = true) && to == null) {
                    foreignKeyFound = true
                    break
                }
            }
            foreignKeyCursor.close()
            //If foreign key was not defined, integrity check fails -> return false.
            if (!foreignKeyFound) return false
        }
        val query = "PRAGMA TABLE_INFO('$tableNameInput');"
        val cursor = db.rawQuery(query, null)

        //Iterate the result rows...
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            // ...until the name checks.
            if (columnName == columnNameInput) {
                val columnType = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull"))
                //If the column is defined as primary key, the pk value is 1.
                val primaryKey = cursor.getInt(cursor.getColumnIndexOrThrow("pk")) > 0
                cursor.close()

                //Check that the attributes are correct and return the result
                return columnType.startsWith(columnTypeInput, ignoreCase = true) && //type can be int or integer
                        notNull == notNullInput && primaryKey == primaryKeyInput
            }
        }
        //We get here if no matching column names were found
        cursor.close()
        return false
    }

    /**
     * Populate films from resource arrays to the database
     */
    private fun populateFilmStocks(db: SQLiteDatabase) {
        val filmStocks = context.resources.getStringArray(R.array.FilmStocks)
        var counter = 0
        for (s in filmStocks) {
            try {
                val components = s.split(",")
                val filmStock = FilmStock()
                filmStock.make = components[0]
                filmStock.model = components[1]
                filmStock.iso = components[2].toInt()
                filmStock.type = FilmType.from(components[3].toInt())
                filmStock.process = FilmProcess.from(components[4].toInt())
                filmStock.isPreadded = true
                val values = FilmStockRepository.buildFilmStockContentValues(filmStock)

                // Insert film stocks if they do not already exist.
                val cursor = db.query(
                    TABLE_FILM_STOCKS, null,
                        "$KEY_FILM_MANUFACTURER_NAME=? AND $KEY_FILM_STOCK_NAME=?",
                        arrayOf(filmStock.make, filmStock.model), null, null, null)
                if (cursor.moveToFirst()) {
                    cursor.close()
                } else {
                    db.insert(TABLE_FILM_STOCKS, null, values)
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                counter++
            } catch (e: NumberFormatException) {
                counter++
            }
        }
        if (counter > 0) {
            Toast.makeText(context, R.string.ErrorAddingFilmStocks, Toast.LENGTH_LONG).show()
        }
    }
}