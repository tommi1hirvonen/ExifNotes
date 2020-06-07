package com.tommihirvonen.exifnotes.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.*
import com.tommihirvonen.exifnotes.adapters.FrameAdapter
import com.tommihirvonen.exifnotes.adapters.FrameAdapter.FrameAdapterListener
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.datastructures.FrameSortMode.Companion.fromValue
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.Utilities
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * FramesFragment is the fragment which is called when the user presses on a roll
 * on the ListView in RollsFragment. It displays all the frames from that roll.
 */
class FramesFragment : LocationUpdatesFragment(), View.OnClickListener, FrameAdapterListener {

    companion object {
        /**
         * Public constant to tag this fragment when it is created.
         */
        const val FRAMES_FRAGMENT_TAG = "FRAMES_FRAGMENT"

        /**
         * Constant passed to EditFrameDialog for result
         */
        private const val FRAME_DIALOG = 1

        /**
         * Constant passed to EditFrameDialog for result
         */
        private const val EDIT_FRAME_DIALOG = 2

        /**
         * Constant passed to ErrorDialogFragment
         */
        private const val SHOW_ON_MAP = 4

        /**
         * Constant passed to LocationPickActivity
         */
        private const val REQUEST_LOCATION_PICK = 5
        private const val REQUEST_EXPORT_FILES = 6
    }

    /**
     * Reference to the singleton database
     */
    private lateinit var database: FilmDbHelper

    /**
     * TextView to show that no frames have been added to this roll
     */
    private lateinit var mainTextView: TextView

    /**
     * ListView to show all the frames on this roll with details
     */
    private lateinit var mainRecyclerView: RecyclerView

    /**
     * Contains all the frames for this roll
     */
    private var frameList = mutableListOf<Frame>()

    /**
     * Adapter to adapt frameList to mainRecyclerView
     */
    private lateinit var frameAdapter: FrameAdapter

    /**
     * Currently selected roll
     */
    private lateinit var roll: Roll

    /**
     * Currently used camera
     */
    private var camera: Camera? = null

    /**
     * Reference to the FloatingActionButton
     */
    private lateinit var floatingActionButton: FloatingActionButton

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        database = FilmDbHelper.getInstance(activity)
        val rollId = requireArguments().getLong(ExtraKeys.ROLL_ID)
        roll = database.getRoll(rollId)!! // Roll must be defined for every Frame
        camera = database.getCamera(roll.cameraId)
        frameList = database.getAllFramesFromRoll(roll)

        //getActivity().getPreferences() returns a preferences file related to the
        //activity it is opened from. getDefaultSharedPreferences() returns the
        //applications global preferences. This is something to keep in mind.
        //If the same sort order setting is to be used elsewhere in the app, then
        //getDefaultSharedPreferences() should be used.
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        sortMode = fromValue(sharedPreferences.getInt(PreferenceConstants.KEY_FRAME_SORT_ORDER, FrameSortMode.FRAME_COUNT.value))

        //Sort the list according to preferences
        Utilities.sortFrameList(activity, sortMode, database, frameList)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val layoutInflater = requireActivity().layoutInflater
        val view = layoutInflater.inflate(R.layout.fragment_frames, container, false)
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar?.title = roll.name
        camera?.let { actionBar?.subtitle = it.name }
        actionBar?.setDisplayHomeAsUpEnabled(true)

        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener(this)
        val secondaryColor = Utilities.getSecondaryUiColor(activity)

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(secondaryColor)
        mainTextView = view.findViewById(R.id.no_added_frames)

        // Access the ListView
        mainRecyclerView = view.findViewById(R.id.frames_recycler_view)

        // Create an adapter for the RecyclerView
        frameAdapter = FrameAdapter(requireActivity(), frameList, this)
        val layoutManager = LinearLayoutManager(activity)
        mainRecyclerView.layoutManager = layoutManager
        // Add dividers for list items.
        mainRecyclerView.addItemDecoration(DividerItemDecoration(mainRecyclerView.context, layoutManager.orientation))

        // Set the RecyclerView to use frameAdapter
        mainRecyclerView.adapter = frameAdapter
        if (frameList.size > 0) {
            mainTextView.visibility = View.GONE
        }
        if (frameAdapter.itemCount > 0) mainRecyclerView.scrollToPosition(frameAdapter.itemCount - 1)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_frames_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (sortMode) {
            FrameSortMode.FRAME_COUNT -> menu.findItem(R.id.frame_count_sort_mode).isChecked = true
            FrameSortMode.DATE -> menu.findItem(R.id.date_sort_mode).isChecked = true
            FrameSortMode.F_STOP -> menu.findItem(R.id.f_stop_sort_mode).isChecked = true
            FrameSortMode.SHUTTER_SPEED -> menu.findItem(R.id.shutter_speed_sort_mode).isChecked = true
            FrameSortMode.LENS -> menu.findItem(R.id.lens_sort_mode).isChecked = true
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
                //Start the preference activity from FramesActivity.
                //The result will be handled in FramesActivity.
                requireActivity().startActivityForResult(preferenceActivityIntent, FramesActivity.PREFERENCE_ACTIVITY_REQUEST)
            }
            R.id.menu_item_help -> {
                val helpTitle = resources.getString(R.string.Help)
                val helpMessage = resources.getString(R.string.main_help)
                Utilities.showGeneralDialog(activity, helpTitle, helpMessage)
            }
            R.id.menu_item_about -> {
                val aboutTitle = resources.getString(R.string.app_name)
                val aboutMessage = "${resources.getString(R.string.about)}\n\n\n${resources.getString(R.string.VersionHistory)}"
                Utilities.showGeneralDialog(activity, aboutTitle, aboutMessage)
            }
            android.R.id.home -> requireActivity().finish()
            R.id.menu_item_show_on_map -> {
                val mapIntent = Intent(activity, MapActivity::class.java)
                val list = ArrayList<Roll>()
                list.add(roll)
                mapIntent.putParcelableArrayListExtra(ExtraKeys.ARRAY_LIST_ROLLS, list)
                mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_TITLE, roll.name)
                camera?.let { mapIntent.putExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE, it.name) }
                startActivityForResult(mapIntent, SHOW_ON_MAP)
            }
            R.id.menu_item_share ->
                // Method getShareRollIntent() may take a while to run since it
                // generates the files that will be shared.
                // -> Run the code on a new thread, which lets the UI thread to finish menu animations.
                Thread(Runnable {
                    val shareIntent = shareRollIntent
                    startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.Share)))
                }).start()
            R.id.menu_item_export -> {
                val intent = Intent()
                intent.action = Intent.ACTION_OPEN_DOCUMENT_TREE
                startActivityForResult(intent, REQUEST_EXPORT_FILES)
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val secondaryColor = Utilities.getSecondaryUiColor(activity)
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(secondaryColor)

        // If action mode is enabled, color the status bar dark grey.
        if (frameAdapter.selectedItemCount > 0 || actionMode != null) {
            Utilities.setStatusBarColor(activity, ContextCompat.getColor(requireActivity(), R.color.dark_grey))
        } else {
            // Otherwise we can update the frame adapter in case the user has changed the UI color.
            // This way the frame count text color will be updated according to the changed settings.
            frameAdapter.notifyDataSetChanged()
        }
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
        Utilities.sortFrameList(activity, sortMode, database, frameList)
        frameAdapter.notifyDataSetChanged()
    }

    /**
     * Creates an Intent to share exiftool commands and a csv
     * for the frames of the roll in question.
     *
     * @return The intent to be shared.
     */
    private val shareRollIntent: Intent get() {

            //Replace illegal characters from the roll name to make it a valid file name.
            val rollName = Utilities.replaceIllegalChars(roll.name)

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
            val csvString = Utilities.createCsvString(activity, roll)
            val exifToolCmds = Utilities.createExifToolCmdsString(activity, roll)

            //Create the files in external storage
            val fileCsv = File(externalStorageDir, fileNameCsv)
            val fileExifToolCmds = File(externalStorageDir, fileNameExifToolCmds)

            //Write the csv file
            Utilities.writeTextFile(fileCsv, csvString)

            //Write the ExifTool commands file
            Utilities.writeTextFile(fileExifToolCmds, exifToolCmds)

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
                    val uri: Uri
                    //Android Nougat requires that the file is given via FileProvider
                    uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            showFrameInfoDialog()
        }
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
        dialog.setTargetFragment(this, EDIT_FRAME_DIALOG)
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditFrameDialog.TAG)
    }

    /**
     * Called when the user presses the FloatingActionButton.
     * Show a dialog fragment to add a new frame.
     */
    private fun showFrameInfoDialog() {

        // If the frame count is greater than 100, then don't add a new frame.
        if (frameList.isNotEmpty()) {
            val countCheck = frameList[frameList.size - 1].count + 1
            if (countCheck > 100) {
                val toast = Toast.makeText(activity,
                        resources.getString(R.string.TooManyFrames), Toast.LENGTH_LONG)
                toast.show()
                return
            }
        }
        val title = requireActivity().resources.getString(R.string.NewFrame)
        val positiveButton = requireActivity().resources.getString(R.string.Add)
        val frame = Frame()
        frame.date = DateTime.fromCurrentTime()
        frame.count = 0
        frame.rollId = roll.id

        //Get the location only if the app has location permission (locationPermissionsGranted) and
        //the user has enabled GPS updates in the app's settings.
        if (locationPermissionsGranted && requestingLocationUpdates)
            lastLocation?.let { frame.location = Location(it) }
        if (frameList.isNotEmpty()) {
            //Get the information for the last added frame.
            //The last added frame has the highest id number (database autoincrement).
            val previousFrame = frameList.maxBy { it.id }
            // Here we can list the properties we want to bring from the previous frame
            previousFrame?.let {
                frame.count = previousFrame.count + 1
                frame.lensId = previousFrame.lensId
                frame.shutter = previousFrame.shutter
                frame.aperture = previousFrame.aperture
                frame.filters = previousFrame.filters
                frame.focalLength = previousFrame.focalLength
                frame.lightSource = previousFrame.lightSource
            }
        } else {
            frame.count = 1
            frame.shutter = null
            frame.aperture = null
        }
        frame.noOfExposures = 1
        val dialog = EditFrameDialog()
        dialog.setTargetFragment(this, FRAME_DIALOG)
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, title)
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton)
        arguments.putParcelable(ExtraKeys.FRAME, frame)
        dialog.arguments = arguments
        dialog.show(parentFragmentManager, EditFrameDialog.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FRAME_DIALOG -> if (resultCode == Activity.RESULT_OK) {
                val frame: Frame = data?.getParcelableExtra(ExtraKeys.FRAME) ?: return
                database.addFrame(frame)
                frameList.add(frame)
                Utilities.sortFrameList(activity, sortMode, database, frameList)
                frameAdapter.notifyItemInserted(frameList.indexOf(frame))
                mainTextView.visibility = View.GONE
                // When the new frame is added jump to view the added entry
                val pos = frameList.indexOf(frame)
                if (pos < frameAdapter.itemCount) mainRecyclerView.scrollToPosition(pos)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // After cancel do nothing
                return
            }
            EDIT_FRAME_DIALOG -> if (resultCode == Activity.RESULT_OK) {
                actionMode?.finish()
                val frame: Frame = data?.getParcelableExtra(ExtraKeys.FRAME) ?: return
                database.updateFrame(frame)
                val oldPosition = frameList.indexOf(frame)
                Utilities.sortFrameList(activity, sortMode, database, frameList)
                val newPosition = frameList.indexOf(frame)
                frameAdapter.notifyItemChanged(oldPosition)
                frameAdapter.notifyItemMoved(oldPosition, newPosition)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // After cancel do nothing
                return
            }
            SHOW_ON_MAP -> if (resultCode == Activity.RESULT_OK) {
                // Update the frame list in case updates were made in MapsActivity.
                frameList = database.getAllFramesFromRoll(roll)
                Utilities.sortFrameList(activity, sortMode, database, frameList)
                frameAdapter = FrameAdapter(requireActivity(), frameList, this)
                mainRecyclerView.adapter = frameAdapter
                frameAdapter.notifyDataSetChanged()
            }
            REQUEST_LOCATION_PICK ->
                // Consume the case when the user has edited
                // the location of several frames in action mode.
                if (resultCode == Activity.RESULT_OK) {
                    val location: Location? =
                            if (data?.hasExtra(ExtraKeys.LOCATION) == true) {
                                data.getParcelableExtra(ExtraKeys.LOCATION)
                            } else null
                    val formattedAddress: String? =
                            if (data?.hasExtra(ExtraKeys.FORMATTED_ADDRESS) == true) {
                                data.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
                            } else null
                    frameAdapter.selectedItemPositions.forEach { position ->
                        val frame = frameList[position]
                        frame.location = location
                        frame.formattedAddress = formattedAddress
                        database.updateFrame(frame)
                    }
                    // Exit action mode after edit,
                    // so that getSelectedItemPositions() isn't an empty list.
                    actionMode?.finish()
                }
            REQUEST_EXPORT_FILES -> if (resultCode == Activity.RESULT_OK) {
                try {
                    val rollName = Utilities.replaceIllegalChars(roll.name)
                    val directoryUri = data?.data ?: return
                    val directoryDocumentFile = DocumentFile.fromTreeUri(requireContext(), directoryUri) ?: return

                    //Get the user setting about which files to export. By default, share both files.
                    val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
                    val filesToExport = prefs.getString(PreferenceConstants.KEY_FILES_TO_EXPORT, PreferenceConstants.VALUE_BOTH)
                    if (filesToExport == PreferenceConstants.VALUE_BOTH || filesToExport == PreferenceConstants.VALUE_CSV) {
                        val csvDocumentFile = directoryDocumentFile.createFile("text/plain", rollName + "_csv.txt") ?: return
                        val csvOutputStream = requireActivity().contentResolver.openOutputStream(csvDocumentFile.uri) ?: return
                        val csvString = Utilities.createCsvString(activity, roll)
                        val csvOutputStreamWriter = OutputStreamWriter(csvOutputStream)
                        csvOutputStreamWriter.write(csvString)
                        csvOutputStreamWriter.flush()
                        csvOutputStreamWriter.close()
                        csvOutputStream.close()
                    }
                    if (filesToExport == PreferenceConstants.VALUE_BOTH || filesToExport == PreferenceConstants.VALUE_EXIFTOOL) {
                        val cmdDocumentFile = directoryDocumentFile.createFile("text/plain", rollName + "_ExifToolCmds.txt") ?: return
                        val cmdOutputStream = requireActivity().contentResolver.openOutputStream(cmdDocumentFile.uri) ?: return
                        val cmdString = Utilities.createExifToolCmdsString(activity, roll)
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
            // Set the status bar color to be dark grey to complement the grey action mode toolbar.
            Utilities.setStatusBarColor(activity, ContextCompat.getColor(requireActivity(), R.color.dark_grey))
            // Hide the floating action button so no new rolls can be added while in action mode.
            floatingActionButton.hide()
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
                    val deleteConfirmDialog = AlertDialog.Builder(activity)
                    // Separate confirm titles for one or multiple frames
                    val title = resources.getQuantityString(R.plurals.ConfirmFramesDelete, selectedItemPositions.size, selectedItemPositions.size)
                    deleteConfirmDialog.setTitle(title)
                    deleteConfirmDialog.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    deleteConfirmDialog.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        selectedItemPositions.sortedDescending().forEach { position ->
                            val frame = frameList[position]
                            database.deleteFrame(frame)
                            frameList.remove(frame)
                            frameAdapter.notifyItemRemoved(position)
                        }
                        if (frameList.size == 0) mainTextView.visibility = View.VISIBLE
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
                        val builder = AlertDialog.Builder(activity)
                        builder.setTitle(String.format(resources.getString(R.string.BatchEditFramesTitle), frameAdapter.selectedItemCount))
                        builder.setItems(R.array.FramesBatchEditOptions) { _: DialogInterface?, i: Int ->
                            when (i) {
                                // Edit frame counts
                                0 -> FrameCountBatchEditDialogBuilder(requireActivity()).create().show()
                                // Edit location
                                1 -> {
                                    val intent = Intent(activity, LocationPickActivity::class.java)
                                    startActivityForResult(intent, REQUEST_LOCATION_PICK)
                                }
                                // Reverse frame counts
                                2 -> {
                                    // Get the selected frames and sort them based on frame count
                                    val selectedFrames = frameAdapter.selectedItemPositions.map { frameList[it] }.sortedBy { it.count }
                                    // Create a list of frame counts in reversed order
                                    val frameCountsReversed = selectedFrames.map { it.count }.reversed()
                                    selectedFrames.forEachIndexed { index, frame ->
                                        frame.count = frameCountsReversed[index]
                                        database.updateFrame(frame)
                                    }
                                    if (sortMode == FrameSortMode.FRAME_COUNT) {
                                        Utilities.sortFrameList(requireContext(), sortMode, database, frameList)
                                    }
                                    frameAdapter.notifyDataSetChanged()
                                    actionMode?.finish()
                                }
                                else -> {
                                }
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
                R.id.menu_item_select_all -> {
                    frameAdapter.toggleSelectionAll()
                    mainRecyclerView.post { frameAdapter.resetAnimateAll() }
                    mode.title = frameAdapter.selectedItemCount.toString() + "/" + frameAdapter.itemCount
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            frameAdapter.clearSelections()
            actionMode = null
            mainRecyclerView.post { frameAdapter.resetAnimationIndex() }
            // Return the status bar to its original color before action mode.
            Utilities.setStatusBarColor(activity, Utilities.getSecondaryUiColor(activity))
            // Make the floating action bar visible again since action mode is exited.
            floatingActionButton.show()
        }

        /**
         * Private class which creates a dialog builder for a custom dialog.
         * Used to batch edit frame counts.
         */
        private inner class FrameCountBatchEditDialogBuilder internal constructor(context: Context) : AlertDialog.Builder(context) {
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
                    frameAdapter.selectedItemPositions.forEach { position ->
                        val frame = frameList[position]
                        frame.count += change
                        database.updateFrame(frame)
                    }
                    actionMode?.finish()
                    Utilities.sortFrameList(activity, sortMode, database, frameList)
                    frameAdapter.notifyDataSetChanged()
                }
            }
        }
    }

}