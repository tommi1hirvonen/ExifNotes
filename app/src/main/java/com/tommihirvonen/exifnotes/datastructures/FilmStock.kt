package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import com.tommihirvonen.exifnotes.R
import kotlinx.android.parcel.Parcelize

@Parcelize
class FilmStock(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var iso: Int = 0,
        var type: Int = 0,
        var process: Int = 0,
        var isPreadded: Boolean = false) : Gear(id, make, model), Comparable<Gear> {

    fun getTypeName(context: Context): String = try {
        context.resources.getStringArray(R.array.FilmTypes)[this.type]
    } catch (ignore: IndexOutOfBoundsException) {
        context.resources.getString(R.string.Unknown)
    }

    fun getProcessName(context: Context): String = try {
        context.resources.getStringArray(R.array.FilmProcesses)[this.process]
    } catch (ignore: IndexOutOfBoundsException) {
        context.resources.getString(R.string.Unknown)
    }

}