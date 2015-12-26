package com.tommihirvonen.filmphotonotes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

// Copyright 2015
// Tommi Hirvonen

public class MainActivity extends ActionBarActivity implements
        //View.OnClickListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, MenuItem.OnMenuItemClickListener, RollNameDialog.onNameSetCallback, EditRollNameDialog.OnNameEditedCallback {

    public final static String EXTRA_MESSAGE = "com.tommihirvonen.filmphotonotes.MESSAGE";

    TextView mainTextView;

    ListView mainListView;
    RollAdapter mArrayAdapter;
    ArrayList<String> mNameList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setTitle("  " + this.getString(R.string.MainActivityTitle));
        getSupportActionBar().setIcon(R.mipmap.film_photo_notes_icon);

        mainTextView = (TextView) findViewById(R.id.no_added_rolls);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.main_listview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new RollAdapter(this, android.R.layout.simple_list_item_1, mNameList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        // Set this activity to react to list items being pressed and held
        mainListView.setOnItemLongClickListener(this);


        // Read the rolls from file and add to list
        File file = new File(getFilesDir(), "List_of_Rolls.txt");
        if ( file.exists() ) readRollFile();
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem addRoll = menu.findItem(R.id.menu_item_add_roll);
        MenuItem deleteRoll = menu.findItem(R.id.menu_item_delete_roll);
        MenuItem about = menu.findItem(R.id.menu_item_about);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        MenuItem lenses = menu.findItem(R.id.menu_item_lenses);

        addRoll.setOnMenuItemClickListener(this);
        deleteRoll.setOnMenuItemClickListener(this);
        about.setOnMenuItemClickListener(this);
        help.setOnMenuItemClickListener(this);
        lenses.setOnMenuItemClickListener(this);

        return true;
    }





    @Override
    // Pressing the roll allows the user to show the frames taken with that roll
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent intent = new Intent(this, RollInfo.class);
        intent.putExtra(EXTRA_MESSAGE, mNameList.get(position));
        startActivity(intent);
    }

    @Override
    //Long pressing the roll allows the user to rename the roll
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        show_EditRollNameDialog(mNameList.get(position));


        //Return true because the item was pressed and held.
        return true;
    }





    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_add_roll:
                //AskForNameOfRoll();
                show_RollNameDialog();
                break;
            case R.id.menu_item_delete_roll:
                //Only delete if there are more than one roll
                if ( mNameList.size() >= 1 ) {

                    // Ask the user which roll to delete

                    // LIST ITEMS DIALOG

                    List<String> listItems = new ArrayList<>();
                    for ( int i = 0; i < mNameList.size(); ++i ) {
                        listItems.add(mNameList.get(i));
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

//                    builder.setTitle("Pick a roll to delete");
//                    builder.setItems(items, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int item) {
//
//                            // Do something with the selection
//
//                            // If the frames file exists, delete it too
//                            File frames_file = new File(getFilesDir(), mNameList.get(item).toString() + ".txt");
//                            if ( frames_file.exists() ) {
//                                try {
//                                    boolean delete = frames_file.delete();
//                                } catch (Exception e) {
//                                    Log.e("App", "Exception while deleting file " + e.getMessage());
//                                }
//                            }
//
//                            mNameList.remove(item);
//
//                            if (mNameList.size() == 0 ) mainTextView.setVisibility(View.VISIBLE);
//                            mArrayAdapter.notifyDataSetChanged();
//
//                            File file = new File(getFilesDir(), "List_of_Rolls.txt");
//                            try {
//                                removeLine(file, item);
//                            }
//                            catch (IOException e){
//                                e.printStackTrace();
//                            }
//
//
//                        }
//                    });
//                    AlertDialog alert = builder.create();
//                    alert.show();


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

                                    // Remove the roll file
                                    String name_of_roll = mNameList.get(which);

                                    File frames_file = new File(getFilesDir(), name_of_roll + ".txt");
                                    if ( frames_file.exists() ) {
                                        try {
                                            boolean delete = frames_file.delete();
                                        }
                                        catch ( Exception e ) {
                                            e.printStackTrace();
                                        }
                                    }

                                    // Remove the roll name line from the List_of_Rolls.txt
                                    File rolls_file = new File(getFilesDir(), "List_of_Rolls.txt");
                                    try {
                                        removeLine(rolls_file, which);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    // Remove the roll from the mNameList. Do this last!!!
                                    mNameList.remove(which);
                                }
                                if (mNameList.size() == 0 ) mainTextView.setVisibility(View.VISIBLE);
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
                //intent.putExtra(EXTRA_MESSAGE, "Data");
                startActivity(intent);

                break;

            case R.id.menu_item_about:

                AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
                aboutDialog.setTitle(R.string.app_name);
                aboutDialog.setMessage(R.string.about);
                aboutDialog.setIcon(R.mipmap.film_photo_notes_icon);


                aboutDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
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

    private void show_EditRollNameDialog(String oldName){
        EditRollNameDialog dialog = new EditRollNameDialog();
        dialog.setOldName(oldName);
        dialog.show(getSupportFragmentManager(), EditRollNameDialog.TAG);
    }

    private void show_RollNameDialog() {
        RollNameDialog dialog = new RollNameDialog();
        dialog.show(getSupportFragmentManager(), RollNameDialog.TAG);
    }

    @Override
    public void onNameSet(String inputName) {
        if(!TextUtils.isEmpty(inputName)) {

                if ( inputName.length() != 0 ) {

                    //Check if a roll with the same name already exists
                    for ( int i = 0; i < mNameList.size(); ++i ) {
                        if ( inputName.equals( mNameList.get(i) )  ) {
                            Toast toast = Toast.makeText(getApplicationContext(), R.string.RollSameName, Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return;
                        }
                    }

                    //Check if there are illegal character in the roll name
                    String ReservedChars = "|\\?*<\":>/";
                    for ( int i = 0; i < inputName.length(); ++i ) {
                        Character c = inputName.charAt(i);
                        if ( ReservedChars.contains(c.toString()) ) {
                            Toast toast = Toast.makeText(getApplicationContext(), R.string.RollIllegalCharacter + c.toString(), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return;
                        }
                    }

                    mainTextView.setVisibility(View.GONE);
                    mNameList.add(inputName);
                    mArrayAdapter.notifyDataSetChanged();

                    // When the new roll is added jump to view the last entry
                    mainListView.setSelection(mainListView.getCount() - 1);


                    //Save the file when the new roll has been added
                    writeRollFile(inputName);




                }
        }
    }

    @Override
    public void OnNameEdited(String newName, String oldName){
        if(!TextUtils.isEmpty(newName)) {
            // Grab the EditText's input

            if ( newName.length() != 0 ) {

                //Check if a roll with the same name already exists
                for ( int i = 0; i < mNameList.size(); ++i ) {
                    if ( newName.equals( mNameList.get(i))  ) {
                        Toast toast = Toast.makeText(getApplicationContext(), R.string.RollSameName, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return;
                    }
                }

                //Check if there are illegal character in the roll name
                String ReservedChars = "|\\?*<\":>/";
                for ( int i = 0; i < newName.length(); ++i ) {
                    Character c = newName.charAt(i);
                    if ( ReservedChars.contains(c.toString()) ) {
                        Toast toast = Toast.makeText(getApplicationContext(), R.string.RollIllegalCharacter + c.toString(), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return;
                    }
                }

                // Change the string in mNameList
                int position = 0;
                for ( int i = 0; i < mNameList.size(); ++i) {
                    if (oldName.equals(mNameList.get(i))) {
                        position = i;
                    }
                }

                mNameList.set(position, newName);

                // Notify array adapter that the dataset has to be updated
                mArrayAdapter.notifyDataSetChanged();

                // List_of_Rolls.txt has to be updated
                updateListOfRolls(newName, oldName);

                // The Roll_File has to renamed
                renameFrameFile(newName, oldName);

            }
        }
    }


    // READ AND WRITE METHODS

    // This method replaces a string in a List_of_Rolls.Txt
    private void updateListOfRolls(String newName, String oldName){
        try
        {
            File file = new File(getFilesDir(), "List_of_Rolls.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line, oldtext = "";
            while((line = reader.readLine()) != null)
            {
                oldtext += line + "\r\n";
            }
            reader.close();

            //To replace a line in a file
            String newtext = oldtext.replaceAll(oldName, newName);

            FileWriter writer = new FileWriter(file);
            writer.write(newtext);writer.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    // This method renames a text file
    private void renameFrameFile(String newName, String oldName) {
        File from = new File(getFilesDir(), oldName + ".txt");
        File to = new File(getFilesDir(), newName + ".txt");
        from.renameTo(to);
    }

    // This method writes to the List_of_Rolls.txt file a new roll of film
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

    // This method reads in the List_of_Rolls.txt file and builds the in-app database
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

