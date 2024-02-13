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
import android.database.sqlite.SQLiteOpenHelper
import com.tommihirvonen.exifnotes.data.extensions.select
import com.tommihirvonen.exifnotes.data.extensions.selectFirstOrNull

fun SQLiteOpenHelper.from(table: String) = Query(readableDatabase, table)

fun Query.select(vararg columns: String) = copy(columns = columns.toList())

fun Query.filter(predicate: String, vararg arguments: Any) =
    copy(filter = predicate to arguments.map { it.toString() })

fun Query.distinct(distinct: Boolean = true) = copy(distinct = distinct)

fun Query.orderBy(vararg columns: String) = copy(orderBy = columns.toList())

fun Query.limit(limit: Int?) = copy(limit = limit)

fun Query.groupBy(vararg columns: String) = AggregateQuery(
    db, table, this.columns, filter, orderBy, limit, columns.toList()
)

fun AggregateQuery.having(having: String) = copy(having = having)

fun <T> Query.map(transform: (Cursor) -> T): List<T> {
    val (selection, selectionArgs) = filter ?: (null to null)
    val ordering = orderBy.joinToString(separator = ",").ifEmpty { null }
    return db.select(table, columns, selection, selectionArgs, distinct,
        orderBy = ordering, limit = limit?.toString(), transform = transform)
}

fun <T> Query.firstOrNull(transform: (Cursor) -> T): T? {
    val (selection, selectionArgs) = filter ?: (null to null)
    val ordering = orderBy.joinToString(separator = ",").ifEmpty { null }
    return db.selectFirstOrNull(table, columns, selection, selectionArgs,
        orderBy = ordering, transform = transform)
}

fun <T> AggregateQuery.map(transform: (Cursor) -> T): List<T> {
    val (selection, selectionArgs) = filter ?: (null to null)
    val ordering = orderBy.joinToString(separator = ",").ifEmpty { null }
    val grouping = groupBy.joinToString(separator = ",").ifEmpty { null }
    return db.select(table, columns, selection, selectionArgs,
        orderBy = ordering, groupBy = grouping, having = having,
        limit = limit?.toString(), transform = transform)
}

fun <T> AggregateQuery.firstOrNull(transform: (Cursor) -> T): T? {
    val (selection, selectionArgs) = filter ?: (null to null)
    val ordering = orderBy.joinToString(separator = ",").ifEmpty { null }
    val grouping = groupBy.joinToString(separator = ",").ifEmpty { null }
    return db.selectFirstOrNull(table, columns, selection, selectionArgs,
        orderBy = ordering, groupBy = grouping, having = having, transform = transform
    )
}