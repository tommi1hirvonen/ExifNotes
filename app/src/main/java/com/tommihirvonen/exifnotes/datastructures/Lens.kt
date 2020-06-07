package com.tommihirvonen.exifnotes.datastructures

import kotlinx.android.parcel.Parcelize

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
        var apertureIncrements: Int = 0) : Gear(id, make, model), Comparable<Gear>
