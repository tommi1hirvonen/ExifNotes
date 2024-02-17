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

internal abstract class Comparison(
    private val column: String,
    private val operator: String,
    private val value: Any) : Condition {

    override val expression: String get() = "$column $operator ?"

    override val arguments: List<Any> get() = listOf(value)
}

internal class Equals private constructor(column: String, value: Any)
    : Comparison(column, "=", value) {
    constructor(column: String, value: Int) : this(column, value as Any)
    constructor(column: String, value: Long) : this(column, value as Any)
}

internal class Greater private constructor(column: String, value: Any)
    : Comparison(column, ">", value) {
    constructor(column: String, value: Int) : this(column, value as Any)
    constructor(column: String, value: Long) : this(column, value as Any)
}

internal class Less private constructor(column: String, value: Any)
    : Comparison(column, "<", value) {
    constructor(column: String, value: Int) : this(column, value as Any)
    constructor(column: String, value: Long) : this(column, value as Any)
}

internal class IsNull(private val column: String) : Condition {
    override val expression: String get() = "$column is null"
    override val arguments: List<Any> get() = emptyList()
}

internal class IsNotNull(private val column: String) : Condition {
    override val expression: String get() = "$column is not null"
    override val arguments: List<Any> get() = emptyList()
}