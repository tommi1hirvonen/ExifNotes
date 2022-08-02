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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.LocationPickActivity
import com.tommihirvonen.exifnotes.databinding.FragmentEditFrameBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.dialogs.EditFilterDialog
import com.tommihirvonen.exifnotes.dialogs.EditLensDialog
import com.tommihirvonen.exifnotes.utilities.*
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Runnable
import kotlin.math.roundToInt

/**
 * Dialog to edit Frame's information
 */
open class EditFrameFragment : Fragment() {
    
    internal lateinit var binding: FragmentEditFrameBinding
        private set

    internal lateinit var frame: Frame

    private lateinit var newFrame: Frame

    private val lens get() = newFrame.roll.camera?.lens ?: newFrame.lens

    /**
     * Holds all the lenses that can be mounted to the used camera
     */
    private var mountableLenses: MutableList<Lens>? = null

    private lateinit var dateTimeLayoutManager: DateTimeLayoutManager

    /**
     * Used to temporarily store the possible new picture name. newPictureFilename is only set,
     * if the user presses ok in the camera activity. If the user cancels the camera activity,
     * then this member's value is ignored and newPictureFilename's value isn't changed.
     */
    private var tempPictureFilename: String? = null

    private val locationResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Set the location
            if (result.data?.hasExtra(ExtraKeys.LOCATION) == true) {
                newFrame.location = result.data?.getParcelableExtra(ExtraKeys.LOCATION)
            }
            // Set the formatted address
            if (result.data?.hasExtra(ExtraKeys.FORMATTED_ADDRESS) == true) {
                newFrame.formattedAddress = result.data?.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
            }
            updateLocationTextView()
        }
    }

    private val captureImageResultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
        if (result) {
            // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
            binding.pictureText.setText(R.string.LoadingPicture)
            binding.pictureText.visibility = View.VISIBLE
            // Decode and compress the picture on a background thread.
            Thread(Runnable {
                // The user has taken a new complementary picture. Update the possible new filename,
                // notify gallery app and set the complementary picture bitmap.
                val filename = tempPictureFilename ?: return@Runnable
                newFrame.pictureFilename = tempPictureFilename
                // Compress the picture file
                try {
                    ComplementaryPicturesManager.compressPictureFile(requireActivity(), filename)
                } catch (e: IOException) {
                    Toast.makeText(activity, R.string.ErrorCompressingComplementaryPicture, Toast.LENGTH_SHORT).show()
                }
                // Set the complementary picture ImageView on the UI thread.
                requireActivity().runOnUiThread { setComplementaryPicture() }
            }).start()
        }
    }

    private val selectImageResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { resultUri ->
        // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
        binding.pictureText.setText(R.string.LoadingPicture)
        binding.pictureText.visibility = View.VISIBLE
        // Decode and compress the selected file on a background thread.
        Thread(Runnable {
            // Create the placeholder file in the complementary pictures directory.
            val pictureFile = ComplementaryPicturesManager.createNewPictureFile(requireActivity())
            try {
                // Get the compressed bitmap from the Uri.
                val pictureBitmap = ComplementaryPicturesManager
                    .getCompressedBitmap(requireActivity(), resultUri) ?: return@Runnable
                try {
                    // Save the compressed bitmap to the placeholder file.
                    ComplementaryPicturesManager.saveBitmapToFile(pictureBitmap, pictureFile)
                    // Update the member reference and set the complementary picture.
                    newFrame.pictureFilename = pictureFile.name
                    // Set the complementary picture ImageView on the UI thread.
                    requireActivity().runOnUiThread { setComplementaryPicture() }
                } catch (e: IOException) {
                    Toast.makeText(activity, R.string.ErrorSavingSelectedPicture, Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                Toast.makeText(activity, R.string.ErrorLocatingSelectedPicture, Toast.LENGTH_SHORT).show()
            }
        }).start()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEditFrameBinding.inflate(inflater, container, false)
        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)
        frame = requireArguments().getParcelable(ExtraKeys.FRAME) ?: return null
        newFrame = frame.copy()

        // If the camera used for this roll is a fixed-lens camera, clear the frame's lens.
        if (newFrame.roll.camera?.isFixedLens == true) {
            newFrame.lens = null
        }

        // Get mountable lenses based on the roll's camera. If no camera was set, get all lenses.
        frame.roll.camera?.let {
            mountableLenses = database.getLinkedLenses(it).toMutableList()
        } ?: run {
            mountableLenses = database.lenses.toMutableList()
        }

        // LENS PICK DIALOG
        if (frame.roll.camera?.isNotFixedLens == true) {
            binding.lensText.text = frame.lens?.name ?: ""
            binding.lensLayout.setOnClickListener(LensLayoutOnClickListener())
            // LENS ADD DIALOG
            binding.addLens.isClickable = true
            binding.addLens.setOnClickListener {
                binding.noteEditText.clearFocus()
                val dialog = EditLensDialog(fixedLens = false)
                val arguments = Bundle()
                arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewLens))
                arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
                dialog.arguments = arguments
                dialog.show(parentFragmentManager.beginTransaction(), EditLensDialog.TAG)
                dialog.setFragmentResultListener("EditLensDialog") { _, bundle ->
                    val lens: Lens = bundle.getParcelable(ExtraKeys.LENS)
                        ?: return@setFragmentResultListener
                    database.addLens(lens)
                    frame.roll.camera?.let {
                        database.addCameraLensLink(it, lens)
                        mountableLenses?.add(lens)
                    }
                    binding.lensText.text = lens.name
                    initializeApertureMenu()
                    if (newFrame.focalLength > lens.maxFocalLength) newFrame.focalLength = lens.maxFocalLength
                    else if (newFrame.focalLength < lens.minFocalLength) newFrame.focalLength = lens.minFocalLength
                    newFrame.lens = lens
                    updateFocalLengthTextView()
                    resetFilters()
                }
            }
        } else {
            binding.lensLayout.visibility = View.GONE
        }


        // DATE & TIME PICK DIALOG
        if (frame.date == null) frame.date = DateTime.fromCurrentTime()
        val dateTime = frame.date
        dateTimeLayoutManager = DateTimeLayoutManager(
                requireActivity(), binding.dateLayout, dateTime, null
        )

        //NOTES FIELD
        binding.noteEditText.isSingleLine = false
        binding.noteEditText.setText(frame.note)
        binding.noteEditText.setSelection(binding.noteEditText.text?.length ?: 0)

        //COUNT BUTTON
        val frameCountValues = IntArray(100) { it + 1 }.map { it.toString() }.toTypedArray()
        val frameCountMenu = binding.frameCountMenu.editText as MaterialAutoCompleteTextView
        frameCountMenu.setSimpleItems(frameCountValues)
        frameCountMenu.setText(newFrame.count.toString(), false)
        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.frameCountMenu.setEndIconOnClickListener(null)
        frameCountMenu.setOnClickListener {
            frameCountMenu.listSelection = frameCountMenu.text.toString().toInt() - 1
        }

        //SHUTTER SPEED BUTTON
        initializeShutterSpeedMenu()

        //APERTURE BUTTON
        initializeApertureMenu(allowCustomValue = true)
        binding.apertureEditImageView.setOnClickListener {
            val view = requireActivity().layoutInflater.inflate(R.layout.dialog_single_decimal_edit_text, null)
            val editText = view.findViewById<EditText>(R.id.edit_text)
            try {
                val num = newFrame.aperture?.toDouble()
                num?.let { editText.setText(num.toString()) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            MaterialAlertDialogBuilder(requireContext())
                    .setView(view)
                    .setTitle(R.string.EnterCustomerApertureValue)
                    .setPositiveButton(R.string.OK) { _, _ ->
                        newFrame.aperture = editText.text.toString()
                        initializeApertureMenu(allowCustomValue = true)
                    }
                    .setNegativeButton(R.string.Cancel) { _, _ -> /*Do nothing*/ }
                    .create()
                    .show()
            editText.requestFocus()
        }

        //FOCAL LENGTH BUTTON
        updateFocalLengthTextView()
        binding.focalLengthLayout.setOnClickListener(FocalLengthLayoutOnClickListener())

        //EXPOSURE COMP BUTTON
        val exposureCompValues = frame.roll.camera?.exposureCompValues(requireContext())
                ?: Camera.defaultExposureCompValues(requireContext())
        val exposureCompMenu = binding.exposureCompMenu.editText as MaterialAutoCompleteTextView
        exposureCompMenu.setSimpleItems(exposureCompValues)
        exposureCompMenu.setText(newFrame.exposureComp, false)
        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.exposureCompMenu.setEndIconOnClickListener(null)
        exposureCompMenu.setOnClickListener {
            val currentIndex = exposureCompValues.indexOf(newFrame.exposureComp ?: "0")
            if (currentIndex >= 0) exposureCompMenu.listSelection = currentIndex
        }

        //NO OF EXPOSURES BUTTON
        val noOfExposuresValues = IntArray(10) { it + 1 }.map { it.toString() }.toTypedArray()
        val noOfExposuresMenu = binding.noOfExposuresMenu.editText as MaterialAutoCompleteTextView
        noOfExposuresMenu.setSimpleItems(noOfExposuresValues)
        noOfExposuresMenu.setText(newFrame.noOfExposures.toString(), false)
        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.noOfExposuresMenu.setEndIconOnClickListener(null)
        noOfExposuresMenu.setOnClickListener {
            val currentIndex = noOfExposuresValues.indexOf(newFrame.noOfExposures.toString())
            if (currentIndex >= 0) noOfExposuresMenu.listSelection = currentIndex
        }

        // LOCATION PICK DIALOG
        updateLocationTextView()
        // If location is set but the formatted address is empty, try to find it
        newFrame.location?.let { location ->
            if (newFrame.formattedAddress == null || newFrame.formattedAddress?.isEmpty() == true) {
                // Make the ProgressBar visible to indicate that a query is being executed
                binding.locationProgressBar.visibility = View.VISIBLE
                // Start a coroutine to asynchronously fetch the formatted address.
                viewLifecycleOwner.lifecycleScope.launch {
                    val (_, addressResult) = Geocoder(requireContext()).getData(location.decimalLocation)
                    newFrame.formattedAddress = addressResult.ifEmpty { null }
                    binding.locationProgressBar.visibility = View.INVISIBLE
                    updateLocationTextView()
                }
            }
        }
        binding.clearLocation.setOnClickListener {
            newFrame.location = null
            newFrame.formattedAddress = null
            updateLocationTextView()
        }
        binding.locationButton.setOnClickListener {
            val intent = Intent(activity, LocationPickActivity::class.java)
            intent.putExtra(ExtraKeys.LOCATION, newFrame.location)
            intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, newFrame.formattedAddress)
            locationResultLauncher.launch(intent)
        }

        // FILTER PICK DIALOG
        updateFiltersTextView()
        binding.filterLayout.setOnClickListener(FilterLayoutOnClickListener())

        // FILTER ADD DIALOG
        binding.addFilter.isClickable = true
        binding.addFilter.setOnClickListener {
            binding.noteEditText.clearFocus()
            val dialog = EditFilterDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilter))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditFilterDialog.TAG)
            dialog.setFragmentResultListener("EditFilterDialog") { _, bundle ->
                val filter: Filter = bundle.getParcelable(ExtraKeys.FILTER)
                    ?: return@setFragmentResultListener
                database.addFilter(filter)
                lens?.let { database.addLensFilterLink(filter, it) }
                newFrame.filters.add(filter)
                updateFiltersTextView()
            }
        }

        //COMPLEMENTARY PICTURE
        binding.complementaryPicturesOptionsButton
            .setOnClickListener(PictureLayoutOnClickListener())

        //FLASH
        binding.flashCheckbox.isChecked = frame.flashUsed

        //LIGHT SOURCE
        val lightSourceValues = resources.getStringArray(R.array.LightSource)
        val lightSourceMenu = binding.lightSourceMenu.editText as MaterialAutoCompleteTextView
        lightSourceMenu.setSimpleItems(lightSourceValues)
        try {
            lightSourceMenu.setText(lightSourceValues[newFrame.lightSource], false)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }


        binding.topAppBar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        binding.positiveButton.setOnClickListener {
            commitChanges()
            val bundle = Bundle()
            bundle.putParcelable(ExtraKeys.FRAME, frame)
            setFragmentResult("EditFrameDialog", bundle)
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        // Start the transition once all views have been measured and laid out.
        (view.parent as ViewGroup).doOnPreDraw {
            startPostponedEnterTransition()
            setComplementaryPicture()
        }
    }

    /**
     * Calculates progress as integer ranging from 0 to 100
     * from current focal length, minimum focal length and maximum focal length.
     * Return 0 if current focal length equals minimum focal length, 100 if current
     * focal length equals maximum focal length, 50 if current focal length is midway
     * in the focal length range and so on...
     *
     * @param focalLength current focal length value
     * @param minValue minimum focal length value
     * @param maxValue maximum focal length value
     * @return integer from 0 to 100 describing progress
     */
    private fun calculateProgress(focalLength: Int, minValue: Int, maxValue: Int): Int {
        // progress = (newFocalLength - minValue) / (maxValue - minValue) * 100
        val result1 = focalLength - minValue.toDouble()
        val result2 = maxValue - minValue.toDouble()
        // No variables of type int can be used if parts of the calculation
        // result in fractions.
        val progressDouble = result1 / result2 * 100
        return progressDouble.roundToInt()
    }

    /**
     * Updates the filters TextView
     */
    private fun updateFiltersTextView() {
        binding.filterText.text = newFrame.filters.joinToString(separator = "\n") { "-${it.name}" }
    }

    internal fun commitChanges() {
        val shutter = binding.shutterSpeedMenu.editText?.text.toString().ifEmpty { null }
        frame.shutter = if (shutter != resources.getString(R.string.NoValue)) shutter else null

        val aperture = binding.apertureMenu.editText?.text.toString().ifEmpty { null }
        frame.aperture = if (aperture != resources.getString(R.string.NoValue)) aperture else null

        frame.count = binding.frameCountMenu.editText?.text.toString().toInt()
        frame.note = binding.noteEditText.text.toString()
        frame.date = dateTimeLayoutManager.dateTime
        frame.lens = newFrame.lens // null if the camera is a fixed-lens camera
        frame.location = newFrame.location
        frame.formattedAddress = newFrame.formattedAddress
        frame.exposureComp = binding.exposureCompMenu.editText?.text.toString().ifEmpty { null }
        frame.noOfExposures = binding.noOfExposuresMenu.editText?.text.toString().toInt()
        frame.focalLength = newFrame.focalLength
        frame.pictureFilename = newFrame.pictureFilename
        frame.filters = newFrame.filters
        frame.flashUsed = binding.flashCheckbox.isChecked

        val lightSourceValues = resources.getStringArray(R.array.LightSource)
        val lightSourceIndex = lightSourceValues.indexOf(binding.lightSourceMenu.editText?.text.toString())
        frame.lightSource = if (lightSourceIndex >= 0) lightSourceIndex else 0
    }

    /**
     * Set the complementary picture ImageView with the newly selected/taken picture
     */
    private fun setComplementaryPicture() {

        // If the picture filename was not set, set text and return. Otherwise continue
        val filename = newFrame.pictureFilename
        if (filename == null) {
            binding.pictureText.text = null
            return
        }
        val pictureFile = ComplementaryPicturesManager.getPictureFile(requireActivity(), filename)

        // If the picture file exists, set the picture ImageView.
        if (pictureFile.exists()) {

            // Set the visibilities first, so that the views in general are displayed
            // when the user scrolls down.
            binding.pictureText.visibility = View.GONE
            binding.ivPicture.visibility = View.VISIBLE

            // Load the bitmap on a background thread
            Thread {

                // Get the target ImageView height.
                // Because the complementary picture ImageView uses subclass SquareImageView,
                // the ImageView width should also be its height. Because the ImageView's
                // width is match_parent, we get the Fragment's width instead.
                // If there is a problem getting the Fragment view, use the resource dimension instead.
                val targetH = view?.width ?: resources.getDimension(R.dimen.ComplementaryPictureImageViewHeight).toInt()

                // Rotate the complementary picture ImageView if necessary
                var rotationTemp = 0
                try {
                    val exifInterface = ExifInterface(pictureFile.absolutePath)
                    val orientation = exifInterface
                            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotationTemp = 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotationTemp = 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotationTemp = 270
                    }
                } catch (ignore: IOException) {
                }
                val rotation = rotationTemp

                // Get the dimensions of the bitmap
                val options = BitmapFactory.Options()
                // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
                // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(pictureFile.absolutePath, options)
                val photoH = options.outHeight

                // Determine how much to scale down the image
                val scale = photoH / targetH

                // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
                options.inJustDecodeBounds = false
                options.inSampleSize = scale

                // Decode the image file into a Bitmap sized to fill the view
                val bitmap = BitmapFactory.decodeFile(pictureFile.absolutePath, options)

                // Do UI changes on the UI thread.
                requireActivity().runOnUiThread {
                    binding.ivPicture.rotation = rotation.toFloat()
                    binding.ivPicture.setImageBitmap(bitmap)
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.fade_in_fast)
                    binding.ivPicture.startAnimation(animation)
                }
            }.start()
        } else {
            binding.pictureText.setText(R.string.PictureSetButNotFound)
        }
    }

    private fun initializeApertureMenu(allowCustomValue: Boolean = false) {
        val aperture = newFrame.aperture
        val displayedApertureValues = (
                lens?.apertureValues(requireContext())
                ?: Lens.defaultApertureValues(requireContext())
                ).let {
                    // If a custom aperture value is set and it's not included in the list,
                    // add it to the list.
                    if (aperture != null && !it.contains(aperture) && allowCustomValue) {
                        it.plus(aperture)
                    } else {
                        it
                    }
                }
        val autoComplete = binding.apertureMenu.editText as MaterialAutoCompleteTextView
        autoComplete.setSimpleItems(displayedApertureValues)
        autoComplete.setText(aperture, false)
        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.apertureMenu.setEndIconOnClickListener(null)
        autoComplete.setOnClickListener {
            val currentIndex = displayedApertureValues.indexOf(autoComplete.text.toString())
            if (currentIndex >= 0) autoComplete.listSelection = currentIndex
        }
    }

    private fun initializeShutterSpeedMenu() {
        val displayedShutterValues = frame.roll.camera?.shutterSpeedValues(requireContext())
                ?: Camera.defaultShutterSpeedValues(requireContext())
        val autoComplete = binding.shutterSpeedMenu.editText as MaterialAutoCompleteTextView
        autoComplete.setSimpleItems(displayedShutterValues)
        autoComplete.setText(newFrame.shutter, false)
        // The end icon of TextInputLayout can be used to toggle the menu open/closed.
        // However in that case, the AutoCompleteTextView onClick method is not called.
        // By setting the endIconOnClickListener to null onClick events are propagated
        // to AutoCompleteTextView. This way we can force the preselection of the current item.
        binding.shutterSpeedMenu.setEndIconOnClickListener(null)
        autoComplete.setOnClickListener {
            val currentIndex = displayedShutterValues.indexOf(autoComplete.text.toString())
            if (currentIndex >= 0) autoComplete.listSelection = currentIndex
        }
    }

    /**
     * Reset filters and update the filter button's text.
     */
    private fun resetFilters() {
        newFrame.filters.clear()
        binding.filterText.text = ""
    }

    /**
     * Updates the location button's text.
     */
    private fun updateLocationTextView() {
        when {
            newFrame.formattedAddress?.isNotEmpty() == true -> binding.locationButton.text = newFrame.formattedAddress
            newFrame.location != null -> {
                binding.locationButton.text = newFrame.location?.readableLocation
                        ?.replace("N ", "N\n")?.replace("S ", "S\n")
            }
            else -> {
                binding.locationButton.text = null
            }
        }
    }

    private fun updateFocalLengthTextView() {
        binding.focalLengthText.text = if (newFrame.focalLength == 0) "" else newFrame.focalLength.toString()
    }

    // LISTENER CLASSES USED TO OPEN NEW DIALOGS AFTER ONCLICK EVENTS
    /**
     * Listener class attached to lens layout.
     * Opens a new dialog to display lens options for current camera.
     * Check the validity of aperture value, focal length and filter after lens has been changed.
     */
    private inner class LensLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val listItems = listOf(resources.getString(R.string.NoLens))
                    .plus(mountableLenses?.map { it.name } ?: emptyList()).toTypedArray()
            val checkedItem = newFrame.lens?.let { lens ->
                mountableLenses?.indexOfFirst { it == lens }?.plus(1) ?: 0 // account for the 'No lens' option (+1)
            } ?: 0

            val builder = MaterialAlertDialogBuilder(requireActivity())
            builder.setTitle(R.string.UsedLens)
            builder.setSingleChoiceItems(listItems, checkedItem) { dialog: DialogInterface, which: Int ->
                // Check if the lens was changed
                if (which > 0) {
                    // Get the new lens, also account for the 'No lens' option (which - 1).
                    val lens = mountableLenses?.get(which - 1) ?: return@setSingleChoiceItems
                    binding.lensText.text = lens.name
                    newFrame.lens = lens
                    if (newFrame.focalLength > lens.maxFocalLength) {
                        newFrame.focalLength = lens.maxFocalLength
                    } else if (newFrame.focalLength < lens.minFocalLength) {
                        newFrame.focalLength = lens.minFocalLength
                    }
                    binding.focalLengthText.text = if (newFrame.focalLength == 0) "" else newFrame.focalLength.toString()

                    //Check the aperture value's validity against the new lens' properties.
                    initializeApertureMenu()
                    // The lens was changed, reset filters
                    resetFilters()
                } else {
                    binding.lensText.text = ""
                    newFrame.lens = null
                    newFrame.focalLength = 0
                    updateFocalLengthTextView()
                    initializeApertureMenu()
                    resetFilters()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            val alert = builder.create()
            alert.show()
        }
    }

    /**
     * Listener class attached to filter layout.
     * Opens a new dialog to display filter options for current lens.
     */
    private inner class FilterLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            // Get list of possible filters that can be selected.
            // If current lens is defined, use that to get linked filters.
            // Otherwise get all filters from database.
            val possibleFilters: List<Filter> =
                    lens?.let { database.getLinkedFilters(it) } ?: database.filters
            // Create a list with filter names to be shown on the multi choice dialog.
            val listItems = possibleFilters.map { it.name }.toTypedArray()
            // List where the mountable selections are stored.
            val filterSelections = possibleFilters.map {
                it to newFrame.filters.contains(it)
            }.toMutableList()
            // Bool array for preselected items in the multi choice list.
            val booleans = filterSelections.map { it.second }.toBooleanArray()

            val builder = MaterialAlertDialogBuilder(requireActivity())
            builder.setTitle(R.string.UsedFilters)
            builder.setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                filterSelections[which] = filterSelections[which].copy(second = isChecked)
            }
            builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                newFrame.filters = filterSelections.filter { it.second }.map { it.first }.toMutableList()
                updateFiltersTextView()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            val alert = builder.create()
            alert.show()
        }
    }

    /**
     * Listener class attached to focal length layout.
     * Opens a new dialog to display focal length options.
     */
    private inner class FocalLengthLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_seek_bar, null)
            val focalLengthSeekBar = dialogView.findViewById<SeekBar>(R.id.seek_bar)
            val focalLengthText = dialogView.findViewById<TextView>(R.id.value_text_view)

            // Get the min and max focal lengths
            val minValue: Int = lens?.minFocalLength ?: 0
            val maxValue: Int = lens?.maxFocalLength ?: 500

            // Set the SeekBar progress percent
            when {
                newFrame.focalLength > maxValue -> {
                    focalLengthSeekBar.progress = 100
                    focalLengthText.text = maxValue.toString()
                }
                newFrame.focalLength < minValue -> {
                    focalLengthSeekBar.progress = 0
                    focalLengthText.text = minValue.toString()
                }
                minValue == maxValue -> {
                    focalLengthSeekBar.progress = 50
                    focalLengthText.text = minValue.toString()
                }
                else -> {
                    focalLengthSeekBar.progress = calculateProgress(newFrame.focalLength, minValue, maxValue)
                    focalLengthText.text = newFrame.focalLength.toString()
                }
            }

            // When the user scrolls the SeekBar, change the TextView to indicate
            // the current focal length converted from the progress (int i)
            focalLengthSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    val focalLength = minValue + (maxValue - minValue) * i / 100
                    focalLengthText.text = focalLength.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // Do nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // Do nothing
                }
            })
            val increaseFocalLength = dialogView.findViewById<TextView>(R.id.increase_focal_length)
            val decreaseFocalLength = dialogView.findViewById<TextView>(R.id.decrease_focal_length)
            increaseFocalLength.setOnClickListener {
                var focalLength = focalLengthText.text.toString().toInt()
                if (focalLength < maxValue) {
                    ++focalLength
                    focalLengthSeekBar.progress = calculateProgress(focalLength, minValue, maxValue)
                    focalLengthText.text = focalLength.toString()
                }
            }
            decreaseFocalLength.setOnClickListener {
                var focalLength = focalLengthText.text.toString().toInt()
                if (focalLength > minValue) {
                    --focalLength
                    focalLengthSeekBar.progress = calculateProgress(focalLength, minValue, maxValue)
                    focalLengthText.text = focalLength.toString()
                }
            }
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseFocalLength))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFrame.focalLength = focalLengthText.text.toString().toInt()
                updateFocalLengthTextView()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to complementary picture layout.
     * Shows various actions regarding the complementary picture.
     */
    private inner class PictureLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val pictureActionDialogBuilder = MaterialAlertDialogBuilder(requireActivity())

            // If a complementary picture was not set, set only the two first options
            val items: Array<String> = if (newFrame.pictureFilename == null) {
                arrayOf(
                        getString(R.string.TakeNewComplementaryPicture),
                        getString(R.string.SelectPictureFromGallery)
                )
            } else {
                arrayOf(
                        getString(R.string.TakeNewComplementaryPicture),
                        getString(R.string.SelectPictureFromGallery),
                        getString(R.string.AddPictureToGallery),
                        getString(R.string.RotateRight90Degrees),
                        getString(R.string.RotateLeft90Degrees),
                        getString(R.string.Clear)
                )
            }

            // Add the items and the listener
            pictureActionDialogBuilder.setItems(items) { dialogInterface: DialogInterface, i: Int ->
                when (i) {
                    0 -> {
                        dialogInterface.dismiss()
                        startPictureActivity()
                    }
                    1 -> {
                        selectImageResultLauncher.launch("image/*")
                    }
                    2 -> {
                        try {
                            ComplementaryPicturesManager.addPictureToGallery(requireActivity(), newFrame.pictureFilename)
                            Toast.makeText(activity, R.string.PictureAddedToGallery, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(activity, R.string.ErrorAddingPictureToGallery, Toast.LENGTH_LONG).show()
                        }
                        dialogInterface.dismiss()
                    }
                    3 -> rotateComplementaryPictureRight()
                    4 -> rotateComplementaryPictureLeft()
                    5 -> {
                        newFrame.pictureFilename = null
                        binding.ivPicture.visibility = View.GONE
                        binding.pictureText.visibility = View.VISIBLE
                        binding.pictureText.text = null
                        dialogInterface.dismiss()
                    }
                }
            }
            pictureActionDialogBuilder.setNegativeButton(R.string.Cancel) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            pictureActionDialogBuilder.create().show()
        }

        /**
         * Starts a camera activity to take a new complementary picture
         */
        private fun startPictureActivity() {
            // Check if the camera feature is available
            if (!requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(activity, R.string.NoCameraFeatureWasFound, Toast.LENGTH_SHORT).show()
                return
            }

            // Advance with taking the picture

            // Create the file where the photo should go
            val pictureFile = ComplementaryPicturesManager.createNewPictureFile(requireActivity())
            tempPictureFilename = pictureFile.name
            //Android Nougat requires that the file is given via FileProvider
            val photoURI: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(requireContext(), requireContext().applicationContext
                        .packageName + ".provider", pictureFile)
            } else {
                Uri.fromFile(pictureFile)
            }
            captureImageResultLauncher.launch(photoURI)
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees clockwise and
         * set the complementary picture orientation with ComplementaryPicturesManager.
         */
        private fun rotateComplementaryPictureRight() {
            val filename = newFrame.pictureFilename ?: return
            try {
                ComplementaryPicturesManager.rotatePictureRight(requireActivity(), filename)
                binding.ivPicture.rotation = binding.ivPicture.rotation + 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_right)
                binding.ivPicture.startAnimation(animation)
            } catch (e: IOException) {
                Toast.makeText(activity, R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees counterclockwise and set
         * the complementary picture orientation with ComplementaryPicturesManager.
         */
        private fun rotateComplementaryPictureLeft() {
            val filename = newFrame.pictureFilename ?: return
            try {
                ComplementaryPicturesManager.rotatePictureLeft(requireActivity(), filename)
                binding.ivPicture.rotation = binding.ivPicture.rotation - 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_left)
                binding.ivPicture.startAnimation(animation)
            } catch (e: IOException) {
                Toast.makeText(activity, R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show()
            }
        }
    }

}