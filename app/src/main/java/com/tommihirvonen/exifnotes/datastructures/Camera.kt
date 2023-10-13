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
import androidx.annotation.Keep
import com.tommihirvonen.exifnotes.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(ExperimentalSerializationApi::class)
@Parcelize
@Serializable
@Keep
data class Camera(
    override var id: Long = 0,
    override var make: String? = null,
    override var model: String? = null,
    var serialNumber: String? = null,
    var minShutter: String? = null,
    var maxShutter: String? = null,
    @EncodeDefault
    var shutterIncrements: Increment = Increment.THIRD,
    @EncodeDefault
    var exposureCompIncrements: PartialIncrement = PartialIncrement.THIRD,
    @EncodeDefault
    var format: Format = Format.MM35,
    var lens: Lens? = null,
    @Transient
    var lensIds: HashSet<Long> = HashSet()) : Gear(), Comparable<Gear> {

    val isFixedLens get() = lens != null
    val isNotFixedLens get() = lens == null

    fun shutterSpeedValues(context: Context): Array<String> =
        when (shutterIncrements) {
            Increment.THIRD -> context.resources.getStringArray(R.array.ShutterValuesThird)
            Increment.HALF -> context.resources.getStringArray(R.array.ShutterValuesHalf)
            Increment.FULL -> context.resources.getStringArray(R.array.ShutterValuesFull)
        }.reversed().let { values ->
            val minIndex = values.indexOfFirst { it == minShutter }
            val maxIndex = values.indexOfFirst { it == maxShutter }
            if (minIndex != -1 && maxIndex != -1) {
                values.filterIndexed { index, _ -> index in minIndex..maxIndex }
                        .plus("B")
                        .plus(context.resources.getString(R.string.NoValue))
            } else {
                values.toMutableList().also { it.add(it.size - 1, "B") }
            }
        }.toTypedArray()

    fun exposureCompValues(context: Context): Array<String> =
        when (exposureCompIncrements) {
            PartialIncrement.THIRD -> context.resources.getStringArray(R.array.CompValues)
            PartialIncrement.HALF -> context.resources.getStringArray(R.array.CompValuesHalf)
        }.reversedArray()

    companion object {
        fun defaultShutterSpeedValues(context: Context): Array<String> =
            context.resources.getStringArray(R.array.ShutterValuesThird)
                    .reversed()
                    .toMutableList()
                    .also{ it.add(it.size - 1, "B") }
                    .toTypedArray()

        fun defaultExposureCompValues(context: Context): Array<String> =
                context.resources.getStringArray(R.array.CompValues).reversedArray()
    }
}