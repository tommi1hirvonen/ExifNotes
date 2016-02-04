package com.tommihirvonen.filmphotonotes;

// Copyright 2015
// Tommi Hirvonen

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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Gravity;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class FramesFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    OnHomeAsUpPressedListener mCallback;

    public interface OnHomeAsUpPressedListener {
        void onHomeAsUpPressed();
    }

    // This on attach is called before API 23
    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        mCallback = (OnHomeAsUpPressedListener) a;
    }

    // This on attach is called after API 23
    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        mCallback = (OnHomeAsUpPressedListener) c;
    }

    public final static String ROLLINFO_EXTRA_MESSAGE = "com.tommihirvonen.filmphotonotes.MESSAGE";
    public final static String LOCATION_ENABLED_EXTRA = "LocationEnabled";

    FilmDbHelper database;
    TextView mainTextView;
    ListView mainListView;
    ArrayList<Frame> mFrameClassList = new ArrayList<>();
    FrameAdapter mFrameAdapter;
    ShareActionProvider mShareActionProvider;
    int rollId;
    int camera_id;
    int counter = 0;

    FloatingActionButton fab;

    // Google client to interact with Google API
    boolean locationEnabled;
    private GoogleApiClient mGoogleApiClient;
    private final static int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    boolean mRequestingLocationUpdates;

    public static final int FRAME_INFO_DIALOG = 1;
    public static final int EDIT_FRAME_INFO_DIALOG = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        rollId = getArguments().getInt("ROLL_ID");
        locationEnabled = getArguments().getBoolean("LOCATION_ENABLED");

        database = new FilmDbHelper(getActivity());
        mFrameClassList = database.getAllFramesFromRoll(rollId);
        camera_id = database.getRoll(rollId).getCamera_id();

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
            mLocationRequest.setInterval(20000);
            mLocationRequest.setFastestInterval(10000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
        // This can be done anyway. It only has effect if locationEnabled is true.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        mRequestingLocationUpdates = prefs.getBoolean("GPSUpdate", true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        final View view = linf.inflate(R.layout.frames_fragment, container, false);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(database.getRoll(rollId).getName());
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(this);

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

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(1);

        if (mFrameClassList.size() >= 1) {
            counter = mFrameClassList.get(mFrameClassList.size() - 1).getCount();
            mainTextView.setVisibility(View.GONE);
        }

        if (mainListView.getCount() >= 1) mainListView.setSelection(mainListView.getCount() - 1);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        MenuItem shareItem = menu.add(Menu.NONE, 98, Menu.NONE, R.string.Share);

        if (shareItem != null) {
            mShareActionProvider = new ShareActionProvider(getActivity());
            mShareActionProvider.setShareIntent(setShareIntent());
        }
        MenuItemCompat.setActionProvider(shareItem, mShareActionProvider);
        // Create an Intent to share your content
        setShareIntent();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            case R.id.menu_item_delete:

                if (mFrameClassList.size() >= 1) {

                    // Ask the user which frame(s) to delete

                    ArrayList<String> listItems = new ArrayList<>();
                    for (int i = 0; i < mFrameClassList.size(); ++i) {
                        listItems.add(" #" + mFrameClassList.get(i).getCount() + "   " + mFrameClassList.get(i).getDate());
                        //            ^ trick to add integer to string
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                    final ArrayList<Integer> selectedItemsIndexList = new ArrayList<>();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.PickFramesToDelete)

                            // Multiple Choice Dialog
                            .setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                    if (isChecked) {
                                        // If the user checked the item, add it to the selected items
                                        selectedItemsIndexList.add(which);
                                    } else if (selectedItemsIndexList.contains(which)) {
                                        // Else, if the item is already in the array, remove it
                                        selectedItemsIndexList.remove(Integer.valueOf(which));
                                    }
                                }
                            })
                                    // Set the action buttons
                            .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                    // Do something with the selection
                                    Collections.sort(selectedItemsIndexList);
                                    for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                                        int which = selectedItemsIndexList.get(i);

                                        Frame frame = mFrameClassList.get(which);
                                        database.deleteFrame(frame);
                                        mFrameClassList.remove(which);


                                    }
                                    if (mFrameClassList.size() == 0)
                                        mainTextView.setVisibility(View.VISIBLE);
                                    mFrameAdapter.notifyDataSetChanged();
                                    if (mFrameClassList.size() >= 1)
                                        counter = mFrameClassList.get(mFrameClassList.size() - 1).getCount();
                                    else counter = 0;
                                    setShareIntent();

                                }
                            })
                            .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    // Do nothing
                                }
                            });

                    AlertDialog alert = builder.create();
                    alert.show();

                }

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

            case R.id.menu_item_about:

                AlertDialog.Builder aboutDialog = new AlertDialog.Builder(getActivity());
                aboutDialog.setTitle(R.string.app_name);
                aboutDialog.setMessage(R.string.about);

                aboutDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                aboutDialog.show();

                break;

            case R.id.menu_item_help:

                AlertDialog.Builder helpDialog = new AlertDialog.Builder(getActivity());
                helpDialog.setTitle(R.string.Help);
                helpDialog.setMessage(R.string.main_help);


                helpDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                helpDialog.show();

                break;

            case android.R.id.home:

                // INTERFACE TO MAINACTIVITY
                mCallback.onHomeAsUpPressed();

                break;

            // 'Show on map' menu item id is 99
            case R.id.menu_item_show_on_map:

                Intent intent2 = new Intent(getActivity(), MapsActivity.class);
                intent2.putExtra(ROLLINFO_EXTRA_MESSAGE, rollId);
                startActivity(intent2);
                break;

        }


        return true;
    }

    private Intent setShareIntent() {

        if (mShareActionProvider != null) {

            // create an Intent with the contents of the TextView
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Development");

            // Get the roll and its information
            Roll roll = database.getRoll(rollId);

            final String separator = ",";
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Roll name: " + roll.getName() + "\n");
            stringBuilder.append("Added: " + roll.getDate() + "\n");
            stringBuilder.append("Camera: " + database.getCamera(camera_id).getName() + "\n");
            stringBuilder.append("Notes: " + roll.getNote() + "\n");
            stringBuilder.append("Frame Count" + separator + "Date" + separator + "Lens" + separator + "Shutter" + separator + "Aperture" + separator + "Notes" + separator + "Location" + "\n");
            for (int i = 0; i < mFrameClassList.size(); ++i) {
                stringBuilder.append(mFrameClassList.get(i).getCount());
                stringBuilder.append(separator);
                stringBuilder.append(mFrameClassList.get(i).getDate());
                stringBuilder.append(separator);
                stringBuilder.append(mFrameClassList.get(i).getLens());
                stringBuilder.append(separator);
                if ( !mFrameClassList.get(i).getShutter().contains("<") ) stringBuilder.append(mFrameClassList.get(i).getShutter());
                stringBuilder.append(separator);
                if ( !mFrameClassList.get(i).getAperture().contains("<") ) stringBuilder.append("f" + mFrameClassList.get(i).getAperture());
                stringBuilder.append(separator);
                stringBuilder.append(mFrameClassList.get(i).getNote());
                stringBuilder.append(separator);
                stringBuilder.append(mFrameClassList.get(i).getLocation());
                stringBuilder.append("\n");
            }
            String shared = stringBuilder.toString();

            shareIntent.putExtra(Intent.EXTRA_TEXT, shared);

            // Make sure the provider knows
            // it should work with that Intent
            return shareIntent;
        }else {
            return new Intent();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:

                showFrameInfoDialog();

                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Edit frame info
        int _id = mFrameClassList.get(position).getId();
        String lens = mFrameClassList.get(position).getLens();
        int count = mFrameClassList.get(position).getCount();
        String date = mFrameClassList.get(position).getDate();
        String shutter = mFrameClassList.get(position).getShutter();
        String aperture = mFrameClassList.get(position).getAperture();
        String note = mFrameClassList.get(position).getNote();
        String location = mFrameClassList.get(position).getLocation();

        EditFrameInfoDialog dialog = EditFrameInfoDialog.newInstance(_id, lens, position, count, date, shutter, aperture, note, location, camera_id);
        dialog.setTargetFragment(this, EDIT_FRAME_INFO_DIALOG);
        dialog.show(getFragmentManager().beginTransaction(), EditFrameInfoDialog.TAG);
    }

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

        String lens;
        int count;
        String date = getCurrentTime();
        String shutter;
        String aperture;
        String location = locationStringFromLocation(mLastLocation);
        if (!mFrameClassList.isEmpty()) {
            Frame previousFrame = mFrameClassList.get(mFrameClassList.size() - 1);
            lens = previousFrame.getLens();
            count = previousFrame.getCount() + 1;
            shutter = previousFrame.getShutter();
            aperture = previousFrame.getAperture();
        } else {
            lens = getResources().getString(R.string.NoLens);
            count = 1;
            shutter = getResources().getString(R.string.NoValue);
            aperture = getResources().getString(R.string.NoValue);
        }

        FrameInfoDialog dialog = FrameInfoDialog.newInstance(lens, count, date, shutter, aperture, location, camera_id);
        dialog.setTargetFragment(this, FRAME_INFO_DIALOG);
        dialog.show(getFragmentManager(), FrameInfoDialog.TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case FRAME_INFO_DIALOG:

                if ( resultCode == Activity.RESULT_OK ) {

                    int count = data.getIntExtra("COUNT", -1);
                    String date = data.getStringExtra("DATE");
                    String lens = data.getStringExtra("LENS");
                    String shutter = data.getStringExtra("SHUTTER");
                    String aperture = data.getStringExtra("APERTURE");
                    String note = data.getStringExtra("NOTE");
                    String location = data.getStringExtra("LOCATION");

                    if (!checkReservedChars(note)) return;

                    if ( count != -1 ) {

                        Frame frame = new Frame(rollId, count, date, lens, shutter, aperture, note, location);

                        // Save the file when the new frame has been added
                        database.addFrame(frame);

                        // When we get the last added frame from the database we get the row id value.
                        frame = database.getLastFrame();

                        mFrameClassList.add(frame);
                        mFrameAdapter.notifyDataSetChanged();
                        mainTextView.setVisibility(View.GONE);

                        // When the new frame is added jump to view the last entry
                        mainListView.setSelection(mainListView.getCount() - 1);
                        // The text you'd like to share has changed,
                        // and you need to update
                        setShareIntent();
                    }
                } else if ( resultCode == Activity.RESULT_CANCELED ) {
                    // After cancel do nothing
                    return;
                }
                break;

            case EDIT_FRAME_INFO_DIALOG:

                if ( resultCode == Activity.RESULT_OK ) {

                    int _id = data.getIntExtra("ID", -1);
                    int count = data.getIntExtra("COUNT", -1);
                    int position = data.getIntExtra("POSITION", -1);
                    String date = data.getStringExtra("DATE");
                    String lens = data.getStringExtra("LENS");
                    String shutter = data.getStringExtra("SHUTTER");
                    String aperture = data.getStringExtra("APERTURE");
                    String note = data.getStringExtra("NOTE");
                    String location = data.getStringExtra("LOCATION");

                    if (!checkReservedChars(note)) return;

                    if ( _id != -1 && count != -1 && position != -1 ) {

                        Frame frame = new Frame();
                        frame.setId(_id);
                        frame.setRoll(rollId);
                        frame.setLens(lens);
                        frame.setCount(count);
                        frame.setDate(date);
                        frame.setShutter(shutter);
                        frame.setAperture(aperture);
                        frame.setNote(note);
                        frame.setLocation(location);
                        database.updateFrame(frame);
                        //Make the change in the class list and the list view
                        mFrameClassList.get(position).setLens(lens);
                        mFrameClassList.get(position).setCount(count);
                        mFrameClassList.get(position).setDate(date);
                        mFrameClassList.get(position).setShutter(shutter);
                        mFrameClassList.get(position).setAperture(aperture);
                        mFrameClassList.get(position).setNote(note);
                        mFrameClassList.get(position).setLocation(location);
                        mFrameAdapter.notifyDataSetChanged();
                        setShareIntent();
                    }
                } else if ( resultCode == Activity.RESULT_CANCELED ) {
                    // After cancel do nothing
                    return;
                }
                break;

        }
    }





    public static String locationStringFromLocation(final Location location) {
        if (location != null)
            return (Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + " " + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES)).replace(",", ".");
        else return "";
    }

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

    public void onStart() {
        if ( locationEnabled ) mGoogleApiClient.connect();
        super.onStart();
    }

    public void onStop() {
        if ( locationEnabled ) mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if ( locationEnabled ) stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

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
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        else LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

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

    @Override
    public void onConnectionSuspended(int i) {
        if ( locationEnabled ) mGoogleApiClient.connect();
    }

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

    private boolean checkReservedChars(String input){
        //Check if there are illegal character in the input string
        String ReservedChars = "|\\?*<\":>/";
        for ( int i = 0; i < input.length(); ++i ) {
            Character c = input.charAt(i);
            if ( ReservedChars.contains(c.toString()) ) {
                Toast toast = Toast.makeText(getActivity(), R.string.NoteIllegalCharacter + c.toString(), Toast.LENGTH_LONG);
                toast.show();
                return false;
            }
        }
        return true;
    }
}
