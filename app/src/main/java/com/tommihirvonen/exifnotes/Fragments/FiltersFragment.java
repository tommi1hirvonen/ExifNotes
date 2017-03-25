package com.tommihirvonen.exifnotes.Fragments;

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

import com.tommihirvonen.exifnotes.Activities.GearActivity;
import com.tommihirvonen.exifnotes.Adapters.FilterAdapter;
import com.tommihirvonen.exifnotes.Datastructures.Filter;
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Dialogs.EditFilterInfoDialog;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Copyright 2016
// Tommi Hirvonen

public class FiltersFragment extends Fragment implements
        AdapterView.OnItemClickListener,
        View.OnClickListener {

    public static final int ADD_FILTER = 1;
    public static final int EDIT_FILTER = 2;
    TextView mainTextView;
    ListView mainListView;
    FilterAdapter filterAdapter;
    List<Filter> filterList;
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
        filterList = database.getAllFilters();

        final View view = linf.inflate(R.layout.filters_fragment, container, false);

        FloatingActionButton floatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_filters);
        floatingActionButton.setOnClickListener(this);

        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = (TextView) view.findViewById(R.id.no_added_filters);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.main_filterslistview);

        // Create an ArrayAdapter for the ListView
        filterAdapter = new FilterAdapter(
                getActivity(), android.R.layout.simple_list_item_1, filterList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(filterAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        registerForContextMenu(mainListView);

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(
                new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(2);

        if (filterList.size() >= 1) mainTextView.setVisibility(View.GONE);

        filterAdapter.notifyDataSetChanged();

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
            Filter filter = null;
            try {
                filter = filterList.get(position);
            } catch(NullPointerException | IndexOutOfBoundsException ignore) {
                //Do nothing
            }
            if (filter != null) {
                menu.setHeaderTitle(filter.getMake() + " " + filter.getModel());
            }
        }

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_delete_edit_select_lenses, menu);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        view.performLongClick();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fab_filters:
                showFilterNameDialog();
                break;
        }
    }

    @SuppressLint("CommitTransaction")
    public void showFilterNameDialog() {
        EditFilterInfoDialog dialog = new EditFilterInfoDialog();
        dialog.setTargetFragment(this, ADD_FILTER);
        Bundle arguments = new Bundle();
        arguments.putString("TITLE", getResources().getString( R.string.NewFilter));
        arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditFilterInfoDialog.TAG);
    }

    @SuppressLint("CommitTransaction")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (getUserVisibleHint()) {

            final int position = info.position;
            final Filter filter = filterList.get(position);

            switch (item.getItemId()) {

                case R.id.menu_item_select_mountable_lenses:

                    showSelectMountableLensesDialog(position);
                    return true;

                case R.id.menu_item_delete:

                    // Check if the filter is being used with one of the rolls.
                    if (database.isFilterBeingUsed(filter)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.FilterNoColon) +
                                " " + filter.getMake() + " " + filter.getModel() + " " +
                                getResources().getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.ConfirmFilterDelete)
                            + " \'" + filter.getMake() + " " + filter.getModel() + "\'?"
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

                            database.deleteFilter(filter);

                            // Remove the filter from the filterList. Do this last!!!
                            filterList.remove(position);

                            if (filterList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                            filterAdapter.notifyDataSetChanged();

                            // Update the LensesFragment through the parent activity.
                            GearActivity gearActivity = (GearActivity)getActivity();
                            gearActivity.updateFragments();

                        }
                    });
                    builder.create().show();

                    return true;

                case R.id.menu_item_edit:

                    EditFilterInfoDialog dialog = new EditFilterInfoDialog();
                    dialog.setTargetFragment(this, EDIT_FILTER);
                    Bundle arguments = new Bundle();
                    arguments.putString("TITLE", getResources().getString( R.string.EditFilter));
                    arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.OK));
                    arguments.putParcelable("FILTER", filter);
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), EditFilterInfoDialog.TAG);

                    return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case ADD_FILTER:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    Filter filter = data.getParcelableExtra("FILTER");

                    if (filter.getMake().length() > 0 && filter.getModel().length() > 0) {

                        mainTextView.setVisibility(View.GONE);

                        long rowId = database.addFilter(filter);
                        filter.setId(rowId);
                        filterList.add(filter);
                        filterAdapter.notifyDataSetChanged();

                        // When the lens is added jump to view the last entry
                        mainListView.setSelection(mainListView.getCount() - 1);
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    // Do nothing.
                    return;
                }
                break;

            case EDIT_FILTER:

                if (resultCode == Activity.RESULT_OK) {

                    Filter filter = data.getParcelableExtra("FILTER");

                    if (filter.getMake().length() > 0 &&
                            filter.getModel().length() > 0 &&
                            filter.getId() > 0) {

                        database.updateFilter(filter);

                        filterAdapter.notifyDataSetChanged();

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

    void showSelectMountableLensesDialog(int position){
        final Filter filter = filterList.get(position);
        final List<Lens> mountableLenses = database.getMountableLenses(filter);
        final List<Lens> allLenses = database.getAllLenses();

        // Make a list of strings for all the lens names to be showed in the
        // multi choice list.
        // Also make an array list containing all the lens id's for list comparison.
        // Comparing lists containing lenses is not easy.
        List<String> listItems = new ArrayList<>();
        List<Long> allLensesId = new ArrayList<>();
        for (int i = 0; i < allLenses.size(); ++i) {
            listItems.add(allLenses.get(i).getMake() + " " + allLenses.get(i).getModel());
            allLensesId.add(allLenses.get(i).getId());
        }

        // Make an array list containing all mountable lens id's.
        List<Long> mountableLensesId = new ArrayList<>();
        for (int i = 0; i < mountableLenses.size(); ++i) {
            mountableLensesId.add(mountableLenses.get(i).getId());
        }

        // Find the items in the list to be preselected
        final boolean[] booleans = new boolean[allLenses.size()];
        for (int i= 0; i < allLensesId.size(); ++i) {
            booleans[i] = mountableLensesId.contains(allLensesId.get(i));
        }



        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // MULTIPLE CHOICE DIALOG

        // Create an array list where the selections are saved. Initialize it with
        // the booleans array.
        final List<Integer> selectedItemsIndexList = new ArrayList<>();
        for (int i = 0; i < booleans.length; ++i) {
            if ( booleans[i] ) selectedItemsIndexList.add(i);
        }

        builder.setTitle(R.string.SelectMountableLenses)
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
                        for (int i = 0; i < allLenses.size(); ++i) {
                            if (!selectedItemsIndexList.contains(i))
                                notSelectedItemsIndexList.add(i);
                        }

                        // Iterate through the selected items
                        for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = selectedItemsIndexList.get(i);
                            Lens lens = allLenses.get(which);
                            database.addMountableFilterLens(filter, lens);
                        }

                        // Iterate through the not selected items
                        for (int i = notSelectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = notSelectedItemsIndexList.get(i);
                            Lens lens = allLenses.get(which);
                            database.deleteMountableFilterLens(filter, lens);
                        }
                        filterAdapter.notifyDataSetChanged();

                        // Update the LensesFragment through the parent activity.
                        GearActivity myActivity = (GearActivity)getActivity();
                        myActivity.updateFragments();
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
        if (filterAdapter != null) filterAdapter.notifyDataSetChanged();
    }
}
