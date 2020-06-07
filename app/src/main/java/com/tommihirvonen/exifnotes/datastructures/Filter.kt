package com.tommihirvonen.exifnotes.datastructures

import kotlinx.android.parcel.Parcelize

/**
 * Filter class holds the information of a photographic filter.
 */
@Parcelize
class Filter(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null) : Gear(id, make, model),Comparable<Gear>
