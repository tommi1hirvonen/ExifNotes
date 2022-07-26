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

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilterAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentFiltersBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditFilterDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel

/**
 * Fragment to display all filters from the database along with details
 */
class FiltersFragment : Fragment() {

    private val model: GearViewModel by activityViewModels()
    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding = FragmentFiltersBinding.inflate(inflater, container, false)
        binding.fabFilters.setOnClickListener(onFabClickListener)

        val layoutManager = LinearLayoutManager(activity)
        binding.filtersRecyclerView.layoutManager = layoutManager
        binding.filtersRecyclerView.addItemDecoration(DividerItemDecoration(binding.filtersRecyclerView.context,
                layoutManager.orientation))

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

        model.cameras.observe(viewLifecycleOwner) { cameras ->
            this.cameras = cameras
            filterAdapter.cameras = cameras
            filterAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    private val onFabClickListener = { _: View ->
        val dialog = EditFilterDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilter))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditFilterDialog.TAG)
        dialog.setFragmentResultListener("EditFilterDialog") { _, bundle ->
            val filter: Filter = bundle.getParcelable(ExtraKeys.FILTER)
                ?: return@setFragmentResultListener
            if (filter.make?.isNotEmpty() == true && filter.model?.isNotEmpty() == true) {
                model.addFilter(filter)
            }
        }
    }

    private val onFilterClickListener = { filter: Filter ->
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(filter.name)
        val items = arrayOf(
            requireActivity().getString(R.string.SelectMountableLenses),
            requireActivity().getString(R.string.SelectMountableCameras),
            requireActivity().getString(R.string.Edit),
            requireActivity().getString(R.string.Delete)
        )
        builder.setItems(items) { _, which ->
            when (which) {
                0 -> { showSelectMountableLensesDialog(filter, fixedLensCameras = false) }
                1 -> { showSelectMountableLensesDialog(filter, fixedLensCameras = true) }
                2 -> { openFilterEditDialog(filter) }
                3 -> { confirmDeleteFilter(filter) }
            }
        }
        builder.setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    /**
     * Show dialog where the user can select which lenses can be mounted to the picked filter.
     *
     * @param filter Filter object for which lens selections should be made
     */
    private fun showSelectMountableLensesDialog(filter: Filter, fixedLensCameras: Boolean) {
        val mountableLenses = if (fixedLensCameras) {
            cameras.mapNotNull { it.lens }
        } else {
            lenses
        }.filter { filter.lensIds.contains(it.id) }
        val allLenses = if (fixedLensCameras) {
            cameras.filter { it.isFixedLens }.mapNotNull {
                it.lens?.make = it.make
                it.lens?.model = it.model
                it.lens
            }
        } else {
            lenses
        }

        // Create a list where the mountable selections are saved.
        val lensSelections = allLenses.map { lens ->
            MountableState(lens, mountableLenses.any { it.id == lens.id })
        }.toMutableList()

        // Make a list of strings for all the lens names to be shown in the multi choice list.
        // If the lens is actually a fixed-lens camera, show the camera name instead.
        val listItems = allLenses.map { it.name }.toTypedArray<CharSequence>()
        // Create a bool array for preselected items in the multi choice list.
        val booleans = lensSelections.map { it.beforeState }.toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        val titleId = if (fixedLensCameras) {
            R.string.SelectMountableCameras
        } else {
            R.string.SelectMountableLenses
        }
        builder.setTitle(titleId)
                .setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    lensSelections[which].afterState = isChecked
                }
                .setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->

                    // Go through new mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && it.afterState }.forEach {
                        model.addLensFilterLink(filter, it.gear as Lens, isFixedLens = fixedLensCameras)
                    }

                    // Go through removed mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        model.deleteLensFilterLink(filter, it.gear as Lens, isFixedLens = fixedLensCameras)
                    }
                }
                .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    private fun openFilterEditDialog(filter: Filter) {
        val dialog = EditFilterDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilter))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
        arguments.putParcelable(ExtraKeys.FILTER, filter)
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditFilterDialog.TAG)
        dialog.setFragmentResultListener("EditFilterDialog") { _, bundle ->
            val filter1: Filter = bundle.getParcelable(ExtraKeys.FILTER)
                ?: return@setFragmentResultListener
            if (filter1.make?.isNotEmpty() == true && filter1.model?.isNotEmpty() == true && filter1.id > 0) {
                model.updateFilter(filter1)
            }
        }
    }

    private fun confirmDeleteFilter(filter: Filter) {
        // Check if the filter is being used with one of the rolls.
        if (database.isFilterBeingUsed(filter)) {
            Toast.makeText(activity, resources.getString(R.string.FilterNoColon) +
                    " " + filter.name + " " +
                    resources.getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show()
        }
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(resources.getString(R.string.ConfirmFilterDelete)
                + " \'" + filter.name + "\'?"
        )
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
            model.deleteFilter(filter)
        }
        builder.create().show()
    }

}