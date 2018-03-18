package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.tommihirvonen.exifnotes.R;

/**
 * Custom DialogPreference added to PreferenceFragment via fragment_preference.xml
 */
public class AppThemeDialogPreference extends DialogPreference {

    /**
     * References to the checkbox views.
     */
    private ImageView checkboxLight;
    private ImageView checkboxDark;

    /**
     * Selected app theme (LIGHT or DARK)
     */
    private String selectedTheme;

    /**
     * Holds the initial app theme
     */
    private String initialTheme;

    /**
     * Custom constructor
     *
     * @param context {@inheritDoc}
     * @param attrs {@inheritDoc}
     */
    public AppThemeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.dialog_preference_app_theme);
    }

    /**
     * Used in PreferenceFragment to set the summary
     *
     * @return the name of the selected app theme with first letter in uppercase and others lowercase
     */
    public String getAppTheme() {
        return selectedTheme.substring(0, 1) + selectedTheme.substring(1).toLowerCase();
    }

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
        if (restorePersistedValue) {
            selectedTheme = getPersistedString(PreferenceConstants.VALUE_APP_THEME_LIGHT); // Light theme by default
        } else {
            selectedTheme = (String) defaultValue;
            persistString(selectedTheme);
        }
        // Safety check - if the selected theme doesn't equal any known themes, use the default theme.
        if (!selectedTheme.equals(PreferenceConstants.VALUE_APP_THEME_LIGHT) && !selectedTheme.equals(PreferenceConstants.VALUE_APP_THEME_DARK)) {
            selectedTheme = PreferenceConstants.VALUE_APP_THEME_LIGHT;
        }
        // Set the initial value so that if the user cancels the dialog,
        // this initial value can be recovered and the selected value can be reset.
        initialTheme = selectedTheme;
    }

    /**
     * Called before the constructor to get the default value from XML
     * if the preference hasn't been set.
     *
     * @param a array of attributes
     * @param index index to the default value in the array
     * @return string containing the default value
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    /**
     * Binds views of the content view of the dialog to data
     *
     * @param view the content view of the dialog if it is custom
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ImageView light = view.findViewById(R.id.app_theme_light);
        ImageView dark = view.findViewById(R.id.app_theme_dark);
        checkboxLight = view.findViewById(R.id.checkbox_light);
        checkboxDark = view.findViewById(R.id.checkbox_dark);
        light.setOnClickListener(new ThemeOnClickListener());
        dark.setOnClickListener(new ThemeOnClickListener());
        updateCheckboxVisibility();
    }

    /**
     * Called when the dialog is closed
     *
     * @param positiveResult whether the positive button (true) was clicked or negative (false)
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            if (callChangeListener(selectedTheme)) {
                // The preference was changed -> update the initial value.
                // If the user opens the dialog again without leaving PreferenceFragment,
                // this step will need to be done.
                initialTheme = selectedTheme;
                persistString(selectedTheme);
                setSummary(getAppTheme());
            }
        } else {
            // Reset the selected theme with the initial theme.
            selectedTheme = initialTheme;
        }
    }

    /**
     * Update the visibility of check boxes displayed on top of theme options.
     * Set the selected to be visible.
     */
    private void updateCheckboxVisibility() {
        checkboxLight.setVisibility(View.GONE);
        checkboxDark.setVisibility(View.GONE);
        switch (selectedTheme) {
            case PreferenceConstants.VALUE_APP_THEME_LIGHT:
                checkboxLight.setVisibility(View.VISIBLE);
                break;
            case PreferenceConstants.VALUE_APP_THEME_DARK:
                checkboxDark.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /**
     * Custom OnClickListener which is set for each theme option
     */
    private class ThemeOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            final int id = view.getId();
            switch (id) {
                case R.id.app_theme_light:
                    selectedTheme = PreferenceConstants.VALUE_APP_THEME_LIGHT;
                    break;
                case R.id.app_theme_dark:
                    selectedTheme = PreferenceConstants.VALUE_APP_THEME_DARK;
                    break;
                default:
                    break;
            }
            updateCheckboxVisibility();
        }
    }

}
