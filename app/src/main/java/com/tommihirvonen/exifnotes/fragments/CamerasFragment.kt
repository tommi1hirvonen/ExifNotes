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
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.GearActivity
import com.tommihirvonen.exifnotes.adapters.GearAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentCamerasBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditCameraDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.utilities.secondaryUiColor

/**
 * Fragment to display all cameras from the database along with details
 */
class CamerasFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentCamerasBinding

    /**
     * Adapter used to adapt cameraList to binding.camerasRecyclerView
     */
    private lateinit var cameraAdapter: GearAdapter

    /**
     * Contains all cameras from the database
     */
    private lateinit var cameraList: MutableList<Camera>

    private var fragmentVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        cameraList = database.getCameras().toMutableList()
        cameraList.sort()
        binding = FragmentCamerasBinding.inflate(inflater, container, false)
        binding.fabCameras.setOnClickListener(this)

        // Also change the floating action button color. Use the darker secondaryColor for this.
        binding.fabCameras.backgroundTintList = ColorStateList.valueOf(secondaryUiColor)

        // Access the ListView
        val layoutManager = LinearLayoutManager(activity)
        binding.camerasRecyclerView.layoutManager = layoutManager
        binding.camerasRecyclerView.addItemDecoration(DividerItemDecoration(binding.camerasRecyclerView.context,
                layoutManager.orientation))

        // Create an ArrayAdapter for the ListView
        cameraAdapter = GearAdapter(requireActivity(), cameraList)

        // Set the ListView to use the ArrayAdapter
        binding.camerasRecyclerView.adapter = cameraAdapter
        if (cameraList.size >= 1) binding.noAddedCameras.visibility = View.GONE
        cameraAdapter.notifyDataSetChanged()
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
            val camera = cameraList[position]
            when (item.itemId) {
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_LENSES -> {
                    showSelectMountableLensesDialog(position)
                    return true
                }
                // Mountable filters can be selected for fixed lens cameras.
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_FILTERS -> {
                    showSelectMountableFiltersDialog(position)
                    return true
                }
                GearAdapter.MENU_ITEM_DELETE -> {

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
                        database.deleteCamera(camera)
                        camera.lens?.let { database.deleteLens(it) }

                        // Remove the camera from the cameraList. Do this last!
                        cameraList.removeAt(position)
                        if (cameraList.size == 0) binding.noAddedCameras.visibility = View.VISIBLE
                        cameraAdapter.notifyItemRemoved(position)

                        // Update the LensesFragment through the parent activity.
                        val gearActivity = requireActivity() as GearActivity
                        gearActivity.updateFragments()
                    }
                    builder.create().show()
                    return true
                }
                GearAdapter.MENU_ITEM_EDIT -> {
                    val dialog = EditCameraDialog()
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditCamera))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.CAMERA, camera)
                    dialog.arguments = arguments
                    dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
                    dialog.setFragmentResultListener("EditCameraDialog") { _, bundle ->
                        val camera1: Camera = bundle.getParcelable(ExtraKeys.CAMERA)
                            ?: return@setFragmentResultListener
                        val oldPos = cameraList.indexOf(camera1)
                        cameraList.sort()
                        val newPos = cameraList.indexOf(camera1)
                        cameraAdapter.notifyItemChanged(oldPos)
                        cameraAdapter.notifyItemMoved(oldPos, newPos)
                        binding.camerasRecyclerView.scrollToPosition(newPos)

                        // Update the LensesFragment through the parent activity.
                        val gearActivity = requireActivity() as GearActivity
                        gearActivity.updateFragments()
                    }
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
        dialog.setFragmentResultListener("EditCameraDialog") { _, bundle ->
            val camera: Camera = bundle.getParcelable(ExtraKeys.CAMERA)
                ?: return@setFragmentResultListener
            if (camera.make?.isNotEmpty() == true && camera.model?.isNotEmpty() == true) {
                binding.noAddedCameras.visibility = View.GONE
                cameraList.add(camera)
                cameraList.sort()
                val listPos = cameraList.indexOf(camera)
                cameraAdapter.notifyItemInserted(listPos)

                // When the lens is added jump to view the last entry
                binding.camerasRecyclerView.scrollToPosition(listPos)
            }
        }
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
        val camera = cameraList[position]
        val mountableLenses = database.getLinkedLenses(camera)
        val allLenses = database.getLenses()

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
                        database.addCameraLensLink(camera, it.gear as Lens)
                    }

                    // Go through removed mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        database.deleteCameraLensLink(camera, it.gear as Lens)
                    }

                    cameraAdapter.notifyItemChanged(position)

                    // Update the LensesFragment through the parent activity.
                    val gearActivity = requireActivity() as GearActivity
                    gearActivity.updateFragments()
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
        val camera = cameraList[position]
        val lens = camera.lens ?: return
        val mountableFilters = database.getLinkedFilters(lens)
        val allFilters = database.allFilters

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
                    database.addLensFilterLink(it.gear as Filter, lens)
                }

                // Go through removed mountable combinations.
                filterSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                    database.deleteLensFilterLink(it.gear as Filter, lens)
                }

                cameraAdapter.notifyItemChanged(position)

                // Update the FiltersFragment through the parent activity.
                val gearActivity = requireActivity() as GearActivity
                gearActivity.updateFragments()
            }
            .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Public method to update the contents of this fragment's ListView
     */
    fun updateFragment() {
        cameraAdapter.notifyDataSetChanged()
    }

}