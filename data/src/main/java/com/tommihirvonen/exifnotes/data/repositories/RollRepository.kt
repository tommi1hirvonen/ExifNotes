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
import com.tommihirvonen.exifnotes.core.entities.Format
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.core.localDateTimeOrNull
import com.tommihirvonen.exifnotes.core.sortableDateTime
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.dsl.*
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RollRepository @Inject constructor(private val database: Database,
                                         private val cameras: CameraRepository,
                                         private val labels: LabelRepository,
                                         private val filmStocks: FilmStockRepository) {

    fun addRoll(roll: Roll): Long {
        val id = database.insert(TABLE_ROLLS, buildRollContentValues(roll))
        roll.id = id
        for (label in roll.labels) {
            if (label.id == 0L) {
                labels.addLabel(label)
            }
            val values = ContentValues().apply {
                put(KEY_ROLL_ID, id)
                put(KEY_LABEL_ID, label.id)
            }
            database.insert(TABLE_LINK_ROLL_LABEL, values)
        }
        return id
    }

    fun getRolls(filterMode: RollFilterMode): List<Roll> {
        val selection: String = when (filterMode) {
            is RollFilterMode.Active -> "$KEY_ROLL_ARCHIVED = 0"
            is RollFilterMode.Archived -> "$KEY_ROLL_ARCHIVED > 0"
            is RollFilterMode.All -> "1 = 1"
            is RollFilterMode.Favorites -> "$KEY_ROLL_FAVORITE > 0"
            is RollFilterMode.HasLabel -> """
                |$KEY_ROLL_ID IN (
                |   SELECT $KEY_ROLL_ID
                |   FROM $TABLE_LINK_ROLL_LABEL
                |   WHERE $KEY_LABEL_ID = ${filterMode.label.id}
                |)
                """.trimMargin()
        }
        return database.from(TABLE_ROLLS)
            .where(selection)
            .orderBy("$KEY_ROLL_DATE DESC")
            .map { row ->
                Roll(
                    id = row.getLong(KEY_ROLL_ID),
                    name = row.getStringOrNull(KEY_ROLLNAME),
                    date = row.getStringOrNull(KEY_ROLL_DATE)?.let(::localDateTimeOrNull) ?: LocalDateTime.now(),
                    unloaded = row.getStringOrNull(KEY_ROLL_UNLOADED)?.let(::localDateTimeOrNull),
                    developed = row.getStringOrNull(KEY_ROLL_DEVELOPED)?.let(::localDateTimeOrNull),
                    note = row.getStringOrNull(KEY_ROLL_NOTE),
                    camera = row.getLongOrNull(KEY_CAMERA_ID)?.let(cameras::getCamera),
                    iso = row.getInt(KEY_ROLL_ISO),
                    pushPull = row.getStringOrNull(KEY_ROLL_PUSH),
                    format = Format.from(row.getInt(KEY_ROLL_FORMAT)),
                    archived = row.getInt(KEY_ROLL_ARCHIVED) > 0,
                    favorite = row.getInt(KEY_ROLL_FAVORITE) > 0,
                    filmStock = row.getLongOrNull(KEY_FILM_STOCK_ID)?.let(filmStocks::getFilmStock)
                ).apply {
                    labels.addAll(this@RollRepository.labels.getLabels(this))
                }
            }
    }

    val rollCounts: RollCounts get() {
        val (active, archived, favorites) = arrayOf(
            "$KEY_ROLL_ARCHIVED <= 0",
            "$KEY_ROLL_ARCHIVED > 0",
            "$KEY_ROLL_FAVORITE > 0"
        ).map { predicate ->
            database.from(TABLE_ROLLS)
                .select("count(*)")
                .where(predicate)
                .firstOrNull { it.getInt(0) } ?: 0
        }
        return RollCounts(active, archived, favorites)
    }

    fun deleteRoll(roll: Roll): Int = database
        .from(TABLE_ROLLS)
        .where { KEY_ROLL_ID eq roll.id }
        .delete()

    fun updateRoll(roll: Roll): Int {
        val contentValues = buildRollContentValues(roll)
        val labels = labels.getLabels(roll)
        val labelsToAdd = roll.labels.filterNot { labels.contains(it) }
        val labelsToRemove = labels.filterNot { roll.labels.contains(it) }
        for (label in labelsToAdd) {
            val values = ContentValues().apply {
                put(KEY_ROLL_ID, roll.id)
                put(KEY_LABEL_ID, label.id)
            }
            database.insert(TABLE_LINK_ROLL_LABEL, values)
        }
        for (label in labelsToRemove) {
            database
                .from(TABLE_LINK_ROLL_LABEL)
                .where {
                    KEY_ROLL_ID eq roll.id
                    KEY_LABEL_ID eq label.id
                }.delete()
        }
        return database
            .from(TABLE_ROLLS)
            .where { KEY_ROLL_ID eq roll.id }
            .update(contentValues)
    }

    fun getNumberOfFrames(roll: Roll): Int = database
        .from(TABLE_FRAMES)
        .select("count(*)")
        .where { KEY_ROLL_ID eq roll.id }
        .firstOrNull { it.getInt(0) } ?: 0

    private fun buildRollContentValues(roll: Roll) = ContentValues().apply {
        put(KEY_ROLLNAME, roll.name)
        put(KEY_ROLL_DATE, roll.date.sortableDateTime)
        put(KEY_ROLL_NOTE, roll.note)

        val camera = roll.camera
        if (camera != null) put(KEY_CAMERA_ID, camera.id)
        else putNull(KEY_CAMERA_ID)

        put(KEY_ROLL_ISO, roll.iso)
        put(KEY_ROLL_PUSH, roll.pushPull)
        put(KEY_ROLL_FORMAT, roll.format.ordinal)
        put(KEY_ROLL_ARCHIVED, roll.archived)
        put(KEY_ROLL_FAVORITE, roll.favorite)

        val filmStock = roll.filmStock
        if (filmStock != null) put(KEY_FILM_STOCK_ID, filmStock.id)
        else putNull(KEY_FILM_STOCK_ID)

        put(KEY_ROLL_UNLOADED, roll.unloaded?.sortableDateTime)
        put(KEY_ROLL_DEVELOPED, roll.developed?.sortableDateTime)
    }
}