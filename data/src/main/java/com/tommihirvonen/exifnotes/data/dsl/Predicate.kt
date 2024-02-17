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

interface Condition {
    val expression: String
    val arguments: List<Any>
}

class Predicate : Condition {
    override val expression: String get() = if (conditions.isEmpty()) {
        "1 = 1"
    } else if (conditions.size == 1) {
        conditions.first().expression
    } else {
        conditions.joinToString(prefix = "(", postfix = ")", separator = " and ") { it.expression }
    }

    override val arguments: List<Any> get() = conditions.flatMap { it.arguments }

    private val conditions = mutableListOf<Condition>()

    infix fun String.eq(value: Int) = conditions.add(Equals(this, value))

    infix fun String.eq(value: Long) = conditions.add(Equals(this, value))
}

class Equals private constructor(private val column: String, private val value: Any) : Condition {

    constructor(column: String, value: Int) : this(column, value as Any)

    constructor(column: String, value: Long) : this(column, value as Any)

    override val expression: String get() = "$column = ?"

    override val arguments: List<Any> get() = listOf(value)
}