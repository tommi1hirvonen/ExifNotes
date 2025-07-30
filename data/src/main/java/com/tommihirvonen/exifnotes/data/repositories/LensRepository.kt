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
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LensRepository @Inject constructor(private val database: Database) {

    fun addLens(lens: Lens, database: SQLiteDatabase? = null): Lens {
        val db = database ?: this.database.writableDatabase
        val id = db.insert(TABLE_LENSES, buildLensContentValues(lens))
        return lens.copy(id = id)
    }

    fun getLens(lensId: Long): Lens? {
        val filters = database.from(TABLE_LINK_LENS_FILTER)
            .select(KEY_FILTER_ID)
            .where { KEY_LENS_ID eq lensId }
            .map { it.getLong(KEY_FILTER_ID) }
            .toHashSet()
        val cameras = database.from(TABLE_LINK_CAMERA_LENS)
            .select(KEY_CAMERA_ID)
            .where { KEY_LENS_ID eq lensId }
            .map { it.getLong(KEY_CAMERA_ID) }
            .toHashSet()
        return database.from(TABLE_LENSES)
            .where { KEY_LENS_ID eq lensId }
            .firstOrNull {
                lensMapper(
                    row = it,
                    filterIds = { filters },
                    cameraIds = { cameras }
                )
            }
    }

    private fun lensMapper(
        row: Cursor,
        filterIds: (Long) -> HashSet<Long>,
        cameraIds: (Long) -> HashSet<Long>
    ): Lens {
        val id = row.getLong(KEY_LENS_ID)
        return Lens(
            id = id,
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
                ?: emptyList(),
            filterIds = filterIds(id),
            cameraIds = cameraIds(id)
        )
    }

    val lenses: List<Lens> get() {
        val filters = database.from(TABLE_LINK_LENS_FILTER)
            .map { it.getLong(KEY_LENS_ID) to it.getLong(KEY_FILTER_ID) }
            .groupBy(Pair<Long, Long>::first, Pair<Long, Long>::second)
        val cameras = database.from(TABLE_LINK_CAMERA_LENS)
            .map { it.getLong(KEY_LENS_ID) to it.getLong(KEY_CAMERA_ID) }
            .groupBy(Pair<Long, Long>::first, Pair<Long, Long>::second)
        return database.from(TABLE_LENSES)
            .where {
                KEY_LENS_ID notIn {
                    from(TABLE_CAMERAS).select(KEY_LENS_ID).where { KEY_LENS_ID.isNotNull() }
                }
            }
            .orderBy {
                KEY_LENS_MAKE.asc().ignoreCase()
                KEY_LENS_MODEL.asc().ignoreCase()
            }
            .map {
                lensMapper(
                    row = it,
                    filterIds = { id -> filters[id]?.toHashSet() ?: HashSet() },
                    cameraIds = { id -> cameras[id]?.toHashSet() ?: HashSet() }
                )
            }
    }

    fun deleteLens(lens: Lens, database: SQLiteDatabase? = null): Int {
        val db = database ?: this.database.writableDatabase
        return db
            .from(TABLE_LENSES)
            .where { KEY_LENS_ID eq lens.id }
            .delete()
    }

    fun isLensInUse(lens: Lens) = database
        .from(TABLE_FRAMES)
        .where { KEY_LENS_ID eq lens.id }
        .exists()

    fun updateLens(lens: Lens, database: SQLiteDatabase? = null): Int {
        val db = database ?: this.database.writableDatabase
        val contentValues = buildLensContentValues(lens)
        return db
            .from(TABLE_LENSES)
            .where { KEY_LENS_ID eq lens.id }
            .update(contentValues)
    }

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