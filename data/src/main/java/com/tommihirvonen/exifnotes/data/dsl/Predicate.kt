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

internal class Predicate : Condition {
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
    infix fun String.gt(value: Int) = conditions.add(Greater(this, value))
    infix fun String.gt(value: Long) = conditions.add(Greater(this, value))
    infix fun String.lt(value: Int) = conditions.add(Less(this, value))
    infix fun String.lt(value: Long) = conditions.add(Less(this, value))
    fun String.isNull() = conditions.add(IsNull(this))
    fun String.isNotNull() = conditions.add(IsNotNull(this))
    infix fun String.`in`(subQuery: () -> SubQuery) =
        conditions.add(InSubQuery(this, false, subQuery()))
    infix fun String.notIn(subQuery: () -> SubQuery) =
        conditions.add(InSubQuery(this, true, subQuery()))
}