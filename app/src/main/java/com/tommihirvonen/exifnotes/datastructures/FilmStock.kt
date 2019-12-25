package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable

class FilmStock : Parcelable {

    var id: Long = 0

    var manufacturerName: String? = null

    var stockName: String? = null

    var iso: Int = 0

    fun getName() : String = "$manufacturerName $stockName"

    constructor()

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing film stock's information
     */
    private constructor(pc : Parcel) {
        this.id = pc.readLong()
        this.manufacturerName = pc.readString()
        this.stockName = pc.readString()
        this.iso = pc.readInt()
    }

    /**
     * Writes this object's members to a Parcel given as argument
     *
     * @param parcel Parcel which should be written with this object's members
     * @param flags not used
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(manufacturerName)
        parcel.writeString(stockName)
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