package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

class FilmStock : Gear, Parcelable {

    var iso: Int = 0

    constructor()

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing film stock's information
     */
    private constructor(pc : Parcel) {
        this.id = pc.readLong()
        this.make = pc.readString()
        this.model = pc.readString()
        this.iso = pc.readInt()
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
        parcel.writeInt(iso)
    }

    /**
     * Not used
     */
    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FilmStock> {
        override fun createFromParcel(parcel: Parcel): FilmStock {
            return FilmStock(parcel)
        }

        override fun newArray(size: Int): Array<FilmStock?> {
            return arrayOfNulls(size)
        }
    }


}