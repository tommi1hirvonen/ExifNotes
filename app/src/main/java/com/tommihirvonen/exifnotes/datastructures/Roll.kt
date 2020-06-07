package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Roll class holds the information of one roll of film.
 */
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
           var format: Int = 0,
           var archived: Boolean = false,
           var filmStockId: Long = 0
           ) : Parcelable