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

package com.tommihirvonen.exifnotes.data.integrity

import android.database.sqlite.SQLiteDatabase

internal class ColumnIntegrityCheck(private val table: String,
                           private val column: String,
                           private val datatype: Datatype) {
    private var notNull: Boolean = false
    private var primaryKey: Boolean = false
    private var autoIncrement: Boolean = false
    private var foreignKey: ForeignKey? = null

    fun notNull(value: Boolean = true): ColumnIntegrityCheck {
        notNull = value
        return this
    }

    fun primaryKey(value: Boolean = true): ColumnIntegrityCheck {
        primaryKey = value
        return this
    }

    fun autoIncrement(value: Boolean = true): ColumnIntegrityCheck {
        autoIncrement = value
        return this
    }

    fun foreignKey(referencedTable: String, onDelete: OnDelete): ColumnIntegrityCheck {
        val fk = ForeignKey(referencedTable, onDelete)
        foreignKey = fk
        return this
    }

    fun check(db: SQLiteDatabase): Boolean {
        if (autoIncrement) {
            // We can check that the table is autoincrement from the master tables.
            // Column 'sql' is the query with which the table was created.
            // If a table is autoincrement, then it can only have one primary key.
            // If the primary key matches, then also the autoincrement column is correct.
            // The primary key will be checked later in this method.
            val incrementQuery = """
                |SELECT *
                |FROM sqlite_master
                |WHERE type = 'table' AND name = '$table' AND sql LIKE '%AUTOINCREMENT%'
            """.trimMargin()
            val incrementCursor = db.rawQuery(incrementQuery, null)
            if (!incrementCursor.moveToFirst()) {
                // No rows were returned. The table has no autoincrement. Integrity check fails.
                incrementCursor.close()
                return false
            }
            incrementCursor.close()
        }
        val fk = foreignKey
        if (fk != null) {
            val (referenceTableNameInput, onDeleteInput) = fk
            val onDeleteActionInput = when (onDeleteInput) {
                OnDelete.CASCADE -> "CASCADE"
                OnDelete.SET_NULL -> "SET NULL"
            }
            // We can check that the column is a foreign key column using one of the SQLite pragma statements.
            val foreignKeyQuery = "PRAGMA FOREIGN_KEY_LIST('$table')"
            val foreignKeyCursor = db.rawQuery(foreignKeyQuery, null)
            var foreignKeyFound = false
            //Iterate through the tables foreign key columns and get the properties.
            while (foreignKeyCursor.moveToNext()) {
                val table = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("table"))
                val from = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("from"))
                val onDelete = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("on_delete"))
                val to = foreignKeyCursor.getString(foreignKeyCursor.getColumnIndexOrThrow("to"))
                //If the table, from-column and on-delete actions match to those defined
                //by the parameters, the foreign key is correct. The to-column value
                //should be null, because during table creation we have used the shorthand form
                //to reference the parent table's primary key.
                if (table == referenceTableNameInput && from == column
                    && onDelete.equals(onDeleteActionInput, ignoreCase = true) && to == null) {
                    foreignKeyFound = true
                    break
                }
            }
            foreignKeyCursor.close()
            //If foreign key was not defined, integrity check fails -> return false.
            if (!foreignKeyFound) return false
        }

        val query = "PRAGMA TABLE_INFO('$table');"
        val cursor = db.rawQuery(query, null)

        //Iterate the result rows...
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            // ...until the name checks.
            if (columnName == column) {
                val columnType = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) > 0
                //If the column is defined as primary key, the pk value is 1.
                val primaryKey = cursor.getInt(cursor.getColumnIndexOrThrow("pk")) > 0
                cursor.close()

                val type = when (datatype) {
                    Datatype.INT -> "int"
                    Datatype.TEXT -> "text"
                }
                //Check that the attributes are correct and return the result
                return columnType.startsWith(type, ignoreCase = true) //type can be int or integer
                        && notNull == this.notNull
                        && primaryKey == this.primaryKey
            }
        }
        //We get here if no matching column names were found
        cursor.close()
        return false
    }
}