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
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.dsl.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabelRepository @Inject constructor(private val database: Database) {
    val labels: List<Label> get() = database
        .from(TABLE_LABELS)
        .orderBy { KEY_LABEL_NAME.asc().ignoreCase() }
        .map(labelMapper)

    fun getLabel(labelId: Long): Label? = database
        .from(TABLE_LABELS)
        .where { KEY_LABEL_ID eq labelId }
        .firstOrNull(labelMapper)

    fun getLabels(roll: Roll) = getLabels(roll.id)

    internal fun getLabels(rollId: Long) = database
        .from(TABLE_LABELS)
        .where {
            KEY_LABEL_ID `in` {
                from(TABLE_LINK_ROLL_LABEL)
                    .select(KEY_LABEL_ID)
                    .where { KEY_ROLL_ID eq rollId }
            }
        }
        .orderBy { KEY_LABEL_NAME.asc().ignoreCase() }
        .map(labelMapper)

    fun addLabel(label: Label): Label {
        val values = buildContentValues(label)
        val id = database.insert(TABLE_LABELS, values)
        return label.copy(id = id)
    }

    fun updateLabel(label: Label): Int {
        val values = buildContentValues(label)
        return database
            .from(TABLE_LABELS)
            .where { KEY_LABEL_ID eq label.id }
            .update(values)
    }

    fun deleteLabel(label: Label): Int = database
        .from(TABLE_LABELS)
        .where { KEY_LABEL_ID eq label.id }
        .delete()

    private fun buildContentValues(label: Label) = { values: ContentValues ->
        values.put(KEY_LABEL_NAME, label.name)
    }

    private val labelMapper = { cursor: Cursor ->
        val id = cursor.getLong(KEY_LABEL_ID)
        val name = cursor.getString(KEY_LABEL_NAME)
        val rollCount = database
            .from(TABLE_LINK_ROLL_LABEL)
            .count { KEY_LABEL_ID eq id }
        Label(id, name, rollCount)
    }
}