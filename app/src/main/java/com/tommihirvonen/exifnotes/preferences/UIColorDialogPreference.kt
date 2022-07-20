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

package com.tommihirvonen.exifnotes.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.tommihirvonen.exifnotes.R

/**
 * Custom DialogPreference added to PreferenceFragment via fragment_preference.xml
 */
class UIColorDialogPreference(context: Context, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    /**
     * Names for the UI color options
     */
    private val uiColorOptions: List<String> = getContext().resources.getStringArray(R.array.UIColorOptions).toList()

    /**
     * Hex color codes for the UI color options
     */
    private val uiColorOptionsData: List<String> = getContext().resources.getStringArray(R.array.UIColorOptionsData).toList()

    /**
     * Holds the index of the selected color option
     */
    var selectedColorIndex = 1
        private set

    /**
     * Used in PreferenceFragment to set the summary
     *
     * @return the name of the selected UI color
     */
    val selectedColorName: String
        get() = uiColorOptions[selectedColorIndex]

    val selectedColorData: String
        get() = uiColorOptionsData[selectedColorIndex]

    fun setUIColor(index: Int) {
        if (index in 0..uiColorOptionsData.size) {
            persistString(uiColorOptionsData[index])
            selectedColorIndex = index
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index_: Int): Any {
        return a.getString(index_)!!
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val colorData = getPersistedString(uiColorOptionsData[1]) // Default index = 1 => cyan
        val index = uiColorOptionsData.indexOf(colorData)
        setUIColor(index)
    }

    override fun getDialogLayoutResource(): Int {
        return R.layout.dialog_preference_ui_color
    }

}