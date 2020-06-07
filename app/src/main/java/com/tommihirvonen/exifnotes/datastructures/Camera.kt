package com.tommihirvonen.exifnotes.datastructures

import kotlinx.android.parcel.Parcelize

@Parcelize
data class Camera(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var serialNumber: String? = null,
        var minShutter: String? = null,
        var maxShutter: String? = null,
        var shutterIncrements: Int = 0,
        var exposureCompIncrements: Int = 0) : Gear(id, make, model), Comparable<Gear>