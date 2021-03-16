package com.tommihirvonen.exifnotes.datastructures

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
    : Gear(id, make, model), Comparable<Gear>