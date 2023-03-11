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

package com.tommihirvonen.exifnotes.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.utilities.DateTimePickHandler

class DateTimeLayout(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {

    val dateLayout: DropdownButtonLayout
    val timeLayout: DropdownButtonLayout

    var dateTimePickHandler: DateTimePickHandler? = null

    init {
        inflate(context, R.layout.date_time_layout, this)

        dateLayout = findViewById(R.id.date_time_layout_date_layout)
        timeLayout = findViewById(R.id.date_time_layout_time_layout)
        dateLayout.setOnClickListener { dateTimePickHandler?.showDatePickDialog() }
        timeLayout.setOnClickListener { dateTimePickHandler?.showTimePickDialog() }

        val labelText = findViewById<TextView>(R.id.label)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DateTimeLayout)
        dateLayout.text = attributes.getString(R.styleable.DateTimeLayout_dateText)
        timeLayout.text = attributes.getString(R.styleable.DateTimeLayout_timeText)
        labelText.text = attributes.getString(R.styleable.DateTimeLayout_label)
        attributes.recycle()
    }
}