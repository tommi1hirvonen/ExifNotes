package com.tommihirvonen.exifnotes.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.tommihirvonen.exifnotes.datastructures.Location;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.GeocodingAsyncTask;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

/**
 * Allows the user to select a location for a frame on a map.
 */
public class LocationPickActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        View.OnClickListener,
        androidx.appcompat.widget.SearchView.OnQueryTextListener {

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

    private String googleMapsApiKey;

    /**
     * Member to indicate whether this activity was continued or not.
     * Some animations will only be activated if this value is false.
     */
    private boolean continueActivity = false;

    /**
     * Holds reference to the GoogleMap map type
     */
    private int mapType;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // If the device is locked, this activity can be shown regardless.
        // This way the user doesn't have to unlock the device with authentication
        // just to access this activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            final Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        if (savedInstanceState != null) continueActivity = true;

        setContentView(R.layout.activity_location_pick);

        final FloatingActionButton confirmFab = findViewById(R.id.fab);
        confirmFab.setOnClickListener(this);

        final FloatingActionButton currentLocationFab = findViewById(R.id.fab_current_location);
        currentLocationFab.setOnClickListener(this);

        Utilities.setUiColor(this, true);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getResources().getString(R.string.PickLocation));
        }

        final int secondaryColor = Utilities.getSecondaryUiColor(getBaseContext());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        confirmFab.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        // In case the app's theme is dark, color the bottom bar dark grey
        if (Utilities.isAppThemeDark(getBaseContext())) {
            final FrameLayout bottomBarLayout = findViewById(R.id.bottom_bar);
            bottomBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_grey));
        }

        progressBar = findViewById(R.id.progress_bar);

        formattedAddressTextView = findViewById(R.id.formatted_address);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        googleMapsApiKey = getResources().getString(R.string.google_maps_key);

        // If the activity is continued, then savedInstanceState is not null.
        // Get the location from there.
        final Location location;
        if (savedInstanceState != null) {
            location = savedInstanceState.getParcelable(ExtraKeys.LOCATION);
            formattedAddress = savedInstanceState.getString(ExtraKeys.FORMATTED_ADDRESS);
        } else {
            // Else get the location from Intent.
            final Intent intent = getIntent();
            location = intent.getParcelableExtra(ExtraKeys.LOCATION);
            formattedAddress = intent.getStringExtra(ExtraKeys.FORMATTED_ADDRESS);
        }
        if (location != null) {
            latLngLocation = location.getLatLng();
            // If the formatted address is set, display it
            if (formattedAddress != null && formattedAddress.length() > 0) {
                formattedAddressTextView.setText(formattedAddress);
            }
            // The formatted address was not set, try to query it
            else {
                progressBar.setVisibility(View.VISIBLE);
                new GeocodingAsyncTask((output, formatted_address) -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    if (formatted_address.length() != 0) {
                        formattedAddressTextView.setText(formatted_address);
                        formattedAddress = formatted_address;
                    } else {
                        formattedAddressTextView.setText(R.string.AddressNotFound);
                        formattedAddress = null;
                    }
                }).execute(location.getDecimalLocation(), googleMapsApiKey);
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_location_pick, menu);
        // Retrieve the SearchView and plug it into SearchManager
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // The SearchView's query hint is localization dependant by default.
        // Replace it with the English text.
        searchView.setQueryHint(getString(R.string.SearchWEllipsis));
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        switch (mapType) {
            case GoogleMap.MAP_TYPE_NORMAL:
            default:
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

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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
    private void setMapType(final int mapType) {
        this.mapType = mapType;
        googleMap_.setMapType(mapType);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType);
        editor.apply();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
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

        if (latLngLocation != null) {
            marker = googleMap_.addMarker(new MarkerOptions().position(latLngLocation));
            if (!continueActivity) {
                LocationPickActivity.this.googleMap_.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngLocation, 15));
                Toast.makeText(getBaseContext(), getResources().getString(R.string.TapOnMap), Toast.LENGTH_SHORT).show();
            }

        }

    }

    @Override
    public void onMapClick(final LatLng latLng) {

        // Get the formatted address
        formattedAddressTextView.setText("");
        progressBar.setVisibility(View.VISIBLE);
        final String latitude = "" + latLng.latitude;
        final String longitude = "" + latLng.longitude;
        final String query = latitude + " " + longitude;
        new GeocodingAsyncTask((output, formatted_address) -> {
            progressBar.setVisibility(View.INVISIBLE);
            if (formatted_address.length() != 0) {
                formattedAddressTextView.setText(formatted_address);
                formattedAddress = formatted_address;
            } else {
                formattedAddressTextView.setText(R.string.AddressNotFound);
                formattedAddress = null;
            }
        }).execute(query, googleMapsApiKey);

        // if the location was cleared before editing -> add marker to selected location
        if (marker == null) marker = googleMap_.addMarker(new MarkerOptions().position(latLng));
            // otherwise set the position
        else marker.setPosition(latLng);
        latLngLocation = latLng;
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        // When the user enters the search string, use GeocodingAsyncTask to get
        // the formatted address and coordinates. Also move the marker if the result was valid.

        formattedAddressTextView.setText("");
        progressBar.setVisibility(View.VISIBLE);
        new GeocodingAsyncTask((output, formatted_address) -> {
            progressBar.setVisibility(View.INVISIBLE);
            if (output.length() != 0) {
                final Location location = new Location(output);
                final LatLng position = location.getLatLng();
                if (position != null) {
                    // marker is null, if the search was made before the marker has been added
                    // -> add marker to selected location
                    if (marker == null)
                        marker = googleMap_.addMarker(new MarkerOptions().position(position));
                    else marker.setPosition(position); // otherwise just set the location
                    latLngLocation = position;
                    googleMap_.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                }
                formattedAddressTextView.setText(formatted_address);
                formattedAddress = formatted_address;

            } else {
                formattedAddressTextView.setText(R.string.AddressNotFound);
                formattedAddress = null;
            }
        }).execute(query, googleMapsApiKey);

        return false;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        // Do nothing
        return false;
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.fab) {
            if (latLngLocation == null) {
                Toast.makeText(getBaseContext(), R.string.NoLocation, Toast.LENGTH_SHORT).show();
            } else {
                final Intent intent = new Intent();
                intent.putExtra(ExtraKeys.LOCATION, new Location(latLngLocation));
                intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, formattedAddress);
                setResult(RESULT_OK, intent);
                finish();
            }
        } else if (v.getId() == R.id.fab_current_location) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling ActivityCompat.requestPermissions()
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    final LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
                    onMapClick(position);
                    googleMap_.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store the currently set location to outState.
        if (latLngLocation != null) {
            outState.putParcelable(ExtraKeys.LOCATION, new Location(latLngLocation));
            outState.putString(ExtraKeys.FORMATTED_ADDRESS, formattedAddress);
        }
    }

}
