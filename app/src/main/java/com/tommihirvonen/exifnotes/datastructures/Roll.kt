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
    var date: DateTime? = null

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
     * Date to store when the roll was unloaded from the camera
     */
    var unloaded: DateTime? = null

    /**
     * Date to store when the roll was developed
     */
    var developed: DateTime? = null

    /**
     *
     * @param input greater than zero if true, zero or smaller if false
     */
    fun setArchived(input: Int) {
        this.archived = input > 0
    }

    private constructor(pc: Parcel) {
        this.id = pc.readLong()
        this.name = pc.readString()
        this.date = pc.readString()?.let { DateTime(it) }
        this.note = pc.readString()
        this.cameraId = pc.readLong()
        this.iso = pc.readInt()
        this.pushPull = pc.readString()
        this.format = pc.readInt()
        this.archived = pc.readInt() == 1
        this.filmStockId = pc.readLong()
        this.unloaded = pc.readString()?.let { DateTime(it) }
        this.developed = pc.readString()?.let { DateTime(it) }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(date.toString())
        parcel.writeString(note)
        parcel.writeLong(cameraId)
        parcel.writeInt(iso)
        parcel.writeString(pushPull)
        parcel.writeInt(format)
        parcel.writeInt(if (archived) 1 else 0)
        parcel.writeLong(filmStockId)
        parcel.writeString(unloaded.toString())
        parcel.writeString(developed.toString())
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
