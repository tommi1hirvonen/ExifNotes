package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import com.tommihirvonen.exifnotes.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class Lens(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var serialNumber: String? = null,
        var minAperture: String? = null,
        var maxAperture: String? = null,
        var minFocalLength: Int = 0,
        var maxFocalLength: Int = 0,
        var apertureIncrements: Increment = Increment.THIRD)
    : Gear(id, make, model), Comparable<Gear> {

    fun apertureValues(context: Context): Array<String> =
        when (apertureIncrements) {
            Increment.THIRD -> context.resources.getStringArray(R.array.ApertureValuesThird)
            Increment.HALF -> context.resources.getStringArray(R.array.ApertureValuesHalf)
            Increment.FULL -> context.resources.getStringArray(R.array.ApertureValuesFull)
        }.let {
            if (it[0] == context.resources.getString(R.string.NoValue)) {
                it.reversed()
            } else {
                it.toList()
            }
        }.let {
            val minIndex = it.indexOfFirst { it_ -> it_ == minAperture }
            val maxIndex = it.indexOfFirst { it_ -> it_ == maxAperture }
            if (minIndex != -1 && maxIndex != -1) {
                it.filterIndexed { index, _ -> index in minIndex..maxIndex }
                        .plus(context.resources.getString(R.string.NoValue))
            } else {
                it
            }
        }.toTypedArray()

    companion object {
        fun defaultApertureValues(context: Context): Array<String> =
                context.resources.getStringArray(R.array.ApertureValuesThird)
    }
}