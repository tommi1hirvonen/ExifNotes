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
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogDoubleNumberpickerBinding
import com.tommihirvonen.exifnotes.databinding.DialogDoubleNumberpickerButtonsBinding
import com.tommihirvonen.exifnotes.databinding.FragmentLensEditBinding
import com.tommihirvonen.exifnotes.datastructures.Increment
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.snackbar

/**
 * Dialog to edit Lens's information
 */
class LensEditFragment : Fragment() {

    companion object {
        /**
         * Constant used to indicate the maximum possible focal length.
         * Could theoretically be anything above zero.
         */
        private const val MAX_FOCAL_LENGTH = 1500
    }

    private val fixedLens by lazy { requireArguments().getBoolean(ExtraKeys.FIXED_LENS) }
    private val lens by lazy { requireArguments().getParcelable(ExtraKeys.LENS) ?: Lens() }
    private val newLens by lazy { lens.copy() }

    private lateinit var binding: FragmentLensEditBinding

    /**
     * Stores the currently displayed aperture values.
     * Changes depending on the currently selected aperture value increments.
     */
    private lateinit var displayedApertureValues: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLensEditBinding.inflate(inflater)

        // Hide certain layouts for fixed lenses. Make and model are later derived from camera.
        if (fixedLens) {
            binding.makeLayout.visibility = View.GONE
            binding.modelLayout.visibility = View.GONE
            binding.serialNumberLayout.visibility = View.GONE
        }

        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)

        // EDIT TEXT FIELDS
        binding.makeEditText.setText(lens.make)
        binding.modelEditText.setText(lens.model)
        binding.serialNumberEditText.setText(lens.serialNumber)


        // APERTURE INCREMENTS
        try {
            binding.incrementSpinner.setSelection(newLens.apertureIncrements.ordinal)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }
        binding.incrementSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //Check if the new increments include both min and max values.
                //Otherwise reset them to null
                displayedApertureValues = when (newLens.apertureIncrements) {
                    Increment.THIRD -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
                    Increment.HALF -> requireActivity().resources.getStringArray(R.array.ApertureValuesHalf)
                    Increment.FULL -> requireActivity().resources.getStringArray(R.array.ApertureValuesFull)
                }
                val minFound = displayedApertureValues.contains(newLens.minAperture)
                val maxFound = displayedApertureValues.contains(newLens.maxAperture)
                // If either one wasn't found in the new values array, null them.
                if (!minFound || !maxFound) {
                    newLens.minAperture = null
                    newLens.maxAperture = null
                    updateApertureRangeTextView()
                }
            }
        }

        // APERTURE RANGE BUTTON
        newLens.minAperture = lens.minAperture
        newLens.maxAperture = lens.maxAperture
        updateApertureRangeTextView()
        binding.apertureRangeLayout.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val binding1 = DialogDoubleNumberpickerBinding.inflate(inflater)
            val maxAperturePicker = binding1.numberPickerOne
            val minAperturePicker = binding1.numberPickerTwo

            //To prevent text edit
            minAperturePicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            maxAperturePicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            initialiseApertureRangePickers(minAperturePicker, maxAperturePicker)
            builder.setView(binding1.root)
            builder.setTitle(resources.getString(R.string.ChooseApertureRange))
            builder.setPositiveButton(resources.getString(R.string.OK), null)
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
            //Override the positiveButton to check the range before accepting.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (minAperturePicker.value == displayedApertureValues.size - 1 &&
                        maxAperturePicker.value != displayedApertureValues.size - 1
                        ||
                        minAperturePicker.value != displayedApertureValues.size - 1 &&
                        maxAperturePicker.value == displayedApertureValues.size - 1) {
                    // No min or max shutter was set
                    binding.root.snackbar(R.string.NoMinOrMaxAperture)
                } else {
                    if (minAperturePicker.value == displayedApertureValues.size - 1 &&
                            maxAperturePicker.value == displayedApertureValues.size - 1) {
                        newLens.minAperture = null
                        newLens.maxAperture = null
                    } else if (minAperturePicker.value < maxAperturePicker.value) {
                        newLens.minAperture = displayedApertureValues[minAperturePicker.value]
                        newLens.maxAperture = displayedApertureValues[maxAperturePicker.value]
                    } else {
                        newLens.minAperture = displayedApertureValues[maxAperturePicker.value]
                        newLens.maxAperture = displayedApertureValues[minAperturePicker.value]
                    }
                    updateApertureRangeTextView()
                }
                dialog.dismiss()
            }
        }

        // FOCAL LENGTH RANGE BUTTON
        updateFocalLengthRangeTextView()
        binding.focalLengthRangeLayout.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val binding1 = DialogDoubleNumberpickerButtonsBinding.inflate(inflater)
            val minFocalLengthPicker = binding1.numberPickerOne
            val maxFocalLengthPicker = binding1.numberPickerTwo
            val jumpAmount = 50
            val minFocalLengthFastRewind = binding1.pickerOneFastRewind
            val minFocalLengthFastForward = binding1.pickerOneFastForward
            val maxFocalLengthFastRewind = binding1.pickerTwoFastRewind
            val maxFocalLengthFastForward = binding1.pickerTwoFastForward

            // To prevent text edit
            minFocalLengthPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            maxFocalLengthPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            initialiseFocalLengthRangePickers(minFocalLengthPicker, maxFocalLengthPicker)
            minFocalLengthFastRewind.setOnClickListener { minFocalLengthPicker.value = minFocalLengthPicker.value - jumpAmount }
            minFocalLengthFastForward.setOnClickListener { minFocalLengthPicker.value = minFocalLengthPicker.value + jumpAmount }
            maxFocalLengthFastRewind.setOnClickListener { maxFocalLengthPicker.value = maxFocalLengthPicker.value - jumpAmount }
            maxFocalLengthFastForward.setOnClickListener { maxFocalLengthPicker.value = maxFocalLengthPicker.value + jumpAmount }
            builder.setView(binding1.root)
            builder.setTitle(resources.getString(R.string.ChooseFocalLengthRange))
            builder.setPositiveButton(resources.getString(R.string.OK), null)
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()

            // Override the positiveButton to check the range before accepting.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                // Set the max and min focal lengths. Check which is smaller and set it to be
                // min and vice versa.
                if (minFocalLengthPicker.value == 0 || maxFocalLengthPicker.value == 0) {
                    newLens.minFocalLength = 0
                    newLens.maxFocalLength = 0
                } else if (minFocalLengthPicker.value < maxFocalLengthPicker.value) {
                    newLens.minFocalLength = minFocalLengthPicker.value
                    newLens.maxFocalLength = maxFocalLengthPicker.value
                } else {
                    newLens.maxFocalLength = minFocalLengthPicker.value
                    newLens.minFocalLength = maxFocalLengthPicker.value
                }
                updateFocalLengthRangeTextView()
                dialog.dismiss()
            }
        }

        binding.topAppBar.setNavigationOnClickListener { navigateBack() }
        binding.positiveButton.setOnClickListener {
            val make = binding.makeEditText.text.toString()
            val model = binding.modelEditText.text.toString()
            val serialNumber = binding.serialNumberEditText.text.toString()
            if ((make.isEmpty() || model.isEmpty()) && !fixedLens) {
                // No make or model was set
                binding.root.snackbar(R.string.MakeAndOrModelIsEmpty)
            } else {
                //All the required information was given. Save.
                lens.make = make
                lens.model = model
                lens.serialNumber = serialNumber
                lens.apertureIncrements = Increment.from(binding.incrementSpinner.selectedItemPosition)
                lens.minAperture = newLens.minAperture
                lens.maxAperture = newLens.maxAperture
                lens.minFocalLength = newLens.minFocalLength
                lens.maxFocalLength = newLens.maxFocalLength

                // Return the new entered name to the calling activity
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.LENS, lens)
                setFragmentResult("LensEditFragment", bundle)
                navigateBack()
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

    private fun navigateBack() =
        requireParentFragment().childFragmentManager.popBackStack()

    /**
     * Called when the aperture range dialog is opened.
     * Sets the values for the NumberPickers.
     *
     * @param minAperturePicker NumberPicker associated with the minimum aperture value
     * @param maxAperturePicker NumberPicker associated with the maximum aperture value
     */
    private fun initialiseApertureRangePickers(minAperturePicker: NumberPicker,
                                               maxAperturePicker: NumberPicker) {
        displayedApertureValues = when (newLens.apertureIncrements) {
            Increment.THIRD -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
            Increment.HALF -> requireActivity().resources.getStringArray(R.array.ApertureValuesHalf)
            Increment.FULL -> requireActivity().resources.getStringArray(R.array.ApertureValuesFull)
        }
        if (displayedApertureValues[0] == resources.getString(R.string.NoValue)) {
            displayedApertureValues.reverse()
        }
        minAperturePicker.minValue = 0
        maxAperturePicker.minValue = 0
        minAperturePicker.maxValue = displayedApertureValues.size - 1
        maxAperturePicker.maxValue = displayedApertureValues.size - 1
        minAperturePicker.displayedValues = displayedApertureValues
        maxAperturePicker.displayedValues = displayedApertureValues
        minAperturePicker.value = displayedApertureValues.size - 1
        maxAperturePicker.value = displayedApertureValues.size - 1
        val initialMinValue = displayedApertureValues.indexOfFirst { it == newLens.minAperture }
        if (initialMinValue != -1) minAperturePicker.value = initialMinValue
        val initialMaxValue = displayedApertureValues.indexOfFirst { it == newLens.maxAperture }
        if (initialMaxValue != -1) maxAperturePicker.value = initialMaxValue
    }

    /**
     * Called when the focal length range dialog is opened.
     * Sets the values for the NumberPickers.
     *
     * @param minFocalLengthPicker NumberPicker associated with the minimum focal length
     * @param maxFocalLengthPicker NumberPicker associated with the maximum focal length
     */
    private fun initialiseFocalLengthRangePickers(minFocalLengthPicker: NumberPicker,
                                                  maxFocalLengthPicker: NumberPicker) {
        minFocalLengthPicker.minValue = 0
        maxFocalLengthPicker.minValue = 0
        minFocalLengthPicker.maxValue = MAX_FOCAL_LENGTH
        maxFocalLengthPicker.maxValue = MAX_FOCAL_LENGTH
        minFocalLengthPicker.value = 50
        maxFocalLengthPicker.value = 50
        val range = 0..MAX_FOCAL_LENGTH
        val initialMinValue = range.indexOfFirst { it == newLens.minFocalLength }
        if (initialMinValue != -1) minFocalLengthPicker.value = initialMinValue
        val initialMaxValue = range.indexOfFirst { it == newLens.maxFocalLength }
        if (initialMaxValue != -1) maxFocalLengthPicker.value = initialMaxValue
    }

    /**
     * Update the aperture range button's text
     */
    private fun updateApertureRangeTextView() {
        binding.apertureRangeText.text =
                if (newLens.minAperture == null || newLens.maxAperture == null) resources.getString(R.string.ClickToSet)
                else "f/${newLens.maxAperture} - f/${newLens.minAperture}"
    }

    /**
     * Update the focal length range button's text
     */
    private fun updateFocalLengthRangeTextView() {
        val text: String =
                if (newLens.minFocalLength == 0 || newLens.maxFocalLength == 0) {
                    resources.getString(R.string.ClickToSet)
                } else if (newLens.minFocalLength == newLens.maxFocalLength) {
                    "" + newLens.minFocalLength
                } else {
                    "${newLens.minFocalLength} - ${newLens.maxFocalLength}"
                }
        binding.focalLengthRangeText.text = text
    }

}