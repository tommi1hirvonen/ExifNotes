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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal fun TableReference.delete(): Int {
    val (selection, selectionArgs) = filter ?: (null to null)
    return db.write.delete(table, selection, selectionArgs?.toTypedArray())
}

internal fun TableReference.update(valuesBuilder: ContentValues.() -> Any) : Int {
    val values = ContentValues()
    valuesBuilder(values)
    return update(values)
}

internal fun TableReference.update(values: ContentValues) : Int {
    val (selection, selectionArgs) = filter ?: (null to null)
    return db.write.update(table, values, selection, selectionArgs?.toTypedArray())
}

internal fun SQLiteOpenHelper.insert(table: String,
                                     valuesBuilder: ContentValues.() -> Any): Long {
    val values = ContentValues()
    valuesBuilder(values)
    return insert(table, values)
}

internal fun SQLiteOpenHelper.insert(table: String, values: ContentValues): Long {
    return writableDatabase.insert(table, values)
}

internal fun SQLiteDatabase.insert(table: String, values: ContentValues): Long {
    return insert(table, null, values)
}