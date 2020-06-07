package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

import java.util.ArrayList

@Parcelize
data class Frame(
        var id: Long = 0,
        var rollId: Long = 0,
        var count: Int = 0,
        var date: DateTime? = null,
        var shutter: String? = null,
        var aperture: String? = null,
        var note: String? = null,
        var location: Location? = null,
        var formattedAddress: String? = null,
        var focalLength: Int = 0,
        var exposureComp: String? = null,
        var noOfExposures: Int = 1,
        var flashUsed: Boolean = false,
        var flashPower: String? = null, // not used
        var flashComp: String? = null, // not used
        var meteringMode: Int = 0, // not used
        var pictureFilename: String? = null,
        private var lightSource_: Int = 0,
        var lens: Lens? = null,
        var filters: MutableList<Filter> = ArrayList()
) : Parcelable {

    init {
        if (lightSource_ !in 0..7) lightSource_ = 0
    }

    var lightSource: Int
        get() = if (lightSource_ in 0..7) lightSource_ else 0
        set(value) { if (value in 0..7) lightSource_ = value }

}