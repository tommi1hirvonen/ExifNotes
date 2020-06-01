package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

/**
 * Abstract super class for different types of gear.
 * Defines all common member variables and methods
 * as well as mandatory interfaces to implement.
 */
abstract class Gear : Parcelable, Comparable<Gear> {

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
    val name: String get() = this.make + " " + this.model

    override fun equals(other: Any?): Boolean {
        val gear: Gear
        if (other is Gear)
            gear = other
        else
            return false
        return gear.id == this.id && gear.make == this.make && gear.model == this.model
    }

    protected constructor(pc: Parcel) {
        this.id = pc.readLong()
        this.make = pc.readString()
        this.model = pc.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

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

    override fun compareTo(other: Gear): Int {
        return name.compareTo(other.name)
    }

}
