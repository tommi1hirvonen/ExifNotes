package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Rect
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
import androidx.core.widget.NestedScrollView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.LocationPickActivity
import com.tommihirvonen.exifnotes.databinding.DialogFrameBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.utilities.*
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Runnable
import kotlin.math.roundToInt

/**
 * Dialog to edit Frame's information
 */
open class EditFrameDialog : BottomSheetDialogFragment() {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val TAG = "EditFrameDialog"
    }
    
    internal lateinit var binding: DialogFrameBinding
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

    private var complementaryPictureLoaded: Boolean = false

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
                requireActivity().runOnUiThread { setComplementaryPicture(animate = true) }
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
                    requireActivity().runOnUiThread { setComplementaryPicture(animate = true) }
                } catch (e: IOException) {
                    Toast.makeText(activity, R.string.ErrorSavingSelectedPicture, Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                Toast.makeText(activity, R.string.ErrorLocatingSelectedPicture, Toast.LENGTH_SHORT).show()
            }
        }).start()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogFrameBinding.inflate(inflater, container, false)
        binding.title.titleTextView.text = requireArguments().getString(ExtraKeys.TITLE)
        val frame1: Frame? = requireArguments().getParcelable(ExtraKeys.FRAME)
        if (frame1 == null) {
            showsDialog = false
            dismiss()
            return null
        }
        frame = frame1
        newFrame = frame.copy()

        // If the camera used for this roll is a fixed-lens camera, clear the frame's lens.
        if (newFrame.roll.camera?.isFixedLens == true) {
            newFrame.lens = null
        }

        // Get mountable lenses based on the roll's camera. If no camera was set, get all lenses.
        frame.roll.camera?.let {
            mountableLenses = database.getLinkedLenses(it).toMutableList()
        } ?: run {
            mountableLenses = database.getLenses().toMutableList()
        }

        // Set a listener to check whether the complementary picture should be loaded and displayed.
        val listener = OnScrollChangeListener(binding.nestedScrollView)
        binding.nestedScrollView.setOnScrollChangeListener(listener)

        binding.title.titleLayout.setBackgroundColor(requireContext().primaryUiColor)

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
                arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewLens))
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
                    initializeApertureSpinner()
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
        binding.dateText.text = dateTime?.dateAsText
        binding.timeText.text = dateTime?.timeAsText
        dateTimeLayoutManager = DateTimeLayoutManager(
                requireActivity(), binding.dateLayout, binding.timeLayout, binding.dateText, binding.timeText, dateTime, null
        )

        //NOTES FIELD
        binding.noteEditText.isSingleLine = false
        binding.noteEditText.setText(frame.note)
        binding.noteEditText.setSelection(binding.noteEditText.text.length)

        //COUNT BUTTON
        val frameCountValues = IntArray(100) { it + 1 }.toTypedArray()
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, frameCountValues)
                .also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.frameCountSpinner.adapter = adapter
                }
        try {
            binding.frameCountSpinner.setSelection(newFrame.count - 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //SHUTTER SPEED BUTTON
        initializeShutterSpeedSpinner()

        //APERTURE BUTTON
        initializeApertureSpinner(allowCustomValue = true)
        binding.apertureSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val aperture = parent?.adapter?.getItem(position) as String
                newFrame.aperture = if (aperture != resources.getString(R.string.NoValue)) aperture else null
            }
        }
        binding.apertureEditImageView.setOnClickListener {
            val view = requireActivity().layoutInflater.inflate(R.layout.dialog_single_decimal_edit_text, null)
            val editText = view.findViewById<EditText>(R.id.edit_text)
            try {
                val num = newFrame.aperture?.toDouble()
                num?.let { editText.setText(num.toString()) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            AlertDialog.Builder(requireContext())
                    .setView(view)
                    .setTitle(R.string.EnterCustomerApertureValue)
                    .setPositiveButton(R.string.OK) { _, _ ->
                        newFrame.aperture = editText.text.toString()
                        initializeApertureSpinner(allowCustomValue = true)
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
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, exposureCompValues)
                .also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.exposureCompSpinner.adapter = adapter
                }
        try {
            val exposureComp = frame.exposureComp
            if (exposureComp != null) binding.exposureCompSpinner.setSelection(exposureCompValues.indexOf(exposureComp))
            else binding.exposureCompSpinner.setSelection(exposureCompValues.indexOf("0"))
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        //NO OF EXPOSURES BUTTON
        try {
            binding.noOfExposuresSpinner.setSelection(newFrame.noOfExposures - 1)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
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
                    val (_, addressResult) = Geocoder.getGeocodeData(location.decimalLocation,
                            resources.getString(R.string.google_maps_key))
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
        binding.locationLayout.setOnClickListener {
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
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewFilter))
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
        binding.pictureLayout.setOnClickListener(PictureLayoutOnClickListener())

        //FLASH
        binding.flashCheckbox.isChecked = frame.flashUsed
        binding.flashLayout.setOnClickListener { binding.flashCheckbox.isChecked = !binding.flashCheckbox.isChecked }

        //LIGHT SOURCE
        try {
            binding.lightSourceSpinner.setSelection(newFrame.lightSource)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        binding.title.negativeImageView.setOnClickListener { dismiss() }
        binding.title.positiveImageView.setOnClickListener {
            commitChanges()
            val bundle = Bundle()
            bundle.putParcelable(ExtraKeys.FRAME, frame)
            setFragmentResult("EditFrameDialog", bundle)
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Check whether the complementary picture should be loaded and displayed immediately because
        // the screen can show the entire content at once and the NestedScrollView cannot be scrolled.
        dialog?.setOnShowListener {
            val scrollView = binding.nestedScrollView
            val childHeight = scrollView.getChildAt(0).height
            val isScrollable = scrollView.height < childHeight + scrollView.paddingTop + scrollView.paddingBottom
            if (!isScrollable) {
                setComplementaryPicture(animate = false)
                complementaryPictureLoaded = true
            }
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
        val shutter = binding.shutterSpeedSpinner.selectedItem as String
        frame.shutter = if (shutter != resources.getString(R.string.NoValue)) shutter else null

        frame.aperture = newFrame.aperture
        frame.count = binding.frameCountSpinner.selectedItem as Int
        frame.note = binding.noteEditText.text.toString()
        frame.date = dateTimeLayoutManager.dateTime
        frame.lens = newFrame.lens // null if the camera is a fixed-lens camera
        frame.location = newFrame.location
        frame.formattedAddress = newFrame.formattedAddress
        frame.exposureComp = binding.exposureCompSpinner.selectedItem as String
        frame.noOfExposures = (binding.noOfExposuresSpinner.selectedItem as String).toInt()
        frame.focalLength = newFrame.focalLength
        frame.pictureFilename = newFrame.pictureFilename
        frame.filters = newFrame.filters
        frame.lightSource = binding.lightSourceSpinner.selectedItemPosition
        frame.flashUsed = binding.flashCheckbox.isChecked
    }

    /**
     * Set the complementary picture ImageView with the newly selected/taken picture
     */
    private fun setComplementaryPicture(animate: Boolean) {

        // If the picture filename was not set, set text and return. Otherwise continue
        val filename = newFrame.pictureFilename
        if (filename == null) {
            binding.pictureText.setText(R.string.ClickToAdd)
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
                // width is match_parent, we get the dialog's width instead.
                // If there is a problem getting the dialog window, use the resource dimension instead.
                val targetH = dialog?.window?.decorView?.width
                        ?: resources.getDimension(R.dimen.ComplementaryPictureImageViewHeight).toInt()

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
                    if (animate) {
                        val animation = AnimationUtils.loadAnimation(activity, R.anim.fade_in_fast)
                        binding.ivPicture.startAnimation(animation)
                    }
                }
            }.start()
        } else {
            binding.pictureText.setText(R.string.PictureSetButNotFound)
        }
    }

    private fun initializeApertureSpinner(allowCustomValue: Boolean = false) {
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
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, displayedApertureValues)
                .also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.apertureSpinner.adapter = adapter
                }
        try {
            val initialValue = displayedApertureValues.indexOfFirst { it == aperture }
            if (initialValue != -1) binding.apertureSpinner.setSelection(initialValue)
            else binding.apertureSpinner.setSelection(displayedApertureValues.size - 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeShutterSpeedSpinner() {
        val displayedShutterValues = frame.roll.camera?.shutterSpeedValues(requireContext())
                ?: Camera.defaultShutterSpeedValues(requireContext())

        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, displayedShutterValues)
                .also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.shutterSpeedSpinner.adapter = adapter
                }
        try {
            val initialValue = displayedShutterValues.indexOfFirst { it == newFrame.shutter }
            if (initialValue != -1) binding.shutterSpeedSpinner.setSelection(initialValue)
            else binding.shutterSpeedSpinner.setSelection(displayedShutterValues.size - 1)
        } catch (e: Exception) {
            e.printStackTrace()
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
            newFrame.formattedAddress?.isNotEmpty() == true -> binding.locationText.text = newFrame.formattedAddress
            newFrame.location != null -> {
                binding.locationText.text = newFrame.location?.readableLocation
                        ?.replace("N ", "N\n")?.replace("S ", "S\n")
            }
            else -> {
                @SuppressLint("SetTextI18n")
                binding.locationText.text = " \n "
            }
        }
    }

    private fun updateFocalLengthTextView() {
        binding.focalLengthText.text = if (newFrame.focalLength == 0) "" else newFrame.focalLength.toString()
    }

    /**
     * Scroll change listener used to detect when the binding.pictureLayout is visible.
     * Only then will the complementary picture be loaded.
     */
    private inner class OnScrollChangeListener(
            nestedScrollView: NestedScrollView) : ScrollIndicatorNestedScrollViewListener(
            nestedScrollView, null, null) {
        override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int,
                                    oldScrollX: Int, oldScrollY: Int) {
            super.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY)
            val scrollBounds = Rect()
            v.getHitRect(scrollBounds)
            if (binding.pictureLayout.getLocalVisibleRect(scrollBounds) && !complementaryPictureLoaded) {
                setComplementaryPicture(animate = true)
                complementaryPictureLoaded = true
            }
        }
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

            val builder = AlertDialog.Builder(activity)
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
                    initializeApertureSpinner()
                    // The lens was changed, reset filters
                    resetFilters()
                } else {
                    binding.lensText.text = ""
                    newFrame.lens = null
                    newFrame.focalLength = 0
                    updateFocalLengthTextView()
                    initializeApertureSpinner()
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
                    lens?.let { database.getLinkedFilters(it) } ?: database.allFilters
            // Create a list with filter names to be shown on the multi choice dialog.
            val listItems = possibleFilters.map { it.name }.toTypedArray()
            // List where the mountable selections are stored.
            val filterSelections = possibleFilters.map {
                it to newFrame.filters.contains(it)
            }.toMutableList()
            // Bool array for preselected items in the multi choice list.
            val booleans = filterSelections.map { it.second }.toBooleanArray()

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.UsedFilter)
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
            val builder = AlertDialog.Builder(activity)
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
            val pictureActionDialogBuilder = AlertDialog.Builder(activity)

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
                        binding.pictureText.setText(R.string.ClickToAdd)
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