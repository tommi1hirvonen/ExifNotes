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
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.CameraAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentCamerasBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditCameraDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel

/**
 * Fragment to display all cameras from the database along with details
 */
class CamerasFragment : Fragment(), View.OnClickListener {

    private val model: GearViewModel by activityViewModels()
    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    private var fragmentVisible = false

    override fun onResume() {
        fragmentVisible = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        fragmentVisible = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding = FragmentCamerasBinding.inflate(inflater, container, false)
        binding.fabCameras.setOnClickListener(this)

        // Access the ListView
        val layoutManager = LinearLayoutManager(activity)
        binding.camerasRecyclerView.layoutManager = layoutManager
        binding.camerasRecyclerView.addItemDecoration(DividerItemDecoration(binding.camerasRecyclerView.context,
                layoutManager.orientation))

        // Create an ArrayAdapter for the ListView
        val cameraAdapter = CameraAdapter(requireActivity())
        binding.camerasRecyclerView.adapter = cameraAdapter

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

    @SuppressLint("CommitTransaction")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (fragmentVisible) {

            // Use the getOrder() method to unconventionally get the clicked item's position.
            // This is set to work correctly in the Adapter class.
            val position = item.order
            val camera = cameras[position]
            when (item.itemId) {
                CameraAdapter.MENU_ITEM_SELECT_MOUNTABLE_LENSES -> {
                    showSelectMountableLensesDialog(position)
                    return true
                }
                // Mountable filters can be selected for fixed lens cameras.
                CameraAdapter.MENU_ITEM_SELECT_MOUNTABLE_FILTERS -> {
                    showSelectMountableFiltersDialog(position)
                    return true
                }
                CameraAdapter.MENU_ITEM_DELETE -> {

                    // Check if the camera is being used with one of the rolls.
                    if (database.isCameraBeingUsed(camera)) {
                        Toast.makeText(activity, resources.getString(R.string.CameraNoColon) +
                                " " + camera.name + " " +
                                resources.getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show()
                        return true
                    }
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(resources.getString(R.string.ConfirmCameraDelete)
                            + " \'" + camera.name + "\'?"
                    )
                    builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        model.deleteCamera(camera)
                        camera.lens?.let { model.deleteLens(it) }
                    }
                    builder.create().show()
                    return true
                }
                CameraAdapter.MENU_ITEM_EDIT -> {
                    val dialog = EditCameraDialog()
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditCamera))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.CAMERA, camera)
                    dialog.arguments = arguments
                    dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Show EditCameraDialog to add a new camera to the database
     */
    @SuppressLint("CommitTransaction")
    private fun showCameraNameDialog() {
        val dialog = EditCameraDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewCamera))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab_cameras) {
            showCameraNameDialog()
        }
    }

    /**
     * Show dialog where the user can select which lenses can be mounted to the picked camera.
     *
     * @param position indicates the position of the picked camera in cameraList
     */
    private fun showSelectMountableLensesDialog(position: Int) {
        val camera = cameras[position]
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

        val builder = AlertDialog.Builder(requireActivity())
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
     * @param position indicates the position of the picked lens in lensList
     */
    private fun showSelectMountableFiltersDialog(position: Int) {
        val camera = cameras[position]
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

        val builder = AlertDialog.Builder(requireActivity())
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