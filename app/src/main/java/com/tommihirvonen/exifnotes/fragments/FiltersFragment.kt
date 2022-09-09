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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilterAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentFiltersBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.dialogs.FilterEditDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.utilities.setIconsVisible
import com.tommihirvonen.exifnotes.utilities.snackbar
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel
import com.tommihirvonen.exifnotes.viewmodels.State

/**
 * Fragment to display all filters from the database along with details
 */
class FiltersFragment : Fragment() {

    private val model: GearViewModel by activityViewModels()
    private val gearFragment by lazy {
        requireParentFragment().requireParentFragment() as GearFragment
    }

    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    private lateinit var binding: FragmentFiltersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if an existing filter edit dialog is open after configuration change
        // and attach listener if so.
        val editDialog = gearFragment.childFragmentManager.findFragmentByTag(FilterEditDialog.TAG)
        editDialog?.setFragmentResultListener(FilterEditDialog.REQUEST_KEY) { _, bundle ->
            bundle.getParcelable<Filter>(ExtraKeys.FILTER)?.let(model::submitFilter)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFiltersBinding.inflate(inflater, container, false)
        binding.fabFilters.setOnClickListener { openFilterEditDialog(null) }

        val layoutManager = LinearLayoutManager(activity)
        binding.filtersRecyclerView.layoutManager = layoutManager

        val filterAdapter = FilterAdapter(requireActivity(), onFilterClickListener)
        binding.filtersRecyclerView.adapter = filterAdapter

        model.filters.observe(viewLifecycleOwner) { filters ->
            this.filters = filters
            filterAdapter.filters = filters
            binding.noAddedFilters.visibility = if (filters.isEmpty()) View.VISIBLE else View.GONE
            filterAdapter.notifyDataSetChanged()
        }

        model.lenses.observe(viewLifecycleOwner) { lenses ->
            this.lenses = lenses
            filterAdapter.lenses = lenses
            filterAdapter.notifyDataSetChanged()
        }

        model.cameras.observe(viewLifecycleOwner) { state ->
            if (state is State.Success) {
                cameras = state.data
                filterAdapter.cameras = cameras
                filterAdapter.notifyDataSetChanged()
            }
        }

        return binding.root
    }

    private val onFilterClickListener = { filter: Filter, view: View ->
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_filter_item, popup.menu)
        popup.setIconsVisible(requireContext())
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_lenses -> showSelectMountableLensesDialog(filter, fixedLensCameras = false)
                R.id.menu_item_cameras -> showSelectMountableLensesDialog(filter, fixedLensCameras = true)
                R.id.menu_item_edit -> openFilterEditDialog(filter)
                R.id.menu_item_delete -> confirmDeleteFilter(filter)
            }
            true
        }
        popup.show()
    }

    /**
     * Show dialog where the user can select which lenses can be mounted to the picked filter.
     *
     * @param filter Filter object for which lens selections should be made
     * @param fixedLensCameras Whether to show dialog for interchangeable lenses
     * or fixed lens cameras
     */
    private fun showSelectMountableLensesDialog(filter: Filter, fixedLensCameras: Boolean) {
        val compatibleLenses = if (fixedLensCameras) {
            cameras.mapNotNull(Camera::lens)
        } else {
            lenses
        }.filter { lens -> filter.lensIds.contains(lens.id) }

        val allLenses = if (fixedLensCameras) {
            cameras.filter(Camera::isFixedLens).mapNotNull { camera ->
                camera.lens?.make = camera.make
                camera.lens?.model = camera.model
                camera.lens
            }
        } else {
            lenses
        }

        // Make a list of strings for all the lens names to be shown in the multi choice list.
        // If the lens is actually a fixed-lens camera, show the camera name instead.
        val listItems = allLenses.map(Lens::name).toTypedArray()
        // Create a bool array for preselected items in the multi choice list.
        val selections = allLenses.map(compatibleLenses::contains).toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        val titleId = if (fixedLensCameras) {
            R.string.SelectMountableCameras
        } else {
            R.string.SelectMountableLenses
        }
        builder.setTitle(titleId)
                .setMultiChoiceItems(listItems, selections) { _, which, isChecked ->
                    selections[which] = isChecked
                }
                .setPositiveButton(R.string.OK) { _, _ ->
                    val (added, removed) = selections
                        .zip(allLenses) { selected, lens ->
                            val beforeState = compatibleLenses.contains(lens)
                            Triple(lens, beforeState, selected)
                        }
                        .filter { it.second != it.third }
                        .partition(Triple<Lens, Boolean, Boolean>::third)
                    added.forEach {
                        model.addLensFilterLink(filter, it.first, isFixedLens = fixedLensCameras)
                    }
                    removed.forEach {
                        model.deleteLensFilterLink(filter, it.first, isFixedLens = fixedLensCameras)
                    }
                }
                .setNegativeButton(R.string.Cancel) { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

    private fun openFilterEditDialog(filter: Filter?) {
        val dialog = FilterEditDialog()
        val arguments = Bundle()
        if (filter != null) {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilter))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
            arguments.putParcelable(ExtraKeys.FILTER, filter)
        } else {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilter))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        }
        dialog.arguments = arguments
        val transaction = gearFragment.childFragmentManager
            .beginTransaction()
            .addToBackStack(GearFragment.BACKSTACK_NAME)
        dialog.show(transaction, FilterEditDialog.TAG)
        dialog.setFragmentResultListener(FilterEditDialog.REQUEST_KEY) { _, bundle ->
            bundle.getParcelable<Filter>(ExtraKeys.FILTER)?.let(model::submitFilter)
        }
    }

    private fun confirmDeleteFilter(filter: Filter) {
        // Check if the filter is being used with one of the rolls.
        if (database.isFilterBeingUsed(filter)) {
            val message = resources.getString(R.string.FilterNoColon) +
                    " " + filter.name + " " +
                    resources.getString(R.string.IsBeingUsed)
            binding.root.snackbar(message, binding.fabFilters, Snackbar.LENGTH_SHORT)
            return
        }
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(resources.getString(R.string.ConfirmFilterDelete)
                + " \'" + filter.name + "\'?"
        )
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.OK) { _, _ ->
            model.deleteFilter(filter)
        }
        builder.create().show()
    }

}