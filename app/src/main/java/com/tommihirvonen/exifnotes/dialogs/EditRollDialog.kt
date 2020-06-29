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
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogRollBinding
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
    
    private lateinit var binding: DialogRollBinding

    /**
     * Holds all the cameras in the database
     */
    private lateinit var cameraList: MutableList<Camera>
    
    private lateinit var newRoll: Roll

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        binding = DialogRollBinding.inflate(layoutInflater)
        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButton = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        val roll = requireArguments().getParcelable(ExtraKeys.ROLL) ?: Roll()
        newRoll = roll.copy()
        cameraList = database.allCameras.toMutableList()
        
        val alert = AlertDialog.Builder(activity)
        binding.nestedScrollView.setOnScrollChangeListener(
                ScrollIndicatorNestedScrollViewListener(
                        requireActivity(),
                        binding.nestedScrollView,
                        binding.scrollIndicatorUp,
                        binding.scrollIndicatorDown))
        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(requireActivity(), title))
        alert.setView(binding.root)

        // Color the dividers white if the app's theme is dark
        if (isAppThemeDark) {
            val color = ContextCompat.getColor(requireActivity(), R.color.white)
            listOf(binding.dividerView1, binding.dividerView2, binding.dividerView3, binding.dividerView4,
                    binding.dividerView5, binding.dividerView6, binding.dividerView7, binding.dividerView8, 
                    binding.dividerView9, binding.dividerView10)
                    .forEach { it.setBackgroundColor(color) }
            val color2 = ContextCompat.getColor(requireActivity(), R.color.light_grey)
            binding.addCamera.drawable.setColorFilterCompat(color2)
            binding.clearFilmStock.drawable.setColorFilterCompat(color2)
            binding.clearDateUnloaded.drawable.setColorFilterCompat(color2)
            binding.clearDateDeveloped.drawable.setColorFilterCompat(color2)
        }
        //==========================================================================================

        // NAME EDIT TEXT
        binding.nameEditText.setText(roll.name)
        // Place the cursor at the end of the input field
        binding.nameEditText.setSelection(binding.nameEditText.text.length)
        binding.nameEditText.isSingleLine = false


        //==========================================================================================
        // NOTE EDIT TEXT
        binding.noteEditText.isSingleLine = false
        binding.noteEditText.setText(roll.note)
        binding.noteEditText.setSelection(binding.noteEditText.text.length)
        //==========================================================================================


        //==========================================================================================
        // FILM STOCK PICK DIALOG
        roll.filmStock?.let {
            binding.filmStockText.text = it.name
            binding.nameEditText.hint = it.name
        } ?: run {
            binding.filmStockText.text = ""
            binding.clearFilmStock.visibility = View.GONE
        }
        binding.clearFilmStock.setOnClickListener {
            newRoll.filmStock = null
            binding.nameEditText.hint = ""
            binding.filmStockText.text = ""
            binding.clearFilmStock.visibility = View.GONE
        }
        binding.filmStockLayout.setOnClickListener {
            val dialog = SelectFilmStockDialog()
            dialog.setTargetFragment(this@EditRollDialog, REQUEST_CODE_SELECT_FILM_STOCK)
            dialog.show(parentFragmentManager.beginTransaction(), null)
        }
        //==========================================================================================


        //==========================================================================================
        // CAMERA PICK DIALOG
        binding.cameraText.text = roll.camera?.name ?: ""
        binding.cameraLayout.setOnClickListener {
            val listItems = listOf(resources.getString(R.string.NoCamera))
                            .plus(cameraList.map { it.name }).toTypedArray()

            val index = cameraList.indexOfFirst { it == newRoll.camera }
            val checkedItem = if (index == -1) 0 else index + 1

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.UsedCamera)
            builder.setSingleChoiceItems(listItems, checkedItem) { dialogInterface: DialogInterface, which: Int ->
                // listItems also contains the No camera option
                newRoll.camera = if (which > 0) {
                    binding.cameraText.text = listItems[which]
                    cameraList[which - 1]
                } else {
                    binding.cameraText.text = ""
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
        binding.addCamera.isClickable = true
        binding.addCamera.setOnClickListener {
            binding.noteEditText.clearFocus()
            binding.nameEditText.clearFocus()
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
        binding.dateText.text = roll.date?.dateAsText
        binding.timeText.text = roll.date?.timeAsText
        val dateLoadedManager = DateTimeLayoutManager(requireActivity(), binding.dateLayout,
                binding.timeLayout, binding.dateText, binding.timeText, roll.date, null)

        //==========================================================================================


        //==========================================================================================
        // DATE & TIME UNLOADED PICK DIALOG
        binding.dateUnloadedText.text = roll.unloaded?.dateAsText
        binding.timeUnloadedText.text = roll.unloaded?.timeAsText
        val dateUnloadedManager = DateTimeLayoutManager(requireActivity(), binding.dateUnloadedLayout,
                binding.timeUnloadedLayout, binding.dateUnloadedText, binding.timeUnloadedText, roll.unloaded,
                binding.clearDateUnloaded)

        //==========================================================================================


        //==========================================================================================
        // DATE & TIME DEVELOPED PICK DIALOG
        binding.dateDevelopedText.text = roll.developed?.dateAsText
        binding.timeDevelopedText.text = roll.developed?.timeAsText
        val dateDevelopedManager = DateTimeLayoutManager(requireActivity(), binding.dateDevelopedLayout,
                binding.timeDevelopedLayout, binding.dateDevelopedText, binding.timeDevelopedText, roll.developed,
                binding.clearDateDeveloped)

        //==========================================================================================


        //==========================================================================================
        //ISO PICKER
        binding.isoText.text = if (roll.iso == 0) "" else roll.iso.toString()
        binding.isoLayout.setOnClickListener {
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
                binding.isoText.text = if (newRoll.iso == 0) "" else newRoll.iso.toString()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
        //==========================================================================================


        //==========================================================================================
        //PUSH PULL PICKER
        binding.pushPullText.text = if (roll.pushPull == null || roll.pushPull == "0") "" else roll.pushPull
        binding.pushPullLayout.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val pushPullPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker).fix()
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
                binding.pushPullText.text = if (newRoll.pushPull == "0") "" else newRoll.pushPull
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
        //==========================================================================================


        //==========================================================================================
        //FORMAT PICK DIALOG
        binding.formatText.text = resources.getStringArray(R.array.FilmFormats)[roll.format]
        binding.formatLayout.setOnClickListener {
            val checkedItem = newRoll.format
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(resources.getString(R.string.ChooseFormat))
            builder.setSingleChoiceItems(R.array.FilmFormats, checkedItem) { dialogInterface: DialogInterface, i: Int ->
                newRoll.format = i
                binding.formatText.text = resources.getStringArray(R.array.FilmFormats)[i]
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
            var name = binding.nameEditText.text.toString()

            // Check if name is not set and if name can be replaced with the film stock's name.
            if (name.isEmpty()) name = newRoll.filmStock?.name ?: ""

            // Check the length again.
            if (name.isNotEmpty()) {
                roll.name = name
                roll.note = binding.noteEditText.text.toString()
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
                binding.cameraText.text = camera.name
                newRoll.camera = camera
            }
            REQUEST_CODE_SELECT_FILM_STOCK ->  if (resultCode == Activity.RESULT_OK) {
                val filmStock: FilmStock = data?.getParcelableExtra(ExtraKeys.FILM_STOCK) ?: return
                binding.filmStockText.text = filmStock.name
                binding.nameEditText.hint = filmStock.name
                newRoll.filmStock = filmStock
                binding.clearFilmStock.visibility = View.VISIBLE
                // If the film stock ISO is defined, set the ISO
                if (filmStock.iso != 0) {
                    newRoll.iso = filmStock.iso
                    binding.isoText.text = if (newRoll.iso == 0) "" else newRoll.iso.toString()
                }
            }
        }
    }

}