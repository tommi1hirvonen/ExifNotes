package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import com.tommihirvonen.exifnotes.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class Camera(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var serialNumber: String? = null,
        var minShutter: String? = null,
        var maxShutter: String? = null,
        var shutterIncrements: Increment = Increment.THIRD,
        var exposureCompIncrements: PartialIncrement = PartialIncrement.THIRD)
    : Gear(id, make, model), Comparable<Gear> {

    fun shutterSpeedValues(context: Context): Array<String> =
        when (shutterIncrements) {
            Increment.THIRD -> context.resources.getStringArray(R.array.ShutterValuesThird)
            Increment.HALF -> context.resources.getStringArray(R.array.ShutterValuesHalf)
            Increment.FULL -> context.resources.getStringArray(R.array.ShutterValuesFull)
        }.reversed().let {
            val minIndex = it.indexOfFirst { it_ -> it_ == minShutter }
            val maxIndex = it.indexOfFirst { it_ -> it_ == maxShutter }
            if (minIndex != -1 && maxIndex != -1) {
                it.filterIndexed { index, _ -> index in minIndex..maxIndex }
                        .plus("B")
                        .plus(context.resources.getString(R.string.NoValue))
            } else {
                it.plus("B")
            }
        }.toTypedArray()

    companion object {
        fun defaultShutterSpeedValues(context: Context): Array<String> =
            context.resources.getStringArray(R.array.ShutterValuesThird)
                    .reversed()
                    .toMutableList()
                    .also{ it.add(it.size - 1, "B") }
                    .toTypedArray()
    }
}