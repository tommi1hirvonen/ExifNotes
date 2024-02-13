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
import android.database.sqlite.SQLiteDatabase
import com.tommihirvonen.exifnotes.data.extensions.select
import com.tommihirvonen.exifnotes.data.extensions.selectFirstOrNull

internal class TableQuery(val db: SQLiteDatabase, val table: String) : Query {
    var columns = listOf<String>()
        private set
    var filter: Pair<String, List<String>>? = null
        private set
    var distinct = false
        private set
    var orderBy = listOf<String>()
        private set
    var limit: Int? = null
        private set

    override fun select(vararg columns: String): TableQuery {
        this.columns = this.columns.plus(columns).distinct().toList()
        return this
    }

    override fun where(predicate: String, vararg arguments: String): TableQuery {
        filter = predicate to arguments.toList()
        return this
    }

    override fun distinct(distinct: Boolean): TableQuery {
        this.distinct = distinct
        return this
    }

    override fun orderBy(vararg columns: String): TableQuery {
        orderBy = columns.toList()
        return this
    }

    override fun limit(limit: Int): TableQuery {
        this.limit = limit
        return this
    }

    override fun groupBy(vararg groupBy: String): AggregateQuery {
        return AggregateTableQuery(this, groupBy.toList())
    }

    override fun <T> map(transform: (Cursor) -> T): List<T> {
        val (selection, selectionArgs) = filter ?: (null to null)
        val ordering = orderBy.joinToString(separator = ",").ifEmpty { null }
        return db.select(table, columns, selection, selectionArgs, distinct,
            orderBy = ordering, limit = limit?.toString(), transform = transform)
    }

    override fun <T> firstOrNull(transform: (Cursor) -> T): T? {
        val (selection, selectionArgs) = filter ?: (null to null)
        val ordering = orderBy.joinToString(separator = ",").ifEmpty { null }
        return db.selectFirstOrNull(table, columns, selection, selectionArgs,
            orderBy = ordering, transform = transform)
    }
}