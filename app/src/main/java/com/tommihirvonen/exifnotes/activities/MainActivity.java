package com.tommihirvonen.exifnotes.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.dialogs.SimpleEula;
import com.tommihirvonen.exifnotes.fragments.FramesFragment;
import com.tommihirvonen.exifnotes.fragments.RollsFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.File;

/**
 * MainActivity is the first activity to be called when the app is launched.
 * It contains the RollsFragment and FramesFragment fragments.
 * The activity switches between these two fragments.
 */
public class MainActivity extends AppCompatActivity implements
        RollsFragment.OnRollSelectedListener,
        FramesFragment.OnHomeAsUpPressedListener {

    /**
     * Tag for permission request
     */
    private final static int MY_MULTIPLE_PERMISSIONS_REQUEST = 1;

    /**
     * Value to store whether location services should be enabled or not.
     * Determined by the location permissions granted to the app by the user.
     */
    private boolean locationPermissionsGranted = false;

    /**
     * Tag for database import request. Used when the PreferenceActivity is launched.
     */
    public static final int PREFERENCE_ACTIVITY_REQUEST = 8;

    /**
     * Create the layout and activate location services.
     *
     * @param savedInstanceState passed to super.onCreate to execute necessary code to properly
     *                           create the fragment
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        new SimpleEula(this).show();

        Utilities.setUiColor(this, false);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(
                "  " + getResources().getString(R.string.MainActivityTitle));

        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        boolean permissionWriteExternalStorage = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean permissionAccessCoarseLocation = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean permissionAccessFineLocation = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        //Check if the app has all necessary permissions
        if (!permissionWriteExternalStorage || !permissionAccessCoarseLocation || !permissionAccessFineLocation) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_MULTIPLE_PERMISSIONS_REQUEST);
        } else {
            locationPermissionsGranted = true;
        }

        // Get from DefaultSharedPreferences whether the user has enabled
        // location updates in the app's settings.
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final boolean requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true);

        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // Show a dialog to go to settings only if GPS is not enabled in system settings
        // but location updates are enabled in the app's settings.
        if (!isGPSEnabled && requestingLocationUpdates) showSettingsAlert();

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            RollsFragment firstFragment = new RollsFragment();

            // Add the fragment to the 'fragment_container' FrameLayout
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment, RollsFragment.ROLLS_FRAGMENT_TAG).commit();
        }
    }

    /**
     * Inflate the main menu.
     *
     * @param menu the menu to be inflated
     * @return super class to execute code for the menu to work properly.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * When resuming we have to change the UI colours again.
     */
    @Override
    public void onResume(){
        super.onResume();

        int primaryColor = Utilities.getPrimaryUiColor(getBaseContext());
        int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());
        Utilities.setSupportActionBarColor(this, primaryColor);
        Utilities.setStatusBarColor(this, secondaryColor);
    }

    /**
     * This function is called when the user presses the back button.
     * If the FramesFragment is in front then pop the stack back by one
     * and bring the RollsFragment to front.
     * Also bring the shadow under the action bar to front.
     */
    @Override
    public void onBackPressed() {

        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            //additional code
        } else {
            getFragmentManager().popBackStack();
            View shadow = findViewById(R.id.shadow);
            shadow.bringToFront();
        }

    }

    /**
     * This function is called when MainActivity is started, in other words when the
     * application is started. All the files created in FramesFragment.setShareIntentExportRoll
     * in the application's external storage directory are deleted. This way we can keep
     * the number of files stored to a minimum.
     */
    @Override
    public void onStart(){
        //Delete all the files created in FramesFragment.setShareIntentExportRoll
        File externalStorageDir = getExternalFilesDir(null);
        Utilities.purgeDirectory(externalStorageDir);
        super.onStart();
    }

    /**
     * This function is called when the user presses a roll in the RollsFragment.
     * Create a new FramesFragment, pass the roll id and locationPermissionsGranted to it and bring it to the
     * front of the stack.
     * Also bring the shadow under the action bar to front.
     *
     * @param rollId The id of the roll that was pressed.
     */
    @Override
    public void onRollSelected(long rollId){
        FramesFragment newFragment = new FramesFragment();
        Bundle args = new Bundle();
        args.putLong(ExtraKeys.ROLL_ID, rollId);
        args.putBoolean(ExtraKeys.LOCATION_ENABLED, locationPermissionsGranted);
        newFragment.setArguments(args);
        getFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.animator.slide_left,
                        R.animator.slide_right,
                        R.animator.slide_left,
                        R.animator.slide_right)
                .replace(R.id.fragment_container, newFragment, FramesFragment.FRAMES_FRAGMENT_TAG)
                .addToBackStack(null).commit();
        View shadow = findViewById(R.id.shadow);
        shadow.bringToFront();
    }


    /**
     * When the user presses the home as up button in the action bar this function is called.
     * Pop the FramesFragment off the stack.
     */
    public void onHomeAsUpPressed(){
        getFragmentManager().popBackStack();
    }


    /**
     * This function is called if GPS is not enabled.
     * Prompt the user to jump to settings to enable GPS.
     */
    private void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle(R.string.GPSSettings);

        // Setting Dialog Message
        alertDialog.setMessage(R.string.GPSNotEnabled);

        // On pressing Settings button
        alertDialog.setPositiveButton(R.string.GoToSettings, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // On pressing disable button
        alertDialog.setNegativeButton(R.string.DisableInApp, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                final SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putBoolean(PreferenceConstants.KEY_GPS_UPDATE, false);
                prefsEditor.apply();
                dialogInterface.dismiss();
            }
        });

        // On pressing cancel button
        alertDialog.setNeutralButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    /**
     * When the user is asked for permission to use location services, handle the request result.
     *
     * @param requestCode The code of the request that was made.
     * @param permissions not used
     * @param grantResults An array which is empty if the request was cancelled.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case MY_MULTIPLE_PERMISSIONS_REQUEST:

                // If request is cancelled, the result arrays are empty. Thus we check
                // the length of grantResults first.

                //Check location permissions
                if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionsGranted = true;
                }

                //Check write permissions
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    //In case write permission was denied, inform the user.
                    Toast.makeText(this, R.string.NoWritePermission, Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    /**
     * The PreferenceActivity is started for result and the result is captured here in MainActivity.
     * The result is OK if the user has successfully imported a new database.
     *
     * @param requestCode passed to the activity when it is launched
     * @param resultCode OK if the user has successfully imported a new database
     * @param data not used
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case PREFERENCE_ACTIVITY_REQUEST:

                if ((resultCode & PreferenceActivity.RESULT_DATABASE_IMPORTED) ==
                        PreferenceActivity.RESULT_DATABASE_IMPORTED) {

                    Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
                    if (fragment instanceof FramesFragment) {
                        getFragmentManager().popBackStack();
                    }
                    fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
                    if (fragment instanceof RollsFragment) {
                        RollsFragment rollsFragment = (RollsFragment) fragment;
                        rollsFragment.updateFragment();
                    }
                }

                if ((resultCode & PreferenceActivity.RESULT_THEME_CHANGED) ==
                        PreferenceActivity.RESULT_THEME_CHANGED) {
                    recreate();
                }

                return;
        }

        // Call super in case the result was not handled here
        super.onActivityResult(requestCode, resultCode, data);
    }
}

