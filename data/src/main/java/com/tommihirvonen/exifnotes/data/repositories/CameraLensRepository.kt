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

import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraLensRepository @Inject constructor(private val database: Database) {
    fun addCameraLensLink(camera: Camera, lens: Lens) {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        val query = ("INSERT INTO " + TABLE_LINK_CAMERA_LENS + "(" + KEY_CAMERA_ID + "," + KEY_LENS_ID
                + ") SELECT " + camera.id + ", " + lens.id
                + " WHERE NOT EXISTS(SELECT 1 FROM " + TABLE_LINK_CAMERA_LENS + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.id + " AND " + KEY_LENS_ID + "=" + lens.id + ");")
        database.writableDatabase.execSQL(query)
    }

    fun deleteCameraLensLink(camera: Camera, lens: Lens) =
        database.writableDatabase.delete(
            TABLE_LINK_CAMERA_LENS,
            "$KEY_CAMERA_ID = ? AND $KEY_LENS_ID = ?", arrayOf(camera.id.toString(), lens.id.toString()))
}