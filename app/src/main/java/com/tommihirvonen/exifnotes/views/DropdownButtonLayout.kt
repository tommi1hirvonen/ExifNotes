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
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.tommihirvonen.exifnotes.R

class DropdownButtonLayout(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {

    val textView: TextView
    private val hintView: TextView

    var text: CharSequence?
    get() = textView.text
    set(value) {
        textView.text = value
        if (value.isNullOrEmpty()) {
            textView.visibility = View.GONE
            hintView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.VISIBLE
            hintView.visibility = View.GONE
        }
    }

    var hint: CharSequence?
    get() = hintView.text
    set(value) {
        hintView.text = value
    }

    init {
        inflate(context, R.layout.dropdown_button_layout, this)
        textView = findViewById(R.id.text)
        hintView = findViewById(R.id.hint)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DropdownButtonLayout)
        val text = attributes.getString(R.styleable.DropdownButtonLayout_text)
        val hint = attributes.getString(R.styleable.DropdownButtonLayout_hint)
        val maxLines = attributes.getInt(R.styleable.DropdownButtonLayout_maxLines, 1)
        textView.text = text
        hintView.text = hint
        textView.maxLines = maxLines
        hintView.maxLines = maxLines
        if (text.isNullOrEmpty()) {
            textView.visibility = View.GONE
            hintView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.VISIBLE
            hintView.visibility = View.GONE
        }
        attributes.recycle()
    }
}