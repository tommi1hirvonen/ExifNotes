package com.tommihirvonen.exifnotes.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import com.tommihirvonen.exifnotes.fragments.PreferenceFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

/**
 * PreferenceActivity contains the PreferenceFragment for editing the app's settings
 * and preferences.
 */
public class PreferenceActivity extends android.preference.PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * The ActionBar layout is added manually
     */
    Toolbar actionbar;

    /**
     * Set the UI, add listeners.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final int primaryColor = Utilities.getPrimaryUiColor(getBaseContext());
        final int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.registerOnSharedPreferenceChangeListener(this);

        Utilities.setStatusBarColor(this, secondaryColor);

        // This is a way to get the action bar in Preferences.
        // This is a legacy implementation. All this is needed in order to make
        // the action bar title and icon appear in white. WTF!?
        setContentView(R.layout.activity_settings_legacy);
        actionbar = (Toolbar) findViewById(R.id.actionbar);
        actionbar.setTitle(R.string.Preferences);
        actionbar.setBackgroundColor(primaryColor);
        actionbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        // And even this shit! Since API 23 (M) this is needed to render the back button white.
        actionbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_ab_back_material));
        if (actionbar.getNavigationIcon() != null) {
            actionbar.getNavigationIcon().mutate().setColorFilter(
                    ContextCompat.getColor(getBaseContext(), R.color.white), PorterDuff.Mode.SRC_IN);
        }

        actionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getFragmentManager().beginTransaction().add(R.id.rel_layout, new PreferenceFragment()).commit();
    }

    /**
     * If the app is run on a pre-L device, then manually inject AppCompat preference element.
     *
     * @param name {@inheritDoc}
     * @param context {@inheritDoc}
     * @param attrs {@inheritDoc}
     * @return null
     */
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * @param fragmentName
     * @return
     */
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * Update the UI color when SharedPreferences are changed.
     *
     * @param sharedPreferences {@inheritDoc}
     * @param key {@inheritDoc}
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final int primaryColor = Utilities.getPrimaryUiColor(getBaseContext());
        final int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());
        Utilities.setStatusBarColor(this, secondaryColor);
        actionbar = (Toolbar) findViewById(R.id.actionbar);
        actionbar.setBackgroundColor(primaryColor);
    }
}
