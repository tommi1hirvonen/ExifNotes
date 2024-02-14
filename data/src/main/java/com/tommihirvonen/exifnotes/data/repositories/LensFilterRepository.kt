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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LensFilterRepository @Inject constructor(private val database: Database) {
    fun addLensFilterLink(filter: Filter, lens: Lens) {
        //Here it is safe to use a raw query, because we only use id values, which are database generated.
        //So there is no danger of SQL injection.
        val query = """
            |insert into $TABLE_LINK_LENS_FILTER ($KEY_FILTER_ID, $KEY_LENS_ID)
            |select ${filter.id}, ${lens.id}
            |where not exists (
            |   select 1
            |   from $TABLE_LINK_LENS_FILTER
            |   where $KEY_FILTER_ID = ${filter.id} and $KEY_LENS_ID = ${lens.id}
            |);
        """.trimMargin()
        database.writableDatabase.execSQL(query)
    }

    fun deleteLensFilterLink(filter: Filter, lens: Lens): Int =
        database.writableDatabase.delete(
            TABLE_LINK_LENS_FILTER,
            "$KEY_FILTER_ID = ? AND $KEY_LENS_ID = ?",
            arrayOf(filter.id.toString(), lens.id.toString()))
}