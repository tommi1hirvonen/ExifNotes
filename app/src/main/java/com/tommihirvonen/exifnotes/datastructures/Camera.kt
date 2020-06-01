package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

/**
 * The camera class holds the information of a camera.
 */
class Camera : Gear, Comparable<Gear> {

    constructor() : super()

    /**
     * serial number (can contain letters)
     */
    var serialNumber: String? = null


    /**
     * minimum shutter speed value (shortest possible duration) in format 1/X or Y"
     * where X and Y are numbers
     */
    var minShutter: String? = null


    /**
     * maximum shutter speed value (longest possible duration) in format 1/X or Y"
     * where X and Y are numbers
     */
    var maxShutter: String? = null

    /**
     * integer defining whether the shutter speed values can be changed in
     * third, half or full stop increments
     *
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    private var shutterIncrements = 0

    /**
     * Integer defining whether the exposure compensation values can be changed in
     * third or half stop increments.
     *
     * 0 = third stop (default)
     * 1 = half stop
     */
    private var exposureCompIncrements = 0

    /**
     *
     * @param input shutter speed value change increments
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    fun setShutterIncrements(input: Int) {
        if (input in 0..2) {
            this.shutterIncrements = input
        }
    }

    /**
     *
     * @param input exposure compensation value change increments
     * 0 = third stop (default)
     * 1 = half stop
     */
    fun setExposureCompIncrements(input: Int) {
        if (input in 0..1) {
            this.exposureCompIncrements = input
        }
    }

    /**
     *
     * @return shutter speed value change increments
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    fun getShutterIncrements(): Int {
        return this.shutterIncrements
    }

    /**
     *
     * @return exposure compensation value change increments
     * 0 = third stop (default)
     * 1 = half stop
     */
    fun getExposureCompIncrements(): Int {
        return this.exposureCompIncrements
    }

    private constructor(pc: Parcel) : super(pc) {
        this.serialNumber = pc.readString()
        this.minShutter = pc.readString()
        this.maxShutter = pc.readString()
        this.shutterIncrements = pc.readInt()
        this.exposureCompIncrements = pc.readInt()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeString(serialNumber)
        parcel.writeString(minShutter)
        parcel.writeString(maxShutter)
        parcel.writeInt(shutterIncrements)
        parcel.writeInt(exposureCompIncrements)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Camera> {
        override fun createFromParcel(parcel: Parcel): Camera {
            return Camera(parcel)
        }

        override fun newArray(size: Int): Array<Camera?> {
            return arrayOfNulls(size)
        }
    }

}
