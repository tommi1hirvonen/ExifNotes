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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.LensAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentLensesBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.utilities.setCommonInterpolator
import com.tommihirvonen.exifnotes.utilities.snackbar
import com.tommihirvonen.exifnotes.viewmodels.GearViewModel
import com.tommihirvonen.exifnotes.viewmodels.State

/**
 * Fragment to display all lenses from the database along with details
 */
class LensesFragment : Fragment() {

    private val gearFragment by lazy {
        requireParentFragment().requireParentFragment() as GearFragment
    }
    private val model: GearViewModel by activityViewModels()
    private var cameras: List<Camera> = emptyList()
    private var lenses: List<Lens> = emptyList()
    private var filters: List<Filter> = emptyList()

    private lateinit var binding:FragmentLensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if an existing lens edit fragment is open after configuration change
        // and attach listener if so.
        val fragment = gearFragment.childFragmentManager.findFragmentByTag(LensEditFragment.TAG)
        fragment?.setFragmentResultListener(LensEditFragment.REQUEST_KEY) { _, bundle ->
            bundle.getParcelable<Lens>(ExtraKeys.LENS)?.let(model::submitLens)
        }
    }

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
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(lens.name)
        val items = arrayOf(
            requireActivity().getString(R.string.SelectMountableCameras),
            requireActivity().getString(R.string.SelectMountableFilters),
            requireActivity().getString(R.string.Edit),
            requireActivity().getString(R.string.Delete)
        )
        builder.setItems(items) { _, which ->
            when (which) {
                0 -> { showSelectMountableCamerasDialog(lens) }
                1 -> { showSelectMountableFiltersDialog(lens) }
                2 -> { showEditLensFragment(view, lens) }
                3 -> { confirmDeleteLens(lens) }
            }
        }
        builder.setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun showEditLensFragment(sharedElement: View, lens: Lens?) {
        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(FastOutSlowInInterpolator())
            .apply { duration = 250L }
        val fragment = LensEditFragment().apply {
            sharedElementEnterTransition = sharedElementTransition
        }
        val arguments = Bundle()
        arguments.putBoolean(ExtraKeys.FIXED_LENS, false)
        if (lens == null) {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewLens))
        } else {
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditLens))
            arguments.putParcelable(ExtraKeys.LENS, lens)
        }

        arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
        fragment.arguments = arguments

        gearFragment.childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(R.id.gear_fragment_container, fragment, LensEditFragment.TAG)
            .addToBackStack(GearFragment.BACKSTACK_NAME)
            .commit()

        fragment.setFragmentResultListener(LensEditFragment.REQUEST_KEY) { _, bundle ->
            bundle.getParcelable<Lens>(ExtraKeys.LENS)?.let(model::submitLens)
        }
    }

    private fun confirmDeleteLens(lens: Lens) {
        // Check if the lens is being used with one of the frames.
        if (database.isLensInUse(lens)) {
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
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
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

        val builder = MaterialAlertDialogBuilder(requireActivity())
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
     * @param lens Lens object for which mountable filters should be selected
     */
    private fun showSelectMountableFiltersDialog(lens: Lens) {
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