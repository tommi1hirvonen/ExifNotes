package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

/**
 * Abstract super class for different types of gear.
 * Defines all common member variables and methods
 * as well as mandatory interfaces to implement.
 */
abstract class Gear : Parcelable {

    constructor()

    /**
     * unique database id
     */
    var id: Long = 0

    /**
     * make of the Gear
     */
    var make: String? = null

    /**
     *  model of the Gear
     */
    var model: String? = null

    /**
     * @return make and model of the Gear concatenated
     */
    val name: String
        get() = this.make + " " + this.model

    /**
     * Method used to compare two instances of Gear.
     * Used when a collection of Gears is being sorted.
     *
     * @param other object that is an instance of Gear
     * @return true if the two instances are copies of each other
     */
    override fun equals(other: Any?): Boolean {
        val gear: Gear
        if (other is Gear)
            gear = other
        else
            return false
        return gear.id == this.id && gear.make == this.make && gear.model == this.model
    }

    /**
     * Constructs Gear from Parcel
     *
     * @param pc Parcel object containing Gear's information
     */
    protected constructor(pc: Parcel) {
        this.id = pc.readLong()
        this.make = pc.readString()
        this.model = pc.readString()
    }

    /**
     * Required by the Parcelable interface. Not used.
     *
     * @return 0
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Writes this object's members to a Parcel given as argument
     *
     * @param parcel Parcel which should be written with this object's members
     * @param i not used
     */
    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(id)
        parcel.writeString(make)
        parcel.writeString(model)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (make?.hashCode() ?: 0)
        result = 31 * result + (model?.hashCode() ?: 0)
        return result
    }

}
