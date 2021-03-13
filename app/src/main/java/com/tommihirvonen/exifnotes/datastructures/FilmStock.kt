package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import com.tommihirvonen.exifnotes.R
import kotlinx.parcelize.Parcelize

@Parcelize
class FilmStock(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var iso: Int = 0,
        private var type_: Int = 0,
        private var process_: Int = 0,
        var isPreadded: Boolean = false) : Gear(id, make, model), Comparable<Gear> {

    init {
        if (type_ !in 0..8) type_ = 0
        if (process_ !in 0..6) process_ = 0
    }

    var type: Int
        get() = type_
        set(value) { if (value in 0..8) type_ = value }

    var process: Int
        get() = process_
        set(value) { if (value in 0..6) process_ = value }

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