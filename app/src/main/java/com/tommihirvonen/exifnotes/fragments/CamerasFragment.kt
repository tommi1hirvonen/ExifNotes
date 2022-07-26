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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.CameraAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentCamerasBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditCameraDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.OnScrollExtendedFabListener
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel

/**
 * Fragment to display all cameras from the database along with details
 */
class CamerasFragment : Fragment() {

    private val model: GearViewModel by activityViewModels()
    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding = FragmentCamerasBinding.inflate(inflater, container, false)

        binding.fabCameras.setOnClickListener { openNewCameraDialog() }

        // Access the ListView
        val layoutManager = LinearLayoutManager(activity)
        binding.camerasRecyclerView.layoutManager = layoutManager
        binding.camerasRecyclerView.addItemDecoration(DividerItemDecoration(binding.camerasRecyclerView.context,
                layoutManager.orientation))

        // Create an ArrayAdapter for the ListView
        val cameraAdapter = CameraAdapter(requireActivity(), onCameraClickListener)
        binding.camerasRecyclerView.adapter = cameraAdapter

        binding.camerasRecyclerView.addOnScrollListener(OnScrollExtendedFabListener(binding.fabCameras))

        model.cameras.observe(viewLifecycleOwner) { cameras ->
            this.cameras = cameras
            cameraAdapter.cameras = cameras
            binding.noAddedCameras.visibility = if (cameras.isEmpty()) View.VISIBLE else View.GONE
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

    private val onCameraClickListener = { camera: Camera ->
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(camera.name)
        val items = arrayOf(
            if (camera.isNotFixedLens) {
                requireActivity().getString(R.string.SelectMountableLenses)
            } else {
                requireActivity().getString(R.string.SelectMountableFilters)
            },
            requireActivity().getString(R.string.Edit),
            requireActivity().getString(R.string.Delete)
        )
        builder.setItems(items) { _, which ->
            when {
                which == 0 && camera.isNotFixedLens -> { showSelectMountableLensesDialog(camera) }
                which == 0 && camera.isFixedLens -> { showSelectMountableFiltersDialog(camera) }
                which == 1 -> { openEditCameraDialog(camera) }
                which == 2 -> { confirmDeleteCamera(camera) }
            }
        }
        builder.setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun confirmDeleteCamera(camera: Camera) {
        // Check if the camera is being used with one of the rolls.
        if (database.isCameraBeingUsed(camera)) {
            Toast.makeText(activity, resources.getString(R.string.CameraNoColon) +
                    " " + camera.name + " " +
                    resources.getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show()
            return
        }
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(resources.getString(R.string.ConfirmCameraDelete)
                + " \'" + camera.name + "\'?"
        )
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
            model.deleteCamera(camera)
            camera.lens?.let { model.deleteLens(it) }
        }
        builder.create().show()
    }

    private fun openNewCameraDialog() {
        val dialog = EditCameraDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewCamera))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
    }

    private fun openEditCameraDialog(camera: Camera) {
        val dialog = EditCameraDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditCamera))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
        arguments.putParcelable(ExtraKeys.CAMERA, camera)
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
    }

    /**
     * Show dialog where the user can select which lenses can be mounted to the picked camera.
     *
     * @param camera Camera object for which mountable lenses should be selected
     */
    private fun showSelectMountableLensesDialog(camera: Camera) {
        val mountableLenses = lenses.filter { camera.lensIds.contains(it.id) }
        val allLenses = lenses

        // Create a list where the mountable selections are saved.
        val lensSelections = allLenses.map {
            MountableState(it, mountableLenses.contains(it))
        }.toMutableList()

        // Make a list of strings for all the lens names to be shown in the multi choice list.
        val listItems = allLenses.map { it.name }.toTypedArray<CharSequence>()
        // Create a bool array for preselected items in the multi choice list.
        val booleans = lensSelections.map { it.beforeState }.toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.SelectMountableLenses)
                .setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    lensSelections[which].afterState = isChecked
                }
                .setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->

                    // Go through new mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && it.afterState }.forEach {
                        model.addCameraLensLink(camera, it.gear as Lens)
                    }

                    // Go through removed mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        model.deleteCameraLensLink(camera, it.gear as Lens)
                    }
                }
                .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Show dialog where the user can select which filters can be mounted to the picked lens.
     *
     * @param camera Camera object for which mountable filters should be selected
     */
    private fun showSelectMountableFiltersDialog(camera: Camera) {
        val lens = camera.lens ?: return
        val mountableFilters = filters.filter { lens.filterIds.contains(it.id) }
        val allFilters = filters

        // Create a list where the mountable selections are saved.
        val filterSelections = allFilters.map {
            MountableState(it, mountableFilters.contains(it))
        }.toMutableList()

        // Make a list of strings for all the lens names to be shown in the multi choice list.
        val listItems = allFilters.map { it.name }.toTypedArray<CharSequence>()
        // Create a bool array for preselected items in the multi choice list.
        val booleans = filterSelections.map { it.beforeState }.toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.SelectMountableFilters)
            .setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                filterSelections[which].afterState = isChecked
            }
            .setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->

                // Go through new mountable combinations.
                filterSelections.filter { it.afterState != it.beforeState && it.afterState }.forEach {
                    model.addLensFilterLink(it.gear as Filter, lens, isFixedLens = true)
                }

                // Go through removed mountable combinations.
                filterSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                    model.deleteLensFilterLink(it.gear as Filter, lens, isFixedLens = true)
                }
            }
            .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

}