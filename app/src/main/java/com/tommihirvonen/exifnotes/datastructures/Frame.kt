package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList

/**
 * The frame class holds the information of one frame.
 */
class Frame : Parcelable {

    /**
     * empty constructor
     */
    constructor()

    /**
     * database id
     */
    var id: Long = 0

    /**
     * database id of the roll to which this frame belongs
     */
    var rollId: Long = 0

    /**
     * frame count number
     */
    var count: Int = 0

    /**
     * datetime of exposure in format 'YYYY-M-D H:MM'
     */
    var date: DateTime? = null

    /**
     * database id of the lens used
     */
    var lensId: Long = 0

    /**
     * shutter speed value in format 1/X, Y" or B, where X and Y are numbers
     */
    var shutter: String? = null

    /**
     * aperture value, number only
     */
    var aperture: String? = null

    /**
     * custom note
     */
    var note: String? = null

    /**
     * latitude and longitude in format '12,3456... 12,3456...'
     */
    var location: Location? = null

    /**
     * formatted address
     */
    var formattedAddress: String? = null

    /**
     * lens's focal length
     */
    var focalLength: Int = 0

    /**
     * used exposure compensation in format 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    var exposureComp: String? = null

    /**
     * number of exposures on this frame (multiple exposure)
     */
    var noOfExposures: Int = 0

    /**
     * Boolean to represent whether flash was used or not.
     */
    var flashUsed = false

    /**
     * NOT YET IN USE
     */
    var flashPower: String? = null

    /**
     * NOT YET IN USE
     */
    var flashComp: String? = null

    /**
     * NOT YET IN USE
     */
    var meteringMode: Int = 0

    /**
     * relative (not absolute) filename of the complementary picture
     */
    var pictureFilename: String? = null

    /**
     * list of Filter objects linked to this frame
     */
    var filters: List<Filter> = ArrayList()

    /**
     * Light source of the picture.
     * Corresponding values are defined in res/values/array.xml (LightSource)
     */
    var lightSource: Int = 0

    fun setFlashUsed(input: Int) {
        this.flashUsed = input > 0
    }

    private constructor(pc: Parcel) {
        this.id = pc.readLong()
        this.rollId = pc.readLong()
        this.count = pc.readInt()
        this.date = pc.readParcelable(DateTime::class.java.classLoader)
        this.lensId = pc.readLong()
        this.shutter = pc.readString()
        this.aperture = pc.readString()
        this.note = pc.readString()
        this.location = pc.readParcelable(Location::class.java.classLoader)
        this.formattedAddress = pc.readString()
        this.focalLength = pc.readInt()
        this.exposureComp = pc.readString()
        this.noOfExposures = pc.readInt()
        this.flashUsed = pc.readInt() == 1
        this.flashPower = pc.readString()
        this.flashComp = pc.readString()
        this.meteringMode = pc.readInt()
        this.pictureFilename = pc.readString()
        this.lightSource = pc.readInt()
        val filtersAmount = pc.readInt()
        val filters = ArrayList<Filter>()
        for (i in 0 until filtersAmount) {
            filters.add(pc.readParcelable<Parcelable>(Filter::class.java.classLoader) as Filter)
        }
        this.filters = filters
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(id)
        parcel.writeLong(rollId)
        parcel.writeInt(count)
        parcel.writeParcelable(date, 0)
        parcel.writeLong(lensId)
        parcel.writeString(shutter)
        parcel.writeString(aperture)
        parcel.writeString(note)
        parcel.writeParcelable(location, 0)
        parcel.writeString(formattedAddress)
        parcel.writeInt(focalLength)
        parcel.writeString(exposureComp)
        parcel.writeInt(noOfExposures)
        parcel.writeInt(if (flashUsed) 1 else 0)
        parcel.writeString(flashPower)
        parcel.writeString(flashComp)
        parcel.writeInt(meteringMode)
        parcel.writeString(pictureFilename)
        parcel.writeInt(lightSource)
        parcel.writeInt(filters.size)
        for (filter in filters) parcel.writeParcelable(filter, 0)
    }

    companion object CREATOR : Parcelable.Creator<Frame> {
        override fun createFromParcel(parcel: Parcel): Frame {
            return Frame(parcel)
        }

        override fun newArray(size: Int): Array<Frame?> {
            return arrayOfNulls(size)
        }
    }
}
