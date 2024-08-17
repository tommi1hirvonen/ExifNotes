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

import android.content.Context
import androidx.annotation.Keep
import com.tommihirvonen.exifnotes.core.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(ExperimentalSerializationApi::class)
@Parcelize
@Serializable
@Keep
data class Lens(
    override var id: Long = 0,
    override var make: String? = null,
    override var model: String? = null,
    var serialNumber: String? = null,
    var minAperture: String? = null,
    var maxAperture: String? = null,
    var minFocalLength: Int = 0,
    var maxFocalLength: Int = 0,
    @EncodeDefault
    var apertureIncrements: Increment = Increment.Third,
    @Transient
    var filterIds: HashSet<Long> = HashSet(),
    @Transient
    var cameraIds: HashSet<Long> = HashSet(),
    @Transient
    var customApertureValues: List<Float> = emptyList()) : Gear(), Comparable<Gear> {

    fun apertureValues(context: Context): Array<String> =
        when (apertureIncrements) {
            Increment.Third -> context.resources.getStringArray(R.array.ApertureValuesThird)
            Increment.Half -> context.resources.getStringArray(R.array.ApertureValuesHalf)
            Increment.Full -> context.resources.getStringArray(R.array.ApertureValuesFull)
        }
            .reversed()
            .let { values ->
                val minIndex = values.indexOfFirst { it == minAperture }
                val maxIndex = values.indexOfFirst { it == maxAperture }
                if (minIndex != -1 && maxIndex != -1) {
                    values.filterIndexed { index, _ -> index in minIndex..maxIndex }
                            .plus(context.resources.getString(R.string.NoValue))
                } else {
                    values
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