/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.fragments

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.*
import com.tommihirvonen.exifnotes.adapters.RollAdapter
import com.tommihirvonen.exifnotes.adapters.RollAdapter.RollAdapterListener
import com.tommihirvonen.exifnotes.databinding.FragmentRollsListBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.dialogs.SelectFilmStockDialog
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.RollViewModel
import com.tommihirvonen.exifnotes.viewmodels.State

/**
 * RollsFragment is the fragment that is displayed first in MainActivity. It contains
 * a list of rolls the user has saved in the database.
 */
class RollsListFragment : Fragment(), RollAdapterListener {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val ROLLS_FRAGMENT_TAG = "ROLLS_FRAGMENT"
    }

    private val model by activityViewModels<RollViewModel>()
    private var rolls = emptyList<Roll>()
    private lateinit var rollAdapter: RollAdapter
    private lateinit var binding: FragmentRollsListBinding

    /**
     * Private callback class which is given as an argument when the SupportActionMode is started.
     */
    private val actionModeCallback = ActionModeCallback()

    /**
     * Reference to the (Support)ActionMode, which is launched when a list item is long pressed.
     */
    private var actionMode: ActionMode? = null

    private val transitionInterpolator = FastOutSlowInInterpolator()
    private val transitionDurationShowFrames = 400L
    private val transitionDurationEditRoll = 250L
    private var reenterFadeDuration = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentRollsListBinding.inflate(inflater, container, false)
        binding.fab.setOnClickListener { showEditRollFragment(null, binding.fab) }
        val layoutManager = LinearLayoutManager(activity)
        binding.rollsRecyclerView.layoutManager = layoutManager
        binding.rollsRecyclerView.addOnScrollListener(OnScrollExtendedFabListener(binding.fab))
        rollAdapter = RollAdapter(requireActivity(), this, binding.rollsRecyclerView)
        binding.rollsRecyclerView.adapter = rollAdapter
        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }
        binding.topAppBar.setOnMenuItemClickListener(onTopMenuItemClickListener)
        binding.navigationView.setNavigationItemSelectedListener(onDrawerMenuItemClickListener)
        val navigationMenu = binding.navigationView.menu
        val topMenu = binding.topAppBar.menu

        model.rollFilterMode.observe(viewLifecycleOwner) { mode ->
            binding.noAddedRolls.visibility = View.GONE
            when (mode) {
                RollFilterMode.ACTIVE -> {
                    binding.topAppBar.subtitle = resources.getString(R.string.ActiveRolls)
                    binding.noAddedRolls.text = resources.getString(R.string.NoActiveRolls)
                    navigationMenu.findItem(R.id.active_rolls_filter).isChecked = true
                }
                RollFilterMode.ARCHIVED -> {
                    binding.topAppBar.subtitle = resources.getString(R.string.ArchivedRolls)
                    binding.noAddedRolls.text = resources.getString(R.string.NoArchivedRolls)
                    navigationMenu.findItem(R.id.archived_rolls_filter).isChecked = true
                }
                RollFilterMode.ALL -> {
                    binding.topAppBar.subtitle = resources.getString(R.string.AllRolls)
                    binding.noAddedRolls.text = resources.getString(R.string.NoActiveOrArchivedRolls)
                    navigationMenu.findItem(R.id.all_rolls_filter).isChecked = true
                }
                null -> {}
            }
        }

        model.rollSortMode.observe(viewLifecycleOwner) { mode ->
            when (mode) {
                RollSortMode.DATE -> { topMenu.findItem(R.id.date_sort_mode).isChecked = true }
                RollSortMode.NAME -> { topMenu.findItem(R.id.name_sort_mode).isChecked = true }
                RollSortMode.CAMERA -> { topMenu.findItem(R.id.camera_sort_mode).isChecked = true }
                null -> {}
            }
        }

        model.rolls.observe(viewLifecycleOwner) { state ->
            when (state) {
                is State.InProgress -> {
                    binding.noAddedRolls.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE
                    rolls = emptyList()
                    rollAdapter.items = rolls
                }
                is State.Success -> {
                    rolls = state.data
                    rollAdapter.items = rolls
                    binding.progressBar.visibility = View.GONE
                    binding.noAddedRolls.visibility = if (rolls.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            rollAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        // Start the transition once all views have been
        // measured and laid out
        (view.parent as? ViewGroup)?.doOnPreDraw {
            ObjectAnimator.ofFloat(binding.container, View.ALPHA, 0f, 1f).apply {
                duration = reenterFadeDuration
                start()
            }
            startPostponedEnterTransition()
        }
    }

    private val onTopMenuItemClickListener = { item: MenuItem ->
        when (item.itemId) {
            R.id.date_sort_mode -> {
                item.isChecked = true
                model.setRollSortMode(RollSortMode.DATE)
            }
            R.id.name_sort_mode -> {
                item.isChecked = true
                model.setRollSortMode(RollSortMode.NAME)
            }
            R.id.camera_sort_mode -> {
                item.isChecked = true
                model.setRollSortMode(RollSortMode.CAMERA)
            }
        }
        true
    }

    private val onDrawerMenuItemClickListener = { item: MenuItem ->
        val navigationMenu = binding.navigationView.menu
        when (item.itemId) {
            R.id.menu_item_gear -> {
                val gearActivityIntent = Intent(activity, GearActivity::class.java)
                gearResultLauncher.launch(gearActivityIntent)
            }
            R.id.menu_item_preferences -> {
                val preferenceActivityIntent = Intent(activity, PreferenceActivity::class.java)
                preferenceResultLauncher.launch(preferenceActivityIntent)
            }
            R.id.menu_item_show_on_map -> {
                val fragment = RollsMapFragment()
                requireParentFragment().childFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_right)
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .add(R.id.rolls_fragment_container, fragment)
                    .commit()
            }
            R.id.active_rolls_filter -> {
                item.isChecked = true
                navigationMenu.findItem(R.id.all_rolls_filter).isChecked = false
                navigationMenu.findItem(R.id.archived_rolls_filter).isChecked = false
                model.setRollFilterMode(RollFilterMode.ACTIVE)
            }
            R.id.archived_rolls_filter -> {
                item.isChecked = true
                navigationMenu.findItem(R.id.active_rolls_filter).isChecked = false
                navigationMenu.findItem(R.id.all_rolls_filter).isChecked = false
                model.setRollFilterMode(RollFilterMode.ARCHIVED)
            }
            R.id.all_rolls_filter -> {
                item.isChecked = true
                navigationMenu.findItem(R.id.active_rolls_filter).isChecked = false
                navigationMenu.findItem(R.id.archived_rolls_filter).isChecked = false
                model.setRollFilterMode(RollFilterMode.ALL)
            }
        }
        binding.drawerLayout.close()
        true
    }

    override fun onResume() {
        super.onResume()
        rollAdapter.notifyDataSetChanged()
    }

    private val gearResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Update fragment after the user navigates back from the GearActivity.
        // Cameras might have been edited, so they need to be reloaded.
        model.loadAll()
    }

    private val preferenceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // If a new database was imported, update the contents of RollsFragment.
        if (result.resultCode and PreferenceActivity.RESULT_DATABASE_IMPORTED ==
            PreferenceActivity.RESULT_DATABASE_IMPORTED) {
            model.loadAll()
        }
    }

    override fun onItemClick(roll: Roll, layout: View) {
        if (rollAdapter.selectedItems.isNotEmpty() || actionMode != null) {
            enableActionMode(roll)
        } else {

            reenterFadeDuration = transitionDurationShowFrames
            exitTransition = SeparateVertical().apply {
                duration = transitionDurationShowFrames
                interpolator = transitionInterpolator
            }
            (exitTransition as Transition).epicenterCallback = object : Transition.EpicenterCallback() {
                override fun onGetEpicenter(transition: Transition) = Rect().also {
                    layout.getGlobalVisibleRect(it)
                }
            }

            val sharedElementTransition = TransitionSet()
                .addTransition(ChangeBounds())
                .addTransition(ChangeTransform())
                .addTransition(ChangeImageTransform())
                .setCommonInterpolator(transitionInterpolator)
                .apply { duration = transitionDurationShowFrames }

            val framesFragment = FramesFragment().apply {
                sharedElementEnterTransition = sharedElementTransition
                sharedElementReturnTransition = sharedElementTransition
            }

            val arguments = Bundle()
            arguments.putParcelable(ExtraKeys.ROLL, roll)
            arguments.putString(ExtraKeys.TRANSITION_NAME, layout.transitionName)
            framesFragment.arguments = arguments

            requireParentFragment().childFragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(layout, layout.transitionName)
                .addToBackStack(null)
                .replace(R.id.rolls_fragment_container, framesFragment)
                .commit()
        }
    }

    override fun onItemLongClick(roll: Roll) {
        enableActionMode(roll)
    }

    private fun enableActionMode(roll: Roll) {
        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        rollAdapter.toggleSelection(roll)
        // If the user deselected the last of the selected items, exit action mode.
        val selectedRolls = rollAdapter.selectedItems
        if (selectedRolls.isEmpty()) {
            actionMode?.finish()
        } else {
            // Set the action mode toolbar title to display the number of selected items.
            actionMode?.title = "${selectedRolls.size}/${rollAdapter.itemCount}"
        }
    }

    private fun showEditRollFragment(roll: Roll?, sharedElement: View) {
        actionMode?.finish()

        exitTransition = null
        reenterFadeDuration = transitionDurationEditRoll

        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(transitionInterpolator)
            .apply { duration = transitionDurationEditRoll }

        val fragment = RollEditFragment().apply {
            sharedElementEnterTransition = sharedElementTransition
        }

        val arguments = Bundle()
        if (roll == null) {
            arguments.putString(ExtraKeys.TITLE, requireActivity().resources.getString(R.string.AddNewRoll))
        } else {
            arguments.putParcelable(ExtraKeys.ROLL, roll)
            arguments.putString(ExtraKeys.TITLE, requireActivity().resources.getString(R.string.EditRoll))
        }

        arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
        fragment.arguments = arguments

        requireParentFragment().childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(R.id.rolls_fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        fragment.setFragmentResultListener("EditRollDialog") { _, bundle ->
            val editedRoll: Roll = bundle.getParcelable(ExtraKeys.ROLL) ?: return@setFragmentResultListener
            if (roll == null) {
                model.addRoll(editedRoll)
            } else {
                model.updateRoll(editedRoll)
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
        rollAdapter.selectedItems.forEach { roll ->
            roll.filmStock = filmStock
            if (updateIso) {
                roll.iso = filmStock?.iso ?: 0
            }
            model.updateRoll(roll)
        }
        actionMode?.finish()
    }

    /**
     * Class which implements ActionMode.Callback.
     * One instance of this class is given as an argument when ActionMode is started.
     */
    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            // Use different action mode menu layouts depending on which rolls are shown.
            when (model.rollFilterMode.value) {
                RollFilterMode.ACTIVE -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_active, menu)
                RollFilterMode.ARCHIVED -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_archived, menu)
                RollFilterMode.ALL -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_all, menu)
                null -> {}
            }
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
            // Get the positions in the rollList of selected items
            val selectedRolls = rollAdapter.selectedItems
            return when (menuItem.itemId) {
                R.id.menu_item_delete -> {
                    // Set the confirm dialog title depending on whether one or more rolls were selected
                    val title =
                            if (selectedRolls.size == 1) resources.getString(R.string.ConfirmRollDelete) + " \'" + selectedRolls.first().name + "\'?"
                            else String.format(resources.getString(R.string.ConfirmRollsDelete), selectedRolls.size)
                    val alertBuilder = MaterialAlertDialogBuilder(requireActivity())
                    alertBuilder.setTitle(title)
                    alertBuilder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    alertBuilder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        selectedRolls.forEach { model.deleteRoll(it) }
                        actionMode.finish()
                    }
                    alertBuilder.create().show()
                    true
                }
                R.id.menu_item_select_all -> {
                    rollAdapter.toggleSelectionAll()
                    // Do not use local variable to get selected count because its size
                    // may no longer be valid after all items were selected.
                    actionMode.title = "${rollAdapter.selectedItems.size}/${rollAdapter.itemCount}"
                    true
                }
                R.id.menu_item_edit -> {
                    if (selectedRolls.size == 1) {
                        actionMode.finish()
                        // Get the first of the selected rolls (only one should be selected anyway)
                        // Finish action mode if the user clicked ok when editing the roll ->
                        // this is done in onActivityResult().
                        showEditRollFragment(selectedRolls.first(), binding.topAppBar)
                    } else {
                        // Show batch edit features
                        val builder = MaterialAlertDialogBuilder(requireActivity())
                        builder.setTitle(String.format(resources
                                .getString(R.string.BatchEditRollsTitle),
                                selectedRolls.size))
                        builder.setItems(R.array.RollsBatchEditOptions) { _: DialogInterface?, which: Int ->
                            when (which) {
                                0 -> {
                                    // Edit film stock
                                    val filmStockDialog = SelectFilmStockDialog()
                                    filmStockDialog.show(parentFragmentManager.beginTransaction(), null)
                                    filmStockDialog.setFragmentResultListener(
                                        "SelectFilmStockDialog") { _, bundle ->
                                        val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                                            ?: return@setFragmentResultListener
                                        MaterialAlertDialogBuilder(requireActivity()).apply {
                                            setMessage(R.string.BatchEditRollsFilmStockISOConfirmation)
                                            setNegativeButton(R.string.No) { _: DialogInterface?, _: Int ->
                                                batchUpdateRollsFilmStock(filmStock, false)
                                            }
                                            setPositiveButton(R.string.Yes) { _: DialogInterface?, _: Int ->
                                                batchUpdateRollsFilmStock(filmStock, true)
                                            }
                                            setNeutralButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                                        }.create().show()
                                    }
                                }
                                1 -> {
                                    // Clear film stock
                                    val builder1 = MaterialAlertDialogBuilder(requireActivity())
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
                    selectedRolls.forEach { roll ->
                        roll.archived = true
                        model.updateRoll(roll)
                    }
                    actionMode.finish()
                    binding.container
                        .snackbar(R.string.RollsArchived, binding.fab, Snackbar.LENGTH_SHORT)
                    true
                }
                R.id.menu_item_unarchive -> {
                    selectedRolls.forEach { roll ->
                        roll.archived = false
                        model.updateRoll(roll)
                    }
                    actionMode.finish()
                    binding.container
                        .snackbar(R.string.RollsActivated, binding.fab, Snackbar.LENGTH_SHORT)
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            rollAdapter.clearSelections()
            actionMode = null
        }
    }

}