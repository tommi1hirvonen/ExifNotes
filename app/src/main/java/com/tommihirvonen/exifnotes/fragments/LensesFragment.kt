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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.LensAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentLensesBinding
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.repositories.LensRepository
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel
import com.tommihirvonen.exifnotes.viewmodels.State
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment to display all lenses from the database along with details
 */
@AndroidEntryPoint
class LensesFragment : Fragment() {

    @Inject lateinit var lensRepository: LensRepository

    // Share the ViewModel together with FiltersFragment and CamerasFragment
    // through the same navigation subgraph.
    private val model by navGraphViewModels<GearViewModel>(R.id.gear_navigation) {
        defaultViewModelProviderFactory
    }

    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    private lateinit var binding:FragmentLensesBinding

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentLensesBinding.inflate(inflater, container, false)

        binding.fabLenses.setOnClickListener { showEditLensFragment(binding.fabLenses, null) }

        val layoutManager = LinearLayoutManager(activity)
        binding.lensesRecyclerView.layoutManager = layoutManager

        val lensAdapter = LensAdapter(requireActivity(), onLensClickListener)
        binding.lensesRecyclerView.adapter = lensAdapter

        model.lenses.observe(viewLifecycleOwner) { lenses ->
            this.lenses = lenses
            lensAdapter.lenses = lenses
            binding.noAddedLenses.visibility = if (lenses.isEmpty()) View.VISIBLE else View.GONE
            lensAdapter.notifyDataSetChanged()
        }

        model.cameras.observe(viewLifecycleOwner) { state ->
            if (state is State.Success) {
                cameras = state.data
                lensAdapter.cameras = cameras
                lensAdapter.notifyDataSetChanged()
            }
        }

        model.filters.observe(viewLifecycleOwner) { filters ->
            this.filters = filters
            lensAdapter.filters = filters
            lensAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    private val onLensClickListener = { lens: Lens, view: View ->
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_lens_item, popup.menu)
        popup.setIconsVisible(requireContext())
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_cameras -> showSelectMountableCamerasDialog(lens)
                R.id.menu_item_filters -> showSelectMountableFiltersDialog(lens)
                R.id.menu_item_edit -> showEditLensFragment(view, lens)
                R.id.menu_item_delete -> confirmDeleteLens(lens)
            }
            true
        }
        popup.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeThenClearNavigationResult(ExtraKeys.LENS, model::submitLens)
    }

    private fun showEditLensFragment(sharedElement: View, lens: Lens?) {
        val title = if (lens == null) {
            resources.getString(R.string.AddNewLens)
        } else {
            resources.getString(R.string.EditLens)
        }
        val action = GearFragmentDirections
            .lensEditAction(lens, false, title, sharedElement.transitionName)
        val extras = FragmentNavigatorExtras(
            sharedElement to sharedElement.transitionName
        )
        findNavController().navigate(action, extras)
    }

    private fun confirmDeleteLens(lens: Lens) {
        // Check if the lens is being used with one of the frames.
        if (lensRepository.isLensInUse(lens)) {
            val message = resources.getString(R.string.LensNoColon) +
                    " " + lens.name + " " +
                    resources.getString(R.string.IsBeingUsed)
            binding.root.snackbar(message, binding.fabLenses)
            return
        }
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(resources.getString(R.string.ConfirmLensDelete)
                + " \'" + lens.name + "\'?"
        )
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.OK) { _, _ ->
            model.deleteLens(lens)
        }
        builder.create().show()
    }

    /**
     * Show dialog where the user can select which cameras can be mounted to the picked lens.
     *
     * @param lens Lens object for which mountable cameras should be selected
     */
    private fun showSelectMountableCamerasDialog(lens: Lens) {
        val compatibleCameras = cameras.filter { lens.cameraIds.contains(it.id) }
        val allCameras = cameras.filter(Camera::isNotFixedLens)
        // Make a list of strings for all the camera names to be shown in the multi choice list.
        val listItems = allCameras.map(Camera::name).toTypedArray()
        // Create a bool array for preselected items in the multi choice list.
        val selections = allCameras.map(compatibleCameras::contains).toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.SelectCompatibleCameras)
                .setMultiChoiceItems(listItems, selections) { _, which, isChecked ->
                    selections[which] = isChecked
                }
                .setPositiveButton(R.string.OK) { _, _ ->
                    val (added, removed) = selections
                        .zip(allCameras) { selected, camera ->
                            val beforeState = compatibleCameras.contains(camera)
                            Triple(camera, beforeState, selected)
                        }
                        .filter { it.second != it.third }
                        .partition(Triple<Camera, Boolean, Boolean>::third)
                    added.forEach { model.addCameraLensLink(it.first, lens) }
                    removed.forEach { model.deleteCameraLensLink(it.first, lens) }
                }
                .setNegativeButton(R.string.Cancel) { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Show dialog where the user can select which filters can be mounted to the picked lens.
     *
     * @param lens Lens object for which mountable filters should be selected
     */
    private fun showSelectMountableFiltersDialog(lens: Lens) {
        val compatibleFilters = filters.filter { lens.filterIds.contains(it.id) }
        // Make a list of strings for all the lens names to be shown in the multi choice list.
        val listItems = filters.map(Filter::name).toTypedArray()
        // Create a bool array for preselected items in the multi choice list.
        val selections = filters.map(compatibleFilters::contains).toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.SelectCompatibleFilters)
                .setMultiChoiceItems(listItems, selections) { _, which, isChecked ->
                    selections[which] = isChecked
                }
                .setPositiveButton(R.string.OK) { _, _ ->
                    val (added, removed) = selections
                        .zip(filters) { selected, filter ->
                            val beforeState = compatibleFilters.contains(filter)
                            Triple(filter, beforeState, selected)
                        }
                        .filter { it.second != it.third }
                        .partition(Triple<Filter, Boolean, Boolean>::third)
                    added.forEach {
                        model.addLensFilterLink(it.first, lens, isFixedLens = false)
                    }
                    removed.forEach {
                        model.deleteLensFilterLink(it.first, lens, isFixedLens = false)
                    }
                }
                .setNegativeButton(R.string.Cancel) { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

}