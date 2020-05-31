package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.tommihirvonen.exifnotes.R

class FilmStock : Gear, Parcelable {

    var iso: Int = 0

    var type: Int = 0

    var process: Int = 0

    var isPreadded: Boolean = false

    constructor()

    fun getTypeName(context: Context): String =
            try {
                context.resources.getStringArray(R.array.FilmTypes)[this.type]
            } catch (ignore: IndexOutOfBoundsException) {
                context.resources.getString(R.string.Unknown)
            }

    fun getProcessName(context: Context): String =
            try {
                context.resources.getStringArray(R.array.FilmProcesses)[this.process]
            } catch (ignore: IndexOutOfBoundsException) {
                context.resources.getString(R.string.Unknown)
            }

    fun setPreadded(input: Int) {
        isPreadded = input == 1
    }

    private constructor(pc : Parcel) {
        this.id = pc.readLong()
        this.make = pc.readString()
        this.model = pc.readString()
        this.iso = pc.readInt()
        this.type = pc.readInt()
        this.process = pc.readInt()
        this.isPreadded = pc.readInt() == 1
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(id)
        parcel.writeString(make)
        parcel.writeString(model)
        parcel.writeInt(iso)
        parcel.writeInt(type)
        parcel.writeInt(process)
        parcel.writeInt(if (isPreadded) 1 else 0)
    }

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