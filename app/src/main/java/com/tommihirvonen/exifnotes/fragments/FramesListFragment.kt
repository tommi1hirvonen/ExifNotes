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

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.doOnPreDraw
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.*
import com.tommihirvonen.exifnotes.adapters.FrameAdapter
import com.tommihirvonen.exifnotes.adapters.FrameAdapter.FrameAdapterListener
import com.tommihirvonen.exifnotes.databinding.DialogSingleDropdownBinding
import com.tommihirvonen.exifnotes.databinding.FragmentFramesListBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.FramesViewModel
import com.tommihirvonen.exifnotes.viewmodels.RollsViewModel
import java.io.IOException
import java.util.*

/**
 * FramesFragment is the fragment which is called when the user presses on a roll
 * on the ListView in RollsFragment. It displays all the frames from that roll.
 */
class FramesListFragment : LocationUpdatesFragment(), FrameAdapterListener {

    companion object {
        const val TAG = "FRAMES_LIST_FRAGMENT"
    }

    // The ViewModel has been instantiated using a factory by the parent fragment.
    private val model by viewModels<FramesViewModel>(ownerProducer = { requireParentFragment() })
    private val rollModel by activityViewModels<RollsViewModel>()

    private val roll get() = model.roll.value!!

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

    private val transitionInterpolator = FastOutSlowInInterpolator()
    private val transitionDuration = 250L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireParentFragment().requireParentFragment().childFragmentManager.popBackStack()
        }

        val rollEditFragment = requireParentFragment().childFragmentManager
            .findFragmentByTag(RollEditFragment.TAG)
        rollEditFragment
            ?.setFragmentResultListener(RollEditFragment.REQUEST_KEY, onRollEditListener)

        val frameEditFragment = requireParentFragment().childFragmentManager
            .findFragmentByTag(FrameEditFragment.TAG)
        frameEditFragment
            ?.setFragmentResultListener(FrameEditFragment.REQUEST_KEY, onFrameEditListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentFramesListBinding.inflate(inflater, container, false)
        binding.topAppBar.transitionName = "frames_top_app_bar_transition"
        binding.viewmodel = model
        binding.topAppBar.setNavigationOnClickListener {
            requireParentFragment().requireParentFragment().childFragmentManager.popBackStack()
        }
        binding.topAppBar.setOnMenuItemClickListener(onTopMenuItemClick)
        binding.bottomAppBar.setOnMenuItemClickListener(onBottomMenuItemSelected)
        binding.fab.setOnClickListener { showEditFrameFragment(null, binding.bottomAppBar) }

        val layoutManager = LinearLayoutManager(activity)
        binding.framesRecyclerView.layoutManager = layoutManager

        frameAdapter = FrameAdapter(requireActivity(), this, binding.framesRecyclerView)
        binding.framesRecyclerView.adapter = frameAdapter
        frameAdapter.onItemSelectedChanged = { frame, selected ->
            if (selected) model.selectedFrames.add(frame)
            else model.selectedFrames.remove(frame)
        }
        frameAdapter.onAllSelectionsChanged = { selected ->
            if (selected) model.selectedFrames.addAll(frames.filterNot(model.selectedFrames::contains))
            else model.selectedFrames.clear()
        }

        val bottomMenu = binding.bottomAppBar.menu
        model.frameSortMode.observe(viewLifecycleOwner) { mode ->
            when (mode) {
                FrameSortMode.FRAME_COUNT -> bottomMenu.findItem(R.id.frame_count_sort_mode).isChecked = true
                FrameSortMode.DATE -> bottomMenu.findItem(R.id.date_sort_mode).isChecked = true
                FrameSortMode.F_STOP -> bottomMenu.findItem(R.id.f_stop_sort_mode).isChecked = true
                FrameSortMode.SHUTTER_SPEED -> bottomMenu.findItem(R.id.shutter_speed_sort_mode).isChecked = true
                FrameSortMode.LENS -> bottomMenu.findItem(R.id.lens_sort_mode).isChecked = true
                null -> {}
            }
        }

        model.frames.observe(viewLifecycleOwner) { frames ->
            this.frames = frames
            frameAdapter.items = frames
            if (model.selectedFrames.isNotEmpty()) {
                frameAdapter.setSelections(model.selectedFrames)
                ensureActionMode()
            }
            binding.noAddedFrames.visibility = if (frames.isEmpty()) View.VISIBLE else View.GONE
            frameAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.container.alpha = 0f
        postponeEnterTransition()
        // Start the transition once all views have been measured and laid out.
        (view.parent as? ViewGroup)?.doOnPreDraw {
            startPostponedEnterTransition()
            ObjectAnimator.ofFloat(binding.container, View.ALPHA, 0f, 1f).apply {
                duration = transitionDuration
                start()
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
        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(transitionInterpolator)
            .apply { duration = transitionDuration }

        val fragment = FrameEditFragment().apply {
            sharedElementEnterTransition = sharedElementTransition
        }

        val arguments = Bundle()
        // New frame is being added.
        if (frame == null) {
            // If the frame count is greater than 100, then don't add a new frame.
            val nextFrameCount = frames.maxOfOrNull(Frame::count)?.plus(1) ?: 1
            if (nextFrameCount > 100) {
                binding.container.snackbar(R.string.TooManyFrames, binding.bottomAppBar)
                return
            }
            val newFrame = Frame(roll).apply {
                date = DateTime.fromCurrentTime()
                count = nextFrameCount
                noOfExposures = 1
                //Get the location only if the app has location permission (locationPermissionsGranted) and
                //the user has enabled GPS updates in the app's settings.
                if (locationPermissionsGranted && requestingLocationUpdates) {
                    lastLocation?.let { location = Location(it) }
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
            val title = requireActivity().resources.getString(R.string.AddNewFrame)
            val positiveButton = requireActivity().resources.getString(R.string.Add)
            arguments.putString(ExtraKeys.TITLE, title)
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton)
            arguments.putParcelable(ExtraKeys.FRAME, newFrame)
        }
        // Existing frame is being edited.
        else {
            val title = "" + requireActivity().getString(R.string.EditFrame) + frame.count
            val positiveButton = requireActivity().resources.getString(R.string.OK)
            arguments.putString(ExtraKeys.TITLE, title)
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton)
            arguments.putParcelable(ExtraKeys.FRAME, frame)
        }

        // Use the provided view as a primary shared element.
        // If no view was provided, use the floating action button.
        arguments.putSerializable(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
        arguments.putString(ExtraKeys.BACKSTACK_NAME, FramesFragment.BACKSTACK_NAME)
        arguments.putInt(ExtraKeys.FRAGMENT_CONTAINER_ID, R.id.frames_fragment_container)
        fragment.arguments = arguments
        requireParentFragment().childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(R.id.frames_fragment_container, fragment, FrameEditFragment.TAG)
            .addToBackStack(FramesFragment.BACKSTACK_NAME)
            .commit()

        fragment.setFragmentResultListener(FrameEditFragment.REQUEST_KEY, onFrameEditListener)
    }

    private val onFrameEditListener: (String, Bundle) -> Unit = { _, bundle ->
        actionMode?.finish()
        bundle.parcelable<Frame>(ExtraKeys.FRAME)?.let(model::submitFrame)
    }

    private val onRollEditListener: (String, Bundle) -> Unit = { _, bundle ->
        bundle.parcelable<Roll>(ExtraKeys.ROLL)?.let{
            rollModel.submitRoll(it)
            model.setRoll(it)
        }
    }

    private val onTopMenuItemClick = { item: MenuItem ->
        when (item.itemId) {
            R.id.menu_item_edit -> showRollEditFragment()
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
                val gearActivityIntent = Intent(activity, GearActivity::class.java)
                startActivity(gearActivityIntent)
            }
            R.id.menu_item_preferences -> {
                val preferenceActivityIntent = Intent(activity, PreferenceActivity::class.java)
                preferenceResultLauncher.launch(preferenceActivityIntent)
            }
            R.id.menu_item_show_on_map -> {
                val fragment = FramesMapFragment()
                requireParentFragment().childFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.enter_fragment, R.anim.exit_fragment, R.anim.enter_fragment, R.anim.exit_fragment)
                    .setReorderingAllowed(true)
                    .addToBackStack(FramesFragment.BACKSTACK_NAME)
                    .add(R.id.frames_fragment_container, fragment, FramesMapFragment.TAG)
                    .commit()
            }
            R.id.menu_item_share_intent ->
                // Getting member shareRollIntent may take a while to run since it
                // generates the files that will be shared.
                // -> Run the code on a new thread, which lets the UI thread to finish menu animations.
                Thread {
                    val shareIntent = RollShareIntentBuilder(requireActivity(), roll).create()
                    shareIntent?.let { startActivity(Intent.createChooser(it, resources.getString(R.string.Share))) }
                }.start()
            R.id.menu_item_export -> {
                val intent = Intent().apply { action = Intent.ACTION_OPEN_DOCUMENT_TREE }
                exportResultLauncher.launch(intent)
            }
        }
        true
    }

    private val preferenceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Finish this activity since the selected roll may not be valid anymore.
        if (result.resultCode and PreferenceActivity.RESULT_DATABASE_IMPORTED == PreferenceActivity.RESULT_DATABASE_IMPORTED) {
            requireActivity().recreate()
        }
    }

    private val exportResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val directoryUri = result.data?.data ?: return@registerForActivityResult
                val directoryDocumentFile = DocumentFile.fromTreeUri(requireContext(), directoryUri)
                    ?: return@registerForActivityResult
                RollExportHelper(requireActivity(), roll, directoryDocumentFile).export()
                binding.root.snackbar(R.string.ExportedFilesSuccessfully, binding.bottomAppBar)
            } catch (e: IOException) {
                e.printStackTrace()
                binding.root.snackbar(R.string.ErrorExporting, binding.bottomAppBar)
            }
        }
    }

    private fun showRollEditFragment() {
        val sharedElement = binding.topAppBar
        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(transitionInterpolator)
            .apply { duration = transitionDuration }
        val args = Bundle().apply {
            putParcelable(ExtraKeys.ROLL, roll)
            putString(ExtraKeys.TITLE, requireActivity().resources.getString(R.string.EditRoll))
            putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
            putString(ExtraKeys.BACKSTACK_NAME, FramesFragment.BACKSTACK_NAME)
            putInt(ExtraKeys.FRAGMENT_CONTAINER_ID, R.id.frames_fragment_container)
        }
        val fragment = RollEditFragment().apply {
            sharedElementEnterTransition = sharedElementTransition
            arguments = args
        }
        requireParentFragment().childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(R.id.frames_fragment_container, fragment, RollEditFragment.TAG)
            .addToBackStack(FramesFragment.BACKSTACK_NAME)
            .commit()
        fragment.setFragmentResultListener(RollEditFragment.REQUEST_KEY, onRollEditListener)
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
                        mode.finish()
                        // Get the first of the selected rolls (only one should be selected anyway)
                        showEditFrameFragment(selectedFrames.first(), binding.topAppBar)
                    } else {
                        val builder = MaterialAlertDialogBuilder(requireActivity())
                        builder.setTitle(String.format(resources.getString(R.string.BatchEditFramesTitle), selectedFrames.size))
                        builder.setItems(R.array.FramesBatchEditOptions) { _, i ->
                            when (i) {
                                // Edit frame counts
                                0 -> FrameCountBatchEditDialogBuilder(requireActivity()).create().show()
                                // Edit date and time
                                1 -> {
                                    val dt = DateTime.fromCurrentTime()
                                    val cal = Calendar.getInstance()
                                    cal.set(dt.year, dt.month - 1, dt.day)
                                    val datePicker = MaterialDatePicker.Builder.datePicker()
                                        .setSelection(cal.timeInMillis)
                                        .build()
                                    datePicker.addOnPositiveButtonClickListener {
                                        cal.timeInMillis = datePicker.selection ?: cal.timeInMillis
                                        val dateTime = DateTime(cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DATE],
                                            dt.hour, dt.minute)
                                        val timePicker = MaterialTimePicker.Builder()
                                            .setHour(dt.hour)
                                            .setMinute(dt.minute)
                                            .setTimeFormat(TimeFormat.CLOCK_24H)
                                            .build()
                                        timePicker.addOnPositiveButtonClickListener {
                                            dateTime.hour = timePicker.hour
                                            dateTime.minute = timePicker.minute
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
                                        val lenses = roll.camera?.let(database::getLinkedLenses)
                                            ?: database.lenses
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
                                    val filters = database.filters
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
                                    val intent = Intent(activity, LocationPickActivity::class.java)
                                    locationResultLauncher.launch(intent)
                                }
                                // Edit light source
                                9 -> {
                                    MaterialAlertDialogBuilder(requireContext())
                                            .setNegativeButton(R.string.Cancel) { _, _ -> }
                                            .setItems(R.array.LightSource) { dialog, which ->
                                                selectedFrames.forEach {
                                                    it.lightSource = which
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
                    selectedFrames.forEach {
                        // Copy the frame and reset its id. This way the ViewModel
                        // thinks it's a new frame instead of an existing one.
                        val frame = it.copy().apply { id = -1 }
                        model.submitFrame(frame)
                    }
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
            frameAdapter.clearSelections()
            actionMode = null
            // Make the floating action bar visible again since action mode is exited.
            binding.fab.show()
        }

        /**
         * Private class which creates a dialog builder for a custom dialog.
         * Used to batch edit frame counts.
         */
        private inner class FrameCountBatchEditDialogBuilder(context: Context)
            : MaterialAlertDialogBuilder(context) {
            init {
                setTitle(R.string.EditFrameCountsBy)
                @SuppressLint("InflateParams")
                val binding = DialogSingleDropdownBinding.inflate(requireActivity().layoutInflater)
                val menu = binding.dropdownMenu.editText as MaterialAutoCompleteTextView
                menu.setText("0", false)
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
                    val change = menu.text.toString().replace("+", "").toInt()
                    model.selectedFrames.forEach { frame ->
                        frame.count += change
                        model.submitFrame(frame)
                    }
                }
            }
        }
    }

    private val locationResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Consume the case when the user has edited
            // the location of several frames in action mode.
            if (result.resultCode == Activity.RESULT_OK) {
                val location: Location? =
                    if (result.data?.hasExtra(ExtraKeys.LOCATION) == true) {
                        result.data?.parcelable(ExtraKeys.LOCATION)
                    } else null
                val formattedAddress: String? =
                    if (result.data?.hasExtra(ExtraKeys.FORMATTED_ADDRESS) == true) {
                        result.data?.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
                    } else null
                model.selectedFrames.forEach { frame ->
                    frame.location = location
                    frame.formattedAddress = formattedAddress
                    model.submitFrame(frame)
                }
            }
        }
}