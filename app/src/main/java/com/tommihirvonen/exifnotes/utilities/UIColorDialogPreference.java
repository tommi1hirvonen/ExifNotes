package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.tommihirvonen.exifnotes.R;

import java.util.Arrays;
import java.util.List;

/**
 * Custom DialogPreference added to PreferenceFragment via fragment_preference.xml
 */
public class UIColorDialogPreference extends DialogPreference {

    /**
     * Name for the selected UI color
     */
    private String selectedColorName;

    /**
     * Hex color code for the selected UI color
     */
    private String selectedColorData;

    /**
     * Names for the UI color options
     */
    private List<String> uiColorOptions;

    /**
     * Hex color codes for the UI color options
     */
    private List<String> uiColorOptionsData;

    /**
     * References to the checkbox views
     */
    private ImageView checkbox1;
    private ImageView checkbox2;
    private ImageView checkbox3;
    private ImageView checkbox4;
    private ImageView checkbox5;
    private ImageView checkbox6;
    private ImageView checkbox7;
    private ImageView checkbox8;

    /**
     * Holds the index of the selected color option
     */
    private int index = 1;

    /**
     * Custom constructor
     *
     * @param context {@inheritDoc}
     * @param attrs {@inheritDoc}
     */
    public UIColorDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.ui_color_dialog_preference_layout);
        // Get color option names
        uiColorOptions = Arrays.asList(getContext().getResources().getStringArray(R.array.UIColorOptions));
        // Get color option data (color codes)
        uiColorOptionsData = Arrays.asList(getContext().getResources().getStringArray(R.array.UIColorOptionsData));
    }

    /**
     * Used in PreferenceFragment to set the summary
     *
     * @return the name of the selected UI color
     */
    public String getSelectedColorName() {
        return selectedColorName;
    }

    /* Use onCreateDialogView() if you want to create the layout programmatically.
    @Override
    protected View onCreateDialogView() {
        FrameLayout dialogView = new FrameLayout(getContext());
        return dialogView;
    }
    */

    /**
     * Called when the class is instantiated. If the value has been set, call getPersistedString().
     * Otherwise get the default value and set the preference using persistString().
     *
     * @param restorePersistedValue {@inheritDoc}
     * @param defaultValue {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            selectedColorData = getPersistedString(uiColorOptionsData.get(1)); // Cyan data
        } else {
            selectedColorData = (String) defaultValue;
            persistString(selectedColorData);
        }
        index = uiColorOptionsData.indexOf(selectedColorData);
        selectedColorName = uiColorOptions.get(index);
    }

    /**
     * Called before the constructor to get the default value from XML
     * if the preference hasn't been set.
     *
     * @param a array of attributes
     * @param index_ index to the default value in the array
     * @return string containing the default value
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index_) {
        return a.getString(index_);
    }

    /**
     * Binds views of the content view of the dialog to data
     *
     * @param view the content view of the dialog if it is custom
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ImageView color1 = view.findViewById(R.id.ui_color_option_1);
        ImageView color2 = view.findViewById(R.id.ui_color_option_2);
        ImageView color3 = view.findViewById(R.id.ui_color_option_3);
        ImageView color4 = view.findViewById(R.id.ui_color_option_4);
        ImageView color5 = view.findViewById(R.id.ui_color_option_5);
        ImageView color6 = view.findViewById(R.id.ui_color_option_6);
        ImageView color7 = view.findViewById(R.id.ui_color_option_7);
        ImageView color8 = view.findViewById(R.id.ui_color_option_8);
        checkbox1 = view.findViewById(R.id.checkbox_1);
        checkbox2 = view.findViewById(R.id.checkbox_2);
        checkbox3 = view.findViewById(R.id.checkbox_3);
        checkbox4 = view.findViewById(R.id.checkbox_4);
        checkbox5 = view.findViewById(R.id.checkbox_5);
        checkbox6 = view.findViewById(R.id.checkbox_6);
        checkbox7 = view.findViewById(R.id.checkbox_7);
        checkbox8 = view.findViewById(R.id.checkbox_8);
        color1.setOnClickListener(new ColorOnClickListener());
        color2.setOnClickListener(new ColorOnClickListener());
        color3.setOnClickListener(new ColorOnClickListener());
        color4.setOnClickListener(new ColorOnClickListener());
        color5.setOnClickListener(new ColorOnClickListener());
        color6.setOnClickListener(new ColorOnClickListener());
        color7.setOnClickListener(new ColorOnClickListener());
        color8.setOnClickListener(new ColorOnClickListener());
        updateCheckboxVisibility();
    }

    /**
     * Called when the dialog is closed.
     *
     * @param positiveResult whether the positive button (true) was clicked or negative (false)
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            if (callChangeListener(selectedColorData)) {
                persistString(selectedColorData);
                setSummary(selectedColorName);
            }
        }
    }

    /**
     * Update the visibility of check boxes displayed on top of color options.
     * Set the selected to be visible.
     */
    private void updateCheckboxVisibility() {
        checkbox1.setVisibility(View.GONE);
        checkbox2.setVisibility(View.GONE);
        checkbox3.setVisibility(View.GONE);
        checkbox4.setVisibility(View.GONE);
        checkbox5.setVisibility(View.GONE);
        checkbox6.setVisibility(View.GONE);
        checkbox7.setVisibility(View.GONE);
        checkbox8.setVisibility(View.GONE);
        switch (index) {
            case 0:
                checkbox1.setVisibility(View.VISIBLE);
                break;
            case 1:
                checkbox2.setVisibility(View.VISIBLE);
                break;
            case 2:
                checkbox3.setVisibility(View.VISIBLE);
                break;
            case 3:
                checkbox4.setVisibility(View.VISIBLE);
                break;
            case 4:
                checkbox5.setVisibility(View.VISIBLE);
                break;
            case 5:
                checkbox6.setVisibility(View.VISIBLE);
                break;
            case 6:
                checkbox7.setVisibility(View.VISIBLE);
                break;
            case 7:
                checkbox8.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /**
     * Custom OnClickListener set for each color option view.
     */
    private class ColorOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            selectedColorName = String.valueOf(view.getContentDescription());
            index = uiColorOptions.indexOf(selectedColorName);
            selectedColorData = uiColorOptionsData.get(index);
            updateCheckboxVisibility();
        }
    }
}
