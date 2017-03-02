package com.tommihirvonen.exifnotes.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.Dialogs.SimpleEula;
import com.tommihirvonen.exifnotes.Fragments.FramesFragment;
import com.tommihirvonen.exifnotes.Fragments.RollsFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.io.File;

// Copyright 2015
// Tommi Hirvonen

/**
 * MainActivity is the first activity to be called when the app is launched.
 * It contains the RollsFragment and FramesFragment fragments.
 * The activity switches between these two fragments.
 */
public class MainActivity extends AppCompatActivity implements
        RollsFragment.OnRollSelectedListener,
        FramesFragment.OnHomeAsUpPressedListener {

    private final static int MY_MULTIPLE_PERMISSIONS_REQUEST = 1;
    boolean locationEnabled = false;

    /**
     * Create the layout and activate location services.
     *
     * @param savedInstanceState passed to super.onCreate to execute necessary code to properly
     *                           create the fragment
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            locationEnabled = true;
        }



        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGPSEnabled) showSettingsAlert();


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
     * Called when the user selects a menu item.
     *
     * @param item The menu item that was selected.
     * @return super so that other handlers can handle the selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
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
     * Create a new FramesFragment, pass the roll id and locationEnabled to it and bring it to the
     * front of the stack.
     * Also bring the shadow under the action bar to front.
     *
     * @param rollId The id of the roll that was pressed.
     */
    @Override
    public void onRollSelected(long rollId){
        FramesFragment newFragment = new FramesFragment();
        Bundle args = new Bundle();
        args.putLong("ROLL_ID", rollId);
        args.putBoolean("LOCATION_ENABLED", locationEnabled);
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
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle(R.string.GPSSettings);

        // Setting Dialog Message
        alertDialog.setMessage(R.string.GPSNotEnabled);

        // On pressing Settings button
        alertDialog.setPositiveButton(R.string.Settings, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
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
                    locationEnabled = true;
                }

                //Check write permissions
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    //In case write permission was denied, inform the user.
                    Toast.makeText(this, R.string.NoWritePermission, Toast.LENGTH_LONG).show();
                }

                break;
        }
    }
}

