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

internal class InSubQuery(
    private val column: String,
    private val notIn: Boolean,
    private val inColumn: String,
    private val inTable: String,
    private val inPredicate: Predicate) : Condition {
    constructor(column: String, notIn: Boolean, subQuery: SubQuery)
            : this(column, notIn, subQuery.column, subQuery.table, subQuery.predicate)
    private val operator get() = if (notIn) "not in" else "in"
    override val expression: String get() = """
        |$column $operator (
        |   select $inColumn
        |   from $inTable
        |   where ${inPredicate.expression}
        |)
    """.trimMargin()
    override val arguments: List<Any> get() = inPredicate.arguments
}

internal data class SubQuery(val table: String, val column: String, val predicate: Predicate)

internal data class SubQueryTable(val table: String)

internal fun from(table: String) = SubQueryTable(table)

internal fun SubQueryTable.select(column: String) = SubQuery(table, column, Predicate())

internal fun SubQuery.where(block: (Predicate.() -> Any)): SubQuery {
    val predicate = Predicate()
    block(predicate)
    return copy(predicate = predicate)
}