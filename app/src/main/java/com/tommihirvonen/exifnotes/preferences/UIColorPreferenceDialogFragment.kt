package com.tommihirvonen.exifnotes.preferences

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.preference.PreferenceDialogFragmentCompat
import com.tommihirvonen.exifnotes.R

class UIColorPreferenceDialogFragment(key: String?) : PreferenceDialogFragmentCompat() {

    init {
        val bundle = Bundle(1)
        bundle.putString(ARG_KEY, key)
        this.arguments = bundle
    }
    
    /**
     * Holds the index of the selected color option
     */
    private var index = 1

    /**
     * References to the checkbox views
     */
    private lateinit var checkbox1: ImageView
    private lateinit var checkbox2: ImageView
    private lateinit var checkbox3: ImageView
    private lateinit var checkbox4: ImageView
    private lateinit var checkbox5: ImageView
    private lateinit var checkbox6: ImageView
    private lateinit var checkbox7: ImageView
    private lateinit var checkbox8: ImageView
    
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        checkbox1 = view.findViewById(R.id.checkbox_1)
        checkbox2 = view.findViewById(R.id.checkbox_2)
        checkbox3 = view.findViewById(R.id.checkbox_3)
        checkbox4 = view.findViewById(R.id.checkbox_4)
        checkbox5 = view.findViewById(R.id.checkbox_5)
        checkbox6 = view.findViewById(R.id.checkbox_6)
        checkbox7 = view.findViewById(R.id.checkbox_7)
        checkbox8 = view.findViewById(R.id.checkbox_8)
        val color1 = view.findViewById<ImageView>(R.id.ui_color_option_1)
        val color2 = view.findViewById<ImageView>(R.id.ui_color_option_2)
        val color3 = view.findViewById<ImageView>(R.id.ui_color_option_3)
        val color4 = view.findViewById<ImageView>(R.id.ui_color_option_4)
        val color5 = view.findViewById<ImageView>(R.id.ui_color_option_5)
        val color6 = view.findViewById<ImageView>(R.id.ui_color_option_6)
        val color7 = view.findViewById<ImageView>(R.id.ui_color_option_7)
        val color8 = view.findViewById<ImageView>(R.id.ui_color_option_8)
        color1.setOnClickListener(ColorOnClickListener())
        color2.setOnClickListener(ColorOnClickListener())
        color3.setOnClickListener(ColorOnClickListener())
        color4.setOnClickListener(ColorOnClickListener())
        color5.setOnClickListener(ColorOnClickListener())
        color6.setOnClickListener(ColorOnClickListener())
        color7.setOnClickListener(ColorOnClickListener())
        color8.setOnClickListener(ColorOnClickListener())
        val preference = preference
        if (preference is UIColorDialogPreference) {
            index = preference.selectedColorIndex
        }
        updateCheckboxVisibility()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val preference = preference
            if (preference is UIColorDialogPreference && preference.callChangeListener(preference.selectedColorData)) {
                preference.setUIColor(index)
                preference.setSummary(preference.selectedColorName)
            }
        }
    }

    private fun updateCheckboxVisibility() {
        checkbox1.visibility = View.GONE
        checkbox2.visibility = View.GONE
        checkbox3.visibility = View.GONE
        checkbox4.visibility = View.GONE
        checkbox5.visibility = View.GONE
        checkbox6.visibility = View.GONE
        checkbox7.visibility = View.GONE
        checkbox8.visibility = View.GONE
        when (index) {
            0 -> checkbox1.visibility = View.VISIBLE
            1 -> checkbox2.visibility = View.VISIBLE
            2 -> checkbox3.visibility = View.VISIBLE
            3 -> checkbox4.visibility = View.VISIBLE
            4 -> checkbox5.visibility = View.VISIBLE
            5 -> checkbox6.visibility = View.VISIBLE
            6 -> checkbox7.visibility = View.VISIBLE
            7 -> checkbox8.visibility = View.VISIBLE
            else -> {
            }
        }
    }

    private inner class ColorOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            when (view.id) {
                R.id.ui_color_option_1 -> index = 0
                R.id.ui_color_option_2 -> index = 1
                R.id.ui_color_option_3 -> index = 2
                R.id.ui_color_option_4 -> index = 3
                R.id.ui_color_option_5 -> index = 4
                R.id.ui_color_option_6 -> index = 5
                R.id.ui_color_option_7 -> index = 6
                R.id.ui_color_option_8 -> index = 7
                else -> {
                }
            }
            updateCheckboxVisibility()
        }
    }
    
}