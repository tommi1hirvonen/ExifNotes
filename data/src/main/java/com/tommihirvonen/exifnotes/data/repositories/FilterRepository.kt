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
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.dsl.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(private val database: Database) {

    fun addFilter(filter: Filter): Long {
        val id = database.insert(TABLE_FILTERS, buildFilterContentValues(filter))
        filter.id = id
        return id
    }

    val filters: List<Filter> get() {
        val lenses = database
            .from(TABLE_LINK_LENS_FILTER)
            .map { it.getLong(KEY_FILTER_ID) to it.getLong(KEY_LENS_ID) }
            .groupBy(Pair<Long, Long>::first, Pair<Long, Long>::second)
        return database
            .from(TABLE_FILTERS)
            .orderBy {
                KEY_FILTER_MAKE.asc().ignoreCase()
                KEY_FILTER_MODEL.asc().ignoreCase()
            }
            .map { filterMapper(it).apply { lensIds = lenses[id]?.toHashSet() ?: HashSet() } }
    }

    fun getFilter(filterId: Long): Filter? {
        val lenses = database
            .from(TABLE_LINK_LENS_FILTER)
            .where { KEY_FILTER_ID eq filterId }
            .map { it.getLong(KEY_LENS_ID) }
        return database
            .from(TABLE_FILTERS)
            .where { KEY_FILTER_ID eq filterId }
            .firstOrNull(filterMapper)?.apply { lensIds = lenses.toHashSet() }
    }

    fun deleteFilter(filter: Filter): Int = database
        .from(TABLE_FILTERS)
        .where { KEY_FILTER_ID eq filter.id }
        .delete()

    fun isFilterBeingUsed(filter: Filter) = database
        .from(TABLE_LINK_FRAME_FILTER)
        .where { KEY_FILTER_ID eq filter.id }
        .exists()

    fun updateFilter(filter: Filter): Int {
        val contentValues = buildFilterContentValues(filter)
        return database
            .from(TABLE_FILTERS)
            .where { KEY_FILTER_ID eq filter.id }
            .update(contentValues)
    }

    fun getLinkedFilters(frame: Frame) = database
        .from(TABLE_FILTERS)
        .where {
            KEY_FILTER_ID `in` {
                from(TABLE_LINK_FRAME_FILTER)
                    .select(KEY_FILTER_ID)
                    .where { KEY_FRAME_ID eq frame.id }
            }
        }.map(filterMapper)

    fun getLinkedFilters(lens: Lens) = database
        .from(TABLE_FILTERS)
        .where {
            KEY_FILTER_ID `in` {
                from(TABLE_LINK_LENS_FILTER).select(KEY_FILTER_ID).where { KEY_LENS_ID eq lens.id }
            }
        }.map(filterMapper)

    private val filterMapper = { cursor: Cursor ->
        Filter(
            id = cursor.getLong(KEY_FILTER_ID),
            make = cursor.getStringOrNull(KEY_FILTER_MAKE),
            model = cursor.getStringOrNull(KEY_FILTER_MODEL)
        )
    }

    private fun buildFilterContentValues(filter: Filter) = ContentValues().apply {
        put(KEY_FILTER_MAKE, filter.make)
        put(KEY_FILTER_MODEL, filter.model)
    }
}