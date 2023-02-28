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
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Lens(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var serialNumber: String? = null,
        var minAperture: String? = null,
        var maxAperture: String? = null,
        var minFocalLength: Int = 0,
        var maxFocalLength: Int = 0,
        var apertureIncrements: Increment = Increment.THIRD,
        var filterIds: HashSet<Long> = HashSet(),
        var cameraIds: HashSet<Long> = HashSet(),
        var customApertureValues: List<Float> = emptyList())
    : Gear(), Comparable<Gear> {

    fun apertureValues(context: Context): Array<String> =
        when (apertureIncrements) {
            Increment.THIRD -> context.resources.getStringArray(R.array.ApertureValuesThird)
            Increment.HALF -> context.resources.getStringArray(R.array.ApertureValuesHalf)
            Increment.FULL -> context.resources.getStringArray(R.array.ApertureValuesFull)
        }
            .reversed()
            .let {
                val minIndex = it.indexOfFirst { it_ -> it_ == minAperture }
                val maxIndex = it.indexOfFirst { it_ -> it_ == maxAperture }
                if (minIndex != -1 && maxIndex != -1) {
                    it.filterIndexed { index, _ -> index in minIndex..maxIndex }
                            .plus(context.resources.getString(R.string.NoValue))
                } else {
                    it
                }
            }
            .plus(customApertureValues.map(Float::toString))
            .sortedBy(String::toFloatOrNull)
            .toTypedArray()

    companion object {
        fun defaultApertureValues(context: Context): Array<String> =
                context.resources.getStringArray(R.array.ApertureValuesThird)
    }
}