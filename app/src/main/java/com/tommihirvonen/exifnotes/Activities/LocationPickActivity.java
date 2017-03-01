package com.tommihirvonen.exifnotes.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tommihirvonen.exifnotes.Utilities.GeocodingAsyncTask;
import com.tommihirvonen.exifnotes.R;

import java.util.Arrays;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class LocationPickActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        View.OnClickListener,
        android.support.v7.widget.SearchView.OnQueryTextListener {

    private GoogleMap googleMap;
    Marker marker;
    LatLng latLngLocation;
    String location = "";
    boolean continueActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( savedInstanceState != null ) continueActivity = true;

        setContentView(R.layout.activity_location_pick);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // ********** Commands to get the action bar and color it **********
        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setTitle(getResources().getString(R.string.PickLocation));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor(secondaryColor));
        }
        // *****************************************************************

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (savedInstanceState != null) {
            location = savedInstanceState.getString("LOCATION");
        } else {
            Intent intent = getIntent();
            location = intent.getStringExtra("LOCATION");
        }
        if (location != null) {
            if (location.length() > 0 && !location.equals("null")) {
                String latString = location.substring(0, location.indexOf(" "));
                String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                double lat = Double.parseDouble(latString.replace(",", "."));
                double lng = Double.parseDouble(lngString.replace(",", "."));
                latLngLocation = new LatLng(lat, lng);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_location_pick, menu);
        // Retrieve the SearchView and plug it into SearchManager
        android.support.v7.widget.SearchView searchView =
                (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(
                        menu.findItem(R.id.action_search));
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnMapClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            this.googleMap.setMyLocationEnabled(true);
        }

        // If the latLngLocation is not empty
        if (location != null) {
            if (location.length() > 0 && !location.equals("null")) {
                String latString = location.substring(0, location.indexOf(" "));
                String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                double lat = Double.parseDouble(latString.replace(",", "."));
                double lng = Double.parseDouble(lngString.replace(",", "."));
                final LatLng position = new LatLng(lat, lng);
                marker = this.googleMap.addMarker(new MarkerOptions().position(position));

                if (!continueActivity)
                    this.googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded() {
                            LocationPickActivity.this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                            // Show hint toast
                            Toast.makeText(getBaseContext(), getResources().getString(R.string.TapOnMap), Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // In case the location was cleared before clicking Edit on map
        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions().position(latLng));
        }
        marker.setPosition(latLng);
        latLngLocation = latLng;
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        // Search for the
        new GeocodingAsyncTask(new GeocodingAsyncTask.AsyncResponse() {
            @Override
            public void processFinish(String output, String formatted_address) {
                if (output.length() != 0 ) {

                    String latString = output.substring(0, output.indexOf(" "));
                    String lngString = output.substring(output.indexOf(" ") + 1, output.length() - 1);
                    double lat = Double.parseDouble(latString.replace(",", "."));
                    double lng = Double.parseDouble(lngString.replace(",", "."));
                    final LatLng position = new LatLng(lat, lng);
                    marker.setPosition(position);
                    latLngLocation = position;
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                    Toast.makeText(getBaseContext(), formatted_address, Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(getBaseContext(), R.string.NoResults, Toast.LENGTH_SHORT).show();
                }
            }
        }).execute(query);

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Do nothing
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:

                if (latLngLocation == null) {
                    Toast.makeText(getBaseContext(), R.string.NoLocation, Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    String latitude = "" + latLngLocation.latitude;
                    String longitude = "" + latLngLocation.longitude;
                    Intent intent = new Intent();
                    intent.putExtra("LATITUDE", latitude);
                    intent.putExtra("LONGITUDE", longitude);
                    setResult(RESULT_OK, intent);
                    finish();
                }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (latLngLocation != null) {
            String latitude = "" + latLngLocation.latitude;
            String longitude = "" + latLngLocation.longitude;
            outState.putBoolean("CONTINUE", true);
            outState.putString("LOCATION", latitude + " " + longitude);
        }
    }
}
