package com.tommihirvonen.exifnotes.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialogCallback;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.fragments.FramesFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * MapsActivity displays all the frames from a roll on a map.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, PopupMenu.OnMenuItemClickListener {

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * List of the roll's frames
     */
    private List<Frame> frameList = new ArrayList<>();

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
     * Inflate the activity, set the UI and get the frames for the selected roll.
     *
     * @param savedInstanceState if not null, then the activity was continued
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getString("AppTheme", "LIGHT").equals("DARK")) {
            setTheme(R.style.Theme_AppCompat);
        }

        database = FilmDbHelper.getInstance(this);

        // In onSaveInstanceState a dummy boolean was put into outState.
        // savedInstanceState is not null if the activity was continued.
        if (savedInstanceState != null) continueActivity = true;

        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();

        long rollId = intent.getLongExtra(FramesFragment.ROLL_ID_EXTRA_MESSAGE, -1);

        // If the rollId is -1, then something went wrong.
        if (rollId == -1) finish();

        FilmDbHelper database = FilmDbHelper.getInstance(this);
        frameList = database.getAllFramesFromRoll(rollId);

        Utilities.setUiColor(this, true);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(database.getRoll(rollId).getName());
            Camera camera = database.getCamera(database.getRoll(rollId).getCameraId());
            getSupportActionBar().setSubtitle(camera.getMake() + " " + camera.getModel());
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
     * Handle the home as up press event
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
                editor.putInt("MAP_TYPE", GoogleMap.MAP_TYPE_NORMAL);
                editor.apply();
                return true;
            case R.id.menu_item_hybrid:
                googleMap_.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                editor.putInt("MAP_TYPE", GoogleMap.MAP_TYPE_HYBRID);
                editor.apply();
                return true;
            case R.id.menu_item_satellite:
                googleMap_.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                editor.putInt("MAP_TYPE", GoogleMap.MAP_TYPE_SATELLITE);
                editor.apply();
                return true;
            case R.id.menu_item_terrain:
                googleMap_.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                editor.putInt("MAP_TYPE", GoogleMap.MAP_TYPE_TERRAIN);
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
     * In this case, draw markers for all the frames on the specified roll.
     *
     * @param googleMap {@inheritDoc}
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap_ = googleMap;

        // If the app's theme is dark, stylize the map with the custom night mode
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getString("AppTheme", "LIGHT").equals("DARK")) {
            googleMap_.setMapStyle(new MapStyleOptions(getResources()
                    .getString(R.string.style_json)));
        }

        googleMap_.setMapType(prefs.getInt("MAP_TYPE", GoogleMap.MAP_TYPE_NORMAL));

        LatLng position;
        List<Marker> markerArrayList = new ArrayList<>();

        for (Frame frame : frameList) {

            // Parse the latLngLocation string
            String location = frame.getLocation();
            if (location != null && location.length() > 0 && !location.equals("null")) {
                String latString = location.substring(0, location.indexOf(" "));
                String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                double lat = Double.parseDouble(latString.replace(",", "."));
                double lng = Double.parseDouble(lngString.replace(",", "."));
                position = new LatLng(lat, lng);
                String title = "#" + frame.getCount();
                String snippet = frame.getDate();

                Marker marker = googleMap_.addMarker(
                        new MarkerOptions().position(position).title(title).snippet(snippet)
                );
                marker.setTag(frame);
                markerArrayList.add(marker);
            }
        }

        if (markerArrayList.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : markerArrayList) {
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

            final GoogleMap.InfoWindowAdapter adapter = new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    if (marker.getTag() instanceof Frame) {

                        final Frame frame = (Frame) marker.getTag();
                        @SuppressLint("InflateParams")
                        View view = getLayoutInflater().inflate(R.layout.info_window, null);

                        TextView frameCountTextView = (TextView) view.findViewById(R.id.frame_count);
                        TextView dateTimeTextView = (TextView) view.findViewById(R.id.date_time);
                        TextView lensTextView = (TextView) view.findViewById(R.id.lens);
                        TextView noteTextView = (TextView) view.findViewById(R.id.note);

                        String frameCountText = "#" + frame.getCount();
                        frameCountTextView.setText(frameCountText);
                        dateTimeTextView.setText(frame.getDate());

                        if (frame.getLensId() > 0) {
                            Lens lens = database.getLens(frame.getLensId());
                            lensTextView.setText(lens.getMake() + " " + lens.getModel());
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
            };

            googleMap_.setInfoWindowAdapter(adapter);

            final AppCompatActivity activity = this;

            googleMap_.setOnInfoWindowClickListener(
                    mapsActivityInfoWindowClickListener(activity, database)
            );

        }
        else {
            Toast.makeText(this, getResources().getString(R.string.NoFramesToShow), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Public static method to create a custom InfoWindowClick listener interface.
     * Also used in AllFramesMapsActivity
     *
     * @param activity calling activity
     * @param database singleton reference to the app's database
     * @return new OnInfoWindowClickListener object
     */
    public static GoogleMap.OnInfoWindowClickListener mapsActivityInfoWindowClickListener(
            final Activity activity, final FilmDbHelper database) {

        return new GoogleMap.OnInfoWindowClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onInfoWindowClick(final Marker marker) {
                if (marker.getTag() instanceof Frame) {

                    Frame frame = (Frame) marker.getTag();

                    if (frame != null) {

                        Bundle arguments = new Bundle();
                        String title = "" + activity.getResources().getString(R.string.EditFrame) + frame.getCount();
                        String positiveButton = activity.getResources().getString(R.string.OK);
                        arguments.putString("TITLE", title);
                        arguments.putString("POSITIVE_BUTTON", positiveButton);
                        arguments.putParcelable("FRAME", frame);

                        EditFrameDialogCallback dialog = new EditFrameDialogCallback();
                        dialog.setArguments(arguments);
                        dialog.setOnPositiveButtonClickedListener(
                                new EditFrameDialogCallback.OnPositiveButtonClickedListener() {
                                    @Override
                                    public void onPositiveButtonClicked(int requestCode, int resultCode, Intent data) {
                                        Frame editedFrame = data.getParcelableExtra("FRAME");
                                        if (editedFrame != null) {
                                            database.updateFrame(editedFrame);
                                            marker.setTag(editedFrame);
                                            marker.hideInfoWindow();
                                            marker.showInfoWindow();
                                            activity.setResult(Activity.RESULT_OK);
                                        }
                                    }
                                });
                        dialog.show(activity.getFragmentManager().beginTransaction(), EditFrameDialog.TAG);

                    }
                }
            }
        };
    }

    /**
     * Store a dummy boolean to outState to indicate that the activity will be resumed.
     * @param outState used to store the dummy boolean
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Insert dummy boolean so that outState is not null.
        outState.putBoolean("CONTINUE", true);
    }

}
