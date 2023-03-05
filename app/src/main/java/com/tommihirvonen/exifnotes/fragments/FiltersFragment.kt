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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilterAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentFiltersBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel
import com.tommihirvonen.exifnotes.viewmodels.State

/**
 * Fragment to display all filters from the database along with details
 */
class FiltersFragment : Fragment() {

    private val model: GearViewModel by activityViewModels()

    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    private lateinit var binding: FragmentFiltersBinding

    @SuppressLint("NotifyDataSetChanged")
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.gear_dest)
        navBackStackEntry.observeThenClearNavigationResult<Filter>(
            viewLifecycleOwner, ExtraKeys.FILTER) { filter ->
            filter?.let(model::submitFilter)
        }
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
            R.string.SelectCompatibleCameras
        } else {
            R.string.SelectCompatibleLenses
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
        val (title, positiveButtonText) = if (filter == null) {
            resources.getString(R.string.AddNewFilter) to resources.getString(R.string.Add)
        } else {
            resources.getString(R.string.EditFilter) to resources.getString(R.string.OK)
        }
        val action = GearFragmentDirections.filterEditAction(filter, title, positiveButtonText)
        findNavController().navigate(action)
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