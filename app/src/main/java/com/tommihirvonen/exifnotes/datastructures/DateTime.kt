/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.datastructures

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar

@Parcelize
class DateTime(
        var year: Int = 0, var month: Int = 0, var day: Int = 0,
        var hour: Int = 0, var minute: Int = 0) : Comparable<DateTime>, Parcelable {

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

    companion object {
        fun fromCurrentTime(): DateTime {
            val c = Calendar.getInstance()
            val year = c[Calendar.YEAR]
            val month = c[Calendar.MONTH] + 1
            val day = c[Calendar.DAY_OF_MONTH]
            val hour = c[Calendar.HOUR_OF_DAY]
            val minute = c[Calendar.MINUTE]
            return DateTime(year, month, day, hour, minute)
        }
    }

}