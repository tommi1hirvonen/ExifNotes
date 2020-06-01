package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

/**
 * Lens class holds the information of one lens.
 */
class Lens : Gear, Parcelable, Comparable<Gear> {

    constructor() : super()

    /**
     * serial number
     */
    var serialNumber: String? = null

    /**
     * minimum aperture (highest f-number), number only
     */
    var minAperture: String? = null

    /**
     * maximum aperture (lowest f-number), number only
     */
    var maxAperture: String? = null

    /**
     * minimum focal length
     */
    var minFocalLength: Int = 0

    /**
     * maximum focal length
     */
    var maxFocalLength: Int = 0

    /**
     * integer defining whether the aperture values can be changed in
     * third, half or full stop increments
     *
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    private var apertureIncrements = 0

    /**
     *
     * @param input aperture value change increments
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    fun setApertureIncrements(input: Int) {
        if (input in 0..2) {
            this.apertureIncrements = input
        }
    }

    /**
     *
     * @return aperture value change increments
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    fun getApertureIncrements(): Int {
        return this.apertureIncrements
    }

    private constructor(pc: Parcel) : super(pc) {
        this.serialNumber = pc.readString()
        this.minAperture = pc.readString()
        this.maxAperture = pc.readString()
        this.minFocalLength = pc.readInt()
        this.maxFocalLength = pc.readInt()
        this.apertureIncrements = pc.readInt()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeString(serialNumber)
        parcel.writeString(minAperture)
        parcel.writeString(maxAperture)
        parcel.writeInt(minFocalLength)
        parcel.writeInt(maxFocalLength)
        parcel.writeInt(apertureIncrements)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Lens> {
        override fun createFromParcel(parcel: Parcel): Lens {
            return Lens(parcel)
        }

        override fun newArray(size: Int): Array<Lens?> {
            return arrayOfNulls(size)
        }
    }

}
