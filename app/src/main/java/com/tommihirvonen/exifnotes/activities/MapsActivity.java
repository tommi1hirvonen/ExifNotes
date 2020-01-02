package com.tommihirvonen.exifnotes.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

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
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialogCallback;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.fragments.FramesFragment;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * MapsActivity displays all the frames from a roll on a map.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

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
     * Holds reference to the GoogleMap map type
     */
    private int mapType;

    /**
     * Inflate the activity, set the UI and get the frames for the selected roll.
     *
     * @param savedInstanceState if not null, then the activity was continued
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);

        super.onCreate(savedInstanceState);

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        database = FilmDbHelper.getInstance(this);

        // In onSaveInstanceState a nothing boolean was put into outState.
        // savedInstanceState is not null if the activity was continued.
        if (savedInstanceState != null) continueActivity = true;

        setContentView(R.layout.activity_maps);
        final Intent intent = getIntent();

        final long rollId = intent.getLongExtra(FramesFragment.ROLL_ID_EXTRA_MESSAGE, -1);

        // If the rollId is -1, then something went wrong.
        if (rollId == -1) finish();

        final Roll roll = database.getRoll(rollId);

        if (roll != null) {

            frameList = database.getAllFramesFromRoll(roll);

            Utilities.setUiColor(this, true);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(roll.getName());
                final Camera camera = database.getCamera(roll.getCameraId());
                getSupportActionBar().setSubtitle(
                        camera != null ? camera.getName() : getString(R.string.NoCamera)
                );
            }

        }

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right);
    }

    /**
     * Inflate the menu
     *
     * @param menu the menu to be inflated
     * @return super class to execute code for the menu to work properly.
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_maps_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
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
     * Handle the home as up press event
     *
     * @param item {@inheritDoc}
     * @return call to super
     */
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
    public void onMapReady(final GoogleMap googleMap) {
        googleMap_ = googleMap;

        // If the app's theme is dark, stylize the map with the custom night mode
        if (Utilities.isAppThemeDark(getBaseContext())) {
            googleMap_.setMapStyle(new MapStyleOptions(getResources()
                    .getString(R.string.style_json)));
        }


        googleMap_.setMapType(mapType);

        LatLng position;
        final List<Marker> markerArrayList = new ArrayList<>();

        for (final Frame frame : frameList) {

            // Parse the latLngLocation string
            final String location = frame.getLocation();
            if (location != null && location.length() > 0 && !location.equals("null")) {
                final String latString = location.substring(0, location.indexOf(" "));
                final String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                final double lat = Double.parseDouble(latString.replace(",", "."));
                final double lng = Double.parseDouble(lngString.replace(",", "."));
                position = new LatLng(lat, lng);
                final String title = "#" + frame.getCount();
                final String snippet = frame.getDate();

                final Marker marker = googleMap_.addMarker(
                        new MarkerOptions().position(position).title(title).snippet(snippet)
                );
                marker.setTag(frame);
                markerArrayList.add(marker);
            }
        }

        if (markerArrayList.size() > 0) {
            final LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (final Marker marker : markerArrayList) {
                builder.include(marker.getPosition());
            }
            final LatLngBounds bounds = builder.build();

            if (!continueActivity) {
                final int width = getResources().getDisplayMetrics().widthPixels;
                final int height = getResources().getDisplayMetrics().heightPixels;
                final int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen

                // We use this command where the map's dimensions are specified.
                // This is because on some devices, the map's layout may not have yet occurred
                // (map size is 0).
                final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                googleMap_.moveCamera(cameraUpdate);
            }

            final GoogleMap.InfoWindowAdapter adapter = new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(final Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(final Marker marker) {
                    if (marker.getTag() instanceof Frame) {

                        final Frame frame = (Frame) marker.getTag();
                        @SuppressLint("InflateParams")
                        final View view = getLayoutInflater().inflate(R.layout.info_window, null);

                        final TextView frameCountTextView = view.findViewById(R.id.frame_count);
                        final TextView dateTimeTextView = view.findViewById(R.id.date_time);
                        final TextView lensTextView = view.findViewById(R.id.lens);
                        final TextView noteTextView = view.findViewById(R.id.note);

                        final String frameCountText = "#" + frame.getCount();
                        frameCountTextView.setText(frameCountText);
                        dateTimeTextView.setText(frame.getDate());

                        final Lens lens = database.getLens(frame.getLensId());
                        lensTextView.setText(
                                lens != null ? lens.getName() : getString(R.string.NoLens)
                        );

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
            final AppCompatActivity activity, final FilmDbHelper database) {

        return marker -> {
            if (marker.getTag() instanceof Frame) {

                final Frame frame = (Frame) marker.getTag();

                if (frame != null) {

                    final Bundle arguments = new Bundle();
                    final String title = "" + activity.getResources().getString(R.string.EditFrame) + frame.getCount();
                    final String positiveButton = activity.getResources().getString(R.string.OK);
                    arguments.putString(ExtraKeys.TITLE, title);
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton);
                    arguments.putParcelable(ExtraKeys.FRAME, frame);

                    final EditFrameDialogCallback dialog = new EditFrameDialogCallback();
                    dialog.setArguments(arguments);
                    dialog.setOnPositiveButtonClickedListener(
                            data -> {
                                final Frame editedFrame = data.getParcelableExtra(ExtraKeys.FRAME);
                                if (editedFrame != null) {
                                    database.updateFrame(editedFrame);
                                    marker.setTag(editedFrame);
                                    marker.hideInfoWindow();
                                    marker.showInfoWindow();
                                    activity.setResult(AppCompatActivity.RESULT_OK);
                                }
                            });
                    dialog.show(activity.getSupportFragmentManager().beginTransaction(), EditFrameDialog.TAG);

                }
            }
        };
    }

    /**
     * Store a nothing boolean to outState to indicate that the activity will be resumed.
     * @param outState used to store the nothing boolean
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Insert nothing boolean so that outState is not null.
        outState.putBoolean(ExtraKeys.CONTINUE, true);
    }

}
