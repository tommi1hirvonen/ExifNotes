package com.tommihirvonen.exifnotes.datastructures

import kotlinx.parcelize.Parcelize

@Parcelize
data class Camera(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var serialNumber: String? = null,
        var minShutter: String? = null,
        var maxShutter: String? = null,
        private var shutterIncrements_: Int = 0,
        private var exposureCompIncrements_: Int = 0) : Gear(id, make, model), Comparable<Gear> {

    init {
        if (shutterIncrements_ !in 0..2) shutterIncrements_ = 0
        if (exposureCompIncrements_ !in 0..1) exposureCompIncrements_ = 0
    }

    var shutterIncrements: Int
        get() = shutterIncrements_
        set(value) { if (value in 0..2) shutterIncrements_ = value }

    var exposureCompIncrements: Int
        get() = exposureCompIncrements_
        set(value) { if (value in 0..1) exposureCompIncrements_ = value }

}