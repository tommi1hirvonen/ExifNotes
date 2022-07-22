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

package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogRollBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.utilities.*

/**
 * Dialog to edit Roll's information
 */
class EditRollDialog : BottomSheetDialogFragment() {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val TAG = "EditRollDialog"
    }
    
    private lateinit var binding: DialogRollBinding

    /**
     * Holds all the cameras in the database
     */
    private lateinit var cameraList: MutableList<Camera>

    private lateinit var roll: Roll
    private lateinit var newRoll: Roll

    private lateinit var dateLoadedManager: DateTimeLayoutManager
    private lateinit var dateUnloadedManager: DateTimeLayoutManager
    private lateinit var dateDevelopedManager: DateTimeLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogRollBinding.inflate(inflater, container, false)
        binding.title.titleTextView.text = requireArguments().getString(ExtraKeys.TITLE)
        binding.title.titleLayout.setBackgroundColor(requireContext().primaryUiColor)

        roll = requireArguments().getParcelable(ExtraKeys.ROLL) ?: Roll()
        newRoll = roll.copy()
        cameraList = database.cameras.toMutableList()


        // NAME EDIT TEXT
        binding.nameEditText.setText(roll.name)
        // Place the cursor at the end of the input field
        binding.nameEditText.setSelection(binding.nameEditText.text.length)
        binding.nameEditText.isSingleLine = false


        // NOTE EDIT TEXT
        binding.noteEditText.isSingleLine = false
        binding.noteEditText.setText(roll.note)
        binding.noteEditText.setSelection(binding.noteEditText.text.length)


        // FILM STOCK PICK DIALOG
        roll.filmStock?.let {
            binding.filmStockText.text = it.name
            binding.nameEditText.hint = it.name
            binding.addFilmStock.visibility = View.GONE
            binding.clearFilmStock.visibility = View.VISIBLE
        } ?: run {
            binding.filmStockText.text = ""
            binding.clearFilmStock.visibility = View.GONE
            binding.addFilmStock.visibility = View.VISIBLE
        }
        binding.clearFilmStock.setOnClickListener {
            newRoll.filmStock = null
            binding.nameEditText.hint = ""
            binding.filmStockText.text = ""
            binding.clearFilmStock.visibility = View.GONE
            binding.addFilmStock.visibility = View.VISIBLE
        }
        binding.addFilmStock.setOnClickListener {
            binding.noteEditText.clearFocus()
            binding.nameEditText.clearFocus()
            val dialog = EditFilmStockDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilmStock))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), null)
            dialog.setFragmentResultListener("EditFilmStockDialog") { _, bundle ->
                val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                    ?: return@setFragmentResultListener
                database.addFilmStock(filmStock)
                setFilmStock(filmStock)
            }
        }
        binding.filmStockLayout.setOnClickListener {
            val dialog = SelectFilmStockDialog()
            dialog.show(parentFragmentManager.beginTransaction(), null)
            dialog.setFragmentResultListener("SelectFilmStockDialog") { _, bundle ->
                val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                    ?: return@setFragmentResultListener
                setFilmStock(filmStock)
            }
        }


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


        // CAMERA ADD DIALOG
        binding.addCamera.isClickable = true
        binding.addCamera.setOnClickListener {
            binding.noteEditText.clearFocus()
            binding.nameEditText.clearFocus()
            val dialog = EditCameraDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewCamera))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
            dialog.setFragmentResultListener("EditCameraDialog") { _, bundle ->
                val camera: Camera = bundle.getParcelable(ExtraKeys.CAMERA)
                    ?: return@setFragmentResultListener
                cameraList.add(camera)
                binding.cameraText.text = camera.name
                newRoll.camera = camera
            }
        }


        // DATE & TIME LOADED PICK DIALOG

        // DATE
        if (roll.date == null) {
            roll.date = DateTime.fromCurrentTime()
        }
        binding.dateText.text = roll.date?.dateAsText
        binding.timeText.text = roll.date?.timeAsText
        dateLoadedManager = DateTimeLayoutManager(requireActivity(), binding.dateLayout,
                binding.timeLayout, binding.dateText, binding.timeText, roll.date, null)



        // DATE & TIME UNLOADED PICK DIALOG
        binding.dateUnloadedText.text = roll.unloaded?.dateAsText
        binding.timeUnloadedText.text = roll.unloaded?.timeAsText
        dateUnloadedManager = DateTimeLayoutManager(requireActivity(), binding.dateUnloadedLayout,
                binding.timeUnloadedLayout, binding.dateUnloadedText, binding.timeUnloadedText, roll.unloaded,
                binding.clearDateUnloaded)



        // DATE & TIME DEVELOPED PICK DIALOG
        binding.dateDevelopedText.text = roll.developed?.dateAsText
        binding.timeDevelopedText.text = roll.developed?.timeAsText
        dateDevelopedManager = DateTimeLayoutManager(requireActivity(), binding.dateDevelopedLayout,
                binding.timeDevelopedLayout, binding.dateDevelopedText, binding.timeDevelopedText, roll.developed,
                binding.clearDateDeveloped)



        //ISO PICKER
        binding.isoText.text = if (roll.iso == 0) "" else roll.iso.toString()
        binding.isoLayout.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            val inflater1 = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater1.inflate(R.layout.dialog_single_numberpicker, null)
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


        //PUSH PULL PICKER
        try {
            if (newRoll.pushPull != null) {
                val values = resources.getStringArray(R.array.CompValues)
                binding.pushPullSpinner.setSelection(values.indexOf(newRoll.pushPull))
            } else {
                binding.pushPullSpinner.setSelection(9)
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }


        //FORMAT PICKER
        try {
            binding.formatSpinner.setSelection(newRoll.format)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        binding.title.negativeImageView.setOnClickListener { dismiss() }
        binding.title.positiveImageView.setOnClickListener {
            if (commitChanges()) {
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.ROLL, roll)
                setFragmentResult("EditRollDialog", bundle)
                dismiss()
            }
        }

        return binding.root
    }

    private fun commitChanges(): Boolean {
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
            roll.pushPull = binding.pushPullSpinner.selectedItem as String?
            roll.format = binding.formatSpinner.selectedItemPosition
            roll.filmStock = newRoll.filmStock
            return true
        } else {
            Toast.makeText(activity, resources.getString(R.string.NoName),
                    Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun setFilmStock(filmStock: FilmStock) {
        binding.filmStockText.text = filmStock.name
        binding.nameEditText.hint = filmStock.name
        newRoll.filmStock = filmStock
        binding.addFilmStock.visibility = View.GONE
        binding.clearFilmStock.visibility = View.VISIBLE
        // If the film stock ISO is defined, set the ISO
        if (filmStock.iso != 0) {
            newRoll.iso = filmStock.iso
            binding.isoText.text = if (newRoll.iso == 0) "" else newRoll.iso.toString()
        }
    }

}