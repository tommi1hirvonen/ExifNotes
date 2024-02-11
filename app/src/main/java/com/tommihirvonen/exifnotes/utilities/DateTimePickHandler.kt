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

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tommihirvonen.exifnotes.core.localDateTimeOrNull
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import java.time.LocalDateTime

/**
 * Helper class to manage the date and time layout onClick events.
 * When the date layout is clicked, a date picker dialog is shown.
 * When the time layout is clicked, a time picker dialog is shown.
 * The DateTime member is managed inside the class.
 */
class DateTimePickHandler(
        private val activity: AppCompatActivity,
        private val initialDateTimeDelegate: () -> (LocalDateTime?),
        private val onDateTimeSelected: (LocalDateTime) -> (Unit)) {

    private val preferences get() = PreferenceManager.getDefaultSharedPreferences(activity)

    fun showDatePickDialog() {
        val dt = initialDateTimeDelegate() ?: LocalDateTime.now()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(dt.epochMilliseconds)
            .build()
        picker.addOnPositiveButtonClickListener {
            val date = picker.selection?.let(::localDateTimeOrNull) ?: dt
            val dateTime = LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, dt.hour, dt.minute)
            onDateTimeSelected(dateTime)
        }
        picker.show(activity.supportFragmentManager, null)
    }

    fun showTimePickDialog() {
        val inputMode = preferences.getInt(PreferenceConstants.KEY_TIME_PICKER_INPUT_MODE,
            MaterialTimePicker.INPUT_MODE_CLOCK).let {
            if (it == MaterialTimePicker.INPUT_MODE_CLOCK || it == MaterialTimePicker.INPUT_MODE_KEYBOARD) {
                it
            } else {
                MaterialTimePicker.INPUT_MODE_CLOCK
            }
        }
        val dt = initialDateTimeDelegate() ?: LocalDateTime.now()
        val picker = MaterialTimePicker.Builder()
            .setInputMode(inputMode)
            .setHour(dt.hour)
            .setMinute(dt.minute)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()
        picker.addOnNegativeButtonClickListener {
            updateTimePickerInputModePreference(picker, inputMode)
        }
        picker.addOnPositiveButtonClickListener {
            updateTimePickerInputModePreference(picker, inputMode)
            val dateTime = LocalDateTime.of(dt.year, dt.month, dt.dayOfMonth,
                picker.hour, picker.minute)
            onDateTimeSelected(dateTime)
        }
        picker.show(activity.supportFragmentManager, null)
    }

    private fun updateTimePickerInputModePreference(picker: MaterialTimePicker, initialInputMode: Int) {
        if (picker.inputMode == initialInputMode) {
            return
        }
        val editor = preferences.edit()
        editor.putInt(PreferenceConstants.KEY_TIME_PICKER_INPUT_MODE, picker.inputMode)
        editor.apply()
    }
}

/**
 * Should be called after the Fragment has been attached to its Activity.
 */
fun Fragment.dateTimePickHandler(initialDateTimeDelegate: () -> (LocalDateTime?),
                                 onDateTimeSelected: (LocalDateTime) -> (Unit)) =
    DateTimePickHandler(requireActivity() as AppCompatActivity, initialDateTimeDelegate, onDateTimeSelected)