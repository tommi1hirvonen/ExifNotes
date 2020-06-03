package com.tommihirvonen.exifnotes.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.GearActivity
import com.tommihirvonen.exifnotes.activities.MainActivity
import com.tommihirvonen.exifnotes.activities.MapActivity
import com.tommihirvonen.exifnotes.activities.PreferenceActivity
import com.tommihirvonen.exifnotes.adapters.RollAdapter
import com.tommihirvonen.exifnotes.adapters.RollAdapter.RollAdapterListener
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.FilterMode
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.datastructures.RollSortMode
import com.tommihirvonen.exifnotes.dialogs.EditRollDialog
import com.tommihirvonen.exifnotes.dialogs.SelectFilmStockDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.Utilities
import java.util.*

/**
 * RollsFragment is the fragment that is displayed first in MainActivity. It contains
 * a list of rolls the user has saved in the database.
 */
class RollsFragment : Fragment(), View.OnClickListener, RollAdapterListener {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val ROLLS_FRAGMENT_TAG = "ROLLS_FRAGMENT"

        /**
         * Constant passed to EditRollDialog for result
         */
        private const val REQUEST_CODE_ADD_ROLL = 1

        /**
         * Constant passed to EditRollDialog for result
         */
        private const val REQUEST_CODE_EDIT_ROLL = 2
        private const val REQUEST_CODE_BATCH_EDIT_FILM_STOCK = 3
    }

    /**
     * Reference to the parent activity's OnRollSelectedListener
     */
    private lateinit var callback: OnRollSelectedListener

    /**
     * Reference to the FloatingActionButton
     */
    private lateinit var floatingActionButton: FloatingActionButton

    /**
     * TextView to show that no rolls have been added to the database
     */
    private lateinit var mainTextView: TextView

    /**
     * ListView to show all the rolls in the database along with details
     */
    private lateinit var mainRecyclerView: RecyclerView

    /**
     * Adapter used to adapt rollList to mainRecyclerView
     */
    private lateinit var rollAdapter: RollAdapter

    /**
     * Contains all rolls from the database
     */
    private var rollList = mutableListOf<Roll>()

    /**
     * Reference to the singleton database
     */
    private lateinit var database: FilmDbHelper

    /**
     * Private callback class which is given as an argument when the SupportActionMode is started.
     */
    private val actionModeCallback = ActionModeCallback()

    /**
     * Reference to the (Support)ActionMode, which is launched when a list item is long pressed.
     */
    private var actionMode: ActionMode? = null

    /**
     * Holds the roll filter status (archived, active or all rolls).
     * This way we don't always have to query the value from SharedPreferences.
     */
    private var filterMode: FilterMode? = null

    /**
     * Holds the roll sort mode (date, name or camera).
     * This way we don't always have to query the value from SharedPreferences.
     */
    private var sortMode: RollSortMode? = null

    interface OnRollSelectedListener {
        /**
         * Called when a use has selected a Roll.
         *
         * @param rollId database id of the selected roll
         */
        fun onRollSelected(rollId: Long)
    }

    // This onAttach() is called before API 23
    @Suppress("DEPRECATION")
    override fun onAttach(a: Activity) {
        super.onAttach(a)
        callback = a as OnRollSelectedListener
    }

    // This onAttach() is called after API 23
    override fun onAttach(c: Context) {
        super.onAttach(c)
        callback = c as OnRollSelectedListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Set the ActionBar title text.
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar?.title = "  " + resources.getString(R.string.MainActivityTitle)
        actionBar?.setDisplayHomeAsUpEnabled(false)

        // Assign the database.
        database = FilmDbHelper.getInstance(activity)

        // Inflate the layout view.
        val layoutInflater = requireActivity().layoutInflater
        val view = layoutInflater.inflate(R.layout.fragment_rolls, container, false)

        // Assign the FloatingActionButton and set this activity to react to the fab being pressed.
        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener(this)

        // Assign the main TextView.
        mainTextView = view.findViewById(R.id.no_added_rolls)

        // Assign the main film roll RecyclerView.
        mainRecyclerView = view.findViewById(R.id.rolls_recycler_view)
        val layoutManager = LinearLayoutManager(activity)
        mainRecyclerView.layoutManager = layoutManager
        mainRecyclerView.addItemDecoration(DividerItemDecoration(mainRecyclerView.context, layoutManager.orientation))

        // Also change the floating action button color. Use the darker secondaryColor for this.
        val secondaryColor = Utilities.getSecondaryUiColor(activity)
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(secondaryColor)

        // Use the updateFragment() method to load the film rolls from the database,
        // create an ArrayAdapter to link the list of rolls to the ListView,
        // update the ActionBar subtitle and main TextView and set the main TextView
        // either visible or hidden.
        updateFragment(true)
        // Return the inflated view.
        return view
    }

    /**
     * Public method to update the contents of this fragment.
     */
    fun updateFragment(recreateRollAdapter: Boolean) {
        val sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(requireActivity().baseContext)
        // Get from preferences which rolls to load from the database.
        filterMode = FilterMode.fromValue(
                sharedPreferences.getInt(PreferenceConstants.KEY_VISIBLE_ROLLS, FilterMode.ACTIVE.value))
        sortMode = RollSortMode.fromValue(
                sharedPreferences.getInt(PreferenceConstants.KEY_ROLL_SORT_ORDER, RollSortMode.DATE.value))

        // Declare variables for the ActionBar subtitle, which shows the film roll filter status
        // and the main TextView, which is displayed if no rolls are shown.
        val subtitleText: String
        val mainTextViewText: String
        when (filterMode) {
            FilterMode.ACTIVE -> {
                subtitleText = resources.getString(R.string.ActiveFilmRolls)
                mainTextViewText = resources.getString(R.string.NoActiveRolls)
                floatingActionButton.show()
            }
            FilterMode.ARCHIVED -> {
                subtitleText = resources.getString(R.string.ArchivedFilmRolls)
                mainTextViewText = resources.getString(R.string.NoArchivedRolls)
                floatingActionButton.hide()
            }
            FilterMode.ALL -> {
                subtitleText = resources.getString(R.string.AllFilmRolls)
                mainTextViewText = resources.getString(R.string.NoActiveOrArchivedRolls)
                floatingActionButton.show()
            }
            else -> {
                subtitleText = resources.getString(R.string.ActiveFilmRolls)
                mainTextViewText = resources.getString(R.string.NoActiveRolls)
                floatingActionButton.show()
            }
        }
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        // Set the ActionBar subtitle.
        actionBar?.subtitle = "   $subtitleText"

        // Set the main TextView text.
        mainTextView.text = mainTextViewText

        // Load the rolls from the database.
        rollList = database.getRolls(filterMode)
        //Order the roll list according to preferences.
        Utilities.sortRollList(sortMode, database, rollList)
        if (recreateRollAdapter) {
            // Create an ArrayAdapter for the ListView.
            rollAdapter = RollAdapter(requireActivity(), rollList, this)
            // Set the ListView to use the ArrayAdapter.
            mainRecyclerView.adapter = rollAdapter
            // Notify the adapter to update itself.
        } else {
            // rollAdapter still references the old rollList. Update its reference.
            rollAdapter.setRollList(rollList)
            // Notify the adapter to update itself
        }
        rollAdapter.notifyDataSetChanged()
        if (rollList.isNotEmpty()) mainTextViewAnimateInvisible() else mainTextViewAnimateVisible()
    }

    override fun onResume() {
        super.onResume()
        rollAdapter.notifyDataSetChanged()
        val secondaryColor = Utilities.getSecondaryUiColor(requireActivity().applicationContext)
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(secondaryColor)
        // If action mode is enabled, color the status bar dark grey.
        if (rollAdapter.selectedItemCount > 0 || actionMode != null) {
            Utilities.setStatusBarColor(activity, ContextCompat.getColor(requireActivity(), R.color.dark_grey))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_rolls_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (filterMode) {
            FilterMode.ACTIVE -> menu.findItem(R.id.active_rolls_filter).isChecked = true
            FilterMode.ARCHIVED -> menu.findItem(R.id.archived_rolls_filter).isChecked = true
            FilterMode.ALL -> menu.findItem(R.id.all_rolls_filter).isChecked = true
            else -> menu.findItem(R.id.active_rolls_filter).isChecked = true
        }
        when (sortMode) {
            RollSortMode.DATE -> menu.findItem(R.id.date_sort_mode).isChecked = true
            RollSortMode.NAME -> menu.findItem(R.id.name_sort_mode).isChecked = true
            RollSortMode.CAMERA -> menu.findItem(R.id.camera_sort_mode).isChecked = true
            else -> menu.findItem(R.id.date_sort_mode).isChecked = true
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_gear -> {
                val gearActivityIntent = Intent(activity, GearActivity::class.java)
                startActivity(gearActivityIntent)
            }
            R.id.menu_item_preferences -> {
                val preferenceActivityIntent = Intent(activity, PreferenceActivity::class.java)
                //Start the preference activity from MainActivity.
                //The result will be handled in MainActivity.
                requireActivity().startActivityForResult(preferenceActivityIntent, MainActivity.PREFERENCE_ACTIVITY_REQUEST)
            }
            R.id.menu_item_help -> {
                val helpTitle = resources.getString(R.string.Help)
                val helpMessage = resources.getString(R.string.main_help)
                Utilities.showGeneralDialog(activity, helpTitle, helpMessage)
            }
            R.id.menu_item_about -> {
                val aboutTitle = resources.getString(R.string.app_name)
                val aboutMessage = """
                    ${resources.getString(R.string.about)}


                    ${resources.getString(R.string.VersionHistory)}
                    """.trimIndent()
                Utilities.showGeneralDialog(activity, aboutTitle, aboutMessage)
            }
            R.id.menu_item_show_on_map -> {

                // Show all frames from all rolls on a map
                val mapIntent = Intent(activity, MapActivity::class.java)
                mapIntent.putParcelableArrayListExtra(ExtraKeys.ARRAY_LIST_ROLLS,
                        rollList as ArrayList<out Parcelable?>)
                mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_TITLE, getString(R.string.AllRolls))
                when (filterMode) {
                    FilterMode.ACTIVE -> mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE,
                            getString(R.string.ActiveRolls))
                    FilterMode.ARCHIVED -> mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE,
                            getString(R.string.ArchivedRolls))
                    FilterMode.ALL -> mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE,
                            getString(R.string.AllRolls))
                    else -> mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE,
                            getString(R.string.ActiveRolls))
                }
                startActivity(mapIntent)
            }
            R.id.active_rolls_filter -> {
                item.isChecked = true
                setFilterMode(FilterMode.ACTIVE)
            }
            R.id.archived_rolls_filter -> {
                item.isChecked = true
                setFilterMode(FilterMode.ARCHIVED)
            }
            R.id.all_rolls_filter -> {
                item.isChecked = true
                setFilterMode(FilterMode.ALL)
            }
            R.id.date_sort_mode -> {
                item.isChecked = true
                setSortMode(RollSortMode.DATE)
            }
            R.id.name_sort_mode -> {
                item.isChecked = true
                setSortMode(RollSortMode.NAME)
            }
            R.id.camera_sort_mode -> {
                item.isChecked = true
                setSortMode(RollSortMode.CAMERA)
            }
        }
        return true
    }

    /**
     * Change the way visible rolls are filtered. Update SharedPreferences and the fragment.
     *
     * @param filterMode enum type referencing the filtering mode
     */
    private fun setFilterMode(filterMode: FilterMode) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_VISIBLE_ROLLS, filterMode.value)
        editor.apply()
        updateFragment(false)
    }

    /**
     * Change the sort order of rolls.
     *
     * @param sortMode enum type referencing the sorting mode
     */
    private fun setSortMode(sortMode: RollSortMode) {
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(PreferenceConstants.KEY_ROLL_SORT_ORDER, sortMode.value)
        editor.apply()
        Utilities.sortRollList(sortMode, database, rollList)
        rollAdapter.notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            showRollDialog()
        }
    }

    override fun onItemClick(position: Int) {
        if (rollAdapter.selectedItemCount > 0 || actionMode != null) {
            enableActionMode(position)
        } else {
            val rollId = rollList[position].id
            callback.onRollSelected(rollId)
        }
    }

    override fun onItemLongClick(position: Int) {
        enableActionMode(position)
    }

    /**
     * Enable ActionMode is not yet enabled and add item to selected items.
     * Hide edit menu item, if more than one items are selected.
     *
     * @param position position of the item in RollAdapter
     */
    private fun enableActionMode(position: Int) {
        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        rollAdapter.toggleSelection(position)
        // If the user deselected the last of the selected items, exit action mode.
        if (rollAdapter.selectedItemCount == 0) {
            actionMode?.finish()
        } else {
            // Set the action mode toolbar title to display the number of selected items.
            actionMode?.title = (rollAdapter.selectedItemCount.toString() + "/" + rollAdapter.itemCount)
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
    private fun showEditRollDialog(position: Int) {
        val dialog = EditRollDialog()
        val arguments = Bundle()
        arguments.putParcelable(ExtraKeys.ROLL, rollList[position])
        arguments.putString(ExtraKeys.TITLE, requireActivity().resources.getString(R.string.EditRoll))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, requireActivity().resources.getString(R.string.OK))
        dialog.arguments = arguments
        dialog.setTargetFragment(this, REQUEST_CODE_EDIT_ROLL)
        dialog.show(parentFragmentManager.beginTransaction(), EditRollDialog.TAG)
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Shows a DialogFragment to add a new roll.
     */
    @SuppressLint("CommitTransaction")
    private fun showRollDialog() {
        val dialog = EditRollDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, requireActivity().resources.getString(R.string.NewRoll))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, requireActivity().resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.setTargetFragment(this, REQUEST_CODE_ADD_ROLL)
        dialog.show(parentFragmentManager.beginTransaction(), EditRollDialog.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_ADD_ROLL -> if (resultCode == Activity.RESULT_OK) {
                val roll: Roll = data?.getParcelableExtra(ExtraKeys.ROLL) ?: return
                roll.id = database.addRoll(roll)
                mainTextViewAnimateInvisible()
                // Add new roll to the top of the list
                rollList.add(0, roll)
                Utilities.sortRollList(sortMode, database, rollList)
                rollAdapter.notifyItemInserted(rollList.indexOf(roll))

                // When the new roll is added jump to view the added entry
                val pos = rollList.indexOf(roll)
                if (pos < rollAdapter.itemCount) mainRecyclerView.scrollToPosition(pos)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // After cancel do nothing
                return
            }
            REQUEST_CODE_EDIT_ROLL -> if (resultCode == Activity.RESULT_OK) {
                if (actionMode != null) actionMode?.finish()
                val roll: Roll = data?.getParcelableExtra(ExtraKeys.ROLL) ?: return
                database.updateRoll(roll)
                // Notify array adapter that the data set has to be updated
                val oldPosition = rollList.indexOf(roll)
                Utilities.sortRollList(sortMode, database, rollList)
                val newPosition = rollList.indexOf(roll)
                rollAdapter.notifyItemChanged(oldPosition)
                rollAdapter.notifyItemMoved(oldPosition, newPosition)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // After cancel do nothing
                return
            }
            REQUEST_CODE_BATCH_EDIT_FILM_STOCK -> {
                if (resultCode != Activity.RESULT_OK) return
                val filmStock: FilmStock = data?.getParcelableExtra(ExtraKeys.FILM_STOCK) ?: return
                val builder = AlertDialog.Builder(activity)
                builder.setMessage(R.string.BatchEditRollsFilmStockISOConfirmation)
                builder.setNegativeButton(R.string.No) { _: DialogInterface?, _: Int -> batchUpdateRollsFilmStock(filmStock, false) }
                builder.setPositiveButton(R.string.Yes) { _: DialogInterface?, _: Int -> batchUpdateRollsFilmStock(filmStock, true) }
                builder.setNeutralButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                builder.create().show()
            }
        }
    }

    /**
     * Update all rolls currently selected in rollAdapter.
     *
     * @param filmStock The rolls will be updated with this film stock. Pass null if you want to
     * clear the film stock property of edited rolls.
     * @param updateIso true if the ISO property of edited rolls should be set to that of the passed
     * film stock. If film stock == null and updateIso == true, the ISO will be
     * reset as well.
     */
    private fun batchUpdateRollsFilmStock(filmStock: FilmStock?, updateIso: Boolean) {
        val selectedRollsPositions = rollAdapter.selectedItemPositions
        for (position in selectedRollsPositions) {
            val roll = rollList[position]
            roll.filmStockId = filmStock?.id ?: 0
            if (updateIso) roll.iso = filmStock?.iso ?: 0
            database.updateRoll(roll)
        }
        if (actionMode != null) {
            actionMode?.finish()
        }
        rollAdapter.notifyDataSetChanged()
    }

    /**
     * Method to fade in the main TextView ("No rolls")
     */
    private fun mainTextViewAnimateVisible() {
        mainTextView.animate().alpha(1.0f).duration = 150
    }

    /**
     * Method to fade out the main TextView ("No rolls")
     */
    private fun mainTextViewAnimateInvisible() {
        mainTextView.animate().alpha(0.0f).duration = 0
    }

    /**
     * Class which implements ActionMode.Callback.
     * One instance of this class is given as an argument when ActionMode is started.
     */
    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {

            // Set the status bar color to be dark grey to complement the grey action mode toolbar.
            Utilities.setStatusBarColor(activity, ContextCompat.getColor(requireActivity(), R.color.dark_grey))

            // Hide the floating action button so no new rolls can be added while in action mode.
            floatingActionButton.hide()

            // Use different action mode menu layouts depending on which rolls are shown.
            when {
                filterMode === FilterMode.ACTIVE -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_active, menu)
                filterMode === FilterMode.ARCHIVED -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_archived, menu)
                else -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_all, menu)
            }
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
            // Get the positions in the rollList of selected items
            val selectedItemPositions = rollAdapter.selectedItemPositions
            return when (menuItem.itemId) {
                R.id.menu_item_delete -> {

                    // Set the confirm dialog title depending on whether one or more rolls were selected
                    val title =
                            if (selectedItemPositions.size == 1) resources.getString(R.string.ConfirmRollDelete) + " \'" + rollList[selectedItemPositions[0]].name + "\'?"
                            else String.format(resources.getString(R.string.ConfirmRollsDelete), selectedItemPositions.size)
                    val alertBuilder = AlertDialog.Builder(activity)
                    alertBuilder.setTitle(title)
                    alertBuilder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    alertBuilder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        selectedItemPositions.sortedDescending().forEach { position ->
                            val roll = rollList[position]
                            // Delete the roll. Database foreign key rules make sure,
                            // that any linked frames are deleted as well.
                            database.deleteRoll(roll)
                            // Remove the roll from the rollList. Do this last!!!
                            rollList.removeAt(position)
                            if (rollList.isEmpty()) mainTextViewAnimateVisible()
                            rollAdapter.notifyItemRemoved(position)
                        }
                        actionMode.finish()
                    }
                    alertBuilder.create().show()
                    true
                }
                R.id.menu_item_select_all -> {
                    rollAdapter.toggleSelectionAll()
                    mainRecyclerView.post { rollAdapter.resetAnimateAll() }
                    actionMode.title = (rollAdapter.selectedItemCount.toString() + "/" + rollAdapter.itemCount)
                    true
                }
                R.id.menu_item_edit -> {
                    if (rollAdapter.selectedItemCount == 1) {
                        // Get the first of the selected rolls (only one should be selected anyway)
                        // Finish action mode if the user clicked ok when editing the roll ->
                        // this is done in onActivityResult().
                        showEditRollDialog(selectedItemPositions[0])
                    } else {
                        // Show batch edit features
                        val builder = AlertDialog.Builder(activity)
                        builder.setTitle(String.format(resources
                                .getString(R.string.BatchEditFramesTitle),
                                rollAdapter.selectedItemCount))
                        builder.setItems(R.array.RollsBatchEditOptions) { _: DialogInterface?, which: Int ->
                            when (which) {
                                0 -> {
                                    // Edit film stock
                                    val filmStockDialog = SelectFilmStockDialog()
                                    filmStockDialog.setTargetFragment(this@RollsFragment,
                                            REQUEST_CODE_BATCH_EDIT_FILM_STOCK)
                                    filmStockDialog.show(parentFragmentManager.beginTransaction(), null)
                                }
                                1 -> {
                                    // Clear film stock
                                    val builder1 = AlertDialog.Builder(activity)
                                    builder1.setMessage(R.string.BatchEditRollsCLearFilmStockISOConfirmation)
                                    builder1.setNegativeButton(R.string.No) { _: DialogInterface?, _: Int -> batchUpdateRollsFilmStock(null, false) }
                                    builder1.setPositiveButton(R.string.Yes) { _: DialogInterface?, _: Int -> batchUpdateRollsFilmStock(null, true) }
                                    builder1.setNeutralButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                                    builder1.create().show()
                                }
                            }
                        }
                        builder.setNegativeButton(R.string.Cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                        builder.create().show()
                    }
                    true
                }
                R.id.menu_item_archive -> {
                    // Iterate the selected rolls based on their index in descending order.
                    // This way we remove objects starting from the end of the list,
                    // which means that the indices of objects still to be removed do not change.
                    selectedItemPositions.sortedDescending().forEach { position ->
                        val roll = rollList[position]
                        roll.archived = true
                        database.updateRoll(roll)
                        if (filterMode === FilterMode.ACTIVE) {
                            rollList.removeAt(position)
                            rollAdapter.notifyItemRemoved(position)
                        }
                    }
                    if (rollList.isEmpty()) mainTextViewAnimateVisible()
                    actionMode.finish()
                    Toast.makeText(activity, resources.getString(R.string.RollsArchived), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_item_unarchive -> {
                    // Iterate the selected rolls based on their index in descending order.
                    // This way we remove objects starting from the end of the list,
                    // which means that the indices of objects still to be removed do not change.
                    selectedItemPositions.sortedDescending().forEach { position ->
                        val roll = rollList[position]
                        roll.archived = false
                        database.updateRoll(roll)
                        if (filterMode === FilterMode.ARCHIVED) {
                            rollList.removeAt(position)
                            rollAdapter.notifyItemRemoved(position)
                        }
                    }
                    if (rollList.isEmpty()) mainTextViewAnimateVisible()
                    actionMode.finish()
                    Toast.makeText(activity, resources.getString(R.string.RollsActivated), Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            rollAdapter.clearSelections()
            actionMode = null
            mainRecyclerView.post { rollAdapter.resetAnimationIndex() }
            // Return the status bar to its original color before action mode.
            Utilities.setStatusBarColor(activity, Utilities.getSecondaryUiColor(activity))
            // Make the floating action bar visible again since action mode is exited.
            floatingActionButton.show()
        }
    }

}