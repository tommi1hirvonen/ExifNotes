package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities
import com.tommihirvonen.exifnotes.utilities.Utilities.ScrollIndicatorNestedScrollViewListener

/**
 * Dialog to edit Lens's information
 */
class EditLensDialog : DialogFragment() {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val TAG = "EditLensDialog"

        /**
         * Constant used to indicate the maximum possible focal length.
         * Could theoretically be anything above zero.
         */
        private const val MAX_FOCAL_LENGTH = 1500
    }

    private lateinit var newLens: Lens

    /**
     * Reference to the TextView to display the aperture value range
     */
    private lateinit var apertureRangeTextView: TextView

    /**
     * Stores the currently displayed aperture values.
     * Changes depending on the currently selected aperture value increments.
     */
    private lateinit var displayedApertureValues: Array<String>

    /**
     * Reference to the TextView to display the focal length range
     */
    private lateinit var focalLengthRangeTextView: TextView

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams")
        val inflatedView = layoutInflater.inflate(R.layout.dialog_lens, null)
        val alert = AlertDialog.Builder(activity)
        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButton = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        val lens = requireArguments().getParcelable(ExtraKeys.LENS) ?: Lens()
        newLens = lens.copy()
        val nestedScrollView: NestedScrollView = inflatedView.findViewById(R.id.nested_scroll_view)
        nestedScrollView.setOnScrollChangeListener(
                ScrollIndicatorNestedScrollViewListener(
                        requireActivity(),
                        nestedScrollView,
                        inflatedView.findViewById(R.id.scrollIndicatorUp),
                        inflatedView.findViewById(R.id.scrollIndicatorDown)))
        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(activity, title))
        alert.setView(inflatedView)

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(activity)) {
            listOf<View>(
                    inflatedView.findViewById(R.id.divider_view1),
                    inflatedView.findViewById(R.id.divider_view2),
                    inflatedView.findViewById(R.id.divider_view3),
                    inflatedView.findViewById(R.id.divider_view4),
                    inflatedView.findViewById(R.id.divider_view5)
            ).forEach { it.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white)) }
        }

        // EDIT TEXT FIELDS
        val makeEditText = inflatedView.findViewById<EditText>(R.id.make_editText)
        makeEditText.setText(lens.make)
        val modelEditText = inflatedView.findViewById<EditText>(R.id.model_editText)
        modelEditText.setText(lens.model)
        val serialNumberEditText = inflatedView.findViewById<EditText>(R.id.serialNumber_editText)
        serialNumberEditText.setText(lens.serialNumber)


        // APERTURE INCREMENTS BUTTON
        val apertureIncrementsTextView = inflatedView.findViewById<TextView>(R.id.increment_text)
        apertureIncrementsTextView.text = resources.getStringArray(R.array.StopIncrements)[lens.apertureIncrements]
        val apertureIncrementLayout = inflatedView.findViewById<LinearLayout>(R.id.increment_layout)
        apertureIncrementLayout.setOnClickListener {
            val checkedItem = newLens.apertureIncrements
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(resources.getString(R.string.ChooseIncrements))
            builder.setSingleChoiceItems(R.array.StopIncrements, checkedItem) { dialogInterface: DialogInterface, i: Int ->
                newLens.apertureIncrements = i
                apertureIncrementsTextView.text = resources.getStringArray(R.array.StopIncrements)[i]

                //Check if the new increments include both min and max values.
                //Otherwise reset them to null
                displayedApertureValues = when (newLens.apertureIncrements) {
                    1 -> requireActivity().resources.getStringArray(R.array.ApertureValuesHalf)
                    2 -> requireActivity().resources.getStringArray(R.array.ApertureValuesFull)
                    0 -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
                    else -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
                }
                val minFound = displayedApertureValues.contains(newLens.minAperture)
                val maxFound = displayedApertureValues.contains(newLens.maxAperture)
                // If either one wasn't found in the new values array, null them.
                if (!minFound || !maxFound) {
                    newLens.minAperture = null
                    newLens.maxAperture = null
                    updateApertureRangeTextView()
                }
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            builder.create().show()
        }

        // APERTURE RANGE BUTTON
        newLens.minAperture = lens.minAperture
        newLens.maxAperture = lens.maxAperture
        apertureRangeTextView = inflatedView.findViewById(R.id.aperture_range_text)
        updateApertureRangeTextView()
        val apertureRangeLayout = inflatedView.findViewById<LinearLayout>(R.id.aperture_range_layout)
        apertureRangeLayout.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_double_numberpicker, null)
            val maxAperturePicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_one)
            val minAperturePicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_two)
            val color =
                    if (Utilities.isAppThemeDark(activity)) ContextCompat.getColor(requireActivity(), R.color.light_grey)
                    else ContextCompat.getColor(requireActivity(), R.color.grey)
            val dash = dialogView.findViewById<ImageView>(R.id.dash)
            Utilities.setColorFilter(dash.drawable.mutate(), color)

            //To prevent text edit
            minAperturePicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            maxAperturePicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            initialiseApertureRangePickers(minAperturePicker, maxAperturePicker)
            builder.setView(dialogView)
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
                    Toast.makeText(activity, resources.getString(R.string.NoMinOrMaxAperture),
                            Toast.LENGTH_LONG).show()
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
        focalLengthRangeTextView = inflatedView.findViewById(R.id.focal_length_range_text)
        updateFocalLengthRangeTextView()
        val focalLengthRangeLayout = inflatedView.findViewById<LinearLayout>(R.id.focal_length_range_layout)
        focalLengthRangeLayout.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_double_numberpicker_buttons, null)
            val minFocalLengthPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_one)
            val maxFocalLengthPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_two)
            val jumpAmount = 50
            val minFocalLengthFastRewind = dialogView.findViewById<LinearLayout>(R.id.picker_one_fast_rewind)
            val minFocalLengthFastForward = dialogView.findViewById<LinearLayout>(R.id.picker_one_fast_forward)
            val maxFocalLengthFastRewind = dialogView.findViewById<LinearLayout>(R.id.picker_two_fast_rewind)
            val maxFocalLengthFastForward = dialogView.findViewById<LinearLayout>(R.id.picker_two_fast_forward)
            val color =
                    if (Utilities.isAppThemeDark(activity)) ContextCompat.getColor(requireActivity(), R.color.light_grey)
                    else ContextCompat.getColor(requireActivity(), R.color.grey)
            listOf<ImageView>(
                    dialogView.findViewById(R.id.picker_one_fast_rewind_image),
                    dialogView.findViewById(R.id.picker_one_fast_forward_image),
                    dialogView.findViewById(R.id.picker_two_fast_rewind_image),
                    dialogView.findViewById(R.id.picker_two_fast_forward_image),
                    dialogView.findViewById(R.id.dash)
            ).forEach { Utilities.setColorFilter(it.drawable.mutate(), color) }

            // To prevent text edit
            minFocalLengthPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            maxFocalLengthPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            initialiseFocalLengthRangePickers(minFocalLengthPicker, maxFocalLengthPicker)
            minFocalLengthFastRewind.setOnClickListener { minFocalLengthPicker.value = minFocalLengthPicker.value - jumpAmount }
            minFocalLengthFastForward.setOnClickListener { minFocalLengthPicker.value = minFocalLengthPicker.value + jumpAmount }
            maxFocalLengthFastRewind.setOnClickListener { maxFocalLengthPicker.value = maxFocalLengthPicker.value - jumpAmount }
            maxFocalLengthFastForward.setOnClickListener { maxFocalLengthPicker.value = maxFocalLengthPicker.value + jumpAmount }
            builder.setView(dialogView)
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

        // FINALISE BUILDING THE DIALOG
        alert.setPositiveButton(positiveButton, null)
        alert.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            val intent = Intent()
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, intent)
        }
        val dialog = alert.create()

        // SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        dialog.show()

        // Override the positive button onClick so that we can dismiss the dialog
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val make = makeEditText.text.toString()
            val model = modelEditText.text.toString()
            val serialNumber = serialNumberEditText.text.toString()
            if (make.isEmpty() && model.isEmpty()) {
                // No make or model was set
                Toast.makeText(activity, resources.getString(R.string.NoMakeOrModel),
                        Toast.LENGTH_SHORT).show()
            } else if (make.isNotEmpty() && model.isEmpty()) {
                // No model was set
                Toast.makeText(activity, resources.getString(R.string.NoModel), Toast.LENGTH_SHORT).show()
            } else if (make.isEmpty()) {
                // No make was set
                Toast.makeText(activity, resources.getString(R.string.NoMake), Toast.LENGTH_SHORT).show()
            } else {
                //All the required information was given. Save.
                lens.make = make
                lens.model = model
                lens.serialNumber = serialNumber
                lens.apertureIncrements = newLens.apertureIncrements
                lens.minAperture = newLens.minAperture
                lens.maxAperture = newLens.maxAperture
                lens.minFocalLength = newLens.minFocalLength
                lens.maxFocalLength = newLens.maxFocalLength

                // Return the new entered name to the calling activity
                val intent = Intent()
                intent.putExtra(ExtraKeys.LENS, lens)
                dialog.dismiss()
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            }
        }
        return dialog
    }

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
            1 -> requireActivity().resources.getStringArray(R.array.ApertureValuesHalf)
            2 -> requireActivity().resources.getStringArray(R.array.ApertureValuesFull)
            0 -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
            else -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
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
        apertureRangeTextView.text =
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
        focalLengthRangeTextView.text = text
    }

}