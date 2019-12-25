package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

/**
 * Roll class holds the information of one roll of film.
 */
class Roll : Parcelable {

    /**
     * empty constructor
     */
    constructor()

    /**
     * database id
     */
    var id: Long = 0

    /**
     * name/title of the roll
     */
    var name: String? = null

    /**
     * datetime when the film roll was loaded, for example
     * in format 'YYYY-M-D H:MM'
     */
    var date: String? = null

    /**
     * custom note
     */
    var note: String? = null

    /**
     * used camera's database id
     */
    var cameraId: Long = 0

    /** ISO value
     */
    var iso: Int = 0

    /**
     * push or pull in format 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    var pushPull: String? = null

    /**
     * film format
     * corresponding values defined in res/values/array.xml
     */
    var format: Int = 0

    /**
     * true if roll is archived, false if not
     */
    var archived = false

    /**
     * Id of the film type used for roll
     */
    var filmStockId: Long = 0

    /**
     *
     * @param input greater than zero if true, zero or smaller if false
     */
    fun setArchived(input: Int) {
        this.archived = input > 0
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing Camera's information
     */
    private constructor(pc: Parcel) {
        this.id = pc.readLong()
        this.name = pc.readString()
        this.date = pc.readString()
        this.note = pc.readString()
        this.cameraId = pc.readLong()
        this.iso = pc.readInt()
        this.pushPull = pc.readString()
        this.format = pc.readInt()
        this.archived = pc.readInt() == 1
        this.filmStockId = pc.readLong()
    }

    /**
     * Not used
     *
     * @return not used
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
        parcel.writeString(name)
        parcel.writeString(date)
        parcel.writeString(note)
        parcel.writeLong(cameraId)
        parcel.writeInt(iso)
        parcel.writeString(pushPull)
        parcel.writeInt(format)
        parcel.writeInt(if (archived) 1 else 0)
        parcel.writeLong(filmStockId)
    }

        companion object CREATOR : Parcelable.Creator<Roll> {
        override fun createFromParcel(parcel: Parcel): Roll {
            return Roll(parcel)
        }

        override fun newArray(size: Int): Array<Roll?> {
            return arrayOfNulls(size)
        }
    }
}
