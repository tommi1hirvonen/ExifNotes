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
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentCameraEditBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Increment
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.PartialIncrement
import com.tommihirvonen.exifnotes.utilities.*

/**
 * Dialog to edit Camera's information
 */
class CameraEditFragment : Fragment() {

    private lateinit var binding: FragmentCameraEditBinding

    private val camera by lazy { requireArguments().getParcelable(ExtraKeys.CAMERA) ?: Camera() }

    private val newCamera by lazy { camera.copy() }

    private lateinit var shutterValueOptions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCameraEditBinding.inflate(inflater)

        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)

        // EDIT TEXT FIELDS
        binding.makeEditText.setText(camera.make)
        binding.modelEditText.setText(camera.model)
        binding.serialNumberEditText.setText(camera.serialNumber)

        // SHUTTER SPEED INCREMENTS
        val minShutterMenu = binding.minShutterMenu.editText as MaterialAutoCompleteTextView
        val maxShutterMenu = binding.maxShutterMenu.editText as MaterialAutoCompleteTextView

        val shutterIncrementValues = resources.getStringArray(R.array.StopIncrements)
        val shutterIncrementMenu =
            binding.shutterSpeedIncrementsMenu.editText as MaterialAutoCompleteTextView
        shutterIncrementMenu.setSimpleItems(shutterIncrementValues)
        try {
            val text = shutterIncrementValues[camera.shutterIncrements.ordinal]
            shutterIncrementMenu.setText(text, false)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }
        shutterIncrementMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            newCamera.shutterIncrements = Increment.from(position)
        }

        shutterValueOptions = getShutterValueOptions()

        shutterIncrementMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            newCamera.shutterIncrements = Increment.from(position)
            shutterValueOptions = getShutterValueOptions()
            minShutterMenu.setSimpleItems(shutterValueOptions)
            maxShutterMenu.setSimpleItems(shutterValueOptions)
            val minFound = shutterValueOptions.contains(newCamera.minShutter)
            val maxFound = shutterValueOptions.contains(newCamera.maxShutter)
            if (!minFound || !maxFound) {
                newCamera.minShutter = null
                newCamera.maxShutter = null
                minShutterMenu.setText(null, false)
                maxShutterMenu.setText(null, false)
            }
        }

        binding.clearShutterRange.setOnClickListener {
            newCamera.minShutter = null
            newCamera.maxShutter = null
            minShutterMenu.setText(null, false)
            maxShutterMenu.setText(null, false)
        }
        minShutterMenu.setSimpleItems(shutterValueOptions)
        maxShutterMenu.setSimpleItems(shutterValueOptions)
        minShutterMenu.setText(newCamera.minShutter, false)
        maxShutterMenu.setText(newCamera.maxShutter, false)

        minShutterMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                newCamera.minShutter = shutterValueOptions[position]
            } else {
                newCamera.minShutter = null
                minShutterMenu.setText(null, false)
            }
        }
        maxShutterMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                newCamera.maxShutter = shutterValueOptions[position]
            } else {
                newCamera.maxShutter = null
                maxShutterMenu.setText(null, false)
            }
        }

        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.minShutterMenu.setEndIconOnClickListener(null)
        minShutterMenu.setOnClickListener {
            val currentIndex = shutterValueOptions.indexOf(minShutterMenu.text.toString())
            if (currentIndex >= 0) minShutterMenu.listSelection = currentIndex
        }
        binding.maxShutterMenu.setEndIconOnClickListener(null)
        maxShutterMenu.setOnClickListener {
            val currentIndex = shutterValueOptions.indexOf(maxShutterMenu.text.toString())
            if (currentIndex >= 0) maxShutterMenu.listSelection = currentIndex
        }


        // EXPOSURE COMPENSATION INCREMENTS
        val exposureCompIncrementValues = resources.getStringArray(R.array.ExposureCompIncrements)
        val exposureCompIncrementMenu =
            binding.expCompIncrementsMenu.editText as MaterialAutoCompleteTextView
        exposureCompIncrementMenu.setSimpleItems(exposureCompIncrementValues)
        try {
            val text = exposureCompIncrementValues[camera.exposureCompIncrements.ordinal]
            exposureCompIncrementMenu.setText(text, false)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }
        exposureCompIncrementMenu.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            newCamera.exposureCompIncrements = PartialIncrement.from(position)
        }

        // FIXED LENS
        binding.fixedLensHelp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setMessage(R.string.FixedLensHelp)
                setPositiveButton(R.string.Close) { _: DialogInterface, _: Int -> }
            }.create().show()
        }
        newCamera.lens?.let {
            binding.fixedLensLayout.text = resources.getString(R.string.ClickToEdit)
            binding.lensClear.visibility = View.VISIBLE
        } ?: run {
            binding.fixedLensLayout.text = resources.getString(R.string.ClickToSet)
            binding.lensClear.visibility = View.INVISIBLE
        }
        binding.fixedLensLayout.setOnClickListener {
            showFixedLensFragment()
        }
        binding.lensClear.setOnClickListener {
            newCamera.lens = null
            binding.fixedLensLayout.text = resources.getString(R.string.ClickToSet)
            binding.lensClear.visibility = View.INVISIBLE
        }

        binding.topAppBar.setNavigationOnClickListener { navigateBack() }

        binding.positiveButton.setOnClickListener {
            val make = binding.makeEditText.text.toString()
            val model = binding.modelEditText.text.toString()
            val serialNumber = binding.serialNumberEditText.text.toString()

            val nameValidation = { _: Camera ->
                (make.isNotEmpty() && model.isNotEmpty()) to
                        resources.getString(R.string.MakeAndOrModelIsEmpty)
            }
            val shutterRangeValidation1 = { c: Camera ->
                (c.minShutter == null && c.maxShutter == null
                        || c.minShutter != null && c.maxShutter != null) to
                        resources.getString(R.string.NoMinOrMaxShutter)
            }
            val shutterRangeValidation2 = { c: Camera ->
                if (c.minShutter == null && c.maxShutter == null) {
                    true to ""
                } else {
                    val minIndex = shutterValueOptions.indexOf(c.minShutter)
                    val maxIndex = shutterValueOptions.indexOf(c.maxShutter)
                    (maxIndex >= minIndex) to resources.getString(R.string.MinShutterSpeedGreaterThanMax)
                }
            }
            val (validationResult, validationMessage) = newCamera.validate(
                nameValidation, shutterRangeValidation1, shutterRangeValidation2)

            if (validationResult) {
                camera.make = make
                camera.model = model
                camera.serialNumber = serialNumber
                camera.shutterIncrements = newCamera.shutterIncrements
                camera.minShutter = newCamera.minShutter
                camera.maxShutter = newCamera.maxShutter
                camera.exposureCompIncrements = newCamera.exposureCompIncrements
                camera.lens = newCamera.lens
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.CAMERA, camera)
                setFragmentResult("CameraEditFragment", bundle)
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

    private fun getShutterValueOptions() = when (newCamera.shutterIncrements) {
        Increment.THIRD -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
        Increment.HALF -> requireActivity().resources.getStringArray(R.array.ShutterValuesHalf)
        Increment.FULL -> requireActivity().resources.getStringArray(R.array.ShutterValuesFull)
    }.reversedArray()

    private fun showFixedLensFragment() {
        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(FastOutSlowInInterpolator())
            .apply { duration = 250L }
        val fragment = LensEditFragment().apply {
            sharedElementEnterTransition = sharedElementTransition
        }
        val arguments = Bundle()
        arguments.putBoolean(ExtraKeys.FIXED_LENS, true)
        newCamera.lens?.let {
            arguments.putParcelable(ExtraKeys.LENS, it)
        }
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.SetFixedLens))
        val sharedElement = binding.fixedLensLayout
        arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
        fragment.arguments = arguments

        requireParentFragment().childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(R.id.gear_fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        fragment.setFragmentResultListener("LensEditFragment") { _, bundle ->
            val lens: Lens = bundle.getParcelable(ExtraKeys.LENS)
                ?: return@setFragmentResultListener
            newCamera.lens = lens
            binding.fixedLensLayout.text = resources.getString(R.string.ClickToEdit)
            binding.lensClear.visibility = View.VISIBLE
        }
    }

}