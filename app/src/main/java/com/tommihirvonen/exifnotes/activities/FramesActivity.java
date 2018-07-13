package com.tommihirvonen.exifnotes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.fragments.FramesFragment;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.Utilities;

/**
 * Activity to contain the fragment for frames
 */
public class FramesActivity extends AppCompatActivity {

    /**
     * Constants passed to PreferenceActivity
     */
    public static final int PREFERENCE_ACTIVITY_REQUEST = 8;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        // Get the arguments from the intent from MainActivity.
        final Intent intent = getIntent();
        final long rollId = intent.getLongExtra(ExtraKeys.ROLL_ID, -1);
        final boolean locationEnabled = intent.getBooleanExtra(ExtraKeys.LOCATION_ENABLED, false);
        final boolean overridePendingTransition = intent.getBooleanExtra(ExtraKeys.OVERRIDE_PENDING_TRANSITION, false);
        if (rollId == -1) finish();

        // If the activity was launched from MainActivity, enable custom transition animations.
        // If the activity was recreated, then custom animations are not enabled during onCreate().
        if (overridePendingTransition) {
            overridePendingTransition(R.anim.enter_from_right, R.anim.hold);
            // Replace the old intent with a modified one, where the OVERRIDE_PENDING_TRANSITION
            // boolean value has been exhausted and set to false.
            intent.putExtra(ExtraKeys.OVERRIDE_PENDING_TRANSITION, false);
            setIntent(intent);
        }

        if (Utilities.isAppThemeDark(getBaseContext())) setTheme(R.style.AppTheme_Dark);

        // The point at which super.onCreate() is called is important.
        // Calling it at the end of the method resulted in the back button not appearing
        // when action mode was enabled.
        super.onCreate(savedInstanceState);

        // Use the same activity layout as in MainActivity.
        setContentView(R.layout.activity_main);

        Utilities.setUiColor(this, true);

        if (findViewById(R.id.fragment_container) != null && savedInstanceState == null) {

            // Pass the arguments from MainActivity on to FramesFragment.
            final FramesFragment framesFragment = new FramesFragment();
            Bundle arguments = new Bundle();
            arguments.putLong(ExtraKeys.ROLL_ID, rollId);
            arguments.putBoolean(ExtraKeys.LOCATION_ENABLED, locationEnabled);
            framesFragment.setArguments(arguments);

            getFragmentManager().beginTransaction().add(
                    R.id.fragment_container, framesFragment, FramesFragment.FRAMES_FRAGMENT_TAG).commit();

            // Bring the shadow element from the activity's layout to front.
            findViewById(R.id.shadow).bringToFront();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        int primaryColor = Utilities.getPrimaryUiColor(getBaseContext());
        int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());
        Utilities.setSupportActionBarColor(this, primaryColor);
        Utilities.setStatusBarColor(this, secondaryColor);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right);
    }

    /**
     * The PreferenceActivity is started for result and the result is captured here.
     *
     * The result code is compared using bitwise operators to determine
     * whether a new database was imported, the app theme was changed or both.
     *
     * @param requestCode passed to the activity when it is launched
     * @param resultCode integer to be compared using bitwise operators to determine the action(s)
     *                   that were taken in PreferenceActivity
     * @param data not used
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case PREFERENCE_ACTIVITY_REQUEST:

                // If a new database was imported, use setResult() to notify MainActivity as well.
                // Finish this activity since the selected roll may not be valid anymore.
                if ((resultCode & PreferenceActivity.RESULT_DATABASE_IMPORTED) ==
                        PreferenceActivity.RESULT_DATABASE_IMPORTED) {
                    setResult(resultCode);
                    finish();
                }
                // If the app theme was changed, recreate activity for changes to take effect.
                if ((resultCode & PreferenceActivity.RESULT_THEME_CHANGED) ==
                        PreferenceActivity.RESULT_THEME_CHANGED) {
                    recreate();
                }
                return;

            default:
                break;
        }

        // Call super in case the result was not handled here
        super.onActivityResult(requestCode, resultCode, data);
    }

}
