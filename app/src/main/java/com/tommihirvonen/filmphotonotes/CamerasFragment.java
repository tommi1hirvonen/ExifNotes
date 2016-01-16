package com.tommihirvonen.filmphotonotes;// Copyright 2015
// Tommi Hirvonen

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CamerasFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    public static final String ARG_PAGE = "ARG_PAGE";

    TextView mainTextView;

    ListView mainListView;

    CameraAdapter mArrayAdapter;

    ArrayList<Camera> mCameraList;

    FilmDbHelper database;

    public static final int DIALOG_FRAGMENT = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        database = new FilmDbHelper(getContext());
        mCameraList = database.getAllCameras();

        final View view = linf.inflate(R.layout.cameras_fragment, container, false);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_cameras);
        fab.setOnClickListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        mainTextView = (TextView) view.findViewById(R.id.no_added_cameras);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.main_cameraslistview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new CameraAdapter(getActivity(), android.R.layout.simple_list_item_1, mCameraList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        if ( mCameraList.size() >= 1 ) mainTextView.setVisibility(View.GONE);

        mArrayAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        if ( item.getItemId() == R.id.menu_item_delete_gear ) {

            // Only delete if there are more than one lens
            if (mCameraList.size() >= 1) {

                // Ask the user which lens to delete

                List<String> listItems = new ArrayList<>();
                for ( int i = 0; i < mCameraList.size(); ++i) {
                    listItems.add(mCameraList.get(i).getName());
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                // MULTIPLE CHOICE DIALOG
                final ArrayList<Integer> selectedItemsIndexList = new ArrayList<>();
                builder.setTitle(R.string.PickCamerasToDelete)
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

                                    Camera camera = mCameraList.get(which);
                                    database.deleteCamera(camera);

                                    // Remove the roll from the mLensList. Do this last!!!
                                    mCameraList.remove(which);
                                }
                                if (mCameraList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
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

            return true;
        }

        return false;
    }

    private void showCameraNameDialog() {
        CameraNameDialog dialog = new CameraNameDialog();
        dialog.setTargetFragment(this, DIALOG_FRAGMENT);
        dialog.show(getFragmentManager().beginTransaction(), CameraNameDialog.TAG);
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab_cameras:
                showCameraNameDialog();
                break;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case DIALOG_FRAGMENT:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    String inputText = data.getStringExtra("NAME");

                    if ( inputText.length() != 0 ) {

                        // Check if a lens with the same name already exists
                        for ( int i = 0; i < mCameraList.size(); ++i ) {
                            if ( inputText.equals( mCameraList.get(i).getName())  ) {
                                Toast toast = Toast.makeText(getActivity(), R.string.CameraSameName, Toast.LENGTH_LONG);
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
                                Toast toast = Toast.makeText(getActivity(), R.string.CameraIllegalCharacter + c.toString(), Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                return;
                            }
                        }

                        mainTextView.setVisibility(View.GONE);

                        Camera camera = new Camera();
                        camera.setName(inputText);
                        database.addCamera(camera);
                        // When we get the last added lens from the database we get the row id value.
                        camera = database.getLastCamera();
                        mCameraList.add(camera);
                        mArrayAdapter.notifyDataSetChanged();

                        // When the lens is added jump to view the last entry
                        mainListView.setSelection(mainListView.getCount() - 1);
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    // Do nothing.
                    return;
                }
                break;
        }
    }
}