package com.tommihirvonen.exifnotes.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.activities.MainActivity;
import com.tommihirvonen.exifnotes.activities.MapActivity;
import com.tommihirvonen.exifnotes.adapters.RollAdapter;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.dialogs.EditRollDialog;
import com.tommihirvonen.exifnotes.dialogs.SelectFilmStockDialog;
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
    private static final int REQUEST_CODE_ADD_ROLL = 1;

    /**
     * Constant passed to EditRollDialog for result
     */
    private static final int REQUEST_CODE_EDIT_ROLL = 2;

    private static final int REQUEST_CODE_BATCH_EDIT_FILM_STOCK = 3;

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
    public void onAttach(@NonNull final Activity a) {
        super.onAttach(a);
        callback = (OnRollSelectedListener) a;
    }

    /**
     * This on attach is called after API 23
     *
     * @param c Context to which the onRollSelectedListener is attached.
     */
    @Override
    public void onAttach(@NonNull final Context c) {
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
    public void onCreate(final Bundle savedInstanceState) {
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
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Set the ActionBar title text.
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("  " + getResources().getString(R.string.MainActivityTitle));
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        // Assign the database.
        database = FilmDbHelper.getInstance(getActivity());
        // Inflate the layout view.
        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
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
        mainRecyclerView.addItemDecoration(new DividerItemDecoration(mainRecyclerView.getContext(),
                layoutManager.getOrientation()));
        // Also change the floating action button color. Use the darker secondaryColor for this.
        final int secondaryColor = Utilities.getSecondaryUiColor(getActivity());
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
    public void updateFragment(final boolean recreateRollAdapter){

        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity().getBaseContext());
        // Get from preferences which rolls to load from the database.
        filterMode = FilterMode.Companion.fromValue(
                sharedPreferences.getInt(PreferenceConstants.KEY_VISIBLE_ROLLS, FilterMode.ACTIVE.getValue()));
        sortMode = RollSortMode.Companion.fromValue(
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

        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        // Set the ActionBar subtitle.
        if (actionBar != null) {
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
        final int secondaryColor = Utilities.getSecondaryUiColor(getActivity().getApplicationContext());
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
    public void onCreateOptionsMenu(@NonNull final Menu menu, final MenuInflater inflater) {
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
    public void onPrepareOptionsMenu(@NonNull final Menu menu) {

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
    public boolean onOptionsItemSelected(final MenuItem item){
        switch (item.getItemId()) {

            case R.id.menu_item_gear:

                final Intent gearActivityIntent = new Intent(getActivity(), GearActivity.class);
                startActivity(gearActivityIntent);
                break;

            case R.id.menu_item_preferences:

                final Intent preferenceActivityIntent = new Intent(getActivity(), PreferenceActivity.class);
                //Start the preference activity from MainActivity.
                //The result will be handled in MainActivity.
                getActivity().startActivityForResult(preferenceActivityIntent, MainActivity.PREFERENCE_ACTIVITY_REQUEST);
                break;

            case R.id.menu_item_help:

                final String helpTitle = getResources().getString(R.string.Help);
                final String helpMessage = getResources().getString(R.string.main_help);
                Utilities.showGeneralDialog(getActivity(), helpTitle, helpMessage);
                break;

            case R.id.menu_item_about:

                final String aboutTitle = getResources().getString(R.string.app_name);
                final String aboutMessage = getResources().getString(R.string.about) + "\n\n\n" +
                        getResources().getString(R.string.VersionHistory);
                Utilities.showGeneralDialog(getActivity(), aboutTitle, aboutMessage);
                break;

            case R.id.menu_item_show_on_map:

                // Show all frames from all rolls on a map
                final Intent mapIntent = new Intent(getActivity(), MapActivity.class);
                mapIntent.putParcelableArrayListExtra(ExtraKeys.ARRAY_LIST_ROLLS,
                        (ArrayList<? extends Parcelable>) rollList);
                mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_TITLE, getString(R.string.AllRolls));
                switch (filterMode) {
                    case ACTIVE: default:
                        mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE,
                                getString(R.string.ActiveRolls));
                        break;
                    case ARCHIVED:
                        mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE,
                                getString(R.string.ArchivedRolls));
                        break;
                    case ALL:
                        mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE,
                                getString(R.string.AllRolls));
                        break;
                }
                startActivity(mapIntent);
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
    private void setFilterMode(final FilterMode filterMode) {
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
    private void setSortMode(final RollSortMode sortMode) {
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
    public void onClick(final View v) {
        if (v.getId() == R.id.fab) {
            showRollDialog();
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
    public void onItemClick(final int position) {
        if (rollAdapter.getSelectedItemCount() > 0 || actionMode != null) {
            enableActionMode(position);
        } else {
            final long rollId = rollList.get(position).getId();
            callback.onRollSelected(rollId);
        }
    }

    /**
     * When an item is long pressed, always add the pressed item to selected items.
     *
     * @param position position of the item in RollAdapter
     */
    @Override
    public void onItemLongClick(final int position) {
        enableActionMode(position);
    }

    /**
     * Enable ActionMode is not yet enabled and add item to selected items.
     * Hide edit menu item, if more than one items are selected.
     *
     * @param position position of the item in RollAdapter
     */
    private void enableActionMode(final int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        rollAdapter.toggleSelection(position);
        // If the user deselected the last of the selected items, exit action mode.
        if (rollAdapter.getSelectedItemCount() == 0) {
            actionMode.finish();
        } else {
            // Set the action mode toolbar title to display the number of selected items.
            actionMode.setTitle(rollAdapter.getSelectedItemCount() + "/"
                    + rollAdapter.getItemCount());
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
    private void showEditRollDialog(final int position){
        final EditRollDialog dialog = new EditRollDialog();
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ExtraKeys.ROLL, rollList.get(position));
        arguments.putString(ExtraKeys.TITLE, getActivity().getResources().getString(R.string.EditRoll));
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, getActivity().getResources().getString(R.string.OK));
        dialog.setArguments(arguments);
        dialog.setTargetFragment(this, REQUEST_CODE_EDIT_ROLL);
        dialog.show(getFragmentManager().beginTransaction(), EditRollDialog.TAG);
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Shows a DialogFragment to add a new roll.
     */
    @SuppressLint("CommitTransaction")
    private void showRollDialog() {
        final EditRollDialog dialog = new EditRollDialog();
        final Bundle arguments = new Bundle();
        arguments.putString(ExtraKeys.TITLE, getActivity().getResources().getString(R.string.NewRoll));
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, getActivity().getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.setTargetFragment(this, REQUEST_CODE_ADD_ROLL);
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
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch(requestCode) {

            case REQUEST_CODE_ADD_ROLL:

                if (resultCode == Activity.RESULT_OK) {

                    final Roll roll = data.getParcelableExtra(ExtraKeys.ROLL);
                    final long rowId = database.addRoll(roll);
                    roll.setId(rowId);

                    mainTextViewAnimateInvisible();
                    // Add new roll to the top of the list
                    rollList.add(0, roll);
                    Utilities.sortRollList(sortMode, database, rollList);
                    rollAdapter.notifyItemInserted(rollList.indexOf(roll));

                    // When the new roll is added jump to view the added entry
                    final int pos = rollList.indexOf(roll);
                    if (pos < rollAdapter.getItemCount()) mainRecyclerView.scrollToPosition(pos);

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After cancel do nothing
                    return;
                }
                break;

            case REQUEST_CODE_EDIT_ROLL:

                if (resultCode == Activity.RESULT_OK) {

                    if (actionMode != null) actionMode.finish();

                    final Roll roll = data.getParcelableExtra(ExtraKeys.ROLL);
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

            case REQUEST_CODE_BATCH_EDIT_FILM_STOCK:

                if (resultCode != Activity.RESULT_OK) return;

                final FilmStock filmStock = data.getParcelableExtra(ExtraKeys.FILM_STOCK);
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.BatchEditRollsFilmStockISOConfirmation);
                builder.setNegativeButton(R.string.No, (dialog, which) ->
                        batchUpdateRollsFilmStock(filmStock, false));
                builder.setPositiveButton(R.string.Yes, (dialog, which) ->
                        batchUpdateRollsFilmStock(filmStock, true));
                builder.setNeutralButton(R.string.Cancel, (dialog, which) -> {});
                builder.create().show();

                break;
        }
    }

    /**
     * Update all rolls currently selected in rollAdapter.
     *
     * @param filmStock The rolls will be updated with this film stock. Pass null if you want to
     *                  clear the film stock property of edited rolls.
     * @param updateIso true if the ISO property of edited rolls should be set to that of the passed
     *                  film stock. If film stock == null and updateIso == true, the ISO will be
     *                  reset as well.
     */
    private void batchUpdateRollsFilmStock(final FilmStock filmStock, final boolean updateIso) {
        final List<Integer> selectedRollsPositions = rollAdapter.getSelectedItemPositions();
        for (final int position : selectedRollsPositions) {
            final Roll roll = rollList.get(position);
            if (filmStock != null) {
                roll.setFilmStockId(filmStock.getId());
                if (updateIso) {
                    roll.setIso(filmStock.getIso());
                }
            } else {
                roll.setFilmStockId(0);
                if (updateIso) {
                    roll.setIso(0);
                }
            }
            database.updateRoll(roll);
        }
        if (actionMode != null) {
            actionMode.finish();
        }
        rollAdapter.notifyDataSetChanged();
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
        public boolean onCreateActionMode(final ActionMode actionMode, final Menu menu) {

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
        public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
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
        public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
            // Get the positions in the rollList of selected items
            final List<Integer> selectedItemPositions = rollAdapter.getSelectedItemPositions();

            switch (menuItem.getItemId()) {

                case R.id.menu_item_delete:

                    // Set the confirm dialog title depending on whether one or more rolls were selected
                    final String title = selectedItemPositions.size() == 1 ?
                            getResources().getString(R.string.ConfirmRollDelete) + " \'" + rollList.get(selectedItemPositions.get(0)).getName() + "\'?" :
                            String.format(getResources().getString(R.string.ConfirmRollsDelete), selectedItemPositions.size());
                    final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                    alertBuilder.setTitle(title);
                    alertBuilder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
                        // Do nothing
                    });
                    alertBuilder.setPositiveButton(R.string.OK, (dialog, which) -> {
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
                    });
                    alertBuilder.create().show();
                    return true;

                case R.id.menu_item_select_all:

                    rollAdapter.toggleSelectionAll();
                    mainRecyclerView.post(() -> rollAdapter.resetAnimateAll());
                    actionMode.setTitle(rollAdapter.getSelectedItemCount() + "/"
                            + rollAdapter.getItemCount());
                    // Set the edit item visibility to false because all rolls are selected.
                    actionMode.getMenu().findItem(R.id.menu_item_edit).setVisible(false);
                    return true;

                case R.id.menu_item_edit:

                    if (rollAdapter.getSelectedItemCount() == 1) {
                        // Get the first of the selected rolls (only one should be selected anyway)
                        // Finish action mode if the user clicked ok when editing the roll ->
                        // this is done in onActivityResult().
                        showEditRollDialog(selectedItemPositions.get(0));
                    } else {
                        // Show batch edit features
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(String.format(getResources()
                                .getString(R.string.BatchEditFramesTitle),
                                rollAdapter.getSelectedItemCount()));
                        builder.setItems(R.array.RollsBatchEditOptions, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    // Edit film stock
                                    final SelectFilmStockDialog filmStockDialog = new SelectFilmStockDialog();
                                    filmStockDialog.setTargetFragment(RollsFragment.this,
                                            REQUEST_CODE_BATCH_EDIT_FILM_STOCK);
                                    filmStockDialog.show(getFragmentManager().beginTransaction(), null);
                                    break;
                                case 1:
                                    // Clear film stock
                                    final AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
                                    builder1.setMessage(R.string.BatchEditRollsCLearFilmStockISOConfirmation);
                                    builder1.setNegativeButton(R.string.No, (dialog1, which1) ->
                                            batchUpdateRollsFilmStock(null, false));
                                    builder1.setPositiveButton(R.string.Yes, (dialog1, which1) ->
                                            batchUpdateRollsFilmStock(null, true));
                                    builder1.setNeutralButton(R.string.Cancel, (dialog1, which1) -> {});
                                    builder1.create().show();
                                    break;
                            }
                        });
                        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> dialog.dismiss());
                        builder.create().show();
                    }


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
        public void onDestroyActionMode(final ActionMode mode) {
            rollAdapter.clearSelections();
            actionMode = null;
            mainRecyclerView.post(() -> rollAdapter.resetAnimationIndex());
            // Return the status bar to its original color before action mode.
            Utilities.setStatusBarColor(getActivity(), Utilities.getSecondaryUiColor(getActivity()));
            // Make the floating action bar visible again since action mode is exited.
            floatingActionButton.show();
        }
    }

}