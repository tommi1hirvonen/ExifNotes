package com.tommihirvonen.filmphotonotes;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
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

public class LensesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        MenuItem.OnMenuItemClickListener, LensNameDialog.onLensNameSetCallback, FloatingActionButton.OnClickListener {

    TextView mainTextView;

    ListView mainListView;

    LensAdapter mArrayAdapter;

    ArrayList<Lens> mLensList = new ArrayList<>();

    FilmDbHelper database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new FilmDbHelper(this);
        mLensList = database.getAllLenses();

        setContentView(R.layout.activity_lenses);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // ********** Commands to get the action bar and color it **********
        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle(R.string.Lenses);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor( Color.parseColor(secondaryColor) );
        }
        // *****************************************************************

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        mainTextView = (TextView) findViewById(R.id.no_added_lenses);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.main_lenseslistview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new LensAdapter(this, android.R.layout.simple_list_item_1, mLensList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        if ( mLensList.size() >= 1 ) mainTextView.setVisibility(View.GONE);

        mArrayAdapter.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_lenses, menu);

        MenuItem deleteLens = menu.findItem(R.id.menu_item_delete_lens);

        deleteLens.setOnMenuItemClickListener(this);

        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_item_delete_lens:

                // Only delete if there are more than one lens
                if (mLensList.size() >= 1) {

                    // Ask the user which lens to delete

                    List<String> listItems = new ArrayList<>();
                    for ( int i = 0; i < mLensList.size(); ++i) {
                        listItems.add(mLensList.get(i).getName());
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    // MULTIPLE CHOICE DIALOG
                    final ArrayList<Integer> selectedItemsIndexList = new ArrayList<>();
                    builder.setTitle(R.string.PickLensesToDelete)
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

                                    Lens lens = mLensList.get(which);
                                    database.deleteLens(lens);

                                    // Remove the roll from the mLensList. Do this last!!!
                                    mLensList.remove(which);
                                }
                                if (mLensList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
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

        }

        return true;
    }

    private void showLensNameDialog() {
        LensNameDialog dialog = new LensNameDialog();
        dialog.show(getSupportFragmentManager(), LensNameDialog.TAG);
    }

    @Override
    public void onLensNameSet(String inputText) {
        if(inputText.length() != 0) {

            if ( inputText.length() != 0 ) {

                // Check if a lens with the same name already exists
                for ( int i = 0; i < mLensList.size(); ++i ) {
                    if ( inputText.equals( mLensList.get(i).getName())  ) {
                        Toast toast = Toast.makeText(getApplicationContext(), R.string.LensSameName, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return;
                    }
                }

                //Check if there are illegal character in the lens name
                String ReservedChars = "|\\?*<\":>/";
                for ( int i = 0; i < inputText.length(); ++i ) {
                    Character c = inputText.charAt(i);
                    if ( ReservedChars.contains(c.toString()) ) {
                        Toast toast = Toast.makeText(getApplicationContext(), R.string.LensIllegalCharacter + c.toString(), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return;
                    }
                }

                mainTextView.setVisibility(View.GONE);

                Lens lens = new Lens();
                lens.setName(inputText);
                database.addLens(lens);
                // When we get the last added lens from the database we get the row id value.
                lens = database.getLastLens();
                mLensList.add(lens);
                mArrayAdapter.notifyDataSetChanged();

                // When the lens is added jump to view the last entry
                mainListView.setSelection(mainListView.getCount() - 1);
            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                showLensNameDialog();
                break;
        }
    }
}
