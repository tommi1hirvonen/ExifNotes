package com.tommihirvonen.exifnotes.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.GeocodingAsyncTask;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

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
    private GoogleMap googleMap_;

    /**
     * Marker object to hold the marker added/moved by the user.
     */
    private Marker marker;

    /**
     * ProgressBar object displayed when the formatted address is queried
     */
    private ProgressBar progressBar;

    /**
     * TextView which displays the current formatted address
     */
    private TextView formattedAddressTextView;

    /**
     * String to hold the current formatted address
     */
    private String formattedAddress;

    /**
     * Holds the current location.
     */
    private LatLng latLngLocation;

    /**
     * Stores the location which is received when the activity is started or resumed.
     */
    private String location = "";

    private String googleMapsApiKey;

    /**
     * Member to indicate whether this acitivty was continued or not.
     * Some animations will only be activated if this value is false.
     */
    private boolean continueActivity = false;

    /**
     * Holds reference to the GoogleMap map type
     */
    private int mapType;

    /**
     * Inflate the activity, set the UI and get the initial location.
     *
     * @param savedInstanceState if not null then the activity is continued
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        if (savedInstanceState != null) continueActivity = true;

        setContentView(R.layout.activity_location_pick);

        FloatingActionButton confirmFab = findViewById(R.id.fab);
        confirmFab.setOnClickListener(this);

        Utilities.setUiColor(this, true);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getResources().getString(R.string.PickLocation));
        }

        int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        confirmFab.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        // In case the app's theme is dark, color the bottom bar dark grey
        if (Utilities.isAppThemeDark(getBaseContext())) {
            FrameLayout bottomBarLayout = findViewById(R.id.bottom_bar);
            bottomBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_grey));
        }

        progressBar = findViewById(R.id.progress_bar);

        formattedAddressTextView = findViewById(R.id.formatted_address);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        googleMapsApiKey = getResources().getString(R.string.google_maps_key);

        // If the activity is continued, then savedInstanceState is not null.
        // Get the location from there.
        if (savedInstanceState != null) {
            location = savedInstanceState.getString(ExtraKeys.LOCATION);
            formattedAddress = savedInstanceState.getString(ExtraKeys.FORMATTED_ADDRESS);
        } else {
            // Else get the location from Intent.
            Intent intent = getIntent();
            location = intent.getStringExtra(ExtraKeys.LOCATION);
            formattedAddress = intent.getStringExtra(ExtraKeys.FORMATTED_ADDRESS);
        }
        if (location != null) {
            if (location.length() > 0 && !location.equals("null")) {

                // Parse the location
                String latString = location.substring(0, location.indexOf(" "));
                String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                double lat = Double.parseDouble(latString.replace(",", "."));
                double lng = Double.parseDouble(lngString.replace(",", "."));
                latLngLocation = new LatLng(lat, lng);

                // If the formatted address is set, display it
                if (formattedAddress != null && formattedAddress.length() > 0) {
                    formattedAddressTextView.setText(formattedAddress);
                }
                // The formatted address was not set, try to query it
                else {
                    progressBar.setVisibility(View.VISIBLE);
                    new GeocodingAsyncTask(new GeocodingAsyncTask.AsyncResponse() {
                        @Override
                        public void processFinish(String output, String formatted_address) {
                            progressBar.setVisibility(View.INVISIBLE);
                            if (formatted_address.length() != 0 ) {
                                formattedAddressTextView.setText(formatted_address);
                                formattedAddress = formatted_address;
                            } else {
                                formattedAddressTextView.setText(R.string.AddressNotFound);
                                formattedAddress = null;
                            }
                        }
                    }).execute(location, googleMapsApiKey);
                }
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
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // The SearchView's query hint is localization dependant by default.
        // Replace it with the English text.
        searchView.setQueryHint(getString(R.string.SearchWEllipsis));
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (mapType) {
            case GoogleMap.MAP_TYPE_NORMAL: default:
                menu.findItem(R.id.menu_item_normal).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                menu.findItem(R.id.menu_item_hybrid).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                menu.findItem(R.id.menu_item_satellite).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                menu.findItem(R.id.menu_item_terrain).setChecked(true);
                break;
        }
        return super.onPrepareOptionsMenu(menu);
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

            case R.id.menu_item_normal:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.menu_item_hybrid:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            case R.id.menu_item_satellite:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            case R.id.menu_item_terrain:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the GoogleMap map type
     *
     * @param mapType One of the map type constants from class GoogleMap
     */
    private void setMapType(int mapType) {
        this.mapType = mapType;
        googleMap_.setMapType(mapType);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType);
        editor.apply();
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
        googleMap_ = googleMap;
        googleMap_.setOnMapClickListener(this);

        // If the app's theme is dark, stylize the map with the custom night mode
        if (Utilities.isAppThemeDark(getBaseContext())) {
            googleMap_.setMapStyle(new MapStyleOptions(getResources()
                    .getString(R.string.style_json)));
        }

        googleMap_.setMapType(mapType);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            googleMap_.setMyLocationEnabled(true);
        }

        // If the latLngLocation is not empty
        if (location != null) {
            if (location.length() > 0 && !location.equals("null")) {
                String latString = location.substring(0, location.indexOf(" "));
                String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                double lat = Double.parseDouble(latString.replace(",", "."));
                double lng = Double.parseDouble(lngString.replace(",", "."));
                final LatLng position = new LatLng(lat, lng);
                marker = googleMap_.addMarker(new MarkerOptions().position(position));

                if (!continueActivity) {
                    LocationPickActivity.this.googleMap_.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.TapOnMap), Toast.LENGTH_SHORT).show();
                }

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

        // Get the formatted address
        formattedAddressTextView.setText("");
        progressBar.setVisibility(View.VISIBLE);
        String latitude = "" + latLng.latitude;
        String longitude = "" + latLng.longitude;
        String query = latitude + " " + longitude;
        new GeocodingAsyncTask(new GeocodingAsyncTask.AsyncResponse() {
            @Override
            public void processFinish(String output, String formatted_address) {
                progressBar.setVisibility(View.INVISIBLE);
                if (formatted_address.length() != 0 ) {
                    formattedAddressTextView.setText(formatted_address);
                    formattedAddress = formatted_address;
                } else {
                    formattedAddressTextView.setText(R.string.AddressNotFound);
                    formattedAddress = null;
                }
            }
        }).execute(query, googleMapsApiKey);

        // if the location was cleared before editing -> add marker to selected location
        if (marker == null) marker = googleMap_.addMarker(new MarkerOptions().position(latLng));
        // otherwise set the position
        else marker.setPosition(latLng);
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
        formattedAddressTextView.setText("");
        progressBar.setVisibility(View.VISIBLE);
        new GeocodingAsyncTask(new GeocodingAsyncTask.AsyncResponse() {
            @Override
            public void processFinish(String output, String formatted_address) {
                progressBar.setVisibility(View.INVISIBLE);
                if (output.length() != 0 ) {

                    String latString = output.substring(0, output.indexOf(" "));
                    String lngString = output.substring(output.indexOf(" ") + 1, output.length() - 1);
                    double lat = Double.parseDouble(latString.replace(",", "."));
                    double lng = Double.parseDouble(lngString.replace(",", "."));
                    final LatLng position = new LatLng(lat, lng);
                    // marker is null, if the search was made before the marker has been added
                    // -> add marker to selected location
                    if (marker == null) marker = googleMap_.addMarker(new MarkerOptions().position(position));
                    // otherwise just set the location
                    else marker.setPosition(position);
                    latLngLocation = position;
                    googleMap_.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                    formattedAddressTextView.setText(formatted_address);
                    formattedAddress = formatted_address;

                } else {
                    formattedAddressTextView.setText(R.string.AddressNotFound);
                    formattedAddress = null;
                }
            }
        }).execute(query, googleMapsApiKey);

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
                    intent.putExtra(ExtraKeys.LATITUDE, latitude);
                    intent.putExtra(ExtraKeys.LONGITUDE, longitude);
                    intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, formattedAddress);
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
            outState.putBoolean(ExtraKeys.CONTINUE, true);
            outState.putString(ExtraKeys.LOCATION, latitude + " " + longitude);
        }
    }

}
