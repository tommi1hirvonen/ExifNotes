package com.tommihirvonen.filmphotonotes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class RollInfo extends AppCompatActivity implements AdapterView.OnItemClickListener, MenuItem.OnMenuItemClickListener,
        FrameInfoDialog.onInfoSetCallback, EditFrameInfoDialog.OnEditSetCallback, FloatingActionButton.OnClickListener {

    FilmDbHelper database;
    TextView mainTextView;
    ListView mainListView;
    ArrayList<Frame> mFrameClassList = new ArrayList<>();
    FrameAdapter mFrameAdapter;
    ShareActionProvider mShareActionProvider;
    String name_of_roll;
    int counter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roll_info);
        Intent intent = getIntent();
        name_of_roll = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        database = new FilmDbHelper(this);
        mFrameClassList = database.getAllFramesFromRoll(name_of_roll);

        // ********** Commands to get the action bar and color it **********
        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle(name_of_roll);
        getSupportActionBar().setSubtitle(R.string.Frames);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(primaryColor)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor( Color.parseColor(secondaryColor) );
        }
        // *****************************************************************

        mainTextView = (TextView) findViewById(R.id.no_added_frames);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.frames_listview);
        // Create an ArrayAdapter for the ListView
        mFrameAdapter = new FrameAdapter(this,android.R.layout.simple_list_item_1, mFrameClassList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mFrameAdapter);

        mainListView.setOnItemClickListener(this);

        if ( mFrameClassList.size() >= 1 ) {
            counter = mFrameClassList.get(mFrameClassList.size() -1).getCount();
            mainTextView.setVisibility(View.GONE);
        }

        if ( mainListView.getCount() >= 1 ) mainListView.setSelection( mainListView.getCount() - 1 );
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_roll_info, menu);

        // Access the Share Item defined in menu XML
        MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        MenuItem deleteFrame = menu.findItem(R.id.menu_item_delete_frame);
        MenuItem frame_help = menu.findItem(R.id.menu_item_frame_help);

        deleteFrame.setOnMenuItemClickListener(this);
        frame_help.setOnMenuItemClickListener(this);

        if (shareItem != null) {
           mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        }

        // Create an Intent to share your content
        setShareIntent();
        return true;
    }

    private void setShareIntent() {

        if (mShareActionProvider != null) {

            // create an Intent with the contents of the TextView
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Development");

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Frame Count;Date;Lens;Shutter;Aperture" + "\n");
            for ( int i = 0; i < mFrameClassList.size(); ++i ) {
                stringBuilder.append(mFrameClassList.get(i).getCount());
                stringBuilder.append(";");
                stringBuilder.append(mFrameClassList.get(i).getDate());
                stringBuilder.append(";");
                stringBuilder.append(mFrameClassList.get(i).getLens());
                stringBuilder.append(";");
                stringBuilder.append(mFrameClassList.get(i).getShutter());
                stringBuilder.append(";");
                stringBuilder.append(mFrameClassList.get(i).getAperture());
                stringBuilder.append("\n");
            }
            String shared = stringBuilder.toString();

            shareIntent.putExtra(Intent.EXTRA_TEXT, shared);

            // Make sure the provider knows
            // it should work with that Intent
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_item_delete_frame:
                if ( mFrameClassList.size() >= 1 ) {

                    // Ask the user which frame(s) to delete

                    ArrayList<String> listItems = new ArrayList<>();
                    for ( int i = 0; i < mFrameClassList.size(); ++i ) {
                        listItems.add(" #" + mFrameClassList.get(i).getCount() + "   " + mFrameClassList.get(i).getDate());
                        //            ^ trick to add integer to string
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                    final ArrayList<Integer> selectedItemsIndexList = new ArrayList<>();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                            if (mFrameClassList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                            mFrameAdapter.notifyDataSetChanged();
                            if (mFrameClassList.size() >= 1) counter = mFrameClassList.get(mFrameClassList.size() - 1).getCount();
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


            case R.id.menu_item_frame_help:

                AlertDialog.Builder helpDialog = new AlertDialog.Builder(this);
                helpDialog.setTitle(R.string.Help);
                helpDialog.setMessage(R.string.frame_help);
                //helpDialog.setIcon(R.mipmap.film_photo_notes_icon);
                helpDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
                    }
                });
                helpDialog.show();
                break;
        }

        return true;
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

        ArrayList<String> mLensList;
        mLensList = readLensFile();

        EditFrameInfoDialog dialog = EditFrameInfoDialog.newInstance(_id, lens, position, count, date, shutter, aperture, mLensList);
        dialog.show(getSupportFragmentManager(), EditFrameInfoDialog.TAG);
    }




    private void showFrameInfoDialog() {

        // If the frame count is greater than 100, then don't add a new frame.
        if ( !mFrameClassList.isEmpty()) {
            int countCheck = mFrameClassList.get(mFrameClassList.size() - 1).getCount() + 1;
            if (countCheck > 100) {
                Toast toast = Toast.makeText(this, getResources().getString(R.string.TooManyFrames), Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        }

        String lens;
        int count;
        String date = getCurrentTime();
        String shutter;
        String aperture;
        if ( !mFrameClassList.isEmpty() ){
            Frame previousFrame = mFrameClassList.get(mFrameClassList.size()-1);
            lens = previousFrame.getLens();
            count = previousFrame.getCount() + 1;
            shutter = previousFrame.getShutter();
            aperture = previousFrame.getAperture();
        } else {
            lens = getResources().getString(R.string.NoLens);
            count = 1;
            shutter = "<empty>";
            aperture = "<empty>";
        }
        ArrayList<String> mLensList;
        mLensList = readLensFile();
        FrameInfoDialog dialog = FrameInfoDialog.newInstance(lens, count, date, shutter, aperture,  mLensList);
        dialog.show(getSupportFragmentManager(), FrameInfoDialog.TAG);
    }

    @Override
    public void onInfoSet(String lens, int count, String date, String shutter, String aperture) {

        Frame frame = new Frame(name_of_roll, count, date, lens, shutter, aperture);


        // Save the file when the new frame has been added
        database.addFrame(frame);

        // When we get the last added frame from the database we get the row id value.
        frame = database.getLastFrame();

        mFrameClassList.add(frame);
        mFrameAdapter.notifyDataSetChanged();
        mainTextView.setVisibility(View.GONE);

        // When the new frame is added jump to view the last entry
        mainListView.setSelection(mainListView.getCount() - 1 );
        // The text you'd like to share has changed,
        // and you need to update
        setShareIntent();

    }



    @Override
    public void onEditSet(int _id, String lens, int position, int count, String date, String shutter, String aperture) {
        if ( lens.length() != 0 ) {

            Frame frame = new Frame();
            frame.setId(_id); frame.setRoll(name_of_roll); frame.setLens(lens); frame.setCount(count);
            frame.setDate(date); frame.setShutter(shutter); frame.setAperture(aperture);
            database.updateFrame(frame);
            //Make the change in the class list and the list view
            mFrameClassList.get(position).setLens(lens);
            mFrameClassList.get(position).setCount(count);
            mFrameClassList.get(position).setDate(date);
            mFrameClassList.get(position).setShutter(shutter);
            mFrameClassList.get(position).setAperture(aperture);
            mFrameAdapter.notifyDataSetChanged();
            setShareIntent();
        }
    }


    private ArrayList<String> readLensFile () {
        ArrayList<String> lenses = new ArrayList<>();
        File file = new File(getFilesDir(), "List_of_Lenses.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ( (line = br.readLine()) != null ) {
                lenses.add(line);
            }
            br.close();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        return lenses;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:

                showFrameInfoDialog();

                break;
        }
    }


    private String getCurrentTime(){
        final Calendar c = Calendar.getInstance();
        int iYear = c.get(Calendar.YEAR);
        int iMonth = c.get(Calendar.MONTH) + 1;
        int iDay = c.get(Calendar.DAY_OF_MONTH);
        int iHour = c.get(Calendar.HOUR_OF_DAY);
        int iMin = c.get(Calendar.MINUTE);
        String current_time;
        if ( iMin < 10 ) {
            current_time = iYear + "-" + iMonth + "-" + iDay + " " + iHour + ":0" + iMin;
        }
        else current_time = iYear + "-" + iMonth + "-" + iDay + " " + iHour + ":" + iMin;
        return current_time;
    }
}
