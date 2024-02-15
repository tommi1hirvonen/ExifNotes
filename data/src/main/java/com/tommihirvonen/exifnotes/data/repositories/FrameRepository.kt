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
import com.tommihirvonen.exifnotes.core.decimalString
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.LightSource
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.latLngOrNull
import com.tommihirvonen.exifnotes.core.localDateTimeOrNull
import com.tommihirvonen.exifnotes.core.sortableDateTime
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.extensions.getInt
import com.tommihirvonen.exifnotes.data.extensions.getLong
import com.tommihirvonen.exifnotes.data.extensions.getLongOrNull
import com.tommihirvonen.exifnotes.data.extensions.getString
import com.tommihirvonen.exifnotes.data.extensions.getStringOrNull
import com.tommihirvonen.exifnotes.data.query.*
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrameRepository @Inject constructor(private val database: Database,
                                          private val lenses: LensRepository) {
    fun addFrame(frame: Frame): Boolean {
        val values = buildFrameContentValues(frame)
        val rowId = database.writableDatabase.use { db ->
            db.insert(TABLE_FRAMES, null, values)
        }
        frame.id = rowId
        // Add the filter links, if the frame was inserted successfully.
        return if (rowId != -1L) {
            frame.filters.forEach { filter -> addFrameFilterLink(frame, filter) }
            true
        } else {
            false
        }
    }

    private fun addFrameFilterLink(frame: Frame, filter: Filter) {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        val query = """
            |insert into $TABLE_LINK_FRAME_FILTER ($KEY_FRAME_ID, $KEY_FILTER_ID)
            |select ${frame.id}, ${filter.id}
            |where not exists (
            |   select *
            |   from $TABLE_LINK_FRAME_FILTER
            |   where $KEY_FRAME_ID = ${frame.id} and $KEY_FILTER_ID = ${filter.id}
            |);
        """.trimMargin()
        database.writableDatabase.execSQL(query)
    }

    fun getFrames(roll: Roll): List<Frame> = database
        .from(TABLE_FRAMES)
        .filter("$KEY_ROLL_ID=?", roll.id)
        .orderBy(KEY_COUNT)
        .map { row ->
            Frame(
                roll = roll,
                id = row.getLong(KEY_FRAME_ID),
                count = row.getInt(KEY_COUNT),
                shutter = row.getStringOrNull(KEY_SHUTTER),
                aperture = row.getStringOrNull(KEY_APERTURE),
                note = row.getStringOrNull(KEY_FRAME_NOTE),
                focalLength = row.getInt(KEY_FOCAL_LENGTH),
                exposureComp = row.getStringOrNull(KEY_EXPOSURE_COMP),
                noOfExposures = row.getInt(KEY_NO_OF_EXPOSURES),
                flashComp = row.getStringOrNull(KEY_FLASH_COMP),
                meteringMode = row.getInt(KEY_METERING_MODE),
                formattedAddress = row.getStringOrNull(KEY_FORMATTED_ADDRESS),
                pictureFilename = row.getStringOrNull(KEY_PICTURE_FILENAME),
                lightSource = LightSource.from(row.getInt(KEY_LIGHT_SOURCE)),
                flashUsed = row.getInt(KEY_FLASH_USED) > 0,
                flashPower = row.getStringOrNull(KEY_FLASH_POWER),
                location = row.getStringOrNull(KEY_LOCATION)?.let(::latLngOrNull),
                date = row.getStringOrNull(KEY_DATE)?.let(::localDateTimeOrNull) ?: LocalDateTime.now(),
                lens = row.getLongOrNull(KEY_LENS_ID)?.let(lenses::getLens)
            ).apply {
                filters = getLinkedFilters(this)
            }
        }

    private fun getLinkedFilters(frame: Frame) = database
        .from(TABLE_FILTERS)
        .filter("""
            |$KEY_FILTER_ID IN (
            |   SELECT $KEY_FILTER_ID
            |   FROM $TABLE_LINK_FRAME_FILTER
            |   WHERE $KEY_FRAME_ID = ?
            |)
        """.trimMargin(), frame.id)
        .map(filterMapper)

    fun updateFrame(frame: Frame): Int {
        val contentValues = buildFrameContentValues(frame)
        val rows = database.writableDatabase
            .update(TABLE_FRAMES, contentValues, "$KEY_FRAME_ID=?", arrayOf(frame.id.toString()))
        if (rows > 0) {
            deleteFrameFilterLinks(frame)
            frame.filters.forEach { filter -> addFrameFilterLink(frame, filter) }
        }
        return rows
    }

    private fun deleteFrameFilterLinks(frame: Frame): Int = database.writableDatabase
        .delete(TABLE_LINK_FRAME_FILTER, "$KEY_FRAME_ID = ?", arrayOf(frame.id.toString()))


    fun deleteFrame(frame: Frame): Int = database.writableDatabase.delete(
        TABLE_FRAMES,
        "$KEY_FRAME_ID = ?",
        arrayOf(frame.id.toString()))


    val complementaryPictureFilenames: List<String> get() = database
        .from(TABLE_FRAMES)
        .select(KEY_PICTURE_FILENAME)
        .filter("$KEY_PICTURE_FILENAME IS NOT NULL")
        .map { it.getString(KEY_PICTURE_FILENAME) }

    private fun buildFrameContentValues(frame: Frame) = ContentValues().apply {
        put(KEY_ROLL_ID, frame.roll.id)
        put(KEY_COUNT, frame.count)
        put(KEY_DATE, frame.date.sortableDateTime)

        val lens = frame.lens
        if (lens != null) put(KEY_LENS_ID, lens.id)
        else putNull(KEY_LENS_ID)

        put(KEY_SHUTTER, frame.shutter)
        put(KEY_APERTURE, frame.aperture)
        put(KEY_FRAME_NOTE, frame.note)
        put(KEY_LOCATION, frame.location?.decimalString)
        put(KEY_FOCAL_LENGTH, frame.focalLength)
        put(KEY_EXPOSURE_COMP, frame.exposureComp)
        put(KEY_NO_OF_EXPOSURES, frame.noOfExposures)
        put(KEY_FLASH_USED, frame.flashUsed)
        put(KEY_FLASH_POWER, frame.flashPower)
        put(KEY_FLASH_COMP, frame.flashComp)
        put(KEY_METERING_MODE, frame.meteringMode)
        put(KEY_FORMATTED_ADDRESS, frame.formattedAddress)
        put(KEY_PICTURE_FILENAME, frame.pictureFilename)
        put(KEY_LIGHT_SOURCE, frame.lightSource.ordinal)
    }
}