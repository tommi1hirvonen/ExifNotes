package com.tommihirvonen.filmphotonotes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements
        //View.OnClickListener,
        AdapterView.OnItemClickListener, MenuItem.OnMenuItemClickListener, set_custom_dialog.OnNameSettedCallback {

    public final static String EXTRA_MESSAGE = "com.tommihirvonen.filmphotonotes.MESSAGE";
    public static final String TAG = "MainActivity";

    TextView mainTextView;
    //Button mainButton;
    //EditText mainEditText;

    ListView mainListView;
    ArrayAdapter mArrayAdapter;
    ArrayList mNameList = new ArrayList();

    //ShareActionProvider mShareActionProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getSupportActionBar().setTitle("Hello world App");
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle(" Film Photo Notes");
        getSupportActionBar().setSubtitle(" Rolls");
        getSupportActionBar().setIcon(R.mipmap.film_photo_notes_icon);

        // Access the Button defined in layout XML
        // and listen for it here
        //mainButton = (Button) findViewById(R.id.main_button);
        //mainButton.setOnClickListener(this);

        // Access the EditText defined in layout XML
        //mainEditText = (EditText) findViewById(R.id.main_edittext);

        mainTextView = (TextView) findViewById(R.id.no_added_rolls);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.main_listview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,mNameList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        //mainTextView.setVisibility(View.GONE);

//        String auto_add_roll = readFromFile();
//        if ( auto_add_roll.charAt(auto_add_roll.length()-1) == '\n' ) auto_add_roll.substring(0, auto_add_roll.length() -1);
//        mNameList.add(readFromFile());
//        mArrayAdapter.notifyDataSetChanged();
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        // Access the Share Item defined in menu XML
        //MenuItem shareItem = menu.findItem(R.id.menu_item_share);

        MenuItem addRoll = menu.findItem(R.id.menu_item_add_roll);

        // Access the object responsible for
        // putting together the sharing submenu
//        if (shareItem != null) {
//            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
//        }

        addRoll.setOnMenuItemClickListener(this);


        // Create an Intent to share your content
        //setShareIntent();

        return true;
    }





//    private void setShareIntent() {
//
//        if (mShareActionProvider != null) {
//
//            // create an Intent with the contents of the TextView
//            Intent shareIntent = new Intent(Intent.ACTION_SEND);
//            shareIntent.setType("text/plain");
//            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Development");
//            if ( mNameList.size() != 0 ) shareIntent.putExtra(Intent.EXTRA_TEXT, mNameList.get(0).toString());
//
//            // Make sure the provider knows
//            // it should work with that Intent
//            mShareActionProvider.setShareIntent(shareIntent);
//        }
//    }





//    @Override
//    public void onClick(View v) {
//        AskForNameOfRoll();
//    }





    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Log the item's position and contents
        // to the console in Debug
        Log.d("omg android", position + ": " + mNameList.get(position));

        Intent intent = new Intent(this, Roll_Info.class);
        intent.putExtra(EXTRA_MESSAGE, mNameList.get(position).toString());
        startActivity(intent);
    }




//    public void AskForNameOfRoll() {
        // **********TRADITIONAL METHOD **********
//        // otherwise, show a dialog to ask for their name
//        AlertDialog.Builder alert = new AlertDialog.Builder(this);
//
//        alert.setTitle("Name of new roll");
//        //alert.setMessage("Give the new roll a name!");
//
//        // Create EditText for entry
//        final EditText input = new EditText(this);
//        input.setHint("Roll name");
//
//        alert.setView(input);
//
//        // Make an "OK" button to save the name
//        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
//
//            public void onClick(DialogInterface dialog, int whichButton) {
//
//                // Grab the EditText's input
//                String inputName = input.getText().toString();
//
//                if ( inputName.length() != 0 ) {
//
//                    for ( int i = 0; i < mNameList.size(); ++i ) {
//                        if ( inputName.equals( mNameList.get(i).toString() )  ) {
//                            Toast.makeText(getApplicationContext(), "ROLL WITH SAME NAME ALREADY EXISTS", Toast.LENGTH_LONG).show();
//                            return;
//                        }
//                    }
//
//                    mNameList.add(inputName);
//                    mArrayAdapter.notifyDataSetChanged();
//
//                    // The text you'd like to share has changed,
//                    // and you need to update
//                    setShareIntent();
//
//                }
//            }
//        });
//
//        // Make a "Cancel" button
//        // that simply dismisses the alert
//        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//
//            public void onClick(DialogInterface dialog, int whichButton) {}
//        });
//
//        alert.show();


        // **********CUSTOM DIALOG **********



//    }




    @Override
    public boolean onMenuItemClick(MenuItem item) {
        //AskForNameOfRoll();
        show_set_custom_dialog();
        return true;
    }

    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("List_of_Rolls.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    private String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = openFileInput("List_of_Rolls.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString).append("\n");
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    private void show_set_custom_dialog() {
        set_custom_dialog dialog = new set_custom_dialog();
        dialog.show(getSupportFragmentManager(), set_custom_dialog.TAG);
    }

    @Override
    public void onNameSetted(String inputText) {
        if(!TextUtils.isEmpty(inputText)) {
            // Grab the EditText's input
                String inputName = inputText;

                if ( inputName.length() != 0 ) {

                    for ( int i = 0; i < mNameList.size(); ++i ) {
                        if ( inputName.equals( mNameList.get(i).toString() )  ) {
                            Toast.makeText(getApplicationContext(), "ROLL WITH SAME NAME ALREADY EXISTS", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    mainTextView.setVisibility(View.GONE);
                    mNameList.add(inputName);
                    mArrayAdapter.notifyDataSetChanged();

                    // The text you'd like to share has changed,
                    // and you need to update
                    //setShareIntent();

                    //Save the file when the new roll has been added
                    //writeToFile(inputName);



                }
        }
    }
}

