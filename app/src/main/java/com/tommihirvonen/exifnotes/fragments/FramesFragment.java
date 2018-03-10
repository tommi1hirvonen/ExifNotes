package com.tommihirvonen.exifnotes.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.SimpleItemAnimator;
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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.tommihirvonen.exifnotes.activities.LocationPickActivity;
import com.tommihirvonen.exifnotes.activities.MainActivity;
import com.tommihirvonen.exifnotes.adapters.FrameAdapter;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.dialogs.DirectoryChooserDialog;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.GearActivity;
import com.tommihirvonen.exifnotes.activities.MapsActivity;
import com.tommihirvonen.exifnotes.activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.File;
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
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    /**
     * Public constant to tag a message in intent to MapsActivity
     */
    public final static String ROLL_ID_EXTRA_MESSAGE = "ROLL_ID";

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

    /**
     * Constant passed to MapsActivity
     */
    private static final int SHOW_ON_MAP = 4;

    /**
     * Constant passed to LocationPickActivity
     */
    private static final int LOCATION_PICK_REQUEST = 5;

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
     * Reference to the ShareActionProvider. Provides services to share
     * the text file containing ExifTool commands for this roll and the CSV file
     * containing details for all frames on this roll.
     */
    private ShareActionProvider shareActionProvider;

    /**
     * Database id of this roll
     */
    private long rollId;

    /**
     * Reference to the FloatingActionButton
     */
    private FloatingActionButton floatingActionButton;


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
     * True if the user has enabled location updates in the app's settings, false if not
     */
    private boolean requestingLocationUpdates;

    /**
     * Reference to the parent activity's OnHomeAsUpPressedListener
     */
    private OnHomeAsUpPressedListener callback;

    /**
     * This interface is implemented in MainActivity.
     */
    public interface OnHomeAsUpPressedListener {
        void onHomeAsUpPressed();
    }

    /**
     * Private callback class which is given as an argument when the SupportActionMode is started.
     */
    private ActionModeCallback actionModeCallback = new ActionModeCallback();

    /**
     * Reference to the (Support)ActionMode, which is launched when a list item is long pressed.
     */
    private ActionMode actionMode;

    /**
     * This on attach is called before API 23
     *
     * @param a Activity to which the OnHomeAsUpPressedListener is attached.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        callback = (OnHomeAsUpPressedListener) a;
    }

    /**
     * This on attach is called after API 23
     *
     * @param c Context to which the OnHomeAsUpPressedListener is attached.
     */
    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        callback = (OnHomeAsUpPressedListener) c;
    }

    /**
     * Called when the fragment is created.
     * Get the frames from the roll and enable location updating. Tell the fragment
     * that it has an options menu so that we can handle OptionsItemSelected events.
     *
     * @param savedInstanceState saved state of the Fragment
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        rollId = getArguments().getLong(ExtraKeys.ROLL_ID);
        locationPermissionsGranted = getArguments().getBoolean(ExtraKeys.LOCATION_ENABLED);

        database = FilmDbHelper.getInstance(getActivity());
        frameList = database.getAllFramesFromRoll(rollId);

        //Sort the list according to preferences
        Utilities.sortFrameList(getActivity(), database, frameList);

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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        final View view = layoutInflater.inflate(R.layout.frames_fragment, container, false);

        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            //noinspection ConstantConditions
            actionBar.setTitle(database.getRoll(rollId).getName());
            Camera camera = database.getCamera(database.getRoll(rollId).getCameraId());
            //noinspection ConstantConditions
            actionBar.setSubtitle(camera.getMake() + " " + camera.getModel());
            //noinspection ConstantConditions
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        floatingActionButton = view.findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);

        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = view.findViewById(R.id.no_added_frames);

        // Tell the host activity that this fragment has menu items it wants to add.
        // To compensate for a bug in pre Nougat (maybe Marshmallow) devices:
        // If the activity was recreated in MainActivity's onActivityResult because of a theme change,
        // invalidate the options menu. Using only setHasOptionsMenu(true) is not enough, because
        // it won't apparently guarantee that onCreateOptionsMenu will be called in this fragment
        // if the parent activity was recreated.
        getActivity().invalidateOptionsMenu();

        // Access the ListView
        mainRecyclerView = view.findViewById(R.id.frames_recycler_view);

        // Create an adapter for the RecyclerView
        frameAdapter = new FrameAdapter(getActivity(), frameList, this);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mainRecyclerView.setLayoutManager(layoutManager);
        // Add dividers for list items.
        mainRecyclerView.addItemDecoration(new DividerItemDecoration(mainRecyclerView.getContext(), layoutManager.getOrientation()));

        // Set the RecyclerView to use frameAdapter
        mainRecyclerView.setAdapter(frameAdapter);

        if (frameList.size() >= 1) {
            mainTextView.setVisibility(View.GONE);
        }

        if (frameAdapter.getItemCount() >= 1) mainRecyclerView.scrollToPosition(frameAdapter.getItemCount()-1);

        return view;
    }

    /**
     * Adds two share items to the options menu for exporting the roll data.
     *
     * @param menu the menu to which the items are added
     * @param inflater not used
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        MenuItem filterItem = menu.findItem(R.id.menu_item_filter);
        filterItem.setVisible(false);

        //Add the menu item for export
        MenuItem shareItem = menu.add(Menu.NONE, 98, Menu.NONE, R.string.ExportOrShare);

        //Add another menu item for exporting to device using inbuilt directory chooser
        menu.add(Menu.NONE, 99, Menu.NONE, R.string.ExportToDevice);

        if (shareItem != null) {
            //Link the Intent to be shared to the ShareActionProvider
            shareActionProvider = new ShareActionProvider(getActivity());
            shareActionProvider.setShareIntent(setShareIntentExportRoll());
        }

        //Link the ShareActionProvider to the menu item
        MenuItemCompat.setActionProvider(shareItem, shareActionProvider);

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Handle events when the user selects an action from the options menu.
     *
     * @param item selected menu item.
     * @return true because the item selection was consumed/handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            case R.id.menu_item_sort:
                //==================================================================================
                //getActivity().getPreferences() returns a preferences file related to the
                //activity it is opened from. getDefaultSharedPreferences() returns the
                //applications global preferences. This is something to keep in mind.
                //If the same sort order setting is to be used elsewhere in the app, then
                //getDefaultSharedPreferences() should be used.
                final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                int checkedItem = sharedPref.getInt(PreferenceConstants.KEY_FRAME_SORT_ORDER, 0);
                AlertDialog.Builder sortDialog = new AlertDialog.Builder(getActivity());
                sortDialog.setTitle(R.string.SortBy);
                sortDialog.setSingleChoiceItems(
                        R.array.FrameSortOptions, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt(PreferenceConstants.KEY_FRAME_SORT_ORDER, which);
                        editor.apply();
                        dialog.dismiss();
                        Utilities.sortFrameList(getActivity(), database, frameList);
                        frameAdapter.notifyDataSetChanged();
                    }
                });
                sortDialog.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Do nothing
                    }
                });
                sortDialog.show();

                break;

            case R.id.menu_item_lenses:
                Intent gearActivityIntent = new Intent(getActivity(), GearActivity.class);
                startActivity(gearActivityIntent);

                break;
            case R.id.menu_item_preferences:

                Intent preferenceActivityIntent = new Intent(getActivity(), PreferenceActivity.class);
                // With these extras we can skip the headers in the preferences.
                preferenceActivityIntent.putExtra(
                        PreferenceActivity.EXTRA_SHOW_FRAGMENT, PreferenceFragment.class.getName());
                preferenceActivityIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);

                //Start the preference activity from MainActivity.
                //The result will be handled in MainActivity.
                getActivity().startActivityForResult(preferenceActivityIntent, MainActivity.PREFERENCE_ACTIVITY_REQUEST);

                break;

            case R.id.menu_item_help:

                String helpTitle = getResources().getString(R.string.Help);
                String helpMessage = getResources().getString(R.string.main_help);
                Utilities.showGeneralDialog(getActivity(), helpTitle, helpMessage);

                break;

            case R.id.menu_item_about:

                String aboutTitle = getResources().getString(R.string.app_name);
                String aboutMessage = getResources().getString(R.string.about) + "\n\n\n" +
                        getResources().getString(R.string.VersionHistory);
                Utilities.showGeneralDialog(getActivity(), aboutTitle, aboutMessage);

                break;

            case android.R.id.home:

                // INTERFACE TO MAINACTIVITY
                callback.onHomeAsUpPressed();

                break;

            case R.id.menu_item_show_on_map:

                // Save the current scroll position
                //mainListViewScrollState = mainRecyclerView.onSaveInstanceState();
                Intent mapsActivityIntent = new Intent(getActivity(), MapsActivity.class);
                mapsActivityIntent.putExtra(ROLL_ID_EXTRA_MESSAGE, rollId);
                startActivityForResult(mapsActivityIntent, SHOW_ON_MAP);
                break;

            //Export to device
            case 99:

                DirectoryChooserDialog dirChooserDialog = DirectoryChooserDialog.newInstance(
                        new DirectoryChooserDialog.OnChosenDirectoryListener() {
                    @Override
                    public void onChosenDirectory(String directory) {
                        //dir is empty if the export was canceled.
                        //Otherwise proceed
                        if (directory.length() > 0) {
                            //Export the files to the given path
                            //Inform the user if something went wrong
                            if (exportFilesTo(directory)){
                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.ExportedFilesTo) + " " + directory,
                                        Toast.LENGTH_LONG).show();
                            } else {

                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.ErrorExporting),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                dirChooserDialog.show(getFragmentManager(), "DirChooserDialogTag");

                break;

        }

        return true;
    }



    /**
     * Used to export csv and/or exiftool commands to the device's own storage.
     *
     * @param dir the directory to which the files are to be exported
     * @return false if something went wrong, true otherwise
     */
    private boolean exportFilesTo(String dir){

        //Replace illegal characters from the roll name to make it a valid file name.
        String rollName = Utilities.replaceIllegalChars(database.getRoll(rollId).getName());

        //Get the user setting about which files to export. By default, share both files.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        String filesToExport = prefs.getString(PreferenceConstants.KEY_FILES_TO_EXPORT,
                PreferenceConstants.VALUE_BOTH);

        //Create the file names for the two files to be put in that intent
        String fileNameCsv = rollName + "_csv" + ".txt";
        String fileNameExifToolCmds = rollName + "_ExifToolCmds" + ".txt";

        //Create the strings to be written on those two files
        String csvString = Utilities.createCsvString(getActivity(), database, rollId);
        String exifToolCmds = Utilities.createExifToolCmdsString(getActivity(), database, rollId);

        //Create the files in external storage
        File fileCsv = new File(dir, fileNameCsv);
        File fileExifToolCmds = new File(dir, fileNameExifToolCmds);

        boolean csvExportSuccess = false;
        boolean exifToolCmdsExportSuccess = false;

        if (filesToExport.equals(PreferenceConstants.VALUE_BOTH) ||
                filesToExport.equals(PreferenceConstants.VALUE_CSV)) {
            //Write the csv file
            if (Utilities.writeTextFile(fileCsv, csvString))
                csvExportSuccess = true;
        }

        if (filesToExport.equals(PreferenceConstants.VALUE_BOTH) ||
                filesToExport.equals(PreferenceConstants.VALUE_EXIFTOOL)) {
            //Write the ExifTool commands file
            if (Utilities.writeTextFile(fileExifToolCmds, exifToolCmds))
                exifToolCmdsExportSuccess = true;
        }

        switch (filesToExport) {
            case PreferenceConstants.VALUE_BOTH:
                return csvExportSuccess && exifToolCmdsExportSuccess;
            case PreferenceConstants.VALUE_CSV:
                return csvExportSuccess;
            case PreferenceConstants.VALUE_EXIFTOOL:
                return exifToolCmdsExportSuccess;
            default:
                return false;
        }

    }

    /**
     * Creates an Intent to share exiftool commands and a csv
     * for the frames of the roll in question.
     *
     * @return The intent to be shared.
     */
    private Intent setShareIntentExportRoll() {

        //Replace illegal characters from the roll name to make it a valid file name.
        String rollName = Utilities.replaceIllegalChars(database.getRoll(rollId).getName());

        //Get the user setting about which files to export. By default, share only ExifTool.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        String filesToExport = prefs.getString(PreferenceConstants.KEY_FILES_TO_EXPORT,
                PreferenceConstants.VALUE_BOTH);

        //Create the Intent to be shared, no initialization yet
        Intent shareIntent;

        //Create the files

        //Get the external storage path (not the same as SD card)
        File externalStorageDir = getActivity().getExternalFilesDir(null);

        //Create the file names for the two files to be put in that intent
        String fileNameCsv = rollName + "_csv" + ".txt";
        String fileNameExifToolCmds = rollName + "_ExifToolCmds" + ".txt";

        //Create the strings to be written on those two files
        String csvString = Utilities.createCsvString(getActivity(), database, rollId);
        String exifToolCmds = Utilities.createExifToolCmdsString(getActivity(), database, rollId);

        //Create the files in external storage
        File fileCsv = new File(externalStorageDir, fileNameCsv);
        File fileExifToolCmds = new File(externalStorageDir, fileNameExifToolCmds);

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
            List<String> filesToSend = new ArrayList<>();
            filesToSend.add(getActivity().getExternalFilesDir(null) + "/" + fileNameCsv);
            filesToSend.add(getActivity().getExternalFilesDir(null) + "/" + fileNameExifToolCmds);

            //Create an ArrayList of files.
            //NOTE: putParcelableArrayListExtra requires an ArrayList as its argument
            ArrayList<Uri> files = new ArrayList<>();
            for(String path : filesToSend ) {
                File file = new File(path);
                Uri uri;
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
                Uri uri;
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
                Uri uri;
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                showFrameInfoDialog();
                break;
        }
    }

    /**
     * Create new dialog to edit the selected Frame's information
     *
     * @param position position of the Frame in frameList
     */
    @SuppressLint("CommitTransaction")
    private void showFrameInfoEditDialog(int position) {
        // Edit frame info
        Frame frame = frameList.get(position);
        Bundle arguments = new Bundle();
        String title = "" + getActivity().getString(R.string.EditFrame) + frame.getCount();
        String positiveButton = getActivity().getResources().getString(R.string.OK);
        arguments.putString(ExtraKeys.TITLE, title);
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton);
        arguments.putParcelable(ExtraKeys.FRAME, frame);

        EditFrameDialog dialog = new EditFrameDialog();
        dialog.setTargetFragment(this, EDIT_FRAME_DIALOG);
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditFrameDialog.TAG);
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Show a dialog fragment to add a new frame.
     */
    private void showFrameInfoDialog() {

        // If the frame count is greater than 100, then don't add a new frame.
        if (!frameList.isEmpty()) {
            int countCheck = frameList.get(frameList.size() - 1).getCount() + 1;
            if (countCheck > 100) {
                Toast toast = Toast.makeText(getActivity(),
                        getResources().getString(R.string.TooManyFrames), Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        }

        String title = getActivity().getResources().getString(R.string.NewFrame);
        String positiveButton = getActivity().getResources().getString(R.string.Add);

        Frame frame = new Frame();
        frame.setDate(Utilities.getCurrentTime());
        frame.setCount(0);
        frame.setRollId(rollId);

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
            for (Frame frameIterator : frameList) {
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
            frame.setFilterId(previousFrame.getFilterId());
            frame.setFocalLength(previousFrame.getFocalLength());

        } else {
            frame.setCount(1);
            frame.setShutter(getResources().getString(R.string.NoValue));
            frame.setAperture(getResources().getString(R.string.NoValue));
        }
        frame.setNoOfExposures(1);

        EditFrameDialog dialog = new EditFrameDialog();
        dialog.setTargetFragment(this, FRAME_DIALOG);
        Bundle arguments = new Bundle();
        arguments.putString(ExtraKeys.TITLE, title);
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton);
        arguments.putParcelable(ExtraKeys.FRAME, frame);
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager(), EditFrameDialog.TAG);
    }

    /**
     * Called when the user is done editing or adding a frame and closes the dialog.
     *
     * @param requestCode the request code that was set for the intent.
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed Intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case FRAME_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    Frame frame = data.getParcelableExtra(ExtraKeys.FRAME);

                    if (frame != null) {

                        long rowId = database.addFrame(frame);
                        frame.setId(rowId);
                        frameList.add(frame);
                        Utilities.sortFrameList(getActivity(), database, frameList);
                        frameAdapter.notifyItemInserted(frameList.indexOf(frame));
                        mainTextView.setVisibility(View.GONE);

                        // When the new frame is added jump to view the added entry
                        int pos = frameList.indexOf(frame);
                        if (pos < frameAdapter.getItemCount()) mainRecyclerView.scrollToPosition(pos);
                        // The text to share has changed and we need to update
                        shareActionProvider.setShareIntent(setShareIntentExportRoll());
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;

            case EDIT_FRAME_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    if (actionMode != null) actionMode.finish();

                    Frame frame = data.getParcelableExtra(ExtraKeys.FRAME);

                    if (frame != null && frame.getId() > 0) {

                        database.updateFrame(frame);
                        final int oldPosition = frameList.indexOf(frame);
                        Utilities.sortFrameList(getActivity(), database, frameList);
                        final int newPosition = frameList.indexOf(frame);
                        frameAdapter.notifyItemChanged(oldPosition);
                        frameAdapter.notifyItemMoved(oldPosition, newPosition);

                        // The text to share has changed and we need to update
                        shareActionProvider.setShareIntent(setShareIntentExportRoll());
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
                    frameList = database.getAllFramesFromRoll(rollId);
                    Utilities.sortFrameList(getActivity(), database, frameList);
                    frameAdapter = new FrameAdapter(getActivity(), frameList, this);
                    mainRecyclerView.setAdapter(frameAdapter);
                    frameAdapter.notifyDataSetChanged();
                }

                break;

            case LOCATION_PICK_REQUEST:

                // Consume the case when the user has edited
                // the location of several frames in action mode.
                if (resultCode == Activity.RESULT_OK) {

                    // Exit action mode.
                    if (actionMode != null) actionMode.finish();

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
    public void onItemClick(int position) {
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
    public void onItemLongClick(int position) {
        enableActionMode(position);
    }

    /**
     * Enable action mode if not yet enabled and add item to selected items.
     *
     * @param position position of the item in FrameAdapter
     */
    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        frameAdapter.toggleSelection(position);
        // If the user deselected the last of the selected items, exit action mode.
        if (frameAdapter.getSelectedItemCount() == 0) actionMode.finish();
        // Set the action mode toolbar title to display the number of selected items.
        else actionMode.setTitle(Integer.toString(frameAdapter.getSelectedItemCount()));
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
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
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
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
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
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            // Get the positions in the frameList of selected items.
            final List<Integer> selectedItemPositions = frameAdapter.getSelectedItemPositions();
            switch (item.getItemId()) {

                case R.id.menu_item_delete:

                    AlertDialog.Builder deleteConfirmDialog = new AlertDialog.Builder(getActivity());
                    // Separate confirm titles for one or multiple frames
                    final String title = selectedItemPositions.size() == 1 ?
                            String.format(getResources().getString(R.string.ConfirmFramesDeleteSingle), selectedItemPositions.size()) :
                            String.format(getResources().getString(R.string.ConfirmFramesDelete), selectedItemPositions.size());
                    deleteConfirmDialog.setTitle(title);
                    deleteConfirmDialog.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Do nothing
                        }
                    });
                    deleteConfirmDialog.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for (int j = selectedItemPositions.size() - 1; j >= 0; j--) {
                                final int framePosition = selectedItemPositions.get(j);
                                final Frame frame = frameList.get(framePosition);
                                database.deleteFrame(frame);
                                frameList.remove(frame);
                                frameAdapter.notifyItemRemoved(framePosition);
                            }
                            if (frameList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                            shareActionProvider.setShareIntent(setShareIntentExportRoll());
                            mode.finish();
                        }
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(String.format(getResources().getString(R.string.BatchEditFramesTitle), frameAdapter.getSelectedItemCount()));
                        builder.setItems(R.array.FramesBatchEditOptions, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        // Edit frame counts
                                        new FrameCountBatchEditDialogBuilder(getActivity()).create().show();
                                        break;
                                    case 1:
                                        // Edit location
                                        Intent intent = new Intent(getActivity(), LocationPickActivity.class);
                                        startActivityForResult(intent, LOCATION_PICK_REQUEST);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do nothing
                                dialogInterface.dismiss();
                            }
                        });
                        builder.create().show();
                    }

                    return true;

                case R.id.menu_item_select_all:

                    frameAdapter.toggleSelectionAll();
                    mainRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            frameAdapter.resetAnimateAll();
                        }
                    });
                    mode.setTitle(Integer.toString(frameAdapter.getSelectedItemCount()));
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
        public void onDestroyActionMode(ActionMode mode) {
            frameAdapter.clearSelections();
            actionMode = null;
            mainRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    frameAdapter.resetAnimationIndex();
                }
            });
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

            FrameCountBatchEditDialogBuilder(Context context) {
                super(context);
                setTitle(R.string.EditFrameCountsBy);
                @SuppressLint("InflateParams")
                View view = getActivity().getLayoutInflater().inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker numberPicker = view.findViewById(R.id.number_picker);
                numberPicker.setMaxValue(200);
                numberPicker.setMinValue(0);
                // Use the NumberPicker.setDisplayedValues() method to set custom
                // values ranging from -100 to +100.
                final List<String> displayedValues = new ArrayList<>();
                for (int k = -100; k <= 100; ++k) {
                    if (k > 0) displayedValues.add("+" + Integer.toString(k));
                    else displayedValues.add(Integer.toString(k));
                }
                numberPicker.setDisplayedValues(displayedValues.toArray(new String[0]));
                numberPicker.setValue(100);
                // Block the NumberPicker from activating the cursor.
                numberPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                setView(view);
                setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do nothing
                    }
                });
                setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final int change = Integer.valueOf(displayedValues.get(numberPicker.getValue()));
                        final List<Integer> framePositions = frameAdapter.getSelectedItemPositions();
                        for (int j = framePositions.size() - 1; j >= 0; j--) {
                            final Frame frame = frameList.get(framePositions.get(j));
                            frame.setCount(frame.getCount() + change);
                            database.updateFrame(frame);
                        }
                        if (actionMode != null) actionMode.finish();
                        Utilities.sortFrameList(getActivity(), database, frameList);
                        frameAdapter.notifyDataSetChanged();
                    }
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
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    /**
     * When the fragment is resumed continue location updates and recolour the FloatingActionButton.
     */
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        //Check if GPSUpdate preference has been changed meanwhile
        requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true);
        //Added check to make sure googleApiClient is not null.
        //Apparently some users were encountering a bug where during onResume
        //googleApiClient was null.
        if (locationPermissionsGranted && googleApiClient != null && googleApiClient.isConnected() && requestingLocationUpdates) {
            startLocationUpdates();
        } else {
            stopLocationUpdates();
        }

        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        //The user might have changed the export settings. Update the ShareActionProvider.
        if (shareActionProvider != null)
            shareActionProvider.setShareIntent(setShareIntentExportRoll());

        // If action mode is enabled, color the status bar dark grey.
        if (frameAdapter.getSelectedItemCount() > 0 || actionMode != null) {
            Utilities.setStatusBarColor(getActivity(), ContextCompat.getColor(getActivity(), R.color.dark_grey));
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
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }
        }
    }

    /**
     * Called when the Google API is connected.
     *
     * @param bundle not used
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //Added check to make sure googleApiClient is not null.
            //Apparently some users were encountering a bug where during onResume
            //googleApiClient was null.
            if (googleApiClient != null) {
                lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
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
    public void onConnectionSuspended(int i) {
        //Added check to make sure googleApiClient is not null.
        //Apparently some users were encountering a bug where during onResume
        //googleApiClient was null.
        if (locationPermissionsGranted && googleApiClient != null) googleApiClient.connect();
    }

    /**
     * Called when the location is changed.
     *
     * @param location the new location
     */
    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (result.hasResolution() && !resolvingError) {
            try {
                resolvingError = true;
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
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
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.setTargetFragment(this, ERROR_DIALOG);
        dialogFragment.show(getFragmentManager(), "errordialog");
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
    public static class ErrorDialogFragment extends com.google.android.gms.common.ErrorDialogFragment {

        /**
         * Empty constructor
         */
        public ErrorDialogFragment() { }

        /**
         * {@inheritDoc}
         * @param outState
         */
        @Override
        public void onSaveInstanceState(final Bundle outState){
            setTargetFragment(null, -1);
        }

        /**
         * {@inheritDoc}
         * @param savedInstanceState
         * @return
         */
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                setTargetFragment(
                        getActivity().getFragmentManager().findFragmentByTag(FRAMES_FRAGMENT_TAG),
                        ERROR_DIALOG);
            }
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        /**
         * Pass the onDismiss message to the FramesFragment
         *
         * @param dialog {@inheritDoc}
         */
        @Override
        public void onDismiss(DialogInterface dialog) {
            if (getActivity() == null || getActivity().getFragmentManager() == null) return;
            FramesFragment framesfragment =
                    (FramesFragment) getActivity().getFragmentManager().findFragmentByTag(FRAMES_FRAGMENT_TAG);
            if (framesfragment != null) framesfragment.onDialogDismissed();
            super.onDismiss(dialog);
        }

    }

}
