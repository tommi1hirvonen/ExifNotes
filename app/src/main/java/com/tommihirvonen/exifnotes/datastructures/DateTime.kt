package com.tommihirvonen.exifnotes.datastructures

import java.util.Calendar


class DateTime {

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

    constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        this.year = year
        this.month = month
        this.day = day
        this.hour = hour
        this.minute = minute
    }

    constructor(dateTimeString: String) {
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
            throw IllegalArgumentException("Invalid dateTimeString argument. Date part parsing failed.")
        }
    }

    val timeAsText: String get() = if (minute < 10) "$hour:0$minute" else "$hour:$minute"
    val dateAsText: String get() = "$year-$month-$day"
    val dateTimeAsText: String get() = this.toString()

    override fun toString(): String {
        return if (minute < 10) {
            "$year-$month-$day $hour:0$minute"
        } else {
            "$year-$month-$day $hour:$minute"
        }
    }

}