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
import com.tommihirvonen.exifnotes.core.entities.FilmProcess
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.FilmType
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.data.constants.*
import com.tommihirvonen.exifnotes.data.dsl.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilmStockRepository @Inject constructor(private val database: Database) {
    companion object {
        internal fun buildFilmStockContentValues(filmStock: FilmStock) = ContentValues().apply {
            put(KEY_FILM_MANUFACTURER_NAME, filmStock.make)
            put(KEY_FILM_STOCK_NAME, filmStock.model)
            put(KEY_FILM_ISO, filmStock.iso)
            put(KEY_FILM_TYPE, filmStock.type.ordinal)
            put(KEY_FILM_PROCESS, filmStock.process.ordinal)
            put(KEY_FILM_IS_PREADDED, filmStock.isPreadded)
        }
    }

    fun addFilmStock(filmStock: FilmStock): Long {
        val id = database.writableDatabase
            .insert(TABLE_FILM_STOCKS, null, buildFilmStockContentValues(filmStock))
        filmStock.id = id
        return id
    }

    private val filmStockMapper = { row: Cursor ->
        FilmStock(
            id = row.getLong(KEY_FILM_STOCK_ID),
            make = row.getStringOrNull(KEY_FILM_MANUFACTURER_NAME),
            model = row.getStringOrNull(KEY_FILM_STOCK_NAME),
            iso = row.getInt(KEY_FILM_ISO),
            type = FilmType.from(row.getInt(KEY_FILM_TYPE)),
            process = FilmProcess.from(row.getInt(KEY_FILM_PROCESS)),
            isPreadded = row.getInt(KEY_FILM_IS_PREADDED) > 0
        )
    }

    internal fun getFilmStock(filmStockId: Long) = database
        .from(TABLE_FILM_STOCKS)
        .filter("$KEY_FILM_STOCK_ID = ?", filmStockId)
        .firstOrNull(filmStockMapper)

    val filmStocks: List<FilmStock> get() = database
        .from(TABLE_FILM_STOCKS)
        .orderBy("$KEY_FILM_MANUFACTURER_NAME collate nocase,$KEY_FILM_STOCK_NAME collate nocase")
        .map(filmStockMapper)

    fun isFilmStockBeingUsed(filmStock: FilmStock) = database
        .from(TABLE_ROLLS)
        .filter("$KEY_FILM_STOCK_ID = ?", filmStock.id)
        .firstOrNull { true } ?: false

    fun deleteFilmStock(filmStock: FilmStock) =
        database.writableDatabase
            .delete(TABLE_FILM_STOCKS, "$KEY_FILM_STOCK_ID=?", arrayOf(filmStock.id.toString()))

    fun updateFilmStock(filmStock: FilmStock): Int {
        val contentValues = buildFilmStockContentValues(filmStock)
        return database.writableDatabase
            .update(TABLE_FILM_STOCKS, contentValues, "$KEY_FILM_STOCK_ID=?", arrayOf(filmStock.id.toString()))
    }
}