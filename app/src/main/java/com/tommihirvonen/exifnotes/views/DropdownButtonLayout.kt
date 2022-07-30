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
import android.widget.FrameLayout
import android.widget.TextView
import com.tommihirvonen.exifnotes.R

class DropdownButtonLayout(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {

    val text: TextView

    init {
        inflate(context, R.layout.dropdown_button_layout, this)
        text = findViewById(R.id.text)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DropdownButtonLayout)
        text.text = attributes.getString(R.styleable.DropdownButtonLayout_text)
        attributes.recycle()
    }
}