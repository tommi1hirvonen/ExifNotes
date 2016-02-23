package com.tommihirvonen.filmphotonotes;

// Copyright 2015
// Tommi Hirvonen

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class RollsFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    OnRollSelectedListener mCallback;

    public interface OnRollSelectedListener{
        void onRollSelected(int rollId);
    }

    // This on attach is called before API 23
    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        mCallback = (OnRollSelectedListener) a;
    }

    // This on attach is called after API 23
    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        mCallback = (OnRollSelectedListener) c;
    }


    FloatingActionButton fab;
    TextView mainTextView;
    ListView mainListView;
    RollAdapter mArrayAdapter;
    ArrayList<Roll> mRollList = new ArrayList<>();
    FilmDbHelper database;

    public static final int ROLL_NAME_DIALOG = 1;
    public static final int EDIT_ROLL_NAME_DIALOG = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("  " + getResources().getString(R.string.MainActivityTitle));
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("");
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);

        database = new FilmDbHelper(getActivity());
        mRollList = database.getAllRolls();

        final View view = linf.inflate(R.layout.rolls_fragment, container, false);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(this);

        mainTextView = (TextView) view.findViewById(R.id.no_added_rolls);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.main_listview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new RollAdapter(getActivity(), android.R.layout.simple_list_item_1, mRollList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        if ( mRollList.size() >= 1 ) mainTextView.setVisibility(View.GONE);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        // Set this activity to react to list items being pressed and held
        //mainListView.setOnItemLongClickListener(this);

        registerForContextMenu(mainListView);

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(2);

        if ( mainListView.getCount() >= 1) mainListView.setSelection(mainListView.getCount() - 1);

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        return view;
    }


    @Override
    public void onResume(){
        super.onResume();
        mArrayAdapter.notifyDataSetChanged();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_delete_edit, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            /*case R.id.menu_item_delete:

                //Only delete if there are more than one roll
                if ( mRollList.size() >= 1 ) {

                    // Ask the user which roll to delete

                    // LIST ITEMS DIALOG

                    List<String> listItems = new ArrayList<>();
                    for ( int i = 0; i < mRollList.size(); ++i ) {
                        listItems.add(mRollList.get(i).getName());
                    }
                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

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
                break;*/

            case R.id.menu_item_lenses:
                Intent intent = new Intent(getActivity(), GearActivity.class);
                startActivity(intent);

                break;
            case R.id.menu_item_preferences:

                Intent preferences_intent = new Intent(getActivity(), PreferenceActivity.class);
                // With these extras we can skip the headers in the preferences.
                preferences_intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, PreferenceFragment.class.getName() );
                preferences_intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );

                startActivity(preferences_intent);

                break;

            case R.id.menu_item_about:

                AlertDialog.Builder aboutDialog = new AlertDialog.Builder(getActivity());
                aboutDialog.setTitle(R.string.app_name);
                aboutDialog.setMessage(R.string.about);

                aboutDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                aboutDialog.show();

                break;

            case R.id.menu_item_help:

                AlertDialog.Builder helpDialog = new AlertDialog.Builder(getActivity());
                helpDialog.setTitle(R.string.Help);
                helpDialog.setMessage(R.string.main_help);


                helpDialog.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                helpDialog.show();

                break;

            case R.id.menu_item_show_on_map:

                // Show all frames from all rolls on a map
                Intent intent2 = new Intent(getActivity(), AllFramesMapsActivity.class);
                startActivity(intent2);

                break;
        }

        return true;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                show_RollNameDialog();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // GET ROLL INFO
        int rollId = mRollList.get(position).getId();
        Log.d("ROLL_ID", "" + rollId);
        mCallback.onRollSelected(rollId);
    }

    /*@Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        show_EditRollNameDialog(mRollList.get(position).getId(), mRollList.get(position).getName(), mRollList.get(position).getNote(), mRollList.get(position).getCamera_id());
        return true;
    }*/



    private void show_EditRollNameDialog(int rollId, String oldName, String oldNote, int camera_id){
        EditRollNameDialog dialog = new EditRollNameDialog();
        dialog.setOldName(rollId, oldName, oldNote, camera_id);
        dialog.setTargetFragment(this, EDIT_ROLL_NAME_DIALOG);
        dialog.show(getFragmentManager().beginTransaction(), EditRollNameDialog.TAG);
    }

    private void show_RollNameDialog() {
        RollNameDialog dialog = new RollNameDialog();
        dialog.setTargetFragment(this, ROLL_NAME_DIALOG);
        dialog.show(getFragmentManager().beginTransaction(), RollNameDialog.TAG);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if ( getUserVisibleHint() ) {
            switch (item.getItemId()) {
                case R.id.menu_item_edit:

                    int position = info.position;
                    show_EditRollNameDialog(mRollList.get(position).getId(), mRollList.get(position).getName(), mRollList.get(position).getNote(), mRollList.get(position).getCamera_id());

                    return true;

                case R.id.menu_item_delete:

                    int which = info.position;

                    // Delete all the frames from the frames database
                    database.deleteAllFramesFromRoll(mRollList.get(which).getId());

                    database.deleteRoll(mRollList.get(which));

                    // Remove the roll from the mRollList. Do this last!!!
                    mRollList.remove(which);

                    if (mRollList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                    mArrayAdapter.notifyDataSetChanged();
                    return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case ROLL_NAME_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    String inputName = data.getStringExtra("NAME");
                    String inputNote = data.getStringExtra("NOTE");
                    int camera_id = data.getIntExtra("CAMERA_ID", -1);

                    if (inputName.length() != 0 && camera_id != -1) {

                        //Check if there are illegal character in the roll name
                        String ReservedChars = "|\\?*<\":>/";
                        for (int i = 0; i < inputName.length(); ++i) {
                            Character c = inputName.charAt(i);
                            if (ReservedChars.contains(c.toString())) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.RollIllegalCharacter) + " " + c.toString(), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }
                        for (int i = 0; i < inputNote.length(); ++i) {
                            Character c = inputNote.charAt(i);
                            if (ReservedChars.contains(c.toString())) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.RollIllegalCharacter) + " " + c.toString(), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }

                        Roll roll = new Roll();
                        roll.setName(inputName);
                        roll.setDate(getCurrentTime());
                        roll.setNote(inputNote);
                        roll.setCamera_id(camera_id);
                        database.addRoll(roll);
                        roll = database.getLastRoll();

                        mainTextView.setVisibility(View.GONE);
                        // Add new roll to the top of the list
                        mRollList.add(0, roll);
                        mArrayAdapter.notifyDataSetChanged();

                        // When the new roll is added jump to view the last entry
                        mainListView.setSelection(mainListView.getCount() - 1);
                    }
                } else if ( resultCode == Activity.RESULT_CANCELED ) {
                    // After cancel do nothing
                    return;
                }
                break;

            case EDIT_ROLL_NAME_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    String newName = data.getStringExtra("NEWNAME");
                    int rollId = data.getIntExtra("ROLL_ID", -1);
                    int camera_id = data.getIntExtra("CAMERA_ID", -1);
                    String newNote = data.getStringExtra("NEWNOTE");

                    if ( newName.length() != 0 && rollId != -1 && camera_id != -1 ) {

                        //Check if there are illegal character in the roll name
                        String ReservedChars = "|\\?*<\":>/";
                        for ( int i = 0; i < newName.length(); ++i ) {
                            Character c = newName.charAt(i);
                            if ( ReservedChars.contains(c.toString()) ) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.RollIllegalCharacter) + " " + c.toString(), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }
                        for ( int i = 0; i < newNote.length(); ++i ) {
                            Character c = newNote.charAt(i);
                            if ( ReservedChars.contains(c.toString()) ) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.NoteIllegalCharacter) + " " + c.toString(), Toast.LENGTH_LONG);
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
                        roll.setCamera_id(camera_id);
                        database.updateRoll(roll);

                        // Notify array adapter that the dataset has to be updated
                        mArrayAdapter.notifyDataSetChanged();
                    }
                } else if ( resultCode == Activity.RESULT_CANCELED ) {
                    // After cancel do nothing
                    return;
                }
                break;

        }
    }

    public static String getCurrentTime() {
        final Calendar c = Calendar.getInstance();
        int iYear = c.get(Calendar.YEAR);
        int iMonth = c.get(Calendar.MONTH) + 1;
        int iDay = c.get(Calendar.DAY_OF_MONTH);
        int iHour = c.get(Calendar.HOUR_OF_DAY);
        int iMin = c.get(Calendar.MINUTE);
        String current_time;
        if (iMin < 10) {
            current_time = iYear + "-" + iMonth + "-" + iDay + " " + iHour + ":0" + iMin;
        } else current_time = iYear + "-" + iMonth + "-" + iDay + " " + iHour + ":" + iMin;
        return current_time;
    }
}
