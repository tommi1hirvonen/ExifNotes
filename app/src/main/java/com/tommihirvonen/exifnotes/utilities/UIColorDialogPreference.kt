package com.tommihirvonen.exifnotes.utilities

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