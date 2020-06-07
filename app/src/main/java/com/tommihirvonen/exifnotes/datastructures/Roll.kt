package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Roll(var id: Long = 0,
           var name: String? = null,
           var date: DateTime? = null,
           var unloaded: DateTime? = null,
           var developed: DateTime? = null,
           var note: String? = null,
           var cameraId: Long = 0,
           var iso: Int = 0,
           var pushPull: String? = null,
           private var format_: Int = 0,
           var archived: Boolean = false,
           var filmStockId: Long = 0
           ) : Parcelable {

    init {
        if (format_ !in 0..3) format_ = 0
    }

    var format: Int
        get() = if (format_ in 0..3) format_ else 0
        set(value) { if (value in 0..3) format_ = value }

}