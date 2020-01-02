package com.tommihirvonen.exifnotes.activities;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.widget.RelativeLayout;

import com.tommihirvonen.exifnotes.fragments.PreferenceFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.Utilities;

/**
 * PreferenceActivity contains the PreferenceFragment for editing the app's settings
 * and preferences.
 */
public class PreferenceActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Public constant custom result code used to indicate that a database was imported
     */
    public static final int RESULT_DATABASE_IMPORTED = 0x10;

    /**
     * Public constant custom result code used to indicate that the app's theme was changed
     */
    public static final int RESULT_THEME_CHANGED = 0x20;

    /**
     * Member to store the current result code to be passed to the activity which started
     * this activity for result.
     */
    private int resultCode = 0x0;

    /**
     * The ActionBar layout is added manually
     */
    private Toolbar actionbar;

    /**
     * Set this activity's result code
     *
     * @param resultCode result code, e.g. RESULT_DATABASE_IMPORTED | RESULT_THEME_CHANGED
     *                   to indicate that both a database was imported and the app's theme
     *                   was changed
     */
    public void setResultCode(final int resultCode) {
        this.resultCode = resultCode;
        setResult(resultCode);
    }

    /**
     * Get this activity's current result code
     *
     * @return current result code
     */
    public int getResultCode() {
        return resultCode;
    }

    /**
     * Set the UI, add listeners.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        super.onPostCreate(savedInstanceState);

        // If the activity was recreated, get the saved result code
        if (savedInstanceState != null) {
            setResultCode(savedInstanceState.getInt(ExtraKeys.RESULT_CODE));
        }

        final int primaryColor = Utilities.getPrimaryUiColor(getBaseContext());
        final int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());

        getSupportActionBar().hide();

        prefs.registerOnSharedPreferenceChangeListener(this);

        Utilities.setStatusBarColor(this, secondaryColor);

        // This is a way to get the action bar in Preferences.
        // This is a legacy implementation. All this is needed in order to make
        // the action bar title and icon appear in white. WTF!?
        setContentView(R.layout.activity_settings_legacy);

        if (Utilities.isAppThemeDark(getBaseContext())) {
            final RelativeLayout relativeLayout = findViewById(R.id.rel_layout);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background_dark_grey));
        }

        actionbar = findViewById(R.id.actionbar);
        actionbar.setTitle(R.string.Preferences);
        actionbar.setBackgroundColor(primaryColor);
        actionbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        // And even this shit! Since API 23 (M) this is needed to render the back button white.
        // Do only for M and up, on older devices this will cause Resources$NotFoundException.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            actionbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_ab_back_material));
        }
        if (actionbar.getNavigationIcon() != null) {
            Utilities.setColorFilter(actionbar.getNavigationIcon().mutate(),
                    ContextCompat.getColor(getBaseContext(), R.color.white));
        }

        actionbar.setNavigationOnClickListener(v -> finish());
        getSupportFragmentManager().beginTransaction().add(R.id.rel_layout, new PreferenceFragment()).commit();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        // Save the result code so that it can be set for this activity's result when recreated
        super.onSaveInstanceState(outState);
        outState.putInt(ExtraKeys.RESULT_CODE, resultCode);
    }


    /**
     * Update the UI color when SharedPreferences are changed.
     *
     * @param sharedPreferences {@inheritDoc}
     * @param key {@inheritDoc}
     */
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        final int primaryColor = Utilities.getPrimaryUiColor(getBaseContext());
        final int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());
        Utilities.setStatusBarColor(this, secondaryColor);
        actionbar = findViewById(R.id.actionbar);
        actionbar.setBackgroundColor(primaryColor);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right);
    }
}
