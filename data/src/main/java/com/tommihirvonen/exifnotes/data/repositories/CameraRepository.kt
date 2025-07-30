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
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Format
import com.tommihirvonen.exifnotes.core.entities.Increment
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.PartialIncrement
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.dsl.*
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.database.sqlite.transaction

@Singleton
class CameraRepository @Inject constructor(
    private val database: Database, private val lenses: LensRepository) {

    fun addCamera(camera: Camera): Camera {
        val lens = camera.lens?.let {
            lenses.addLens(
                it.copy(make = camera.make, model = camera.model)
            )
        }
        val id = database.insert(TABLE_CAMERAS, buildCameraContentValues(camera.copy(lens = lens)))
        return camera.copy(id = id, lens = lens)
    }

    private fun cameraMapper(cursor: Cursor, lensIds: (Long) -> HashSet<Long>): Camera {
        val id = cursor.getLong(KEY_CAMERA_ID)
        return Camera(
            id = id,
            make = cursor.getStringOrNull(KEY_CAMERA_MAKE),
            model = cursor.getStringOrNull(KEY_CAMERA_MODEL),
            serialNumber = cursor.getStringOrNull(KEY_CAMERA_SERIAL_NO),
            minShutter = cursor.getStringOrNull(KEY_CAMERA_MIN_SHUTTER),
            maxShutter = cursor.getStringOrNull(KEY_CAMERA_MAX_SHUTTER),
            shutterIncrements = Increment.from(cursor.getInt(KEY_CAMERA_SHUTTER_INCREMENTS)),
            exposureCompIncrements = PartialIncrement.from(cursor.getInt(
                KEY_CAMERA_EXPOSURE_COMP_INCREMENTS
            )),
            format = Format.from(cursor.getInt(KEY_CAMERA_FORMAT)),
            lens = cursor.getLongOrNull(KEY_LENS_ID)?.let(lenses::getLens),
            lensIds = lensIds(id)
        )
    }

    fun getCamera(cameraId: Long): Camera? {
        val lenses = database
            .from(TABLE_LINK_CAMERA_LENS)
            .select(KEY_LENS_ID)
            .where { KEY_CAMERA_ID eq cameraId }
            .map { it.getLong(KEY_LENS_ID) }
            .toHashSet()
        return database.from(TABLE_CAMERAS)
            .where { KEY_CAMERA_ID eq cameraId }
            .firstOrNull { cameraMapper(it) { lenses } }
    }

    val cameras: List<Camera> get() {
        val lenses = database
            .from(TABLE_LINK_CAMERA_LENS)
            .map { it.getLong(KEY_CAMERA_ID) to it.getLong(KEY_LENS_ID) }
            .groupBy(Pair<Long, Long>::first, Pair<Long, Long>::second)
        return database
            .from(TABLE_CAMERAS)
            .orderBy {
                KEY_CAMERA_MAKE.asc().ignoreCase()
                KEY_CAMERA_MODEL.asc().ignoreCase()
            }
            .map {
                cameraMapper(it) { id -> lenses[id]?.toHashSet() ?: HashSet() }
            }
    }

    fun deleteCamera(camera: Camera): Int {
        // In case of fixed lens cameras, also delete the lens from database.
        camera.lens?.let(lenses::deleteLens)
        return database
            .from(TABLE_CAMERAS)
            .where { KEY_CAMERA_ID eq camera.id }
            .delete()
    }

    fun isCameraBeingUsed(camera: Camera) = database
        .from(TABLE_ROLLS)
        .where { KEY_CAMERA_ID eq camera.id }
        .exists()

    fun updateCamera(camera: Camera): Pair<Int, Camera> {
        // Check if the camera previously had a fixed lens.
        val previousLensId = database
            .from(TABLE_CAMERAS)
            .select(KEY_LENS_ID)
            .where { KEY_CAMERA_ID eq camera.id }
            .firstOrNull { it.getLong(KEY_LENS_ID) }
            ?: 0

        val database = database.writableDatabase
        return database.transaction {
            // If the camera currently has a fixed lens, update/add it to the database.
            // This needs to be done first since the cameras table references the lenses table.
            val lens = camera.lens
                ?.copy(make = camera.make, model = camera.model)
                ?.let { l ->
                    if (lenses.updateLens(l, this) == 0) {
                        lenses.addLens(l, this)
                    } else {
                        l
                    }
                }
            val contentValues = buildCameraContentValues(camera.copy(lens = lens))
            val affectedRows = this
                .from(TABLE_CAMERAS)
                .where { KEY_CAMERA_ID eq camera.id }
                .update(contentValues)
            if (affectedRows == 0) {
                // If there are no affected rows, then the camera does not yet exist
                // in the database. Returning here rolls back the transaction.
                return 0 to camera
            }

            // If the camera's current lens is null, delete the old fixed lens from the database.
            if (previousLensId > 0 && camera.lens == null) {
                lenses.deleteLens(Lens(id = previousLensId), this)
            }

            // Camera and possible fixed lens were updated completely.
            // Commit transaction and return affected row count.
            affectedRows to camera.copy(lens = lens)
        }
    }

    fun getLinkedLenses(camera: Camera): List<Lens> {
        val lensIds = database
            .from(TABLE_LINK_CAMERA_LENS)
            .select(KEY_LENS_ID)
            .where { KEY_CAMERA_ID eq camera.id }
            .map { it.getLong(KEY_LENS_ID) }
        return lensIds
            .mapNotNull(lenses::getLens)
            .sortedWith(compareBy(Lens::make).thenBy(Lens::model))
    }

    private fun buildCameraContentValues(camera: Camera) = ContentValues().apply {
        put(KEY_CAMERA_MAKE, camera.make)
        put(KEY_CAMERA_MODEL, camera.model)
        put(KEY_CAMERA_SERIAL_NO, camera.serialNumber)
        put(KEY_CAMERA_MIN_SHUTTER, camera.minShutter)
        put(KEY_CAMERA_MAX_SHUTTER, camera.maxShutter)
        put(KEY_CAMERA_SHUTTER_INCREMENTS, camera.shutterIncrements.ordinal)
        put(KEY_CAMERA_EXPOSURE_COMP_INCREMENTS, camera.exposureCompIncrements.ordinal)
        put(KEY_CAMERA_FORMAT, camera.format.ordinal)
        val lens = camera.lens
        if (lens != null) put(KEY_LENS_ID, lens.id)
        else putNull(KEY_LENS_ID)
    }
}