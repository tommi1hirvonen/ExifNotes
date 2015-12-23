package com.tommihirvonen.filmphotonotes;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Tommi on 23.12.2015.
 */
public class LensesActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, MenuItem.OnMenuItemClickListener, LensNameDialog.onLensNameSetCallback {

    TextView mainTextView;

    ListView mainListView;

    LensAdapter mArrayAdapter;

    ArrayList<String> mLensList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lenses);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        getSupportActionBar().setTitle("  Lenses");

        getSupportActionBar().setIcon(R.mipmap.film_photo_notes_icon);

        mainTextView = (TextView) findViewById(R.id.no_added_lenses);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.main_lenseslistview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new LensAdapter(this, android.R.layout.simple_list_item_1, mLensList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        // Read the lenses from file and add to list
        File file = new File(getFilesDir(), "List_of_Lenses.txt");
        if ( file.exists() ) readLensFile();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_lenses, menu);

        MenuItem addLens = menu.findItem(R.id.menu_item_add_lens);
        MenuItem deleteLens = menu.findItem(R.id.menu_item_delete_lens);

        addLens.setOnMenuItemClickListener(this);
        deleteLens.setOnMenuItemClickListener(this);

        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Log.d("FilmPhotoNotes", position + ": " + mLensList.get(position));

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_item_add_lens:
                showLensNameDialog();
                break;

            case R.id.menu_item_delete_lens:

                // Only delete if there are more than one lens
                if (mLensList.size() >= 1) {

                    // Ask the user which lens to delete

                    List<String> listItems = new ArrayList<String>();
                    for ( int i = 0; i < mLensList.size(); ++i) {
                        listItems.add(mLensList.get(i).toString());
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    // MULTIPLE CHOICE DIALOG
                    final ArrayList<Integer> selectedItemsIndexList = new ArrayList<>();
                    builder.setTitle("Pick lenses to delete")
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
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                // Do something with the selections
                                Collections.sort(selectedItemsIndexList);
                                for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                                    int which = selectedItemsIndexList.get(i);

                                    // Remove the lens line from the List_of_Lenses.txt
                                    File lenses_file = new File(getFilesDir(), "List_of_Lenses.txt");
                                    try {
                                        MainActivity.removeLine(lenses_file, which);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    // Remove the roll from the mNameList. Do this last!!!
                                    mLensList.remove(which);
                                }
                                if (mLensList.size() == 0 ) mainTextView.setVisibility(View.VISIBLE);
                                mArrayAdapter.notifyDataSetChanged();

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
        if(!TextUtils.isEmpty(inputText)) {
            String inputName = inputText;

            if ( inputName.length() != 0 ) {

                // Check if a lens with the same name already exists
                for ( int i = 0; i < mLensList.size(); ++i ) {
                    if ( inputName.equals( mLensList.get(i).toString() )  ) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Lens with same name already exists!", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return;
                    }
                }

                mainTextView.setVisibility(View.GONE);
                mLensList.add(inputName);
                mArrayAdapter.notifyDataSetChanged();

                // When the lens is added jump to view the last entry
                mainListView.setSelection(mainListView.getCount() - 1);

                // Save the file when the new roll has been added
                writeLensFile(inputName);
            }
        }
    }

    private void readLensFile() {

        File file = new File(getFilesDir(), "List_of_Lenses.txt");

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;

            while ( (line = br.readLine()) != null ) {
                mLensList.add(line);
                mainTextView.setVisibility(View.GONE);
            }
            br.close();

            mArrayAdapter.notifyDataSetChanged();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    private void writeLensFile(String input) {
        try {
            File file = new File(getFilesDir(), "List_of_Lenses.txt");
            FileWriter writer = new FileWriter(file, true);
            writer.write(input + "\n");
            writer.flush();
            writer.close();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

}
