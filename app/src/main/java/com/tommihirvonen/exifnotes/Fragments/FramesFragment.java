package com.tommihirvonen.exifnotes.Fragments;

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
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.tommihirvonen.exifnotes.Adapters.FrameAdapter;
import com.tommihirvonen.exifnotes.Datastructures.Frame;
import com.tommihirvonen.exifnotes.Dialogs.DirectoryChooserDialog;
import com.tommihirvonen.exifnotes.Dialogs.EditFrameInfoDialog;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Activities.GearActivity;
import com.tommihirvonen.exifnotes.Activities.MapsActivity;
import com.tommihirvonen.exifnotes.Activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

/**
 * FramesFragment is the fragment which is called when the user presses on a roll
 * on the ListView in RollsFragment.
 */
public class FramesFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public final static String ROLLINFO_EXTRA_MESSAGE = "com.tommihirvonen.filmphotonotes.MESSAGE";
    public static final String FRAMES_FRAGMENT_TAG = "FRAMES_FRAGMENT";
    public static final int FRAME_INFO_DIALOG = 1;
    public static final int EDIT_FRAME_INFO_DIALOG = 2;
    public static final int ERROR_DIALOG = 3;

    FilmDbHelper database;
    TextView mainTextView;
    ListView mainListView;
    List<Frame> frameList = new ArrayList<>();
    FrameAdapter frameAdapter;
    Utilities utilities;

    ShareActionProvider shareActionProvider;

    long rollId;
    long cameraId;
    int counter = 0;

    FloatingActionButton floatingActionButton;

    // Google client to interact with Google API
    boolean locationEnabled;
    private GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    boolean requestingLocationUpdates;

    OnHomeAsUpPressedListener callback;

    /**
     * This interface is implemented in MainActivity.
     */
    public interface OnHomeAsUpPressedListener {
        void onHomeAsUpPressed();
    }

    /**
     * This on attach is called before API 23
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
     * @param c Context to which the OnHomeAsUpPressedListener is attached.
     */
    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        callback = (OnHomeAsUpPressedListener) c;
    }



    /**
     * Called when the fragment is created.
     * Get the frames from the roll and enable location updating.
     *
     * @param savedInstanceState saved state of the Fragment
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        utilities = new Utilities(getActivity());

        rollId = getArguments().getLong("ROLL_ID");
        locationEnabled = getArguments().getBoolean("LOCATION_ENABLED");

        database = FilmDbHelper.getInstance(getActivity());
        frameList = database.getAllFramesFromRoll(rollId);
        cameraId = database.getRoll(rollId).getCameraId();

        //Sort the list according to preferences
        Utilities.sortFrameList(getActivity(), database, frameList);

        // Activate GPS locating if the user has granted permission.
        if (locationEnabled) {

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
        // This can be done anyway. It only has effect if locationEnabled is true.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        requestingLocationUpdates = prefs.getBoolean("GPSUpdate", true);
    }

    /**
     * Inflate the fragment.
     *
     * @param inflater not used
     * @param container not used
     * @param savedInstanceState not used
     * @return The inflated view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        final View view = linf.inflate(R.layout.frames_fragment, container, false);

        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            //noinspection ConstantConditions
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(
                    database.getRoll(rollId).getName());
            //noinspection ConstantConditions
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        floatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);
        //floatingActionButton.setOnLongClickListener(this);

        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = (TextView) view.findViewById(R.id.no_added_frames);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.frames_listview);
        // Create an ArrayAdapter for the ListView
        frameAdapter = new FrameAdapter(
                getActivity(), android.R.layout.simple_list_item_1, frameList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(frameAdapter);

        mainListView.setOnItemClickListener(this);

        registerForContextMenu(mainListView);

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(
                new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(1);

        if (frameList.size() >= 1) {
            counter = frameList.get(frameList.size() - 1).getCount();
            mainTextView.setVisibility(View.GONE);
        }

        if (mainListView.getCount() >= 1) mainListView.setSelection(mainListView.getCount() - 1);

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
     * Inflate the context menu to show actions when pressing and holding on a roll.
     *
     * @param menu the menu to be inflated
     * @param v the context menu view, not used
     * @param menuInfo not used
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_delete_edit, menu);
    }

    /**
     * Called when the user long presses on a frame AND selects a context menu item.
     * @param item the context menu item that was selected
     * @return true if the FramesFragment is in front, false if it is not
     */
    @SuppressLint("CommitTransaction")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (getUserVisibleHint()) {
            switch (item.getItemId()) {
                case R.id.menu_item_edit:

                    int position = info.position;

                    // Edit frame info
                    Frame frame = frameList.get(position);
                    String title = "" + getActivity().getString(R.string.EditFrame) + frame.getCount();
                    String positiveButton = getActivity().getResources().getString(R.string.OK);
                    Bundle arguments = new Bundle();
                    arguments.putString("TITLE", title);
                    arguments.putString("POSITIVE_BUTTON", positiveButton);
                    arguments.putParcelable("FRAME", frame);

                    EditFrameInfoDialog dialog = new EditFrameInfoDialog();
                    dialog.setTargetFragment(this, EDIT_FRAME_INFO_DIALOG);
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), EditFrameInfoDialog.TAG);

                    return true;

                case R.id.menu_item_delete:

                    int which = info.position;

                    Frame deletableFrame = frameList.get(which);
                    database.deleteFrame(deletableFrame);
                    frameList.remove(which);

                    if (frameList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                    frameAdapter.notifyDataSetChanged();
                    if (frameList.size() >= 1)
                        counter = frameList.get(frameList.size() - 1).getCount();
                    else counter = 0;

                    shareActionProvider.setShareIntent(setShareIntentExportRoll());

                    return true;
            }
        }
        return false;
    }

    /**
     * Handle events when the user selects an action from the options menu.
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
                int checkedItem = sharedPref.getInt("FrameSortOrder", 0);
                AlertDialog.Builder sortDialog = new AlertDialog.Builder(getActivity());
                sortDialog.setTitle(R.string.SortBy);
                sortDialog.setSingleChoiceItems(
                        R.array.FrameSortOptions, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("FrameSortOrder", which);
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
                Intent intent = new Intent(getActivity(), GearActivity.class);
                startActivity(intent);

                break;
            case R.id.menu_item_preferences:

                Intent preferences_intent = new Intent(getActivity(), PreferenceActivity.class);
                // With these extras we can skip the headers in the preferences.
                preferences_intent.putExtra(
                        PreferenceActivity.EXTRA_SHOW_FRAGMENT, PreferenceFragment.class.getName());
                preferences_intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);

                startActivity(preferences_intent);

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

                Intent intent2 = new Intent(getActivity(), MapsActivity.class);
                intent2.putExtra(ROLLINFO_EXTRA_MESSAGE, rollId);
                startActivity(intent2);
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
                            if (!exportFilesTo(directory)){
                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.ErrorExporting),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.ExportedFilesTo) + " " + directory,
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
     * This function is used to export csv and/or exiftool commands to the devices own storage.
     * @param dir the directory to which the files are to be exported
     * @return false if something went wrong, true otherwise
     */
    private boolean exportFilesTo(String dir){

        //Replace illegal characters from the roll name to make it a valid file name.
        String rollName = Utilities.replaceIllegalChars(database.getRoll(rollId).getName());

        //Get the user setting about which files to export. By default, share both files.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getBaseContext());
        String filesToExport = prefs.getString("FilesToExport", "BOTH");

        //Create the file names for the two files to be put in that intent
        String fileNameCsv = rollName + "_csv" + ".txt";
        String fileNameExifToolCmds = rollName + "_ExifToolCmds" + ".txt";

        //Create the strings to be written on those two files
        String csvString = Utilities.createCsvString(getActivity(), database, rollId);
        String exifToolCmds = Utilities.createExifToolCmdsString(getActivity(), database, rollId);

        //Create the files in external storage
        File fileCsv = new File(dir, fileNameCsv);
        File fileExifToolCmds = new File(dir, fileNameExifToolCmds);

        if (filesToExport.equals("BOTH") || filesToExport.equals("CSV")) {
            //Write the csv file
            if (!Utilities.writeTextFile(fileCsv, csvString)) return false;
        }

        if (filesToExport.equals("BOTH") || filesToExport.equals("EXIFTOOL")) {
            //Write the ExifTool commands file
            if (!Utilities.writeTextFile(fileExifToolCmds, exifToolCmds)) return false;
        }

        //Export was successful
        return true;
    }

    /**
     * This function creates an Intent to share exiftool commands and a csv
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
        String filesToExport = prefs.getString("FilesToExport", "BOTH");

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

        //FileOutputStream fOut = null;

        //Write the csv file
        Utilities.writeTextFile(fileCsv, csvString);

        //Write the ExifTool commands file
        Utilities.writeTextFile(fileExifToolCmds, exifToolCmds);

        //If the user has chosen to export both files
        if (filesToExport.equals("BOTH")) {
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
            if (filesToExport.equals("CSV")) {
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
            else if (filesToExport.equals("EXIFTOOL")) {
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
     * This function is called when FloatingActionButton is pressed.
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
     * This function is called when a frame is pressed.
     * Show the EditFrameInfoDialog.
     *
     * @param parent the parent AdapterView, not used
     * @param view the view of the clicked item, not used
     * @param position position of the item in the ListView
     * @param id id of the item clicked, not used
     */
    @SuppressLint("CommitTransaction")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Edit frame info
        Frame frame = frameList.get(position);
        Bundle arguments = new Bundle();
        String title = "" + getActivity().getString(R.string.EditFrame) + frame.getCount();
        String positiveButton = getActivity().getResources().getString(R.string.OK);
        arguments.putString("TITLE", title);
        arguments.putString("POSITIVE_BUTTON", positiveButton);
        arguments.putParcelable("FRAME", frame);

        EditFrameInfoDialog dialog = new EditFrameInfoDialog();
        dialog.setTargetFragment(this, EDIT_FRAME_INFO_DIALOG);
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditFrameInfoDialog.TAG);
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Shows a dialog fragment to add a new frame.
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

        //Get the location only if the app has location permission (locationEnabled) and
        //the user has enabled GPS updates in the app's settings.
        if (locationEnabled &&
                PreferenceManager.getDefaultSharedPreferences(
                        getActivity().
                        getBaseContext()).getBoolean("GPSUpdate", true))
            frame.setLocation(Utilities.locationStringFromLocation(lastLocation));
        //else frame.setLocation("");

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
            frame.setLensId(previousFrame.getLensId());
            frame.setShutter(previousFrame.getShutter());
            frame.setAperture(previousFrame.getAperture());
            frame.setFilterId(previousFrame.getFilterId());
            frame.setFocalLength(previousFrame.getFocalLength());
            frame.setExposureComp(previousFrame.getExposureComp());
        } else {
            frame.setCount(1);
            frame.setShutter(getResources().getString(R.string.NoValue));
            frame.setAperture(getResources().getString(R.string.NoValue));
        }

        EditFrameInfoDialog dialog = new EditFrameInfoDialog();
        dialog.setTargetFragment(this, FRAME_INFO_DIALOG);
        Bundle arguments = new Bundle();
        arguments.putString("TITLE", title);
        arguments.putString("POSITIVE_BUTTON", positiveButton);
        arguments.putParcelable("FRAME", frame);
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager(), EditFrameInfoDialog.TAG);
    }

    /**
     * This function is called when the user is done editing or adding a frame and
     * closes the dialog.
     *
     * @param requestCode the request code that was set for the intent.
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed Intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case FRAME_INFO_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    Frame frame = data.getParcelableExtra("FRAME");

                    if (frame != null) {

                        // Save the file when the new frame has been added
                        long rowId = database.addFrame(frame);
                        frame.setId(rowId);

                        frameList.add(frame);
                        Utilities.sortFrameList(getActivity(), database, frameList);
                        frameAdapter.notifyDataSetChanged();
                        mainTextView.setVisibility(View.GONE);

                        // When the new frame is added jump to view the added entry
                        int pos = frameList.indexOf(frame);
                        if (pos < mainListView.getCount()) mainListView.setSelection(pos);
                        // The text you'd like to share has changed,
                        // and you need to update
                        shareActionProvider.setShareIntent(setShareIntentExportRoll());
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;

            case EDIT_FRAME_INFO_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    Frame frame = data.getParcelableExtra("FRAME");

                    if (frame != null && frame.getId() > 0) {

                        database.updateFrame(frame);
                        Utilities.sortFrameList(getActivity(), database, frameList);
                        frameAdapter.notifyDataSetChanged();

                        // The text you'd like to share has changed,
                        // and you need to update
                        shareActionProvider.setShareIntent(setShareIntentExportRoll());
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;

        }
    }

    /**
     * When the fragment is started connect to Google Play services to get accurate location.
     */
    public void onStart() {
        if (locationEnabled) googleApiClient.connect();
        super.onStart();
    }

    /**
     * When the fragment is stopped disconnect from the Google Play service.s
     */
    public void onStop() {
        if (locationEnabled) googleApiClient.disconnect();
        super.onStop();
    }

    /**
     * When the fragment is paused also pause location updates.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (locationEnabled) stopLocationUpdates();
    }

    /**
     * This function is called when the fragment is paused.
     */
    protected void stopLocationUpdates() {
        if (googleApiClient.isConnected())
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
        requestingLocationUpdates = prefs.getBoolean("GPSUpdate", true);
        if (locationEnabled && googleApiClient.isConnected() && requestingLocationUpdates) {
            startLocationUpdates();
        } else {
            stopLocationUpdates();
        }

        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        //The user might have changed the export settings. Update the ShareActionProvider.
        if (shareActionProvider != null)
            shareActionProvider.setShareIntent(setShareIntentExportRoll());
    }

    /**
     * This function is called when the fragment is resumed.
     */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    /**
     * Called when the Google API is connected
     * @param bundle not used
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (requestingLocationUpdates) {
                startLocationUpdates();
            }
        }
    }

    /**
     * Called when the connection to the Google API is suspended
     * @param i not used
     */
    @Override
    public void onConnectionSuspended(int i) {
        if ( locationEnabled ) googleApiClient.connect();
    }

    /**
     * Called when the location is changed.
     * @param location the new location
     */
    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }









    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean resolvingError = false;

    /**
     * Called if the connection to the Google API has failed.
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
                googleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            resolvingError = true;
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
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

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        resolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends com.google.android.gms.common.ErrorDialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public void onSaveInstanceState(final Bundle outState){
            setTargetFragment(null, -1);
        }

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

        @Override
        public void onDismiss(DialogInterface dialog) {
            FramesFragment framesfragment =
                    (FramesFragment)getActivity().getFragmentManager().findFragmentByTag(FRAMES_FRAGMENT_TAG);
            framesfragment.onDialogDismissed();
        }
    }

}
