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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentEditRollBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.dialogs.EditCameraDialog
import com.tommihirvonen.exifnotes.dialogs.EditFilmStockDialog
import com.tommihirvonen.exifnotes.dialogs.SelectFilmStockDialog
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.RollViewModel

/**
 * Dialog to edit Roll's information
 */
class EditRollFragment : Fragment() {

    private val model by activityViewModels<RollViewModel>()
    private var cameras = emptyList<Camera>()
    private val cameraItems get() = listOf(resources.getString(R.string.NoCamera))
        .plus(cameras.map { it.name }).toTypedArray()
    
    private lateinit var binding: FragmentEditRollBinding

    private val roll by lazy { requireArguments().getParcelable(ExtraKeys.ROLL) ?: Roll() }
    private val newRoll by lazy { roll.copy() }

    private lateinit var dateLoadedManager: DateTimeLayoutManager
    private lateinit var dateUnloadedManager: DateTimeLayoutManager
    private lateinit var dateDevelopedManager: DateTimeLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditRollBinding.inflate(inflater, container, false)

        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)

        val cameraAutoComplete = binding.cameraMenu.editText as MaterialAutoCompleteTextView
        model.cameras.observe(viewLifecycleOwner) { cameras ->
            this.cameras = cameras
            cameraAutoComplete.setSimpleItems(cameraItems)
        }

        // NAME EDIT TEXT
        binding.nameEditText.addTextChangedListener { binding.nameLayout.error = null }
        binding.nameEditText.setText(roll.name)
        // Place the cursor at the end of the input field
        binding.nameEditText.setSelection(binding.nameEditText.text?.length ?: 0)
        binding.nameEditText.isSingleLine = false


        // NOTE EDIT TEXT
        binding.noteEditText.isSingleLine = false
        binding.noteEditText.setText(roll.note)
        binding.noteEditText.setSelection(binding.noteEditText.text?.length ?: 0)


        // FILM STOCK PICK DIALOG
        roll.filmStock?.let {
            binding.filmStockLayout.text = it.name
            binding.addFilmStock.visibility = View.GONE
            binding.clearFilmStock.visibility = View.VISIBLE
        } ?: run {
            binding.filmStockLayout.text = null
            binding.clearFilmStock.visibility = View.GONE
            binding.addFilmStock.visibility = View.VISIBLE
        }
        binding.clearFilmStock.setOnClickListener {
            newRoll.filmStock = null
            binding.filmStockLayout.text = null
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
        cameraAutoComplete.setText(roll.camera?.name, false)
        cameraAutoComplete.setSimpleItems(cameraItems)
        cameraAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            newRoll.camera = if (position > 0) {
                cameras[position - 1].also { cameraAutoComplete.setText(it.name, false) }
            } else {
                cameraAutoComplete.setText(null, false)
                null
            }
        }

        // CAMERA ADD DIALOG
        binding.addCamera.isClickable = true
        binding.addCamera.setOnClickListener {
            binding.noteEditText.clearFocus()
            binding.nameEditText.clearFocus()
            val dialog = EditCameraDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewCamera))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
            dialog.setFragmentResultListener("EditCameraDialog") { _, bundle ->
                val camera: Camera = bundle.getParcelable(ExtraKeys.CAMERA)
                    ?: return@setFragmentResultListener
                model.addCamera(camera)
                cameraAutoComplete.setSimpleItems(cameraItems)
                cameraAutoComplete.setText(camera.name)
                newRoll.camera = camera
            }
        }

        // DATE & TIME LOADED PICK DIALOG

        // DATE
        if (roll.date == null) {
            roll.date = DateTime.fromCurrentTime()
        }

        dateLoadedManager = DateTimeLayoutManager(
            requireActivity(),
            binding.dateLoadedLayout,
            roll.date,
            null)

        // DATE & TIME UNLOADED PICK DIALOG
        dateUnloadedManager = DateTimeLayoutManager(
            requireActivity(),
            binding.dateUnloadedLayout,
            roll.unloaded,
            binding.clearDateUnloaded)

        // DATE & TIME DEVELOPED PICK DIALOG
        dateDevelopedManager = DateTimeLayoutManager(
            requireActivity(),
            binding.dateDevelopedLayout,
            roll.developed,
            binding.clearDateDeveloped)


        //ISO PICKER
        val iso = if (roll.iso == 0) null else roll.iso.toString()
        val isoAutoComplete = binding.isoPushPullFormat.isoMenu.editText as MaterialAutoCompleteTextView
        isoAutoComplete.setText(iso, false)
        val isoValues = requireActivity().resources.getStringArray(R.array.ISOValues)
        isoAutoComplete.setSimpleItems(isoValues)
        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.isoPushPullFormat.isoMenu.setEndIconOnClickListener(null)
        isoAutoComplete.setOnClickListener {
            val currentIndex = isoValues.indexOf(isoAutoComplete.text.toString())
            if (currentIndex >= 0) isoAutoComplete.listSelection = currentIndex
        }


        //PUSH PULL PICKER
        val pushPullValues = resources.getStringArray(R.array.CompValues)
        val pushPullAutoComplete = binding.isoPushPullFormat.pushPullMenu.editText as MaterialAutoCompleteTextView
        pushPullAutoComplete.setText(newRoll.pushPull, false)
        pushPullAutoComplete.setSimpleItems(pushPullValues)
        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.isoPushPullFormat.pushPullMenu.setEndIconOnClickListener(null)
        pushPullAutoComplete.setOnClickListener {
            val currentIndex = pushPullValues.indexOf(pushPullAutoComplete.text.toString())
            if (currentIndex >= 0) pushPullAutoComplete.listSelection = currentIndex
        }


        //FORMAT PICKER
        val formats = resources.getStringArray(R.array.FilmFormats)
        val formatsAutoComplete = binding.isoPushPullFormat.formatMenu.editText as MaterialAutoCompleteTextView
        try {
            formatsAutoComplete.setText(formats[newRoll.format], false)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }
        formatsAutoComplete.setSimpleItems(formats)

        //binding.title.negativeButton.setOnClickListener { requireActivity().onBackPressed() }
        binding.topAppBar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        binding.positiveButton.setOnClickListener {
            if (commitChanges()) {
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.ROLL, roll)
                setFragmentResult("EditRollDialog", bundle)
                requireActivity().onBackPressed()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        // Start the transition once all views have been measured and laid out.
        (view.parent as ViewGroup).doOnPreDraw {
            startPostponedEnterTransition()
        }
    }

    private fun commitChanges(): Boolean {
        val name = binding.nameEditText.text.toString()
        if (name.isNotEmpty()) {
            roll.name = name
            roll.note = binding.noteEditText.text.toString()
            roll.camera = newRoll.camera
            roll.date = dateLoadedManager.dateTime
            roll.unloaded = dateUnloadedManager.dateTime
            roll.developed = dateDevelopedManager.dateTime

            val isoText = binding.isoPushPullFormat.isoMenu.editText?.text.toString()
            roll.iso = if (isoText.isEmpty()) 0 else isoText.toInt()

            roll.pushPull = binding.isoPushPullFormat.pushPullMenu.editText?.text.toString().ifEmpty { null }

            try {
                val format = binding.isoPushPullFormat.formatMenu.editText?.text.toString()
                val formats = resources.getStringArray(R.array.FilmFormats)
                roll.format = formats.indexOf(format)
            } catch (e: ArrayIndexOutOfBoundsException) {
                e.printStackTrace()
            }

            roll.filmStock = newRoll.filmStock
            return true
        } else {
            binding.nameLayout.error = getString(R.string.NoName)
            Toast.makeText(activity, resources.getString(R.string.NoName),
                    Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun setFilmStock(filmStock: FilmStock) {
        binding.filmStockLayout.text = filmStock.name
        newRoll.filmStock = filmStock
        binding.addFilmStock.visibility = View.GONE
        binding.clearFilmStock.visibility = View.VISIBLE
        // If the film stock ISO is defined, set the ISO
        if (filmStock.iso != 0) {
            newRoll.iso = filmStock.iso
            val iso = if (newRoll.iso == 0) null else newRoll.iso.toString()
            val isoAutoComplete = binding.isoPushPullFormat.isoMenu.editText as MaterialAutoCompleteTextView
            isoAutoComplete.setText(iso, false)
        }
    }

}