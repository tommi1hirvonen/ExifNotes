package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Pair;
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
     * Name (first) and data (second) for the selected UI color
     */
    private Pair<String, String> selectedColor;

    /**
     * Name (first) and data (second)
     */
    private Pair<String, String> initialColor;

    /**
     * Names for the UI color options
     */
    private List<String> uiColorOptions;

    /**
     * Hex color codes for the UI color options
     */
    private List<String> uiColorOptionsData;

    /**
     * Holds the index of the selected color option
     */
    private int index = 1;

    /**
     * The index of the initial color option
     */
    private int initialIndex = 1;

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
        return selectedColor.first;
    }

    /* Use onCreateDialogView() if you want to create the layout programmatically.
    @Override
    protected View onCreateDialogView() {
        FrameLayout dialogView = new FrameLayout(getContext());
        return dialogView;
    }
    */

    /**
     * Called when the class is instantiated (PreferenceFragment is created).
     * If the value has been set, call getPersistedString().
     * Otherwise get the default value and set the preference using persistString().
     *
     * @param restorePersistedValue {@inheritDoc}
     * @param defaultValue {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String selectedColorName, selectedColorData;

        // Get the persisted value if the value has been set (restorePersistedValue = true).
        if (restorePersistedValue) {
            selectedColorData= getPersistedString(uiColorOptionsData.get(1)); // Cyan data by default (index = 1)
        }
        // Otherwise get the default value and set the preference.
        else {
            selectedColorData = (String) defaultValue;
            persistString(selectedColorData);
        }
        // Get the index value from the list of UI color data options.
        index = uiColorOptionsData.indexOf(selectedColorData);

        // Safety check - if the index < 0 (selectedColorData was not found in uiColorOptionsData)
        // or the index is greater than the size of uiColorOptionsData, default to a value we
        // know exists and set selectedColorData accordingly.
        if (index < 0 || index >= uiColorOptionsData.size()) {
            index = 1; // index = 1 -> cyan data
            selectedColorData = uiColorOptionsData.get(index);
        }

        // Set the selected color's name from name list.
        selectedColorName = uiColorOptions.get(index);

        // Update this class's member
        selectedColor = Pair.create(selectedColorName, selectedColorData);

        // Set the initial values so that if the user cancels the dialog,
        // these initial values can be recovered and the selected values can be reset.
        initialColor = selectedColor;
        initialIndex = index;
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
            if (callChangeListener(selectedColor.second)) {
                // The preference was changed -> update the initial values.
                // If the user opens the dialog again without leaving PreferenceFragment,
                // this step will need to be done.
                initialColor = selectedColor;
                initialIndex = index;
                persistString(selectedColor.second);
                setSummary(selectedColor.first);
            }
        } else {
            // Reset the selected values with the initial values.
            selectedColor = initialColor;
            index = initialIndex;
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
            final int id = view.getId();
            switch (id) {
                case R.id.ui_color_option_1:
                    index = 0;
                    break;
                case R.id.ui_color_option_2:
                    index = 1;
                    break;
                case R.id.ui_color_option_3:
                    index = 2;
                    break;
                case R.id.ui_color_option_4:
                    index = 3;
                    break;
                case R.id.ui_color_option_5:
                    index = 4;
                    break;
                case R.id.ui_color_option_6:
                    index = 5;
                    break;
                case R.id.ui_color_option_7:
                    index = 6;
                    break;
                case R.id.ui_color_option_8:
                    index = 7;
                    break;
                default:
                    break;
            }
            final String selectedColorName = uiColorOptions.get(index);
            final String selectedColorData = uiColorOptionsData.get(index);
            selectedColor = Pair.create(selectedColorName, selectedColorData);
            updateCheckboxVisibility();
        }
    }
}
