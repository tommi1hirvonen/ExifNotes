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
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import java.util.ArrayList

@Parcelize
data class Frame(
    var id: Long = 0,
    var roll: Roll,
    var count: Int = 0,
    var date: DateTime? = null,
    var shutter: String? = null,
    var aperture: String? = null,
    var note: String? = null,
    var location: Location? = null,
    var formattedAddress: String? = null,
    var focalLength: Int = 0,
    var exposureComp: String? = null,
    var noOfExposures: Int = 1,
    var flashUsed: Boolean = false,
    var flashPower: String? = null, // not used
    var flashComp: String? = null, // not used
    var meteringMode: Int = 0, // not used
    var pictureFilename: String? = null,
    var lightSource: LightSource = LightSource.UNKNOWN,
    var lens: Lens? = null,
    var filters: List<Filter> = ArrayList()
) : Parcelable {

    constructor(roll_: Roll) : this(roll = roll_)

    override fun equals(other: Any?) = other is Frame && other.id == id

    override fun hashCode() = id.hashCode()
}

fun List<Frame>.sorted(context: Context, sortMode: FrameSortMode): List<Frame> =
    sortedWith(sortMode.getComparator(context))