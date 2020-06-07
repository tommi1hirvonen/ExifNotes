package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

import java.util.ArrayList

/**
 * The frame class holds the information of one frame.
 */
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
        var flashPower: String? = null,
        var flashComp: String? = null,
        var meteringMode: Int = 0,
        var pictureFilename: String? = null,
        var lightSource: Int = 0,
        var lens: Lens? = null,
        var filters: MutableList<Filter> = ArrayList()
) : Parcelable