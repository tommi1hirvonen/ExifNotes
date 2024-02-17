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

import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.dsl.delete
import com.tommihirvonen.exifnotes.data.dsl.exists
import com.tommihirvonen.exifnotes.data.dsl.from
import com.tommihirvonen.exifnotes.data.dsl.insert
import com.tommihirvonen.exifnotes.data.dsl.where
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LensFilterRepository @Inject constructor(private val database: Database) {
    fun addLensFilterLink(filter: Filter, lens: Lens) {
        val exists = database
            .from(TABLE_LINK_LENS_FILTER)
            .where {
                KEY_FILTER_ID eq filter.id
                KEY_LENS_ID eq lens.id
            }.exists()
        if (!exists) {
            database.insert(TABLE_LINK_LENS_FILTER) {
                put(KEY_FILTER_ID, filter.id)
                put(KEY_LENS_ID, lens.id)
            }
        }
    }

    fun deleteLensFilterLink(filter: Filter, lens: Lens): Int = database
        .from(TABLE_LINK_LENS_FILTER)
        .where {
            KEY_FILTER_ID eq filter.id
            KEY_LENS_ID eq lens.id
        }.delete()
}