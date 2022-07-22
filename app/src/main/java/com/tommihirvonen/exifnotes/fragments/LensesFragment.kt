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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.LensAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentLensesBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditLensDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.utilities.secondaryUiColor
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel

/**
 * Fragment to display all lenses from the database along with details
 */
class LensesFragment : Fragment(), View.OnClickListener {

    private val model: GearViewModel by activityViewModels()
    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

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
        val binding = FragmentLensesBinding.inflate(inflater, container, false)

        binding.fabLenses.setOnClickListener(this)

        // Also change the floating action button color. Use the darker secondaryColor for this.
        binding.fabLenses.backgroundTintList = ColorStateList.valueOf(secondaryUiColor)

        val layoutManager = LinearLayoutManager(activity)
        binding.lensesRecyclerView.layoutManager = layoutManager
        binding.lensesRecyclerView.addItemDecoration(DividerItemDecoration(binding.lensesRecyclerView.context,
                layoutManager.orientation))

        val lensAdapter = LensAdapter(requireActivity())
        binding.lensesRecyclerView.adapter = lensAdapter

        model.lenses.observe(viewLifecycleOwner) { lenses ->
            this.lenses = lenses
            lensAdapter.lenses = lenses
            binding.noAddedLenses.visibility = if (lenses.isEmpty()) View.VISIBLE else View.GONE
            lensAdapter.notifyDataSetChanged()
        }

        model.cameras.observe(viewLifecycleOwner) { cameras ->
            this.cameras = cameras
            lensAdapter.cameras = cameras
            lensAdapter.notifyDataSetChanged()
        }

        model.filters.observe(viewLifecycleOwner) { filters ->
            this.filters = filters
            lensAdapter.filters = filters
            lensAdapter.notifyDataSetChanged()
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
            val lens = lenses[position]
            when (item.itemId) {
                LensAdapter.MENU_ITEM_SELECT_MOUNTABLE_CAMERAS -> {
                    showSelectMountableCamerasDialog(position)
                    return true
                }
                LensAdapter.MENU_ITEM_SELECT_MOUNTABLE_FILTERS -> {
                    showSelectMountableFiltersDialog(position)
                    return true
                }
                LensAdapter.MENU_ITEM_DELETE -> {

                    // Check if the lens is being used with one of the frames.
                    if (database.isLensInUse(lens)) {
                        Toast.makeText(activity, resources.getString(R.string.LensNoColon) +
                                " " + lens.name + " " +
                                resources.getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show()
                        return true
                    }
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(resources.getString(R.string.ConfirmLensDelete)
                            + " \'" + lens.name + "\'?"
                    )
                    builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        model.deleteLens(lens)
                    }
                    builder.create().show()
                    return true
                }
                LensAdapter.MENU_ITEM_EDIT -> {
                    val dialog = EditLensDialog(fixedLens = false)
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditLens))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.LENS, lens)
                    arguments.putInt(ExtraKeys.POSITION, position)
                    dialog.arguments = arguments
                    dialog.show(parentFragmentManager.beginTransaction(), EditLensDialog.TAG)
                    dialog.setFragmentResultListener("EditLensDialog") { _, bundle ->
                        val lens1: Lens = bundle.getParcelable(ExtraKeys.LENS)
                            ?: return@setFragmentResultListener
                        if (lens1.make?.isNotEmpty() == true && lens1.model?.isNotEmpty() == true && lens1.id > 0) {
                            model.updateLens(lens1, isFixedLens = false)
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    /**
     * Show EditLensDialog to add a new lens to the database
     */
    @SuppressLint("CommitTransaction")
    private fun showLensNameDialog() {
        val dialog = EditLensDialog(fixedLens = false)
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewLens))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditLensDialog.TAG)
        dialog.setFragmentResultListener("EditLensDialog") { _, bundle ->
            val lens: Lens = bundle.getParcelable(ExtraKeys.LENS)
                ?: return@setFragmentResultListener
            if (lens.make?.isNotEmpty() == true && lens.model?.isNotEmpty() == true) {
                model.addLens(lens, isFixedLens = false)
            }
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab_lenses) {
            showLensNameDialog()
        }
    }

    /**
     * Show dialog where the user can select which cameras can be mounted to the picked lens.
     *
     * @param position indicates the position of the picked lens in lensList
     */
    private fun showSelectMountableCamerasDialog(position: Int) {
        val lens = lenses[position]
        val mountableCameras = cameras.filter { lens.cameraIds.contains(it.id) }
        val allCameras = cameras.filter { it.isNotFixedLens }

        // Create a list where the mountable selections are saved.
        val cameraSelections = allCameras.map {
            MountableState(it, mountableCameras.contains(it))
        }.toMutableList()

        // Make a list of strings for all the camera names to be shown in the multi choice list.
        val listItems = allCameras.map { it.name }.toTypedArray<CharSequence>()
        // Create a bool array for preselected items in the multi choice list.
        val booleans = cameraSelections.map { it.beforeState }.toBooleanArray()

        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.SelectMountableCameras)
                .setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    cameraSelections[which].afterState = isChecked
                }
                .setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->

                    // Go through new mountable combinations.
                    cameraSelections.filter { it.afterState != it.beforeState && it.afterState }.forEach {
                        model.addCameraLensLink(it.gear as Camera, lens)
                    }

                    // Go through removed mountable combinations.
                    cameraSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        model.deleteCameraLensLink(it.gear as Camera, lens)
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
        val lens = lenses[position]
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
                        model.addLensFilterLink(it.gear as Filter, lens, isFixedLens = false)
                    }

                    // Go through removed mountable combinations.
                    filterSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        model.deleteLensFilterLink(it.gear as Filter, lens, isFixedLens = false)
                    }
                }
                .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

}