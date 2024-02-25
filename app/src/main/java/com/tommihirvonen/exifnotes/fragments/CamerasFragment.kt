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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.CameraAdapter
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.databinding.FragmentCamerasBinding
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel
import com.tommihirvonen.exifnotes.viewmodels.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment to display all cameras from the database along with details
 */
@AndroidEntryPoint
class CamerasFragment : Fragment() {

    @Inject lateinit var cameraRepository: CameraRepository

    // Share the ViewModel together with FiltersFragment and LensesFragment
    // through the same navigation subgraph.
    private val model by navGraphViewModels<GearViewModel>(R.id.gear_navigation) {
        defaultViewModelProviderFactory
    }

    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    private lateinit var binding: FragmentCamerasBinding

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentCamerasBinding.inflate(inflater, container, false)

        binding.fabCameras.setOnClickListener {
            showEditCameraFragment(binding.fabCameras, null)
        }

        // Access the ListView
        val layoutManager = LinearLayoutManager(activity)
        binding.camerasRecyclerView.layoutManager = layoutManager

        // Create an ArrayAdapter for the ListView
        val cameraAdapter = CameraAdapter(requireActivity(), onCameraClickListener)
        binding.camerasRecyclerView.adapter = cameraAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.cameras.collect { state ->
                    when (state) {
                        is State.InProgress -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.noAddedCameras.visibility = View.GONE
                            cameras = emptyList()
                            cameraAdapter.cameras = cameras
                        }
                        is State.Success -> {
                            cameras = state.data
                            cameraAdapter.cameras = cameras
                            binding.progressBar.visibility = View.GONE
                            binding.noAddedCameras.visibility = if (cameras.isEmpty()) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                        }
                    }
                    cameraAdapter.notifyDataSetChanged()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.lenses.collect { lenses ->
                    this@CamerasFragment.lenses = lenses
                    cameraAdapter.lenses = lenses
                    cameraAdapter.notifyDataSetChanged()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.filters.collect { filters ->
                    this@CamerasFragment.filters = filters
                    cameraAdapter.filters = filters
                    cameraAdapter.notifyDataSetChanged()
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeThenClearNavigationResult(ExtraKeys.CAMERA, model::submitCamera)
    }

    @SuppressLint("RestrictedApi")
    private val onCameraClickListener = { camera: Camera, view: View ->
        val menuRes = if (camera.isFixedLens) {
            R.menu.menu_camera_item_fixed_lens
        } else {
            R.menu.menu_camera_item_interchangeable_lens
        }
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(menuRes, popup.menu)
        popup.setIconsVisible(requireContext())
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_filters -> showSelectMountableFiltersDialog(camera)
                R.id.menu_item_lenses -> showSelectMountableLensesDialog(camera)
                R.id.menu_item_edit -> showEditCameraFragment(view, camera)
                R.id.menu_item_delete -> confirmDeleteCamera(camera)
            }
            true
        }
        popup.show()
    }

    private fun confirmDeleteCamera(camera: Camera) {
        // Check if the camera is being used with one of the rolls.
        if (cameraRepository.isCameraBeingUsed(camera)) {
            val message = resources.getString(R.string.CameraNoColon) +
                    " " + camera.name + " " +
                    resources.getString(R.string.IsBeingUsed)
            binding.root.snackbar(message, binding.fabCameras, Snackbar.LENGTH_SHORT)
            return
        }
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(resources.getString(R.string.ConfirmCameraDelete)
                + " \'" + camera.name + "\'?"
        )
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.OK) { _, _ ->
            model.deleteCamera(camera)
        }
        builder.create().show()
    }

    private fun showEditCameraFragment(sharedElement: View, camera: Camera?) {
        val title = if (camera == null) {
            resources.getString(R.string.AddNewCamera)
        } else {
            resources.getString(R.string.EditCamera)
        }
        val action = GearFragmentDirections.cameraEditAction(camera, title, sharedElement.transitionName)
        val extras = FragmentNavigatorExtras(
            sharedElement to sharedElement.transitionName
        )
        findNavController().navigate(action, extras)
    }

    /**
     * Show dialog where the user can select which lenses can be mounted to the picked camera.
     *
     * @param camera Camera object for which mountable lenses should be selected
     */
    private fun showSelectMountableLensesDialog(camera: Camera) {
        val compatibleLenses = lenses.filter { camera.lensIds.contains(it.id) }
        // Make a list of strings for all the lens names to be shown in the multi choice list.
        val listItems = lenses.map(Lens::name).toTypedArray()
        // Create a bool array for preselected items in the multi choice list.
        val selections = lenses.map(compatibleLenses::contains).toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.SelectCompatibleLenses)
                .setMultiChoiceItems(listItems, selections) { _, which, isChecked ->
                    selections[which] = isChecked
                }
                .setPositiveButton(R.string.OK) { _, _ ->
                    // Go through changed combinations and add or remove them.
                    val (added, removed) = selections
                        .zip(lenses) { selected, lens ->
                            val beforeState = compatibleLenses.contains(lens)
                            Triple(lens, beforeState, selected)
                        }
                        .filter { it.second != it.third }
                        .partition(Triple<Lens, Boolean, Boolean>::third)
                    added.forEach { model.addCameraLensLink(camera, it.first) }
                    removed.forEach { model.deleteCameraLensLink(camera, it.first) }
                }
                .setNegativeButton(R.string.Cancel) { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Show dialog where the user can select which filters can be mounted to the picked lens.
     *
     * @param camera Camera object for which mountable filters should be selected
     */
    private fun showSelectMountableFiltersDialog(camera: Camera) {
        val lens = camera.lens ?: return // Dialog should only be shown to fixed lens cameras.

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
                // Go through changed combinations and add or remove them.
                val (added, removed) = selections
                    .zip(filters) { selected, filter ->
                        val beforeState = compatibleFilters.contains(filter)
                        Triple(filter, beforeState, selected)
                    }
                    .filter { it.second != it.third }
                    .partition(Triple<Filter, Boolean, Boolean>::third)
                added.forEach { model.addLensFilterLink(it.first, lens, isFixedLens = true) }
                removed.forEach { model.deleteLensFilterLink(it.first, lens, isFixedLens = true) }
            }
            .setNegativeButton(R.string.Cancel) { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

}