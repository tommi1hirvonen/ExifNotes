package com.tommihirvonen.exifnotes.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialogCallback;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity to display all the frames from a list of rolls on a map.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    private List<Pair<Roll, Boolean>> allRolls = new ArrayList<>();

    /**
     * List to hold all the rolls from the database
     */
    private List<Pair<Roll, Bitmap>> selectedRolls = new ArrayList<>();

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

    private final List<Marker> markerList = new ArrayList<>();

    private List<Bitmap> markerBitmaps;

    private BottomSheetBehavior bottomSheetBehavior;

    private RollMarkerAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);

        super.onCreate(savedInstanceState);

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        // In onSaveInstanceState a dummy boolean was put into outState.
        // savedInstanceState is not null if the activity was continued.
        if (savedInstanceState != null) continueActivity = true;

        // Set the UI
        setContentView(R.layout.activity_map);

        Utilities.setUiColor(this, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(
                    getIntent().getStringExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE)
            );
            getSupportActionBar().setTitle(
                    getIntent().getStringExtra(ExtraKeys.MAPS_ACTIVITY_TITLE)
            );
        }

        // Set the bottom sheet
        final View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        final float peekHeightOffset = getResources().getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {

            }
            @Override
            public void onSlide(@NonNull View view, float v) {
                final float offset = bottomSheet.getHeight() * v + peekHeightOffset - peekHeightOffset * v;
                switch (bottomSheetBehavior.getState()) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        googleMap_.setPadding(0, 0, 0, Math.round(offset));
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                    case BottomSheetBehavior.STATE_EXPANDED:
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                }
            }
        });

        // Get map type from preferences
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);

        // Set the roll list and other arrays
        database = FilmDbHelper.getInstance(this);
        List<Roll> rollListTemp = getIntent().getParcelableArrayListExtra(ExtraKeys.ARRAY_LIST_ROLLS);
        if (rollListTemp == null) {
            rollListTemp = new ArrayList<>();
        }
        for (Roll roll : rollListTemp) {
            allRolls.add(new Pair<>(roll, true));
        }
        // If only one roll can be displayed, hide the bottom sheet.
        if (allRolls.size() == 1) {
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        markerBitmaps = getMarkerBitmapList();

        // Set the list view and adapter
        final ListView listView = findViewById(R.id.rolls_list_view);
        adapter = new RollMarkerAdapter(this, selectedRolls);
        listView.setAdapter(adapter);
        updateSelectedRolls();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_activity, menu);
        // If only one roll is displayed, hide the filter icon.
        if (allRolls.size() == 1) {
            menu.findItem(R.id.menu_item_filter).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        // If the GoogleMap was not initialized, disable map type menu items.
        // This can happen when Play services are not installed on the device.
        if (googleMap_ == null) {
            menu.findItem(R.id.menu_item_normal).setEnabled(false);
            menu.findItem(R.id.menu_item_hybrid).setEnabled(false);
            menu.findItem(R.id.menu_item_satellite).setEnabled(false);
            menu.findItem(R.id.menu_item_terrain).setEnabled(false);
        }
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

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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

            case R.id.menu_item_filter:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final String[] rollNames = new String[allRolls.size()];
                for (int i = 0; i < allRolls.size(); ++i) {
                    rollNames[i] = allRolls.get(i).first.getName();
                }
                final boolean[] selectedRollsTemp = new boolean[allRolls.size()];
                for (int i = 0; i < selectedRollsTemp.length; i++) {
                    selectedRollsTemp[i] = allRolls.get(i).second;
                }
                builder.setMultiChoiceItems(rollNames, selectedRollsTemp, (dialog, which, isChecked) ->
                        selectedRollsTemp[which] = isChecked);
                builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {});
                builder.setPositiveButton(R.string.FilterNoColon, (dialog, which) -> {
                    for (int i = 0; i < selectedRollsTemp.length; i++) {
                        allRolls.get(i).second = selectedRollsTemp[i];
                    }
                    updateSelectedRolls();
                    updateMarkers();
                });
                builder.setNeutralButton(R.string.DeselectAll, null);
                final AlertDialog dialog = builder.create();
                dialog.show();
                // Override the neutral button onClick listener after the dialog is shown.
                // This way the dialog isn't dismissed when the button is pressed.
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    final ListView listView = dialog.getListView();
                    for (int i = 0; i < listView.getCount(); i++) {
                        listView.setItemChecked(i, false);
                    }
                    Arrays.fill(selectedRollsTemp, false);
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Insert dummy boolean so that outState is not null.
        outState.putBoolean(ExtraKeys.CONTINUE, true);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        googleMap_ = googleMap;

        // If only one roll can be displayed, the bottom sheet will be hidden.
        // Therefore there is no need to add the bottom padding to the map.
        if (allRolls.size() > 1) {
            final int peekHeightOffset = getResources().getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight);
            googleMap_.setPadding(0, 0, 0, peekHeightOffset);
        }

        // If the app's theme is dark, stylize the map with the custom night mode
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (Utilities.isAppThemeDark(getBaseContext())) {
            googleMap_.setMapStyle(new MapStyleOptions(getResources()
                    .getString(R.string.style_json)));
        }

        googleMap_.setMapType(prefs.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL));
        updateMarkers();

        if (allRolls.size() == 1) {
            googleMap_.setInfoWindowAdapter(new InfoWindowAdapterSingleRoll());
        } else {
            googleMap_.setInfoWindowAdapter(new InfoWindowAdapterMultipleRolls());
        }
        googleMap_.setOnInfoWindowClickListener(new OnInfoWindowClickListener());

    }

    private void updateSelectedRolls() {
        selectedRolls.clear();
        int i = 0;
        for (Pair<Roll, Boolean> pair : allRolls) {
            // If the the roll is not selected, continue.
            if (!pair.second) {
                continue;
            }
            selectedRolls.add(new Pair<>(pair.first, markerBitmaps.get(i)));
            i++;
            if (i >= markerBitmaps.size()) {
                i = 0;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateMarkers() {
        for (Marker marker : markerList) {
            marker.remove();
        }
        markerList.clear();

        for (int rollIterator = 0; rollIterator < selectedRolls.size(); ++rollIterator) {

            final Pair<Roll, Bitmap> pair = selectedRolls.get(rollIterator);

            final Roll roll = pair.first;
            final List<Frame> frameList = database.getAllFramesFromRoll(roll);
            final Bitmap markerBitmap = pair.second;
            final BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(markerBitmap);

            for (final Frame frame : frameList) {

                // Parse the latLngLocation string
                final String location = frame.getLocation();
                if (location != null && location.length() > 0 && !location.equals("null")) {
                    final String latString = location.substring(0, location.indexOf(" "));
                    final String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                    final double lat = Double.parseDouble(latString.replace(",", "."));
                    final double lng = Double.parseDouble(lngString.replace(",", "."));
                    final LatLng position = new LatLng(lat, lng);
                    final String title = "" + roll.getName();
                    final String snippet = "#" + frame.getCount();
                    final Marker marker = googleMap_.addMarker(new MarkerOptions()
                            .icon(bitmapDescriptor)
                            .position(position)
                            .title(title)
                            .snippet(snippet)
                            .anchor(0.5f, 1.0f)); // Since we use a custom marker icon, set offset.

                    marker.setTag(frame);
                    markerList.add(marker);
                }
            }
        }

        if (markerList.size() > 0 && !continueActivity) {

            final LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (final Marker marker : markerList) {
                builder.include(marker.getPosition());
            }
            final LatLngBounds bounds = builder.build();

            final int width = getResources().getDisplayMetrics().widthPixels;
            final int height = getResources().getDisplayMetrics().heightPixels;
            final int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen

            // We use this command where the map's dimensions are specified.
            // This is because on some devices, the map's layout may not have yet occurred
            // (map size is 0).
            final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            googleMap_.moveCamera(cameraUpdate);

        } else {
            Toast.makeText(this, getResources().getString(R.string.NoFramesToShow), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets the GoogleMap map type
     *
     * @param mapType One of the map type constants from class GoogleMap
     */
    private void setMapType(final int mapType) {
        this.mapType = mapType;
        if (googleMap_ != null) {
            googleMap_.setMapType(mapType);
        }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType);
        editor.apply();
    }

    private class InfoWindowAdapterMultipleRolls implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
        @Override
        public View getInfoContents(Marker marker) {
            if (marker.getTag() instanceof Frame) {
                final Frame frame = (Frame) marker.getTag();
                final Roll roll = database.getRoll(frame.getRollId());
                final Camera camera = roll != null && roll.getCameraId() > 0 ?
                        database.getCamera(roll.getCameraId()) :
                        null;
                final Lens lens = frame.getLensId() > 0 ?
                        database.getLens(frame.getLensId()) :
                        null;
                @SuppressLint("InflateParams")
                final View view = getLayoutInflater().inflate(R.layout.info_window_all_frames, null);
                final TextView rollTextView = view.findViewById(R.id.roll_name);
                final TextView cameraTextView = view.findViewById(R.id.camera);
                final TextView frameCountTextView = view.findViewById(R.id.frame_count);
                final TextView dateTimeTextView = view.findViewById(R.id.date_time);
                final TextView lensTextView = view.findViewById(R.id.lens);
                final TextView noteTextView = view.findViewById(R.id.note);
                rollTextView.setText(roll != null ? roll.getName() : "");
                cameraTextView.setText(
                        camera == null ? getString(R.string.NoCamera) : camera.getName()
                );
                final String frameCountText = "#" + frame.getCount();
                frameCountTextView.setText(frameCountText);
                dateTimeTextView.setText(frame.getDate()!= null ? frame.getDate().getDateTimeAsText() : "");
                lensTextView.setText(
                        lens == null ? getString(R.string.NoLens) : lens.getName()
                );
                noteTextView.setText(frame.getNote());
                return view;
            } else {
                return null;
            }
        }
    }

    private class InfoWindowAdapterSingleRoll implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
        @Override
        public View getInfoContents(Marker marker) {
            if (marker.getTag() instanceof Frame) {
                final Frame frame = (Frame) marker.getTag();
                final Lens lens = frame.getLensId() > 0 ?
                        database.getLens(frame.getLensId()) :
                        null;
                @SuppressLint("InflateParams")
                final View view = getLayoutInflater().inflate(R.layout.info_window, null);
                final TextView frameCountTextView = view.findViewById(R.id.frame_count);
                final TextView dateTimeTextView = view.findViewById(R.id.date_time);
                final TextView lensTextView = view.findViewById(R.id.lens);
                final TextView noteTextView = view.findViewById(R.id.note);
                final String frameCountText = "#" + frame.getCount();
                frameCountTextView.setText(frameCountText);
                dateTimeTextView.setText(frame.getDate() != null ? frame.getDate().getDateTimeAsText() : "");

                lensTextView.setText(
                        lens == null ? getString(R.string.NoLens) : lens.getName()
                );
                noteTextView.setText(frame.getNote());
                return view;
            } else {
                return null;
            }
        }
    }

    private class OnInfoWindowClickListener implements GoogleMap.OnInfoWindowClickListener {
        @Override
        public void onInfoWindowClick(Marker marker) {
            if (marker.getTag() instanceof Frame) {
                final Frame frame = (Frame) marker.getTag();
                if (frame != null) {
                    final Bundle arguments = new Bundle();
                    final String title = "" + getResources().getString(R.string.EditFrame) + frame.getCount();
                    final String positiveButton = getResources().getString(R.string.OK);
                    arguments.putString(ExtraKeys.TITLE, title);
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton);
                    arguments.putParcelable(ExtraKeys.FRAME, frame);

                    final EditFrameDialogCallback dialog = new EditFrameDialogCallback();
                    dialog.setArguments(arguments);
                    dialog.setOnPositiveButtonClickedListener(data -> {
                                final Frame editedFrame = data.getParcelableExtra(ExtraKeys.FRAME);
                                if (editedFrame != null) {
                                    database.updateFrame(editedFrame);
                                    marker.setTag(editedFrame);
                                    marker.hideInfoWindow();
                                    marker.showInfoWindow();
                                    setResult(AppCompatActivity.RESULT_OK);
                                }
                            });
                    dialog.show(getSupportFragmentManager().beginTransaction(), EditFrameDialog.TAG);
                }
            }
        }
    }

    private @Nullable Bitmap getMarkerBitmap(final Context context) {
        final Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_red);
        if (drawable == null) return null;
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private @Nullable Bitmap getMarkerBitmap(final Context context, final float hue) {
        final Bitmap bitmap = getMarkerBitmap(context);
        if (bitmap != null) {
            return setBitmapHue(bitmap, hue);
        } else {
            return null;
        }
    }

    private Bitmap setBitmapHue(final Bitmap bitmap, final float hue){
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final float[] hvs = new float[3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = bitmap.getPixel(x, y);
                Color.colorToHSV(pixel, hvs);
                hvs[0] = hue;
                bitmap.setPixel(x, y, Color.HSVToColor(Color.alpha(pixel), hvs));
            }
        }
        return bitmap;
    }

    private static class RollMarkerAdapter extends ArrayAdapter<Pair<Roll, Bitmap>> {

        private List<Pair<Roll, Bitmap>> rollList;

        RollMarkerAdapter(final Context context, final List<Pair<Roll, Bitmap>> rollList) {
            super(context, android.R.layout.simple_list_item_1, rollList);
            this.rollList = rollList;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final Pair<Roll, Bitmap> pair = rollList.get(position);
            final Roll roll = pair.first;
            final Bitmap markerBitmap = pair.second;
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_roll_map_activity, parent, false);
                holder = new ViewHolder();
                holder.rollNameTextView = convertView.findViewById(R.id.roll_text_view);
                holder.markerImageView = convertView.findViewById(R.id.marker_image_view);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.rollNameTextView.setText(roll.getName());
            holder.markerImageView.setImageBitmap(markerBitmap);
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            // Disable onClick events for the adapter. We only display the visible rolls.
            return false;
        }

        private static class ViewHolder {
            TextView rollNameTextView;
            ImageView markerImageView;
        }

    }

    private List<Bitmap> getMarkerBitmapList() {
        final List<Bitmap> bitmaps = new ArrayList<>();
        bitmaps.add(0, getMarkerBitmap(this));
        bitmaps.add(1, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_AZURE));
        bitmaps.add(2, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_GREEN));
        bitmaps.add(3, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_ORANGE));
        bitmaps.add(4, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_YELLOW));
        bitmaps.add(5, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_BLUE));
        bitmaps.add(6, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_ROSE));
        bitmaps.add(7, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_CYAN));
        bitmaps.add(8, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_VIOLET));
        bitmaps.add(9, getMarkerBitmap(this, BitmapDescriptorFactory.HUE_MAGENTA));
        return bitmaps;
    }

    /**
     * Helper class utilizing Java Generics to represent a pair of objects.
     *
     * @param <T> object of type T placed in the first member of Pair
     * @param <U> object of type U placed in the second member of Pair
     */
    private static class Pair<T, U> {
        T first;
        U second;
        Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
    }

}
