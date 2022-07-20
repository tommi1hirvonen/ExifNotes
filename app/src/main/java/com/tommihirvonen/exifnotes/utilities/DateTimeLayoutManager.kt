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

package com.tommihirvonen.exifnotes.utilities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.View
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.datastructures.DateTime.Companion.fromCurrentTime

/**
 * Helper class to manage the date and time layout onClick events.
 * When the date layout is clicked, a date picker dialog is shown.
 * When the time layout is clicked, a time picker dialog is shown.
 * The DateTime member is managed inside the class.
 */
class DateTimeLayoutManager(
        activity: Activity,
        dateLayout: View,
        timeLayout: View,
        dateTextView: TextView,
        timeTextView: TextView,
        var dateTime: DateTime?,
        clearLayout: View?
) {

    init {
        dateLayout.setOnClickListener {
            val dateTimeTemp = dateTime ?: fromCurrentTime()
            val dialog = DatePickerDialog(activity, { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                val dateTime = DateTime(year, month + 1, dayOfMonth, dateTimeTemp.hour, dateTimeTemp.minute)
                dateTextView.text = dateTime.dateAsText
                timeTextView.text = dateTime.timeAsText
                this.dateTime = dateTime
            }, dateTimeTemp.year, dateTimeTemp.month - 1, dateTimeTemp.day)
            dialog.show()
        }

        timeLayout.setOnClickListener {
            val dateTimeTemp = dateTime ?: fromCurrentTime()
            val dialog = TimePickerDialog(activity, { _: TimePicker?, hourOfDay: Int, minute: Int ->
                val dateTime = DateTime(dateTimeTemp.year, dateTimeTemp.month, dateTimeTemp.day, hourOfDay, minute)
                dateTextView.text = dateTime.dateAsText
                timeTextView.text = dateTime.timeAsText
                this.dateTime = dateTime
            }, dateTimeTemp.hour, dateTimeTemp.minute, true)
            dialog.show()
        }

        clearLayout?.setOnClickListener {
            dateTime = null
            dateTextView.text = ""
            timeTextView.text = ""
        }
    }

}