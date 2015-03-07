package com.tommihirvonen.filmphotonotes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class MainActivity extends ActionBarActivity implements
        //View.OnClickListener,
        AdapterView.OnItemClickListener, MenuItem.OnMenuItemClickListener, roll_name_dialog.OnNameSettedCallback {

    public final static String EXTRA_MESSAGE = "com.tommihirvonen.filmphotonotes.MESSAGE";
    public static final String TAG = "MainActivity";

    TextView mainTextView;
    //Button mainButton;
    //EditText mainEditText;

    ListView mainListView;
    //ArrayAdapter mArrayAdapter;
    RollAdapter mArrayAdapter;
    ArrayList<String> mNameList = new ArrayList<>();

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
        //mArrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,mNameList);
        mArrayAdapter = new RollAdapter(this, android.R.layout.simple_list_item_1, mNameList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);


        // Read the rolls from file and add to list
        File file = new File(getFilesDir(), "List_of_Rolls.txt");
        if ( file.exists() ) readRollFile();
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        // Access the Share Item defined in menu XML
        //MenuItem shareItem = menu.findItem(R.id.menu_item_share);

        MenuItem addRoll = menu.findItem(R.id.menu_item_add_roll);
        MenuItem deleteRoll = menu.findItem(R.id.menu_item_delete_roll);

        // Access the object responsible for
        // putting together the sharing submenu
//        if (shareItem != null) {
//            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
//        }

        addRoll.setOnMenuItemClickListener(this);
        deleteRoll.setOnMenuItemClickListener(this);


        // Create an Intent to share your content
        //setShareIntent();

        return true;
    }





    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Log the item's position and contents
        // to the console in Debug
        Log.d("FilmPhotoNotes", position + ": " + mNameList.get(position));

        Intent intent = new Intent(this, Roll_Info.class);
        intent.putExtra(EXTRA_MESSAGE, mNameList.get(position).toString());
        startActivity(intent);
    }





    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_add_roll:
                //AskForNameOfRoll();
                show_roll_name_dialog();
                break;
            case R.id.menu_item_delete_roll:
                //Only delete if there are more than one roll
                if ( mNameList.size() >= 1 ) {

                    // Ask the user which roll to delete
//
                    List<String> listItems = new ArrayList<String>();
                    for ( int i = 0; i < mNameList.size(); ++i ) {
                        listItems.add(mNameList.get(i).toString());
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Pick a roll to delete");
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {

                            // Do something with the selection

                            // If the frames file exists, delete it too
                            File frames_file = new File(getFilesDir(), mNameList.get(item).toString() + ".txt");
                            if ( frames_file.exists() ) {
                                try {
                                    boolean delete = frames_file.delete();
                                } catch (Exception e) {
                                    Log.e("App", "Exception while deleting file " + e.getMessage());
                                }
                            }

                            mNameList.remove(item);

                            if (mNameList.size() == 0 ) mainTextView.setVisibility(View.VISIBLE);
                            mArrayAdapter.notifyDataSetChanged();

                            File file = new File(getFilesDir(), "List_of_Rolls.txt");
                            try {
                                removeLine(file, item);
                            }
                            catch (IOException e){
                                e.printStackTrace();
                            }


                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();




                }
                break;
        }

        return true;
    }



    private void show_roll_name_dialog() {
        roll_name_dialog dialog = new roll_name_dialog();
        dialog.show(getSupportFragmentManager(), roll_name_dialog.TAG);
    }

    @Override
    public void onNameSetted(String inputText) {
        if(!TextUtils.isEmpty(inputText)) {
            // Grab the EditText's input
                String inputName = inputText;

                if ( inputName.length() != 0 ) {

                    //Check if a roll with the same name already exists
                    for ( int i = 0; i < mNameList.size(); ++i ) {
                        if ( inputName.equals( mNameList.get(i).toString() )  ) {
                            Toast toast = Toast.makeText(getApplicationContext(), "Roll with same name already exists!", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return;
                        }
                    }

                    //Check if there are illegal character in the roll name


                    mainTextView.setVisibility(View.GONE);
                    mNameList.add(inputName);
                    mArrayAdapter.notifyDataSetChanged();

                    // When the new roll is added jump to view the last entry
                    mainListView.setSelection(mainListView.getCount() - 1 );

                    // The text you'd like to share has changed,
                    // and you need to update
                    //setShareIntent();

                    //Save the file when the new roll has been added
                    writeRollFile(inputName);




                }
        }
    }


    // READ AND WRITE METHODS

    private void writeRollFile(String input) {
        try {
            File file = new File(getFilesDir(), "List_of_Rolls.txt");

            FileWriter writer = new FileWriter(file, true);
            writer.write(input + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readRollFile() {
        //Get the text file
        File file = new File(getFilesDir(), "List_of_Rolls.txt");

        //Read text from file
        //StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
//                text.append(line);
//                text.append('\n');
                mNameList.add(line);
                mainTextView.setVisibility(View.GONE);
            }
            br.close();

            mArrayAdapter.notifyDataSetChanged();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //return text.toString();
    }

    // Method removes a line from a file.
    // It takes the file and the removed line number as argument
    static public void removeLine(final File file, final int lineIndex) throws IOException{
        final List<String> lines = new LinkedList<>();
        final Scanner reader = new Scanner(new FileInputStream(file), "UTF-8");
        while(reader.hasNextLine())
            lines.add(reader.nextLine());
        reader.close();
        assert lineIndex >= 0 && lineIndex <= lines.size() - 1;
        lines.remove(lineIndex);
        final BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
        for(final String line : lines)
            writer.write(line + "\n");
        writer.flush();
        writer.close();
    }
}

