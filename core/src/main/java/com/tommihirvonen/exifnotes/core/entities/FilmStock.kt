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

package com.tommihirvonen.exifnotes.core.entities

import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Parcelize
@Serializable
@Keep
data class FilmStock(
    override var id: Long = 0,
    override var make: String? = null,
    override var model: String? = null,
    var iso: Int = 0,
    @EncodeDefault
    var type: FilmType = FilmType.Unknown,
    @EncodeDefault
    var process: FilmProcess = FilmProcess.Unknown,
    @EncodeDefault
    var isPreadded: Boolean = false) : Gear(), Comparable<Gear>

fun List<FilmStock>.sorted(sortMode: FilmStockSortMode): List<FilmStock> =
    sortedWith(sortMode.comparator)