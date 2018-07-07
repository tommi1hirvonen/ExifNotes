package com.tommihirvonen.exifnotes.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.datastructures.FilterMode;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * AllFramesMapsActivity displays all the frames in the user's database on a map.
 */
public class AllFramesMapsActivity extends AppCompatActivity implements OnMapReadyCallback, PopupMenu.OnMenuItemClickListener {

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * List to hold all the rolls from the database
     */
    private List<Roll> rollList = new ArrayList<>();

    /**
     * GoogleMap object to show the map and to hold all the markers for all frames
     */
    private GoogleMap googleMap_;

    /**
     * Member to indicate whether this activity was continued or not.
     * Some animations will only be activated if this value is false.
     */
    private boolean continueActivity = false;

    /**
     * Sets up the activity's layout and view and reads all the rolls from the database.
     *
     * @param savedInstanceState if not null, then the activity is continued
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        // In onSaveInstanceState a dummy boolean was put into outState.
        // savedInstanceState is not null if the activity was continued.
        if (savedInstanceState != null) continueActivity = true;

        setContentView(R.layout.activity_maps);

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final FilterMode filterMode = FilterMode.fromValue(
                sharedPreferences.getInt(PreferenceConstants.KEY_VISIBLE_ROLLS, FilterMode.ACTIVE.getValue()));

        database = FilmDbHelper.getInstance(this);
        rollList = database.getRolls(filterMode);

        Utilities.setUiColor(this, true);

        // Set the ActionBar title and subtitle.
        if (getSupportActionBar() != null) {
            // Set the subtitle according to which film rolls are shown.
            switch (filterMode) {
                case ACTIVE: default:
                    getSupportActionBar().setSubtitle(R.string.ActiveRolls);
                    break;
                case ARCHIVED:
                    getSupportActionBar().setSubtitle(R.string.ArchivedRolls);
                    break;
                case ALL:
                    getSupportActionBar().setSubtitle(R.string.AllRolls);
                    break;
            }
            getSupportActionBar().setTitle(R.string.AllFrames);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Inflate the menu
     *
     * @param menu the menu to be inflated
     * @return super class to execute code for the menu to work properly.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_maps_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handles the home as up press event.
     *
     * @param item {@inheritDoc}
     * @return call to super
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_item_map_type:
                View menuItemView = findViewById(R.id.menu_item_map_type);
                PopupMenu popupMenu = new PopupMenu(this, menuItemView);
                popupMenu.inflate(R.menu.menu_map_types);
                popupMenu.setOnMenuItemClickListener(this);
                popupMenu.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle the map type PopupMenu item click events
     *
     * @param item MenuItem which was clicked
     * @return true if the item id matches to one of the map type menu items
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        switch (item.getItemId()) {
            case R.id.menu_item_normal:
                googleMap_.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                editor.putInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
                editor.apply();
                return true;
            case R.id.menu_item_hybrid:
                googleMap_.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                editor.putInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_HYBRID);
                editor.apply();
                return true;
            case R.id.menu_item_satellite:
                googleMap_.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                editor.putInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_SATELLITE);
                editor.apply();
                return true;
            case R.id.menu_item_terrain:
                googleMap_.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                editor.putInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_TERRAIN);
                editor.apply();
                return true;
        }
        editor.apply();
        return false;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * In this case, draw markers for all the frames in the user's database.
     *
     * @param googleMap {@inheritDoc}
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap_ = googleMap;

        // If the app's theme is dark, stylize the map with the custom night mode
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (Utilities.isAppThemeDark(getBaseContext())) {
            googleMap_.setMapStyle(new MapStyleOptions(getResources()
                    .getString(R.string.style_json)));
        }

        googleMap_.setMapType(prefs.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL));

        LatLng position;
        List<Marker> markerList = new ArrayList<>();
        List<Frame> frameList;

        // Iterator to change marker color
        int i = 0;
        ArrayList<BitmapDescriptor> markerStyles = new ArrayList<>();
        markerStyles.add(0, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        markerStyles.add(1, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        markerStyles.add(2, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        markerStyles.add(3, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        markerStyles.add(4, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        markerStyles.add(5, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        markerStyles.add(6, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        markerStyles.add(7, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
        markerStyles.add(8, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        markerStyles.add(9, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));

        for (Roll roll : rollList) {

            frameList = database.getAllFramesFromRoll(roll.getId());

            for (Frame frame : frameList) {

                // Parse the latLngLocation string
                String location = frame.getLocation();
                if (location != null && location.length() > 0 && !location.equals("null")) {
                    String latString = location.substring(0, location.indexOf(" "));
                    String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                    double lat = Double.parseDouble(latString.replace(",", "."));
                    double lng = Double.parseDouble(lngString.replace(",", "."));
                    position = new LatLng(lat, lng);
                    String title = "" + roll.getName();
                    String snippet = "#" + frame.getCount();
                    Marker marker = googleMap_.addMarker(new MarkerOptions()
                            .icon(markerStyles.get(i))
                            .position(position)
                            .title(title)
                            .snippet(snippet));
                    marker.setTag(frame);
                    markerList.add(marker);
                }
            }
            ++i;
            if (i > 9) i = 0;
        }

        if (markerList.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : markerList) {
                builder.include(marker.getPosition());
            }
            final LatLngBounds bounds = builder.build();

            if (!continueActivity) {
                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen

                // We use this command where the map's dimensions are specified.
                // This is because on some devices, the map's layout may not have yet occurred
                // (map size is 0).
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                googleMap_.moveCamera(cameraUpdate);
            }

            googleMap_.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {

                    if (marker.getTag() instanceof Frame) {

                        Frame frame = (Frame) marker.getTag();
                        Roll roll = database.getRoll(frame.getRollId());
                        Camera camera = database.getCamera(roll.getCameraId());
                        Lens lens = null;
                        if (frame.getLensId() > 0) lens = database.getLens(frame.getLensId());

                        @SuppressLint("InflateParams")
                        View view = getLayoutInflater().inflate(R.layout.info_window_all_frames, null);

                        TextView rollTextView = view.findViewById(R.id.roll_name);
                        TextView cameraTextView = view.findViewById(R.id.camera);
                        TextView frameCountTextView = view.findViewById(R.id.frame_count);
                        TextView dateTimeTextView = view.findViewById(R.id.date_time);
                        TextView lensTextView = view.findViewById(R.id.lens);
                        TextView noteTextView = view.findViewById(R.id.note);

                        rollTextView.setText(roll.getName());
                        cameraTextView.setText(camera.getName());

                        String frameCountText = "#" + frame.getCount();
                        frameCountTextView.setText(frameCountText);

                        dateTimeTextView.setText(frame.getDate());

                        if (lens != null) {
                            lensTextView.setText(lens.getName());
                        }
                        else {
                            lensTextView.setText(getResources().getString(R.string.NoLens));
                        }

                        noteTextView.setText(frame.getNote());

                        return view;

                    } else {
                        return null;
                    }

                }
            });

            final Activity activity = this;

            googleMap_.setOnInfoWindowClickListener(
                    MapsActivity.mapsActivityInfoWindowClickListener(activity, database)
            );

        }
        else {
            Toast.makeText(this, getResources().getString(R.string.NoFramesToShow), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Puts a dummy boolean in outState so that it is not null.
     *
     * @param outState used to store the dummy boolean
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Insert dummy boolean so that outState is not null.
        outState.putBoolean(ExtraKeys.CONTINUE, true);
    }
}
