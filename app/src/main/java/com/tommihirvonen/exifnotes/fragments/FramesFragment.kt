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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.*
import com.tommihirvonen.exifnotes.adapters.FrameAdapter
import com.tommihirvonen.exifnotes.adapters.FrameAdapter.FrameAdapterListener
import com.tommihirvonen.exifnotes.databinding.FragmentFramesBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.datastructures.FrameSortMode.Companion.fromValue
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * FramesFragment is the fragment which is called when the user presses on a roll
 * on the ListView in RollsFragment. It displays all the frames from that roll.
 */
class FramesFragment : LocationUpdatesFragment(), FrameAdapterListener {
    
    private lateinit var binding: FragmentFramesBinding

    /**
     * Contains all the frames for this roll
     */
    private var frameList = mutableListOf<Frame>()

    /**
     * Adapter to adapt frameList to binding.framesRecyclerView
     */
    private lateinit var frameAdapter: FrameAdapter

    /**
     * Utility function to return a list of currently selected frames.
     */
    private val selectedFrames get() = frameAdapter.selectedItemPositions
            .map { frameList[it] }.sortedBy { it.count }

    /**
     * Currently selected roll
     */
    private lateinit var roll: Roll

    /**
     * Holds the frame sort mode.
     */
    private lateinit var sortMode: FrameSortMode

    /**
     * Private callback class which is given as an argument when the SupportActionMode is started.
     */
    private val actionModeCallback = ActionModeCallback()

    /**
     * Reference to the (Support)ActionMode, which is launched when a list item is long pressed.
     */
    private var actionMode: ActionMode? = null

    private val preferenceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Finish this activity since the selected roll may not be valid anymore.
        if (result.resultCode and PreferenceActivity.RESULT_DATABASE_IMPORTED == PreferenceActivity.RESULT_DATABASE_IMPORTED) {
            requireActivity().setResult(result.resultCode)
            requireActivity().finish()
        }
        // If the app theme was changed, recreate activity for changes to take effect.
        if (result.resultCode and PreferenceActivity.RESULT_THEME_CHANGED == PreferenceActivity.RESULT_THEME_CHANGED) {
            requireActivity().recreate()
        }
    }

    private val mapResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Update the frame list in case updates were made in MapsActivity.
            frameList = database.getFrames(roll).toMutableList()
            Frame.sortFrameList(requireActivity(), sortMode, frameList)
            frameAdapter = FrameAdapter(requireActivity(), frameList, this)
            binding.framesRecyclerView.adapter = frameAdapter
            frameAdapter.notifyDataSetChanged()
        }
    }

    private val locationResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Consume the case when the user has edited
        // the location of several frames in action mode.
        if (result.resultCode == Activity.RESULT_OK) {
            val location: Location? =
                if (result.data?.hasExtra(ExtraKeys.LOCATION) == true) {
                    result.data?.getParcelableExtra(ExtraKeys.LOCATION)
                } else null
            val formattedAddress: String? =
                if (result.data?.hasExtra(ExtraKeys.FORMATTED_ADDRESS) == true) {
                    result.data?.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
                } else null
            selectedFrames.forEach { frame ->
                frame.location = location
                frame.formattedAddress = formattedAddress
                database.updateFrame(frame)
            }
        }
    }

    private val exportResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val rollName = roll.name?.illegalCharsRemoved()
                val directoryUri = result.data?.data ?: return@registerForActivityResult
                val directoryDocumentFile = DocumentFile.fromTreeUri(requireContext(), directoryUri)
                    ?: return@registerForActivityResult

                //Get the user setting about which files to export. By default, share both files.
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
                val filesToExport = prefs.getString(
                    PreferenceConstants.KEY_FILES_TO_EXPORT,
                    PreferenceConstants.VALUE_BOTH)
                if (filesToExport == PreferenceConstants.VALUE_BOTH
                    || filesToExport == PreferenceConstants.VALUE_CSV) {
                    val csvDocumentFile = directoryDocumentFile.createFile("text/plain",
                        rollName + "_csv.txt") ?: return@registerForActivityResult
                    val csvOutputStream = requireActivity().contentResolver
                        .openOutputStream(csvDocumentFile.uri) ?: return@registerForActivityResult
                    val csvString = CsvBuilder(requireActivity(), roll).create()
                    val csvOutputStreamWriter = OutputStreamWriter(csvOutputStream)
                    csvOutputStreamWriter.write(csvString)
                    csvOutputStreamWriter.flush()
                    csvOutputStreamWriter.close()
                    csvOutputStream.close()
                }
                if (filesToExport == PreferenceConstants.VALUE_BOTH
                    || filesToExport == PreferenceConstants.VALUE_EXIFTOOL) {
                    val cmdDocumentFile = directoryDocumentFile.createFile("text/plain",
                        rollName + "_ExifToolCmds.txt") ?: return@registerForActivityResult
                    val cmdOutputStream = requireActivity().contentResolver
                        .openOutputStream(cmdDocumentFile.uri) ?: return@registerForActivityResult
                    val cmdString = ExifToolCommandsBuilder(requireActivity(), roll).create()
                    val cmdOutputStreamWriter = OutputStreamWriter(cmdOutputStream)
                    cmdOutputStreamWriter.write(cmdString)
                    cmdOutputStreamWriter.flush()
                    cmdOutputStreamWriter.close()
                    cmdOutputStream.close()
                }
                Toast.makeText(activity, R.string.ExportedFilesSuccessfully, Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(activity, R.string.ErrorExporting, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roll = requireArguments().getParcelable(ExtraKeys.ROLL)!! // Roll must be defined for every Frame
        frameList = database.getFrames(roll).toMutableList()

        //getActivity().getPreferences() returns a preferences file related to the
        //activity it is opened from. getDefaultSharedPreferences() returns the
        //applications global preferences. This is something to keep in mind.
        //If the same sort order setting is to be used elsewhere in the app, then
        //getDefaultSharedPreferences() should be used.
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        sortMode = fromValue(sharedPreferences.getInt(
            PreferenceConstants.KEY_FRAME_SORT_ORDER,
                FrameSortMode.FRAME_COUNT.value))

        //Sort the list according to preferences
        Frame.sortFrameList(requireActivity(), sortMode, frameList)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentFramesBinding.inflate(inflater, container, false)
        binding.root.transitionName = "transition_target"
        binding.topAppBar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        binding.topAppBar.title = roll.name
        roll.camera?.let { binding.topAppBar.subtitle = it.name }
        val menu = binding.topAppBar.menu
        when (sortMode) {
            FrameSortMode.FRAME_COUNT -> menu.findItem(R.id.frame_count_sort_mode).isChecked = true
            FrameSortMode.DATE -> menu.findItem(R.id.date_sort_mode).isChecked = true
            FrameSortMode.F_STOP -> menu.findItem(R.id.f_stop_sort_mode).isChecked = true
            FrameSortMode.SHUTTER_SPEED -> menu.findItem(R.id.shutter_speed_sort_mode).isChecked = true
            FrameSortMode.LENS -> menu.findItem(R.id.lens_sort_mode).isChecked = true
        }
        binding.topAppBar.setOnMenuItemClickListener(onMenuItemSelected)

        binding.fab.setOnClickListener { showFrameInfoDialog() }

        frameAdapter = FrameAdapter(requireActivity(), frameList, this)
        val layoutManager = LinearLayoutManager(activity)
        binding.framesRecyclerView.layoutManager = layoutManager
        // Add dividers for list items.
        binding.framesRecyclerView.addItemDecoration(DividerItemDecoration(binding.framesRecyclerView.context,
                layoutManager.orientation))

        // Set the RecyclerView to use frameAdapter
        binding.framesRecyclerView.adapter = frameAdapter
        if (frameList.size > 0) {
            binding.noAddedFrames.visibility = View.GONE
        }

        binding.framesRecyclerView.addOnScrollListener(OnScrollExtendedFabListener(binding.fab))

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
                duration = 500
                start()
            }
        }
    }

    private val onMenuItemSelected = { item: MenuItem ->
        when (item.itemId) {
            R.id.frame_count_sort_mode -> {
                item.isChecked = true
                setSortMode(FrameSortMode.FRAME_COUNT)
            }
            R.id.date_sort_mode -> {
                item.isChecked = true
                setSortMode(FrameSortMode.DATE)
            }
            R.id.f_stop_sort_mode -> {
                item.isChecked = true
                setSortMode(FrameSortMode.F_STOP)
            }
            R.id.shutter_speed_sort_mode -> {
                item.isChecked = true
                setSortMode(FrameSortMode.SHUTTER_SPEED)
            }
            R.id.lens_sort_mode -> {
                item.isChecked = true
                setSortMode(FrameSortMode.LENS)
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
                val mapIntent = Intent(activity, MapActivity::class.java)
                val list = ArrayList<Roll>()
                list.add(roll)
                mapIntent.putParcelableArrayListExtra(ExtraKeys.ARRAY_LIST_ROLLS, list)
                mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_TITLE, roll.name)
                roll.camera?.let { mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE, it.name) }
                mapResultLauncher.launch(mapIntent)
            }
            R.id.menu_item_share ->
                // Getting member shareRollIntent may take a while to run since it
                // generates the files that will be shared.
                // -> Run the code on a new thread, which lets the UI thread to finish menu animations.
                Thread {
                    val shareIntent = shareRollIntent
                    shareIntent?.let { startActivity(Intent.createChooser(it, resources.getString(R.string.Share))) }
                }.start()
            R.id.menu_item_export -> {
                val intent = Intent()
                intent.action = Intent.ACTION_OPEN_DOCUMENT_TREE
                exportResultLauncher.launch(intent)
            }
        }
        true
    }

    /**
     * Change the sort order of frames.
     *
     * @param sortMode enum type referencing the sort mode
     */
    private fun setSortMode(sortMode: FrameSortMode) {
        this.sortMode = sortMode
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(PreferenceConstants.KEY_FRAME_SORT_ORDER, sortMode.value)
        editor.apply()
        Frame.sortFrameList(requireActivity(), sortMode, frameList)
        frameAdapter.notifyDataSetChanged()
    }

    /**
     * Creates an Intent to share exiftool commands and a csv
     * for the frames of the roll in question.
     *
     * @return The intent to be shared.
     */
    private val shareRollIntent: Intent? get() {

        //Replace illegal characters from the roll name to make it a valid file name.
        val rollName = roll.name?.illegalCharsRemoved()

        //Get the user setting about which files to export. By default, share only ExifTool.
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        val filesToExport = prefs.getString(PreferenceConstants.KEY_FILES_TO_EXPORT, PreferenceConstants.VALUE_BOTH)

        //Create the Intent to be shared, no initialization yet
        val shareIntent: Intent

        //Create the files

        //Get the external storage path (not the same as SD card)
        val externalStorageDir = requireActivity().getExternalFilesDir(null)

        //Create the file names for the two files to be put in that intent
        val fileNameCsv = rollName + "_csv" + ".txt"
        val fileNameExifToolCmds = rollName + "_ExifToolCmds" + ".txt"

        //Create the strings to be written on those two files
        val csvString = CsvBuilder(requireActivity(), roll).create()
        val exifToolCmds = ExifToolCommandsBuilder(requireActivity(), roll).create()

        //Create the files in external storage
        val fileCsv = File(externalStorageDir, fileNameCsv)
        val fileExifToolCmds = File(externalStorageDir, fileNameExifToolCmds)

        try {
            //Write the csv file
            fileCsv.writeText(csvString)

            //Write the ExifTool commands file
            fileExifToolCmds.writeText(exifToolCmds)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Error creating text files", Toast.LENGTH_SHORT).show()
            return null
        }

        //If the user has chosen to export both files
        if (filesToExport == PreferenceConstants.VALUE_BOTH) {
            //Create the intent to be shared
            shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            shareIntent.type = "text/plain"

            //Create an array with the file names
            val filesToSend: MutableList<String> = ArrayList()
            filesToSend.add(externalStorageDir.toString() + "/" + fileNameCsv)
            filesToSend.add(externalStorageDir.toString() + "/" + fileNameExifToolCmds)

            //Create an ArrayList of files.
            //NOTE: putParcelableArrayListExtra requires an ArrayList as its argument
            val files = ArrayList<Uri>()
            for (path in filesToSend) {
                val file = File(path)
                //Android Nougat requires that the file is given via FileProvider
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(requireContext(), requireContext().applicationContext
                            .packageName + ".provider", file)
                } else {
                    Uri.fromFile(file)
                }
                files.add(uri)
            }

            //Add the two files to the Intent as extras
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        } else {
            shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            //The user has chosen to export only the csv
            if (filesToExport == PreferenceConstants.VALUE_CSV) {
                //Android Nougat requires that the file is given via FileProvider
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(requireContext(), requireContext().applicationContext
                            .packageName + ".provider", fileCsv)
                } else {
                    Uri.fromFile(fileCsv)
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            } else if (filesToExport == PreferenceConstants.VALUE_EXIFTOOL) {
                //Android Nougat requires that the file is given via FileProvider
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(requireContext(), requireContext().applicationContext
                            .packageName + ".provider", fileExifToolCmds)
                } else {
                    Uri.fromFile(fileExifToolCmds)
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        return shareIntent
    }

    /**
     * Create new dialog to edit the selected Frame's information
     *
     * @param position position of the Frame in frameList
     */
    @SuppressLint("CommitTransaction")
    private fun showFrameInfoEditDialog(position: Int) {
        // Edit frame info
        val frame = frameList[position]
        val arguments = Bundle()
        val title = "" + requireActivity().getString(R.string.EditFrame) + frame.count
        val positiveButton = requireActivity().resources.getString(R.string.OK)
        arguments.putString(ExtraKeys.TITLE, title)
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton)
        arguments.putParcelable(ExtraKeys.FRAME, frame)
        val dialog = EditFrameDialog()
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditFrameDialog.TAG)
        dialog.setFragmentResultListener("EditFrameDialog") { _, bundle ->
            actionMode?.finish()
            val frame1: Frame = bundle.getParcelable(ExtraKeys.FRAME)
                ?: return@setFragmentResultListener
            database.updateFrame(frame1)
            val oldPosition = frameList.indexOf(frame1)
            Frame.sortFrameList(requireActivity(), sortMode, frameList)
            val newPosition = frameList.indexOf(frame1)
            frameAdapter.notifyItemChanged(oldPosition)
            frameAdapter.notifyItemMoved(oldPosition, newPosition)
        }
    }

    /**
     * Called when the user presses the binding.fab.
     * Show a dialog fragment to add a new frame.
     */
    private fun showFrameInfoDialog() {

        // If the frame count is greater than 100, then don't add a new frame.
        val nextFrameCount = frameList.maxByOrNull { it.count }?.count?.plus(1) ?: 1
        if (nextFrameCount > 100) {
            Toast.makeText(activity, resources.getString(R.string.TooManyFrames),
                    Toast.LENGTH_LONG).show()
            return
        }
        val title = requireActivity().resources.getString(R.string.AddNewFrame)
        val positiveButton = requireActivity().resources.getString(R.string.Add)
        val frame = Frame(roll)
        frame.date = DateTime.fromCurrentTime()
        frame.count = nextFrameCount
        frame.noOfExposures = 1

        //Get the location only if the app has location permission (locationPermissionsGranted) and
        //the user has enabled GPS updates in the app's settings.
        if (locationPermissionsGranted && requestingLocationUpdates)
            lastLocation?.let { frame.location = Location(it) }
        if (frameList.isNotEmpty()) {
            //Get the information for the last added frame.
            //The last added frame has the highest id number (database autoincrement).
            val previousFrame = frameList.maxByOrNull { it.id }
            // Here we can list the properties we want to bring from the previous frame
            previousFrame?.let {
                frame.lens = previousFrame.lens
                frame.shutter = previousFrame.shutter
                frame.aperture = previousFrame.aperture
                frame.filters = previousFrame.filters
                frame.focalLength = previousFrame.focalLength
                frame.lightSource = previousFrame.lightSource
            }
        }

        val dialog = EditFrameDialog()
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, title)
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton)
        arguments.putParcelable(ExtraKeys.FRAME, frame)
        dialog.arguments = arguments
        dialog.show(parentFragmentManager, EditFrameDialog.TAG)
        dialog.setFragmentResultListener("EditFrameDialog") { _, bundle ->
            val frame1: Frame = bundle.getParcelable(ExtraKeys.FRAME)
                ?: return@setFragmentResultListener
            database.addFrame(frame1)
            frameList.add(frame1)
            Frame.sortFrameList(requireActivity(), sortMode, frameList)
            frameAdapter.notifyItemInserted(frameList.indexOf(frame1))
            binding.noAddedFrames.visibility = View.GONE
            // When the new frame is added jump to view the added entry
            val pos = frameList.indexOf(frame1)
            if (pos < frameAdapter.itemCount) binding.framesRecyclerView.scrollToPosition(pos)
        }
    }

    override fun onItemClick(position: Int) {
        if (frameAdapter.selectedItemCount > 0 || actionMode != null) {
            enableActionMode(position)
        } else {
            showFrameInfoEditDialog(position)
        }
    }

    override fun onItemLongClick(position: Int) {
        enableActionMode(position)
    }

    /**
     * Enable action mode if not yet enabled and add item to selected items.
     *
     * @param position position of the item in FrameAdapter
     */
    private fun enableActionMode(position: Int) {
        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        frameAdapter.toggleSelection(position)
        // If the user deselected the last of the selected items, exit action mode.
        if (frameAdapter.selectedItemCount == 0) actionMode?.finish()
        else actionMode?.title = (frameAdapter.selectedItemCount.toString() + "/" + frameAdapter.itemCount)
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
            val selectedItemPositions = frameAdapter.selectedItemPositions
            return when (item.itemId) {
                R.id.menu_item_delete -> {
                    val deleteConfirmDialog = MaterialAlertDialogBuilder(requireActivity())
                    // Separate confirm titles for one or multiple frames
                    val title = resources.getQuantityString(R.plurals.ConfirmFramesDelete, selectedItemPositions.size, selectedItemPositions.size)
                    deleteConfirmDialog.setTitle(title)
                    deleteConfirmDialog.setNegativeButton(R.string.Cancel) { _, _ -> }
                    deleteConfirmDialog.setPositiveButton(R.string.OK) { _, _ ->
                        selectedItemPositions.sortedDescending().forEach { position ->
                            val frame = frameList[position]
                            database.deleteFrame(frame)
                            frameList.remove(frame)
                            frameAdapter.notifyItemRemoved(position)
                        }
                        if (frameList.size == 0) binding.noAddedFrames.visibility = View.VISIBLE
                        mode.finish()
                    }
                    deleteConfirmDialog.create().show()
                    true
                }
                R.id.menu_item_edit -> {

                    // If only one frame is selected, show frame edit dialog.
                    if (frameAdapter.selectedItemCount == 1) {
                        // Get the first of the selected rolls (only one should be selected anyway)
                        showFrameInfoEditDialog(selectedItemPositions[0])
                    } else {
                        val builder = MaterialAlertDialogBuilder(requireActivity())
                        builder.setTitle(String.format(resources.getString(R.string.BatchEditFramesTitle), frameAdapter.selectedItemCount))
                        builder.setItems(R.array.FramesBatchEditOptions) { _, i ->
                            when (i) {
                                // Edit frame counts
                                0 -> FrameCountBatchEditDialogBuilder(requireActivity()).create().show()
                                // Edit date and time
                                1 -> {
                                    val dateTimeTemp = DateTime.fromCurrentTime()
                                    // Show date dialog.
                                    val dateDialog = DatePickerDialog(requireActivity(),
                                            { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                                        // If date was set, show time dialog.
                                        val dateTime = DateTime(year, month + 1, dayOfMonth,
                                                dateTimeTemp.hour, dateTimeTemp.minute)
                                        val timeDialog = TimePickerDialog(requireActivity(),
                                                { _: TimePicker?, hourOfDay: Int, minute: Int ->
                                            // If time was set, update selected frames.
                                            dateTime.hour = hourOfDay
                                            dateTime.minute = minute
                                            selectedFrames.forEach {
                                                it.date = dateTime
                                                database.updateFrame(it)
                                            }
                                            frameAdapter.notifyDataSetChanged()
                                        }, dateTimeTemp.hour, dateTimeTemp.minute, true)
                                        timeDialog.show()

                                    }, dateTimeTemp.year, dateTimeTemp.month - 1, dateTimeTemp.day)
                                    dateDialog.show()
                                }
                                // Edit lens
                                2 -> {
                                    MaterialAlertDialogBuilder(requireContext()).apply {
                                        setNegativeButton(R.string.Cancel) { _, _ -> }
                                        val lenses = roll.camera?.let { database.getLinkedLenses(it) }
                                                ?: database.lenses
                                        val listItems = listOf(resources.getString(R.string.NoLens))
                                                .plus(lenses.map { it.name })
                                                .toTypedArray()
                                        setItems(listItems) { dialog, which ->
                                            if (which == 0) {
                                                // No lens was selected
                                                selectedFrames.forEach {
                                                    it.lens = null
                                                    database.updateFrame(it)
                                                }
                                            } else {
                                                val lens = lenses[which - 1]
                                                selectedFrames.forEach {
                                                    it.lens = lens
                                                    database.updateFrame(it)
                                                }
                                            }
                                            frameAdapter.notifyDataSetChanged()
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
                                                    database.updateFrame(it)
                                                }
                                                frameAdapter.notifyDataSetChanged()
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
                                                    database.updateFrame(it)
                                                }
                                            } else {
                                                selectedFrames.forEach {
                                                    it.shutter = listItems[which]
                                                    database.updateFrame(it)
                                                }
                                            }
                                            dialog.dismiss()
                                            frameAdapter.notifyDataSetChanged()
                                        }
                                    }.create().show()
                                }
                                // Edit filters
                                5 -> {
                                    val filters = database.filters.map { it to false }.toMutableList()
                                    val listItems = filters.map { it.first.name }.toTypedArray()
                                    MaterialAlertDialogBuilder(requireContext())
                                            .setMultiChoiceItems(listItems, BooleanArray(listItems.size)) { _, which, isChecked ->
                                                filters[which] = filters[which].first to isChecked
                                            }
                                            .setNegativeButton(R.string.Cancel) { _, _ -> }
                                            .setPositiveButton(R.string.OK) { _, _ ->
                                                selectedFrames.forEach { frame ->
                                                    frame.filters = filters.filter { it.second }
                                                            .map { it.first }.toMutableList()
                                                    database.updateFrame(frame)
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
                                                    database.updateFrame(it)
                                                }
                                                frameAdapter.notifyDataSetChanged()
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
                                                database.updateFrame(it)
                                            }
                                            dialog.dismiss()
                                            frameAdapter.notifyDataSetChanged()
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
                                                    database.updateFrame(it)
                                                }
                                                dialog.dismiss()
                                                frameAdapter.notifyDataSetChanged()
                                            }
                                            .create().show()
                                }
                                // Reverse frame counts
                                10 -> {
                                    // Create a list of frame counts in reversed order
                                    val frameCountsReversed = selectedFrames.map { it.count }.reversed()
                                    selectedFrames.forEachIndexed { index, frame ->
                                        frame.count = frameCountsReversed[index]
                                        database.updateFrame(frame)
                                    }
                                    if (sortMode == FrameSortMode.FRAME_COUNT) {
                                        Frame.sortFrameList(requireActivity(), sortMode, frameList)
                                    }
                                    frameAdapter.notifyDataSetChanged()
                                    actionMode?.finish()
                                }
                                else -> { }
                            }
                        }
                        builder.setNegativeButton(R.string.Cancel) { dialogInterface: DialogInterface, _: Int ->
                            // Do nothing
                            dialogInterface.dismiss()
                        }
                        builder.create().show()
                    }
                    true
                }
                R.id.menu_item_copy -> {
                    // Capture the id values for newly added frames.
                    val newIds = selectedFrames.map {
                        val frame = it.copy()
                        database.addFrame(frame)
                        frameList.add(frame)
                        frame.id
                    }
                    Frame.sortFrameList(requireActivity(), sortMode, frameList)
                    // Capture the positions in the sorted this of the newly added frames.
                    val positions = frameList.mapIndexed { index, frame -> frame.id to index }
                            .filter { newIds.contains(it.first) }.map { it.second }
                    frameAdapter.clearSelections()
                    frameAdapter.notifyDataSetChanged()
                    // Select the newly added frames based on their positions.
                    positions.forEach { frameAdapter.toggleSelection(it) }
                    true
                }
                R.id.menu_item_select_all -> {
                    frameAdapter.toggleSelectionAll()
                    binding.framesRecyclerView.post { frameAdapter.resetAnimateAll() }
                    mode.title = frameAdapter.selectedItemCount.toString() + "/" + frameAdapter.itemCount
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            frameAdapter.clearSelections()
            actionMode = null
            binding.framesRecyclerView.post { frameAdapter.resetAnimationIndex() }
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
                val view = requireActivity().layoutInflater.inflate(R.layout.dialog_single_numberpicker, null)
                val numberPicker = view.findViewById<NumberPicker>(R.id.number_picker)
                numberPicker.maxValue = 200
                numberPicker.minValue = 0
                // Use the NumberPicker.setDisplayedValues() method to set custom
                // values ranging from -100 to +100.
                val displayedValues = (-100..100).map { if (it > 0) "+$it" else it.toString() }
                numberPicker.displayedValues = displayedValues.toTypedArray()
                numberPicker.value = 100
                // Block the NumberPicker from activating the cursor.
                numberPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                setView(view)
                setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                    // Replace the plus sign because on pre L devices this seems to cause a crash
                    val change = displayedValues[numberPicker.value].replace("+", "").toInt()
                    selectedFrames.forEach { frame ->
                        frame.count += change
                        database.updateFrame(frame)
                    }
                    val selectedIds = selectedFrames.map { it.id }
                    Frame.sortFrameList(requireActivity(), sortMode, frameList)
                    // Capture the positions in the sorted this of the newly added frames.
                    val positions = frameList.mapIndexed { index, frame -> frame.id to index }
                            .filter { selectedIds.contains(it.first) }.map { it.second }
                    frameAdapter.clearSelections()
                    frameAdapter.notifyDataSetChanged()
                    // Select the newly added frames based on their positions.
                    positions.forEach { frameAdapter.toggleSelection(it) }
                }
            }
        }
    }

}