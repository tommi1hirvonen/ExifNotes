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

enum class RollFilterMode(value: Int) {
    ACTIVE(0),
    ARCHIVED(1),
    ALL(2);

    var value: Int = 0
        internal set

    init {
        this.value = value
    }

    companion object {

        fun fromValue(value: Int): RollFilterMode {
            return when (value) {
                0 -> ACTIVE
                1 -> ARCHIVED
                2 -> ALL
                else -> ACTIVE
            }
        }
    }
}
