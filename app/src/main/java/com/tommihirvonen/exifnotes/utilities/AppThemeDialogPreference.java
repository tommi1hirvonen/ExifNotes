package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.tommihirvonen.exifnotes.R;

/**
 * Custom DialogPreference added to PreferenceFragment via fragment_preference.xml
 */
public class AppThemeDialogPreference extends DialogPreference {

    private String selectedTheme;

    public AppThemeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setAppTheme(String theme) {
        selectedTheme = theme;
        persistString(selectedTheme);
    }

    public String getAppTheme() {
        return selectedTheme;
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

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        setAppTheme(getPersistedString(PreferenceConstants.VALUE_APP_THEME_LIGHT));
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.dialog_preference_app_theme;
    }
}
