/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes

import com.tommihirvonen.exifnotes.entities.FilmProcess
import com.tommihirvonen.exifnotes.entities.FilmStock
import com.tommihirvonen.exifnotes.entities.FilmType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class FilmStockTest {
    @Test
    fun filmStock_serialize() {
        val filmStock = FilmStock(
            id = 123,
            make = "ILFORD",
            model = "HP5+",
            iso = 400,
            type = FilmType.BW_NEGATIVE,
            process = FilmProcess.BW_NEGATIVE,
            isPreadded = true
        )
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(filmStock)
        println(json)
    }
}