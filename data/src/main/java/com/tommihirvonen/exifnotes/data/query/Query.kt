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

package com.tommihirvonen.exifnotes.data.query

interface Query : Queryable {
    fun select(vararg columns: String): Queryable

    fun distinct(distinct: Boolean = true): Queryable

    fun orderBy(vararg columns: String): Queryable

    fun limit(limit: Int): Queryable

    fun where(predicate: String, vararg arguments: String): Queryable

    fun groupBy(vararg groupBy: String): AggregateQuery
}