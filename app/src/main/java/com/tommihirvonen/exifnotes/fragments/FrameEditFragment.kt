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
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.LocationPickActivity
import com.tommihirvonen.exifnotes.databinding.DialogSingleEditTextBinding
import com.tommihirvonen.exifnotes.databinding.FragmentFrameEditBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.dialogs.FilterEditDialog
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.FrameEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.FrameEditViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Dialog to edit Frame's information
 */
class FrameEditFragment : Fragment() {

    companion object {
        const val TAG = "FRAME_EDIT_FRAGMENT"
        const val REQUEST_KEY = TAG
    }
    
    private lateinit var binding: FragmentFrameEditBinding

    private val backStackName by lazy {
        requireArguments().getString(ExtraKeys.BACKSTACK_NAME)
    }

    private val fragmentContainerId by lazy {
        requireArguments().getInt(ExtraKeys.FRAGMENT_CONTAINER_ID)
    }

    private val frame by lazy {
        requireArguments().parcelable<Frame>(ExtraKeys.FRAME)
            ?: throw IllegalArgumentException("Frame is a required argument for fragment FrameEditFragment")
    }

    private val model by lazy {
        val factory = FrameEditViewModelFactory(requireActivity().application, frame.copy())
        ViewModelProvider(this, factory)[FrameEditViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireParentFragment().childFragmentManager.popBackStack()
        }

        val addFilterDialog = requireParentFragment().childFragmentManager
            .findFragmentByTag(FilterEditDialog.TAG)
        addFilterDialog?.setFragmentResultListener(FilterEditDialog.REQUEST_KEY) { _, bundle ->
            bundle.parcelable<Filter>(ExtraKeys.FILTER)?.let(model::addFilter)
        }

        val addLensFragment = requireParentFragment().childFragmentManager
            .findFragmentByTag(LensEditFragment.TAG)
        addLensFragment?.setFragmentResultListener(LensEditFragment.REQUEST_KEY) { _, bundle ->
            bundle.parcelable<Lens>(ExtraKeys.LENS)?.let(model::addLens)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFrameEditBinding.inflate(inflater, container, false)
        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)
        binding.topAppBar.setNavigationOnClickListener {
            requireParentFragment().childFragmentManager.popBackStack()
        }
        binding.viewmodel = model.observable
        binding.addLens.setOnClickListener { showNewLensFragment() }

        DateTimeLayoutManager(
            requireActivity() as AppCompatActivity,
            binding.dateLayout,
            model.frame::date,
            model.observable::setDate)

        binding.apertureEditButton.setOnClickListener {
            val view = requireActivity().layoutInflater.inflate(R.layout.dialog_single_decimal_edit_text, null)
            val editText = view.findViewById<EditText>(R.id.edit_text)
            try {
                val num = model.frame.aperture?.toDouble()
                num?.let { editText.setText(num.toString()) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            MaterialAlertDialogBuilder(requireContext())
                    .setView(view)
                    .setTitle(R.string.EnterCustomerApertureValue)
                    .setPositiveButton(R.string.OK) { _, _ ->
                        model.observable.setAperture(editText.text.toString())
                    }
                    .setNegativeButton(R.string.Cancel) { _, _ -> /*Do nothing*/ }
                    .create()
                    .show()
            editText.requestFocus()
        }

        binding.shutterEditButton.setOnClickListener {
            val customShutterBinding = DialogSingleEditTextBinding.inflate(layoutInflater)
            customShutterBinding.textView.text = resources.getString(R.string.AllowedFormatsCustomShutterValue)
            val regexInteger = "[1-9]+[0-9]*\\.?".toRegex()
            val regexDecimal = "[1-9]+[0-9]*(?:\\.[0-9]+)?".toRegex()
            val regexFractionPartial = "1/".toRegex()
            val regexFraction = "1/[1-9]+[0-9]*".toRegex()
            customShutterBinding.editText.filters = arrayOf(object : InputFilter {
                override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned?,
                                    dstart: Int, dend: Int): CharSequence? {
                    val sourceString = source.toString()
                    val destString = dest.toString()
                    val text = destString.substring(0, dstart) +
                            sourceString.substring(start, end) +
                            destString.substring(dend)
                    val regexes = arrayOf(regexInteger, regexDecimal, regexFractionPartial, regexFraction)
                    val anyMatches = regexes.any {
                        val result = it.matches(text)
                        result
                    }
                    return if (anyMatches) null else ""
                }
            })
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(customShutterBinding.root)
                .setTitle(R.string.EnterCustomShutterSpeedValue)
                .setPositiveButton(R.string.OK) { _, _ -> }
                .setNegativeButton(R.string.Cancel) { _, _ -> /*Do nothing*/ }
                .create()
            dialog.setOnShowListener {
                dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener shutterPositiveOnClick@ {
                    val value = customShutterBinding.editText.text.toString()
                    if (regexInteger.matches(value)) {
                        model.observable.setShutterSpeed("${value.replace(".", "")}\"")
                    } else if (regexDecimal.matches(value)) {
                        model.observable.setShutterSpeed("$value\"")
                    } else if (regexFraction.matches(value)) {
                        model.observable.setShutterSpeed(value)
                    } else {
                        Toast.makeText(requireContext(), R.string.IncorrectValueFormat, Toast.LENGTH_SHORT).show()
                        return@shutterPositiveOnClick
                    }
                    dialog.dismiss()
                }
            }
            dialog.show()
        }

        binding.focalLengthButton.setOnClickListener(focalLengthButtonOnClickListener)

        binding.locationButton.setOnClickListener {
            val intent = Intent(activity, LocationPickActivity::class.java)
            intent.putExtra(ExtraKeys.LOCATION, model.frame.location)
            intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, model.frame.formattedAddress)
            locationResultLauncher.launch(intent)
        }

        binding.filtersButton.setOnClickListener(filtersButtonOnClickListener)

        binding.addFilter.setOnClickListener {
            binding.noteEditText.clearFocus()
            val dialog = FilterEditDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilter))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            val transaction = requireParentFragment().childFragmentManager
                .beginTransaction()
                .addToBackStack(backStackName)
            dialog.show(transaction, FilterEditDialog.TAG)
            dialog.setFragmentResultListener(FilterEditDialog.REQUEST_KEY) { _, bundle ->
                bundle.parcelable<Filter>(ExtraKeys.FILTER)?.let(model::addFilter)
            }
        }

        binding.complementaryPicturesOptionsButton
            .setOnClickListener(ComplementaryPictureOptionsListener())

        binding.positiveButton.setOnClickListener {
            if (model.validate()) {
                val bundle = Bundle().apply {
                    putParcelable(ExtraKeys.FRAME, model.frame)
                }
                setFragmentResult(REQUEST_KEY, bundle)
                requireParentFragment().childFragmentManager.popBackStack()
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

    override fun onResume() {
        super.onResume()
        // On some devices the fragment is recreated when returning from the camera activity
        // (when taking a new complementary picture) and on some devices it is simply resumed.
        // Handle both cases here.
        binding.root.doOnPreDraw {
            setComplementaryPicture()
        }
    }

    private val locationResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val location = if (result.data?.hasExtra(ExtraKeys.LOCATION) == true) {
                result.data?.parcelable<LatLng>(ExtraKeys.LOCATION)
            } else {
                null
            }
            val formattedAddress = if (result.data?.hasExtra(ExtraKeys.FORMATTED_ADDRESS) == true) {
                result.data?.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
            } else {
                null
            }
            model.observable.setLocation(location, formattedAddress)
        }
    }

    private val captureImageResultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
        // This callback is called before the Fragment is resumed.
        // Handle the complementary picture here but update the UI element in onResume().
        if (result) {
            // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
            binding.pictureText.setText(R.string.LoadingPicture)
            binding.pictureText.visibility = View.VISIBLE
            // Decode and compress the picture on a background thread.
            lifecycleScope.launch(Dispatchers.IO) {
                // The user has taken a new complementary picture. Update the new filename.
                val filename = model.pictureFilename ?: return@launch
                model.frame.pictureFilename = filename
                // Compress the picture file.
                try {
                    ComplementaryPicturesManager.compressPictureFile(requireActivity(), filename)
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        binding.root.snackbar(R.string.ErrorCompressingComplementaryPicture)
                    }
                }
            }
        }
    }

    private val selectImageResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { resultUri ->
        if (resultUri == null) { // Selecting an image from gallery was canceled.
            return@registerForActivityResult
        }
        // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
        binding.pictureText.setText(R.string.LoadingPicture)
        binding.pictureText.visibility = View.VISIBLE
        // Decode and compress the selected file on a background thread.
        lifecycleScope.launch(Dispatchers.IO) {
            // Create the placeholder file in the complementary pictures directory.
            val pictureFile = ComplementaryPicturesManager.createNewPictureFile(requireActivity())
            try {
                // Get the compressed bitmap from the Uri.
                val pictureBitmap = ComplementaryPicturesManager
                    .getCompressedBitmap(requireActivity(), resultUri) ?: return@launch
                try {
                    // Save the compressed bitmap to the placeholder file.
                    ComplementaryPicturesManager.saveBitmapToFile(pictureBitmap, pictureFile)
                    // Update the member reference and set the complementary picture.
                    model.frame.pictureFilename = pictureFile.name
                    withContext(Dispatchers.Main) { setComplementaryPicture() }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        binding.root.snackbar(R.string.ErrorSavingSelectedPicture)
                    }
                }
            } catch (e: FileNotFoundException) {
                withContext(Dispatchers.Main) {
                    binding.root.snackbar(R.string.ErrorLocatingSelectedPicture)
                }
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
     * Set the complementary picture ImageView with the newly selected/taken picture
     */
    private fun setComplementaryPicture() {

        // If the picture filename was not set, set text and return. Otherwise continue
        val filename = model.frame.pictureFilename
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
            lifecycleScope.launch(Dispatchers.IO) {
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
                withContext(Dispatchers.Main) {
                    binding.ivPicture.rotation = rotation.toFloat()
                    binding.ivPicture.setImageBitmap(bitmap)
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.fade_in_fast)
                    binding.ivPicture.startAnimation(animation)
                }
            }
        } else {
            binding.pictureText.setText(R.string.PictureSetButNotFound)
        }
    }

    private fun showNewLensFragment() {
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
        arguments.putBoolean(ExtraKeys.FIXED_LENS, false)
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewLens))
        val sharedElement = binding.addLens
        arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
        fragment.arguments = arguments
        requireParentFragment().childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(fragmentContainerId, fragment, LensEditFragment.TAG)
            .addToBackStack(backStackName)
            .commit()

        fragment.setFragmentResultListener(LensEditFragment.REQUEST_KEY) { _, bundle ->
            bundle.parcelable<Lens>(ExtraKeys.LENS)?.let(model::addLens)
        }
    }

    // LISTENER CLASSES USED TO OPEN NEW DIALOGS AFTER ONCLICK EVENTS

    /**
     * Listener class attached to filter layout.
     * Opens a new dialog to display filter options for current lens.
     */
    private val filtersButtonOnClickListener = View.OnClickListener {
        val possibleFilters = model.filters
        // Create a list with filter names to be shown on the multi choice dialog.
        val listItems = possibleFilters.map(Filter::name).toTypedArray()
        // Bool array for preselected items in the multi choice list.
        val booleans = possibleFilters.map(model.frame.filters::contains).toBooleanArray()

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.UsedFilters)
        builder.setMultiChoiceItems(listItems, booleans) { _, which, isChecked ->
            booleans[which] = isChecked
        }
        builder.setPositiveButton(R.string.OK) { _, _ ->
            val selectedFilters = booleans.zip(possibleFilters)
                .mapNotNull { (selected, filter) -> if (selected) filter else null }
            model.observable.setFilters(selectedFilters)
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Listener class attached to focal length layout.
     * Opens a new dialog to display focal length options.
     */
    private val focalLengthButtonOnClickListener = View.OnClickListener {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val inflater = requireActivity().layoutInflater
        @SuppressLint("InflateParams")
        val dialogView = inflater.inflate(R.layout.dialog_seek_bar, null)
        val focalLengthSeekBar = dialogView.findViewById<SeekBar>(R.id.seek_bar)
        val focalLengthText = dialogView.findViewById<TextView>(R.id.value_text_view)

        // Get the min and max focal lengths
        val minValue: Int = model.lens?.minFocalLength ?: 0
        val maxValue: Int = model.lens?.maxFocalLength ?: 500

        // Set the SeekBar progress percent
        when {
            model.frame.focalLength > maxValue -> {
                focalLengthSeekBar.progress = 100
                focalLengthText.text = maxValue.toString()
            }
            model.frame.focalLength < minValue -> {
                focalLengthSeekBar.progress = 0
                focalLengthText.text = minValue.toString()
            }
            minValue == maxValue -> {
                focalLengthSeekBar.progress = 50
                focalLengthText.text = minValue.toString()
            }
            else -> {
                focalLengthSeekBar.progress =
                    calculateProgress(model.frame.focalLength, minValue, maxValue)
                focalLengthText.text = model.frame.focalLength.toString()
            }
        }

        // When the user scrolls the SeekBar, change the TextView to indicate
        // the current focal length converted from the progress (int i)
        focalLengthSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val focalLength = minValue + (maxValue - minValue) * i / 100
                focalLengthText.text = focalLength.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
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
        builder.setPositiveButton(resources.getString(R.string.OK)) { _, _ ->
            model.observable.setFocalLength(focalLengthText.text.toString().toInt())
        }
        builder.setNegativeButton(resources.getString(R.string.Cancel)) { _, _ -> }
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Listener class attached to complementary picture layout.
     * Shows various actions regarding the complementary picture.
     */
    private inner class ComplementaryPictureOptionsListener : View.OnClickListener {
        override fun onClick(view: View) {
            val popupMenu = PopupMenu(requireContext(), view)
            // If a complementary picture was not set, set only the two first options
            val items: Array<String> = if (model.frame.pictureFilename == null) {
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
            items.forEachIndexed { index, text ->
                popupMenu.menu.add(Menu.NONE, index, Menu.NONE, text)
            }
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> {
                        startPictureActivity()
                    }
                    1 -> {
                        selectImageResultLauncher.launch("image/*")
                    }
                    2 -> {
                        try {
                            ComplementaryPicturesManager
                                .addPictureToGallery(requireActivity(), model.frame.pictureFilename)
                            binding.root.snackbar(R.string.PictureAddedToGallery)
                        } catch (e: Exception) {
                            binding.root.snackbar(R.string.ErrorAddingPictureToGallery)
                        }
                    }
                    3 -> rotateComplementaryPictureRight()
                    4 -> rotateComplementaryPictureLeft()
                    5 -> {
                        model.frame.pictureFilename = null
                        binding.ivPicture.visibility = View.GONE
                        binding.pictureText.visibility = View.VISIBLE
                        binding.pictureText.text = null
                    }
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        /**
         * Starts a camera activity to take a new complementary picture
         */
        private fun startPictureActivity() {
            // Check if the camera feature is available
            if (!requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                binding.root.snackbar(R.string.NoCameraFeatureWasFound)
                return
            }

            // Advance with taking the picture

            // Create the file where the photo should go
            val pictureFile = ComplementaryPicturesManager.createNewPictureFile(requireActivity())
            // Store the filename to the view model, so that it is retained even if the fragments gets recreated.
            model.pictureFilename = pictureFile.name
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
            val filename = model.frame.pictureFilename ?: return
            try {
                ComplementaryPicturesManager.rotatePictureRight(requireActivity(), filename)
                binding.ivPicture.rotation = binding.ivPicture.rotation + 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_right)
                binding.ivPicture.startAnimation(animation)
            } catch (e: IOException) {
                binding.root.snackbar(R.string.ErrorWhileEditingPicturesExifData)
            }
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees counterclockwise and set
         * the complementary picture orientation with ComplementaryPicturesManager.
         */
        private fun rotateComplementaryPictureLeft() {
            val filename = model.frame.pictureFilename ?: return
            try {
                ComplementaryPicturesManager.rotatePictureLeft(requireActivity(), filename)
                binding.ivPicture.rotation = binding.ivPicture.rotation - 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_left)
                binding.ivPicture.startAnimation(animation)
            } catch (e: IOException) {
                binding.root.snackbar(R.string.ErrorWhileEditingPicturesExifData)
            }
        }
    }
}