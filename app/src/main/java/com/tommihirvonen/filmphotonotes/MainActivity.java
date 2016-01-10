package com.tommihirvonen.filmphotonotes;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class MainActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, MenuItem.OnMenuItemClickListener,
        RollNameDialog.onNameSetCallback, EditRollNameDialog.OnNameEditedCallback, FloatingActionButton.OnClickListener {

    public final static String EXTRA_MESSAGE = "com.tommihirvonen.filmphotonotes.MESSAGE";
    public final static String LOCATION_ENABLED_EXTRA = "LocationEnabled";

    TextView mainTextView;

    ListView mainListView;
    RollAdapter mArrayAdapter;
    ArrayList<Roll> mRollList = new ArrayList<>();
    FilmDbHelper database;

    private final static int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    boolean locationEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new FilmDbHelper(this);
        mRollList = database.getAllRolls();

        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        new SimpleEula(this).show();

        // ********** Commands to get the action bar and color it **********
        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle("  " + getResources().getString(R.string.MainActivityTitle));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor( Color.parseColor(secondaryColor) );
        }
        // *****************************************************************

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        mainTextView = (TextView) findViewById(R.id.no_added_rolls);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.main_listview);



        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new RollAdapter(this, android.R.layout.simple_list_item_1, mRollList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        if ( mRollList.size() >= 1 ) mainTextView.setVisibility(View.GONE);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        // Set this activity to react to list items being pressed and held
        mainListView.setOnItemLongClickListener(this);

        if ( mainListView.getCount() >= 1) mainListView.setSelection(mainListView.getCount() - 1);

        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        // Check if the app has location permission.
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationEnabled = true;

        }
        // It does not. Show dialog to request permission.
        else ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION);

        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if ( !isGPSEnabled ) showSettingsAlert();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem deleteRoll = menu.findItem(R.id.menu_item_delete_roll);
        MenuItem about = menu.findItem(R.id.menu_item_about);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        MenuItem lenses = menu.findItem(R.id.menu_item_lenses);
        MenuItem preferences = menu.findItem(R.id.menu_item_preferences);

        deleteRoll.setOnMenuItemClickListener(this);
        about.setOnMenuItemClickListener(this);
        help.setOnMenuItemClickListener(this);
        lenses.setOnMenuItemClickListener(this);
        preferences.setOnMenuItemClickListener(this);

        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        mArrayAdapter.notifyDataSetChanged();
    }



    @Override
    // Pressing the roll allows the user to show the frames taken with that roll
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent intent = new Intent(this, RollInfo.class);
        intent.putExtra(EXTRA_MESSAGE, mRollList.get(position).getId());
        intent.putExtra(LOCATION_ENABLED_EXTRA, locationEnabled);
        startActivity(intent);
    }

    @Override
    //Long pressing the roll allows the user to rename the roll
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        show_EditRollNameDialog(mRollList.get(position).getId(), mRollList.get(position).getName(), mRollList.get(position).getNote());

        //Return true because the item was pressed and held.
        return true;
    }





    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_item_delete_roll:
                //Only delete if there are more than one roll
                if ( mRollList.size() >= 1 ) {

                    // Ask the user which roll to delete

                    // LIST ITEMS DIALOG

                    List<String> listItems = new ArrayList<>();
                    for ( int i = 0; i < mRollList.size(); ++i ) {
                        listItems.add(mRollList.get(i).getName());
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    // MULTIPLE CHOICE DIALOG
                    final ArrayList<Integer> selectedItemsIndexList = new ArrayList<>();
                    builder.setTitle(R.string.PickRollsToDelete)
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

                                // Do something with the selections
                                Collections.sort(selectedItemsIndexList);
                                for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                                    int which = selectedItemsIndexList.get(i);

                                    // Delete all the frames from the frames database
                                    database.deleteAllFramesFromRoll(mRollList.get(which).getId());

                                    database.deleteRoll(mRollList.get(which));

                                    // Remove the roll from the mRollList. Do this last!!!
                                    mRollList.remove(which);
                                }
                                if (mRollList.size() == 0 ) mainTextView.setVisibility(View.VISIBLE);
                                mArrayAdapter.notifyDataSetChanged();

                            }
                        })
                        .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Do nothing
                            }
                        }
                    );

                    AlertDialog alert = builder.create();
                    alert.show();


                }
                break;

            case R.id.menu_item_lenses:
                Intent intent = new Intent(this, LensesActivity.class);
                startActivity(intent);

                break;
            case R.id.menu_item_preferences:

                Intent preferences_intent = new Intent(this, PreferenceActivity.class);
                // With these extras we can skip the headers in the preferences.
                preferences_intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, PreferenceFragment.class.getName() );
                preferences_intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );

                startActivity(preferences_intent);

                break;

            case R.id.menu_item_about:

                AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
                aboutDialog.setTitle(R.string.app_name);
                aboutDialog.setMessage(R.string.about);

                aboutDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                aboutDialog.show();

                break;

            case R.id.menu_item_help:

                AlertDialog.Builder helpDialog = new AlertDialog.Builder(this);
                helpDialog.setTitle(R.string.Help);
                helpDialog.setMessage(R.string.main_help);


                helpDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                helpDialog.show();

                break;
        }

        return true;
    }

    private void show_EditRollNameDialog(int rollId, String oldName, String oldNote){
        EditRollNameDialog dialog = new EditRollNameDialog();
        dialog.setOldName(rollId, oldName, oldNote);
        dialog.show(getSupportFragmentManager(), EditRollNameDialog.TAG);
    }

    private void show_RollNameDialog() {
        RollNameDialog dialog = new RollNameDialog();
        dialog.show(getSupportFragmentManager(), RollNameDialog.TAG);
    }

    @Override
    public void onNameSet(String inputName, String inputNote) {

        if ( inputName.length() != 0 ) {

            //Check if there are illegal character in the roll name
            String ReservedChars = "|\\?*<\":>/";
            for ( int i = 0; i < inputName.length(); ++i ) {
                Character c = inputName.charAt(i);
                if ( ReservedChars.contains(c.toString()) ) {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.RollIllegalCharacter + c.toString(), Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
            }

            Roll roll = new Roll();
            roll.setName(inputName);
            roll.setDate(RollInfo.getCurrentTime());
            roll.setNote(inputNote);
            database.addRoll(roll);
            roll = database.getLastRoll();

            mainTextView.setVisibility(View.GONE);
            mRollList.add(roll);
            mArrayAdapter.notifyDataSetChanged();

            // When the new roll is added jump to view the last entry
            mainListView.setSelection(mainListView.getCount() - 1);
        }

    }

    @Override
    public void OnNameEdited(int rollId, String newName, String newNote){

        if ( newName.length() != 0 ) {

            //Check if there are illegal character in the roll name
            String ReservedChars = "|\\?*<\":>/";
            for ( int i = 0; i < newName.length(); ++i ) {
                Character c = newName.charAt(i);
                if ( ReservedChars.contains(c.toString()) ) {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.RollIllegalCharacter + c.toString(), Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
            }

            // Change the string in mRollList
            int position = 0;
            for ( int i = 0; i < mRollList.size(); ++i) {
                if ( rollId == mRollList.get(i).getId() ) {
                    position = i;
                }
            }
            Roll roll = mRollList.get(position);
            roll.setName(newName);
            roll.setNote(newNote);
            database.updateRoll(roll);

            // Notify array adapter that the dataset has to be updated
            mArrayAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                show_RollNameDialog();
                break;
        }
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    locationEnabled = true;

                }
            }
        }
    }
}

