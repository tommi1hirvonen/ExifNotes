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

enum class RollSortMode constructor(value: Int) {
    DATE(0),
    NAME(1),
    CAMERA(2);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    val comparator: Comparator<Roll> get() = when (this) {
        DATE -> compareByDescending { roll ->
            roll.date
        }
        NAME -> compareByDescending<Roll> { roll ->
            val numberPrefixRegex = "(\\d+)[\\w\\s]*".toRegex()
            val result = numberPrefixRegex.matchEntire(roll.name ?: "")
            val numberPrefix = result?.groups?.get(1)?.value?.toIntOrNull()
            // Use descending order and reverse values
            // to make nulls (roll names with no number prefix) appear last in the list.
            numberPrefix?.let { -it }
        }.thenBy { roll ->
            roll.name
        }.thenByDescending { roll ->
            roll.developed ?: roll.unloaded ?: roll.date
        }
        CAMERA -> compareBy<Roll> { roll ->
            roll.camera
        }.thenByDescending { roll ->
            roll.developed ?: roll.unloaded ?: roll.date
        }
    }

    companion object {

        fun fromValue(value: Int): RollSortMode {
            return when (value) {
                0 -> DATE
                1 -> NAME
                2 -> CAMERA
                else -> DATE
            }
        }
    }
}
