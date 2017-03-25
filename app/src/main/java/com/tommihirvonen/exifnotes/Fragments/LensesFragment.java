package com.tommihirvonen.exifnotes.Fragments;

// Copyright 2015
// Tommi Hirvonen

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
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

import com.tommihirvonen.exifnotes.Adapters.LensAdapter;
import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.Datastructures.Filter;
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Dialogs.EditLensInfoDialog;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Activities.GearActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LensesFragment extends Fragment implements
        View.OnClickListener, AdapterView.OnItemClickListener {

    public static final int ADD_LENS = 1;
    public static final int EDIT_LENS = 2;

    TextView mainTextView;
    ListView mainListView;
    LensAdapter lensAdapter;
    List<Lens> lensList = new ArrayList<>();
    FilmDbHelper database;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        database = FilmDbHelper.getInstance(getActivity());
        lensList = database.getAllLenses();

        final View view = linf.inflate(R.layout.lenses_fragment, container, false);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_lenses);
        fab.setOnClickListener(this);

        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = (TextView) view.findViewById(R.id.no_added_lenses);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.main_lenseslistview);

        // Create an ArrayAdapter for the ListView
        lensAdapter = new LensAdapter(getActivity(), android.R.layout.simple_list_item_1, lensList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(lensAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        registerForContextMenu(mainListView);

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(
                new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(2);

        if (lensList.size() >= 1) mainTextView.setVisibility(View.GONE);

        lensAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        //Set the title for the context menu
        AdapterView.AdapterContextMenuInfo info = null;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException ignore) {
            //Do nothing
        }
        if (info != null) {
            final int position = info.position;
            Lens lens = null;
            try {
                lens = lensList.get(position);
            } catch(NullPointerException | IndexOutOfBoundsException ignore) {
                //Do nothing
            }
            if (lens != null) {
                menu.setHeaderTitle(lens.getMake() + " " + lens.getModel());
            }
        }

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_delete_edit_select_cameras, menu);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view.performLongClick();
    }

    @SuppressLint("CommitTransaction")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (getUserVisibleHint()) {

            final int position = info.position;
            final Lens lens = lensList.get(position);

            switch (item.getItemId()) {

                case R.id.menu_item_select_mountable_cameras:

                    showSelectMountableCamerasDialog(position);
                    return true;

                case R.id.menu_item_select_mountable_filters:

                    showSelectMountableFiltersDialog(position);
                    return true;

                case R.id.menu_item_delete:

                    // Check if the lens is being used with one of the frames.
                    if (database.isLensInUse(lens)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.LensNoColon) +
                                " " + lens.getMake() + " " + lens.getModel() + " " +
                                getResources().getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.ConfirmLensDelete)
                            + " \'" + lens.getMake() + " " + lens.getModel() + "\'?"
                    );
                    builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing

                        }
                    });
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            database.deleteLens(lens);

                            // Remove the lens from the lensList. Do this last!!!
                            lensList.remove(position);

                            if (lensList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                            lensAdapter.notifyDataSetChanged();

                            // Update the CamerasFragment through the parent activity.
                            GearActivity gearActivity = (GearActivity)getActivity();
                            gearActivity.updateFragments();

                        }
                    });
                    builder.create().show();

                    return true;

                case R.id.menu_item_edit:

                    EditLensInfoDialog dialog = new EditLensInfoDialog();
                    dialog.setTargetFragment(this, EDIT_LENS);
                    Bundle arguments = new Bundle();
                    arguments.putString("TITLE", getResources().getString( R.string.EditLens));
                    arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.OK));
                    arguments.putParcelable("LENS", lens);
                    arguments.putInt("POSITION", position);
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), EditLensInfoDialog.TAG);

                    return true;
            }
        }
        return false;
    }

    @SuppressLint("CommitTransaction")
    private void showLensNameDialog() {
        EditLensInfoDialog dialog = new EditLensInfoDialog();
        dialog.setTargetFragment(this, ADD_LENS);
        Bundle arguments = new Bundle();
        arguments.putString("TITLE", getResources().getString( R.string.NewLens));
        arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditLensInfoDialog.TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case ADD_LENS:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    Lens lens = data.getParcelableExtra("LENS");

                    if (lens.getMake().length() > 0 && lens.getModel().length() > 0) {

                        mainTextView.setVisibility(View.GONE);

                        long rowId = database.addLens(lens);
                        lens.setId(rowId);
                        lensList.add(lens);
                        lensAdapter.notifyDataSetChanged();

                        // When the lens is added jump to view the last entry
                        mainListView.setSelection(mainListView.getCount() - 1);
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    // Do nothing.
                    return;
                }

                break;

            case EDIT_LENS:

                if (resultCode == Activity.RESULT_OK) {

                    Lens lens = data.getParcelableExtra("LENS");

                    if (lens.getMake().length() > 0 &&
                            lens.getModel().length() > 0 &&
                            lens.getId() > 0) {

                        database.updateLens(lens);
                        lensAdapter.notifyDataSetChanged();
                        // Update the LensesFragment through the parent activity.
                        GearActivity gearActivity = (GearActivity)getActivity();
                        gearActivity.updateFragments();

                    } else {
                        Toast.makeText(getActivity(), "Something went wrong :(",
                                Toast.LENGTH_SHORT).show();
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){

                    return;
                }

                break;
        }
    }


    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab_lenses:
                showLensNameDialog();
                break;
        }
    }

    void showSelectMountableCamerasDialog(int position){
        final Lens lens = lensList.get(position);
        final List<Camera> mountableCameras = database.getMountableCameras(lens);
        final List<Camera> allCameras = database.getAllCameras();

        // Make a list of strings for all the camera names to be showed in the
        // multi choice list.
        // Also make an array list containing all the camera id's for list comparison.
        // Comparing lists containing frames is not easy.
        List<String> listItems = new ArrayList<>();
        List<Long> allCamerasId = new ArrayList<>();
        for (int i = 0; i < allCameras.size(); ++i) {
            listItems.add(allCameras.get(i).getMake() + " " + allCameras.get(i).getModel());
            allCamerasId.add(allCameras.get(i).getId());
        }

        // Make an array list containing all mountable camera id's.
        List<Long> mountableCamerasId = new ArrayList<>();
        for (int i = 0; i < mountableCameras.size(); ++i) {
            mountableCamerasId.add(mountableCameras.get(i).getId());
        }

        // Find the items in the list to be preselected
        final boolean[] booleans = new boolean[allCameras.size()];
        for (int i= 0; i < allCamerasId.size(); ++i) {
            booleans[i] = mountableCamerasId.contains(allCamerasId.get(i));
        }

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // MULTIPLE CHOICE DIALOG

        // Create an array list where the selections are saved. Initialize it with
        // the booleans array.
        final List<Integer> selectedItemsIndexList = new ArrayList<>();
        for (int i = 0; i < booleans.length; ++i) {
            if (booleans[i]) selectedItemsIndexList.add(i);
        }

        builder.setTitle(R.string.SelectMountableCameras)
                .setMultiChoiceItems(items, booleans, new DialogInterface.OnMultiChoiceClickListener() {
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

                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        // Do something with the selections
                        Collections.sort(selectedItemsIndexList);

                        // Get the not selected indices.
                        ArrayList<Integer> notSelectedItemsIndexList = new ArrayList<>();
                        for (int i = 0; i < allCameras.size(); ++i) {
                            if (!selectedItemsIndexList.contains(i))
                                notSelectedItemsIndexList.add(i);
                        }

                        // Iterate through the selected items
                        for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = selectedItemsIndexList.get(i);
                            Camera camera = allCameras.get(which);
                            database.addMountable(camera, lens);
                        }

                        // Iterate through the not selected items
                        for (int i = notSelectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = notSelectedItemsIndexList.get(i);
                            Camera camera = allCameras.get(which);
                            database.deleteMountable(camera, lens);
                        }
                        lensAdapter.notifyDataSetChanged();

                        // Update the CamerasFragment through the parent activity.
                        GearActivity gearActivity = (GearActivity)getActivity();
                        gearActivity.updateFragments();
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    void showSelectMountableFiltersDialog(int position){
        final Lens lens = lensList.get(position);
        final List<Filter> mountableFilters = database.getMountableFilters(lens);
        final List<Filter> allFilters = database.getAllFilters();

        // Make a list of strings for all the filter names to be showed in the
        // multi choice list.
        // Also make an array list containing all the filter id's for list comparison.
        // Comparing lists containing frames is not easy.
        List<String> listItems = new ArrayList<>();
        List<Long> allFiltersId = new ArrayList<>();
        for (int i = 0; i < allFilters.size(); ++i) {
            listItems.add(allFilters.get(i).getMake() + " " + allFilters.get(i).getModel());
            allFiltersId.add(allFilters.get(i).getId());
        }

        // Make an array list containing all mountable filter id's.
        List<Long> mountableFiltersId = new ArrayList<>();
        for (int i = 0; i < mountableFilters.size(); ++i) {
            mountableFiltersId.add(mountableFilters.get(i).getId());
        }

        // Find the items in the list to be preselected
        final boolean[] booleans = new boolean[allFilters.size()];
        for (int i= 0; i < allFiltersId.size(); ++i) {
            booleans[i] = mountableFiltersId.contains(allFiltersId.get(i));
        }



        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // MULTIPLE CHOICE DIALOG

        // Create an array list where the selections are saved. Initialize it with
        // the booleans array.
        final List<Integer> selectedItemsIndexList = new ArrayList<>();
        for (int i = 0; i < booleans.length; ++i) {
            if (booleans[i]) selectedItemsIndexList.add(i);
        }

        builder.setTitle(R.string.SelectMountableFilters)
                .setMultiChoiceItems(items, booleans, new DialogInterface.OnMultiChoiceClickListener() {
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

                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        // Do something with the selections
                        Collections.sort(selectedItemsIndexList);

                        // Get the not selected indices.
                        List<Integer> notSelectedItemsIndexList = new ArrayList<>();
                        for (int i = 0; i < allFilters.size(); ++i) {
                            if (!selectedItemsIndexList.contains(i))
                                notSelectedItemsIndexList.add(i);
                        }

                        // Iterate through the selected items
                        for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = selectedItemsIndexList.get(i);
                            Filter filter = allFilters.get(which);
                            database.addMountableFilterLens(filter, lens);
                        }

                        // Iterate through the not selected items
                        for (int i = notSelectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = notSelectedItemsIndexList.get(i);
                            Filter filter = allFilters.get(which);
                            database.deleteMountableFilterLens(filter, lens);
                        }
                        lensAdapter.notifyDataSetChanged();

                        // Update the FiltersFragment through the parent activity.
                        GearActivity gearActivity = (GearActivity)getActivity();
                        gearActivity.updateFragments();
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void updateFragment(){
        if (lensAdapter != null) lensAdapter.notifyDataSetChanged();
    }
}
