package com.tommihirvonen.exifnotes.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
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
import android.widget.AbsListView;
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
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Datastructures.Roll;
import com.tommihirvonen.exifnotes.Dialogs.DirectoryChooserDialog;
import com.tommihirvonen.exifnotes.Dialogs.EditFrameInfoDialog;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Activities.GearActivity;
import com.tommihirvonen.exifnotes.Activities.MapsActivity;
import com.tommihirvonen.exifnotes.Activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

/**
 * FramesFragment is the fragment which is called when the user presses on a roll
 * on the ListView in RollsFragment.
 */
public class FramesFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnLongClickListener, AbsListView.OnScrollListener {

    OnHomeAsUpPressedListener mCallback;

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
    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        mCallback = (OnHomeAsUpPressedListener) a;
    }

    /**
     * This on attach is called after API 23
     * @param c Context to which the OnHomeAsUpPressedListener is attached.
     */
    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        mCallback = (OnHomeAsUpPressedListener) c;
    }

    public final static String ROLLINFO_EXTRA_MESSAGE = "com.tommihirvonen.filmphotonotes.MESSAGE";

    FilmDbHelper database;
    TextView mainTextView;
    ListView mainListView;
    ArrayList<Frame> mFrameClassList = new ArrayList<>();
    FrameAdapter mFrameAdapter;

    ShareActionProvider mShareActionProvider;

    long rollId;
    long camera_id;
    int counter = 0;

    FloatingActionButton fab;

    // Google client to interact with Google API
    boolean locationEnabled;
    private GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    boolean mRequestingLocationUpdates;

    public static final int FRAME_INFO_DIALOG = 1;
    public static final int EDIT_FRAME_INFO_DIALOG = 2;

    /**
     * Called when the fragment is created.
     * Get the frames from the roll and enable location updating.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        rollId = getArguments().getLong("ROLL_ID");
        locationEnabled = getArguments().getBoolean("LOCATION_ENABLED");

        database = new FilmDbHelper(getActivity());
        mFrameClassList = database.getAllFramesFromRoll(rollId);
        camera_id = database.getRoll(rollId).getCamera_id();

        //Sort the list according to preferences
        sortFrameList(mFrameClassList);

        // Activate GPS locating if the user has granted permission.
        if (locationEnabled) {

            // Create an instance of GoogleAPIClient for latlng_location services.
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
            // Create locationRequest to update the current latlng_location.
            mLocationRequest = new LocationRequest();
            // 10 seconds
            mLocationRequest.setInterval(10*1000);
            // 1 second
            mLocationRequest.setFastestInterval(1*1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
        // This can be done anyway. It only has effect if locationEnabled is true.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        mRequestingLocationUpdates = prefs.getBoolean("GPSUpdate", true);
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

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(database.getRoll(rollId).getName());
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(this);
        fab.setOnLongClickListener(this);

        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        mainTextView = (TextView) view.findViewById(R.id.no_added_frames);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.frames_listview);
        // Create an ArrayAdapter for the ListView
        mFrameAdapter = new FrameAdapter(getActivity(), android.R.layout.simple_list_item_1, mFrameClassList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mFrameAdapter);

        mainListView.setOnItemClickListener(this);

        registerForContextMenu(mainListView);

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(1);

        if (mFrameClassList.size() >= 1) {
            counter = mFrameClassList.get(mFrameClassList.size() - 1).getCount();
            mainTextView.setVisibility(View.GONE);
        }

        if (mainListView.getCount() >= 1) mainListView.setSelection(mainListView.getCount() - 1);

        mainListView.setOnScrollListener(this);

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
            mShareActionProvider = new ShareActionProvider(getActivity());
            mShareActionProvider.setShareIntent(setShareIntentExportRoll());
        }

        //Link the ShareActionProvider to the menu item
        MenuItemCompat.setActionProvider(shareItem, mShareActionProvider);

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
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if ( getUserVisibleHint() ) {
            switch (item.getItemId()) {
                case R.id.menu_item_edit:

                    int position = info.position;

                    // Edit frame info
                    Frame frame = mFrameClassList.get(position);
                    long _id = frame.getId();
                    long lens_id = frame.getLensId();
                    int count = frame.getCount();
                    String date = frame.getDate();
                    String shutter = frame.getShutter();
                    String aperture = frame.getAperture();
                    String note = frame.getNote();
                    String location = frame.getLocation();
                    String title = "" + getActivity().getString(R.string.EditFrame) + count;
                    String positiveButton = getActivity().getResources().getString(R.string.OK);

                    EditFrameInfoDialog dialog = EditFrameInfoDialog.newInstance(_id, lens_id, position, count, date, shutter, aperture, note, location, camera_id, title, positiveButton);
                    dialog.setTargetFragment(this, EDIT_FRAME_INFO_DIALOG);
                    dialog.show(getFragmentManager().beginTransaction(), EditFrameInfoDialog.TAG);

                    return true;

                case R.id.menu_item_delete:

                    int which = info.position;

                    Frame deletableFrame = mFrameClassList.get(which);
                    database.deleteFrame(deletableFrame);
                    mFrameClassList.remove(which);

                    if (mFrameClassList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                    mFrameAdapter.notifyDataSetChanged();
                    if (mFrameClassList.size() >= 1) counter = mFrameClassList.get(mFrameClassList.size() - 1).getCount();
                    else counter = 0;

                    mShareActionProvider.setShareIntent(setShareIntentExportRoll());

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
                final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                int checkedItem = sharedPref.getInt("FrameSortOrder", 0);
                AlertDialog.Builder sortDialog = new AlertDialog.Builder(getActivity());
                sortDialog.setTitle(R.string.SortBy);
                sortDialog.setSingleChoiceItems(R.array.FrameSortOptions, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("FrameSortOrder", which);
                        editor.commit();
                        dialog.dismiss();
                        sortFrameList(mFrameClassList);
                        mFrameAdapter.notifyDataSetChanged();
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
                preferences_intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, PreferenceFragment.class.getName() );
                preferences_intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );

                startActivity(preferences_intent);

                break;

            case R.id.menu_item_help:

                String helpTitle = getResources().getString(R.string.Help);
                String helpMessage = getResources().getString(R.string.main_help);
                Utilities.showGeneralDialog(getActivity(), helpTitle, helpMessage);

                break;

            case R.id.menu_item_about:

                String aboutTitle = getResources().getString(R.string.app_name);
                String aboutMessage = getResources().getString(R.string.about) + "\n\n\n" + getResources().getString(R.string.VersionHistory);
                Utilities.showGeneralDialog(getActivity(), aboutTitle, aboutMessage);

                break;

            case android.R.id.home:

                // INTERFACE TO MAINACTIVITY
                mCallback.onHomeAsUpPressed();

                break;

            case R.id.menu_item_show_on_map:

                Intent intent2 = new Intent(getActivity(), MapsActivity.class);
                intent2.putExtra(ROLLINFO_EXTRA_MESSAGE, rollId);
                startActivity(intent2);
                break;

            //Export to device
            case 99:

                DirectoryChooserDialog dirChooserDialog = DirectoryChooserDialog.newInstance(new DirectoryChooserDialog.OnChosenDirectoryListener() {
                    @Override
                    public void onChosenDir(String dir) {
                        //dir is empty if the export was canceled.
                        //Otherwise proceed
                        if (dir.length() > 0) {
                            //Export the files to the given path
                            //Inform the user if something went wrong
                            if (!exportFilesTo(dir)){
                                Toast.makeText(getActivity(), getResources().getString(R.string.ErrorExporting), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), getResources().getString(R.string.ExportedFilesTo) + " " + dir, Toast.LENGTH_LONG).show();
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
     * This function is called when the user has selected a sorting criteria.
     */
    public void sortFrameList(ArrayList<Frame> listToSort) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        int sortId = sharedPref.getInt("FrameSortOrder", 0);
        switch (sortId){
            //Sort by count
            case 0:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame o1, Frame o2) {
                        // Negative to reverse the sorting order
                        int count1 = o1.getCount();
                        int count2 = o2.getCount();
                        int result = 0;
                        if (count1 < count2) result = -1;
                        else result = 1;
                        return result;
                    }
                });
                break;

            //Sort by date
            case 1:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame o1, Frame o2) {
                        String date1 = o1.getDate();
                        String date2 = o2.getDate();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d H:m");
                        Date d1 = null;
                        Date d2 = null;
                        try {
                            d1 = format.parse(date1);
                            d2 = format.parse(date2);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        int result = 0;
                        long diff = 0;
                        //Handle possible NullPointerException
                        if (d1 != null && d2 != null) diff = d1.getTime() - d2.getTime();
                        if (diff < 0 ) result = -1;
                        else result = 1;

                        return result;
                    }
                });
                break;

            //Sort by f-stop
            case 2:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame o1, Frame o2) {

                        final String[] allApertureValues = new String[]{getActivity().getString(R.string.NoValue), "1.0", "1.1", "1.2", "1.4", "1.6", "1.8", "2.0", "2.2", "2.5",
                                "2.8", "3.2", "3.5", "4.0", "4.5", "5.0", "5.6", "6.3", "6.7", "7.1", "8", "9", "9.5",
                                "10", "11", "13", "14", "16", "18", "19", "20", "22", "25", "27", "29", "32", "36", "38",
                                "42", "45", "50", "57", "64"};
                        String aperture1 = o1.getAperture();
                        String aperture2 = o2.getAperture();
                        int pos1 = 0;
                        int pos2 = 0;
                        for (int i = 0; i < allApertureValues.length; ++i){
                            if (aperture1.equals(allApertureValues[i])) pos1 = i;
                            if (aperture2.equals(allApertureValues[i])) pos2 = i;
                        }
                        int result = 0;
                        if (pos1 < pos2) result = -1;
                        else result = 1;
                        return result;
                    }
                });
                break;

            //Sort by shutter speed
            case 3:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame o1, Frame o2) {

                        final String[] allShutterValues = new String[]{getActivity().getString(R.string.NoValue), "B", "30", "25", "20", "15", "13", "10", "8", "6", "5", "4",
                                "3.2", "3", "2.5", "2", "1.6", "1.5","1.3", "1", "0.8", "0.7", "0.6", "1/2", "0.4", "1/3", "0.3",
                                "1/4", "1/5", "1/6", "1/8", "1/10", "1/13", "1/15", "1/20", "1/25",
                                "1/30", "1/40", "1/45", "1/50", "1/60", "1/80", "1/90", "1/100", "1/125", "1/160", "1/180", "1/200",
                                "1/250", "1/320", "1/350", "1/400", "1/500", "1/640", "1/750", "1/800", "1/1000", "1/1250", "1/1500",
                                "1/1600", "1/2000", "1/2500", "1/3000", "1/3200", "1/4000", "1/5000", "1/6000", "1/6400", "1/8000"};

                        //Shutter speed strings need to be modified so that the sorting
                        //works properly.
                        String shutter1 = o1.getShutter().replace("\"", "");
                        String shutter2 = o2.getShutter().replace("\"", "");
                        int pos1 = 0;
                        int pos2 = 0;
                        for (int i = 0; i < allShutterValues.length; ++i){
                            if (shutter1.equals(allShutterValues[i])) pos1 = i;
                            if (shutter2.equals(allShutterValues[i])) pos2 = i;
                        }
                        int result = 0;
                        if (pos1 < pos2) result = -1;
                        else result = 1;
                        return result;
                    }
                });
                break;

            //Sort by lens
            case 4:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame o1, Frame o2) {
                        String s1;
                        Lens lens1;

                        String s2;
                        Lens lens2;

                        if (o1.getLensId() != -1) {
                            lens1 = database.getLens(o1.getLensId());
                            s1 = lens1.getMake() + lens1.getModel();
                        } else s1 = "-1";
                        if (o2.getLensId() != -1) {
                            lens2 = database.getLens(o2.getLensId());
                            s2 = lens2.getMake() + lens2.getModel();
                        } else s2 = "-1";

                        return s1.compareTo(s2);
                    }
                });
                break;
        }
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String filesToExport = prefs.getString("FilesToExport", "BOTH");

        //Create the file names for the two files to be put in that intent
        String fileNameCsv = rollName + "_csv" + ".txt";
        String fileNameExifToolCmds = rollName + "_ExifToolCmds" + ".txt";

        //Create the strings to be written on those two files
        String csvString = createCsvString();
        String exifToolCmds = createExifToolCmdsString();

        //Create the files in external storage
        File fileCsv = new File(dir, fileNameCsv);
        File fileExifToolCmds = new File(dir, fileNameExifToolCmds);

        if (filesToExport.equals("BOTH") || filesToExport.equals("CSV")) {
            //Write the csv file
            if (! Utilities.writeTextFile(fileCsv, csvString)) return false;
        }

        if (filesToExport.equals("BOTH") || filesToExport.equals("EXIFTOOL")) {
            //Write the ExifTool commands file
            if (! Utilities.writeTextFile(fileExifToolCmds, exifToolCmds)) return false;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
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
        String csvString = createCsvString();
        String exifToolCmds = createExifToolCmdsString();

        //Create the files in external storage
        File fileCsv = new File(externalStorageDir, fileNameCsv);
        File fileExifToolCmds = new File(externalStorageDir, fileNameExifToolCmds);

        FileOutputStream fOut = null;

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
            ArrayList<String> filesToSend = new ArrayList<>();
            filesToSend.add(getActivity().getExternalFilesDir(null) + "/" + fileNameCsv);
            filesToSend.add(getActivity().getExternalFilesDir(null) + "/" + fileNameExifToolCmds);

            //Create an array of Files
            ArrayList<Uri> files = new ArrayList<Uri>();
            for(String path : filesToSend ) {
                File file = new File(path);
                Uri uri = Uri.fromFile(file);
                files.add(uri);
            }

            //Add the two files to the Intent as extras
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);

        } else {
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            //The user has chosen to export only the csv
            if (filesToExport.equals("CSV")) shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(fileCsv));
            //The user has chosen to export only the ExifTool commands
            else if (filesToExport.equals("EXIFTOOL")) shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(fileExifToolCmds));
        }

        return shareIntent;
    }

    /**
     * This function creates a string containing the ExifTool commands about the roll
     *
     * @return String containing the ExifTool commands
     */
    private String createExifToolCmdsString() {
        StringBuilder stringBuilder = new StringBuilder();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String artistName = prefs.getString("ArtistName", "");
        String copyrightInformation = prefs.getString("CopyrightInformation", "");
        String exiftoolPath = prefs.getString("ExiftoolPath", "");
        String picturesPath = prefs.getString("PicturesPath", "");

        String exiftoolCmd = "exiftool";
        String artistTag = "-Artist=";
        String copyrightTag = "-Copyright=";
        String cameraMakeTag = "-Make=";
        String cameraModelTag = "-Model=";
        String lensMakeTag = "-LensMake=";
        String lensModelTag = "-LensModel=";
        String dateTag = "-DateTime=";
        String shutterTag = "-ShutterSpeedValue=";
        String apertureTag = "-ApertureValue=";
        String commentTag = "-UserComment=";
        String gpsLatTag = "-GPSLatitude=";
        String gpsLatRefTag = "-GPSLatitudeRef=";
        String gpsLngTag = "-GPSLongitude=";
        String gpsLngRefTag = "-GPSLongitudeRef=";
        String fileEnding = ".jpg";
        String quote = "\"";
        String space = " ";
        String lineSep = System.getProperty("line.separator");

        for ( Frame frame : mFrameClassList ) {
            if ( exiftoolPath.length() > 0 ) stringBuilder.append(exiftoolPath);
            stringBuilder.append(exiftoolCmd + space);
            stringBuilder.append(cameraMakeTag + quote + database.getCamera(camera_id).getMake() + quote + space);
            stringBuilder.append(cameraModelTag + quote + database.getCamera(camera_id).getModel() + quote + space);
            if ( frame.getLensId() != -1 ) {
                stringBuilder.append(lensMakeTag + quote + database.getLens(frame.getLensId()).getMake() + quote + space);
                stringBuilder.append(lensModelTag + quote + database.getLens(frame.getLensId()).getModel() + quote + space);
            }
            stringBuilder.append(dateTag + quote + frame.getDate().replace("-", ":") + quote + space);
            if ( !frame.getShutter().contains("<") ) stringBuilder.append(shutterTag + quote + frame.getShutter().replace("\"","") + quote + space);
            if ( !frame.getAperture().contains("<") )stringBuilder.append(apertureTag + quote + frame.getAperture() + quote + space);
            if ( frame.getNote().length() > 0 ) stringBuilder.append(commentTag + quote + Normalizer.normalize(frame.getNote(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "") + quote + space);

            if ( frame.getLocation().length() > 0 ) {
                String latString = frame.getLocation().substring(0, frame.getLocation().indexOf(" "));
                String lngString = frame.getLocation().substring(frame.getLocation().indexOf(" ") + 1, frame.getLocation().length());
                String latRef = "";
                if (latString.substring(0, 1).equals("-")) {
                    latRef = "S";
                    latString = latString.substring(1, latString.length());
                } else latRef = "N";
                String lngRef = "";
                if (lngString.substring(0, 1).equals("-")) {
                    lngRef = "W";
                    lngString = lngString.substring(1, lngString.length());
                } else lngRef = "E";
                latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
                List<String> latStringList = Arrays.asList(latString.split(":"));
                lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
                List<String> lngStringList = Arrays.asList(lngString.split(":"));

                stringBuilder.append(gpsLatTag + quote + latStringList.get(0) + space + latStringList.get(1) + space + latStringList.get(2) + quote + space);
                stringBuilder.append(gpsLatRefTag + quote + latRef + quote + space);
                stringBuilder.append(gpsLngTag + quote + lngStringList.get(0) + space + lngStringList.get(1) + space + lngStringList.get(2) + quote + space);
                stringBuilder.append(gpsLngRefTag + quote + lngRef + quote + space);
            }

            if ( artistName.length() > 0 ) stringBuilder.append(artistTag + quote + artistName + quote + space);
            if ( copyrightInformation.length() > 0 ) stringBuilder.append(copyrightTag + quote + copyrightInformation + quote + space);
            if ( picturesPath.contains(" ") ) stringBuilder.append(quote);
            if ( picturesPath.length() > 0 ) stringBuilder.append(picturesPath);
            stringBuilder.append(frame.getCount() + fileEnding);
            if ( picturesPath.contains(" ") ) stringBuilder.append(quote);
            stringBuilder.append(";" + lineSep + lineSep);

        }

        return stringBuilder.toString();
    }

    /**
     * This function creates a string which contains csv information about the roll.
     *
     * @return String containing the csv information
     */
    private String createCsvString() {
        // Get the roll and its information
        Roll roll = database.getRoll(rollId);

        final String separator = ",";
        StringBuilder stringBuilder = new StringBuilder();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String artistName = prefs.getString("ArtistName", "");
        String copyrightInformation = prefs.getString("CopyrightInformation", "");

        stringBuilder.append("Roll name: " + roll.getName() + "\n");
        stringBuilder.append("Added: " + roll.getDate() + "\n");
        stringBuilder.append("Camera: " + database.getCamera(camera_id).getMake() + " " + database.getCamera(camera_id).getModel() + "\n");
        stringBuilder.append("Notes: " + roll.getNote() + "\n");
        stringBuilder.append("Artist name: " + artistName + "\n");
        stringBuilder.append("Copyright: " + copyrightInformation + "\n");
        stringBuilder.append("Frame Count" + separator + "Date" + separator + "Lens" + separator +
                "Shutter" + separator + "Aperture" + separator + "Notes" + separator + "Location" + "\n");

        for ( Frame frame : mFrameClassList ) {
            stringBuilder.append(frame.getCount());
            stringBuilder.append(separator);
            stringBuilder.append(frame.getDate());
            stringBuilder.append(separator);
            if ( frame.getLensId() != -1 ) stringBuilder.append(database.getLens(frame.getLensId()).getMake() + " " + database.getLens(frame.getLensId()).getModel());
            stringBuilder.append(separator);
            if ( !frame.getShutter().contains("<") )stringBuilder.append(frame.getShutter());
            stringBuilder.append(separator);
            if ( !frame.getAperture().contains("<") )stringBuilder.append("f" + frame.getAperture());
            stringBuilder.append(separator);
            if ( frame.getNote().length() > 0 ) stringBuilder.append(frame.getNote());
            stringBuilder.append(separator);
            if ( frame.getLocation().length() > 0 ) {
                String latString = frame.getLocation().substring(0, frame.getLocation().indexOf(" "));
                String lngString = frame.getLocation().substring(frame.getLocation().indexOf(" ") + 1, frame.getLocation().length());
                String latRef = "";
                if (latString.substring(0, 1).equals("-")) {
                    latRef = "S";
                    latString = latString.substring(1, latString.length());
                } else latRef = "N";
                String lngRef = "";
                if (lngString.substring(0, 1).equals("-")) {
                    lngRef = "W";
                    lngString = lngString.substring(1, lngString.length());
                } else lngRef = "E";
                latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
                List<String> latStringList = Arrays.asList(latString.split(":"));
                lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
                List<String> lngStringList = Arrays.asList(lngString.split(":"));

                String space = " ";

                stringBuilder.append(latStringList.get(0) + "°" + space +
                        latStringList.get(1) + "\'" + space +
                        latStringList.get(2).replace(',', '.') + "\"" + space);

                stringBuilder.append(latRef + space);

                stringBuilder.append(lngStringList.get(0) + "°" + space +
                        lngStringList.get(1) + "\'" + space +
                        lngStringList.get(2).replace(',', '.') + "\"" + space);

                stringBuilder.append(lngRef);
            }
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
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
     * If the FloatingActionButton is long pressed then hide it for three seconds
     * or until the ListView is scrolled.
     *
     * @param v the view that was clicked
     * @return true because the click was consumed
     */
    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.fab:

                fab.hide();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fab.show();
                    }
                }, 3000);

                break;
        }
        return true;
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
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Edit frame info
        Frame frame = mFrameClassList.get(position);
        long _id = frame.getId();
        long lens_id = frame.getLensId();
        int count = frame.getCount();
        String date = frame.getDate();
        String shutter = frame.getShutter();
        String aperture = frame.getAperture();
        String note = frame.getNote();
        String location = frame.getLocation();
        String title = "" + getActivity().getString(R.string.EditFrame) + count;
        String positiveButton = getActivity().getResources().getString(R.string.OK);

        EditFrameInfoDialog dialog = EditFrameInfoDialog.newInstance(_id, lens_id, position, count, date, shutter, aperture, note, location, camera_id, title, positiveButton);
        dialog.setTargetFragment(this, EDIT_FRAME_INFO_DIALOG);
        dialog.show(getFragmentManager().beginTransaction(), EditFrameInfoDialog.TAG);
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Shows a dialog fragment to add a new frame.
     */
    private void showFrameInfoDialog() {

        // If the frame count is greater than 100, then don't add a new frame.
        if (!mFrameClassList.isEmpty()) {
            int countCheck = mFrameClassList.get(mFrameClassList.size() - 1).getCount() + 1;
            if (countCheck > 100) {
                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.TooManyFrames), Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        }

        long lens_id = -1;
        int count = 0;
        String date = getCurrentTime();
        String shutter;
        String aperture;
        String location;
        String title = getActivity().getResources().getString(R.string.NewFrame);
        String positiveButton = getActivity().getResources().getString(R.string.Add);

        //Get the location only if the app has location permission (locationEnabled) and
        //the user has enabled GPS updates in the app's settings.
        if ( locationEnabled && PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext()).getBoolean("GPSUpdate", true) ) location = locationStringFromLocation(mLastLocation);
        else location = "";

        if (!mFrameClassList.isEmpty()) {

            //Get the information for the last added frame.
            //The last added frame has the highest id number (database autoincrement).
            Frame previousFrame = mFrameClassList.get(mFrameClassList.size() - 1);
            long i = 0;
            for (Frame frame : mFrameClassList) {
                if (frame.getId() > i) {
                    i = frame.getId();
                    previousFrame = frame;
                }
                //Set the frame count to one higher than the highest frame count
                if (frame.getCount() >= count) count = frame.getCount() + 1;
            }
            lens_id = previousFrame.getLensId();
            shutter = previousFrame.getShutter();
            aperture = previousFrame.getAperture();
        } else {
            count = 1;
            shutter = getResources().getString(R.string.NoValue);
            aperture = getResources().getString(R.string.NoValue);
        }

        EditFrameInfoDialog dialog = EditFrameInfoDialog.newInstance(-1, lens_id, -1, count, date,
                shutter, aperture, "", location, camera_id, title, positiveButton);
        dialog.setTargetFragment(this, FRAME_INFO_DIALOG);
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

                if ( resultCode == Activity.RESULT_OK ) {

                    int count = data.getIntExtra("COUNT", -1);
                    String date = data.getStringExtra("DATE");
                    long lens_id = data.getLongExtra("LENS_ID", -1);
                    String shutter = data.getStringExtra("SHUTTER");
                    String aperture = data.getStringExtra("APERTURE");
                    String note = data.getStringExtra("NOTE");
                    String location = data.getStringExtra("LOCATION");

                    if ( count != -1 ) {

                        Frame frame = new Frame(rollId, count, date, lens_id, shutter, aperture, note, location);

                        // Save the file when the new frame has been added
                        long rowId = database.addFrame(frame);
                        frame.setId(rowId);

                        mFrameClassList.add(frame);
                        sortFrameList(mFrameClassList);
                        mFrameAdapter.notifyDataSetChanged();
                        mainTextView.setVisibility(View.GONE);

                        // When the new frame is added jump to view the added entry
                        int pos = 0;
                        pos = mFrameClassList.indexOf(frame);
                        if (pos < mainListView.getCount()) mainListView.setSelection(pos);
                        // The text you'd like to share has changed,
                        // and you need to update
                        mShareActionProvider.setShareIntent(setShareIntentExportRoll());
                    }
                } else if ( resultCode == Activity.RESULT_CANCELED ) {
                    // After cancel do nothing
                    return;
                }
                break;

            case EDIT_FRAME_INFO_DIALOG:

                if ( resultCode == Activity.RESULT_OK ) {

                    long _id = data.getLongExtra("ID", -1);
                    int count = data.getIntExtra("COUNT", -1);
                    int position = data.getIntExtra("POSITION", -1);
                    String date = data.getStringExtra("DATE");
                    long lens_id = data.getLongExtra("LENS_ID", -1);
                    String shutter = data.getStringExtra("SHUTTER");
                    String aperture = data.getStringExtra("APERTURE");
                    String note = data.getStringExtra("NOTE");
                    String location = data.getStringExtra("LOCATION");

                    if ( _id != -1 && count != -1 && position != -1 ) {

                        Frame frame = new Frame();
                        frame.setId(_id);
                        frame.setRoll(rollId);
                        frame.setLensId(lens_id);
                        frame.setCount(count);
                        frame.setDate(date);
                        frame.setShutter(shutter);
                        frame.setAperture(aperture);
                        frame.setNote(note);
                        frame.setLocation(location);
                        database.updateFrame(frame);
                        //Make the change in the class list and the list view
                        mFrameClassList.get(position).setLensId(lens_id);
                        mFrameClassList.get(position).setCount(count);
                        mFrameClassList.get(position).setDate(date);
                        mFrameClassList.get(position).setShutter(shutter);
                        mFrameClassList.get(position).setAperture(aperture);
                        mFrameClassList.get(position).setNote(note);
                        mFrameClassList.get(position).setLocation(location);
                        sortFrameList(mFrameClassList);
                        mFrameAdapter.notifyDataSetChanged();

                        // The text you'd like to share has changed,
                        // and you need to update
                        mShareActionProvider.setShareIntent(setShareIntentExportRoll());
                    }
                } else if ( resultCode == Activity.RESULT_CANCELED ) {
                    // After cancel do nothing
                    return;
                }
                break;

        }
    }

    /**
     * This function is called when the user scrolls the ListView.
     * It displays the FloatingActionButton in case it was hidden.
     *
     * @param view not used
     * @param scrollState not used
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        fab.show();
    }

    /**
     * Not used
     * @param view not used
     * @param firstVisibleItem not used
     * @param visibleItemCount not used
     * @param totalItemCount not used
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    /**
     * This function is used to convert a Location to a string.
     *
     * @param location Location to be converted
     * @return the converted string
     */
    public static String locationStringFromLocation(final Location location) {
        if (location != null)
            return (Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + " " + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES)).replace(",", ".");
        else return "";
    }

    /**
     * Gets the current date and time.
     * @return Date and time as a string in format YYYY-M-D H:MM
     */
    public static String getCurrentTime() {
        final Calendar c = Calendar.getInstance();
        int iYear = c.get(Calendar.YEAR);
        int iMonth = c.get(Calendar.MONTH) + 1;
        int iDay = c.get(Calendar.DAY_OF_MONTH);
        int iHour = c.get(Calendar.HOUR_OF_DAY);
        int iMin = c.get(Calendar.MINUTE);
        String current_time;
        if (iMin < 10) {
            current_time = iYear + "-" + iMonth + "-" + iDay + " " + iHour + ":0" + iMin;
        } else current_time = iYear + "-" + iMonth + "-" + iDay + " " + iHour + ":" + iMin;
        return current_time;
    }



    /**
     * When the fragment is started connect to Google Play services to get accurate location.
     */
    public void onStart() {
        if ( locationEnabled ) mGoogleApiClient.connect();
        super.onStart();
    }

    /**
     * When the fragment is stopped disconnect from the Google Play service.s
     */
    public void onStop() {
        if ( locationEnabled ) mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * When the fragment is paused also pause location updates.
     */
    @Override
    public void onPause() {
        super.onPause();
        if ( locationEnabled ) stopLocationUpdates();
    }

    /**
     * This function is called when the fragment is paused.
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * When the fragment is resumed continue location updates and recolour the FloatingActionButton.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (locationEnabled && mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        //The user might have changed the export settings. Update the ShareActionProvider.
        if (mShareActionProvider != null) mShareActionProvider.setShareIntent(setShareIntentExportRoll());
    }

    /**
     * This function is called when the fragment is resumed.
     */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        else LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Called when the Google API is connected
     * @param bundle not used
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mRequestingLocationUpdates) {
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
        if ( locationEnabled ) mGoogleApiClient.connect();
    }

    /**
     * Called when the location is changed.
     * @param location the new location
     */
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }









    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    /**
     * Called if the connection to the Google API has failed.
     * @param result describes if the connection was successful
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
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
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {

        }
    }

}
