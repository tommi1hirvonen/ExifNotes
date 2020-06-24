package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.utilities.Utilities.ScrollIndicatorNestedScrollViewListener

/**
 * Dialog to edit Roll's information
 */
class EditRollDialog : DialogFragment() {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val TAG = "EditRollDialog"
        private const val REQUEST_CODE_ADD_CAMERA = 1
        private const val REQUEST_CODE_SELECT_FILM_STOCK = 2
    }

    /**
     * Holds all the cameras in the database
     */
    private lateinit var cameraList: MutableList<Camera>

    /**
     * The Button showing the currently selected camera
     */
    private lateinit var cameraTextView: TextView
    private lateinit var filmStockTextView: TextView
    private lateinit var isoTextView: TextView
    private lateinit var filmStockClearImageView: ImageView
    private lateinit var nameEditText: EditText

    //These variables are used so that the object itself is not updated
    //unless the user presses ok.

    private lateinit var newRoll: Roll

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {
        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButton = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        val roll = requireArguments().getParcelable(ExtraKeys.ROLL) ?: Roll()
        newRoll = roll.copy()
        cameraList = database.allCameras.toMutableList()
        val layoutInflater = requireActivity().layoutInflater
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams")
        val inflatedView = layoutInflater.inflate(R.layout.dialog_roll, null)
        val alert = AlertDialog.Builder(activity)
        val nestedScrollView: NestedScrollView = inflatedView.findViewById(R.id.nested_scroll_view)
        nestedScrollView.setOnScrollChangeListener(
                ScrollIndicatorNestedScrollViewListener(
                        requireActivity(),
                        nestedScrollView,
                        inflatedView.findViewById(R.id.scrollIndicatorUp),
                        inflatedView.findViewById(R.id.scrollIndicatorDown)))
        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(requireActivity(), title))
        alert.setView(inflatedView)

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(activity)) {
            listOf<View>(
                    inflatedView.findViewById(R.id.divider_view1),
                    inflatedView.findViewById(R.id.divider_view2),
                    inflatedView.findViewById(R.id.divider_view3),
                    inflatedView.findViewById(R.id.divider_view4),
                    inflatedView.findViewById(R.id.divider_view5),
                    inflatedView.findViewById(R.id.divider_view6),
                    inflatedView.findViewById(R.id.divider_view7),
                    inflatedView.findViewById(R.id.divider_view8),
                    inflatedView.findViewById(R.id.divider_view9),
                    inflatedView.findViewById(R.id.divider_view10)
            ).forEach { it.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white)) }

            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.add_camera)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.clear_film_stock)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.clear_date_unloaded)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.clear_date_developed)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
        }
        //==========================================================================================

        // NAME EDIT TEXT
        nameEditText = inflatedView.findViewById(R.id.name_editText)
        nameEditText.setText(roll.name)
        // Place the cursor at the end of the input field
        nameEditText.setSelection(nameEditText.text.length)
        nameEditText.isSingleLine = false


        //==========================================================================================
        // NOTE EDIT TEXT
        val noteEditText = inflatedView.findViewById<EditText>(R.id.note_editText)
        noteEditText.isSingleLine = false
        noteEditText.setText(roll.note)
        noteEditText.setSelection(noteEditText.text.length)
        //==========================================================================================


        //==========================================================================================
        // FILM STOCK PICK DIALOG
        filmStockTextView = inflatedView.findViewById(R.id.film_stock_text)
        filmStockClearImageView = inflatedView.findViewById(R.id.clear_film_stock)
        roll.filmStock?.let {
            filmStockTextView.text = it.name
            nameEditText.hint = it.name
        } ?: run {
            filmStockTextView.text = ""
            filmStockClearImageView.visibility = View.GONE
        }
        filmStockClearImageView.setOnClickListener {
            newRoll.filmStock = null
            nameEditText.hint = ""
            filmStockTextView.text = ""
            filmStockClearImageView.visibility = View.GONE
        }
        val filmStockLayout = inflatedView.findViewById<LinearLayout>(R.id.film_stock_layout)
        filmStockLayout.setOnClickListener {
            val dialog = SelectFilmStockDialog()
            dialog.setTargetFragment(this@EditRollDialog, REQUEST_CODE_SELECT_FILM_STOCK)
            dialog.show(parentFragmentManager.beginTransaction(), null)
        }
        //==========================================================================================


        //==========================================================================================
        // CAMERA PICK DIALOG
        cameraTextView = inflatedView.findViewById(R.id.camera_text)
        cameraTextView.text = roll.camera?.name ?: ""

        val cameraLayout = inflatedView.findViewById<LinearLayout>(R.id.camera_layout)
        cameraLayout.setOnClickListener {
            val listItems = listOf(resources.getString(R.string.NoCamera))
                            .plus(cameraList.map { it.name }).toTypedArray()

            val index = cameraList.indexOfFirst { it == newRoll.camera }
            val checkedItem = if (index == -1) 0 else index + 1

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.UsedCamera)
            builder.setSingleChoiceItems(listItems, checkedItem) { dialogInterface: DialogInterface, which: Int ->
                // listItems also contains the No camera option
                newRoll.camera = if (which > 0) {
                    cameraTextView.text = listItems[which]
                    cameraList[which - 1]
                } else {
                    cameraTextView.text = ""
                    null
                }
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            val alert1 = builder.create()
            alert1.show()
        }
        //==========================================================================================


        //==========================================================================================
        // CAMERA ADD DIALOG
        val addCameraImageView = inflatedView.findViewById<ImageView>(R.id.add_camera)
        addCameraImageView.isClickable = true
        addCameraImageView.setOnClickListener {
            noteEditText.clearFocus()
            nameEditText.clearFocus()
            val dialog = EditCameraDialog()
            dialog.setTargetFragment(this@EditRollDialog, REQUEST_CODE_ADD_CAMERA)
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewCamera))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
        }
        //==========================================================================================


        //==========================================================================================
        // DATE & TIME LOADED PICK DIALOG

        // DATE
        if (roll.date == null) {
            roll.date = DateTime.fromCurrentTime()
        }
        val dateTimeLoaded = roll.date
        val dateTextView = inflatedView.findViewById<TextView>(R.id.date_text)
        val timeTextView = inflatedView.findViewById<TextView>(R.id.time_text)
        dateTextView.text = dateTimeLoaded?.dateAsText
        timeTextView.text = dateTimeLoaded?.timeAsText
        val dateLayout = inflatedView.findViewById<LinearLayout>(R.id.date_layout)
        val timeLayout = inflatedView.findViewById<LinearLayout>(R.id.time_layout)
        val dateLoadedManager = DateTimeLayoutManager(requireActivity(), dateLayout,
                timeLayout, dateTextView, timeTextView, dateTimeLoaded, null)

        //==========================================================================================


        //==========================================================================================
        // DATE & TIME UNLOADED PICK DIALOG
        val dateUnloadedTextView = inflatedView.findViewById<TextView>(R.id.date_unloaded_text)
        val timeUnloadedTextView = inflatedView.findViewById<TextView>(R.id.time_unloaded_text)
        val dateTimeUnloaded: DateTime? = roll.unloaded
        dateUnloadedTextView.text = dateTimeUnloaded?.dateAsText
        timeUnloadedTextView.text = dateTimeUnloaded?.timeAsText
        val dateUnloadedLayout = inflatedView.findViewById<LinearLayout>(R.id.date_unloaded_layout)
        val timeUnloadedLayout = inflatedView.findViewById<LinearLayout>(R.id.time_unloaded_layout)
        val clearDateUnloaded = inflatedView.findViewById<ImageView>(R.id.clear_date_unloaded)
        val dateUnloadedManager = DateTimeLayoutManager(requireActivity(), dateUnloadedLayout,
                timeUnloadedLayout, dateUnloadedTextView, timeUnloadedTextView, dateTimeUnloaded,
                clearDateUnloaded)

        //==========================================================================================


        //==========================================================================================
        // DATE & TIME DEVELOPED PICK DIALOG
        val dateDevelopedTextView = inflatedView.findViewById<TextView>(R.id.date_developed_text)
        val timeDevelopedTextView = inflatedView.findViewById<TextView>(R.id.time_developed_text)
        val dateTimeDeveloped: DateTime? = roll.developed
        dateDevelopedTextView.text = dateTimeDeveloped?.dateAsText
        timeDevelopedTextView.text = dateTimeDeveloped?.timeAsText
        val dateDevelopedLayout = inflatedView.findViewById<LinearLayout>(R.id.date_developed_layout)
        val timeDevelopedLayout = inflatedView.findViewById<LinearLayout>(R.id.time_developed_layout)
        val clearDateDeveloped = inflatedView.findViewById<ImageView>(R.id.clear_date_developed)
        val dateDevelopedManager = DateTimeLayoutManager(requireActivity(), dateDevelopedLayout,
                timeDevelopedLayout, dateDevelopedTextView, timeDevelopedTextView, dateTimeDeveloped,
                clearDateDeveloped)

        //==========================================================================================


        //==========================================================================================
        //ISO PICKER
        isoTextView = inflatedView.findViewById(R.id.iso_text)
        isoTextView.text = if (roll.iso == 0) "" else roll.iso.toString()
        val isoLayout = inflatedView.findViewById<LinearLayout>(R.id.iso_layout)
        isoLayout.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val isoPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            val isoValues = requireActivity().resources.getStringArray(R.array.ISOValues)
            isoPicker.minValue = 0
            isoPicker.maxValue = isoValues.size - 1
            isoPicker.displayedValues = isoValues
            isoPicker.value = 0
            val initialValue = isoValues.indexOfFirst { it.toInt() == newRoll.iso }
            if (initialValue != -1) isoPicker.value = initialValue

            //To prevent text edit
            isoPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseISO))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newRoll.iso = isoValues[isoPicker.value].toInt()
                isoTextView.text = if (newRoll.iso == 0) "" else newRoll.iso.toString()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
        //==========================================================================================


        //==========================================================================================
        //PUSH PULL PICKER
        val pushPullTextView = inflatedView.findViewById<TextView>(R.id.push_pull_text)
        pushPullTextView.text = if (roll.pushPull == null || roll.pushPull == "0") "" else roll.pushPull
        val pushPullLayout = inflatedView.findViewById<LinearLayout>(R.id.push_pull_layout)
        pushPullLayout.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val pushPullPicker = Utilities.fixNumberPicker(dialogView.findViewById(R.id.number_picker))
            val compValues = requireActivity().resources.getStringArray(R.array.CompValues)
            pushPullPicker.minValue = 0
            pushPullPicker.maxValue = compValues.size - 1
            pushPullPicker.displayedValues = compValues
            pushPullPicker.value = 9
            val initialValue = compValues.indexOfFirst { it == newRoll.pushPull }
            if (initialValue != -1) pushPullPicker.value = initialValue

            //To prevent text edit
            pushPullPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChoosePushOrPull))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newRoll.pushPull = compValues[pushPullPicker.value]
                pushPullTextView.text = if (newRoll.pushPull == "0") "" else newRoll.pushPull
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
        //==========================================================================================


        //==========================================================================================
        //FORMAT PICK DIALOG
        val formatTextView = inflatedView.findViewById<TextView>(R.id.format_text)
        formatTextView.text = resources.getStringArray(R.array.FilmFormats)[roll.format]
        val formatLayout = inflatedView.findViewById<LinearLayout>(R.id.format_layout)
        formatLayout.setOnClickListener {
            val checkedItem = newRoll.format
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(resources.getString(R.string.ChooseFormat))
            builder.setSingleChoiceItems(R.array.FilmFormats, checkedItem) { dialogInterface: DialogInterface, i: Int ->
                newRoll.format = i
                formatTextView.text = resources.getStringArray(R.array.FilmFormats)[i]
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            builder.create().show()
        }
        //==========================================================================================


        //FINALISE SETTING UP THE DIALOG
        alert.setPositiveButton(positiveButton, null)
        alert.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            //dialog.cancel();
            val intent = Intent()
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, intent)
        }
        val dialog = alert.create()

        // SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        dialog.show()

        // We override the positive button onClick so that we can dismiss the dialog
        // only when both roll name and camera are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            var name = nameEditText.text.toString()

            // Check if name is not set and if name can be replaced with the film stock's name.
            if (name.isEmpty()) name = newRoll.filmStock?.name ?: ""

            // Check the length again.
            if (name.isNotEmpty()) {
                roll.name = name
                roll.note = noteEditText.text.toString()
                roll.camera = newRoll.camera
                roll.date = dateLoadedManager.dateTime
                roll.unloaded = dateUnloadedManager.dateTime
                roll.developed = dateDevelopedManager.dateTime
                roll.iso = newRoll.iso
                roll.pushPull = newRoll.pushPull
                roll.format = newRoll.format
                roll.filmStock = newRoll.filmStock
                val intent = Intent()
                intent.putExtra(ExtraKeys.ROLL, roll)
                dialog.dismiss()
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            } else {
                Toast.makeText(activity, resources.getString(R.string.NoName),
                        Toast.LENGTH_SHORT).show()
            }
        }
        return dialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_ADD_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                // After Ok code.
                val camera: Camera = data?.getParcelableExtra(ExtraKeys.CAMERA) ?: return
                camera.id = database.addCamera(camera)
                cameraList.add(camera)
                cameraTextView.text = camera.name
                newRoll.camera = camera
            }
            REQUEST_CODE_SELECT_FILM_STOCK ->  if (resultCode == Activity.RESULT_OK) {
                val filmStock: FilmStock = data?.getParcelableExtra(ExtraKeys.FILM_STOCK) ?: return
                filmStockTextView.text = filmStock.name
                nameEditText.hint = filmStock.name
                newRoll.filmStock = filmStock
                filmStockClearImageView.visibility = View.VISIBLE
                // If the film stock ISO is defined, set the ISO
                if (filmStock.iso != 0) {
                    newRoll.iso = filmStock.iso
                    isoTextView.text = if (newRoll.iso == 0) "" else newRoll.iso.toString()
                }
            }
        }
    }

}