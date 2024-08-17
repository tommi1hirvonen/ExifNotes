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
            put(KEY_FILM_IS_PREADDED, filmStock.isPreAdded)
        }
    }

    fun addFilmStock(filmStock: FilmStock): FilmStock {
        val id = database.insert(TABLE_FILM_STOCKS, buildFilmStockContentValues(filmStock))
        return filmStock.copy(id = id)
    }

    private val filmStockMapper = { row: Cursor ->
        FilmStock(
            id = row.getLong(KEY_FILM_STOCK_ID),
            make = row.getStringOrNull(KEY_FILM_MANUFACTURER_NAME),
            model = row.getStringOrNull(KEY_FILM_STOCK_NAME),
            iso = row.getInt(KEY_FILM_ISO),
            type = FilmType.from(row.getInt(KEY_FILM_TYPE)),
            process = FilmProcess.from(row.getInt(KEY_FILM_PROCESS)),
            isPreAdded = row.getInt(KEY_FILM_IS_PREADDED) > 0
        )
    }

    fun getFilmStock(filmStockId: Long) = database
        .from(TABLE_FILM_STOCKS)
        .where { KEY_FILM_STOCK_ID eq filmStockId }
        .firstOrNull(filmStockMapper)

    val filmStocks: List<FilmStock> get() = database
        .from(TABLE_FILM_STOCKS)
        .orderBy {
            KEY_FILM_MANUFACTURER_NAME.asc().ignoreCase()
            KEY_FILM_STOCK_NAME.asc().ignoreCase()
        }
        .map(filmStockMapper)

    fun isFilmStockBeingUsed(filmStock: FilmStock) = database
        .from(TABLE_ROLLS)
        .where { KEY_FILM_STOCK_ID eq filmStock.id }
        .exists()

    fun deleteFilmStock(filmStock: FilmStock) = database
        .from(TABLE_FILM_STOCKS)
        .where { KEY_FILM_STOCK_ID eq filmStock.id }
        .delete()

    fun updateFilmStock(filmStock: FilmStock): Int {
        val contentValues = buildFilmStockContentValues(filmStock)
        return database
            .from(TABLE_FILM_STOCKS)
            .where { KEY_FILM_STOCK_ID eq filmStock.id }
            .update(contentValues)
    }
}