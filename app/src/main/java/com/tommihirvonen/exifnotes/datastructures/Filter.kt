package com.tommihirvonen.exifnotes.datastructures

import kotlinx.parcelize.Parcelize

@Parcelize
class Filter(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null) : Gear(id, make, model),Comparable<Gear>
