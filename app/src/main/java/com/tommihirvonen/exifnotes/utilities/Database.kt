package com.tommihirvonen.exifnotes.utilities

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.*
import java.io.File
import java.io.IOException

val Context.database: Database get() = Database.getInstance(applicationContext)

val Fragment.database: Database get() = Database.getInstance(requireContext().applicationContext)

fun <T> Cursor.map(transform: (Cursor) -> T): List<T> =
        generateSequence { if (this.moveToNext()) this else null }.map { transform(it) }.toList()

fun <T> Cursor.withFirstOrNull(transform: (Cursor) -> T): T? =
        if (this.moveToFirst()) transform(this) else null

fun <T> Cursor.withFirstOrDefault(default: T, transform: (Cursor) -> T): T =
        if (this.moveToFirst()) transform(this) else default

/**
 * FilmDbHelper is the SQL database class that holds all the information
 * the user stores in the app. This class provides all necessary CRUD operations as well as
 * export and import functionality.
 */
class Database private constructor(private val context: Context)
    : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

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
    }

    // ******************** CRUD operations for the frames table ********************
    /**
     * Adds a new frame to the database.
     * @param frame the new frame to be added to the database
     */
    fun addFrame(frame: Frame): Boolean {
        val values = buildFrameContentValues(frame)
        val rowId = writableDatabase.insert(TABLE_FRAMES, null, values)
        frame.id = rowId
        // Add the filter links, if the frame was inserted successfully.
        return if (rowId != -1L) {
            frame.filters.forEach { filter -> addFrameFilterLink(frame, filter) }
            true
        } else {
            false
        }
    }

    /**
     * Gets all the frames from a specified roll.
     * @param roll Roll object whose frames should be fetched
     * @return an array of Frames
     */
    fun getAllFramesFromRoll(roll: Roll): List<Frame> {
        val cursor = readableDatabase.query(TABLE_FRAMES, null,
                "$KEY_ROLL_ID=?", arrayOf(roll.id.toString()), null, null, KEY_COUNT)
        return cursor.map { getFrameFromCursor(it, roll) }.also { cursor.close() }
    }

    /**
     * Updates the information of a frame.
     * @param frame the frame to be updated.
     */
    fun updateFrame(frame: Frame): Int {
        val contentValues = buildFrameContentValues(frame)
        val rows = writableDatabase.update(TABLE_FRAMES, contentValues, "$KEY_FRAME_ID=?", arrayOf(frame.id.toString()))
        deleteAllFrameFilterLinks(frame)
        frame.filters.forEach { filter -> addFrameFilterLink(frame, filter) }
        return rows
    }

    /**
     * Deletes a frame from the database.
     * @param frame the frame to be deleted
     */
    fun deleteFrame(frame: Frame): Int =
            writableDatabase.delete(TABLE_FRAMES, "$KEY_FRAME_ID = ?", arrayOf(frame.id.toString()))

    /**
     * Gets all complementary picture filenames from the frames table.
     * @return List of all complementary picture filenames
     */
    val allComplementaryPictureFilenames: List<String> get() {
        val cursor = readableDatabase.query(TABLE_FRAMES, arrayOf(KEY_PICTURE_FILENAME),
                "$KEY_PICTURE_FILENAME IS NOT NULL", null, null, null, null)
        return cursor.map { it.getString(it.getColumnIndexOrThrow(KEY_PICTURE_FILENAME)) }.also { cursor.close() }
    }

    // ******************** CRUD operations for the lenses table ********************
    /**
     * Adds a new lens to the database.
     * @param lens the lens to be added to the database
     */
    fun addLens(lens: Lens): Long {
        val id = writableDatabase.insert(TABLE_LENSES, null, buildLensContentValues(lens))
        lens.id = id
        return id
    }

    /**
     * Gets a lens corresponding to the id.
     * @param lens_id the id of the lens
     * @return a Lens corresponding to the id
     */
    private fun getLens(lens_id: Long): Lens? {
        val cursor = readableDatabase.query(TABLE_LENSES, null,
                "$KEY_LENS_ID=?", arrayOf(lens_id.toString()), null, null, null)
        return cursor.withFirstOrNull { getLensFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Get lenses from the database.
     */
    fun getLenses(includeFixedLenses: Boolean = false): List<Lens> {
        val selection =
            if (includeFixedLenses) null
            else "$KEY_LENS_ID not in (select $KEY_LENS_ID from $TABLE_CAMERAS where $KEY_LENS_ID is not null)"
        val cursor = readableDatabase.query(TABLE_LENSES, null, selection, null,
                null, null, "$KEY_LENS_MAKE collate nocase,$KEY_LENS_MODEL collate nocase")
        return cursor.map { getLensFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Deletes Lens from the database.
     * @param lens the Lens to be deleted
     */
    fun deleteLens(lens: Lens): Int =
            writableDatabase.delete(TABLE_LENSES, "$KEY_LENS_ID = ?", arrayOf(lens.id.toString()))

    /**
     * Checks if the lens is being used in some frame.
     * @param lens Lens to be checked
     * @return true if the lens is in use, false if not
     */
    fun isLensInUse(lens: Lens): Boolean {
        val cursor = readableDatabase.query(TABLE_FRAMES, arrayOf(KEY_LENS_ID),
                "$KEY_LENS_ID=?", arrayOf(lens.id.toString()), null, null, null)
        return cursor.moveToFirst().also { cursor.close() }
    }

    /**
     * Updates the information of a lens
     * @param lens the Lens to be updated
     */
    fun updateLens(lens: Lens): Int {
        val contentValues = buildLensContentValues(lens)
        return writableDatabase.update(TABLE_LENSES, contentValues, "$KEY_LENS_ID=?", arrayOf(lens.id.toString()))
    }

    // ******************** CRUD operations for the cameras table ********************
    /**
     * Adds a new camera to the database.
     * @param camera the camera to be added to the database
     */
    fun addCamera(camera: Camera): Long {
        val id = writableDatabase.insert(TABLE_CAMERAS, null, buildCameraContentValues(camera))
        camera.id = id
        return id
    }

    /**
     * Gets the Camera corresponding to the camera id
     * @param camera_id the id of the Camera
     * @return the Camera corresponding to the given id
     */
    private fun getCamera(camera_id: Long): Camera? {
        val cursor = readableDatabase.query(TABLE_CAMERAS, null,
                "$KEY_CAMERA_ID=?", arrayOf(camera_id.toString()), null, null, null)
        return cursor.withFirstOrNull { getCameraFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Get a fixed-lens camera by its lens id.
     */
    fun getCameraByLensId(lensId: Long): Camera? {
        val cursor = readableDatabase.query(TABLE_CAMERAS, null, "$KEY_LENS_ID = ?",
            arrayOf(lensId.toString()), null, null, null, "1")
        return cursor.withFirstOrNull { getCameraFromCursor(it) }.also { cursor.close() }
    }

    fun getCameras(includeFixedLensCameras: Boolean = true, onlyFixedLensCameras: Boolean = false): List<Camera> {
        val selection = when {
            onlyFixedLensCameras -> "$KEY_LENS_ID is not null"
            includeFixedLensCameras -> null
            else -> "$KEY_LENS_ID is null"
        }
        val cursor = readableDatabase.query(TABLE_CAMERAS, null, selection, null,
            null, null, "$KEY_CAMERA_MAKE collate nocase,$KEY_CAMERA_MODEL collate nocase")
        return cursor.map { getCameraFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Deletes the specified camera from the database
     * @param camera the camera to be deleted
     */
    fun deleteCamera(camera: Camera): Int {
        // In case of fixed lens cameras, also delete the lens from database.
        camera.lens?.let {
            writableDatabase.delete(TABLE_LENSES, "$KEY_LENS_ID = ?", arrayOf(it.id.toString()))
        }
        return writableDatabase.delete(TABLE_CAMERAS, "$KEY_CAMERA_ID = ?", arrayOf(camera.id.toString()))
    }

    /**
     * Checks if a camera is being used in some roll.
     * @param camera the camera to be checked
     * @return true if the camera is in use, false if not
     */
    fun isCameraBeingUsed(camera: Camera): Boolean {
        val cursor = readableDatabase.query(TABLE_ROLLS, arrayOf(KEY_CAMERA_ID),
                "$KEY_CAMERA_ID=?", arrayOf(camera.id.toString()), null, null, null)
        return cursor.moveToFirst().also { cursor.close() }
    }

    /**
     * Updates the information of the specified camera.
     * @param camera the camera to be updated
     */
    fun updateCamera(camera: Camera): Int {
        val contentValues = buildCameraContentValues(camera)
        return writableDatabase.update(TABLE_CAMERAS, contentValues, "$KEY_CAMERA_ID=?", arrayOf(camera.id.toString()))
    }

    // ******************** CRUD operations for the camera-lens link table ********************
    /**
     * Adds a mountable combination of camera and lens to the database.
     * @param camera the camera that can be mounted with the lens
     * @param lens the lens that can be mounted with the camera
     */
    fun addCameraLensLink(camera: Camera, lens: Lens) {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        val query = ("INSERT INTO " + TABLE_LINK_CAMERA_LENS + "(" + KEY_CAMERA_ID + "," + KEY_LENS_ID
                + ") SELECT " + camera.id + ", " + lens.id
                + " WHERE NOT EXISTS(SELECT 1 FROM " + TABLE_LINK_CAMERA_LENS + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.id + " AND " + KEY_LENS_ID + "=" + lens.id + ");")
        writableDatabase.execSQL(query)
    }

    /**
     * Deletes a mountable combination from the database
     * @param camera the camera that can be mounted with the lens
     * @param lens the lens that can be mounted with the camera
     */
    fun deleteCameraLensLink(camera: Camera, lens: Lens) =
            writableDatabase.delete(TABLE_LINK_CAMERA_LENS,
                    "$KEY_CAMERA_ID = ? AND $KEY_LENS_ID = ?", arrayOf(camera.id.toString(), lens.id.toString()))

    /**
     * Gets all the lenses that can be mounted to the specified camera
     * @param camera the camera whose lenses we want to get
     * @return a List of all linked lenses
     */
    fun getLinkedLenses(camera: Camera): List<Lens> {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        val query = ("SELECT * FROM " + TABLE_LENSES + " WHERE " + KEY_LENS_ID + " IN "
                + "(" + "SELECT " + KEY_LENS_ID + " FROM " + TABLE_LINK_CAMERA_LENS + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.id + ") ORDER BY " + KEY_LENS_MAKE)
        val cursor = readableDatabase.rawQuery(query, null)
        return cursor.map { getLensFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Gets all the cameras that can be mounted to the specified lens
     * @param lens the lens whose cameras we want to get
     * @return a List of all linked cameras
     */
    fun getLinkedCameras(lens: Lens): List<Camera> {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        val query = ("SELECT * FROM " + TABLE_CAMERAS + " WHERE " + KEY_CAMERA_ID + " IN "
                + "(" + "SELECT " + KEY_CAMERA_ID + " FROM " + TABLE_LINK_CAMERA_LENS + " WHERE "
                + KEY_LENS_ID + "=" + lens.id + ") ORDER BY " + KEY_CAMERA_MAKE)
        val cursor = readableDatabase.rawQuery(query, null)
        return cursor.map { getCameraFromCursor(it) }.also { cursor.close() }
    }

    // ******************** CRUD operations for the rolls table ********************
    /**
     * Adds a new roll to the database.
     * @param roll the roll to be added
     */
    fun addRoll(roll: Roll): Long {
        val id = writableDatabase.insert(TABLE_ROLLS, null, buildRollContentValues(roll))
        roll.id = id
        return id
    }

    /**
     * Gets all the rolls in the database
     * @return a List of all the rolls in the database
     */
    fun getRolls(filterMode: RollFilterMode?): List<Roll> {
        val selectionArg: String? = when (filterMode) {
            RollFilterMode.ACTIVE -> "$KEY_ROLL_ARCHIVED=0"
            RollFilterMode.ARCHIVED -> "$KEY_ROLL_ARCHIVED>0"
            RollFilterMode.ALL -> null
            else -> "$KEY_ROLL_ARCHIVED=0"
        }
        val cursor = readableDatabase.query(TABLE_ROLLS, null, selectionArg, null,
                null, null, "$KEY_ROLL_DATE DESC")
        return cursor.map { getRollFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Deletes a roll from the database.
     * @param roll the roll to be deleted from the database
     */
    fun deleteRoll(roll: Roll): Int =
            writableDatabase.delete(TABLE_ROLLS, "$KEY_ROLL_ID = ?", arrayOf(roll.id.toString()))

    /**
     * Updates the specified roll's information
     * @param roll the roll to be updated
     */
    fun updateRoll(roll: Roll): Int {
        val contentValues = buildRollContentValues(roll)
        return writableDatabase.update(TABLE_ROLLS, contentValues, "$KEY_ROLL_ID=?", arrayOf(roll.id.toString()))
    }

    /**
     * Gets the number of frames on a specified roll.
     * @param roll the roll whose frame count we want
     * @return an integer of the the number of frames on that roll
     */
    fun getNumberOfFrames(roll: Roll): Int {
        val cursor = readableDatabase.query(TABLE_FRAMES, arrayOf("COUNT($KEY_FRAME_ID)"),
                "$KEY_ROLL_ID=?", arrayOf(roll.id.toString()), null, null, null)
        return cursor.withFirstOrDefault(0) { it.getInt(0) }.also { cursor.close() }
    }

    // ******************** CRUD operations for the filters table ********************
    /**
     * Adds a new filter to the database.
     * @param filter the filter to be added to the database
     */
    fun addFilter(filter: Filter): Long {
        val id = writableDatabase.insert(TABLE_FILTERS, null, buildFilterContentValues(filter))
        filter.id = id
        return id
    }

    /**
     * Gets all the filters from the database
     * @return a List of all the filters in the database
     */
    val allFilters: List<Filter> get() {
        val cursor = readableDatabase.query(TABLE_FILTERS, null, null, null,
                null, null, "$KEY_FILTER_MAKE collate nocase,$KEY_FILTER_MODEL collate nocase")
        return cursor.map { getFilterFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Deletes the specified filter from the database
     * @param filter the filter to be deleted
     */
    fun deleteFilter(filter: Filter): Int =
            writableDatabase.delete(TABLE_FILTERS, "$KEY_FILTER_ID = ?", arrayOf(filter.id.toString()))

    /**
     * Checks if a filter is being used in some roll.
     * @param filter the filter to be checked
     * @return true if the filter is in use, false if not
     */
    fun isFilterBeingUsed(filter: Filter): Boolean {
        val cursor = readableDatabase.query(TABLE_LINK_FRAME_FILTER, arrayOf(KEY_FILTER_ID),
                "$KEY_FILTER_ID=?", arrayOf(filter.id.toString()), null, null, null)
        return cursor.moveToFirst().also { cursor.close() }
    }

    /**
     * Updates the information of the specified filter.
     * @param filter the filter to be updated
     */
    fun updateFilter(filter: Filter): Int {
        val contentValues = buildFilterContentValues(filter)
        return writableDatabase.update(TABLE_FILTERS, contentValues, "$KEY_FILTER_ID=?", arrayOf(filter.id.toString()))
    }

    // ******************** CRUD operations for the lens-filter link table ********************
    /**
     * Adds a mountable combination of filter and lens to the database.
     * @param filter the filter that can be mounted with the lens
     * @param lens the lens that can be mounted with the filter
     */
    fun addLensFilterLink(filter: Filter, lens: Lens) {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        val query = ("INSERT INTO " + TABLE_LINK_LENS_FILTER + "(" + KEY_FILTER_ID + "," + KEY_LENS_ID
                + ") SELECT " + filter.id + ", " + lens.id
                + " WHERE NOT EXISTS(SELECT 1 FROM " + TABLE_LINK_LENS_FILTER + " WHERE "
                + KEY_FILTER_ID + "=" + filter.id + " AND " + KEY_LENS_ID + "=" + lens.id + ");")
        writableDatabase.execSQL(query)
    }

    /**
     * Deletes a mountable combination from the database
     * @param filter the filter that can be mounted with the lens
     * @param lens the lens that can be mounted with the filter
     */
    fun deleteLensFilterLink(filter: Filter, lens: Lens): Int =
            writableDatabase.delete(TABLE_LINK_LENS_FILTER,
                    "$KEY_FILTER_ID = ? AND $KEY_LENS_ID = ?", arrayOf(filter.id.toString(), lens.id.toString()))

    /**
     * Gets all the lenses that can be mounted to the specified filter
     * @param filter the filter whose lenses we want to get
     * @return a List of all linked lenses
     */
    fun getLinkedLenses(filter: Filter): List<Lens> {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        val query = ("SELECT * FROM " + TABLE_LENSES + " WHERE " + KEY_LENS_ID + " IN "
                + "(" + "SELECT " + KEY_LENS_ID + " FROM " + TABLE_LINK_LENS_FILTER + " WHERE "
                + KEY_FILTER_ID + "=" + filter.id + ") ORDER BY " + KEY_LENS_MAKE)
        val cursor = readableDatabase.rawQuery(query, null)
        return cursor.map { getLensFromCursor(it) }.also { cursor.close() }
    }

    /**
     * Gets all the filters that can be mounted to the specified lens
     * @param lens the lens whose filters we want to get
     * @return a List of all linked filters
     */
    fun getLinkedFilters(lens: Lens): List<Filter> {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        val query = ("SELECT * FROM " + TABLE_FILTERS + " WHERE " + KEY_FILTER_ID + " IN "
                + "(" + "SELECT " + KEY_FILTER_ID + " FROM " + TABLE_LINK_LENS_FILTER + " WHERE "
                + KEY_LENS_ID + "=" + lens.id + ") ORDER BY " + KEY_FILTER_MAKE)
        val cursor = readableDatabase.rawQuery(query, null)
        return cursor.map { getFilterFromCursor(it) }.also { cursor.close() }
    }

    // ******************** CRUD operations for the frame-filter link table ********************
    /**
     * Adds a new link between a frame and a filter object
     *
     * @param frame frame that is linked to the filter
     * @param filter filter that is linked to the frame
     */
    private fun addFrameFilterLink(frame: Frame, filter: Filter) {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        val query = ("insert into " + TABLE_LINK_FRAME_FILTER + " ("
                + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ") "
                + "select " + frame.id + ", " + filter.id + " "
                + "where not exists (select * from " + TABLE_LINK_FRAME_FILTER + " "
                + "where " + KEY_FRAME_ID + " = " + frame.id + " and "
                + KEY_FILTER_ID + " = " + filter.id + ");")
        writableDatabase.execSQL(query)
    }

    /**
     * Deletes all links between a single frame and all its linked filters.
     *
     * @param frame Frame object whose filter links should be deleted
     */
    private fun deleteAllFrameFilterLinks(frame: Frame): Int =
            writableDatabase.delete(TABLE_LINK_FRAME_FILTER, "$KEY_FRAME_ID = ?", arrayOf(frame.id.toString()))

    /**
     * Gets all filter objects that are linked to a specific Frame object
     *
     * @param frame the Frame object whose linked filters we want to get
     * @return List object containing all Filter objects linked to the specified Frame object
     */
    private fun getLinkedFilters(frame: Frame): List<Filter> {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection
        val query = ("select * from " + TABLE_FILTERS + " where " + KEY_FILTER_ID + " in "
                + "(select " + KEY_FILTER_ID + " from " + TABLE_LINK_FRAME_FILTER + " where "
                + KEY_FRAME_ID + " = " + frame.id + ") order by " + KEY_FILTER_MAKE)
        val cursor = readableDatabase.rawQuery(query, null)
        return cursor.map { getFilterFromCursor(it) }.also { cursor.close() }
    }

    // ******************** CRUD operations for the film stock table ********************

    fun addFilmStock(filmStock: FilmStock): Long {
        val id = writableDatabase.insert(TABLE_FILM_STOCKS, null, buildFilmStockContentValues(filmStock))
        filmStock.id = id
        return id
    }

    private fun getFilmStock(filmStockId: Long): FilmStock? {
        val cursor = readableDatabase.query(TABLE_FILM_STOCKS, null,
                "$KEY_FILM_STOCK_ID=?", arrayOf(filmStockId.toString()), null, null, null)
        return cursor.withFirstOrNull { getFilmStockFromCursor(cursor, FilmStock()) }.also { cursor.close() }
    }

    val allFilmStocks: List<FilmStock> get() {
        val cursor = readableDatabase.query(TABLE_FILM_STOCKS, null, null,
                null, null, null,
                "$KEY_FILM_MANUFACTURER_NAME collate nocase,$KEY_FILM_STOCK_NAME collate nocase")
        return cursor.map { getFilmStockFromCursor(it, FilmStock()) }.also { cursor.close() }
    }

    val allFilmManufacturers: List<String> get() {
        val cursor = readableDatabase.query(true, TABLE_FILM_STOCKS, arrayOf(KEY_FILM_MANUFACTURER_NAME),
                null, null, null, null, "$KEY_FILM_MANUFACTURER_NAME collate nocase", null)
        return cursor.map { it.getString(it.getColumnIndexOrThrow(KEY_FILM_MANUFACTURER_NAME)) }.also { cursor.close() }
    }

    fun isFilmStockBeingUsed(filmStock: FilmStock): Boolean {
        val cursor = readableDatabase.query(TABLE_ROLLS, arrayOf(KEY_FILM_STOCK_ID),
                "$KEY_FILM_STOCK_ID=?", arrayOf(filmStock.id.toString()), null, null, null)
        return cursor.moveToFirst().also { cursor.close() }
    }

    fun deleteFilmStock(filmStock: FilmStock) =
            writableDatabase.delete(TABLE_FILM_STOCKS, "$KEY_FILM_STOCK_ID=?", arrayOf(filmStock.id.toString()))

    fun updateFilmStock(filmStock: FilmStock) {
        val contentValues = buildFilmStockContentValues(filmStock)
        writableDatabase.update(TABLE_FILM_STOCKS, contentValues, "$KEY_FILM_STOCK_ID=?", arrayOf(filmStock.id.toString()))
    }

    //*********************** METHODS TO GET OBJECTS FROM CURSOR **********************************

    /**
     * Returns a Frame object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Frame object
     * @return Frame object generated from cursor
     */
    private fun getFrameFromCursor(cursor: Cursor, roll: Roll): Frame {
        val frame = Frame(roll)
        return getFrameFromCursor(cursor, frame)
    }

    /**
     * Sets the attributes of a Frame object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param frame the Frame whose attributes should be set
     * @return reference to the Frame object given as the parameter
     */
    private fun getFrameFromCursor(cursor: Cursor, frame: Frame): Frame {
        frame.id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FRAME_ID))
        frame.count = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COUNT))
        val date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE))
        if (date != null) frame.date = DateTime(date)
        val lensId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LENS_ID))
        if (lensId > 0) frame.lens = getLens(lensId)
        frame.shutter = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHUTTER))
        frame.aperture = cursor.getString(cursor.getColumnIndexOrThrow(KEY_APERTURE))
        frame.note = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FRAME_NOTE))
        val location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LOCATION))
        if (location != null) frame.location = Location(location)
        frame.focalLength = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FOCAL_LENGTH))
        frame.exposureComp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXPOSURE_COMP))
        frame.noOfExposures = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NO_OF_EXPOSURES))
        val flashUsed = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FLASH_USED))
        frame.flashUsed = flashUsed > 0
        frame.flashPower = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FLASH_POWER))
        frame.flashComp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FLASH_COMP))
        frame.meteringMode = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_METERING_MODE))
        frame.formattedAddress = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FORMATTED_ADDRESS))
        frame.pictureFilename = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PICTURE_FILENAME))
        frame.lightSource = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LIGHT_SOURCE))
        frame.filters = getLinkedFilters(frame).toMutableList()
        return frame
    }

    /**
     * Returns a Roll object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Roll object
     * @return Roll object generated from cursor
     */
    private fun getRollFromCursor(cursor: Cursor): Roll {
        val roll = Roll()
        return getRollFromCursor(cursor, roll)
    }

    /**
     * Sets the attributes of a Roll object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param roll the Roll whose attributes should be set
     * @return reference to the Roll object given as the parameter
     */
    private fun getRollFromCursor(cursor: Cursor, roll: Roll): Roll {
        roll.id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROLL_ID))
        roll.name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLLNAME))
        val date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLL_DATE))
        if (date != null) roll.date = DateTime(date)
        roll.note = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLL_NOTE))
        val cameraId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CAMERA_ID))
        if (cameraId > 0) roll.camera = getCamera(cameraId)
        roll.iso = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ROLL_ISO))
        roll.pushPull = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLL_PUSH))
        roll.format = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ROLL_FORMAT))
        val archived = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ROLL_ARCHIVED))
        roll.archived = archived > 0
        val filmStockId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FILM_STOCK_ID)).toLong()
        if (filmStockId > 0) roll.filmStock = getFilmStock(filmStockId)
        val unloaded = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLL_UNLOADED))
        if (unloaded != null) roll.unloaded = DateTime(unloaded)
        val developed = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLL_DEVELOPED))
        if (developed != null) roll.developed = DateTime(developed)
        return roll
    }

    /**
     * Returns a Lens object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Lens object
     * @return Lens object generated from cursor
     */
    private fun getLensFromCursor(cursor: Cursor): Lens {
        val lens = Lens()
        return getLensFromCursor(cursor, lens)
    }

    /**
     * Sets the attributes of a Lens object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param lens the Lens whose attributes should be set
     * @return reference to the Lens object given as the parameter
     */
    private fun getLensFromCursor(cursor: Cursor, lens: Lens): Lens {
        lens.id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LENS_ID))
        lens.make = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LENS_MAKE))
        lens.model = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LENS_MODEL))
        lens.serialNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LENS_SERIAL_NO))
        lens.minAperture = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LENS_MIN_APERTURE))
        lens.maxAperture = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LENS_MAX_APERTURE))
        lens.minFocalLength = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LENS_MIN_FOCAL_LENGTH))
        lens.maxFocalLength = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LENS_MAX_FOCAL_LENGTH))
        val incrementIndex = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LENS_APERTURE_INCREMENTS))
        lens.apertureIncrements = Increment.from(incrementIndex)
        return lens
    }

    /**
     * Returns a Camera object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Camera object
     * @return Camera object generated from cursor
     */
    private fun getCameraFromCursor(cursor: Cursor): Camera {
        val camera = Camera()
        return getCameraFromCursor(cursor, camera)
    }

    /**
     * Sets the attributes of a Camera object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param camera the Camera whose attributes should be set
     * @return reference to the Camera object given as the parameter
     */
    private fun getCameraFromCursor(cursor: Cursor, camera: Camera): Camera {
        camera.id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CAMERA_ID))
        camera.make = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAMERA_MAKE))
        camera.model = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAMERA_MODEL))
        camera.serialNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAMERA_SERIAL_NO))
        camera.minShutter = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAMERA_MIN_SHUTTER))
        camera.maxShutter = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAMERA_MAX_SHUTTER))
        val shutterIncrementIndex = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CAMERA_SHUTTER_INCREMENTS))
        camera.shutterIncrements = Increment.from(shutterIncrementIndex)
        val compIncrementIndex = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CAMERA_EXPOSURE_COMP_INCREMENTS))
        camera.exposureCompIncrements = PartialIncrement.from(compIncrementIndex)
        val lensId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LENS_ID))
        if (lensId > 0) camera.lens = getLens(lensId)
        return camera
    }

    /**
     * Returns a Filter object generated from a Cursor object.
     *
     * @param cursor Cursor object containing the attributes for a Filter object
     * @return Filter object generated from cursor
     */
    private fun getFilterFromCursor(cursor: Cursor): Filter {
        val filter = Filter()
        return getFilterFromCursor(cursor, filter)
    }

    /**
     * Sets the attributes of a Filter object using a Cursor object.
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param filter the Filter whose attributes should be set
     * @return reference to the Filter object given as the parameter
     */
    private fun getFilterFromCursor(cursor: Cursor, filter: Filter): Filter {
        filter.id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FILTER_ID))
        filter.make = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FILTER_MAKE))
        filter.model = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FILTER_MODEL))
        return filter
    }

    /**
     * Sets the attributes of a FilmStock object using a Cursor object
     *
     * @param cursor Cursor object which should be used to get the attributes
     * @param filmStock the FilmStock whose attributes should be set
     * @return reference to the FilmStock object given as the parameter
     */
    private fun getFilmStockFromCursor(cursor: Cursor, filmStock: FilmStock): FilmStock {
        filmStock.id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FILM_STOCK_ID))
        filmStock.make = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FILM_MANUFACTURER_NAME))
        filmStock.model = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FILM_STOCK_NAME))
        filmStock.iso = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FILM_ISO))
        filmStock.type = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FILM_TYPE))
        filmStock.process = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FILM_PROCESS))
        val preadded = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FILM_IS_PREADDED))
        filmStock.isPreadded = preadded > 0
        return filmStock
    }

    //*********************** METHODS TO BUILD CONTENT VALUES **********************************
    /**
     * Builds ContentValues container from a Frame object.
     *
     * @param frame Frame object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Frame object.
     */
    private fun buildFrameContentValues(frame: Frame): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_ROLL_ID, frame.roll.id)
        contentValues.put(KEY_COUNT, frame.count)
        contentValues.put(KEY_DATE, if (frame.date != null) frame.date.toString() else null)

        val lens = frame.lens
        if (lens != null) contentValues.put(KEY_LENS_ID, lens.id)
        else contentValues.putNull(KEY_LENS_ID)

        contentValues.put(KEY_SHUTTER, frame.shutter)
        contentValues.put(KEY_APERTURE, frame.aperture)
        contentValues.put(KEY_FRAME_NOTE, frame.note)
        contentValues.put(KEY_LOCATION, if (frame.location != null) frame.location.toString() else null)
        contentValues.put(KEY_FOCAL_LENGTH, frame.focalLength)
        contentValues.put(KEY_EXPOSURE_COMP, frame.exposureComp)
        contentValues.put(KEY_NO_OF_EXPOSURES, frame.noOfExposures)
        contentValues.put(KEY_FLASH_USED, frame.flashUsed)
        contentValues.put(KEY_FLASH_POWER, frame.flashPower)
        contentValues.put(KEY_FLASH_COMP, frame.flashComp)
        contentValues.put(KEY_METERING_MODE, frame.meteringMode)
        contentValues.put(KEY_FORMATTED_ADDRESS, frame.formattedAddress)
        contentValues.put(KEY_PICTURE_FILENAME, frame.pictureFilename)
        contentValues.put(KEY_LIGHT_SOURCE, frame.lightSource)
        return contentValues
    }

    /**
     * Builds ContentValues container from a Lens object.
     *
     * @param lens Lens object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the lens object.
     */
    private fun buildLensContentValues(lens: Lens): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_LENS_MAKE, lens.make)
        contentValues.put(KEY_LENS_MODEL, lens.model)
        contentValues.put(KEY_LENS_SERIAL_NO, lens.serialNumber)
        contentValues.put(KEY_LENS_MIN_APERTURE, lens.minAperture)
        contentValues.put(KEY_LENS_MAX_APERTURE, lens.maxAperture)
        contentValues.put(KEY_LENS_MIN_FOCAL_LENGTH, lens.minFocalLength)
        contentValues.put(KEY_LENS_MAX_FOCAL_LENGTH, lens.maxFocalLength)
        contentValues.put(KEY_LENS_APERTURE_INCREMENTS, lens.apertureIncrements.ordinal)
        return contentValues
    }

    /**
     * Builds ContentValues container from a Camera object.
     *
     * @param camera Camera object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Camera object.
     */
    private fun buildCameraContentValues(camera: Camera): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_CAMERA_MAKE, camera.make)
        contentValues.put(KEY_CAMERA_MODEL, camera.model)
        contentValues.put(KEY_CAMERA_SERIAL_NO, camera.serialNumber)
        contentValues.put(KEY_CAMERA_MIN_SHUTTER, camera.minShutter)
        contentValues.put(KEY_CAMERA_MAX_SHUTTER, camera.maxShutter)
        contentValues.put(KEY_CAMERA_SHUTTER_INCREMENTS, camera.shutterIncrements.ordinal)
        contentValues.put(KEY_CAMERA_EXPOSURE_COMP_INCREMENTS, camera.exposureCompIncrements.ordinal)

        val lens = camera.lens
        if (lens != null) contentValues.put(KEY_LENS_ID, lens.id)
        else contentValues.putNull(KEY_LENS_ID)

        return contentValues
    }

    /**
     * Builds ContentValues container from a Roll object.
     *
     * @param roll Roll object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Roll object.
     */
    private fun buildRollContentValues(roll: Roll): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_ROLLNAME, roll.name)
        contentValues.put(KEY_ROLL_DATE, if (roll.date != null) roll.date.toString() else null)
        contentValues.put(KEY_ROLL_NOTE, roll.note)

        val camera = roll.camera
        if (camera != null) contentValues.put(KEY_CAMERA_ID, camera.id)
        else contentValues.putNull(KEY_CAMERA_ID)

        contentValues.put(KEY_ROLL_ISO, roll.iso)
        contentValues.put(KEY_ROLL_PUSH, roll.pushPull)
        contentValues.put(KEY_ROLL_FORMAT, roll.format)
        contentValues.put(KEY_ROLL_ARCHIVED, roll.archived)

        val filmStock = roll.filmStock
        if (filmStock != null) contentValues.put(KEY_FILM_STOCK_ID, filmStock.id)
        else contentValues.putNull(KEY_FILM_STOCK_ID)

        contentValues.put(KEY_ROLL_UNLOADED, if (roll.unloaded != null) roll.unloaded.toString() else null)
        contentValues.put(KEY_ROLL_DEVELOPED, if (roll.developed != null) roll.developed.toString() else null)
        return contentValues
    }

    /**
     * Builds ContentValues container from a Filter object.
     *
     * @param filter Filter object of which the ContentValues is created.
     * @return ContentValues containing the attributes of the Filter object.
     */
    private fun buildFilterContentValues(filter: Filter): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_FILTER_MAKE, filter.make)
        contentValues.put(KEY_FILTER_MODEL, filter.model)
        return contentValues
    }

    /**
     * Builds ContentValues container from a FilmStock object
     *
     * @param filmStock FilmStock object of which the ContentValues is created
     * @return ContentValues containing the attributes of the FilmStock object
     */
    private fun buildFilmStockContentValues(filmStock: FilmStock): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_FILM_MANUFACTURER_NAME, filmStock.make)
        contentValues.put(KEY_FILM_STOCK_NAME, filmStock.model)
        contentValues.put(KEY_FILM_ISO, filmStock.iso)
        contentValues.put(KEY_FILM_TYPE, filmStock.type)
        contentValues.put(KEY_FILM_PROCESS, filmStock.process)
        contentValues.put(KEY_FILM_IS_PREADDED, filmStock.isPreadded)
        return contentValues
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
    @Throws(IOException::class)
    fun importDatabase(context: Context, importDatabasePath: String): Boolean {
        val newDb = File(importDatabasePath)
        val oldDbBackup = File(context.getDatabasePath(DATABASE_NAME).absolutePath + "_backup")
        val oldDb = getDatabaseFile(context)
        if (newDb.exists()) {
            close()

            //Backup the old database file in case the new file is corrupted.
            oldDb.copyTo(oldDbBackup, overwrite = true)

            //Replace the old database file with the new one.
            newDb.copyTo(oldDb, overwrite = true)

            // Access the copied database so SQLiteHelper will cache it and mark it as created.
            val success = booleanArrayOf(true)
            try {
                SQLiteDatabase.openDatabase(getDatabaseFile(context).absolutePath, null,
                        SQLiteDatabase.OPEN_READWRITE, {
                    // If the database was corrupt, try to replace with the old backup.
                    try {
                        oldDbBackup.copyTo(oldDb, overwrite = true)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    success[0] = false
                })
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
        return checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_ID, integer, 0,
                primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MAKE, text, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MODEL, text, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MAX_SHUTTER, text, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_MIN_SHUTTER, text, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_SERIAL_NO, text, 0) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_SHUTTER_INCREMENTS, integer, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_CAMERA_EXPOSURE_COMP_INCREMENTS, integer, 1) &&
                checkColumnProperties(TABLE_CAMERAS, KEY_LENS_ID, integer, 0,
                    primaryKeyInput = false, autoIncrementInput = false, foreignKeyInput = true,
                    referenceTableNameInput = TABLE_LENSES, onDeleteActionInput = setNull) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAKE, text, 1) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MODEL, text, 1) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAX_APERTURE, text, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MIN_APERTURE, text, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MAX_FOCAL_LENGTH, integer, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_MIN_FOCAL_LENGTH, integer, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_SERIAL_NO, text, 0) &&
                checkColumnProperties(TABLE_LENSES, KEY_LENS_APERTURE_INCREMENTS, integer, 1) &&
                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_MAKE, text, 1) &&
                checkColumnProperties(TABLE_FILTERS, KEY_FILTER_MODEL, text, 1) &&
                checkColumnProperties(TABLE_LINK_CAMERA_LENS, KEY_CAMERA_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_CAMERAS, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_LINK_CAMERA_LENS, KEY_LENS_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_LENSES, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_LINK_LENS_FILTER, KEY_LENS_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_LENSES, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_LINK_LENS_FILTER, KEY_FILTER_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_FILTERS, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_LINK_FRAME_FILTER, KEY_FRAME_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_FRAMES, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_LINK_FRAME_FILTER, KEY_FILTER_ID, integer, 1,
                        primaryKeyInput = true, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_FILTERS, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLLNAME, text, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_DATE, text, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_NOTE, text, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_CAMERA_ID, integer, 0,
                        primaryKeyInput = false, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_CAMERAS, onDeleteActionInput = setNull) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ISO, integer, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_PUSH, text, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_FORMAT, integer, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_ARCHIVED, integer, 1) &&
                checkColumnProperties(TABLE_ROLLS, KEY_FILM_STOCK_ID, integer, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_UNLOADED, text, 0) &&
                checkColumnProperties(TABLE_ROLLS, KEY_ROLL_DEVELOPED, text, 0) &&
                checkColumnProperties(TABLE_FRAMES, KEY_FRAME_ID, integer, 0,
                        primaryKeyInput = true, autoIncrementInput = true) &&
                checkColumnProperties(TABLE_FRAMES, KEY_ROLL_ID, integer, 1,
                        primaryKeyInput = false, autoIncrementInput = false, foreignKeyInput = true,
                        referenceTableNameInput = TABLE_ROLLS, onDeleteActionInput = cascade) &&
                checkColumnProperties(TABLE_FRAMES, KEY_COUNT, integer, 1) &&
                checkColumnProperties(TABLE_FRAMES, KEY_DATE, text, 1) &&
                checkColumnProperties(TABLE_FRAMES, KEY_LENS_ID, integer, 0,
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
                checkColumnProperties(TABLE_FILM_STOCKS, KEY_FILM_STOCK_ID, integer, 0,
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
                return columnType.startsWith(columnTypeInput) && //type can be int or integer
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
                filmStock.type = components[3].toInt()
                filmStock.process = components[4].toInt()
                filmStock.isPreadded = true
                val values = buildFilmStockContentValues(filmStock)

                // Insert film stocks if they do not already exist.
                val cursor = db.query(TABLE_FILM_STOCKS, null,
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

    companion object {
        /**
         * Store reference to the singleton instance.
         */
        private var instance: Database? = null

        /**
         * Singleton method to get reference to the database instance
         *
         * @param context current context
         * @return reference to the database singleton instance
         */
        @Synchronized
        internal fun getInstance(context: Context): Database {
            // Use the application context, which will ensure that you
            // don't accidentally leak an Activity's context.
            // See this article for more information: http://bit.ly/6LRzfx
            if (instance == null) {
                instance = Database(context.applicationContext)
            }
            // Safe to assert not null because instance is private and getInstance() is synchronized.
            return instance!!
        }

        /**
         * Returns a File object referencing the .db database file used to store this database.
         *
         * @param context the application's context
         * @return File referencing the database file used by this SQLite database
         */
        fun getDatabaseFile(context: Context): File {
            val databasePath = context.getDatabasePath(DATABASE_NAME).absolutePath
            return File(databasePath)
        }

        //=============================================================================================
        //Table names
        private const val TABLE_FRAMES = "frames"
        private const val TABLE_LENSES = "lenses"
        private const val TABLE_ROLLS = "rolls"
        private const val TABLE_CAMERAS = "cameras"
        private const val TABLE_LINK_CAMERA_LENS = "link_camera_lens"

        //Added in database version 14
        private const val TABLE_FILTERS = "filters"
        private const val TABLE_LINK_LENS_FILTER = "link_lens_filter"

        //Added in database version 19
        private const val TABLE_LINK_FRAME_FILTER = "link_frame_filter"

        //Added in database version 20
        private const val TABLE_FILM_STOCKS = "film_stocks"

        //=============================================================================================
        //Column names
        //Frame
        private const val KEY_FRAME_ID = "frame_id"
        private const val KEY_COUNT = "count"
        private const val KEY_DATE = "date"
        private const val KEY_SHUTTER = "shutter"
        private const val KEY_APERTURE = "aperture"
        private const val KEY_FRAME_NOTE = "frame_note"
        private const val KEY_LOCATION = "location"

        //Added in database version 14
        private const val KEY_FOCAL_LENGTH = "focal_length"
        private const val KEY_EXPOSURE_COMP = "exposure_comp"
        private const val KEY_NO_OF_EXPOSURES = "no_of_exposures"
        private const val KEY_FLASH_USED = "flash_used"
        private const val KEY_FLASH_POWER = "flash_power"
        private const val KEY_FLASH_COMP = "flash_comp"
        private const val KEY_FRAME_SIZE = "frame_size"
        private const val KEY_METERING_MODE = "metering_mode"

        //Added in database version 15
        private const val KEY_FORMATTED_ADDRESS = "formatted_address"

        //Added in database version 17
        private const val KEY_PICTURE_FILENAME = "picture_filename"

        // Added in database version 20
        private const val KEY_LIGHT_SOURCE = "light_source"

        //Lens
        private const val KEY_LENS_ID = "lens_id"
        private const val KEY_LENS_MAKE = "lens_make"
        private const val KEY_LENS_MODEL = "lens_model"

        //Added in database version 14
        private const val KEY_LENS_MAX_APERTURE = "lens_max_aperture"
        private const val KEY_LENS_MIN_APERTURE = "lens_min_aperture"
        private const val KEY_LENS_MAX_FOCAL_LENGTH = "lens_max_focal_length"
        private const val KEY_LENS_MIN_FOCAL_LENGTH = "lens_min_focal_length"
        private const val KEY_LENS_SERIAL_NO = "lens_serial_no"
        private const val KEY_LENS_APERTURE_INCREMENTS = "aperture_increments"

        //Camera
        private const val KEY_CAMERA_ID = "camera_id"
        private const val KEY_CAMERA_MAKE = "camera_make"
        private const val KEY_CAMERA_MODEL = "camera_model"

        //Added in database version 14
        private const val KEY_CAMERA_MAX_SHUTTER = "camera_max_shutter"
        private const val KEY_CAMERA_MIN_SHUTTER = "camera_min_shutter"
        private const val KEY_CAMERA_SERIAL_NO = "camera_serial_no"
        private const val KEY_CAMERA_SHUTTER_INCREMENTS = "shutter_increments"

        //Added in database version 18
        private const val KEY_CAMERA_EXPOSURE_COMP_INCREMENTS = "exposure_comp_increments"

        //Roll
        private const val KEY_ROLL_ID = "roll_id"
        private const val KEY_ROLLNAME = "rollname"
        private const val KEY_ROLL_DATE = "roll_date"
        private const val KEY_ROLL_NOTE = "roll_note"

        //Added in database version 14
        private const val KEY_ROLL_ISO = "roll_iso"
        private const val KEY_ROLL_PUSH = "roll_push"
        private const val KEY_ROLL_FORMAT = "roll_format"

        //Added in database version 16
        private const val KEY_ROLL_ARCHIVED = "roll_archived"

        //Added in database version 21
        private const val KEY_ROLL_UNLOADED = "roll_unloaded"
        private const val KEY_ROLL_DEVELOPED = "roll_developed"

        //Filter
        //Added in database version 14
        private const val KEY_FILTER_ID = "filter_id"
        private const val KEY_FILTER_MAKE = "filter_make"
        private const val KEY_FILTER_MODEL = "filter_model"

        //Film stocks
        //Added in database version 20
        private const val KEY_FILM_STOCK_ID = "film_stock_id"
        private const val KEY_FILM_MANUFACTURER_NAME = "film_manufacturer_name"
        private const val KEY_FILM_STOCK_NAME = "film_stock_name"
        private const val KEY_FILM_ISO = "film_iso"
        private const val KEY_FILM_TYPE = "film_type"
        private const val KEY_FILM_PROCESS = "film_process"
        private const val KEY_FILM_IS_PREADDED = "film_is_preadded"

        //=============================================================================================
        //Database information
        private const val DATABASE_NAME = "filmnotes.db"

        //Updated version from 13 to 14 - 2016-12-03 - v1.7.0
        //Updated version from 14 to 15 - 2017-04-29 - v1.9.0
        //Updated version from 15 to 16 - 2018-02-17 - v1.9.5
        //Updated version from 16 to 17 - 2018-03-26 - v1.11.0
        //Updated version from 17 to 18 - 2018-07-08 - v1.12.0
        //Updated version from 18 to 19 - 2018-07-17 - awaiting
        private const val DATABASE_VERSION = 22

        //=============================================================================================
        //onCreate strings
        private const val CREATE_FILM_STOCKS_TABLE = ("create table " + TABLE_FILM_STOCKS
                + "(" + KEY_FILM_STOCK_ID + " integer primary key autoincrement, "
                + KEY_FILM_MANUFACTURER_NAME + " text not null, "
                + KEY_FILM_STOCK_NAME + " text not null, "
                + KEY_FILM_ISO + " integer,"
                + KEY_FILM_TYPE + " integer,"
                + KEY_FILM_PROCESS + " integer,"
                + KEY_FILM_IS_PREADDED + " integer"
                + ");")
        private const val CREATE_LENS_TABLE = ("create table " + TABLE_LENSES
                + "(" + KEY_LENS_ID + " integer primary key autoincrement, "
                + KEY_LENS_MAKE + " text not null, "
                + KEY_LENS_MODEL + " text not null, "
                + KEY_LENS_MAX_APERTURE + " text, "
                + KEY_LENS_MIN_APERTURE + " text, "
                + KEY_LENS_MAX_FOCAL_LENGTH + " integer, "
                + KEY_LENS_MIN_FOCAL_LENGTH + " integer, "
                + KEY_LENS_SERIAL_NO + " text, "
                + KEY_LENS_APERTURE_INCREMENTS + " integer not null"
                + ");")
        private const val CREATE_CAMERA_TABLE = ("create table " + TABLE_CAMERAS
                + "(" + KEY_CAMERA_ID + " integer primary key autoincrement, "
                + KEY_CAMERA_MAKE + " text not null, "
                + KEY_CAMERA_MODEL + " text not null, "
                + KEY_CAMERA_MAX_SHUTTER + " text, "
                + KEY_CAMERA_MIN_SHUTTER + " text, "
                + KEY_CAMERA_SERIAL_NO + " text, "
                + KEY_CAMERA_SHUTTER_INCREMENTS + " integer not null, "
                + KEY_CAMERA_EXPOSURE_COMP_INCREMENTS + " integer not null default 0, "
                + KEY_LENS_ID + " integer references " + TABLE_LENSES + " on delete set null"
                + ");")
        private const val CREATE_FILTER_TABLE = ("create table " + TABLE_FILTERS
                + "(" + KEY_FILTER_ID + " integer primary key autoincrement, "
                + KEY_FILTER_MAKE + " text not null, "
                + KEY_FILTER_MODEL + " text not null"
                + ");")
        private const val CREATE_ROLL_TABLE = ("create table " + TABLE_ROLLS
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
                + ");")
        private const val CREATE_FRAME_TABLE = ("create table " + TABLE_FRAMES
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
                + ");")
        private const val CREATE_LINK_CAMERA_LENS_TABLE = ("create table " + TABLE_LINK_CAMERA_LENS
                + "(" + KEY_CAMERA_ID + " integer not null references " + TABLE_CAMERAS + " on delete cascade, "
                + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
                + "primary key(" + KEY_CAMERA_ID + ", " + KEY_LENS_ID + ")"
                + ");")
        private const val CREATE_LINK_LENS_FILTER_TABLE = ("create table " + TABLE_LINK_LENS_FILTER
                + "(" + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
                + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
                + "primary key(" + KEY_LENS_ID + ", " + KEY_FILTER_ID + ")"
                + ");")
        private const val CREATE_LINK_FRAME_FILTER_TABLE = ("create table " + TABLE_LINK_FRAME_FILTER
                + "(" + KEY_FRAME_ID + " integer not null references " + TABLE_FRAMES + " on delete cascade, "
                + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
                + "primary key(" + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ")"
                + ");")

        //=============================================================================================
        //onUpgrade strings
        //Legacy table names for onUpgrade() statements.
        //These table names were used in pre 19 versions of the database.
        private const val LEGACY_TABLE_LINK_CAMERA_LENS = "mountables"
        private const val LEGACY_TABLE_LINK_LENS_FILTER = "mountable_filters_lenses"
        private const val ON_UPGRADE_CREATE_FILTER_TABLE = ("create table " + TABLE_FILTERS
                + "(" + KEY_FILTER_ID + " integer primary key autoincrement, "
                + KEY_FILTER_MAKE + " text not null, "
                + KEY_FILTER_MODEL + " text not null"
                + ");")
        private const val ON_UPGRADE_CREATE_LINK_LENS_FILTER_TABLE = ("create table " + LEGACY_TABLE_LINK_LENS_FILTER
                + "(" + KEY_LENS_ID + " integer not null, "
                + KEY_FILTER_ID + " integer not null"
                + ");")
        private const val ALTER_TABLE_FRAMES_1 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_FOCAL_LENGTH + " integer;")
        private const val ALTER_TABLE_FRAMES_2 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_EXPOSURE_COMP + " text;")
        private const val ALTER_TABLE_FRAMES_3 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_NO_OF_EXPOSURES + " integer;")
        private const val ALTER_TABLE_FRAMES_4 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_FLASH_USED + " integer;")
        private const val ALTER_TABLE_FRAMES_5 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_FLASH_POWER + " text;")
        private const val ALTER_TABLE_FRAMES_6 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_FLASH_COMP + " text;")
        private const val ALTER_TABLE_FRAMES_7 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_FRAME_SIZE + " text;")
        private const val ALTER_TABLE_FRAMES_8 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_FILTER_ID + " integer;")
        private const val ALTER_TABLE_FRAMES_9 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_METERING_MODE + " integer;")
        private const val ALTER_TABLE_FRAMES_10 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_FORMATTED_ADDRESS + " text;")
        private const val ALTER_TABLE_FRAMES_11 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_PICTURE_FILENAME + " text;")
        private const val ALTER_TABLE_FRAMES_12 = ("ALTER TABLE " + TABLE_FRAMES
                + " ADD COLUMN " + KEY_LIGHT_SOURCE + " integer;")
        private const val ALTER_TABLE_LENSES_1 = ("ALTER TABLE " + TABLE_LENSES
                + " ADD COLUMN " + KEY_LENS_MAX_APERTURE + " text;")
        private const val ALTER_TABLE_LENSES_2 = ("ALTER TABLE " + TABLE_LENSES
                + " ADD COLUMN " + KEY_LENS_MIN_APERTURE + " text;")
        private const val ALTER_TABLE_LENSES_3 = ("ALTER TABLE " + TABLE_LENSES
                + " ADD COLUMN " + KEY_LENS_MAX_FOCAL_LENGTH + " integer;")
        private const val ALTER_TABLE_LENSES_4 = ("ALTER TABLE " + TABLE_LENSES
                + " ADD COLUMN " + KEY_LENS_MIN_FOCAL_LENGTH + " integer;")
        private const val ALTER_TABLE_LENSES_5 = ("ALTER TABLE " + TABLE_LENSES
                + " ADD COLUMN " + KEY_LENS_SERIAL_NO + " text;")
        private const val ALTER_TABLE_LENSES_6 = ("ALTER TABLE " + TABLE_LENSES
                + " ADD COLUMN " + KEY_LENS_APERTURE_INCREMENTS + " integer not null default 0;")
        private const val ALTER_TABLE_CAMERAS_1 = ("ALTER TABLE " + TABLE_CAMERAS
                + " ADD COLUMN " + KEY_CAMERA_MAX_SHUTTER + " text;")
        private const val ALTER_TABLE_CAMERAS_2 = ("ALTER TABLE " + TABLE_CAMERAS
                + " ADD COLUMN " + KEY_CAMERA_MIN_SHUTTER + " text;")
        private const val ALTER_TABLE_CAMERAS_3 = ("ALTER TABLE " + TABLE_CAMERAS
                + " ADD COLUMN " + KEY_CAMERA_SERIAL_NO + " text;")
        private const val ALTER_TABLE_CAMERAS_4 = ("ALTER TABLE " + TABLE_CAMERAS
                + " ADD COLUMN " + KEY_CAMERA_SHUTTER_INCREMENTS + " integer not null default 0;")
        private const val ALTER_TABLE_CAMERAS_5 = ("ALTER TABLE " + TABLE_CAMERAS
                + " ADD COLUMN " + KEY_CAMERA_EXPOSURE_COMP_INCREMENTS + " integer not null default 0;")
        private const val ALTER_TABLE_CAMERAS_6 = ("ALTER TABLE " + TABLE_CAMERAS
                + " ADD COLUMN " + KEY_LENS_ID + " integer references " + TABLE_LENSES + " on delete set null;")
        private const val ALTER_TABLE_ROLLS_1 = ("ALTER TABLE " + TABLE_ROLLS
                + " ADD COLUMN " + KEY_ROLL_ISO + " integer;")
        private const val ALTER_TABLE_ROLLS_2 = ("ALTER TABLE " + TABLE_ROLLS
                + " ADD COLUMN " + KEY_ROLL_PUSH + " text;")
        private const val ALTER_TABLE_ROLLS_3 = ("ALTER TABLE " + TABLE_ROLLS
                + " ADD COLUMN " + KEY_ROLL_FORMAT + " integer;")
        private const val ALTER_TABLE_ROLLS_4 = ("ALTER TABLE " + TABLE_ROLLS
                + " ADD COLUMN " + KEY_ROLL_ARCHIVED + " integer not null default 0;")
        private const val ALTER_TABLE_ROLLS_5 = ("ALTER TABLE " + TABLE_ROLLS
                + " ADD COLUMN " + KEY_FILM_STOCK_ID + " integer references " + TABLE_FILM_STOCKS + " on delete set null;")
        private const val ALTER_TABLE_ROLLS_6 = ("ALTER TABLE " + TABLE_ROLLS
                + " ADD COLUMN " + KEY_ROLL_UNLOADED + " text;")
        private const val ALTER_TABLE_ROLLS_7 = ("ALTER TABLE " + TABLE_ROLLS
                + " ADD COLUMN " + KEY_ROLL_DEVELOPED + " text;")
        private const val REPLACE_QUOTE_CHARS = ("UPDATE " + TABLE_FRAMES
                + " SET " + KEY_SHUTTER + " = REPLACE(" + KEY_SHUTTER + ", \'q\', \'\"\')"
                + " WHERE " + KEY_SHUTTER + " LIKE \'%q\';")

        // (1) Rename the table, (2) create a new table with new structure,
        // (3) insert data from the renamed table to the new table and (4) drop the renamed table.
        private const val ROLLS_TABLE_REVISION_1 = "alter table $TABLE_ROLLS rename to temp_rolls;"
        private const val ROLLS_TABLE_REVISION_2 = ("create table " + TABLE_ROLLS
                + "(" + KEY_ROLL_ID + " integer primary key autoincrement, "
                + KEY_ROLLNAME + " text not null, "
                + KEY_ROLL_DATE + " text not null, "
                + KEY_ROLL_NOTE + " text, "
                + KEY_CAMERA_ID + " integer references " + TABLE_CAMERAS + " on delete set null, "
                + KEY_ROLL_ISO + " integer, "
                + KEY_ROLL_PUSH + " text, "
                + KEY_ROLL_FORMAT + " integer, "
                + KEY_ROLL_ARCHIVED + " integer not null default 0"
                + ");")
        private const val ROLLS_TABLE_REVISION_3 = ("insert into " + TABLE_ROLLS + " ("
                + KEY_ROLL_ID + ", " + KEY_ROLLNAME + ", " + KEY_ROLL_DATE + ", " + KEY_ROLL_NOTE + ", "
                + KEY_CAMERA_ID + ", " + KEY_ROLL_ISO + ", " + KEY_ROLL_PUSH + ", "
                + KEY_ROLL_FORMAT + ", " + KEY_ROLL_ARCHIVED + ") "
                + "select "
                + KEY_ROLL_ID + ", " + KEY_ROLLNAME + ", " + KEY_ROLL_DATE + ", " + KEY_ROLL_NOTE + ", "
                + "case when " + KEY_CAMERA_ID + " not in (select " + KEY_CAMERA_ID + " from " + TABLE_CAMERAS + ") then null else " + KEY_CAMERA_ID + " end" + ", "
                + KEY_ROLL_ISO + ", " + KEY_ROLL_PUSH + ", "
                + KEY_ROLL_FORMAT + ", " + KEY_ROLL_ARCHIVED + " "
                + "from temp_rolls;")
        private const val ROLLS_TABLE_REVISION_4 = "drop table temp_rolls;"

        // (1) Rename the table, (2) create a new table with new structure,
        // (3) insert data from the renamed table to the new table,
        // (4) create a new link table between frames and filters,
        // (5) insert data from the renamed table to the new link table and
        // (6) drop the renamed table.
        private const val FRAMES_TABLE_REVISION_1 = "alter table $TABLE_FRAMES rename to temp_frames;"
        private const val FRAMES_TABLE_REVISION_2 = ("create table " + TABLE_FRAMES
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
                + ");")
        private const val FRAMES_TABLE_REVISION_3 = ("insert into " + TABLE_FRAMES + " ("
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
                + "where " + KEY_ROLL_ID + " in (select " + KEY_ROLL_ID + " from " + TABLE_ROLLS + ")" + ";")
        private const val ON_UPGRADE_CREATE_LINK_FRAME_FILTER_TABLE = ("create table " + TABLE_LINK_FRAME_FILTER
                + "(" + KEY_FRAME_ID + " integer not null references " + TABLE_FRAMES + " on delete cascade, "
                + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
                + "primary key(" + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ")"
                + ");")
        private const val FRAMES_TABLE_REVISION_4 = ("insert into " + TABLE_LINK_FRAME_FILTER + " ("
                + KEY_FRAME_ID + ", " + KEY_FILTER_ID + ") "
                + "select " + KEY_FRAME_ID + ", " + KEY_FILTER_ID + " "
                + "from temp_frames "
                + "where " + KEY_FILTER_ID + " in (select " + KEY_FILTER_ID + " from " + TABLE_FILTERS + ")"
                + "and " + KEY_ROLL_ID + " in (select " + KEY_ROLL_ID + " from " + TABLE_ROLLS + ")" + ";")
        private const val FRAMES_TABLE_REVISION_5 = "drop table temp_frames;"

        // (1) Rename the table (in pre database 19 versions called "mountables"),
        // (2) create a new table with new structure,
        // (3) insert data from the renamed table to the new table and (4) drop the renamed table.
        private const val CAMERA_LENS_LINK_TABLE_REVISION_1 = "alter table $LEGACY_TABLE_LINK_CAMERA_LENS rename to temp_mountables;"
        private const val CAMERA_LENS_LINK_TABLE_REVISION_2 = ("create table " + TABLE_LINK_CAMERA_LENS
                + "(" + KEY_CAMERA_ID + " integer not null references " + TABLE_CAMERAS + " on delete cascade, "
                + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
                + "primary key(" + KEY_CAMERA_ID + ", " + KEY_LENS_ID + ")"
                + ");")
        private const val CAMERA_LENS_LINK_TABLE_REVISION_3 = ("insert into " + TABLE_LINK_CAMERA_LENS + " ("
                + KEY_CAMERA_ID + ", " + KEY_LENS_ID + ") "
                + "select " + KEY_CAMERA_ID + ", " + KEY_LENS_ID + " "
                + "from temp_mountables "
                + "where " + KEY_CAMERA_ID + " in (select " + KEY_CAMERA_ID + " from " + TABLE_CAMERAS + ") "
                + "and " + KEY_LENS_ID + " in (select " + KEY_LENS_ID + " from " + TABLE_LENSES + ")" + ";")
        private const val CAMERA_LENS_LINK_TABLE_REVISION_4 = "drop table temp_mountables;"

        // (1) Rename the table (in pre database 19 versions called "mountable_filters_lenses"),
        // (2) create a new table with new structure,
        // (3) insert data from the renamed table to the new table and (4) drop the renamed table.
        private const val LENS_FILTER_LINK_TABLE_REVISION_1 = "alter table $LEGACY_TABLE_LINK_LENS_FILTER rename to temp_mountable_filters_lenses;"
        private const val LENS_FILTER_LINK_TABLE_REVISION_2 = ("create table " + TABLE_LINK_LENS_FILTER
                + "(" + KEY_LENS_ID + " integer not null references " + TABLE_LENSES + " on delete cascade, "
                + KEY_FILTER_ID + " integer not null references " + TABLE_FILTERS + " on delete cascade, "
                + "primary key(" + KEY_LENS_ID + ", " + KEY_FILTER_ID + ")"
                + ");")
        private const val LENS_FILTER_LINK_TABLE_REVISION_3 = ("insert into " + TABLE_LINK_LENS_FILTER + " ("
                + KEY_LENS_ID + ", " + KEY_FILTER_ID + ") "
                + "select " + KEY_LENS_ID + ", " + KEY_FILTER_ID + " "
                + "from temp_mountable_filters_lenses "
                + "where " + KEY_LENS_ID + " in (select " + KEY_LENS_ID + " from " + TABLE_LENSES + ") "
                + "and " + KEY_FILTER_ID + " in (select " + KEY_FILTER_ID + " from " + TABLE_FILTERS + ")" + ";")
        private const val LENS_FILTER_LINK_TABLE_REVISION_4 = "drop table temp_mountable_filters_lenses;"

    }

}