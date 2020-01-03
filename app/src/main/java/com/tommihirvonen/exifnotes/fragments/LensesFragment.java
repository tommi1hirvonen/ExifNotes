package com.tommihirvonen.exifnotes.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.adapters.GearAdapter;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.dialogs.EditLensDialog;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.GearActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment to display all lenses from the database along with details
 */
public class LensesFragment extends Fragment implements View.OnClickListener {

    /**
     * Constant passed to EditLensDialog for result
     */
    private static final int ADD_LENS = 1;

    /**
     * Constant passed to EditLensDialog for result
     */
    private static final int EDIT_LENS = 2;

    /**
     * TextView to show that no lenses have been added to the database
     */
    private TextView mainTextView;

    /**
     * ListView to show all the lenses in the database along with details
     */
    private RecyclerView mainRecyclerView;

    /**
     * Adapter used to adapt lensList to mainRecyclerView
     */
    private GearAdapter lensAdapter;

    /**
     * Contains all lenses from the database
     */
    private List<Lens> lensList = new ArrayList<>();

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    private boolean fragmentVisible = false;

    /**
     * Called when the fragment is created.
     * Tell the fragment that it has an options menu so that we can handle
     * OptionsItemSelected events.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        fragmentVisible = true;
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        fragmentVisible = false;
    }

    /**
     * Inflate the fragment. Get all lenses from the database. Set the UI objects
     * and display all lenses in the ListView.
     *
     * @param inflater {@inheritDoc}
     * @param container {@inheritDoc}
     * @param savedInstanceState {@inheritDoc}
     * @return the inflated view ready to be shown
     */
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        database = FilmDbHelper.getInstance(getActivity());
        lensList = database.getAllLenses();
        Utilities.sortGearList(lensList);

        final View view = layoutInflater.inflate(R.layout.fragment_lenses, container, false);

        final FloatingActionButton fab = view.findViewById(R.id.fab_lenses);
        fab.setOnClickListener(this);

        final int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = view.findViewById(R.id.no_added_lenses);

        // Access the ListView
        mainRecyclerView = view.findViewById(R.id.lenses_recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mainRecyclerView.setLayoutManager(layoutManager);
        mainRecyclerView.addItemDecoration(new DividerItemDecoration(mainRecyclerView.getContext(),
                layoutManager.getOrientation()));

        lensAdapter = new GearAdapter(getActivity(), lensList);
        mainRecyclerView.setAdapter(lensAdapter);

        if (lensList.size() >= 1) mainTextView.setVisibility(View.GONE);

        lensAdapter.notifyDataSetChanged();

        return view;
    }

    /**
     * Called when the user long presses on a lens AND selects a context menu item.
     *
     * @param item the context menu item that was selected
     * @return true if LensesFragment is in front, false if not
     */
    @SuppressLint("CommitTransaction")
    @Override
    public boolean onContextItemSelected(@NonNull final MenuItem item) {
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (fragmentVisible) {

            // Use the getOrder() method to unconventionally get the clicked item's position.
            // This is set to work correctly in the Adapter class.
            final int position = item.getOrder();
            final Lens lens = lensList.get(position);

            switch (item.getItemId()) {

                case GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_CAMERAS:

                    showSelectMountableCamerasDialog(position);
                    return true;

                case GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_FILTERS:

                    showSelectMountableFiltersDialog(position);
                    return true;

                case GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_DELETE:

                    // Check if the lens is being used with one of the frames.
                    if (database.isLensInUse(lens)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.LensNoColon) +
                                " " + lens.getName() + " " +
                                getResources().getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.ConfirmLensDelete)
                            + " \'" + lens.getName() + "\'?"
                    );
                    builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
                        // Do nothing

                    });
                    builder.setPositiveButton(R.string.OK, (dialog, which) -> {

                        database.deleteLens(lens);

                        // Remove the lens from the lensList. Do this last!!!
                        lensList.remove(position);

                        if (lensList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                        lensAdapter.notifyItemRemoved(position);

                        // Update the CamerasFragment through the parent activity.
                        final GearActivity gearActivity = (GearActivity)getActivity();
                        gearActivity.updateFragments();

                    });
                    builder.create().show();

                    return true;

                case GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_EDIT:

                    final EditLensDialog dialog = new EditLensDialog();
                    dialog.setTargetFragment(this, EDIT_LENS);
                    final Bundle arguments = new Bundle();
                    arguments.putString(ExtraKeys.TITLE, getResources().getString( R.string.EditLens));
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.OK));
                    arguments.putParcelable(ExtraKeys.LENS, lens);
                    arguments.putInt(ExtraKeys.POSITION, position);
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), EditLensDialog.TAG);

                    return true;
            }
        }
        return false;
    }

    /**
     * Show EditLensDialog to add a new lens to the database
     */
    @SuppressLint("CommitTransaction")
    private void showLensNameDialog() {
        final EditLensDialog dialog = new EditLensDialog();
        dialog.setTargetFragment(this, ADD_LENS);
        final Bundle arguments = new Bundle();
        arguments.putString(ExtraKeys.TITLE, getResources().getString( R.string.NewLens));
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditLensDialog.TAG);
    }

    /**
     * Called when the user is done editing or adding a lens and closes the dialog.
     * Handle lens addition and edit differently.
     *
     * @param requestCode the request code that was set for the intent
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed Intent
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch(requestCode) {

            case ADD_LENS:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    final Lens lens = data.getParcelableExtra(ExtraKeys.LENS);

                    if (lens.getMake().length() > 0 && lens.getModel().length() > 0) {

                        mainTextView.setVisibility(View.GONE);

                        final long rowId = database.addLens(lens);
                        lens.setId(rowId);
                        lensList.add(lens);
                        Utilities.sortGearList(lensList);
                        final int listPos = lensList.indexOf(lens);
                        lensAdapter.notifyItemInserted(listPos);

                        // When the lens is added jump to view the last entry
                        mainRecyclerView.scrollToPosition(listPos);
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    // Do nothing.
                    return;
                }

                break;

            case EDIT_LENS:

                if (resultCode == Activity.RESULT_OK) {

                    final Lens lens = data.getParcelableExtra(ExtraKeys.LENS);

                    if (lens.getMake().length() > 0 &&
                            lens.getModel().length() > 0 &&
                            lens.getId() > 0) {

                        database.updateLens(lens);
                        final int oldPos = lensList.indexOf(lens);
                        Utilities.sortGearList(lensList);
                        final int newPos = lensList.indexOf(lens);
                        lensAdapter.notifyItemChanged(oldPos);
                        lensAdapter.notifyItemMoved(oldPos, newPos);
                        mainRecyclerView.scrollToPosition(newPos);

                        // Update the LensesFragment through the parent activity.
                        final GearActivity gearActivity = (GearActivity)getActivity();
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

    /**
     * Called when the FloatingActionButton is pressed.
     *
     * @param v view which was clicked
     */
    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.fab_lenses) {
            showLensNameDialog();
        }
    }

    /**
     * Show dialog where the user can select which cameras can be mounted to the picked lens.
     *
     * @param position indicates the position of the picked lens in lensList
     */
    private void showSelectMountableCamerasDialog(final int position){
        final Lens lens = lensList.get(position);
        final List<Camera> mountableCameras = database.getLinkedCameras(lens);
        final List<Camera> allCameras = database.getAllCameras();

        // Make a list of strings for all the camera names to be showed in the
        // multi choice list.
        // Also make an array list containing all the camera id's for list comparison.
        // Comparing lists containing frames is not easy.
        final List<String> listItems = new ArrayList<>();
        final List<Long> allCamerasId = new ArrayList<>();
        for (int i = 0; i < allCameras.size(); ++i) {
            listItems.add(allCameras.get(i).getName());
            allCamerasId.add(allCameras.get(i).getId());
        }

        // Make an array list containing all mountable camera id's.
        final List<Long> mountableCamerasId = new ArrayList<>();
        for (int i = 0; i < mountableCameras.size(); ++i) {
            mountableCamerasId.add(mountableCameras.get(i).getId());
        }

        // Find the items in the list to be preselected
        final boolean[] booleans = new boolean[allCameras.size()];
        for (int i= 0; i < allCamerasId.size(); ++i) {
            booleans[i] = mountableCamerasId.contains(allCamerasId.get(i));
        }

        final CharSequence[] items = listItems.toArray(new CharSequence[0]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // MULTIPLE CHOICE DIALOG

        // Create an array list where the selections are saved. Initialize it with
        // the booleans array.
        final List<Integer> selectedItemsIndexList = new ArrayList<>();
        for (int i = 0; i < booleans.length; ++i) {
            if (booleans[i]) selectedItemsIndexList.add(i);
        }

        builder.setTitle(R.string.SelectMountableCameras)
                .setMultiChoiceItems(items, booleans, (dialog, which, isChecked) -> {
                    if (isChecked) {

                        // If the user checked the item, add it to the selected items
                        selectedItemsIndexList.add(which);

                    } else if (selectedItemsIndexList.contains(which)) {

                        // Else, if the item is already in the array, remove it
                        selectedItemsIndexList.remove(Integer.valueOf(which));

                    }
                })

                .setPositiveButton(R.string.OK, (dialog, id) -> {

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
                        database.addCameraLensLink(camera, lens);
                    }

                    // Iterate through the not selected items
                    for (int i = notSelectedItemsIndexList.size() - 1; i >= 0; --i) {
                        int which = notSelectedItemsIndexList.get(i);
                        Camera camera = allCameras.get(which);
                        database.deleteCameraLensLink(camera, lens);
                    }
                    lensAdapter.notifyItemChanged(position);

                    // Update the CamerasFragment through the parent activity.
                    GearActivity gearActivity = (GearActivity)getActivity();
                    gearActivity.updateFragments();
                })
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                    // Do nothing
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Show dialog where the user can select which filters can be mounted to the picked lens.
     *
     * @param position indicates the position of the picked lens in lensList
     */
    private void showSelectMountableFiltersDialog(final int position){
        final Lens lens = lensList.get(position);
        final List<Filter> mountableFilters = database.getLinkedFilters(lens);
        final List<Filter> allFilters = database.getAllFilters();

        // Make a list of strings for all the filter names to be showed in the
        // multi choice list.
        // Also make an array list containing all the filter id's for list comparison.
        // Comparing lists containing frames is not easy.
        final List<String> listItems = new ArrayList<>();
        final List<Long> allFiltersId = new ArrayList<>();
        for (int i = 0; i < allFilters.size(); ++i) {
            listItems.add(allFilters.get(i).getName());
            allFiltersId.add(allFilters.get(i).getId());
        }

        // Make an array list containing all mountable filter id's.
        final List<Long> mountableFiltersId = new ArrayList<>();
        for (int i = 0; i < mountableFilters.size(); ++i) {
            mountableFiltersId.add(mountableFilters.get(i).getId());
        }

        // Find the items in the list to be preselected
        final boolean[] booleans = new boolean[allFilters.size()];
        for (int i= 0; i < allFiltersId.size(); ++i) {
            booleans[i] = mountableFiltersId.contains(allFiltersId.get(i));
        }



        final CharSequence[] items = listItems.toArray(new CharSequence[0]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // MULTIPLE CHOICE DIALOG

        // Create an array list where the selections are saved. Initialize it with
        // the booleans array.
        final List<Integer> selectedItemsIndexList = new ArrayList<>();
        for (int i = 0; i < booleans.length; ++i) {
            if (booleans[i]) selectedItemsIndexList.add(i);
        }

        builder.setTitle(R.string.SelectMountableFilters)
                .setMultiChoiceItems(items, booleans, (dialog, which, isChecked) -> {
                    if (isChecked) {

                        // If the user checked the item, add it to the selected items
                        selectedItemsIndexList.add(which);

                    } else if (selectedItemsIndexList.contains(which)) {

                        // Else, if the item is already in the array, remove it
                        selectedItemsIndexList.remove(Integer.valueOf(which));

                    }
                })

                .setPositiveButton(R.string.OK, (dialog, id) -> {

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
                        database.addLensFilterLink(filter, lens);
                    }

                    // Iterate through the not selected items
                    for (int i = notSelectedItemsIndexList.size() - 1; i >= 0; --i) {
                        int which = notSelectedItemsIndexList.get(i);
                        Filter filter = allFilters.get(which);
                        database.deleteLensFilterLink(filter, lens);
                    }
                    lensAdapter.notifyItemChanged(position);

                    // Update the FiltersFragment through the parent activity.
                    GearActivity gearActivity = (GearActivity)getActivity();
                    gearActivity.updateFragments();
                })
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                    // Do nothing
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Public method to update the contents of this fragment's ListView
     */
    public void updateFragment(){
        if (lensAdapter != null) lensAdapter.notifyDataSetChanged();
    }
}
