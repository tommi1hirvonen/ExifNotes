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

package com.tommihirvonen.exifnotes.data.extensions

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getLongOrNull

internal fun <T> Cursor.map(transform: (Cursor) -> T): List<T> =
    generateSequence { if (moveToNext()) this else null }
        .map(transform)
        .toList()

internal fun <T> SQLiteOpenHelper.select(table: String,
                                         columns: List<String>? = null,
                                         selection: String? = null,
                                         selectionArgs: List<String>? = null,
                                         distinct: Boolean = false,
                                         groupBy: String? = null,
                                         having: String? = null,
                                         orderBy: String? = null,
                                         limit: String? = null,
                                         transform: (Cursor) -> T): List<T> =
    readableDatabase.select(table, columns, selection, selectionArgs, distinct,
        groupBy, having, orderBy, limit, transform)

internal fun <T> SQLiteDatabase.select(table: String,
                                       columns: List<String>? = null,
                                       selection: String? = null,
                                       selectionArgs: List<String>? = null,
                                       distinct: Boolean = false,
                                       groupBy: String? = null,
                                       having: String? = null,
                                       orderBy: String? = null,
                                       limit: String? = null,
                                       transform: (Cursor) -> T): List<T> =
    query(distinct, table, columns?.toTypedArray(), selection, selectionArgs?.toTypedArray(),
        groupBy, having, orderBy, limit).use { cursor -> cursor.map(transform) }

internal fun <T> SQLiteOpenHelper.selectFirstOrNull(table: String,
                                                    columns: List<String>? = null,
                                                    selection: String? = null,
                                                    selectionArgs: List<String>? = null,
                                                    groupBy: String? = null,
                                                    having: String? = null,
                                                    orderBy: String? = null,
                                                    transform: (Cursor) -> T): T? =
    readableDatabase.selectFirstOrNull(table, columns, selection, selectionArgs, groupBy, having,
        orderBy, transform)

internal fun <T> SQLiteDatabase.selectFirstOrNull(table: String,
                                                  columns: List<String>? = null,
                                                  selection: String? = null,
                                                  selectionArgs: List<String>? = null,
                                                  groupBy: String? = null,
                                                  having: String? = null,
                                                  orderBy: String? = null,
                                                  transform: (Cursor) -> T): T? =
    query(table, columns?.toTypedArray(), selection, selectionArgs?.toTypedArray(),
        groupBy, having, orderBy, "1").use { cursor ->
        if (cursor.moveToFirst()) transform(cursor) else null
    }

internal fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

internal fun Cursor.getLongOrNull(columnName: String): Long? = getLongOrNull(getColumnIndexOrThrow(columnName))

internal fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

internal fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))

internal fun Cursor.getStringOrNull(columnName: String): String? = getString(getColumnIndexOrThrow(columnName))