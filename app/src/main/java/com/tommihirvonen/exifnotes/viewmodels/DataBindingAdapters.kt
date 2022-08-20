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

package com.tommihirvonen.exifnotes.viewmodels

import android.text.InputFilter
import android.widget.AdapterView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

object DataBindingAdapters {
    @BindingAdapter("errorText")
    @JvmStatic
     fun setErrorText(view: TextInputLayout, errorText: String?) {
        view.error = errorText
    }

    @BindingAdapter("inputFilter")
    @JvmStatic
    fun setInputFilter(view: TextInputEditText, filter: InputFilter) {
        view.filters = arrayOf(filter)
    }

    @BindingAdapter("onItemClick")
    @JvmStatic
    fun setOnItemClickListener(view: MaterialAutoCompleteTextView,
                                  listener: AdapterView.OnItemClickListener) {
        view.onItemClickListener = listener
    }

    @BindingAdapter("textNoFilter")
    @JvmStatic
    fun setText(view: MaterialAutoCompleteTextView, text: String?) {
        view.setText(text, false)
    }
}