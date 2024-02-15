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

package com.tommihirvonen.exifnotes.data.repositories

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.dsl.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LensRepository @Inject constructor(private val database: Database) {

    fun addLens(lens: Lens, database: SQLiteDatabase? = null): Long {
        val db = database ?: this.database.writableDatabase
        val id = db.insert(TABLE_LENSES, null, buildLensContentValues(lens))
        lens.id = id
        return id
    }

    internal fun getLens(lensId: Long): Lens? {
        val filters = database.from(TABLE_LINK_LENS_FILTER)
            .select(KEY_FILTER_ID)
            .filter("$KEY_LENS_ID = ?", lensId)
            .map { it.getLong(KEY_FILTER_ID) }
            .toHashSet()
        val cameras = database.from(TABLE_LINK_CAMERA_LENS)
            .select(KEY_CAMERA_ID)
            .filter("$KEY_LENS_ID = ?", lensId)
            .map { it.getLong(KEY_CAMERA_ID) }
            .toHashSet()
        return database.from(TABLE_LENSES)
            .filter("$KEY_LENS_ID = ?", lensId)
            .firstOrNull {
                lensMapper(it).apply {
                    filterIds = filters
                    cameraIds = cameras
                }
            }
    }

    private val lensMapper = { row: Cursor ->
        Lens(
            id = row.getLong(KEY_LENS_ID),
            make = row.getString(KEY_LENS_MAKE),
            model = row.getString(KEY_LENS_MODEL),
            serialNumber = row.getStringOrNull(KEY_LENS_SERIAL_NO),
            minAperture = row.getStringOrNull(KEY_LENS_MIN_APERTURE),
            maxAperture = row.getStringOrNull(KEY_LENS_MAX_APERTURE),
            minFocalLength = row.getInt(KEY_LENS_MIN_FOCAL_LENGTH),
            maxFocalLength = row.getInt(KEY_LENS_MAX_FOCAL_LENGTH),
            apertureIncrements = row.getInt(KEY_LENS_APERTURE_INCREMENTS).let(Increment::from),
            customApertureValues = row.getStringOrNull(KEY_LENS_CUSTOM_APERTURE_VALUES)
                ?.let(Json::decodeFromString)
                ?: emptyList()
        )
    }

    val lenses: List<Lens> get() {
        val filters = database.from(TABLE_LINK_LENS_FILTER)
            .map { it.getLong(KEY_LENS_ID) to it.getLong(KEY_FILTER_ID) }
            .groupBy(Pair<Long, Long>::first, Pair<Long, Long>::second)
        val cameras = database.from(TABLE_LINK_CAMERA_LENS)
            .map { it.getLong(KEY_LENS_ID) to it.getLong(KEY_CAMERA_ID) }
            .groupBy(Pair<Long, Long>::first, Pair<Long, Long>::second)
        val selection = """
            |$KEY_LENS_ID not in (
            |   select $KEY_LENS_ID from $TABLE_CAMERAS where $KEY_LENS_ID is not null
            |)
            """.trimMargin()
        val orderBy = "$KEY_LENS_MAKE collate nocase,$KEY_LENS_MODEL collate nocase"
        return database.from(TABLE_LENSES)
            .filter(selection)
            .orderBy(orderBy)
            .map {
                lensMapper(it).apply {
                    filterIds = filters[id]?.toHashSet() ?: HashSet()
                    cameraIds = cameras[id]?.toHashSet() ?: HashSet()
                }
            }
    }

    fun deleteLens(lens: Lens, database: SQLiteDatabase? = null): Int {
        val db = database ?: this.database.writableDatabase
        return db.delete(TABLE_LENSES, "$KEY_LENS_ID = ?", arrayOf(lens.id.toString()))
    }

    fun isLensInUse(lens: Lens) = database
        .from(TABLE_FRAMES)
        .filter("$KEY_LENS_ID = ?", lens.id)
        .firstOrNull { true } ?: false

    fun updateLens(lens: Lens, database: SQLiteDatabase? = null): Int {
        val db = database ?: this.database.writableDatabase
        val contentValues = buildLensContentValues(lens)
        return db.update(TABLE_LENSES, contentValues, "$KEY_LENS_ID=?", arrayOf(lens.id.toString()))
    }

    fun getLinkedFilters(lens: Lens) = database
        .from(TABLE_FILTERS)
        .filter("""
            |$KEY_FILTER_ID IN (
            |   SELECT $KEY_FILTER_ID 
            |   FROM $TABLE_LINK_LENS_FILTER WHERE $KEY_LENS_ID = ?
            |)
        """.trimMargin(), lens.id)
        .map(filterMapper)

    private fun buildLensContentValues(lens: Lens) = ContentValues().apply {
        put(KEY_LENS_MAKE, lens.make)
        put(KEY_LENS_MODEL, lens.model)
        put(KEY_LENS_SERIAL_NO, lens.serialNumber)
        put(KEY_LENS_MIN_APERTURE, lens.minAperture)
        put(KEY_LENS_MAX_APERTURE, lens.maxAperture)
        put(KEY_LENS_MIN_FOCAL_LENGTH, lens.minFocalLength)
        put(KEY_LENS_MAX_FOCAL_LENGTH, lens.maxFocalLength)
        put(KEY_LENS_APERTURE_INCREMENTS, lens.apertureIncrements.ordinal)
        put(KEY_LENS_CUSTOM_APERTURE_VALUES, Json.encodeToString(lens.customApertureValues))
    }
}