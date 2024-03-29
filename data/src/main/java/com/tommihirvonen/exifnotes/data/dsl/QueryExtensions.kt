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

package com.tommihirvonen.exifnotes.data.dsl

import android.database.Cursor
import androidx.core.database.getLongOrNull

internal fun TableReference.select(vararg columns: String) =
    Query(db, table, filter = filter, columns = columns.toList())

internal fun Query.select(vararg columns: String) =
    copy(columns = columns.toList())

internal fun Query.where(predicate: String, vararg arguments: Any) =
    copy(filter = predicate to arguments.map { it.toString() })

internal fun Query.where(block: (Predicate.() -> Any)): Query {
    val predicate = Predicate()
    block(predicate)
    val filter = predicate.expression to predicate.arguments.map { it.toString() }
    return copy(filter = filter)
}

internal fun TableReference.distinct(distinct: Boolean = true) =
    Query(db, table, filter = filter, distinct = distinct)

internal fun Query.distinct(distinct: Boolean = true) =
    copy(distinct = distinct)

internal fun TableReference.orderBy(block: OrderByBuilder.() -> Any): Query {
    val builder = OrderByBuilder()
    block(builder)
    return Query(db, table, filter = filter, orderBy = builder.expression)
}

internal fun Query.orderBy(block: OrderByBuilder.() -> Any): Query {
    val builder = OrderByBuilder()
    block(builder)
    return copy(orderBy = builder.expression)
}

internal fun TableReference.limit(limit: Int?) =
    Query(db, table, filter = filter, limit = limit)

internal fun Query.limit(limit: Int?) =
    copy(limit = limit)

internal fun Query.groupBy(vararg columns: String) = AggregateQuery(
    db, table, this.columns, filter, orderBy, limit, columns.toList()
)

internal fun AggregateQuery.having(having: String) =
    copy(having = having)

internal fun TableReference.count(block: (Predicate.() -> Any)) = where(block).count()

internal fun TableReference.count() = select("count(*)").firstOrNull { it.getInt(0) } ?: 0

internal fun TableReference.exists() = select("1").firstOrNull {  } != null

internal fun Query.exists() = select("1").firstOrNull {  } != null

internal fun <T> TableReference.map(transform: (Cursor) -> T): List<T> =
    Query(db, table, filter = filter).map(transform)

internal fun <T> Query.map(transform: (Cursor) -> T): List<T> {
    val (selection, selectionArgs) = filter ?: (null to null)
    return db.select(table, columns, selection, selectionArgs, distinct,
        orderBy = orderBy, limit = limit?.toString(), transform = transform)
}

internal fun <T> TableReference.firstOrNull(transform: (Cursor) -> T): T? =
    Query(db, table, filter = filter).firstOrNull(transform)

internal fun <T> Query.firstOrNull(transform: (Cursor) -> T): T? {
    val (selection, selectionArgs) = filter ?: (null to null)
    return db.selectFirstOrNull(table, columns, selection, selectionArgs,
        orderBy = orderBy, transform = transform)
}

internal fun <T> AggregateQuery.map(transform: (Cursor) -> T): List<T> {
    val (selection, selectionArgs) = filter ?: (null to null)
    val grouping = groupBy.joinToString(separator = ",").ifEmpty { null }
    return db.select(table, columns, selection, selectionArgs,
        orderBy = orderBy, groupBy = grouping, having = having,
        limit = limit?.toString(), transform = transform)
}

internal fun <T> AggregateQuery.firstOrNull(transform: (Cursor) -> T): T? {
    val (selection, selectionArgs) = filter ?: (null to null)
    val grouping = groupBy.joinToString(separator = ",").ifEmpty { null }
    return db.selectFirstOrNull(table, columns, selection, selectionArgs,
        orderBy = orderBy, groupBy = grouping, having = having, transform = transform
    )
}

private fun <T> Cursor.map(transform: (Cursor) -> T): List<T> =
    generateSequence { if (moveToNext()) this else null }
        .map(transform)
        .toList()

private fun <T> DatabaseProvider.select(
    table: String,
    columns: List<String>? = null,
    selection: String? = null,
    selectionArgs: List<String>? = null,
    distinct: Boolean = false,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null,
    transform: (Cursor) -> T): List<T> =
    read.query(distinct, table, columns?.toTypedArray(), selection, selectionArgs?.toTypedArray(),
        groupBy, having, orderBy, limit).use { cursor -> cursor.map(transform) }

private fun <T> DatabaseProvider.selectFirstOrNull(
    table: String,
    columns: List<String>? = null,
    selection: String? = null,
    selectionArgs: List<String>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    transform: (Cursor) -> T): T? =
    read.query(table, columns?.toTypedArray(), selection, selectionArgs?.toTypedArray(),
        groupBy, having, orderBy, "1").use { cursor ->
        if (cursor.moveToFirst()) transform(cursor) else null
    }

internal fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

internal fun Cursor.getLongOrNull(columnName: String): Long? = getLongOrNull(getColumnIndexOrThrow(columnName))

internal fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

internal fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))

internal fun Cursor.getStringOrNull(columnName: String): String? = getString(getColumnIndexOrThrow(columnName))