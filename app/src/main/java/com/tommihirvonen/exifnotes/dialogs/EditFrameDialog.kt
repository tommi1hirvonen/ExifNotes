package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.LocationPickActivity
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.utilities.GeocodingAsyncTask.AsyncResponse
import com.tommihirvonen.exifnotes.utilities.Utilities.ScrollIndicatorNestedScrollViewListener
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Dialog to edit Frame's information
 */
open class EditFrameDialog : DialogFragment() {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val TAG = "EditFrameDialog"

        /**
         * Constant passed to LocationPickActivity for result
         */
        private const val PLACE_PICKER_REQUEST = 1

        /**
         * Constant passed to EditLensDialog for result
         */
        private const val ADD_LENS = 2

        /**
         * Constant passed to EditFilterDialog for result
         */
        private const val ADD_FILTER = 3

        /**
         * Constant passed to takePictureIntent for result
         */
        private const val CAPTURE_IMAGE_REQUEST = 4

        /**
         * Constant passed to selectPictureIntent for result
         */
        private const val SELECT_PICTURE_REQUEST = 5
    }

    /**
     * Reference to the camera used to take this frame
     */
    private var camera: Camera? = null

    internal lateinit var frame: Frame

    /**
     * Holds all the lenses that can be mounted to the used camera
     */
    private var mountableLenses: MutableList<Lens>? = null

    /**
     * Reference to the singleton database
     */
    private lateinit var database: FilmDbHelper

    private lateinit var dateTimeLayoutManager: DateTimeLayoutManager

    /**
     * Button used to display the currently selected location
     */
    private lateinit var locationTextView: TextView

    /**
     * Button used to display the currently selected lens
     */
    private lateinit var lensTextView: TextView

    /**
     * Button used to display the currently selected filter
     */
    private lateinit var filtersTextView: TextView

    /**
     * Reference to the complementary picture's layout. If this view becomes visible,
     * load the complementary picture.
     */
    private lateinit var pictureLayout: LinearLayout

    /**
     * ImageView used to display complementary image taken with the phone's camera
     */
    private lateinit var pictureImageView: ImageView

    /**
     * TextView used to display text related to the complementary picture
     */
    private lateinit var pictureTextView: TextView

    /**
     * Currently selected lens
     */
    private var newLens: Lens? = null

    /**
     * Currently selected latitude longitude location in format '12,3456... 12,3456...'
     */
    private var newLocation: Location? = null

    /**
     * Currently set formatted address for location
     */
    private var newFormattedAddress: String? = null

    /**
     * Currently selected filter(s)
     */
    private lateinit var newFilters: MutableList<Filter>

    /**
     * Currently selected lens's aperture increment setting
     */
    private var apertureIncrements = 0

    /**
     * The shutter speed increment setting of the camera used
     */
    private var shutterIncrements = 0

    /**
     * The exposure compensation increment setting of the camera used
     */
    private var exposureCompIncrements = 0

    /**
     * Currently selected frame count number
     */
    private var newFrameCount = 0

    /**
     * Currently selected shutter speed value in format 1/X, Y" or B, where X and Y are numbers
     */
    private var newShutter: String? = null

    /**
     * Currently selected aperture value, number only
     */
    private var newAperture: String? = null

    /**
     * Currently selected focal length
     */
    private var newFocalLength = 0

    /**
     * Currently selected exposure compensation in format
     * 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    private var newExposureComp: String? = null

    /**
     * Currently selected number of exposures (multiple exposure)
     */
    private var newNoOfExposures = 0

    /**
     * Currently selected light source
     */
    private var newLightSource = 0

    /**
     * Currently selected filename of the complementary picture
     */
    private var newPictureFilename: String? = null

    /**
     * Used to temporarily store the possible new picture name. newPictureFilename is only set,
     * if the user presses ok in the camera activity. If the user cancels the camera activity,
     * then this member's value is ignored and newPictureFilename's value isn't changed.
     */
    private var tempPictureFilename: String? = null

    /**
     * TextView used to display the current aperture value
     */
    private lateinit var apertureTextView: TextView

    /**
     * TextView used to display the current shutter speed value
     */
    private lateinit var shutterTextView: TextView

    /**
     * TextView used to display the current frame count
     */
    private lateinit var frameCountTextView: TextView

    /**
     * TextView used to display the current exposure compensation value
     */
    private lateinit var exposureCompTextView: TextView

    /**
     * TextView used to display the current number of exposures value
     */
    private lateinit var noOfExposuresTextView: TextView

    /**
     * Button used to display the current focal length value
     */
    private lateinit var focalLengthTextView: TextView

    /**
     * CheckBox for toggling whether flash was used or not
     */
    private lateinit var flashCheckBox: CheckBox

    /**
     * TextView used to display the current light source
     */
    private lateinit var lightSourceTextView: TextView

    /**
     * Reference to the EditText used to edit notes
     */
    private lateinit var noteEditText: EditText

    /**
     * Stores the currently displayed shutter speed values.
     */
    private lateinit var displayedShutterValues: Array<String>

    /**
     * Stores the currently displayed aperture values.
     * Changes depending on the currently selected lens.
     */
    private lateinit var displayedApertureValues: Array<String>

    /**
     * Stores the currently displayed exposure compensation values.
     * Changes depending on the film's camera.
     */
    private lateinit var displayedExposureCompValues: Array<String>

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {

        // Inflate the fragment. Get the edited frame and used camera.
        // Initialize UI objects and display the frame's information.
        // Add listeners to buttons to open new dialogs to change the frame's information.
        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButton = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        frame = requireArguments().getParcelable(ExtraKeys.FRAME) ?: Frame()
        database = FilmDbHelper.getInstance(activity)

        val roll = database.getRoll(frame.rollId)
        if (roll != null) {
            val camera = database.getCamera(roll.cameraId)
            if (camera != null) {
                mountableLenses = database.getLinkedLenses(camera)
                shutterIncrements = camera.getShutterIncrements()
                exposureCompIncrements = camera.getExposureCompIncrements()
                this.camera = camera
            } else {
                mountableLenses = database.allLenses
            }
        }

        val lens = database.getLens(frame.lensId)
        if (lens != null) apertureIncrements = lens.getApertureIncrements()

        val layoutInflater = requireActivity().layoutInflater
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams")
        val inflatedView = layoutInflater.inflate(R.layout.dialog_frame, null)
        val alert = AlertDialog.Builder(activity)
        val nestedScrollView: NestedScrollView = inflatedView.findViewById(R.id.nested_scroll_view)
        val listener = OnScrollChangeListener(
                requireActivity(),
                nestedScrollView,
                inflatedView.findViewById(R.id.scrollIndicatorUp),
                inflatedView.findViewById(R.id.scrollIndicatorDown))
        nestedScrollView.setOnScrollChangeListener(listener)
        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(activity, title))
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
                    inflatedView.findViewById(R.id.divider_view10),
                    inflatedView.findViewById(R.id.divider_view11),
                    inflatedView.findViewById(R.id.divider_view12),
                    inflatedView.findViewById(R.id.divider_view13),
                    inflatedView.findViewById(R.id.divider_view14)
            ).forEach { it.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white)) }
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.add_lens)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.add_filter)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.clear_location)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
        }


        //==========================================================================================
        //LENS TEXT
        lensTextView = inflatedView.findViewById(R.id.lens_text)
        if (lens != null) lensTextView.text = lens.name else lensTextView.text = ""

        // LENS PICK DIALOG
        newLens = database.getLens(frame.lensId)
        val lensLayout = inflatedView.findViewById<LinearLayout>(R.id.lens_layout)
        lensLayout.setOnClickListener(LensLayoutOnClickListener())

        // LENS ADD DIALOG
        val addLensImageView = inflatedView.findViewById<ImageView>(R.id.add_lens)
        addLensImageView.isClickable = true
        addLensImageView.setOnClickListener {
            noteEditText.clearFocus()
            val dialog = EditLensDialog()
            dialog.setTargetFragment(this@EditFrameDialog, ADD_LENS)
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewLens))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditLensDialog.TAG)
        }


        //==========================================================================================
        // DATE & TIME PICK DIALOG
        val dateLayout = inflatedView.findViewById<LinearLayout>(R.id.date_layout)
        val timeLayout = inflatedView.findViewById<LinearLayout>(R.id.time_layout)
        val dateTextView = inflatedView.findViewById<TextView>(R.id.date_text)
        val timeTextView = inflatedView.findViewById<TextView>(R.id.time_text)
        if (frame.date == null) frame.date = DateTime.fromCurrentTime()
        val dateTime = frame.date
        dateTextView.text = dateTime?.dateAsText
        timeTextView.text = dateTime?.timeAsText
        dateTimeLayoutManager = DateTimeLayoutManager(
                requireActivity(), dateLayout, timeLayout, dateTextView, timeTextView, dateTime, null
        )


        //==========================================================================================
        //NOTES FIELD
        noteEditText = inflatedView.findViewById(R.id.note_editText)
        noteEditText.isSingleLine = false
        noteEditText.setText(frame.note)
        noteEditText.setSelection(noteEditText.text.length)


        //==========================================================================================
        //COUNT BUTTON
        newFrameCount = frame.count
        frameCountTextView = inflatedView.findViewById(R.id.frame_count_text)
        frameCountTextView.text = newFrameCount.toString()
        val frameCountLayout = inflatedView.findViewById<LinearLayout>(R.id.frame_count_layout)
        frameCountLayout.setOnClickListener(FrameCountLayoutOnClickListener())


        //==========================================================================================
        //SHUTTER SPEED BUTTON
        newShutter = frame.shutter
        shutterTextView = inflatedView.findViewById(R.id.shutter_text)
        updateShutterTextView()
        val shutterLayout = inflatedView.findViewById<LinearLayout>(R.id.shutter_layout)
        shutterLayout.setOnClickListener(ShutterLayoutOnClickListener())


        //==========================================================================================
        //APERTURE BUTTON
        newAperture = frame.aperture
        apertureTextView = inflatedView.findViewById(R.id.aperture_text)
        updateApertureTextView()
        val apertureLayout = inflatedView.findViewById<LinearLayout>(R.id.aperture_layout)
        apertureLayout.setOnClickListener(ApertureLayoutOnClickListener())


        //==========================================================================================
        //FOCAL LENGTH BUTTON
        newFocalLength = frame.focalLength
        focalLengthTextView = inflatedView.findViewById(R.id.focal_length_text)
        updateFocalLengthTextView()
        val focalLengthLayout = inflatedView.findViewById<LinearLayout>(R.id.focal_length_layout)
        focalLengthLayout.setOnClickListener(FocalLengthLayoutOnClickListener())


        //==========================================================================================
        //EXPOSURE COMP BUTTON
        newExposureComp = frame.exposureComp
        exposureCompTextView = inflatedView.findViewById(R.id.exposure_comp_text)
        exposureCompTextView.text = if (newExposureComp == null || newExposureComp == "0") "" else newExposureComp
        val exposureCompLayout = inflatedView.findViewById<LinearLayout>(R.id.exposure_comp_layout)
        exposureCompLayout.setOnClickListener(ExposureCompLayoutOnClickListener())


        //==========================================================================================
        //NO OF EXPOSURES BUTTON

        //Check that the number is bigger than zero.
        newNoOfExposures = if (frame.noOfExposures > 0) frame.noOfExposures else 1
        noOfExposuresTextView = inflatedView.findViewById(R.id.no_of_exposures_text)
        noOfExposuresTextView.text = newNoOfExposures.toString()
        val noOfExposuresLayout = inflatedView.findViewById<LinearLayout>(R.id.no_of_exposures_layout)
        noOfExposuresLayout.setOnClickListener(NoOfExposuresLayoutOnClickListener())


        //==========================================================================================
        // LOCATION PICK DIALOG
        locationTextView = inflatedView.findViewById(R.id.location_text)
        newLocation = frame.location
        newFormattedAddress = frame.formattedAddress
        updateLocationTextView()
        val locationProgressBar = inflatedView.findViewById<ProgressBar>(R.id.location_progress_bar)

        // If location is set but the formatted address is empty, try to find it
        newLocation?.let { location ->
            if (newFormattedAddress == null || newFormattedAddress?.isEmpty() == true) {
                // Make the ProgressBar visible to indicate that a query is being executed
                locationProgressBar.visibility = View.VISIBLE
                GeocodingAsyncTask(AsyncResponse { _: String?, formatted_address: String ->
                    locationProgressBar.visibility = View.INVISIBLE
                    newFormattedAddress = if (formatted_address.isNotEmpty()) formatted_address else null
                    updateLocationTextView()
                }).execute(location.decimalLocation, resources.getString(R.string.google_maps_key))

            }
        }
        val clearLocation = inflatedView.findViewById<ImageView>(R.id.clear_location)
        clearLocation.setOnClickListener {
            newLocation = null
            newFormattedAddress = null
            updateLocationTextView()
        }
        val locationLayout = inflatedView.findViewById<LinearLayout>(R.id.location_layout)
        locationLayout.setOnClickListener {
            val intent = Intent(activity, LocationPickActivity::class.java)
            intent.putExtra(ExtraKeys.LOCATION, newLocation)
            intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, newFormattedAddress)
            startActivityForResult(intent, PLACE_PICKER_REQUEST)
        }


        //==========================================================================================
        //FILTER BUTTON
        filtersTextView = inflatedView.findViewById(R.id.filter_text)
        newFilters = frame.filters.toMutableList()
        updateFiltersTextView()

        // FILTER PICK DIALOG
        val filterLayout = inflatedView.findViewById<LinearLayout>(R.id.filter_layout)
        filterLayout.setOnClickListener(FilterLayoutOnClickListener())

        // FILTER ADD DIALOG
        val addFilterImageView = inflatedView.findViewById<ImageView>(R.id.add_filter)
        addFilterImageView.isClickable = true
        addFilterImageView.setOnClickListener {
            if (newLens == null) {
                Toast.makeText(activity, resources.getString(R.string.SelectLensToAddFilters),
                        Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            noteEditText.clearFocus()
            val dialog = EditFilterDialog()
            dialog.setTargetFragment(this@EditFrameDialog, ADD_FILTER)
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewFilter))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditFilterDialog.TAG)
        }


        //==========================================================================================
        //COMPLEMENTARY PICTURE
        newPictureFilename = frame.pictureFilename
        pictureLayout = inflatedView.findViewById(R.id.picture_layout)
        pictureImageView = inflatedView.findViewById(R.id.iv_picture)
        pictureTextView = inflatedView.findViewById(R.id.picture_text)
        pictureLayout.setOnClickListener(PictureLayoutOnClickListener())


        //==========================================================================================
        //FLASH
        flashCheckBox = inflatedView.findViewById(R.id.flash_checkbox)
        flashCheckBox.isChecked = frame.flashUsed
        val flashUsedLayout = inflatedView.findViewById<View>(R.id.flash_layout)
        flashUsedLayout.setOnClickListener { flashCheckBox.isChecked = !flashCheckBox.isChecked }


        //==========================================================================================
        //LIGHT SOURCE
        newLightSource = frame.lightSource
        lightSourceTextView = inflatedView.findViewById(R.id.light_source_text)
        val lightSource: String?
        lightSource = try {
            resources.getStringArray(R.array.LightSource)[newLightSource]
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
            resources.getString(R.string.ClickToSet)
        }
        lightSourceTextView.text = if (newLightSource == 0) resources.getString(R.string.ClickToSet) else lightSource
        val lightSourceLayout = inflatedView.findViewById<LinearLayout>(R.id.light_source_layout)
        lightSourceLayout.setOnClickListener(LightSourceLayoutOnClickListener())


        //==========================================================================================
        //FINALISE BUILDING THE DIALOG
        alert.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            val intent = Intent()
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, intent)
        }
        alert.setPositiveButton(positiveButton, null)
        val dialog = alert.create()

        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        dialog.show()

        //User pressed OK, save.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(object : OnPositiveButtonClickListener(dialog) {
                    override fun onClick(view: View) {
                        super.onClick(view)
                        // Return the new entered name to the calling activity
                        val intent = Intent()
                        intent.putExtra(ExtraKeys.FRAME, frame)
                        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                    }
                })
        return dialog
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
        filtersTextView.text = newFilters.joinToString(separator = "\n") { "-${it.name}" }
    }

    /**
     * Method to finalize the member that is passed to the target fragment.
     * Also used to delete possibly unused older complementary pictures.
     */
    private fun onDialogDismiss() {
        frame.shutter = newShutter
        frame.aperture = newAperture
        frame.count = newFrameCount
        frame.note = noteEditText.text.toString()
        frame.date = dateTimeLayoutManager.dateTime
        frame.lensId = newLens?.id ?: 0
        frame.location = newLocation
        frame.formattedAddress = newFormattedAddress
        frame.exposureComp = newExposureComp
        frame.noOfExposures = newNoOfExposures
        frame.focalLength = newFocalLength
        frame.pictureFilename = newPictureFilename
        frame.filters = newFilters
        frame.lightSource = newLightSource
        frame.flashUsed = flashCheckBox.isChecked
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            // Set the location
            if (data?.hasExtra(ExtraKeys.LOCATION) == true) {
                newLocation = data.getParcelableExtra(ExtraKeys.LOCATION)
            }
            // Set the formatted address
            if (data?.hasExtra(ExtraKeys.FORMATTED_ADDRESS) == true) {
                newFormattedAddress = data.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
            }
            updateLocationTextView()
        }

        if (requestCode == ADD_LENS && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            val lens: Lens = data?.getParcelableExtra(ExtraKeys.LENS) ?: return
            lens.id = database.addLens(lens)
            camera?.let {
                database.addCameraLensLink(it, lens)
                mountableLenses?.add(lens)
            }
            lensTextView.text = lens.name
            apertureIncrements = lens.getApertureIncrements()
            checkApertureValueValidity()
            if (newFocalLength > lens.maxFocalLength) newFocalLength = lens.maxFocalLength
            else if (newFocalLength < lens.minFocalLength) newFocalLength = lens.minFocalLength
            newLens = lens
            updateFocalLengthTextView()
            resetFilters()
        }

        if (requestCode == ADD_FILTER && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            val filter: Filter = data?.getParcelableExtra(ExtraKeys.FILTER) ?: return
            filter.id = database.addFilter(filter)
            newLens?.let { database.addLensFilterLink(filter, it) }
            newFilters.add(filter)
            updateFiltersTextView()
        }

        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
            pictureTextView.setText(R.string.LoadingPicture)
            pictureTextView.visibility = View.VISIBLE
            // Decode and compress the picture on a background thread.
            Thread(Runnable {

                // The user has taken a new complementary picture. Update the possible new filename,
                // notify gallery app and set the complementary picture bitmap.
                newPictureFilename = tempPictureFilename

                // Compress the picture file
                try {
                    ComplementaryPicturesManager.compressPictureFile(activity, newPictureFilename)
                } catch (e: IOException) {
                    Toast.makeText(activity, R.string.ErrorCompressingComplementaryPicture, Toast.LENGTH_SHORT).show()
                }
                // Set the complementary picture ImageView on the UI thread.
                requireActivity().runOnUiThread { setComplementaryPicture() }
            }).start()
        }

        if (requestCode == SELECT_PICTURE_REQUEST && resultCode == Activity.RESULT_OK) {
            val selectedPictureUri = data?.data ?: return
            // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
            pictureTextView.setText(R.string.LoadingPicture)
            pictureTextView.visibility = View.VISIBLE
            // Decode and compress the selected file on a background thread.
            Thread(Runnable {

                // Create the placeholder file in the complementary pictures directory.
                val pictureFile = ComplementaryPicturesManager.createNewPictureFile(activity)
                try {
                    // Get the compressed bitmap from the Uri.
                    val pictureBitmap = ComplementaryPicturesManager
                            .getCompressedBitmap(activity, selectedPictureUri)
                    try {
                        // Save the compressed bitmap to the placeholder file.
                        ComplementaryPicturesManager.saveBitmapToFile(pictureBitmap, pictureFile)
                        // Update the member reference and set the complementary picture.
                        newPictureFilename = pictureFile.name
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

    }

    /**
     * Set the complementary picture ImageView with the newly selected/taken picture
     */
    private fun setComplementaryPicture() {

        // If the picture filename was not set, set text and return. Otherwise continue
        if (newPictureFilename == null) {
            pictureTextView.setText(R.string.ClickToAdd)
            return
        }
        val pictureFile = ComplementaryPicturesManager.getPictureFile(activity, newPictureFilename)

        // If the picture file exists, set the picture ImageView.
        if (pictureFile.exists()) {

            // Set the visibilities first, so that the views in general are displayed
            // when the user scrolls down.
            pictureTextView.visibility = View.GONE
            pictureImageView.visibility = View.VISIBLE

            // Load the bitmap on a background thread
            Thread(Runnable {

                // Get the target ImageView height.
                // Because the complementary picture ImageView uses subclass SquareImageView,
                // the ImageView width should also be its height. Because the ImageView's
                // width is match_parent, we get the dialog's width instead.
                // If there is a problem getting the dialog window, use the resource dimension instead.
                val targetH = dialog?.window?.decorView?.width ?: resources.getDimension(R.dimen.ComplementaryPictureImageViewHeight).toInt()

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
                    pictureImageView.rotation = rotation.toFloat()
                    pictureImageView.setImageBitmap(bitmap)
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.fade_in_fast)
                    pictureImageView.startAnimation(animation)
                }
            }).start()
        } else {
            pictureTextView.setText(R.string.PictureSetButNotFound)
        }
    }

    /**
     * Set the displayed aperture values depending on the lens's aperture increments
     * and its max and min aperture values. If no lens is selected, default to third stop
     * increments and don't limit the aperture values from either end.
     */
    private fun setDisplayedApertureValues() {
        //Get the array of displayed aperture values according to the set increments.
        displayedApertureValues = when (apertureIncrements) {
            0 -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
            1 -> requireActivity().resources.getStringArray(R.array.ApertureValuesHalf)
            2 -> requireActivity().resources.getStringArray(R.array.ApertureValuesFull)
            else -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
        }
        //Reverse the order if necessary.
        if (displayedApertureValues[0] == resources.getString(R.string.NoValue)) {
            // By reversing the order we can reverse the order in the NumberPicker too
            displayedApertureValues.reverse()
        }

        // Set the min and max values only if a lens is selected and they are set for the lens.
        // Otherwise the displayedApertureValues array will be left alone
        // (all aperture values available, since min and max were not defined).
        newLens?.let { lens ->
            val minIndex = displayedApertureValues.indexOfFirst { it == lens.minAperture }
            val maxIndex = displayedApertureValues.indexOfFirst { it == lens.maxAperture }
            if (minIndex != -1 && maxIndex != -1) {
                val apertureValues = displayedApertureValues.filterIndexed { index, _ ->
                    index in minIndex..maxIndex
                }.plus(resources.getString(R.string.NoValue))
                displayedApertureValues = apertureValues.toTypedArray()
            }
        }
    }

    /**
     * Called when the shutter speed value dialog is opened.
     * Set the values for the NumberPicker
     *
     * @param shutterPicker NumberPicker associated with the shutter speed value
     */
    private fun initialiseShutterPicker(shutterPicker: NumberPicker) {
        // Set the increments according to settings
        displayedShutterValues = when (shutterIncrements) {
            0 -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
            1 -> requireActivity().resources.getStringArray(R.array.ShutterValuesHalf)
            2 -> requireActivity().resources.getStringArray(R.array.ShutterValuesFull)
            else -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
        }
        //Reverse the order if necessary
        if (displayedShutterValues[0] == resources.getString(R.string.NoValue)) {
            // By reversing the order we can reverse the order in the NumberPicker too
            displayedShutterValues.reverse()
        }


        // Set the min and max values only if a lens is selected and they are set for the lens.
        // Otherwise the displayedShutterValues array will be left alone
        // (all shutter values available, since min and max were not defined).
        camera?.let { camera ->
            val minIndex = displayedShutterValues.indexOfFirst { it == camera.minShutter }
            val maxIndex = displayedShutterValues.indexOfFirst { it == camera.maxShutter }
            displayedShutterValues = if (minIndex != -1 && maxIndex != -1) {
                val shutterValues = displayedShutterValues.filterIndexed { index, _ ->
                    index in minIndex..maxIndex
                }.plus("B").plus(resources.getString(R.string.NoValue))
                shutterValues.toTypedArray()
            } else {
                displayedShutterValues.toMutableList().also {
                    it.add(it.size - 1, "B")
                }.toTypedArray()
            }
        }

        shutterPicker.minValue = 0
        shutterPicker.maxValue = displayedShutterValues.size - 1
        shutterPicker.displayedValues = displayedShutterValues
        shutterPicker.value = displayedShutterValues.size - 1
        val initialValue = displayedShutterValues.indexOfFirst { it == newShutter }
        if (initialValue != -1) shutterPicker.value = initialValue
    }

    /**
     * Reset filters and update the filter button's text.
     */
    private fun resetFilters() {
        newFilters.clear()
        filtersTextView.text = ""
    }

    /**
     * Updates the shutter speed value TextView's text.
     */
    private fun updateShutterTextView() {
        if (newShutter == null) {
            shutterTextView.text = ""
        } else {
            shutterTextView.text = newShutter
        }
    }

    /**
     * Updates the aperture value button's text.
     */
    private fun updateApertureTextView() {
        if (newAperture == null) {
            apertureTextView.text = ""
        } else {
            val newText = "f/$newAperture"
            apertureTextView.text = newText
        }
    }

    /**
     * Updates the location button's text.
     */
    private fun updateLocationTextView() {
        when {
            newFormattedAddress?.isNotEmpty() == true -> locationTextView.text = newFormattedAddress
            newLocation != null -> {
                locationTextView.text = newLocation?.readableLocation
                        ?.replace("N ", "N\n")?.replace("S ", "S\n")
            }
            else -> {
                @SuppressLint("SetTextI18n")
                locationTextView.text = " \n "
            }
        }
    }

    /**
     * Updates the focal length TextView
     */
    private fun updateFocalLengthTextView() {
        focalLengthTextView.text = if (newFocalLength == 0) "" else newFocalLength.toString()
    }

    /**
     * When the currently selected lens is changed, check the validity of the currently
     * selected aperture value. I.e. it has to be within the new lens's aperture range.
     */
    private fun checkApertureValueValidity() {
        setDisplayedApertureValues()
        var apertureFound = false
        for (string in displayedApertureValues) {
            if (string == newAperture) {
                apertureFound = true
                break
            }
        }
        if (!apertureFound) {
            newAperture = null
            updateApertureTextView()
        }
    }

    /**
     * Class used by this class AlertDialog class and its subclasses. Implemented for positive button
     * onClick events.
     */
    internal open inner class OnPositiveButtonClickListener(private val dialog: AlertDialog) : View.OnClickListener {
        override fun onClick(view: View) {
            onDialogDismiss()
            dialog.dismiss()
        }

    }

    /**
     * Scroll change listener used to detect when the pictureLayout is visible.
     * Only then will the complementary picture be loaded.
     */
    private inner class OnScrollChangeListener internal constructor(context: Context, nestedScrollView: NestedScrollView,
                                                                    indicatorUp: View, indicatorDown: View) : ScrollIndicatorNestedScrollViewListener(context, nestedScrollView, indicatorUp, indicatorDown) {
        private var pictureLoaded = false
        override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int,
                                    oldScrollX: Int, oldScrollY: Int) {
            super.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY)
            val scrollBounds = Rect()
            v.getHitRect(scrollBounds)
            if (pictureLayout.getLocalVisibleRect(scrollBounds) && !pictureLoaded) {
                setComplementaryPicture()
                pictureLoaded = true
            }
        }
    }
    //==============================================================================================
    // LISTENER CLASSES USED TO OPEN NEW DIALOGS AFTER ONCLICK EVENTS
    /**
     * Listener class attached to shutter speed layout.
     * Opens a new dialog to display shutter speed options.
     */
    private inner class ShutterLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val shutterPicker = Utilities.fixNumberPicker(
                    dialogView.findViewById(R.id.number_picker)
            )
            initialiseShutterPicker(shutterPicker)
            shutterPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseShutterSpeed))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newShutter =
                        if (shutterPicker.value != shutterPicker.maxValue) displayedShutterValues[shutterPicker.value]
                        else null
                updateShutterTextView()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to aperture value layout.
     * Opens a new dialog to display aperture value options.
     */
    private inner class ApertureLayoutOnClickListener : View.OnClickListener {
        /**
         * Variable to denote whether a custom aperture is being used or a predefined one.
         */
        private var manualOverride = false

        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker_custom, null)
            val aperturePicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            val editText = dialogView.findViewById<EditText>(R.id.edit_text)
            val customApertureSwitch: SwitchCompat = dialogView.findViewById(R.id.custom_aperture_switch)

            // Initialise the aperture value NumberPicker. The return value is true, if the
            // aperture value corresponds to a predefined value. The return value is false,
            // if the aperture value is a custom value.
            val apertureValueMatch = initialiseAperturePicker(aperturePicker)
            aperturePicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseApertureValue))

            // If the aperture value did not match to any predefined aperture values, enable custom input.
            if (!apertureValueMatch) {
                customApertureSwitch.isChecked = true
                editText.setText(newAperture)
                aperturePicker.visibility = View.INVISIBLE
                editText.visibility = View.VISIBLE
                manualOverride = true
            } else {
                // Otherwise set manualOverride to false. This has to be done in case manual
                // override was enabled in a previous onClick event and the value of manualOverride
                // was left to true.
                manualOverride = false
            }

            // The user can switch between predefined aperture values and custom values using a switch.
            customApertureSwitch.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
                if (b) {
                    aperturePicker.visibility = View.INVISIBLE
                    editText.visibility = View.VISIBLE
                    manualOverride = true
                    val animation1 = AnimationUtils.loadAnimation(activity, R.anim.exit_to_right_alt)
                    val animation2 = AnimationUtils.loadAnimation(activity, R.anim.enter_from_left_alt)
                    aperturePicker.startAnimation(animation1)
                    editText.startAnimation(animation2)
                } else {
                    editText.visibility = View.INVISIBLE
                    aperturePicker.visibility = View.VISIBLE
                    manualOverride = false
                    val animation1 = AnimationUtils.loadAnimation(activity, R.anim.enter_from_right_alt)
                    val animation2 = AnimationUtils.loadAnimation(activity, R.anim.exit_to_left_alt)
                    aperturePicker.startAnimation(animation1)
                    editText.startAnimation(animation2)
                }
            }
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                // Use the aperture value from the NumberPicker or EditText
                // depending on whether a custom value was used.
                newAperture = if (!manualOverride) {
                    if (aperturePicker.value != aperturePicker.maxValue) displayedApertureValues[aperturePicker.value] else null
                } else {
                    val customAperture = editText.text.toString()
                    if (customAperture.isNotEmpty()) customAperture else null
                }
                updateApertureTextView()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }

        /**
         * Called when the aperture value dialog is opened.
         * Sets the values for the NumberPicker.
         *
         * @param aperturePicker NumberPicker associated with the aperture value
         * @return true if the current aperture value corresponds to some predefined aperture value,
         * false if not.
         */
        private fun initialiseAperturePicker(aperturePicker: NumberPicker): Boolean {
            setDisplayedApertureValues()
            //If no lens is selected, end here
            if (newLens == null) {
                aperturePicker.displayedValues = null
                aperturePicker.minValue = 0
                aperturePicker.maxValue = displayedApertureValues.size - 1
                aperturePicker.displayedValues = displayedApertureValues
                aperturePicker.value = displayedApertureValues.size - 1
                // Null aperture value is empty, which is a known value. Return true.
                if (newAperture == null) return true
                for (i in displayedApertureValues.indices) {
                    if (newAperture == displayedApertureValues[i]) {
                        aperturePicker.value = i
                        return true
                    }
                }
                return false
            }

            //Set the NumberPicker displayed values to null. If we set the displayed values
            //and the maxValue is smaller than the length of the new displayed values array,
            //ArrayIndexOutOfBounds is thrown.
            //Also if we set maxValue and the currently displayed values array length is smaller,
            //ArrayIndexOutOfBounds is thrown.
            //Setting displayed values to null solves this problem.
            aperturePicker.displayedValues = null
            aperturePicker.minValue = 0
            aperturePicker.maxValue = displayedApertureValues.size - 1
            aperturePicker.displayedValues = displayedApertureValues
            aperturePicker.value = displayedApertureValues.size - 1
            // Null aperture value is empty, which is a known value. Return true.
            if (newAperture == null) return true
            for (i in displayedApertureValues.indices) {
                if (newAperture == displayedApertureValues[i]) {
                    aperturePicker.value = i
                    return true
                }
            }
            return false
        }
    }

    /**
     * Listener class attached to frame count layout.
     * Opens a new dialog to display frame count options.
     */
    private inner class FrameCountLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams") val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val frameCountPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            frameCountPicker.minValue = 0
            frameCountPicker.maxValue = 100
            frameCountPicker.value = newFrameCount
            frameCountPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseFrameCount))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFrameCount = frameCountPicker.value
                frameCountTextView.text = newFrameCount.toString()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to lens layout.
     * Opens a new dialog to display lens options for current camera.
     * Check the validity of aperture value, focal length and filter after lens has been changed.
     */
    private inner class LensLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val listItems = listOf(resources.getString(R.string.NoLens))
                    .plus(mountableLenses?.map { it.name } ?: emptyList()).toTypedArray()
            val checkedItem = newLens?.let { lens ->
                mountableLenses?.indexOfFirst { it == lens }?.plus(1) ?: 0 // account for the 'No lens' option (+1)
            } ?: 0

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.UsedLens)
            builder.setSingleChoiceItems(listItems, checkedItem) { dialog: DialogInterface, which: Int ->
                // Check if the lens was changed
                if (which > 0) {
                    // Get the new lens, also account for the 'No lens' option (which - 1).
                    val lens = mountableLenses?.get(which - 1) ?: return@setSingleChoiceItems
                    lensTextView.text = lens.name
                    newLens = lens
                    if (newFocalLength > lens.maxFocalLength) {
                        newFocalLength = lens.maxFocalLength
                    } else if (newFocalLength < lens.minFocalLength) {
                        newFocalLength = lens.minFocalLength
                    }
                    focalLengthTextView.text = if (newFocalLength == 0) "" else newFocalLength.toString()
                    apertureIncrements = lens.getApertureIncrements()

                    //Check the aperture value's validity against the new lens' properties.
                    checkApertureValueValidity()
                    // The lens was changed, reset filters
                    resetFilters()
                } else {
                    lensTextView.text = ""
                    newLens = null
                    newFocalLength = 0
                    updateFocalLengthTextView()
                    apertureIncrements = 0
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
                    newLens?.let { database.getLinkedFilters(it) }
                    ?: database.allFilters
            // Create a list with filter names to be shown on the multi choice dialog.
            val listItems = possibleFilters.map { it.name }.toTypedArray()
            // List where the mountable selections are stored.
            val filterSelections = possibleFilters.map {
                it to newFilters.contains(it)
            }.toMutableList()
            // Bool array for preselected items in the multi choice list.
            val booleans = filterSelections.map { it.second }.toBooleanArray()

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.UsedFilter)
            builder.setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                filterSelections[which] = filterSelections[which].copy(second = isChecked)
            }
            builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                newFilters = filterSelections.filter { it.second }.map { it.first }.toMutableList()
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
            val focalLengthTextView = dialogView.findViewById<TextView>(R.id.value_text_view)

            // Get the min and max focal lengths
            val minValue: Int = newLens?.minFocalLength ?: 0
            val maxValue: Int = newLens?.maxFocalLength ?: 500

            // Set the SeekBar progress percent
            when {
                newFocalLength > maxValue -> {
                    focalLengthSeekBar.progress = 100
                    focalLengthTextView.text = maxValue.toString()
                }
                newFocalLength < minValue -> {
                    focalLengthSeekBar.progress = 0
                    focalLengthTextView.text = minValue.toString()
                }
                minValue == maxValue -> {
                    focalLengthSeekBar.progress = 50
                    focalLengthTextView.text = minValue.toString()
                }
                else -> {
                    focalLengthSeekBar.progress = calculateProgress(newFocalLength, minValue, maxValue)
                    focalLengthTextView.text = newFocalLength.toString()
                }
            }

            // When the user scrolls the SeekBar, change the TextView to indicate
            // the current focal length converted from the progress (int i)
            focalLengthSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    val focalLength = minValue + (maxValue - minValue) * i / 100
                    focalLengthTextView.text = focalLength.toString()
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
                var focalLength = focalLengthTextView.text.toString().toInt()
                if (focalLength < maxValue) {
                    ++focalLength
                    focalLengthSeekBar.progress = calculateProgress(focalLength, minValue, maxValue)
                    focalLengthTextView.text = focalLength.toString()
                }
            }
            decreaseFocalLength.setOnClickListener {
                var focalLength = focalLengthTextView.text.toString().toInt()
                if (focalLength > minValue) {
                    --focalLength
                    focalLengthSeekBar.progress = calculateProgress(focalLength, minValue, maxValue)
                    focalLengthTextView.text = focalLength.toString()
                }
            }
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseFocalLength))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFocalLength = focalLengthTextView.text.toString().toInt()
                updateFocalLengthTextView()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to exposure compensation layout.
     * Opens a new dialog to display exposure compensation options.
     */
    private inner class ExposureCompLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams") val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val exposureCompPicker = Utilities.fixNumberPicker(
                    dialogView.findViewById(R.id.number_picker)
            )
            displayedExposureCompValues = when (exposureCompIncrements) {
                0 -> requireActivity().resources.getStringArray(R.array.CompValues)
                1 -> requireActivity().resources.getStringArray(R.array.CompValuesHalf)
                else -> requireActivity().resources.getStringArray(R.array.CompValues)
            }
            exposureCompPicker.minValue = 0
            exposureCompPicker.maxValue = displayedExposureCompValues.size - 1
            exposureCompPicker.displayedValues = displayedExposureCompValues
            exposureCompPicker.value = floor(displayedExposureCompValues.size / 2.toDouble()).toInt()
            val initialValue = displayedExposureCompValues.indexOfFirst { it == newExposureComp }
            if (initialValue != -1) exposureCompPicker.value = initialValue
            exposureCompPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseExposureComp))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newExposureComp = displayedExposureCompValues[exposureCompPicker.value]
                exposureCompTextView.text =
                        if (newExposureComp == null || newExposureComp == "0") ""
                        else newExposureComp
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to number of exposures layout.
     * Opens a new dialog to display number of exposures options.
     */
    private inner class NoOfExposuresLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val noOfExposuresPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            noOfExposuresPicker.minValue = 1
            noOfExposuresPicker.maxValue = 10
            noOfExposuresPicker.value = 1
            if (newNoOfExposures > 1) {
                noOfExposuresPicker.value = newNoOfExposures
            }
            noOfExposuresPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseNoOfExposures))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newNoOfExposures = noOfExposuresPicker.value
                noOfExposuresTextView.text = newNoOfExposures.toString()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)
            ) { _: DialogInterface?, _: Int -> }
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
            val items: Array<String> = if (newPictureFilename == null) {
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
                        val selectPictureIntent = Intent(Intent.ACTION_PICK)
                        selectPictureIntent.type = "image/*"
                        startActivityForResult(selectPictureIntent, SELECT_PICTURE_REQUEST)
                    }
                    2 -> {
                        try {
                            ComplementaryPicturesManager.addPictureToGallery(activity, newPictureFilename)
                            Toast.makeText(activity, R.string.PictureAddedToGallery, Toast.LENGTH_SHORT).show()
                        } catch (e: IOException) {
                            Toast.makeText(activity, R.string.ErrorAddingPictureToGallery, Toast.LENGTH_LONG).show()
                        }
                        dialogInterface.dismiss()
                    }
                    3 -> rotateComplementaryPictureRight()
                    4 -> rotateComplementaryPictureLeft()
                    5 -> {
                        newPictureFilename = null
                        pictureImageView.visibility = View.GONE
                        pictureTextView.visibility = View.VISIBLE
                        pictureTextView.setText(R.string.ClickToAdd)
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
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                // Create the file where the photo should go
                val pictureFile = ComplementaryPicturesManager.createNewPictureFile(activity)
                tempPictureFilename = pictureFile.name
                val photoURI: Uri
                //Android Nougat requires that the file is given via FileProvider
                photoURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(requireContext(), requireContext().applicationContext
                            .packageName + ".provider", pictureFile)
                } else {
                    Uri.fromFile(pictureFile)
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
            }
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees clockwise and
         * set the complementary picture orientation with ComplementaryPicturesManager.
         */
        private fun rotateComplementaryPictureRight() {
            try {
                ComplementaryPicturesManager.rotatePictureRight(activity, newPictureFilename)
                pictureImageView.rotation = pictureImageView.rotation + 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_right)
                pictureImageView.startAnimation(animation)
            } catch (e: IOException) {
                Toast.makeText(activity, R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees counterclockwise and set
         * the complementary picture orientation with ComplementaryPicturesManager.
         */
        private fun rotateComplementaryPictureLeft() {
            try {
                ComplementaryPicturesManager.rotatePictureLeft(activity, newPictureFilename)
                pictureImageView.rotation = pictureImageView.rotation - 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_left)
                pictureImageView.startAnimation(animation)
            } catch (e: IOException) {
                Toast.makeText(activity, R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Listener class attached to light source layout.
     * Shows different light source options as a simple list.
     */
    private inner class LightSourceLayoutOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            val builder = AlertDialog.Builder(activity)
            val checkedItem = newLightSource
            builder.setSingleChoiceItems(R.array.LightSource, checkedItem) { dialog: DialogInterface, which: Int ->
                newLightSource = which
                lightSourceTextView.text =
                        if (newLightSource == 0) resources.getString(R.string.ClickToSet)
                        else resources.getStringArray(R.array.LightSource)[which]
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            builder.create().show()
        }
    }

}