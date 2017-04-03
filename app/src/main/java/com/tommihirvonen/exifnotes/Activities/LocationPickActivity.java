package com.tommihirvonen.exifnotes.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.tommihirvonen.exifnotes.Utilities.Utilities;

/**
 * LocationPickActivity allows the user to select a location for a frame on a map.
 */
public class LocationPickActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        View.OnClickListener,
        android.support.v7.widget.SearchView.OnQueryTextListener {

    /**
     * GoogleMap object to show the map and marker
     */
    private GoogleMap googleMap;

    /**
     * Marker object to hold the marker added/moved by the user.
     */
    Marker marker;

    /**
     * Holds the current location.
     */
    LatLng latLngLocation;

    /**
     * Stores the location which is received when the activity is started or resumed.
     */
    String location = "";

    /**
     * Member to indicate whether this acitivty was continued or not.
     * Some animations will only be activated if this value is false.
     */
    boolean continueActivity = false;

    /**
     * Inflate the activity, set the UI and get the initial location.
     *
     * @param savedInstanceState if not null then the activity is continued
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) continueActivity = true;

        setContentView(R.layout.activity_location_pick);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);

        Utilities.setUiColor(this, true);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(
                getResources().getString(R.string.PickLocation));

        int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // If the activity is continued, then savedInstanceState is not null.
        // Get the location from there.
        if (savedInstanceState != null) {
            location = savedInstanceState.getString("LOCATION");
        } else {
            // Else get the location from Intent.
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

    /**
     * Inflate the options menu with a search bar for location search functionality.
     *
     * @param menu {@inheritDoc}
     * @return call to super
     */
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

    /**
     * Handle home as up press event.
     *
     * @param item {@inheritDoc}
     * @return call to super
     */
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
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * In this case, add marker to display the currently selected location.
     *
     * @param googleMap {@inheritDoc}
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

    /**
     * When the user presses on the map, move or add marker to the selected location.
     *
     * @param latLng {@inheritDoc}
     */
    @Override
    public void onMapClick(LatLng latLng) {
        // In case the location was cleared before clicking Edit on map
        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions().position(latLng));
        }
        marker.setPosition(latLng);
        latLngLocation = latLng;
    }

    /**
     * When the user enters the search string, use GeocodingAsyncTask to get
     * the formatted address and coordinates. Also move the marker if the result was valid.
     *
     * @param query the string the user wrote to the search field
     * @return always false
     */
    @Override
    public boolean onQueryTextSubmit(String query) {

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

    /**
     * Not used
     *
     * @param newText {@inheritDoc}
     * @return always false
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        // Do nothing
        return false;
    }

    /**
     * Handle the FloatingActionButton press event, confirm the location.
     * Break, if no location was set.
     *
     * @param v {@inheritDoc}
     */
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

    /**
     * Store the currently set location to outState.
     *
     * @param outState used to store the currently set location
     */
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
