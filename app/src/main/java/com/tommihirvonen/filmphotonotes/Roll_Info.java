package com.tommihirvonen.filmphotonotes;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Struct;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


public class Roll_Info extends ActionBarActivity implements  MenuItem.OnMenuItemClickListener {

    TextView mainTextView;
    ListView mainListView;
    ArrayAdapter mArrayAdapter;
    ArrayList mFrameList = new ArrayList();
    ShareActionProvider mShareActionProvider;
    String name_of_roll;
    int counter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roll_info);
        Intent intent = getIntent();
        name_of_roll = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle(name_of_roll);
        getSupportActionBar().setSubtitle("Frames");
        //getSupportActionBar().setIcon(R.mipmap.film_photo_notes_icon);
        mainTextView = (TextView) findViewById(R.id.no_added_frames);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.frames_listview);
        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,mFrameList);
        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Read the frames from file and add to list
        File file = new File(getFilesDir(), name_of_roll + ".txt");
        if ( file.exists() ) readFrameFile();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_roll_info, menu);

        // Access the Share Item defined in menu XML
        MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        MenuItem addFrame = menu.findItem(R.id.menu_item_add_frame);
        MenuItem deleteFrame = menu.findItem(R.id.menu_item_delete_frame);


        // Access the object responsible for
        // putting together the sharing submenu


        addFrame.setOnMenuItemClickListener(this);
        deleteFrame.setOnMenuItemClickListener(this);

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
            if ( mFrameList.size() != 0 ) shareIntent.putExtra(Intent.EXTRA_TEXT, mFrameList.get(0).toString());

            // Make sure the provider knows
            // it should work with that Intent
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
           case R.id.menu_item_add_frame:

               SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
               String asGmt = df.format(Calendar.getInstance().getTime());

                ++counter;
               mainTextView.setVisibility(View.GONE);

               mFrameList.add(counter + ".         " + asGmt );
               mArrayAdapter.notifyDataSetChanged();

               // When the new frame is added jump to view the last entry
               mainListView.setSelection(mainListView.getCount() - 1 );
               // The text you'd like to share has changed,
               // and you need to update
               setShareIntent();

               // Save the file when the new frame has been added
               writeFrameFile(counter + ".         " + asGmt);

               break;

            case R.id.menu_item_delete_frame:
                if ( mFrameList.size() >= 1 ) {
                    //--counter;
                    mFrameList.remove(0);
                    if ( mFrameList.size() == 0 ) mainTextView.setVisibility(View.VISIBLE);
                    mArrayAdapter.notifyDataSetChanged();

                    File file = new File(getFilesDir(), name_of_roll + ".txt");
                    try {
                        MainActivity.removeLine(file, 0);
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }

                    setShareIntent();
                }
                break;
        }


        return true;
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
                mFrameList.add(line);
                mainTextView.setVisibility(View.GONE);
            }
            br.close();

            mArrayAdapter.notifyDataSetChanged();
            setShareIntent();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //return text.toString();
    }
}
