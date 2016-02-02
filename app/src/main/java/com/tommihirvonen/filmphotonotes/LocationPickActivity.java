package com.tommihirvonen.filmphotonotes;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.List;


public class LocationPickActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, SearchView.OnQueryTextListener, View.OnClickListener {

    private GoogleMap mMap;
    Marker marker;

    LatLng latlng_location;
    String location = "";

    boolean continue_activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( savedInstanceState != null ) continue_activity = true;
        else continue_activity = false;

        setContentView(R.layout.activity_location_pick);

        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

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
        getSupportActionBar().setTitle(getResources().getString(R.string.PickLocation));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
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

        if ( savedInstanceState != null ) {
            location = savedInstanceState.getString("LOCATION");
        } else {
            Intent intent = getIntent();
            location = intent.getStringExtra("LOCATION");
        }
        if (location.length() > 0 && !location.equals("null")) {
            String latString = location.substring(0, location.indexOf(" "));
            String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
            double lat = Double.parseDouble(latString.replace(",", "."));
            double lng = Double.parseDouble(lngString.replace(",", "."));
            latlng_location = new LatLng(lat, lng);
        }
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
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setPadding(0, 180, 0, 0);



        // If the latlng_location is not empty
        if (location.length() > 0 && !location.equals("null")) {
            String latString = location.substring(0, location.indexOf(" "));
            String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
            double lat = Double.parseDouble(latString.replace(",", "."));
            double lng = Double.parseDouble(lngString.replace(",", "."));
            final LatLng position = new LatLng(lat, lng);
            marker = mMap.addMarker(new MarkerOptions().position(position));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }

            if ( !continue_activity ) mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                    // Show hint toast
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.TapOnMap), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        marker.setPosition(latLng);
        latlng_location = latLng;
    }


    @Override
    public boolean onQueryTextSubmit(String query) {

        // Search for the
        new GeocodingAsyncTask(new GeocodingAsyncTask.AsyncResponse() {
            @Override
            public void processFinish(String output) {
                if ( output.length() != 0 ) {

                    String latString = output.substring(0, output.indexOf(" "));
                    String lngString = output.substring(output.indexOf(" ") + 1, output.length() - 1);
                    double lat = Double.parseDouble(latString.replace(",", "."));
                    double lng = Double.parseDouble(lngString.replace(",", "."));
                    final LatLng position = new LatLng(lat, lng);
                    marker.setPosition(position);
                    latlng_location = position;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));

                } else {
                    Toast.makeText(getBaseContext(), "No results!", Toast.LENGTH_SHORT).show();
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
                String latitude = "" + latlng_location.latitude;
                String longitude = "" + latlng_location.longitude;
                Intent intent = new Intent();
                intent.putExtra("LATITUDE", latitude);
                intent.putExtra("LONGITUDE", longitude);
                setResult(RESULT_OK, intent);
                finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String latitude = "" + latlng_location.latitude;
        String longitude = "" + latlng_location.longitude;
        outState.putBoolean("CONTINUE", true);
        outState.putString("LOCATION", latitude + " " + longitude);
    }
}
