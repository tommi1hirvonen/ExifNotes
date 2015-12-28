package com.tommihirvonen.filmphotonotes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

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
        FrameInfoDialog.onInfoSetCallback, EditFrameInfoDialog.OnEditSettedCallback {

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

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle(name_of_roll);
        getSupportActionBar().setSubtitle(R.string.Frames);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mainTextView = (TextView) findViewById(R.id.no_added_frames);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.frames_listview);
        // Create an ArrayAdapter for the ListView
        mFrameAdapter = new FrameAdapter(this,android.R.layout.simple_list_item_1, mFrameClassList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mFrameAdapter);

        mainListView.setOnItemClickListener(this);

        // Read the frames from file and add to list
        File file = new File(getFilesDir(), name_of_roll + ".txt");
        if ( file.exists() ) readFrameFile();

        if ( mFrameClassList.size() >= 1 ) counter = mFrameClassList.get(mFrameClassList.size() -1).getCount();

        if ( mainListView.getCount() >= 1 ) mainListView.setSelection( mainListView.getCount() - 1 );

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_roll_info, menu);

        // Access the Share Item defined in menu XML
        MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        MenuItem addFrame = menu.findItem(R.id.menu_item_add_frame);
        MenuItem deleteFrame = menu.findItem(R.id.menu_item_delete_frame);
        MenuItem frame_help = menu.findItem(R.id.menu_item_frame_help);


        // Access the object responsible for
        // putting together the sharing submenu


        addFrame.setOnMenuItemClickListener(this);
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
           case R.id.menu_item_add_frame:

               SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
               boolean showLensSelection = prefs.getBoolean("AlwaysShowLensSelection", true);

               // If AlwaysShowLensSelection is true then show frame info dialog.
               // Otherwise assume same lens is used.
               if ( showLensSelection ) {
                   show_frame_info_dialog();
               }
               else {

                   // If the added frame is the first on, then ask the user for the used lens
                   if ( mFrameClassList.isEmpty() ) show_frame_info_dialog();
                   // Otherwise assume the same lens is used
                   else {

                       String lens = mFrameClassList.get(mFrameClassList.size() - 1).getLens();

                       // Dateformat doesn't seem to work for some reason. This is a workaround.
                       // There is another instance of this same operation in onInfoSet
                       final Calendar c = Calendar.getInstance();
                       int iYear = c.get(Calendar.YEAR);
                       int iMonth = c.get(Calendar.MONTH) + 1;
                       int iDay = c.get(Calendar.DAY_OF_MONTH);
                       int iHour = c.get(Calendar.HOUR_OF_DAY);
                       int iMin = c.get(Calendar.MINUTE);
                       String current_time = iYear + "-" + iMonth + "-" + iDay + " " + iHour + ":" + iMin;

                       ++counter;
                       mainTextView.setVisibility(View.GONE);

                       // ****************************************************************************
                       // ******************** Implement these two in this method ********************
                       // ****************************************************************************
                       String shutter = "ka";
                       String aperture = "pow";

                       Frame frame = new Frame(counter, current_time, lens, shutter, aperture);
                       mFrameClassList.add(frame);
                       mFrameAdapter.notifyDataSetChanged();

                       // When the new frame is added jump to view the last entry
                       mainListView.setSelection(mainListView.getCount() - 1 );
                       // The text you'd like to share has changed,
                       // and you need to update
                       setShareIntent();

                       // Save the file when the new frame has been added
                       writeFrameFile(frame.getCount() + "," + frame.getDate() + "," + frame.getLens() + "," + frame.getShutter() + "," + frame.getAperture());

                   }
               }
               break;

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

                    // List Dialog
//                    builder.setItems(items, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//
//                            // Do something with the selection
//                            mFrameClassList.remove(which);
//                            //mFrameList.remove(0);
//                            if ( mFrameClassList.size() == 0 ) mainTextView.setVisibility(View.VISIBLE);
//                            //mArrayAdapter.notifyDataSetChanged();
//                            mFrameAdapter.notifyDataSetChanged();
//
//                            File file = new File(getFilesDir(), name_of_roll + ".txt");
//                            try {
//                                MainActivity.removeLine(file, which);
//                            }
//                            catch (IOException e){
//                                e.printStackTrace();
//                            }
//
//                            if ( mFrameClassList.size() >= 1 ) counter = mFrameClassList.get(mFrameClassList.size() -1).getCount();
//                            setShareIntent();
//
//                        }
//                    });

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
                                mFrameClassList.remove(which);

                                File file = new File(getFilesDir(), name_of_roll + ".txt");
                                try {
                                    MainActivity.removeLine(file, which);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (mFrameClassList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                            //mArrayAdapter.notifyDataSetChanged();
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
        String lens = mFrameClassList.get(position).getLens();
        int count = mFrameClassList.get(position).getCount();
        String date = mFrameClassList.get(position).getDate();

        // ***************************************************************************************
        // ******************** Implement shutter and aperture in this method ********************
        // ***************************************************************************************

        ArrayList<String> mLensList;
        mLensList = readLensFile();


        EditFrameInfoDialog dialog = EditFrameInfoDialog.newInstance(lens, position, count, date, mLensList);
        dialog.show(getSupportFragmentManager(), EditFrameInfoDialog.TAG);
    }



    // METHODS TO WRITE AND READ THE FRAMES FILE

    private void writeFrameFile(String input) {
        try {
            File file = new File(getFilesDir(), name_of_roll + ".txt");
            FileWriter writer = new FileWriter(file, true);
            writer.write(input + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // This method reads in the roll info file to create the in-app database of all the frames
    // taken with this roll
    private void readFrameFile() {
        //Get the text file
        File file = new File(getFilesDir(), name_of_roll + ".txt");

        //Read text from file
        //StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
//                text.append(line);
//                text.append('\n');
                ++counter;

                List<String> new_frame_strings = Arrays.asList(line.split(","));
                Frame frame = new Frame(Integer.parseInt(new_frame_strings.get(0)), new_frame_strings.get(1), new_frame_strings.get(2), new_frame_strings.get(3), new_frame_strings.get(4));
                mFrameClassList.add(frame);

                //mFrameList.add(line);
                mainTextView.setVisibility(View.GONE);
                mFrameAdapter.notifyDataSetChanged();
            }
            br.close();

            //mArrayAdapter.notifyDataSetChanged();
            setShareIntent();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //return text.toString();
    }


    private void show_frame_info_dialog() {
        ArrayList<String> mLensList;
        mLensList = readLensFile();
        FrameInfoDialog dialog = FrameInfoDialog.newInstance("" + R.string.NewFrame, mLensList);
        dialog.show(getSupportFragmentManager(), FrameInfoDialog.TAG);
    }

    @Override
    public void onInfoSet(String lens) {

        // Grab the EditText's input
        if ( lens.length() != 0 ) {


            // Add new frame

            // Dateformat doesn't seem to work for some reason. This is a workaround.
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

            // Update counter in case the previous frame's counter was edited
            if (mFrameClassList.size() >= 1) counter = mFrameClassList.get(mFrameClassList.size()-1).getCount();

            ++counter;
            mainTextView.setVisibility(View.GONE);

            // ****************************************************************************
            // ******************** Implement these two in this method ********************
            // ****************************************************************************
            String shutter = "ka";
            String aperture = "pow";

            Frame frame = new Frame(counter, current_time, lens, shutter, aperture);
            mFrameClassList.add(frame);
            mFrameAdapter.notifyDataSetChanged();


            // When the new frame is added jump to view the last entry
            mainListView.setSelection(mainListView.getCount() - 1 );
            // The text you'd like to share has changed,
            // and you need to update
            setShareIntent();

            // Save the file when the new frame has been added
            writeFrameFile(frame.getCount() + "," + frame.getDate() + "," + frame.getLens() + "," + frame.getShutter() + "," + frame.getAperture());


        }

    }



    @Override
    public void onEditSetted(String lens, int position, int count, String date) {
        if ( lens.length() != 0 ) {

            // Replace the old lens in the text file with the new one
            try {
                String old_line = mFrameClassList.get(position).getCount() + "," + mFrameClassList.get(position).getDate() + "," + mFrameClassList.get(position).getLens();
                String new_line = count + "," + date + "," + lens;
                updateLine(old_line, new_line);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            //Make the change in the class list and the list view
            mFrameClassList.get(position).setLens(lens);
            mFrameClassList.get(position).setCount(count);
            mFrameClassList.get(position).setDate(date);
            mFrameAdapter.notifyDataSetChanged();
            setShareIntent();

        }
    }

    // This method updates a frame's information if it is edited
    private void updateLine(String toUpdate, String updated) throws IOException {
        File new_file = new File(getFilesDir(), name_of_roll + ".txt");
        BufferedReader file = new BufferedReader(new FileReader(new_file));
        String line;
        String input = "";

        while ((line = file.readLine()) != null)
            input += line + "\n";

        input = input.replace(toUpdate, updated);

        FileOutputStream os = new FileOutputStream(new_file);
        os.write(input.getBytes());

        file.close();
        os.close();
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


}
