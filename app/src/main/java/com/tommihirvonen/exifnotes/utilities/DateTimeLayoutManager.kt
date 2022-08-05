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

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.datastructures.DateTime.Companion.fromCurrentTime
import com.tommihirvonen.exifnotes.views.DateTimeLayout
import java.util.*

/**
 * Helper class to manage the date and time layout onClick events.
 * When the date layout is clicked, a date picker dialog is shown.
 * When the time layout is clicked, a time picker dialog is shown.
 * The DateTime member is managed inside the class.
 */
class DateTimeLayoutManager(
        activity: AppCompatActivity,
        dateTimeLayout: DateTimeLayout,
        var dateTime: DateTime?,
        clearLayout: View?
) {

    init {
        dateTimeLayout.dateLayout.setOnClickListener {
            val dt = dateTime ?: fromCurrentTime()
            val cal = Calendar.getInstance()
            cal.set(dt.year, dt.month - 1, dt.day)
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(cal.timeInMillis)
                .build()
            picker.addOnPositiveButtonClickListener {
                cal.timeInMillis = picker.selection ?: cal.timeInMillis
                val dateTime = DateTime(cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DATE],
                    dt.hour, dt.minute)
                dateTimeLayout.dateLayout.text = dateTime.dateAsText
                dateTimeLayout.timeLayout.text = dateTime.timeAsText
                this.dateTime = dateTime
            }
            picker.show(activity.supportFragmentManager, null)
        }

        dateTimeLayout.timeLayout.setOnClickListener {
            val dt = dateTime ?: fromCurrentTime()
            val picker = MaterialTimePicker.Builder()
                .setHour(dt.hour)
                .setMinute(dt.minute)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .build()

            picker.addOnPositiveButtonClickListener {
                val dateTime = DateTime(dt.year, dt.month, dt.day,
                    picker.hour, picker.minute)
                dateTimeLayout.dateLayout.text = dateTime.dateAsText
                dateTimeLayout.timeLayout.text = dateTime.timeAsText
                this.dateTime = dateTime
            }
            picker.show(activity.supportFragmentManager, null)
        }

        clearLayout?.setOnClickListener {
            dateTime = null
            dateTimeLayout.dateLayout.text = null
            dateTimeLayout.timeLayout.text = null
        }

        dateTimeLayout.dateLayout.text = dateTime?.dateAsText
        dateTimeLayout.timeLayout.text = dateTime?.timeAsText
    }

}