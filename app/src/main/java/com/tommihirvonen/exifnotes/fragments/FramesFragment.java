package com.tommihirvonen.exifnotes.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.gms.common.SupportErrorDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.tommihirvonen.exifnotes.activities.MapActivity;
import com.tommihirvonen.exifnotes.activities.FramesActivity;
import com.tommihirvonen.exifnotes.activities.LocationPickActivity;
import com.tommihirvonen.exifnotes.adapters.FrameAdapter;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.DateTime;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.GearActivity;
import com.tommihirvonen.exifnotes.activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.FrameSortMode;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * FramesFragment is the fragment which is called when the user presses on a roll
 * on the ListView in RollsFragment. It displays all the frames from that roll.
 */
public class FramesFragment extends Fragment implements
        View.OnClickListener,
        FrameAdapter.FrameAdapterListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * Public constant to tag this fragment when it is created.
     */
    public static final String FRAMES_FRAGMENT_TAG = "FRAMES_FRAGMENT";

    /**
     * Constant passed to EditFrameDialog for result
     */
    private static final int FRAME_DIALOG = 1;

    /**
     * Constant passed to EditFrameDialog for result
     */
    private static final int EDIT_FRAME_DIALOG = 2;

    /**
     * Constant passed to ErrorDialogFragment
     */
    private static final int ERROR_DIALOG = 3;

    private static final int SHOW_ON_MAP = 4;

    /**
     * Constant passed to LocationPickActivity
     */
    private static final int REQUEST_LOCATION_PICK = 5;

    private static final int REQUEST_EXPORT_FILES = 6;

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * TextView to show that no frames have been added to this roll
     */
    private TextView mainTextView;

    /**
     * ListView to show all the frames on this roll with details
     */
    private RecyclerView mainRecyclerView;

    /**
     * Contains all the frames for this roll
     */
    private List<Frame> frameList = new ArrayList<>();

    /**
     * Adapter to adapt frameList to mainRecyclerView
     */
    private FrameAdapter frameAdapter;

    /**
     * Currently selected roll
     */
    private Roll roll;

    /**
     * Currently used camera
     */
    private Camera camera;

    /**
     * Reference to the FloatingActionButton
     */
    private FloatingActionButton floatingActionButton;

    /**
     * Holds the frame sort mode.
     */
    private FrameSortMode sortMode;

    // Google client to interact with Google API
    /**
     * Boolean to specify whether the user has granted the app location permissions
     */
    private boolean locationPermissionsGranted;

    /**
     * Reference to the GoogleApiClient providing location services
     */
    private GoogleApiClient googleApiClient;

    /**
     * Member to hold the last received location
     */
    private Location lastLocation;

    /**
     * Member to specify what kind of location requests the app needs (time interval, accuracy)
     */
    private LocationRequest locationRequest;

    /**
     * Member to specify the callback implementation for the location services.
     */
    private LocationCallback locationCallback;

    /**
     * True if the user has enabled location updates in the app's settings, false if not
     */
    private boolean requestingLocationUpdates;

    /**
     * Private callback class which is given as an argument when the SupportActionMode is started.
     */
    private final ActionModeCallback actionModeCallback = new ActionModeCallback();

    /**
     * Reference to the (Support)ActionMode, which is launched when a list item is long pressed.
     */
    private ActionMode actionMode;

    /**
     * Called when the fragment is created.
     * Get the frames from the roll and enable location updating. Tell the fragment
     * that it has an options menu so that we can handle OptionsItemSelected events.
     *
     * @param savedInstanceState saved state of the Fragment
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        database = FilmDbHelper.getInstance(getActivity());

        final long rollId = getArguments().getLong(ExtraKeys.ROLL_ID);
        roll = database.getRoll(rollId);
        if (roll != null) camera = database.getCamera(roll.getCameraId());
        locationPermissionsGranted = getArguments().getBoolean(ExtraKeys.LOCATION_ENABLED);
        frameList = database.getAllFramesFromRoll(roll);

        //getActivity().getPreferences() returns a preferences file related to the
        //activity it is opened from. getDefaultSharedPreferences() returns the
        //applications global preferences. This is something to keep in mind.
        //If the same sort order setting is to be used elsewhere in the app, then
        //getDefaultSharedPreferences() should be used.
        final SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        sortMode = FrameSortMode.Companion.fromValue(
                sharedPreferences.getInt(PreferenceConstants.KEY_FRAME_SORT_ORDER, FrameSortMode.FRAME_COUNT.getValue()));

        //Sort the list according to preferences
        Utilities.sortFrameList(getActivity(), sortMode, database, frameList);

        // Activate GPS locating if the user has granted permission.
        if (locationPermissionsGranted) {

            // Create an instance of GoogleAPIClient for latlng_location services.
            if (googleApiClient == null) {
                googleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
            // Create locationRequest to update the current latlng_location.
            locationRequest = new LocationRequest();
            // 10 seconds
            locationRequest.setInterval(10*1000);
            // 1 second
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        // This can be done anyway. It only has effect if locationPermissionsGranted is true.
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                lastLocation = locationResult.getLastLocation();
            }
        };
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true);
    }

    /**
     * Inflate the fragment. Set the UI objects and display all the frames in RecyclerView.
     *
     * @param inflater not used
     * @param container not used
     * @param savedInstanceState not used
     * @return The inflated view
     */
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        final View view = layoutInflater.inflate(R.layout.fragment_frames, container, false);

        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            if (roll != null) actionBar.setTitle(roll.getName());
            if (camera != null) actionBar.setSubtitle(camera.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        floatingActionButton = view.findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);

        final int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = view.findViewById(R.id.no_added_frames);

        // Access the ListView
        mainRecyclerView = view.findViewById(R.id.frames_recycler_view);

        // Create an adapter for the RecyclerView
        frameAdapter = new FrameAdapter(getActivity(), frameList, this);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mainRecyclerView.setLayoutManager(layoutManager);
        // Add dividers for list items.
        mainRecyclerView.addItemDecoration(new DividerItemDecoration(mainRecyclerView.getContext(),
                layoutManager.getOrientation()));

        // Set the RecyclerView to use frameAdapter
        mainRecyclerView.setAdapter(frameAdapter);

        if (frameList.size() >= 1) {
            mainTextView.setVisibility(View.GONE);
        }

        if (frameAdapter.getItemCount() >= 1) mainRecyclerView.scrollToPosition(frameAdapter.getItemCount()-1);

        return view;
    }

    /**
     * Inflate the action bar many layout for FramesFragment.
     *
     * @param menu the menu to be inflated
     * @param inflater the MenuInflater from Activity
     */
    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, final MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_frames_fragment, menu);
    }

    /**
     * Called after onCreateOptionsMenu() to prepare the menu.
     * Check the correct sort option.
     *
     * @param menu reference to the menu that is to be prepared
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull final Menu menu) {
        switch (sortMode) {
            case FRAME_COUNT: default:
                menu.findItem(R.id.frame_count_sort_mode).setChecked(true);
                break;
            case DATE:
                menu.findItem(R.id.date_sort_mode).setChecked(true);
                break;
            case F_STOP:
                menu.findItem(R.id.f_stop_sort_mode).setChecked(true);
                break;
            case SHUTTER_SPEED:
                menu.findItem(R.id.shutter_speed_sort_mode).setChecked(true);
                break;
            case LENS:
                menu.findItem(R.id.lens_sort_mode).setChecked(true);
                break;
        }
        super.onPrepareOptionsMenu(menu);
    }

    /**
     * Handle events when the user selects an action from the options menu.
     *
     * @param item selected menu item.
     * @return true because the item selection was consumed/handled.
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item){
        switch (item.getItemId()) {

            case R.id.frame_count_sort_mode:
                item.setChecked(true);
                setSortMode(FrameSortMode.FRAME_COUNT);
                break;

            case R.id.date_sort_mode:
                item.setChecked(true);
                setSortMode(FrameSortMode.DATE);
                break;

            case R.id.f_stop_sort_mode:
                item.setChecked(true);
                setSortMode(FrameSortMode.F_STOP);
                break;

            case R.id.shutter_speed_sort_mode:
                item.setChecked(true);
                setSortMode(FrameSortMode.SHUTTER_SPEED);
                break;

            case R.id.lens_sort_mode:
                item.setChecked(true);
                setSortMode(FrameSortMode.LENS);
                break;

            case R.id.menu_item_gear:
                final Intent gearActivityIntent = new Intent(getActivity(), GearActivity.class);
                startActivity(gearActivityIntent);

                break;
            case R.id.menu_item_preferences:

                final Intent preferenceActivityIntent = new Intent(getActivity(), PreferenceActivity.class);
                //Start the preference activity from FramesActivity.
                //The result will be handled in FramesActivity.
                getActivity().startActivityForResult(preferenceActivityIntent, FramesActivity.PREFERENCE_ACTIVITY_REQUEST);

                break;

            case R.id.menu_item_help:

                final String helpTitle = getResources().getString(R.string.Help);
                final String helpMessage = getResources().getString(R.string.main_help);
                Utilities.showGeneralDialog(getActivity(), helpTitle, helpMessage);

                break;

            case R.id.menu_item_about:

                final String aboutTitle = getResources().getString(R.string.app_name);
                final String aboutMessage = getResources().getString(R.string.about) + "\n\n\n" +
                        getResources().getString(R.string.VersionHistory);
                Utilities.showGeneralDialog(getActivity(), aboutTitle, aboutMessage);

                break;

            case android.R.id.home:

                getActivity().finish();
                break;

            case R.id.menu_item_show_on_map:

                final Intent mapIntent = new Intent(getActivity(), MapActivity.class);
                final ArrayList<Roll> list = new ArrayList<>();
                list.add(roll);
                mapIntent.putParcelableArrayListExtra(ExtraKeys.ARRAY_LIST_ROLLS, list);
                mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_TITLE, roll.getName());
                if (camera != null) {
                    mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE, camera.getName());
                }
                startActivityForResult(mapIntent, SHOW_ON_MAP);
                break;

            // Share
            case R.id.menu_item_share:

                // Method getShareRollIntent() may take a while to run since it
                // generates the files that will be shared.
                // -> Run the code on a new thread, which lets the UI thread to finish menu animations.
                new Thread(() -> {
                    Intent shareIntent = getShareRollIntent();
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.Share)));
                }).start();

                break;

            //Export to device
            case R.id.menu_item_export:

                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_EXPORT_FILES);

                break;

        }

        return true;
    }

    /**
     * Change the sort order of frames.
     *
     * @param sortMode enum type referencing the sort mode
     */
    private void setSortMode(final FrameSortMode sortMode) {
        this.sortMode = sortMode;
        final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PreferenceConstants.KEY_FRAME_SORT_ORDER, sortMode.getValue());
        editor.apply();
        Utilities.sortFrameList(getActivity(), sortMode, database, frameList);
        frameAdapter.notifyDataSetChanged();
    }


    /**
     * Creates an Intent to share exiftool commands and a csv
     * for the frames of the roll in question.
     *
     * @return The intent to be shared.
     */
    private Intent getShareRollIntent() {

        //Replace illegal characters from the roll name to make it a valid file name.
        final String rollName = Utilities.replaceIllegalChars(roll.getName() != null ? roll.getName() : "");

        //Get the user setting about which files to export. By default, share only ExifTool.
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        final String filesToExport = prefs.getString(PreferenceConstants.KEY_FILES_TO_EXPORT,
                PreferenceConstants.VALUE_BOTH);

        //Create the Intent to be shared, no initialization yet
        final Intent shareIntent;

        //Create the files

        //Get the external storage path (not the same as SD card)
        final File externalStorageDir = getActivity().getExternalFilesDir(null);

        //Create the file names for the two files to be put in that intent
        final String fileNameCsv = rollName + "_csv" + ".txt";
        final String fileNameExifToolCmds = rollName + "_ExifToolCmds" + ".txt";

        //Create the strings to be written on those two files
        final String csvString = Utilities.createCsvString(getActivity(), roll);
        final String exifToolCmds = Utilities.createExifToolCmdsString(getActivity(), roll);

        //Create the files in external storage
        final File fileCsv = new File(externalStorageDir, fileNameCsv);
        final File fileExifToolCmds = new File(externalStorageDir, fileNameExifToolCmds);

        //Write the csv file
        Utilities.writeTextFile(fileCsv, csvString);

        //Write the ExifTool commands file
        Utilities.writeTextFile(fileExifToolCmds, exifToolCmds);

        //If the user has chosen to export both files
        if (filesToExport.equals(PreferenceConstants.VALUE_BOTH)) {
            //Create the intent to be shared
            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("text/plain");

            //Create an array with the file names
            final List<String> filesToSend = new ArrayList<>();
            filesToSend.add(externalStorageDir + "/" + fileNameCsv);
            filesToSend.add(externalStorageDir + "/" + fileNameExifToolCmds);

            //Create an ArrayList of files.
            //NOTE: putParcelableArrayListExtra requires an ArrayList as its argument
            final ArrayList<Uri> files = new ArrayList<>();
            for(final String path : filesToSend ) {
                final File file = new File(path);
                final Uri uri;
                //Android Nougat requires that the file is given via FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext()
                            .getPackageName() + ".provider", file);
                } else {
                    uri = Uri.fromFile(file);
                }
                files.add(uri);
            }

            //Add the two files to the Intent as extras
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);

        } else {
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            //The user has chosen to export only the csv
            if (filesToExport.equals(PreferenceConstants.VALUE_CSV)) {
                final Uri uri;
                //Android Nougat requires that the file is given via FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext()
                            .getPackageName() + ".provider", fileCsv);
                } else {
                    uri = Uri.fromFile(fileCsv);
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            }
            //The user has chosen to export only the ExifTool commands
            else if (filesToExport.equals(PreferenceConstants.VALUE_EXIFTOOL)) {
                final Uri uri;
                //Android Nougat requires that the file is given via FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext()
                            .getPackageName() + ".provider", fileExifToolCmds);
                } else {
                    uri = Uri.fromFile(fileExifToolCmds);
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            }
        }

        return shareIntent;
    }


    /**
     * Called when FloatingActionButton is pressed.
     * Show the user the FrameInfoDialog to add a new frame.
     *
     * @param v The view which was clicked.
     */
    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.fab) {
            showFrameInfoDialog();
        }
    }

    /**
     * Create new dialog to edit the selected Frame's information
     *
     * @param position position of the Frame in frameList
     */
    @SuppressLint("CommitTransaction")
    private void showFrameInfoEditDialog(final int position) {
        // Edit frame info
        final Frame frame = frameList.get(position);
        final Bundle arguments = new Bundle();
        final String title = "" + getActivity().getString(R.string.EditFrame) + frame.getCount();
        final String positiveButton = getActivity().getResources().getString(R.string.OK);
        arguments.putString(ExtraKeys.TITLE, title);
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton);
        arguments.putParcelable(ExtraKeys.FRAME, frame);

        final EditFrameDialog dialog = new EditFrameDialog();
        dialog.setTargetFragment(this, EDIT_FRAME_DIALOG);
        dialog.setArguments(arguments);
        dialog.show(getParentFragmentManager().beginTransaction(), EditFrameDialog.TAG);
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Show a dialog fragment to add a new frame.
     */
    private void showFrameInfoDialog() {

        // If the frame count is greater than 100, then don't add a new frame.
        if (!frameList.isEmpty()) {
            final int countCheck = frameList.get(frameList.size() - 1).getCount() + 1;
            if (countCheck > 100) {
                final Toast toast = Toast.makeText(getActivity(),
                        getResources().getString(R.string.TooManyFrames), Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        }

        final String title = getActivity().getResources().getString(R.string.NewFrame);
        final String positiveButton = getActivity().getResources().getString(R.string.Add);

        final Frame frame = new Frame();
        frame.setDate(DateTime.Companion.fromCurrentTime());
        frame.setCount(0);
        frame.setRollId(roll.getId());

        //Get the location only if the app has location permission (locationPermissionsGranted) and
        //the user has enabled GPS updates in the app's settings.
        if (locationPermissionsGranted &&
                PreferenceManager.getDefaultSharedPreferences(
                        getActivity().
                        getBaseContext()).getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true))
            frame.setLocation(Utilities.locationStringFromLocation(lastLocation));

        if (!frameList.isEmpty()) {

            //Get the information for the last added frame.
            //The last added frame has the highest id number (database autoincrement).
            Frame previousFrame = frameList.get(frameList.size() - 1);
            long i = 0;
            for (final Frame frameIterator : frameList) {
                if (frameIterator.getId() > i) {
                    i = frameIterator.getId();
                    previousFrame = frameIterator;
                }
                //Set the frame count to one higher than the highest frame count
                if (frameIterator.getCount() >= frame.getCount())
                    frame.setCount(frameIterator.getCount() + 1);
            }

            // Here we can list the properties we want to bring from the previous frame
            frame.setLensId(previousFrame.getLensId());
            frame.setShutter(previousFrame.getShutter());
            frame.setAperture(previousFrame.getAperture());
            frame.setFilters(previousFrame.getFilters());
            frame.setFocalLength(previousFrame.getFocalLength());
            frame.setLightSource(previousFrame.getLightSource());

        } else {
            frame.setCount(1);
            frame.setShutter(null);
            frame.setAperture(null);
        }
        frame.setNoOfExposures(1);

        final EditFrameDialog dialog = new EditFrameDialog();
        dialog.setTargetFragment(this, FRAME_DIALOG);
        final Bundle arguments = new Bundle();
        arguments.putString(ExtraKeys.TITLE, title);
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton);
        arguments.putParcelable(ExtraKeys.FRAME, frame);
        dialog.setArguments(arguments);
        dialog.show(getParentFragmentManager(), EditFrameDialog.TAG);
    }

    /**
     * Called when the user is done editing or adding a frame and closes the dialog.
     *
     * @param requestCode the request code that was set for the intent.
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed Intent
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {

            case FRAME_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    final Frame frame = data.getParcelableExtra(ExtraKeys.FRAME);

                    if (frame != null) {

                        if (database.addFrame(frame)) {
                            frameList.add(frame);
                            Utilities.sortFrameList(getActivity(), sortMode, database, frameList);
                            frameAdapter.notifyItemInserted(frameList.indexOf(frame));
                            mainTextView.setVisibility(View.GONE);

                            // When the new frame is added jump to view the added entry
                            final int pos = frameList.indexOf(frame);
                            if (pos < frameAdapter.getItemCount()) mainRecyclerView.scrollToPosition(pos);
                        }

                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;

            case EDIT_FRAME_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    if (actionMode != null) actionMode.finish();

                    final Frame frame = data.getParcelableExtra(ExtraKeys.FRAME);

                    if (frame != null && frame.getId() > 0) {

                        database.updateFrame(frame);
                        final int oldPosition = frameList.indexOf(frame);
                        Utilities.sortFrameList(getActivity(), sortMode, database, frameList);
                        final int newPosition = frameList.indexOf(frame);
                        frameAdapter.notifyItemChanged(oldPosition);
                        frameAdapter.notifyItemMoved(oldPosition, newPosition);

                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;

            case REQUEST_RESOLVE_ERROR:

                resolvingError = false;
                if (resultCode == Activity.RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if (googleApiClient != null && !googleApiClient.isConnecting() &&
                            !googleApiClient.isConnected()) {
                        googleApiClient.connect();
                    }
                }

                break;

            case SHOW_ON_MAP:

                if (resultCode == Activity.RESULT_OK) {

                    // Update the frame list in case updates were made in MapsActivity.
                    frameList = database.getAllFramesFromRoll(roll);
                    Utilities.sortFrameList(getActivity(), sortMode, database, frameList);
                    frameAdapter = new FrameAdapter(getActivity(), frameList, this);
                    mainRecyclerView.setAdapter(frameAdapter);
                    frameAdapter.notifyDataSetChanged();
                }

                break;

            case REQUEST_LOCATION_PICK:

                // Consume the case when the user has edited
                // the location of several frames in action mode.
                if (resultCode == Activity.RESULT_OK) {

                    final String location;
                    final String formattedAddress;
                    if (data.hasExtra(ExtraKeys.LATITUDE) && data.hasExtra(ExtraKeys.LONGITUDE)) {
                        location = "" + data.getStringExtra(ExtraKeys.LATITUDE) + " " +
                                data.getStringExtra(ExtraKeys.LONGITUDE);
                    } else location = null;
                    if (data.hasExtra(ExtraKeys.FORMATTED_ADDRESS)) {
                        formattedAddress = data.getStringExtra(ExtraKeys.FORMATTED_ADDRESS);
                    } else formattedAddress = null;
                    final List<Integer> framePositions = frameAdapter.getSelectedItemPositions();
                    for (int i = framePositions.size() - 1; i >= 0; i--) {
                        final Frame frame = frameList.get(framePositions.get(i));
                        frame.setLocation(location);
                        frame.setFormattedAddress(formattedAddress);
                        database.updateFrame(frame);
                    }
                    // Exit action mode after edit,
                    // so that getSelectedItemPositions() isn't an empty list.
                    if (actionMode != null) actionMode.finish();
                }

                break;

            case REQUEST_EXPORT_FILES:

                if (resultCode == Activity.RESULT_OK) {

                    try {
                        final String rollName = Utilities.replaceIllegalChars(roll.getName() != null ? roll.getName() : "");
                        final Uri directoryUri = data.getData();
                        final DocumentFile directoryDocumentFile = DocumentFile.fromTreeUri(getContext(), directoryUri);


                        //Get the user setting about which files to export. By default, share both files.
                        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                                getActivity().getBaseContext());
                        final String filesToExport = prefs.getString(PreferenceConstants.KEY_FILES_TO_EXPORT,
                                PreferenceConstants.VALUE_BOTH);

                        if (filesToExport.equals(PreferenceConstants.VALUE_BOTH) ||
                                filesToExport.equals(PreferenceConstants.VALUE_CSV)) {

                            final DocumentFile csvDocumentFile = directoryDocumentFile
                                    .createFile("text/plain", rollName + "_csv.txt");
                            final OutputStream csvOutputStream = getActivity()
                                    .getContentResolver().openOutputStream(csvDocumentFile.getUri());
                            final String csvString = Utilities.createCsvString(getActivity(), roll);
                            final OutputStreamWriter csvOutputStreamWriter = new OutputStreamWriter(csvOutputStream);
                            csvOutputStreamWriter.write(csvString);
                            csvOutputStreamWriter.flush();
                            csvOutputStreamWriter.close();
                            csvOutputStream.close();

                        }
                        if (filesToExport.equals(PreferenceConstants.VALUE_BOTH) ||
                                filesToExport.equals(PreferenceConstants.VALUE_EXIFTOOL)) {

                            final DocumentFile cmdDocumentFile = directoryDocumentFile
                                    .createFile("text/plain", rollName + "_ExifToolCmds.txt");
                            final OutputStream cmdOutputStream = getActivity()
                                    .getContentResolver().openOutputStream(cmdDocumentFile.getUri());
                            final String cmdString = Utilities.createExifToolCmdsString(getActivity(), roll);
                            final OutputStreamWriter cmdOutputStreamWriter = new OutputStreamWriter(cmdOutputStream);
                            cmdOutputStreamWriter.write(cmdString);
                            cmdOutputStreamWriter.flush();
                            cmdOutputStreamWriter.close();
                            cmdOutputStream.close();

                        }

                        Toast.makeText(getActivity(), R.string.ExportedFilesSuccessfully, Toast.LENGTH_SHORT).show();

                    } catch (final IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), R.string.ErrorExporting, Toast.LENGTH_SHORT).show();
                    }
                }

                break;

        }
    }

    /**
     * Called when a frame is pressed.
     * if action mode is enabled, add the pressed item to selected items.
     * Otherwise show the EditFrameDialog.
     *
     * @param position position of the item in the RecyclerView
     */
    @Override
    public void onItemClick(final int position) {
        if (frameAdapter.getSelectedItemCount() > 0 || actionMode != null) {
            enableActionMode(position);
        } else {
            showFrameInfoEditDialog(position);
        }
    }

    /**
     * When an item is long pressed, always add the pressed item to selected items.
     *
     * @param position position of the item in FrameAdapter
     */
    @Override
    public void onItemLongClick(final int position) {
        enableActionMode(position);
    }

    /**
     * Enable action mode if not yet enabled and add item to selected items.
     *
     * @param position position of the item in FrameAdapter
     */
    private void enableActionMode(final int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        frameAdapter.toggleSelection(position);
        // If the user deselected the last of the selected items, exit action mode.
        if (frameAdapter.getSelectedItemCount() == 0) actionMode.finish();
        // Set the action mode toolbar title to display the number of selected items and the number of all items.
        else actionMode.setTitle(frameAdapter.getSelectedItemCount() + "/"
                + frameAdapter.getItemCount());
    }

    /**
     * Class which implements ActionMode.Callback.
     * One instance of this class is given as an argument when action mode is started.
     */
    private class ActionModeCallback implements ActionMode.Callback {

        /**
         * Called when the ActionMode is started.
         * Inflate the menu and set the visibility of some menu items.
         *
         * @param mode {@inheritDoc}
         * @param menu {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
            // Set the status bar color to be dark grey to complement the grey action mode toolbar.
            Utilities.setStatusBarColor(getActivity(), ContextCompat.getColor(getActivity(), R.color.dark_grey));
            // Hide the floating action button so no new rolls can be added while in action mode.
            floatingActionButton.hide();
            mode.getMenuInflater().inflate(R.menu.menu_action_mode_frames, menu);
            return true;
        }

        /**
         * Called to refresh the ActionMode menu whenever it is invalidated.
         *
         * @param mode {@inheritDoc}
         * @param menu {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
            return false;
        }

        /**
         * Called when the user presses on an action menu item.
         *
         * @param mode {@inheritDoc}
         * @param item {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
            // Get the positions in the frameList of selected items.
            final List<Integer> selectedItemPositions = frameAdapter.getSelectedItemPositions();
            switch (item.getItemId()) {

                case R.id.menu_item_delete:

                    final AlertDialog.Builder deleteConfirmDialog = new AlertDialog.Builder(getActivity());
                    // Separate confirm titles for one or multiple frames
                    final String title = getResources().getQuantityString(R.plurals.ConfirmFramesDelete, selectedItemPositions.size(), selectedItemPositions.size());
                    deleteConfirmDialog.setTitle(title);
                    deleteConfirmDialog.setNegativeButton(R.string.Cancel, (dialogInterface, i) -> {
                        // Do nothing
                    });
                    deleteConfirmDialog.setPositiveButton(R.string.OK, (dialogInterface, i) -> {
                        for (int j = selectedItemPositions.size() - 1; j >= 0; j--) {
                            final int framePosition = selectedItemPositions.get(j);
                            final Frame frame = frameList.get(framePosition);
                            database.deleteFrame(frame);
                            frameList.remove(frame);
                            frameAdapter.notifyItemRemoved(framePosition);
                        }
                        if (frameList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                        mode.finish();
                    });
                    deleteConfirmDialog.create().show();
                    return true;

                case R.id.menu_item_edit:

                    // If only one frame is selected, show frame edit dialog.
                    if (frameAdapter.getSelectedItemCount() == 1) {
                        // Get the first of the selected rolls (only one should be selected anyway)
                        showFrameInfoEditDialog(selectedItemPositions.get(0));
                    }
                    // If multiple frames are selected, show batch edit features.
                    else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(String.format(getResources().getString(R.string.BatchEditFramesTitle), frameAdapter.getSelectedItemCount()));
                        builder.setItems(R.array.FramesBatchEditOptions, (dialogInterface, i) -> {
                            switch (i) {
                                case 0:
                                    // Edit frame counts
                                    new FrameCountBatchEditDialogBuilder(getActivity()).create().show();
                                    break;
                                case 1:
                                    // Edit location
                                    final Intent intent = new Intent(getActivity(), LocationPickActivity.class);
                                    startActivityForResult(intent, REQUEST_LOCATION_PICK);
                                    break;
                                default:
                                    break;
                            }
                        });
                        builder.setNegativeButton(R.string.Cancel, (dialogInterface, i) -> {
                            // Do nothing
                            dialogInterface.dismiss();
                        });
                        builder.create().show();
                    }

                    return true;

                case R.id.menu_item_select_all:

                    frameAdapter.toggleSelectionAll();
                    mainRecyclerView.post(() -> frameAdapter.resetAnimateAll());
                    mode.setTitle(frameAdapter.getSelectedItemCount() + "/"
                            + frameAdapter.getItemCount());
                    return true;

                default:
                    return false;
            }
        }

        /**
         * Called when an action mode is about to be exited and destroyed.
         *
         * @param mode {@inheritDoc}
         */
        @Override
        public void onDestroyActionMode(final ActionMode mode) {
            frameAdapter.clearSelections();
            actionMode = null;
            mainRecyclerView.post(() -> frameAdapter.resetAnimationIndex());
            // Return the status bar to its original color before action mode.
            Utilities.setStatusBarColor(getActivity(), Utilities.getSecondaryUiColor(getActivity()));
            // Make the floating action bar visible again since action mode is exited.
            floatingActionButton.show();
        }

        /**
         * Private class which creates a dialog builder for a custom dialog.
         * Used to batch edit frame counts.
         */
        private class FrameCountBatchEditDialogBuilder extends AlertDialog.Builder {

            FrameCountBatchEditDialogBuilder(final Context context) {
                super(context);
                setTitle(R.string.EditFrameCountsBy);
                @SuppressLint("InflateParams")
                final View view = getActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_single_numberpicker, null);
                final NumberPicker numberPicker = view.findViewById(R.id.number_picker);
                numberPicker.setMaxValue(200);
                numberPicker.setMinValue(0);
                // Use the NumberPicker.setDisplayedValues() method to set custom
                // values ranging from -100 to +100.
                final List<String> displayedValues = new ArrayList<>();
                for (int k = -100; k <= 100; ++k) {
                    if (k > 0) displayedValues.add("+" + k);
                    else displayedValues.add(Integer.toString(k));
                }
                numberPicker.setDisplayedValues(displayedValues.toArray(new String[0]));
                numberPicker.setValue(100);
                // Block the NumberPicker from activating the cursor.
                numberPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                setView(view);
                setNegativeButton(R.string.Cancel, (dialogInterface, i) -> {
                    // Do nothing
                });
                setPositiveButton(R.string.OK, (dialogInterface, i) -> {
                    final int change = Integer.valueOf(
                            // Replace the plus sign because on pre L devices this seems to cause a crash
                            displayedValues.get(numberPicker.getValue()).replace("+", "")
                    );
                    final List<Integer> framePositions = frameAdapter.getSelectedItemPositions();
                    for (int j = framePositions.size() - 1; j >= 0; j--) {
                        final Frame frame = frameList.get(framePositions.get(j));
                        frame.setCount(frame.getCount() + change);
                        database.updateFrame(frame);
                    }
                    if (actionMode != null) actionMode.finish();
                    Utilities.sortFrameList(getActivity(), sortMode, database, frameList);
                    frameAdapter.notifyDataSetChanged();
                });
            }

        }
    }

    /**
     * When the fragment is started connect to Google Play services to get accurate location.
     */
    public void onStart() {
        //Added check to make sure googleApiClient is not null.
        //Apparently some users were encountering a bug where during onResume
        //googleApiClient was null.
        if (locationPermissionsGranted && googleApiClient != null) googleApiClient.connect();
        super.onStart();
    }

    /**
     * When the fragment is stopped disconnect from the Google Play services.
     */
    public void onStop() {
        //Added check to make sure googleApiClient is not null.
        //Apparently some users were encountering a bug where during onResume
        //googleApiClient was null.
        if (locationPermissionsGranted && googleApiClient != null) googleApiClient.disconnect();
        super.onStop();
    }

    /**
     * When the fragment is paused also pause location updates.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (locationPermissionsGranted) stopLocationUpdates();
    }

    /**
     * Called when the fragment is paused. Stop location updates.
     */
    private void stopLocationUpdates() {
        //Added check to make sure googleApiClient is not null.
        //Apparently some users were encountering a bug where during onResume
        //googleApiClient was null.
        if (googleApiClient != null && googleApiClient.isConnected())
            LocationServices.getFusedLocationProviderClient(getActivity()).removeLocationUpdates(locationCallback);
    }

    /**
     * When the fragment is resumed continue location updates and recolour the FloatingActionButton.
     */
    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        //Check if GPSUpdate preference has been changed meanwhile
        requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true);
        //Added check to make sure googleApiClient is not null.
        //Apparently some users were encountering a bug where during onResume
        //googleApiClient was null.
        if (locationPermissionsGranted && googleApiClient != null &&
                googleApiClient.isConnected() && requestingLocationUpdates) {
            startLocationUpdates();
        } else {
            stopLocationUpdates();
        }

        final int secondaryColor = Utilities.getSecondaryUiColor(getActivity());
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        // If action mode is enabled, color the status bar dark grey.
        if (frameAdapter.getSelectedItemCount() > 0 || actionMode != null) {
            Utilities.setStatusBarColor(getActivity(), ContextCompat.getColor(getActivity(), R.color.dark_grey));
        } else {
            // Otherwise we can update the frame adapter in case the user has changed the UI color.
            // This way the frame count text color will be updated according to the changed settings.
            frameAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Called when the fragment is resumed. Start location updates.
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //Added check to make sure googleApiClient is not null.
            //Apparently some users were encountering a bug where during onResume
            //googleApiClient was null.
            if (googleApiClient != null) {
                LocationServices.getFusedLocationProviderClient(getActivity())
                        .requestLocationUpdates(locationRequest, locationCallback, null);
            }
        }
    }

    /**
     * Called when the Google API is connected.
     *
     * @param bundle not used
     */
    @Override
    public void onConnected(final Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //Added check to make sure googleApiClient is not null.
            //Apparently some users were encountering a bug where during onResume
            //googleApiClient was null.
            if (googleApiClient != null) {
                LocationServices.getFusedLocationProviderClient(getActivity()).getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) lastLocation = location;
                        });
            }
            if (requestingLocationUpdates) {
                startLocationUpdates();
            }
        }
    }

    /**
     * Called when the connection to the Google API is suspended
     *
     * @param i not used
     */
    @Override
    public void onConnectionSuspended(final int i) {
        //Added check to make sure googleApiClient is not null.
        //Apparently some users were encountering a bug where during onResume
        //googleApiClient was null.
        if (locationPermissionsGranted && googleApiClient != null) googleApiClient.connect();
    }



    /**
     * Request code to use when launching the resolution activity
     */
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    /**
     * Unique tag for the error dialog fragment
     */
    private static final String DIALOG_ERROR = "dialog_error";

    /**
     * Boolean to track whether the app is already resolving an error
     */
    private boolean resolvingError = false;

    /**
     * Called if the connection to the Google API has failed. Try to resolve the error.
     *
     * @param result describes if the connection was successful
     */
    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult result) {
        if (result.hasResolution() && !resolvingError) {
            try {
                resolvingError = true;
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (final IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.

                //Added check to make sure googleApiClient is not null.
                //Apparently some users were encountering a bug where during onResume
                //googleApiClient was null.
                if (googleApiClient != null) {
                    googleApiClient.connect();
                }
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            resolvingError = true;
        }
    }

    /**
     * Creates a dialog for an error message
     */
    private void showErrorDialog(final int errorCode) {
        // Create a fragment for the error dialog
        final ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        final Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.setTargetFragment(this, ERROR_DIALOG);
        dialogFragment.show(getParentFragmentManager(), "errordialog");
    }

    /**
     * Called from ErrorDialogFragment when the dialog is dismissed.
     */
    private void onDialogDismissed() {
        resolvingError = false;
    }

    /**
     * A fragment to display an error dialog
     */
    static class ErrorDialogFragment extends SupportErrorDialogFragment {

        /**
         * Empty constructor
         */
        public ErrorDialogFragment() { }

        /**
         * {@inheritDoc}
         * @param outState
         */
        @Override
        public void onSaveInstanceState(@NonNull final Bundle outState){
            setTargetFragment(null, -1);
        }

        /**
         * {@inheritDoc}
         * @param savedInstanceState
         * @return
         */
        @NonNull
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                setTargetFragment(
                        getActivity().getSupportFragmentManager().findFragmentByTag(FRAMES_FRAGMENT_TAG),
                        ERROR_DIALOG);
            }
            // Get the error code and retrieve the appropriate dialog
            final int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        /**
         * Pass the onDismiss message to the FramesFragment
         *
         * @param dialog {@inheritDoc}
         */
        @Override
        public void onDismiss(@NonNull final DialogInterface dialog) {
            if (getActivity() == null) return;
            final FramesFragment framesfragment = (FramesFragment) getActivity()
                    .getSupportFragmentManager().findFragmentByTag(FRAMES_FRAGMENT_TAG);
            if (framesfragment != null) framesfragment.onDialogDismissed();
            super.onDismiss(dialog);
        }

    }

}
