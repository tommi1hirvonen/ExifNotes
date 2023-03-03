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
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.CameraAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentCamerasBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel
import com.tommihirvonen.exifnotes.viewmodels.State

/**
 * Fragment to display all cameras from the database along with details
 */
class CamerasFragment : Fragment() {

    private val gearFragment by lazy {
        requireParentFragment().requireParentFragment() as GearFragment
    }
    private val model: GearViewModel by activityViewModels()
    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    private lateinit var binding: FragmentCamerasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if an existing camera edit fragment is open after configuration change
        // and attach listener if so.
        val fragment = gearFragment.childFragmentManager.findFragmentByTag(CameraEditFragment.TAG)
        fragment?.setFragmentResultListener(CameraEditFragment.REQUEST_KEY) { _, bundle ->
            bundle.parcelable<Camera>(ExtraKeys.CAMERA)?.let(model::submitCamera)
        }
    }

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

        model.cameras.observe(viewLifecycleOwner) { state ->
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
                    binding.noAddedCameras.visibility = if (cameras.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            cameraAdapter.notifyDataSetChanged()
        }

        model.lenses.observe(viewLifecycleOwner) { lenses ->
            this.lenses = lenses
            cameraAdapter.lenses = lenses
            cameraAdapter.notifyDataSetChanged()
        }

        model.filters.observe(viewLifecycleOwner) { filters ->
            this.filters = filters
            cameraAdapter.filters = filters
            cameraAdapter.notifyDataSetChanged()
        }

        return binding.root
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
        if (database.isCameraBeingUsed(camera)) {
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
        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(FastOutSlowInInterpolator())
            .apply { duration = 250L }

        val fragment = CameraEditFragment().apply {
            sharedElementEnterTransition = sharedElementTransition
        }

        val arguments = Bundle()
        if (camera == null) {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewCamera))
        } else {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditCamera))
            arguments.putParcelable(ExtraKeys.CAMERA, camera)
        }

        val fragmentContainerId = R.id.gear_fragment_container

        arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
        arguments.putString(ExtraKeys.BACKSTACK_NAME, GearFragment.BACKSTACK_NAME)
        arguments.putInt(ExtraKeys.FRAGMENT_CONTAINER_ID, fragmentContainerId)
        fragment.arguments = arguments

        gearFragment.childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(fragmentContainerId, fragment, CameraEditFragment.TAG)
            .addToBackStack(GearFragment.BACKSTACK_NAME)
            .commit()
        fragment.setFragmentResultListener(CameraEditFragment.REQUEST_KEY) { _, bundle ->
            bundle.parcelable<Camera>(ExtraKeys.CAMERA)?.let(model::submitCamera)
        }
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