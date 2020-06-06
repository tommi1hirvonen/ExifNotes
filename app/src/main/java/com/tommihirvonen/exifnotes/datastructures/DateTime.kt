package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcel
import android.os.Parcelable
import java.util.Calendar

class DateTime() : Comparable<DateTime>, Parcelable {

    var year: Int = 0
        private set
    var month: Int = 0
        private set
    var day: Int = 0
        private set
    var hour: Int = 0
        private set
    var minute: Int = 0
        private set

    constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int) : this() {
        this.year = year
        this.month = month
        this.day = day
        this.hour = hour
        this.minute = minute
    }

    constructor(dateTimeString: String) : this() {
        try {
            val date = dateTimeString.split(" ")[0]
            val time = dateTimeString.split(" ")[1]
            val (year, month, day) = date.split("-")
            val (hour, minute) = time.split(":")
            this.year = year.toInt()
            this.month = month.toInt()
            this.day = day.toInt()
            this.hour = hour.toInt()
            this.minute = minute.toInt()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid dateTimeString argument: $dateTimeString")
        }
    }

    private fun Int.pad(): String = this.toString().padStart(2, '0')

    val timeAsText: String get() =
        "${hour.pad()}:${minute.pad()}"

    val dateAsText: String get() =
        "$year-${month.pad()}-${day.pad()}"

    val dateTimeAsText: String get() = this.toString()

    override fun toString(): String {
        return "$dateAsText $timeAsText"
    }

    override fun compareTo(other: DateTime): Int =
            when {
                year != other.year -> year - other.year
                month != other.month -> month - other.month
                day != other.day -> day - other.day
                hour != other.hour -> hour - other.hour
                else -> minute - other.minute
            }

    constructor(parcel: Parcel) : this() {
        year = parcel.readInt()
        month = parcel.readInt()
        day = parcel.readInt()
        hour = parcel.readInt()
        minute = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(year)
        parcel.writeInt(month)
        parcel.writeInt(day)
        parcel.writeInt(hour)
        parcel.writeInt(minute)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DateTime> {
        fun fromCurrentTime(): DateTime {
            val c = Calendar.getInstance()
            val year = c[Calendar.YEAR]
            val month = c[Calendar.MONTH] + 1
            val day = c[Calendar.DAY_OF_MONTH]
            val hour = c[Calendar.HOUR_OF_DAY]
            val minute = c[Calendar.MINUTE]
            return DateTime(year, month, day, hour, minute)
        }

        override fun createFromParcel(parcel: Parcel): DateTime {
            return DateTime(parcel)
        }

        override fun newArray(size: Int): Array<DateTime?> {
            return arrayOfNulls(size)
        }
    }

}