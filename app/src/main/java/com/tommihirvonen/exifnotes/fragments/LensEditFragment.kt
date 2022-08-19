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
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentLensEditBinding
import com.tommihirvonen.exifnotes.datastructures.Increment
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.snackbar
import com.tommihirvonen.exifnotes.utilities.validate

/**
 * Dialog to edit Lens's information
 */
class LensEditFragment : Fragment() {

    private val fixedLens by lazy { requireArguments().getBoolean(ExtraKeys.FIXED_LENS) }
    private val lens by lazy { requireArguments().getParcelable(ExtraKeys.LENS) ?: Lens() }
    private val newLens by lazy { lens.copy() }

    private lateinit var apertureValueOptions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentLensEditBinding.inflate(inflater)

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
        val minApertureMenu = binding.minApertureMenu.editText as MaterialAutoCompleteTextView
        val maxApertureMenu = binding.maxApertureMenu.editText as MaterialAutoCompleteTextView

        val apertureIncrementsValues = resources.getStringArray(R.array.StopIncrements)
        val apertureIncrementsMenu =
            binding.apertureIncrementsMenu.editText as MaterialAutoCompleteTextView
        apertureIncrementsMenu.setSimpleItems(apertureIncrementsValues)

        try {
            val text = apertureIncrementsValues[lens.apertureIncrements.ordinal]
            apertureIncrementsMenu.setText(text, false)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        apertureValueOptions = getApertureValueOptions()

        apertureIncrementsMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            newLens.apertureIncrements = Increment.from(position)
            apertureValueOptions = getApertureValueOptions()
            minApertureMenu.setSimpleItems(apertureValueOptions)
            maxApertureMenu.setSimpleItems(apertureValueOptions)
            val minFound = apertureValueOptions.contains(newLens.minAperture)
            val maxFound = apertureValueOptions.contains(newLens.maxAperture)
            // If either one wasn't found in the new values array, null them.
            if (!minFound || !maxFound) {
                newLens.minAperture = null
                newLens.maxAperture = null
                minApertureMenu.setText(null, false)
                maxApertureMenu.setText(null, false)
            }
        }

        // APERTURE RANGE BUTTON
        newLens.minAperture = lens.minAperture
        newLens.maxAperture = lens.maxAperture
        binding.clearApertureRange.setOnClickListener {
            newLens.minAperture = null
            newLens.maxAperture = null
            minApertureMenu.setText(null, false)
            maxApertureMenu.setText(null, false)
        }
        minApertureMenu.setSimpleItems(apertureValueOptions)
        maxApertureMenu.setSimpleItems(apertureValueOptions)
        minApertureMenu.setText(newLens.minAperture, false)
        maxApertureMenu.setText(newLens.maxAperture, false)

        minApertureMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                newLens.minAperture = apertureValueOptions[position]
            }
            else {
                newLens.minAperture = null
                minApertureMenu.setText(null, false)
            }
        }
        maxApertureMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                newLens.maxAperture = apertureValueOptions[position]
            }
            else {
                newLens.maxAperture = null
                maxApertureMenu.setText(null, false)
            }
        }

        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.minApertureMenu.setEndIconOnClickListener(null)
        minApertureMenu.setOnClickListener {
            val currentIndex = apertureValueOptions.indexOf(minApertureMenu.text.toString())
            if (currentIndex >= 0) minApertureMenu.listSelection = currentIndex
        }
        binding.maxApertureMenu.setEndIconOnClickListener(null)
        maxApertureMenu.setOnClickListener {
            val currentIndex = apertureValueOptions.indexOf(maxApertureMenu.text.toString())
            if (currentIndex >= 0) maxApertureMenu.listSelection = currentIndex
        }

        // FOCAL LENGTH RANGE BUTTON
        binding.minFocalLengthEditText.filters = arrayOf<InputFilter>(FocalLengthInputFilter())
        binding.minFocalLengthEditText.setText(newLens.minFocalLength.toString())
        binding.maxFocalLengthEditText.filters = arrayOf<InputFilter>(FocalLengthInputFilter())
        binding.maxFocalLengthEditText.setText(newLens.maxFocalLength.toString())

        binding.topAppBar.setNavigationOnClickListener { navigateBack() }
        binding.positiveButton.setOnClickListener {
            val make = binding.makeEditText.text.toString()
            val model = binding.modelEditText.text.toString()
            val serialNumber = binding.serialNumberEditText.text.toString()
            val minFocalLength = binding.minFocalLengthEditText.text.toString().toInt()
            val maxFocalLength = binding.maxFocalLengthEditText.text.toString().toInt()

            val nameValidation = { _: Lens ->
                (make.isNotEmpty() && model.isNotEmpty() || fixedLens) to
                        resources.getString(R.string.MakeAndOrModelIsEmpty)
            }
            val focalLengthValidation = { _: Lens -> (minFocalLength <= maxFocalLength) to
                    resources.getString(R.string.MinFocalLengthGreaterThanMax)
            }
            val apertureRangeValidation = { l: Lens -> validateApertureRange(l) }

            val (validationResult, validationMessage) = newLens.validate(
                nameValidation, focalLengthValidation, apertureRangeValidation)

            if (validationResult) {
                //All the required information was given. Save.
                lens.make = make
                lens.model = model
                lens.serialNumber = serialNumber
                lens.minAperture = newLens.minAperture
                lens.maxAperture = newLens.maxAperture
                lens.minFocalLength = minFocalLength
                lens.maxFocalLength = maxFocalLength

                val incrementIndex = apertureIncrementsValues
                    .indexOf(apertureIncrementsMenu.text.toString())
                lens.apertureIncrements = Increment.from(incrementIndex)

                // Return the new entered name to the calling activity
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.LENS, lens)
                setFragmentResult("LensEditFragment", bundle)
                navigateBack()
            } else {
                binding.root.snackbar(validationMessage)
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

    private fun getApertureValueOptions() = when (newLens.apertureIncrements) {
        Increment.THIRD -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
        Increment.HALF -> requireActivity().resources.getStringArray(R.array.ApertureValuesHalf)
        Increment.FULL -> requireActivity().resources.getStringArray(R.array.ApertureValuesFull)
    }

    private fun validateApertureRange(lens: Lens): Pair<Boolean, String> {
        if (lens.minAperture == null && lens.maxAperture != null ||
            lens.minAperture != null && lens.maxAperture == null) {
            return false to resources.getString(R.string.NoMinOrMaxAperture)
        }
        val min = lens.minAperture?.toDoubleOrNull() ?: 0.0
        val max = lens.maxAperture?.toDoubleOrNull() ?: 0.0
        // Note, that the minimum aperture should actually be smaller in numeric value than the max.
        // Small aperture values mean a large opening and vice versa.
        if (min < max) {
            return false to resources.getString(R.string.MinApertureGreaterThanMax)
        }
        return true to ""
    }

    /**
     * Private InputFilter class used to make sure user entered focal length values are
     * between 0 and 1000000
     */
    private inner class FocalLengthInputFilter : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int,
                            dend: Int): CharSequence? {
            try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in 0..1500) return null
            } catch (ignored: NumberFormatException) { }
            return ""
        }
    }

}