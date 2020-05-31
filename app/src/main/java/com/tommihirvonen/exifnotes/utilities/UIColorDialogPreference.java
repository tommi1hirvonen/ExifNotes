package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.tommihirvonen.exifnotes.R;

import java.util.Arrays;
import java.util.List;

/**
 * Custom DialogPreference added to PreferenceFragment via fragment_preference.xml
 */
public class UIColorDialogPreference extends DialogPreference {

    /**
     * Names for the UI color options
     */
    private final List<String> uiColorOptions;

    /**
     * Hex color codes for the UI color options
     */
    private final List<String> uiColorOptionsData;

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
    public UIColorDialogPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
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
        return uiColorOptions.get(index);
    }

    int getSelectedColorIndex() {
        return index;
    }

    String getSelectedColorData() {
        return uiColorOptionsData.get(index);
    }

    void setUIColor(final int index) {
        persistString(uiColorOptionsData.get(index));
        this.index = index;
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index_) {
        return a.getString(index_);
    }

    @Override
    protected void onSetInitialValue(@Nullable final Object defaultValue) {
        final String colorData = getPersistedString(uiColorOptionsData.get(1)); // Default index = 1 => cyan
        int index_ = uiColorOptionsData.indexOf(colorData);
        if (index_ < 0 || index_ >= uiColorOptionsData.size()) {
            index_ = 1;
        }
        setUIColor(index_);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.dialog_preference_ui_color;
    }
}
