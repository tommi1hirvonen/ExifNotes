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

package com.tommihirvonen.exifnotes.data.query

import android.database.Cursor
import com.tommihirvonen.exifnotes.data.extensions.select
import com.tommihirvonen.exifnotes.data.extensions.selectFirstOrNull

internal class AggregateTableQuery(private val query: TableQuery, private val groupBy: List<String>)
    : AggregateQuery {

    private var having: String? = null

    override fun having(having: String): Queryable {
        this.having = having
        return this
    }

    override fun <T> map(transform: (Cursor) -> T): List<T> {
        query.apply {
            val (selection, selectionArgs) = filter ?: (null to null)
            val ordering = orderBy.joinToString(separator = ",").ifEmpty { null }
            val grouping = groupBy.joinToString(separator = ",").ifEmpty { null }
            return db.select(table, columns, selection, selectionArgs, distinct,
                orderBy = ordering, groupBy = grouping, having = having,
                limit = limit?.toString(), transform = transform)
        }
    }

    override fun <T> firstOrNull(transform: (Cursor) -> T): T? {
        query.apply {
            val (selection, selectionArgs) = query.filter ?: (null to null)
            val ordering = query.orderBy.joinToString(separator = ",").ifEmpty { null }
            val grouping = groupBy.joinToString(separator = ",").ifEmpty { null }
            return query.db.selectFirstOrNull(table, columns, selection, selectionArgs,
                orderBy = ordering, groupBy = grouping, having = having, transform = transform
            )
        }
    }
}