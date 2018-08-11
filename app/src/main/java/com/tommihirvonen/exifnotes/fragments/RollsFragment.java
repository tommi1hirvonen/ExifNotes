package com.tommihirvonen.exifnotes.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.activities.MainActivity;
import com.tommihirvonen.exifnotes.adapters.RollAdapter;
import com.tommihirvonen.exifnotes.activities.AllFramesMapsActivity;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.dialogs.EditRollDialog;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.GearActivity;
import com.tommihirvonen.exifnotes.activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.FilterMode;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.datastructures.RollSortMode;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * RollsFragment is the fragment that is displayed first in MainActivity. It contains
 * a list of rolls the user has saved in the database.
 */
public class RollsFragment extends Fragment implements
        View.OnClickListener,
        RollAdapter.RollAdapterListener {

    /**
     * Public constant used to tag the fragment when created
     */
    public static final String ROLLS_FRAGMENT_TAG = "ROLLS_FRAGMENT";

    /**
     * Constant passed to EditRollDialog for result
     */
    private static final int ROLL_DIALOG = 1;

    /**
     * Constant passed to EditRollDialog for result
     */
    private static final int EDIT_ROLL_DIALOG = 2;

    /**
     * Reference to the parent activity's OnRollSelectedListener
     */
    private OnRollSelectedListener callback;

    /**
     * Reference to the FloatingActionButton
     */
    private FloatingActionButton floatingActionButton;

    /**
     * TextView to show that no rolls have been added to the database
     */
    private TextView mainTextView;

    /**
     * ListView to show all the rolls in the database along with details
     */
    private RecyclerView mainRecyclerView;

    /**
     * Adapter used to adapt rollList to mainRecyclerView
     */
    private RollAdapter rollAdapter;

    /**
     * Contains all rolls from the database
     */
    private List<Roll> rollList = new ArrayList<>();

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * Private callback class which is given as an argument when the SupportActionMode is started.
     */
    private final ActionModeCallback actionModeCallback = new ActionModeCallback();

    /**
     * Reference to the (Support)ActionMode, which is launched when a list item is long pressed.
     */
    private ActionMode actionMode;

    /**
     * Holds the roll filter status (archived, active or all rolls).
     * This way we don't always have to query the value from SharedPreferences.
     */
    private FilterMode filterMode;

    /**
     * Holds the roll sort mode (date, name or camera).
     * This way we don't always have to query the value from SharedPreferences.
     */
    private RollSortMode sortMode;

    /**
     * This interface is implemented in MainActivity.
     */
    public interface OnRollSelectedListener{
        void onRollSelected(long rollId);
    }

    /**
     * This on attach is called before API 23
     *
     * @param a Activity to which the onRollSelectedListener is attached.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        callback = (OnRollSelectedListener) a;
    }

    /**
     * This on attach is called after API 23
     *
     * @param c Context to which the onRollSelectedListener is attached.
     */
    @Override
    public void onAttach(Context c) {
        super.onAttach(c);
        callback = (OnRollSelectedListener) c;
    }

    /**
     * Called when the fragment is created.
     * Tell the fragment that it has an options menu so that we can handle
     * OptionsItemSelected events.
     *
     * @param savedInstanceState passed to super.onCreate to execute necessary code to properly
     *                           create the fragment
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Inflate the fragment. Get all rolls from the database. Set the UI objects
     * and display all the rolls in the ListView.
     *
     * @param inflater {@inheritDoc}
     * @param container {@inheritDoc}
     * @param savedInstanceState possible saved state in case the fragment was resumed
     * @return The inflated view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set the ActionBar title text.
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            //noinspection ConstantConditions
            actionBar.setTitle("  " + getResources().getString(R.string.MainActivityTitle));
            //noinspection ConstantConditions
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        // Assign the database.
        database = FilmDbHelper.getInstance(getActivity());
        // Inflate the layout view.
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        final View view = layoutInflater.inflate(R.layout.fragment_rolls, container, false);
        // Assign the FloatingActionButton and set this activity to react to the fab being pressed.
        floatingActionButton = view.findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);
        // Assign the main TextView.
        mainTextView = view.findViewById(R.id.no_added_rolls);
        // Assign the main film roll RecyclerView.
        mainRecyclerView = view.findViewById(R.id.rolls_recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mainRecyclerView.setLayoutManager(layoutManager);
        mainRecyclerView.addItemDecoration(new DividerItemDecoration(mainRecyclerView.getContext(), layoutManager.getOrientation()));
        // Also change the floating action button color. Use the darker secondaryColor for this.
        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));
        // Use the updateFragment() method to load the film rolls from the database,
        // create an ArrayAdapter to link the list of rolls to the ListView,
        // update the ActionBar subtitle and main TextView and set the main TextView
        // either visible or hidden.
        updateFragment(true);
        // Return the inflated view.
        return view;
    }

    /**
     * Public method to update the contents of this fragment.
     */
    public void updateFragment(boolean recreateRollAdapter){

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        // Get from preferences which rolls to load from the database.
        filterMode = FilterMode.fromValue(
                sharedPreferences.getInt(PreferenceConstants.KEY_VISIBLE_ROLLS, FilterMode.ACTIVE.getValue()));
        sortMode = RollSortMode.fromValue(
                sharedPreferences.getInt(PreferenceConstants.KEY_ROLL_SORT_ORDER, RollSortMode.DATE.getValue()));

        // Declare variables for the ActionBar subtitle, which shows the film roll filter status
        // and the main TextView, which is displayed if no rolls are shown.
        final String subtitleText;
        final String mainTextViewText;
        switch (filterMode) {
            case ACTIVE: default:
                subtitleText = getResources().getString(R.string.ActiveFilmRolls);
                mainTextViewText = getResources().getString(R.string.NoActiveRolls);
                floatingActionButton.show();
                break;
            case ARCHIVED:
                subtitleText = getResources().getString(R.string.ArchivedFilmRolls);
                mainTextViewText = getResources().getString(R.string.NoArchivedRolls);
                floatingActionButton.hide();
                break;
            case ALL:
                subtitleText = getResources().getString(R.string.AllFilmRolls);
                mainTextViewText = getResources().getString(R.string.NoActiveOrArchivedRolls);
                floatingActionButton.show();
                break;
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        // Set the ActionBar subtitle.
        if (actionBar != null) {
            //noinspection ConstantConditions
            actionBar.setSubtitle("   " + subtitleText);
        }

        // Set the main TextView text.
        mainTextView.setText(mainTextViewText);

        // Load the rolls from the database.
        rollList = database.getRolls(filterMode);
        //Order the roll list according to preferences.

        Utilities.sortRollList(sortMode, database, rollList);
        if (recreateRollAdapter) {
            // Create an ArrayAdapter for the ListView.
            rollAdapter = new RollAdapter(getActivity(), rollList, this);
            // Set the ListView to use the ArrayAdapter.
            mainRecyclerView.setAdapter(rollAdapter);
            // Notify the adapter to update itself.
            rollAdapter.notifyDataSetChanged();
        } else {
            // rollAdapter still references the old rollList. Update its reference.
            rollAdapter.setRollList(rollList);
            // Notify the adapter to update itself
            rollAdapter.notifyDataSetChanged();
        }
        if (rollList.size() > 0) mainTextViewAnimateInvisible();
        else mainTextViewAnimateVisible();
    }

    /**
     * When resuming RollsFragment we have to color the FloatingActionButton and
     * notify the array adapter that the displayed amount of frames has changed for some roll.
     */
    @Override
    public void onResume(){
        super.onResume();
        rollAdapter.notifyDataSetChanged();
        int secondaryColor = Utilities.getSecondaryUiColor(getActivity().getApplicationContext());
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));
        // If action mode is enabled, color the status bar dark grey.
        if (rollAdapter.getSelectedItemCount() > 0 || actionMode != null) {
            Utilities.setStatusBarColor(getActivity(), ContextCompat.getColor(getActivity(), R.color.dark_grey));
        }
    }

    /**
     * Inflate the action bar many layout for RollsFragment.
     *
     * @param menu the menu to be inflated
     * @param inflater the MenuInflater from Activity
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_rolls_fragment, menu);
    }

    /**
     * Called after onCreateOptionsMenu() to prepare the menu.
     * Check the correct filter and sort options.
     *
     * @param menu reference to the menu that is to be prepared
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        switch (filterMode) {
            case ACTIVE: default:
                menu.findItem(R.id.active_rolls_filter).setChecked(true);
                break;
            case ARCHIVED:
                menu.findItem(R.id.archived_rolls_filter).setChecked(true);
                break;
            case ALL:
                menu.findItem(R.id.all_rolls_filter).setChecked(true);
                break;
        }

        switch (sortMode) {

            case DATE: default:
                menu.findItem(R.id.date_sort_mode).setChecked(true);
                break;

            case NAME:
                menu.findItem(R.id.name_sort_mode).setChecked(true);
                break;

            case CAMERA:
                menu.findItem(R.id.camera_sort_mode).setChecked(true);
                break;
        }

        super.onPrepareOptionsMenu(menu);
    }

    /**
     * Handle events when the user selects an action from the options menu.
     *
     * @param item selected menu item.
     * @return true because the item selection was consumed/handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            case R.id.menu_item_gear:

                Intent gearActivityIntent = new Intent(getActivity(), GearActivity.class);
                startActivity(gearActivityIntent);
                break;

            case R.id.menu_item_preferences:

                Intent preferenceActivityIntent = new Intent(getActivity(), PreferenceActivity.class);
                // With these extras we can skip the headers in the preferences.
                preferenceActivityIntent.putExtra(
                        PreferenceActivity.EXTRA_SHOW_FRAGMENT, PreferenceFragment.class.getName());
                preferenceActivityIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);

                //Start the preference activity from MainActivity.
                //The result will be handled in MainActivity.
                getActivity().startActivityForResult(preferenceActivityIntent, MainActivity.PREFERENCE_ACTIVITY_REQUEST);
                break;

            case R.id.menu_item_help:

                String helpTitle = getResources().getString(R.string.Help);
                String helpMessage = getResources().getString(R.string.main_help);
                Utilities.showGeneralDialog(getActivity(), helpTitle, helpMessage);
                break;

            case R.id.menu_item_about:

                String aboutTitle = getResources().getString(R.string.app_name);
                String aboutMessage = getResources().getString(R.string.about) + "\n\n\n" +
                        getResources().getString(R.string.VersionHistory);
                Utilities.showGeneralDialog(getActivity(), aboutTitle, aboutMessage);
                break;

            case R.id.menu_item_show_on_map:

                // Show all frames from all rolls on a map
                Intent allFramesMapsActivityIntent = new Intent(getActivity(), AllFramesMapsActivity.class);
                startActivity(allFramesMapsActivityIntent);
                break;

            case R.id.active_rolls_filter:
                item.setChecked(true);
                setFilterMode(FilterMode.ACTIVE);
                break;

            case R.id.archived_rolls_filter:
                item.setChecked(true);
                setFilterMode(FilterMode.ARCHIVED);
                break;

            case R.id.all_rolls_filter:
                item.setChecked(true);
                setFilterMode(FilterMode.ALL);
                break;

            case R.id.date_sort_mode:
                item.setChecked(true);
                setSortMode(RollSortMode.DATE);
                break;

            case R.id.name_sort_mode:
                item.setChecked(true);
                setSortMode(RollSortMode.NAME);
                break;

            case R.id.camera_sort_mode:
                item.setChecked(true);
                setSortMode(RollSortMode.CAMERA);
                break;

        }
        return true;
    }

    /**
     * Change the way visible rolls are filtered. Update SharedPreferences and the fragment.
     *
     * @param filterMode enum type referencing the filtering mode
     */
    private void setFilterMode(FilterMode filterMode) {
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PreferenceConstants.KEY_VISIBLE_ROLLS, filterMode.getValue());
        editor.apply();
        updateFragment(false);
    }

    /**
     * Change the sort order of rolls.
     *
     * @param sortMode enum type referencing the sorting mode
     */
    private void setSortMode(RollSortMode sortMode) {
        final SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PreferenceConstants.KEY_ROLL_SORT_ORDER, sortMode.getValue());
        editor.apply();
        Utilities.sortRollList(sortMode, database, rollList);
        rollAdapter.notifyDataSetChanged();
    }

    /**
     * Called when FloatingActionButton is pressed.
     * Show the user the RollNameDialog to add a new roll.
     *
     * @param v view which was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                showRollDialog();
                break;
        }
    }

    /**
     * Called when a roll is pressed.
     * If the action mode is enabled, add the pressed item to the selected items.
     * Otherwise forward the press to the callback interface in MainActivity.
     *
     * @param position position of the item in RollAdapter
     */
    @Override
    public void onItemClick(int position) {
        if (rollAdapter.getSelectedItemCount() > 0 || actionMode != null) {
            enableActionMode(position);
        } else {
            long rollId = rollList.get(position).getId();
            callback.onRollSelected(rollId);
        }
    }

    /**
     * When an item is long pressed, always add the pressed item to selected items.
     *
     * @param position position of the item in RollAdapter
     */
    @Override
    public void onItemLongClick(int position) {
        enableActionMode(position);
    }

    /**
     * Enable ActionMode is not yet enabled and add item to selected items.
     * Hide edit menu item, if more than one items are selected.
     *
     * @param position position of the item in RollAdapter
     */
    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        rollAdapter.toggleSelection(position);
        // If the user deselected the last of the selected items, exit action mode.
        if (rollAdapter.getSelectedItemCount() == 0) {
            actionMode.finish();
        } else {
            // Set the visibility of edit item depending on whether only one roll was selected.
            final boolean visible = rollAdapter.getSelectedItemCount() == 1;
            actionMode.getMenu().findItem(R.id.menu_item_edit).setVisible(visible);
            // Set the action mode toolbar title to display the number of selected items.
            actionMode.setTitle(Integer.toString(rollAdapter.getSelectedItemCount()) + "/"
                    + Integer.toString(rollAdapter.getItemCount()));
        }
    }

    /**
     * Called when the user long presses on a roll and chooses
     * to edit a roll's information. Shows a DialogFragment to edit
     * the roll's information.
     *
     * @param position the position of the roll in rollList
     */
    @SuppressLint("CommitTransaction")
    private void showEditRollDialog(int position){
        EditRollDialog dialog = new EditRollDialog();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ExtraKeys.ROLL, rollList.get(position));
        arguments.putString(ExtraKeys.TITLE, getActivity().getResources().getString(R.string.EditRoll));
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, getActivity().getResources().getString(R.string.OK));
        dialog.setArguments(arguments);
        dialog.setTargetFragment(this, EDIT_ROLL_DIALOG);
        dialog.show(getFragmentManager().beginTransaction(), EditRollDialog.TAG);
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Shows a DialogFragment to add a new roll.
     */
    @SuppressLint("CommitTransaction")
    private void showRollDialog() {
        EditRollDialog dialog = new EditRollDialog();
        Bundle arguments = new Bundle();
        arguments.putString(ExtraKeys.TITLE, getActivity().getResources().getString(R.string.NewRoll));
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, getActivity().getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.setTargetFragment(this, ROLL_DIALOG);
        dialog.show(getFragmentManager().beginTransaction(), EditRollDialog.TAG);
    }

    /**
     * Called when the user is done editing or adding a roll and
     * closes the dialog. Handle roll addition and edit differently.
     *
     * @param requestCode the request code that was set for the intent
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed Intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case ROLL_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    Roll roll = data.getParcelableExtra(ExtraKeys.ROLL);
                    long rowId = database.addRoll(roll);
                    roll.setId(rowId);

                    mainTextViewAnimateInvisible();
                    // Add new roll to the top of the list
                    rollList.add(0, roll);
                    Utilities.sortRollList(sortMode, database, rollList);
                    rollAdapter.notifyItemInserted(rollList.indexOf(roll));

                    // When the new roll is added jump to view the added entry
                    int pos = rollList.indexOf(roll);
                    if (pos < rollAdapter.getItemCount()) mainRecyclerView.scrollToPosition(pos);

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;

            case EDIT_ROLL_DIALOG:

                if (resultCode == Activity.RESULT_OK) {

                    if (actionMode != null) actionMode.finish();

                    Roll roll = data.getParcelableExtra(ExtraKeys.ROLL);
                    database.updateRoll(roll);
                    // Notify array adapter that the dataset has to be updated
                    final int oldPosition = rollList.indexOf(roll);
                    Utilities.sortRollList(sortMode, database, rollList);
                    final int newPosition = rollList.indexOf(roll);
                    rollAdapter.notifyItemChanged(oldPosition);
                    rollAdapter.notifyItemMoved(oldPosition, newPosition);

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;
        }
    }

    /**
     * Method to fade in the main TextView ("No rolls")
     */
    private void mainTextViewAnimateVisible() {
        mainTextView.animate().alpha(1.0f).setDuration(150);
    }

    /**
     * Method to fade out the main TextView ("No rolls")
     */
    private void mainTextViewAnimateInvisible() {
        mainTextView.animate().alpha(0.0f).setDuration(0);
    }

    /**
     * Class which implements ActionMode.Callback.
     * One instance of this class is given as an argument when ActionMode is started.
     */
    private class ActionModeCallback implements ActionMode.Callback {

        /**
         * Called when the ActionMode is started.
         * Inflate the menu and set the visibility of some menu items.
         *
         * @param actionMode {@inheritDoc}
         * @param menu {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

            // Set the status bar color to be dark grey to complement the grey action mode toolbar.
            Utilities.setStatusBarColor(getActivity(), ContextCompat.getColor(getActivity(), R.color.dark_grey));

            // Hide the floating action button so no new rolls can be added while in action mode.
            floatingActionButton.hide();

            // Use different action mode menu layouts depending on which rolls are shown.
            if (filterMode == FilterMode.ACTIVE)
                actionMode.getMenuInflater().inflate(R.menu.menu_action_mode_rolls_active, menu);
            else if (filterMode == FilterMode.ARCHIVED)
                actionMode.getMenuInflater().inflate(R.menu.menu_action_mode_rolls_archived, menu);
            else
                actionMode.getMenuInflater().inflate(R.menu.menu_action_mode_rolls_all, menu);

            return true;
        }

        /**
         * Called to refresh the ActionMode menu whenever it is invalidated.
         *
         * @param actionMode {@inheritDoc}
         * @param menu {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        /**
         * Called when the user presses on an action menu item.
         *
         * @param actionMode {@inheritDoc}
         * @param menuItem {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            // Get the positions in the rollList of selected items
            final List<Integer> selectedItemPositions = rollAdapter.getSelectedItemPositions();

            switch (menuItem.getItemId()) {

                case R.id.menu_item_delete:

                    // Set the confirm dialog title depending on whether one or more rolls were selected
                    final String title = selectedItemPositions.size() == 1 ?
                            getResources().getString(R.string.ConfirmRollDelete) + " \'" + rollList.get(selectedItemPositions.get(0)).getName() + "\'?" :
                            String.format(getResources().getString(R.string.ConfirmRollsDelete), selectedItemPositions.size());
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setTitle(title);
                    alertBuilder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    });
                    alertBuilder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                                final int rollPosition = selectedItemPositions.get(i);
                                final Roll roll = rollList.get(rollPosition);
                                // Delete the roll. Database foreign key rules make sure,
                                // that any linked frames are deleted as well.
                                database.deleteRoll(roll);
                                // Remove the roll from the rollList. Do this last!!!
                                rollList.remove(rollPosition);

                                if (rollList.size() == 0) mainTextViewAnimateVisible();
                                rollAdapter.notifyItemRemoved(rollPosition);
                            }
                            actionMode.finish();
                        }
                    });
                    alertBuilder.create().show();
                    return true;

                case R.id.menu_item_select_all:

                    rollAdapter.toggleSelectionAll();
                    mainRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            rollAdapter.resetAnimateAll();
                        }
                    });
                    actionMode.setTitle(Integer.toString(rollAdapter.getSelectedItemCount()) + "/"
                            + Integer.toString(rollAdapter.getItemCount()));
                    // Set the edit item visibility to false because all rolls are selected.
                    actionMode.getMenu().findItem(R.id.menu_item_edit).setVisible(false);
                    return true;

                case R.id.menu_item_edit:

                    // Get the first of the selected rolls (only one should be selected anyway)
                    // Finish action mode if the user clicked ok when editing the roll ->
                    // this is done in onActivityResult().
                    showEditRollDialog(selectedItemPositions.get(0));
                    return true;

                case R.id.menu_item_archive:

                    for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                        final int position = selectedItemPositions.get(i);
                        final Roll roll = rollList.get(position);
                        roll.setArchived(true);
                        database.updateRoll(roll);
                        if (filterMode == FilterMode.ACTIVE) {
                            rollList.remove(position);
                            rollAdapter.notifyItemRemoved(position);
                        }
                    }
                    if (rollList.size() == 0) mainTextViewAnimateVisible();
                    actionMode.finish();
                    Toast.makeText(getActivity(), getResources().getString(R.string.RollsArchived), Toast.LENGTH_SHORT).show();
                    return true;

                case R.id.menu_item_unarchive:

                    for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                        final int position = selectedItemPositions.get(i);
                        final Roll roll = rollList.get(position);
                        roll.setArchived(false);
                        database.updateRoll(roll);
                        if (filterMode == FilterMode.ARCHIVED) {
                            rollList.remove(position);
                            rollAdapter.notifyItemRemoved(position);
                        }
                    }
                    if (rollList.size() == 0) mainTextViewAnimateVisible();
                    actionMode.finish();
                    Toast.makeText(getActivity(), getResources().getString(R.string.RollsActivated), Toast.LENGTH_SHORT).show();
                    return true;

                default:
                    return false;
            }
        }

        /**
         * Called when an action mode is about to be exited and destroyed.
         *
         * @param mode {@inheritDoc}
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            rollAdapter.clearSelections();
            actionMode = null;
            mainRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    rollAdapter.resetAnimationIndex();
                }
            });
            // Return the status bar to its original color before action mode.
            Utilities.setStatusBarColor(getActivity(), Utilities.getSecondaryUiColor(getActivity()));
            // Make the floating action bar visible again since action mode is exited.
            floatingActionButton.show();
        }
    }

}