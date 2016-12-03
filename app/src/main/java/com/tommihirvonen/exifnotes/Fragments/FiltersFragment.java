package com.tommihirvonen.exifnotes.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
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

import com.tommihirvonen.exifnotes.Adapters.FilterAdapter;
import com.tommihirvonen.exifnotes.Datastructures.Filter;
import com.tommihirvonen.exifnotes.Dialogs.EditGearInfoDialog;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Copyright 2016
// Tommi Hirvonen

public class FiltersFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {

    TextView mainTextView;
    ListView mainListView;
    FilterAdapter mArrayAdapter;
    ArrayList<Filter> mFilterList;
    FilmDbHelper database;
    public static final int ADD_FILTER = 1;
    public static final int EDIT_FILTER = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        database = new FilmDbHelper(getActivity());
        mFilterList = database.getAllFilters();

        final View view = linf.inflate(R.layout.filters_fragment, container, false);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_filters);
        fab.setOnClickListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        mainTextView = (TextView) view.findViewById(R.id.no_added_filters);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.main_filterslistview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new FilterAdapter(getActivity(), android.R.layout.simple_list_item_1, mFilterList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        registerForContextMenu(mainListView);

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(2);

        if ( mFilterList.size() >= 1 ) mainTextView.setVisibility(View.GONE);

        mArrayAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_delete_edit, menu);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Filter filter = mFilterList.get(i);

        EditGearInfoDialog dialog = new EditGearInfoDialog();
        dialog.setTargetFragment(this, EDIT_FILTER);
        Bundle arguments = new Bundle();
        arguments.putString("TITLE", getResources().getString( R.string.EditFilter));
        arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.OK));
        arguments.putString("MAKE", filter.getMake());
        arguments.putString("MODEL", filter.getModel());
        arguments.putInt("GEAR_ID", filter.getId());
        arguments.putInt("POSITION", i);
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditGearInfoDialog.TAG);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fab_filters:
                showFilterNameDialog();
                break;
        }
    }

    public void showFilterNameDialog() {
        EditGearInfoDialog dialog = new EditGearInfoDialog();
        dialog.setTargetFragment(this, ADD_FILTER);
        Bundle arguments = new Bundle();
        arguments.putString("TITLE", getResources().getString( R.string.NewFilter));
        arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditGearInfoDialog.TAG);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if ( getUserVisibleHint() ) {

            int which = info.position;
            Filter filter = mFilterList.get(which);

            switch (item.getItemId()) {

                case R.id.menu_item_delete:

                    // Check if the filter is being used with one of the rolls.
                    if (database.isFilterBeingUsed(filter)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.FilterNoColon) + " " + filter.getMake() + " " + filter.getModel() + " " + getResources().getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    database.deleteFilter(filter);

                    // Remove the roll from the mLensList. Do this last!!!
                    mFilterList.remove(which);

                    if (mFilterList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                    mArrayAdapter.notifyDataSetChanged();

                    return true;

                case R.id.menu_item_edit:

                    EditGearInfoDialog dialog = new EditGearInfoDialog();
                    dialog.setTargetFragment(this, EDIT_FILTER);
                    Bundle arguments = new Bundle();
                    arguments.putString("TITLE", getResources().getString( R.string.EditFilter));
                    arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.OK));
                    arguments.putString("MAKE", filter.getMake());
                    arguments.putString("MODEL", filter.getModel());
                    arguments.putInt("GEAR_ID", filter.getId());
                    arguments.putInt("POSITION", which);
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), EditGearInfoDialog.TAG);

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

                    String inputTextMake = data.getStringExtra("MAKE");
                    String inputTextModel = data.getStringExtra("MODEL");

                    if ( inputTextMake.length() != 0 && inputTextModel.length() != 0 ) {

                        // Check if a filter with the same name already exists
                        for ( int i = 0; i < mFilterList.size(); ++i ) {
                            if ( inputTextMake.equals( mFilterList.get(i).getMake()) && inputTextModel.equals(mFilterList.get(i).getModel())  ) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.FilterSameName), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }

                        mainTextView.setVisibility(View.GONE);

                        Filter filter = new Filter();
                        filter.setMake(inputTextMake);
                        filter.setModel(inputTextModel);
                        database.addFilter(filter);
                        // When we get the last added lens from the database we get the row id value.
                        filter = database.getLastFilter();
                        mFilterList.add(filter);
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

            case EDIT_FILTER:

                if (resultCode == Activity.RESULT_OK) {

                    String newMake = data.getStringExtra("MAKE");
                    String newModel = data.getStringExtra("MODEL");
                    int gearId = data.getIntExtra("GEAR_ID", -1);
                    int position = data.getIntExtra("POSITION", -1);

                    if ( gearId != -1 && position != -1 && newMake.length() > 0 && newModel.length() > 0 ) {

                        // Check if a filter with the same name already exists
                        for ( int i = 0; i < mFilterList.size(); ++i ) {
                            if ( newMake.equals( mFilterList.get(i).getMake()) && newModel.equals(mFilterList.get(i).getModel())  ) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.FilterSameName), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }

                        Filter filter = mFilterList.get(position);
                        filter.setMake(newMake);
                        filter.setModel(newModel);

                        database.updateFilter(filter);

                        mArrayAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(getActivity(), "Something went wrong :(", Toast.LENGTH_SHORT).show();
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){

                    return;
                }

                break;
        }
    }
}
