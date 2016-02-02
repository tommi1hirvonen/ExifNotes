package com.tommihirvonen.filmphotonotes;

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

import java.util.Arrays;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class MainActivity extends AppCompatActivity implements RollsFragment.OnRollSelectedListener, FramesFragment.OnHomeAsUpPressedListener {

    private final static int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    boolean locationEnabled = false;


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

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            //firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment, "ROLLSFRAGMENT").commit();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


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

    public void onHomeAsUpPressed(){
        getFragmentManager().popBackStack();
    }






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

