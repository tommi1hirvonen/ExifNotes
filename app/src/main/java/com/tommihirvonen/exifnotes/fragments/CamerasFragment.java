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
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.dialogs.EditCameraDialog;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.GearActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment to display all cameras from the database along with details
 */
public class CamerasFragment extends Fragment implements View.OnClickListener {

    /**
     * Constant passed to EditCameraDialog for result
     */
    public static final int ADD_CAMERA = 1;

    /**
     * Constant passed to EditCameraDialog for result
     */
    private static final int EDIT_CAMERA = 2;

    /**
     * TextView to show that no cameras have been added to the database
     */
    private TextView mainTextView;

    /**
     * ListView to show all the cameras in the database along with details
     */
    private RecyclerView mainRecyclerView;

    /**
     * Adapter used to adapt cameraList to mainRecyclerView
     */
    private GearAdapter cameraAdapter;

    /**
     * Contains all cameras from the database
     */
    private List<Camera> cameraList;

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
     * Inflate the fragment. Get all cameras from the database. Set the UI objects
     * and display all cameras in the ListView.
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
        cameraList = database.getAllCameras();
        Utilities.sortGearList(cameraList);

        final View view = layoutInflater.inflate(R.layout.fragment_cameras, container, false);

        final FloatingActionButton floatingActionButton = view.findViewById(R.id.fab_cameras);
        floatingActionButton.setOnClickListener(this);

        final int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = view.findViewById(R.id.no_added_cameras);

        // Access the ListView
        mainRecyclerView = view.findViewById(R.id.cameras_recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mainRecyclerView.setLayoutManager(layoutManager);
        mainRecyclerView.addItemDecoration(new DividerItemDecoration(mainRecyclerView.getContext(),
                layoutManager.getOrientation()));

        // Create an ArrayAdapter for the ListView
        cameraAdapter = new GearAdapter(getActivity(), cameraList);

        // Set the ListView to use the ArrayAdapter
        mainRecyclerView.setAdapter(cameraAdapter);

        if (cameraList.size() >= 1) mainTextView.setVisibility(View.GONE);

        cameraAdapter.notifyDataSetChanged();

        return view;
    }

    /**
     * Called when the user long presses on a camera AND selects a context menu item.
     *
     * @param item the context menu item that was selected
     * @return true if CamerasFragment is in front, false if not
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
            final Camera camera = cameraList.get(position);

            switch (item.getItemId()) {

                case R.id.menu_item_select_mountable_lenses:

                    showSelectMountableLensesDialog(position);
                    return true;

                case R.id.menu_item_delete:

                    // Check if the camera is being used with one of the rolls.
                    if (database.isCameraBeingUsed(camera)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.CameraNoColon) +
                                " " + camera.getName() + " " +
                                getResources().getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.ConfirmCameraDelete)
                            + " \'" + camera.getName() + "\'?"
                    );
                    builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
                        // Do nothing

                    });
                    builder.setPositiveButton(R.string.OK, (dialog, which) -> {

                        database.deleteCamera(camera);

                        // Remove the camera from the cameraList. Do this last!!!
                        cameraList.remove(position);

                        if (cameraList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                        cameraAdapter.notifyItemRemoved(position);

                        // Update the LensesFragment through the parent activity.
                        final GearActivity gearActivity = (GearActivity)getActivity();
                        gearActivity.updateFragments();

                    });
                    builder.create().show();

                    return true;

                case R.id.menu_item_edit:

                    final EditCameraDialog dialog = new EditCameraDialog();
                    dialog.setTargetFragment(this, EDIT_CAMERA);
                    final Bundle arguments = new Bundle();
                    arguments.putString(ExtraKeys.TITLE, getResources().getString( R.string.EditCamera));
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.OK));
                    arguments.putParcelable(ExtraKeys.CAMERA, camera);

                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), EditCameraDialog.TAG);

                    return true;
            }
        }
        return false;
    }

    /**
     * Show EditCameraDialog to add a new camera to the database
     */
    @SuppressLint("CommitTransaction")
    private void showCameraNameDialog() {
        final EditCameraDialog dialog = new EditCameraDialog();
        dialog.setTargetFragment(this, ADD_CAMERA);
        final Bundle arguments = new Bundle();
        arguments.putString(ExtraKeys.TITLE, getResources().getString( R.string.NewCamera));
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditCameraDialog.TAG);
    }

    /**
     * Called when the FloatingActionButton is pressed.
     *
     * @param v view which was clicked
     */
    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.fab_cameras) {
            showCameraNameDialog();
        }
    }

    /**
     * Called when the user is done editing or adding a camera and closes the dialog.
     * Handle camera addition and edit differently.
     *
     * @param requestCode the request code that was set for the intent
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed Intent
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch(requestCode) {

            case ADD_CAMERA:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    final Camera camera = data.getParcelableExtra(ExtraKeys.CAMERA);

                    if (camera.getMake().length() > 0 && camera.getModel().length() > 0) {

                        mainTextView.setVisibility(View.GONE);

                        final long rowId = database.addCamera(camera);
                        camera.setId(rowId);
                        cameraList.add(camera);
                        Utilities.sortGearList(cameraList);
                        final int listPos = cameraList.indexOf(camera);
                        cameraAdapter.notifyItemInserted(listPos);

                        // When the lens is added jump to view the last entry
                        mainRecyclerView.scrollToPosition(listPos);
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    // Do nothing.
                    return;
                }
                break;

            case EDIT_CAMERA:

                if (resultCode == Activity.RESULT_OK) {

                    final Camera camera = data.getParcelableExtra(ExtraKeys.CAMERA);

                    if (camera.getMake().length() > 0 &&
                            camera.getModel().length() > 0 &&
                            camera.getId() > 0) {

                        database.updateCamera(camera);
                        final int oldPos = cameraList.indexOf(camera);
                        Utilities.sortGearList(cameraList);
                        final int newPos = cameraList.indexOf(camera);
                        cameraAdapter.notifyItemChanged(oldPos);
                        cameraAdapter.notifyItemMoved(oldPos, newPos);
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
     * Show dialog where the user can select which lenses can be mounted to the picked camera.
     *
     * @param position indicates the position of the picked camera in cameraList
     */
    private void showSelectMountableLensesDialog(final int position){
        final Camera camera = cameraList.get(position);
        final List<Lens> mountableLenses = database.getLinkedLenses(camera);
        final List<Lens> allLenses = database.getAllLenses();

        // Make a list of strings for all the camera names to be showed in the
        // multi choice list.
        // Also make an array list containing all the camera id's for list comparison.
        // Comparing lists containing frames is not easy.
        final List<String> listItems = new ArrayList<>();
        final List<Long> allLensesId = new ArrayList<>();
        for (int i = 0; i < allLenses.size(); ++i) {
            listItems.add(allLenses.get(i).getName());
            allLensesId.add(allLenses.get(i).getId());
        }

        // Make an array list containing all mountable camera id's.
        final List<Long> mountableLensesId = new ArrayList<>();
        for (int i = 0; i < mountableLenses.size(); ++i) {
            mountableLensesId.add(mountableLenses.get(i).getId());
        }

        // Find the items in the list to be preselected
        final boolean[] booleans = new boolean[allLenses.size()];
        for (int i= 0; i < allLensesId.size(); ++i) {
            booleans[i] = mountableLensesId.contains(allLensesId.get(i));
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

        builder.setTitle(R.string.SelectMountableLenses)
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
                    for (int i = 0; i < allLenses.size(); ++i) {
                        if (!selectedItemsIndexList.contains(i))
                            notSelectedItemsIndexList.add(i);
                    }

                    // Iterate through the selected items
                    for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                        int which = selectedItemsIndexList.get(i);
                        Lens lens = allLenses.get(which);
                        database.addCameraLensLink(camera, lens);
                    }

                    // Iterate through the not selected items
                    for (int i = notSelectedItemsIndexList.size() - 1; i >= 0; --i) {
                        int which = notSelectedItemsIndexList.get(i);
                        Lens lens = allLenses.get(which);
                        database.deleteCameraLensLink(camera, lens);
                    }
                    cameraAdapter.notifyItemChanged(position);

                    // Update the LensesFragment through the parent activity.
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
        if (cameraAdapter != null) cameraAdapter.notifyDataSetChanged();
    }
}
