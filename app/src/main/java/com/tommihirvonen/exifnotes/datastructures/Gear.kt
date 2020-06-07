package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcelable

/**
 * Abstract super class for different types of gear.
 * Defines all common member variables and methods
 * as well as mandatory interfaces to implement.
 */
abstract class Gear(open var id: Long, open var make: String?, open var model: String?) : Parcelable, Comparable<Gear> {

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
