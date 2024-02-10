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
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogSingleEditTextBinding
import com.tommihirvonen.exifnotes.databinding.FragmentFrameEditBinding
import com.tommihirvonen.exifnotes.entities.*
import com.tommihirvonen.exifnotes.entities.Filter
import com.tommihirvonen.exifnotes.geocoder.GeocoderRequestBuilder
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.FrameEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.FrameEditViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Dialog to edit Frame's information
 */
@AndroidEntryPoint
class FrameEditFragment : Fragment() {

    @Inject
    lateinit var geocoderRequestBuilder: GeocoderRequestBuilder

    val arguments by navArgs<FrameEditFragmentArgs>()
    
    private lateinit var binding: FragmentFrameEditBinding

    private val model by viewModels<FrameEditViewModel> {
        FrameEditViewModelFactory(requireActivity().application, geocoderRequestBuilder,
            arguments.frame.copy())
    }

    val dateTimePickHandler by lazy { dateTimePickHandler(model.frame::date, model.observable::setDate) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(FastOutSlowInInterpolator())
            .apply { duration = 250L }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFrameEditBinding.inflate(inflater, container, false)
        binding.viewmodel = model.observable
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        // Start the transition once all views have been measured and laid out.
        (view.parent as ViewGroup).doOnPreDraw {
            startPostponedEnterTransition()
        }
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.frame_edit_dest)
        navBackStackEntry.observeThenClearNavigationResult<Filter>(viewLifecycleOwner, ExtraKeys.FILTER) { filter ->
            filter?.let(model::addFilter)
        }
        observeThenClearNavigationResult(ExtraKeys.LENS, model::addLens)
        observeThenClearNavigationResult<LocationPickResponse>(ExtraKeys.LOCATION) { response ->
            val (location, formattedAddress) = response
            model.observable.setLocation(location, formattedAddress)
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

    fun navigateBack() = findNavController().navigateUp()

    fun showCustomApertureDialog() {
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

    fun showCustomShutterDialog() {
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

    fun navigateToAddLensFragment() {
        val title = resources.getString(R.string.AddNewLens)
        val sharedElement = binding.addLens
        val action = FrameEditFragmentDirections
            .frameEditLensEditAction(null, false, title, sharedElement.transitionName)
        val extras = FragmentNavigatorExtras(
            sharedElement to sharedElement.transitionName
        )
        findNavController().navigate(action, extras)
    }

    fun navigateToLocationPickFragment() {
        val action = FrameEditFragmentDirections
            .frameEditLocationPickAction(model.frame.location, model.frame.formattedAddress)
        findNavController().navigate(action)
    }

    fun showAddFilterDialog() {
        val title = resources.getString(R.string.AddNewFilter)
        val positiveButtonText = resources.getString(R.string.Add)
        val action = FrameEditFragmentDirections
            .frameEditFilterEditAction(null, title, positiveButtonText)
        findNavController().navigate(action)
    }

    fun showFiltersPickDialog() {
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

    fun showFocalLengthDialog() {
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

    fun showComplementaryPictureOptions(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        // If a complementary picture was not set, set only the two first options
        val menuRes = if (model.frame.pictureFilename == null) {
            R.menu.menu_complementary_picture_not_set
        } else {
            R.menu.menu_complementary_picture_set
        }
        popupMenu.inflate(menuRes)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.new_complementary_picture -> {
                    startPictureActivity()
                }
                R.id.select_from_gallery -> {
                    selectImageResultLauncher.launch("image/*")
                }
                R.id.add_to_gallery -> {
                    try {
                        ComplementaryPicturesManager
                            .addPictureToGallery(requireActivity(), model.frame.pictureFilename)
                        binding.root.snackbar(R.string.PictureAddedToGallery)
                    } catch (e: Exception) {
                        binding.root.snackbar(R.string.ErrorAddingPictureToGallery)
                    }
                }
                R.id.rotate_right -> rotateComplementaryPictureRight()
                R.id.rotate_left -> rotateComplementaryPictureLeft()
                R.id.clear -> {
                    model.frame.pictureFilename = null
                    binding.complementaryPicture.visibility = View.GONE
                    binding.pictureText.visibility = View.VISIBLE
                    binding.pictureText.text = null
                }
            }
            return@setOnMenuItemClickListener true
        }
        // Use reflection to enable icons for the popup menu.
        try {
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(menu, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        popupMenu.show()
    }

    fun submit() {
        if (model.validate()) {
            setNavigationResult(model.frame, ExtraKeys.FRAME)
            navigateBack()
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
            binding.complementaryPicture.visibility = View.VISIBLE

            // Load the bitmap on a background thread
            lifecycleScope.launch(Dispatchers.IO) {
                // Get the target ImageView height.
                // Because the complementary picture ImageView uses subclass SquareImageView,
                // the ImageView width should also be its height. Because the ImageView's
                // width is match_parent, we get the Fragment's width instead.
                // If there is a problem getting the Fragment view, use the resource dimension instead.
                val targetH = view?.width ?: resources.getDimension(R.dimen.ComplementaryPictureImageViewHeight).toInt()

                // Rotate the ImageView if necessary
                val rotation = try {
                    val exifInterface = ExifInterface(pictureFile.absolutePath)
                    val orientation = exifInterface
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } catch (e: Exception) {
                    0
                }

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
                    binding.complementaryPicture.rotation = rotation.toFloat()
                    binding.complementaryPicture.setImageBitmap(bitmap)
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.fade_in_fast)
                    binding.complementaryPicture.startAnimation(animation)
                }
            }
        } else {
            binding.pictureText.setText(R.string.PictureSetButNotFound)
        }
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
            binding.complementaryPicture.rotation += 90
            val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_right)
            binding.complementaryPicture.startAnimation(animation)
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
            binding.complementaryPicture.rotation -= 90
            val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_left)
            binding.complementaryPicture.startAnimation(animation)
        } catch (e: IOException) {
            binding.root.snackbar(R.string.ErrorWhileEditingPicturesExifData)
        }
    }

}