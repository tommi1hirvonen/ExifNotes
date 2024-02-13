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
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.*
import com.tommihirvonen.exifnotes.adapters.RollAdapter
import com.tommihirvonen.exifnotes.adapters.RollAdapter.RollAdapterListener
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.core.entities.RollSortMode
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import com.tommihirvonen.exifnotes.databinding.FragmentRollsListBinding
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.RollsViewModel
import com.tommihirvonen.exifnotes.viewmodels.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * RollsFragment is the fragment that is displayed first in MainActivity. It contains
 * a list of rolls the user has saved in the database.
 */
@AndroidEntryPoint
class RollsListFragment : Fragment(), RollAdapterListener {

    @Inject lateinit var rollRepository: RollRepository

    private val model by activityViewModels<RollsViewModel>()
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

    private var tappedRollPosition = RecyclerView.NO_POSITION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tappedRollPosition = savedInstanceState
            ?.getInt(ExtraKeys.TAP_POSITION, RecyclerView.NO_POSITION)
            ?: RecyclerView.NO_POSITION
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ExtraKeys.TAP_POSITION, tappedRollPosition)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentRollsListBinding.inflate(inflater, container, false)
        binding.model = model
        binding.lifecycleOwner = this

        binding.fab.setOnClickListener { showEditRollFragment(null, binding.fab) }
        val layoutManager = LinearLayoutManager(activity)
        binding.rollsRecyclerView.layoutManager = layoutManager
        binding.rollsRecyclerView.addOnScrollListener(OnScrollExtendedFabListener(binding.fab))

        rollAdapter = RollAdapter(rollRepository, requireActivity(), this, binding.rollsRecyclerView)
        binding.rollsRecyclerView.adapter = rollAdapter
        rollAdapter.onItemSelectedChanged = { item, selected ->
            if (selected) model.selectedRolls.add(item)
            else model.selectedRolls.remove(item)
        }
        rollAdapter.onAllSelectionsChanged = { selected ->
            if (selected) model.selectedRolls.addAll(rolls.filterNot(model.selectedRolls::contains))
            else model.selectedRolls.clear()
        }

        // Transition named used when editing frame via ActionMode menu.
        binding.topAppBar.transitionName = "rolls_top_app_bar_transition"
        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout?.open()
        }
        binding.topAppBar.setOnMenuItemClickListener(onTopMenuItemClickListener)

        binding.navigationView.setNavigationItemSelectedListener(onDrawerMenuItemClickListener)
        val navigationMenu = binding.navigationView.menu
        val activeRollsMenuItem = navigationMenu.findItem(R.id.active_rolls_filter)
        val activeRollsCountBadge = activeRollsMenuItem.actionView as TextView
        val archivedRollsMenuItem = navigationMenu.findItem(R.id.archived_rolls_filter)
        val archivedRollsCountBadge = archivedRollsMenuItem.actionView as TextView
        val allRollsMenuItem = navigationMenu.findItem(R.id.all_rolls_filter)
        val allRollsCountBadge = allRollsMenuItem.actionView as TextView
        val favoriteRollsMenuItem = navigationMenu.findItem(R.id.favorite_rolls_filter)
        val favoriteRollsCountBadge = favoriteRollsMenuItem.actionView as TextView
        val initializeActionView = { textView: TextView ->
            textView.gravity = Gravity.CENTER_VERTICAL
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                textView.setTextAppearance(requireContext(), R.style.TextAppearance_Material3_LabelMedium)
            } else {
                textView.setTextAppearance(R.style.TextAppearance_Material3_LabelLarge)
            }
        }

        val filterMenuItems = sequenceOf(activeRollsMenuItem, archivedRollsMenuItem,
            allRollsMenuItem, favoriteRollsMenuItem)
        val filterMenuItemBadges = sequenceOf(activeRollsCountBadge, archivedRollsCountBadge,
            allRollsCountBadge, favoriteRollsCountBadge)
        filterMenuItemBadges.forEach(initializeActionView)

        val labelsMenu = binding.navigationView.menu.addSubMenu(resources.getString(R.string.Labels))
        val labelBadges = mutableListOf<TextView>()
        val labelMenuItems = mutableListOf<MenuItem>()
        model.labels.observe(viewLifecycleOwner) { labels ->
            labelsMenu.clear()
            labelBadges.clear()
            labelMenuItems.clear()
            for (label in labels) {
                val item = labelsMenu.add(label.name)
                labelMenuItems.add(item)
                item.isCheckable = true
                item.setIcon(R.drawable.ic_outline_label_24)
                val textView = TextView(requireActivity())
                labelBadges.add(textView)
                item.actionView = textView
                initializeActionView(textView)
                textView.text = label.rollCount.toString()
                textView.setTypeface(null, Typeface.NORMAL)
                val filter = model.rollFilterMode.value
                if (filter is RollFilterMode.HasLabel && filter.label.id == label.id) {
                    item.isChecked = true
                    textView.setTypeface(null, Typeface.BOLD)
                }
                item.setOnMenuItemClickListener {
                    model.setRollFilterMode(RollFilterMode.HasLabel(label))
                    labelMenuItems.forEach { it.isChecked = false }
                    item.isChecked = true
                    labelBadges.forEach { it.setTypeface(null, Typeface.NORMAL) }
                    textView.setTypeface(null, Typeface.BOLD)
                    binding.drawerLayout?.close()
                    true
                }
            }
        }

        model.rollFilterMode.observe(viewLifecycleOwner) { mode ->
            binding.noAddedRolls.visibility = View.GONE
            when (mode) {
                RollFilterMode.Active -> {
                    labelMenuItems.forEach { it.isChecked = false }
                    filterMenuItemBadges.plus(labelBadges)
                        .forEach { it.setTypeface(null, Typeface.NORMAL) }
                    binding.noAddedRolls.text = resources.getString(R.string.NoActiveRolls)
                    activeRollsMenuItem.isChecked = true
                    activeRollsCountBadge.setTypeface(null, Typeface.BOLD)
                }
                RollFilterMode.Archived -> {
                    labelMenuItems.forEach { it.isChecked = false }
                    filterMenuItemBadges.plus(labelBadges)
                        .forEach { it.setTypeface(null, Typeface.NORMAL) }
                    binding.noAddedRolls.text = resources.getString(R.string.NoArchivedRolls)
                    archivedRollsMenuItem.isChecked = true
                    archivedRollsCountBadge.setTypeface(null, Typeface.BOLD)
                }
                RollFilterMode.All -> {
                    labelMenuItems.forEach { it.isChecked = false }
                    filterMenuItemBadges.plus(labelBadges)
                        .forEach { it.setTypeface(null, Typeface.NORMAL) }
                    binding.noAddedRolls.text = resources.getString(R.string.NoActiveOrArchivedRolls)
                    allRollsMenuItem.isChecked = true
                    allRollsCountBadge.setTypeface(null, Typeface.BOLD)
                }
                RollFilterMode.Favorites -> {
                    labelMenuItems.forEach { it.isChecked = false }
                    filterMenuItemBadges.plus(labelBadges)
                        .forEach { it.setTypeface(null, Typeface.NORMAL) }
                    binding.noAddedRolls.text = resources.getString(R.string.NoFavorites)
                    favoriteRollsMenuItem.isChecked = true
                    favoriteRollsCountBadge.setTypeface(null, Typeface.BOLD)
                }
                is RollFilterMode.HasLabel -> {
                    binding.noAddedRolls.text = resources.getString(R.string.NoRolls)
                    filterMenuItems.forEach { it.isChecked = false }
                    filterMenuItemBadges.forEach { it.setTypeface(null, Typeface.NORMAL) }
                }
                null -> {}
            }
        }

        val topMenu = binding.topAppBar.menu
        model.rollSortMode.observe(viewLifecycleOwner) { mode ->
            when (mode) {
                RollSortMode.DATE -> { topMenu.findItem(R.id.date_sort_mode).isChecked = true }
                RollSortMode.NAME -> { topMenu.findItem(R.id.name_sort_mode).isChecked = true }
                RollSortMode.CAMERA -> { topMenu.findItem(R.id.camera_sort_mode).isChecked = true }
                null -> {}
            }
        }

        model.rollCounts.observe(viewLifecycleOwner) { counts ->
            val (active, archived, favorites) = counts
            val all = active + archived
            activeRollsCountBadge.text = active.toString()
            archivedRollsCountBadge.text = archived.toString()
            allRollsCountBadge.text = all.toString()
            favoriteRollsCountBadge.text = favorites.toString()
        }

        model.rolls.observe(viewLifecycleOwner) { state ->
            when (state) {
                is State.InProgress -> {
                    binding.noAddedRolls.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE
                    rolls = emptyList()
                    rollAdapter.items = rolls
                    startPostponedEnterTransition()
                }
                is State.Success -> {
                    rolls = state.data
                    rollAdapter.items = rolls
                    if (model.selectedRolls.isNotEmpty()) {
                        rollAdapter.setSelections(model.selectedRolls)
                        ensureActionMode()
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.noAddedRolls.visibility =
                        if (rolls.isEmpty()) View.VISIBLE else View.GONE

                    // If returning from FramesFragment,
                    // reset the exitTransition in case it was lost because of configuration change.
                    if (tappedRollPosition != RecyclerView.NO_POSITION) {
                        if (exitTransition == null) {
                            exitTransition = SeparateVertical().apply {
                                duration = transitionDurationShowFrames
                                interpolator = transitionInterpolator
                            }
                        }
                    }
                    (view?.parent as? ViewGroup)?.doOnPreDraw {
                        if (tappedRollPosition != RecyclerView.NO_POSITION) {
                            val lm = binding.rollsRecyclerView.layoutManager as LinearLayoutManager
                            val itemView = lm.findViewByPosition(tappedRollPosition)
                            itemView?.let { v ->
                                (exitTransition as Transition).epicenterCallback =
                                    object : Transition.EpicenterCallback() {
                                        override fun onGetEpicenter(transition: Transition) =
                                            Rect().also {
                                                v.getGlobalVisibleRect(it)
                                            }
                                    }
                            }
                        }
                        ObjectAnimator.ofFloat(binding.container, View.ALPHA, 0f, 1f).apply {
                            duration = reenterFadeDuration
                            start()
                        }
                        startPostponedEnterTransition()
                        tappedRollPosition = RecyclerView.NO_POSITION
                    }
                }
            }
            rollAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        observeThenClearNavigationResult(ExtraKeys.ROLL, model::submitRoll)
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.rolls_list_dest)
        navBackStackEntry.observeThenClearNavigationResult<FilmStock>(viewLifecycleOwner, ExtraKeys.SELECT_FILM_STOCK) { filmStock ->
            MaterialAlertDialogBuilder(requireActivity()).apply {
                setMessage(R.string.BatchEditRollsFilmStockISOConfirmation)
                setNegativeButton(R.string.No) { _, _ ->
                    batchUpdateRollsFilmStock(filmStock, false)
                }
                setPositiveButton(R.string.Yes) { _, _ ->
                    batchUpdateRollsFilmStock(filmStock, true)
                }
                setNeutralButton(R.string.Cancel) { _, _ -> }
            }.create().show()
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
        binding.drawerLayout?.close()
        val filterMenuItemIds = sequenceOf(R.id.archived_rolls_filter, R.id.active_rolls_filter,
            R.id.favorite_rolls_filter, R.id.all_rolls_filter)
        val filterMenuItems = filterMenuItemIds.map { binding.navigationView.menu.findItem(it) }
        when (item.itemId) {
            R.id.menu_item_gear -> {
                exitTransition = null
                model.refreshPending = true
                val action = RollsListFragmentDirections.gearAction()
                viewLifecycleOwner.lifecycleScope.launch {
                    // Small delay so that the drawer has enough time to close
                    // before the transaction happens
                    delay(225)
                    findNavController().navigate(action)
                }
            }
            R.id.menu_item_preferences -> {
                val preferenceActivityIntent = Intent(activity, PreferenceActivity::class.java)
                viewLifecycleOwner.lifecycleScope.launch {
                    // Small delay so that the drawer has enough time to close
                    // before a new activity is started.
                    delay(225)
                    preferenceResultLauncher.launch(preferenceActivityIntent)
                }
            }
            R.id.menu_item_show_on_map -> {
                exitTransition = null
                viewLifecycleOwner.lifecycleScope.launch {
                    // Small delay so that the drawer has enough time to close
                    // before a new fragment is started.
                    delay(225)
                    val action = RollsListFragmentDirections.rollsMapAction()
                    findNavController().navigate(action)
                }
            }
            R.id.menu_item_labels -> {
                exitTransition = null
                model.refreshPending = true
                val action = RollsListFragmentDirections.labelsAction()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(225)
                    findNavController().navigate(action)
                }
            }
            R.id.active_rolls_filter -> {
                filterMenuItems.forEach { it.isChecked = false }
                item.isChecked = true
                model.setRollFilterMode(RollFilterMode.Active)
            }
            R.id.archived_rolls_filter -> {
                filterMenuItems.forEach { it.isChecked = false }
                item.isChecked = true
                model.setRollFilterMode(RollFilterMode.Archived)
            }
            R.id.all_rolls_filter -> {
                filterMenuItems.forEach { it.isChecked = false }
                item.isChecked = true
                model.setRollFilterMode(RollFilterMode.All)
            }
            R.id.favorite_rolls_filter -> {
                filterMenuItems.forEach { it.isChecked = false }
                item.isChecked = true
                model.setRollFilterMode(RollFilterMode.Favorites)
            }
        }
        true
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (model.refreshPending) {
            model.refreshPending = false
            model.loadAll()
        }
        rollAdapter.notifyDataSetChanged()
    }

    private val preferenceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // If a new database was imported, update the contents of RollsFragment.
        if (result.resultCode and PreferenceActivity.RESULT_DATABASE_IMPORTED ==
            PreferenceActivity.RESULT_DATABASE_IMPORTED) {
            model.loadAll()
        }
    }

    override fun onItemClick(roll: Roll, layout: View, position: Int) {
        tappedRollPosition = position
        if (model.selectedRolls.isNotEmpty() || actionMode != null) {
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

            val action = RollsListFragmentDirections.framesListAction(roll, layout.transitionName)
            val extras = FragmentNavigatorExtras(
                layout to layout.transitionName
            )
            findNavController().navigate(action, extras)
        }
    }

    override fun onItemLongClick(roll: Roll) {
        enableActionMode(roll)
    }

    private fun enableActionMode(roll: Roll) {
        rollAdapter.toggleSelection(roll)
        // If the user deselected the last of the selected items, exit action mode.
        if (model.selectedRolls.isEmpty()) {
            actionMode?.finish()
        } else {
            ensureActionMode()
        }
    }

    private fun ensureActionMode() {
        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        // Set the action mode toolbar title to display the number of selected items.
        actionMode?.title = "${model.selectedRolls.size}/${rolls.size}"
    }

    private fun showEditRollFragment(roll: Roll?, sharedElement: View?) {
        actionMode?.finish()
        exitTransition = null
        reenterFadeDuration = transitionDurationEditRoll
        val title = if (roll == null) {
            requireActivity().resources.getString(R.string.AddNewRoll)
        } else {
            requireActivity().resources.getString(R.string.EditRoll)
        }
        val action = RollsListFragmentDirections.rollEditAction(roll, title, sharedElement?.transitionName)
        if (sharedElement == null) {
            findNavController().navigate(action)
        } else {
            val extras = FragmentNavigatorExtras(
                sharedElement to sharedElement.transitionName)
            findNavController().navigate(action, extras)
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
        model.selectedRolls.forEach { roll ->
            roll.filmStock = filmStock
            if (updateIso) {
                roll.iso = filmStock?.iso ?: 0
            }
            model.submitRoll(roll)
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
                is RollFilterMode.Active -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_active, menu)
                is RollFilterMode.Archived -> actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_archived, menu)
                is RollFilterMode.All, is RollFilterMode.Favorites, is RollFilterMode.HasLabel ->
                    actionMode.menuInflater.inflate(R.menu.menu_action_mode_rolls_all, menu)
                null -> {}
            }
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
            // Get the positions in the rollList of selected items
            val selectedRolls = model.selectedRolls
            return when (menuItem.itemId) {
                R.id.menu_item_delete -> {
                    // Set the confirm dialog title depending on whether one or more rolls were selected
                    val title =
                            if (selectedRolls.size == 1) resources.getString(R.string.ConfirmRollDelete) + " \'" + selectedRolls.first().name + "\'?"
                            else String.format(resources.getString(R.string.ConfirmRollsDelete), selectedRolls.size)
                    val alertBuilder = MaterialAlertDialogBuilder(requireActivity())
                    alertBuilder.setTitle(title)
                    alertBuilder.setNegativeButton(R.string.Cancel) { _, _ -> }
                    alertBuilder.setPositiveButton(R.string.OK) { _, _ ->
                        selectedRolls.forEach(model::deleteRoll)
                        actionMode.finish()
                    }
                    alertBuilder.create().show()
                    true
                }
                R.id.menu_item_select_all -> {
                    rollAdapter.toggleSelectionAll()
                    // Do not use local variable to get selected count because its size
                    // may no longer be valid after all items were selected.
                    actionMode.title = "${model.selectedRolls.size}/${rolls.size}"
                    true
                }
                R.id.menu_item_edit -> {
                    if (selectedRolls.size == 1) {
                        // Capture the selected roll. Calling actionMode.finish()
                        // clears the selected rolls list.
                        val selectedRoll = selectedRolls.first()
                        actionMode.finish()
                        // Get the first of the selected rolls (only one should be selected anyway)
                        // Finish action mode if the user clicked ok when editing the roll ->
                        // this is done in onActivityResult().
                        showEditRollFragment(selectedRoll, null)
                    } else {
                        // Show batch edit features
                        val builder = MaterialAlertDialogBuilder(requireActivity())
                        builder.setTitle(String.format(resources
                                .getString(R.string.BatchEditRollsTitle),
                                selectedRolls.size))
                        builder.setItems(R.array.RollsBatchEditOptions) { _, which ->
                            when (which) {
                                0 -> {
                                    // Edit film stock
                                    val action = RollsListFragmentDirections.selectFilmStockAction()
                                    findNavController().navigate(action)
                                }
                                1 -> {
                                    // Clear film stock
                                    val builder1 = MaterialAlertDialogBuilder(requireActivity())
                                    builder1.setMessage(R.string.BatchEditRollsCLearFilmStockISOConfirmation)
                                    builder1.setNegativeButton(R.string.No) { _, _ ->
                                        batchUpdateRollsFilmStock(null, false)
                                    }
                                    builder1.setPositiveButton(R.string.Yes) { _, _ ->
                                        batchUpdateRollsFilmStock(null, true)
                                    }
                                    builder1.setNeutralButton(R.string.Cancel) { _, _ -> }
                                    builder1.create().show()
                                }
                            }
                        }
                        builder.setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
                        builder.create().show()
                    }
                    true
                }
                R.id.menu_item_archive -> {
                    selectedRolls.forEach { roll ->
                        roll.archived = true
                        model.submitRoll(roll)
                    }
                    actionMode.finish()
                    binding.container
                        .snackbar(R.string.RollsArchived, binding.fab, Snackbar.LENGTH_SHORT)
                    true
                }
                R.id.menu_item_unarchive -> {
                    selectedRolls.forEach { roll ->
                        roll.archived = false
                        model.submitRoll(roll)
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