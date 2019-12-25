package com.tommihirvonen.exifnotes.utilities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.tommihirvonen.exifnotes.R;

public class AppThemePreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    public static AppThemePreferenceDialogFragment newInstance(String key) {
        final AppThemePreferenceDialogFragment fragment = new AppThemePreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    /**
     * References to the checkbox views.
     */
    private ImageView checkboxLight;
    private ImageView checkboxDark;

    /**
     * Selected app theme (LIGHT or DARK)
     */
    private String selectedTheme;


    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ImageView light = view.findViewById(R.id.app_theme_light);
        ImageView dark = view.findViewById(R.id.app_theme_dark);
        checkboxLight = view.findViewById(R.id.checkbox_light);
        checkboxDark = view.findViewById(R.id.checkbox_dark);
        light.setOnClickListener(new ThemeOnClickListener());
        dark.setOnClickListener(new ThemeOnClickListener());

        DialogPreference preference = getPreference();
        if (preference instanceof AppThemeDialogPreference) {
            selectedTheme = ((AppThemeDialogPreference) preference).getAppTheme();
        }

        updateCheckboxVisibility();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            DialogPreference preference = getPreference();
            if (preference instanceof AppThemeDialogPreference && preference.callChangeListener(selectedTheme)) {
                ((AppThemeDialogPreference) preference).setAppTheme(selectedTheme);
                preference.setSummary(((AppThemeDialogPreference)preference).getAppTheme());
            }
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
