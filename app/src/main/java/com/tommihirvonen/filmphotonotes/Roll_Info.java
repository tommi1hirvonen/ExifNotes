package com.tommihirvonen.filmphotonotes;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class Roll_Info extends ActionBarActivity
        implements  MenuItem.OnMenuItemClickListener
        {


    ListView mainListView;
    ArrayAdapter mArrayAdapter;
    ArrayList mFrameList = new ArrayList();
    ShareActionProvider mShareActionProvider;
    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roll_info);
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle(message);
        //getSupportActionBar().setIcon(R.mipmap.film_photo_notes_icon);


        // Access the ListView
        mainListView = (ListView) findViewById(R.id.frames_listview);
        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,mFrameList);
        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_roll_info, menu);

        // Access the Share Item defined in menu XML
        MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        MenuItem addRoll = menu.findItem(R.id.menu_item_add_frame);
        MenuItem deleteRoll = menu.findItem(R.id.menu_item_delete_roll);


        // Access the object responsible for
        // putting together the sharing submenu


        addRoll.setOnMenuItemClickListener(this);
        deleteRoll.setOnMenuItemClickListener(this);

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
                ++counter;
               mFrameList.add("MOI" + counter);
               mArrayAdapter.notifyDataSetChanged();
               setShareIntent();

                break;

            case R.id.menu_item_delete_roll:
                if ( mFrameList.size() >= 1 ) {
                    --counter;
                    mFrameList.remove(0);
                    mArrayAdapter.notifyDataSetChanged();
                    setShareIntent();
                }
                break;
        }


        return true;
    }
}
