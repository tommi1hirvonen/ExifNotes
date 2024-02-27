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
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.*
import com.tommihirvonen.exifnotes.adapters.FrameAdapter
import com.tommihirvonen.exifnotes.adapters.FrameAdapter.FrameAdapterListener
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.FrameSortMode
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.LightSource
import com.tommihirvonen.exifnotes.utilities.LocationPickResponse
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.localDateTimeOrNull
import com.tommihirvonen.exifnotes.data.repositories.CameraRepository
import com.tommihirvonen.exifnotes.data.repositories.FilterRepository
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.data.repositories.LabelRepository
import com.tommihirvonen.exifnotes.data.repositories.LensRepository
import com.tommihirvonen.exifnotes.databinding.DialogSingleDropdownBinding
import com.tommihirvonen.exifnotes.databinding.FragmentFramesListBinding
import com.tommihirvonen.exifnotes.rollexport.RollExportHelper
import com.tommihirvonen.exifnotes.rollexport.RollExportOption
import com.tommihirvonen.exifnotes.rollexport.RollShareIntentBuilder
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.FramesViewModel
import com.tommihirvonen.exifnotes.viewmodels.FramesViewModelFactory
import com.tommihirvonen.exifnotes.viewmodels.RollsViewModel
import com.tommihirvonen.exifnotes.viewmodels.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * FramesFragment is the fragment which is called when the user presses on a roll
 * on the ListView in RollsFragment. It displays all the frames from that roll.
 */
@AndroidEntryPoint
class FramesListFragment : LocationUpdatesFragment(), FrameAdapterListener {

    @Inject lateinit var frameRepository: FrameRepository
    @Inject lateinit var cameraRepository: CameraRepository
    @Inject lateinit var labelRepository: LabelRepository
    @Inject lateinit var lensRepository: LensRepository
    @Inject lateinit var filterRepository: FilterRepository
    @Inject lateinit var complementaryPicturesManager: ComplementaryPicturesManager
    @Inject lateinit var rollShareIntentBuilder: RollShareIntentBuilder
    @Inject lateinit var rollExportHelper: RollExportHelper

    val arguments by navArgs<FramesListFragmentArgs>()

    private val rollsModel by activityViewModels<RollsViewModel>()
    private val model by navGraphViewModels<FramesViewModel>(R.id.frames_navigation) {
        FramesViewModelFactory(requireActivity().application,
            frameRepository, arguments.roll)
    }

    private val roll get() = model.roll.value

    private lateinit var frameAdapter: FrameAdapter
    private lateinit var binding: FragmentFramesListBinding
    private var frames = emptyList<Frame>()

    /**
     * Private callback class which is given as an argument when the SupportActionMode is started.
     */
    private val actionModeCallback = ActionModeCallback()

    /**
     * Reference to the (Support)ActionMode, which is launched when a list item is long pressed.
     */
    private var actionMode: ActionMode? = null

    private var selectedRollExportOptions = emptyList<RollExportOption>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        postponeEnterTransition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .setCommonInterpolator(FastOutSlowInInterpolator())
            .apply { duration = 400L }

        sharedElementEnterTransition = sharedElementTransition
        sharedElementReturnTransition = sharedElementTransition
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentFramesListBinding.inflate(inflater, container, false)
        binding.root.transitionName = arguments.transitionName
        binding.topAppBar.transitionName = "frames_top_app_bar_transition"
        binding.viewmodel = model
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.topAppBar.setOnMenuItemClickListener(onTopMenuItemClick)
        binding.bottomAppBar.setOnMenuItemClickListener(onBottomMenuItemSelected)
        binding.fab.setOnClickListener { showEditFrameFragment(null, binding.bottomAppBar) }

        val layoutManager = LinearLayoutManager(activity)
        binding.framesRecyclerView.layoutManager = layoutManager

        frameAdapter = FrameAdapter(requireActivity(), complementaryPicturesManager,
            this, binding.framesRecyclerView)
        binding.framesRecyclerView.adapter = frameAdapter
        frameAdapter.onItemSelectedChanged = { frame, selected ->
            if (selected) {
                model.selectedFrames.add(frame)
            } else {
                model.selectedFrames.remove(frame)
            }
        }
        frameAdapter.onAllSelectionsChanged = { selected ->
            if (selected) {
                val framesToAdd = frames.filter { f -> model.selectedFrames.any { it.id == f.id } }
                model.selectedFrames.addAll(framesToAdd)
            } else {
                model.selectedFrames.clear()
            }
        }

        val bottomMenu = binding.bottomAppBar.menu
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.frameSortMode.collect { mode ->
                    when (mode) {
                        FrameSortMode.FRAME_COUNT -> bottomMenu.findItem(R.id.frame_count_sort_mode).isChecked = true
                        FrameSortMode.DATE -> bottomMenu.findItem(R.id.date_sort_mode).isChecked = true
                        FrameSortMode.F_STOP -> bottomMenu.findItem(R.id.f_stop_sort_mode).isChecked = true
                        FrameSortMode.SHUTTER_SPEED -> bottomMenu.findItem(R.id.shutter_speed_sort_mode).isChecked = true
                        FrameSortMode.LENS -> bottomMenu.findItem(R.id.lens_sort_mode).isChecked = true
                    }
                }
            }
        }

        val topMenu = binding.topAppBar.menu
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.roll.collect { roll ->
                    topMenu.findItem(R.id.menu_item_favorite_on).isVisible = !roll.favorite
                    topMenu.findItem(R.id.menu_item_favorite_off).isVisible = roll.favorite
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.frames.collect { state ->
                    if (state !is State.Success) {
                        return@collect
                    }
                    val frames = state.data
                    this@FramesListFragment.frames = frames
                    frameAdapter.items = frames
                    if (model.selectedFrames.isNotEmpty()) {
                        frameAdapter.setSelections(model.selectedFrames)
                        ensureActionMode()
                    }
                    binding.noAddedFrames.visibility = if (frames.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    frameAdapter.notifyDataSetChanged()
                    startPostponedEnterTransition()
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeThenClearNavigationResult<Frame>(ExtraKeys.FRAME) { frame ->
            actionMode?.finish()
            model.submitFrame(frame)
        }
        observeThenClearNavigationResult(ExtraKeys.ROLL) { roll: Roll ->
            rollsModel.submitRoll(roll)
            model.setRoll(roll)
        }
        observeThenClearNavigationResult<Label>(ExtraKeys.LABEL) { label ->
            labelRepository.addLabel(label)
            roll.labels = roll.labels
                .plus(label)
                .sortedBy { it.name }
            rollsModel.submitRoll(roll)
        }
        observeThenClearNavigationResult<LocationPickResponse>(ExtraKeys.LOCATION) { response ->
            val (location, formattedAddress) = response
            model.selectedFrames.forEach { frame ->
                frame.location = location
                frame.formattedAddress = formattedAddress
                model.submitFrame(frame)
            }
        }
    }

    override fun onItemClick(frame: Frame, view: View) {
        if (model.selectedFrames.isNotEmpty() || actionMode != null) {
            enableActionMode(frame)
        } else {
            showEditFrameFragment(frame, view)
        }
    }

    override fun onItemLongClick(frame: Frame) {
        enableActionMode(frame)
    }

    private fun showEditFrameFragment(frame: Frame?, sharedElement: View) {
        val title = if (frame == null) {
            requireActivity().resources.getString(R.string.AddNewFrame)
        } else {
            "${requireActivity().getString(R.string.EditFrame)}${frame.count}"
        }

        val frameToEdit = frame ?: Frame(roll).apply {
            // If the frame count is greater than 100, then don't add a new frame.
            val nextFrameCount = frames.maxOfOrNull(Frame::count)?.plus(1) ?: 1
            if (nextFrameCount > 100) {
                binding.container.snackbar(R.string.TooManyFrames, binding.bottomAppBar)
                return
            }
            date = LocalDateTime.now()
            count = nextFrameCount
            noOfExposures = 1
            //Get the location only if the app has location permission (locationPermissionsGranted) and
            //the user has enabled GPS updates in the app's settings.
            if (locationPermissionsGranted && requestingLocationUpdates) {
                lastLocation?.let { location = LatLng(it.latitude, it.longitude) }
            }
            //Get the information for the last added frame.
            //The last added frame has the highest id number (database autoincrement).
            val previousFrame = frames.maxByOrNull(Frame::id)
            // Here we can list the properties we want to bring from the previous frame
            previousFrame?.let {
                lens = it.lens
                shutter = it.shutter
                aperture = it.aperture
                filters = it.filters
                focalLength = it.focalLength
                lightSource = it.lightSource
            }
        }

        val action = FramesListFragmentDirections
            .framesFrameEditAction(frameToEdit, title, sharedElement.transitionName)
        val extras = FragmentNavigatorExtras(
            sharedElement to sharedElement.transitionName
        )
        findNavController().navigate(action, extras)
    }

    private val onTopMenuItemClick = { item: MenuItem ->
        when (item.itemId) {
            R.id.menu_item_edit -> {
                val title = requireActivity().resources.getString(R.string.EditRoll)
                val action = FramesListFragmentDirections
                    .framesRollEditAction(roll, title, null)
                findNavController().navigate(action)
            }
            R.id.menu_item_favorite_on -> {
                model.toggleFavorite(true)
                rollsModel.submitRoll(roll)
            }
            R.id.menu_item_favorite_off -> {
                model.toggleFavorite(false)
                rollsModel.submitRoll(roll)
            }
            R.id.menu_item_labels -> {
                LabelsDialogBuilder().show()
            }
        }
        true
    }

    private val onBottomMenuItemSelected = { item: MenuItem ->
        when (item.itemId) {
            R.id.frame_count_sort_mode -> {
                item.isChecked = true
                model.setFrameSortMode(FrameSortMode.FRAME_COUNT)
            }
            R.id.date_sort_mode -> {
                item.isChecked = true
                model.setFrameSortMode(FrameSortMode.DATE)
            }
            R.id.f_stop_sort_mode -> {
                item.isChecked = true
                model.setFrameSortMode(FrameSortMode.F_STOP)
            }
            R.id.shutter_speed_sort_mode -> {
                item.isChecked = true
                model.setFrameSortMode(FrameSortMode.SHUTTER_SPEED)
            }
            R.id.lens_sort_mode -> {
                item.isChecked = true
                model.setFrameSortMode(FrameSortMode.LENS)
            }
            R.id.menu_item_gear -> {
                val action = FramesListFragmentDirections.framesGearAction()
                findNavController().navigate(action)
            }
            R.id.menu_item_preferences -> {
                val preferenceActivityIntent = Intent(activity, PreferenceActivity::class.java)
                preferenceResultLauncher.launch(preferenceActivityIntent)
            }
            R.id.menu_item_show_on_map -> {
                val action = FramesListFragmentDirections.framesMapAction()
                findNavController().navigate(action)
            }
            R.id.menu_item_share_intent ->
                ExportFileSelectDialogBuilder(R.string.FilesToShare) { selectedOptions ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val shareIntent = rollShareIntentBuilder.create(roll, selectedOptions)
                        shareIntent?.let { startActivity(Intent.createChooser(it, resources.getString(R.string.Share))) }
                    }
                }.create().show()
            R.id.menu_item_export -> {
                ExportFileSelectDialogBuilder(R.string.FilesToExport) { selectedOptions ->
                    val intent = Intent().apply { action = Intent.ACTION_OPEN_DOCUMENT_TREE }
                    selectedRollExportOptions = selectedOptions
                    exportResultLauncher.launch(intent)
                }.create().show()
            }
        }
        true
    }

    private inner class LabelsDialogBuilder : MaterialAlertDialogBuilder(requireActivity()) {
        init {
            val labels = rollsModel.labels.value
            val listItems = labels.map(Label::name).toTypedArray()
            val selections = labels.map { label ->
                roll.labels.any { it.id == label.id }
            }.toBooleanArray()
            setTitle(R.string.Labels)
            setMultiChoiceItems(listItems, selections) { _, which, isChecked ->
                selections[which] = isChecked
            }
            setPositiveButton(R.string.OK) { _, _ ->
                val (added, removed) = selections
                    .zip(labels) { selected, label ->
                        val before = roll.labels.any { l -> l.id == label.id }
                        Triple(label, before, selected)
                    }
                    .filter { it.second != it.third }
                    .partition { it.third }
                val (toAdd, toRemove) = added.map { it.first } to removed.map{ it.first }
                roll.labels = roll.labels
                    .filterNot { l -> toRemove.any { it.id == l.id } }
                    .plus(toAdd)
                    .sortedBy { it.name }
                rollsModel.submitRoll(roll)
            }
            setNeutralButton(R.string.NewLabel) { _, _ ->
                val action = FramesListFragmentDirections.framesLabelEditAction(null,
                    resources.getString(R.string.NewLabel),
                    resources.getString(R.string.Add))
                findNavController().navigate(action)
            }
            setNegativeButton(R.string.Cancel) { _, _ -> }
        }
    }

    private inner class ExportFileSelectDialogBuilder(
        titleStringResourceId: Int,
        onFileOptionsSelected: (List<RollExportOption>) -> Any?)
        : MaterialAlertDialogBuilder(requireContext()) {
        init {
            setTitle(titleStringResourceId)
            val options = RollExportOption.entries.toTypedArray()
            val items = options.map { it.toString() }.toTypedArray()
            val booleans = items.map { false }.toBooleanArray()
            setMultiChoiceItems(items, booleans) { _, which, isChecked ->
                booleans[which] = isChecked
            }
            setNegativeButton(R.string.Cancel) { _, _ -> }
            setPositiveButton(R.string.OK) { _, _ ->
                val selected = booleans
                    .zip(options)
                    .filter(Pair<Boolean, RollExportOption>::first)
                    .map(Pair<Boolean, RollExportOption>::second)
                onFileOptionsSelected(selected)
            }
        }
    }

    private val preferenceResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Finish this activity since the selected roll may not be valid anymore.
            if (result.resultCode and PreferenceActivity.RESULT_DATABASE_IMPORTED == PreferenceActivity.RESULT_DATABASE_IMPORTED) {
                requireActivity().recreate()
            }
        }

    private val exportResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val directoryUri = result.data?.data ?: return@registerForActivityResult
                    val directoryDocumentFile = DocumentFile.fromTreeUri(requireContext(), directoryUri)
                        ?: return@registerForActivityResult
                    rollExportHelper.export(roll, selectedRollExportOptions, directoryDocumentFile)
                    binding.root.snackbar(R.string.ExportedFilesSuccessfully, binding.bottomAppBar)
                } catch (e: IOException) {
                    e.printStackTrace()
                    binding.root.snackbar(R.string.ErrorExporting, binding.bottomAppBar)
                }
            }
        }

    private fun enableActionMode(frame: Frame) {
        frameAdapter.toggleSelection(frame)
        // If the user deselected the last of the selected items, exit action mode.
        if (model.selectedFrames.isEmpty()){
            actionMode?.finish()
        } else {
            ensureActionMode()
        }
    }

    private fun ensureActionMode() {
        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        // Set the action mode toolbar title to display the number of selected items.
        actionMode?.title = "${model.selectedFrames.size}/${frames.size}"
    }

    /**
     * Class which implements ActionMode.Callback.
     * One instance of this class is given as an argument when action mode is started.
     */
    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Hide the floating action button so no new rolls can be added while in action mode.
            binding.fab.hide()
            mode.menuInflater.inflate(R.menu.menu_action_mode_frames, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            // Get the positions in the frameList of selected items.
            val selectedFrames = model.selectedFrames
            return when (item.itemId) {
                R.id.menu_item_delete -> {
                    // Separate confirm titles for one or multiple frames
                    val title = resources.getQuantityString(
                        R.plurals.ConfirmFramesDelete, selectedFrames.size, selectedFrames.size)
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(title)
                        .setNegativeButton(R.string.Cancel) { _, _ -> }
                        .setPositiveButton(R.string.OK) { _, _ ->
                            selectedFrames.forEach(model::deleteFrame)
                            mode.finish()
                        }
                        .create().show()
                    true
                }
                R.id.menu_item_edit -> {

                    // If only one frame is selected, show frame edit dialog.
                    if (selectedFrames.size == 1) {
                        // Get the first of the selected rolls (only one should be selected anyway)
                        // Capture it before calling mode.finish() and resetting the selected frames list.
                        val frame = selectedFrames.first()
                        mode.finish()
                        showEditFrameFragment(frame, binding.topAppBar)
                    } else {
                        val builder = MaterialAlertDialogBuilder(requireActivity())
                        builder.setTitle(String.format(resources.getString(R.string.BatchEditFramesTitle), selectedFrames.size))
                        builder.setItems(R.array.FramesBatchEditOptions) { _, i ->
                            when (i) {
                                // Edit frame counts
                                0 -> {
                                    FrameCountBatchEditDialogBuilder(
                                        requireActivity(),
                                        R.string.EditFrameCountsBy,
                                        0) { countChange ->
                                        selectedFrames.forEach { frame ->
                                            frame.count += countChange
                                            model.submitFrame(frame)
                                        }
                                    }.create().show()
                                }
                                // Edit date and time
                                1 -> {
                                    val now = LocalDateTime.now()
                                    val datePicker = MaterialDatePicker.Builder.datePicker()
                                        .setSelection(now.epochMilliseconds)
                                        .build()
                                    datePicker.addOnPositiveButtonClickListener {
                                        val date = datePicker.selection?.let(::localDateTimeOrNull)
                                            ?: LocalDateTime.now()
                                        val timePicker = MaterialTimePicker.Builder()
                                            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                                            .setHour(now.hour)
                                            .setMinute(now.minute)
                                            .setTimeFormat(TimeFormat.CLOCK_24H)
                                            .build()
                                        timePicker.addOnPositiveButtonClickListener {
                                            val dateTime = LocalDateTime.of(
                                                date.year, date.monthValue, date.dayOfMonth,
                                                timePicker.hour, timePicker.minute)
                                            selectedFrames.forEach {
                                                it.date = dateTime
                                                model.submitFrame(it)
                                            }
                                        }
                                        timePicker.show(childFragmentManager, null)
                                    }
                                    datePicker.show(childFragmentManager, null)
                                }
                                // Edit lens
                                2 -> {
                                    MaterialAlertDialogBuilder(requireContext()).apply {
                                        setNegativeButton(R.string.Cancel) { _, _ -> }
                                        val lenses = roll.camera?.let(cameraRepository::getLinkedLenses)
                                            ?: lensRepository.lenses
                                        val listItems = listOf(resources.getString(R.string.NoLens))
                                            .plus(lenses.map(Lens::name)).toTypedArray()
                                        setItems(listItems) { dialog, which ->
                                            if (which == 0) {
                                                // No lens was selected
                                                selectedFrames.forEach {
                                                    it.lens = null
                                                    model.submitFrame(it)
                                                }
                                            } else {
                                                val lens = lenses[which - 1]
                                                selectedFrames.forEach {
                                                    it.lens = lens
                                                    model.submitFrame(it)
                                                }
                                            }
                                            dialog.dismiss()
                                        }
                                    }.create().show()
                                }
                                // Edit aperture
                                3 -> {
                                    val view = requireActivity().layoutInflater
                                            .inflate(R.layout.dialog_single_decimal_edit_text, null)
                                    val editText = view.findViewById<EditText>(R.id.edit_text)
                                    MaterialAlertDialogBuilder(requireContext())
                                            .setView(view)
                                            .setTitle(R.string.EnterCustomerApertureValue)
                                            .setPositiveButton(R.string.OK) { _, _ ->
                                                selectedFrames.forEach {
                                                    it.aperture = editText.text.toString()
                                                    model.submitFrame(it)
                                                }
                                            }
                                            .setNegativeButton(R.string.Cancel) { _, _ -> }
                                            .create()
                                            .show()
                                    editText.requestFocus()
                                }
                                // Edit shutter speed
                                4 -> {
                                    MaterialAlertDialogBuilder(requireContext()).apply {
                                        setNegativeButton(R.string.Cancel) { _, _ -> }
                                        val listItems =
                                                roll.camera?.shutterSpeedValues(requireContext())
                                                        ?: Camera.defaultShutterSpeedValues(requireContext())
                                        setItems(listItems) { dialog, which ->
                                            if (which == 0) {
                                                selectedFrames.forEach {
                                                    it.shutter = null
                                                    model.submitFrame(it)
                                                }
                                            } else {
                                                selectedFrames.forEach {
                                                    it.shutter = listItems[which]
                                                    model.submitFrame(it)
                                                }
                                            }
                                            dialog.dismiss()
                                        }
                                    }.create().show()
                                }
                                // Edit filters
                                5 -> {
                                    val filters = filterRepository.filters
                                    val listItems = filters.map(Filter::name).toTypedArray()
                                    val selections = BooleanArray(filters.size)
                                    MaterialAlertDialogBuilder(requireContext())
                                            .setMultiChoiceItems(listItems, selections) { _, which, isChecked ->
                                                selections[which] = isChecked
                                            }
                                            .setNegativeButton(R.string.Cancel) { _, _ -> }
                                            .setPositiveButton(R.string.OK) { _, _ ->
                                                val selectedFilters = selections.zip(filters)
                                                    .mapNotNull { (selected, filter) ->
                                                        if (selected) filter else null
                                                    }
                                                selectedFrames.forEach { frame ->
                                                    frame.filters = selectedFilters
                                                    model.submitFrame(frame)
                                                }
                                            }
                                            .create().show()
                                }
                                // Edit focal length
                                6 -> {
                                    val view = requireActivity().layoutInflater
                                            .inflate(R.layout.dialog_single_integer_edit_text, null)
                                    val editText = view.findViewById<EditText>(R.id.edit_text)
                                    MaterialAlertDialogBuilder(requireContext())
                                            .setView(view)
                                            .setTitle(R.string.EditFocalLength)
                                            .setPositiveButton(R.string.OK) { _, _ ->
                                                selectedFrames.forEach {
                                                    it.focalLength = editText.text.toString()
                                                            .toIntOrNull() ?: it.focalLength
                                                    model.submitFrame(it)
                                                }
                                            }
                                            .setNegativeButton(R.string.Cancel) { _, _ -> }
                                            .create()
                                            .show()
                                    editText.requestFocus()
                                }
                                // Edit exposure compensation
                                7 -> {
                                    MaterialAlertDialogBuilder(requireContext()).apply {
                                        setNegativeButton(R.string.Cancel) { _, _ -> }
                                        val listItems = roll.camera?.exposureCompValues(requireContext())
                                                ?: Camera.defaultExposureCompValues(requireContext())
                                        setItems(listItems) { dialog, which ->
                                            selectedFrames.forEach {
                                                it.exposureComp = listItems[which]
                                                model.submitFrame(it)
                                            }
                                            dialog.dismiss()
                                        }
                                    }.create().show()
                                }
                                // Edit location
                                8 -> {
                                    val action = FramesListFragmentDirections
                                        .framesListLocationPickAction(null, null, showToolbar = true)
                                    findNavController().navigate(action)
                                }
                                // Edit light source
                                9 -> {
                                    val lightSources = LightSource.entries.toTypedArray()
                                    val descriptions = lightSources
                                        .map { it.description(requireContext()) }
                                        .toTypedArray()
                                    MaterialAlertDialogBuilder(requireContext())
                                            .setNegativeButton(R.string.Cancel) { _, _ -> }
                                            .setItems(descriptions) { dialog, which ->
                                                selectedFrames.forEach {
                                                    it.lightSource = lightSources[which]
                                                    model.submitFrame(it)
                                                }
                                                dialog.dismiss()
                                            }
                                            .create().show()
                                }
                                // Reverse frame counts
                                10 -> {
                                    // Create a list of frame counts in reversed order
                                    val frameCountsReversed = selectedFrames
                                        .map(Frame::count)
                                        .reversed()
                                    selectedFrames.zip(frameCountsReversed) { frame, count ->
                                        frame.count = count
                                        model.submitFrame(frame)
                                    }
                                }
                                else -> { }
                            }
                        }
                        builder.setNegativeButton(R.string.Cancel) { dialogInterface, _ ->
                            // Do nothing
                            dialogInterface.dismiss()
                        }
                        builder.create().show()
                    }
                    true
                }
                R.id.menu_item_copy -> {
                    FrameCountBatchEditDialogBuilder(requireActivity(),
                        R.string.EditCopiedFramesCountsBy,
                        selectedFrames.size) { countChange ->
                        selectedFrames.forEach {
                            // Copy the frame and reset its id. This way the ViewModel
                            // thinks it's a new frame instead of an existing one.
                            val frame = it.copy().apply {
                                id = -1
                                count += countChange
                            }
                            model.submitFrame(frame)
                        }
                    }.create().show()
                    true
                }
                R.id.menu_item_select_all -> {
                    frameAdapter.toggleSelectionAll()
                    // Do not use local variable to get selected count because its size
                    // may no longer be valid after all items were selected.
                    mode.title = "${model.selectedFrames.size}/${frames.size}"
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            val fragment = parentFragmentManager.primaryNavigationFragment
            if (fragment is LocationPickFragment) {
                findNavController().navigateUp()
            }
            frameAdapter.clearSelections()
            actionMode = null
            // Make the floating action bar visible again since action mode is exited.
            binding.fab.show()
        }

        /**
         * Private class which creates a dialog builder for a custom dialog.
         * Used to batch edit frame counts.
         */
        private inner class FrameCountBatchEditDialogBuilder(
            context: Context,
            titleStringResourceId: Int,
            initialCountChange: Int,
            onCountChange: (Int) -> (Any)) : MaterialAlertDialogBuilder(context) {
            init {
                setTitle(titleStringResourceId)
                @SuppressLint("InflateParams")
                val binding = DialogSingleDropdownBinding.inflate(requireActivity().layoutInflater)
                val menu = binding.dropdownMenu.editText as MaterialAutoCompleteTextView
                val initialValue =  when (initialCountChange) {
                    in 1..100 -> "+$initialCountChange"
                    in -100..-1 -> initialCountChange.toString()
                    else -> "0"
                }
                menu.setText(initialValue, false)
                val displayedValues = (-100..100).map { if (it > 0) "+$it" else it.toString() }
                    .toTypedArray()
                menu.setSimpleItems(displayedValues)
                binding.dropdownMenu.setEndIconOnClickListener(null)
                menu.setOnClickListener {
                    val currentIndex = displayedValues.indexOf(menu.text.toString())
                    if (currentIndex >= 0) menu.listSelection = currentIndex
                }
                setView(binding.root)
                setNegativeButton(R.string.Cancel) { _, _ -> }
                setPositiveButton(R.string.OK) { _, _ ->
                    // Replace the plus sign because on pre L devices this seems to cause a crash
                    val countChange = menu.text.toString().replace("+", "").toInt()
                    onCountChange(countChange)
                }
            }
        }
    }

}