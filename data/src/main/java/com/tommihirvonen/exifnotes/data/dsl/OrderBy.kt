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

internal class OrderByBuilder {
    private val columns = mutableListOf<OrderByColumn>()

    val expression = if (columns.isEmpty()) {
        null
    } else {
        columns.joinToString(", ") { it.expression }
    }

    fun String.asc(): OrderByColumn {
        val orderBy = OrderByColumn(this, false)
        columns.add(orderBy)
        return orderBy
    }

    fun String.desc(): OrderByColumn {
        val orderBy = OrderByColumn(this, true)
        columns.add(orderBy)
        return orderBy
    }
}

internal data class OrderByColumn(
    private val column: String,
    private val descending: Boolean = false,
    private var ignoreCase: Boolean = false) {
    val expression get() = "$column $direction $collate"
    private val direction get() = if (descending) "desc" else "asc"
    private val collate get() = if (ignoreCase) "collate nocase" else ""
    fun ignoreCase() {
        ignoreCase = true
    }
}