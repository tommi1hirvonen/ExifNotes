package com.tommihirvonen.exifnotes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.fragments.FramesFragment;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.Utilities;

/**
 * Activity to contain the fragment for frames
 */
public class FramesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);

        if (Utilities.isAppThemeDark(getBaseContext())) setTheme(R.style.AppTheme_Dark);

        // Use the same activity layout as in MainActivity.
        setContentView(R.layout.activity_main);

        Utilities.setUiColor(this, true);

        if (findViewById(R.id.fragment_container) != null && savedInstanceState == null) {

            // Get the arguments from the intent from MainActivity...
            final Intent intent = getIntent();
            final long rollId = intent.getLongExtra(ExtraKeys.ROLL_ID, -1);
            final boolean locationEnabled = intent.getBooleanExtra(ExtraKeys.LOCATION_ENABLED, false);
            if (rollId == -1) finish();

            // ...and pass them on to FramesFragment.
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

        super.onCreate(savedInstanceState);
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

}
