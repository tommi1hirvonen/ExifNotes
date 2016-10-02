package com.tommihirvonen.exifnotes.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.tommihirvonen.exifnotes.Dialogs.SimpleEula;
import com.tommihirvonen.exifnotes.Fragments.FramesFragment;
import com.tommihirvonen.exifnotes.Fragments.RollsFragment;
import com.tommihirvonen.exifnotes.R;

import java.io.File;
import java.util.Arrays;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

/**
 * MainActivity is the first activity to be called when the app is launched.
 * It contains the RollsFragment and FramesFragment fragments.
 * The activity switches between these two fragments.
 */
public class MainActivity extends AppCompatActivity implements RollsFragment.OnRollSelectedListener, FramesFragment.OnHomeAsUpPressedListener {

    private final static int MY_PERMISSIONS_REQUEST_LOCATION = 1;
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

        // ********** Commands to get the action bar and color it **********
        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setTitle("  " + getResources().getString(R.string.MainActivityTitle));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor( Color.parseColor(secondaryColor) );
        }
        // *****************************************************************

        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        // Check if the app has latlng_location permission.
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationEnabled = true;

        }
        // It does not. Show dialog to request permission.
        else ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION);

        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if ( !isGPSEnabled ) showSettingsAlert();


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
                    .add(R.id.fragment_container, firstFragment, "ROLLSFRAGMENT").commit();
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(secondaryColor));
        }
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

//    /**
//     * This function is called when MainActivity is stopped, in other words when the
//     * application is stopped. All the files created in FramesFragment.setShareIntentExportRoll
//     * in the application's external storage directory are deleted.
//     */
//    @Override
//    public void onStop(){
//        //Delete all the files created in FramesFragment.setShareIntentExportRoll
//        File externalStorageDir = getExternalFilesDir(null);
//        purgeDirectory(externalStorageDir);
//        super.onStop();
//    }
//
//    /**
//     * This function deletes all the files in a directory
//     *
//     * @param dir the directory whose files are to be deleted
//     */
//    private void purgeDirectory(File dir) {
//        for(File file: dir.listFiles()) {
//            if (!file.isDirectory()) {
//                file.delete();
//            }
//        }
//    }

    /**
     * This function is called when the user presses a roll in the RollsFragment.
     * Create a new FramesFragment, pass the roll id and locationEnabled to it and bring it to the
     * front of the stack.
     * Also bring the shadow under the action bar to front.
     *
     * @param rollId The id of the roll that was pressed.
     */
    @Override
    public void onRollSelected(int rollId){
        FramesFragment newFragment = new FramesFragment();
        Bundle args = new Bundle();
        args.putInt("ROLL_ID", rollId);
        args.putBoolean("LOCATION_ENABLED", locationEnabled);
        newFragment.setArguments(args);
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_left, R.animator.slide_right, R.animator.slide_left, R.animator.slide_right)
                .replace(R.id.fragment_container, newFragment, "FRAMESFRAGMENT")
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    locationEnabled = true;

                }
            }
        }
    }
}

