/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import com.tommihirvonen.exifnotes.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(ExperimentalSerializationApi::class)
@Parcelize
@Serializable
data class FilmStock(
    override var id: Long = 0,
    override var make: String? = null,
    override var model: String? = null,
    var iso: Int = 0,
    @EncodeDefault
    var type: FilmType = FilmType.UNKNOWN,
    @SerialName("process")
    @EncodeDefault
    private var process_: Int = 0,
    @Transient
    var isPreadded: Boolean = false) : Gear(), Comparable<Gear> {

    init {
        if (process_ !in 0..6) process_ = 0
    }

    var process: Int
        get() = process_
        set(value) { if (value in 0..6) process_ = value }

    fun getProcessName(context: Context): String = try {
        context.resources.getStringArray(R.array.FilmProcesses)[this.process]
    } catch (ignore: IndexOutOfBoundsException) {
        context.resources.getString(R.string.Unknown)
    }

}

fun List<FilmStock>.sorted(sortMode: FilmStockSortMode): List<FilmStock> =
    sortedWith(sortMode.comparator)