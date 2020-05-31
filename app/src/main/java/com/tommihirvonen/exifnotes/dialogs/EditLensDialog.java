package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dialog to edit Lens's information
 */
public class EditLensDialog extends DialogFragment {

    /**
     * Public constant used to tag the fragment when created
     */
    public static final String TAG = "EditLensDialog";

    /**
     * Constant used to indicate the maximum possible focal length.
     * Could theoretically be anything above zero.
     */
    private static final int MAX_FOCAL_LENGTH = 1500;

    /**
     * Holds the information of the edited Lens
     */
    private Lens lens;

    /**
     * Stores the currently selected aperture value increment setting
     */
    private int newApertureIncrements;

    /**
     * Currently selected minimum aperture (highest f-number)
     */
    private String newMinAperture;

    /**
     * Currently selected maximum aperture (lowest f-number)
     */
    private String newMaxAperture;

    /**
     * Reference to the TextView to display the aperture value range
     */
    private TextView apertureRangeTextView;

    /**
     * Stores the currently displayed aperture values.
     * Changes depending on the currently selected aperture value increments.
     */
    private String[] displayedApertureValues;

    /**
     * Currently selected minimum focal length
     */
    private int newMinFocalLength;

    /**
     * Currently selected maximum focal length
     */
    private int newMaxFocalLength;

    /**
     * Reference to the TextView to display the focal length range
     */
    private TextView focalLengthRangeTextView;

    /**
     * Empty constructor
     */
    public EditLensDialog(){

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (final Bundle SavedInstanceState) {

        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView =
                layoutInflater.inflate(R.layout.dialog_lens, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        final String title = getArguments().getString(ExtraKeys.TITLE);
        final String positiveButton = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        lens = getArguments().getParcelable(ExtraKeys.LENS);
        if (lens == null) lens = new Lens();

        final NestedScrollView nestedScrollView = inflatedView.findViewById(
                R.id.nested_scroll_view);
        nestedScrollView.setOnScrollChangeListener(
                new Utilities.ScrollIndicatorNestedScrollViewListener(getActivity(),
                        nestedScrollView,
                        inflatedView.findViewById(R.id.scrollIndicatorUp),
                        inflatedView.findViewById(R.id.scrollIndicatorDown)));

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflatedView);

        //==========================================================================================
        //DIVIDERS

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(getActivity())) {
            final List<View> dividerList = new ArrayList<>();
            dividerList.add(inflatedView.findViewById(R.id.divider_view1));
            dividerList.add(inflatedView.findViewById(R.id.divider_view2));
            dividerList.add(inflatedView.findViewById(R.id.divider_view3));
            dividerList.add(inflatedView.findViewById(R.id.divider_view4));
            dividerList.add(inflatedView.findViewById(R.id.divider_view5));
            for (final View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
        }
        //==========================================================================================

        // EDIT TEXT FIELDS
        final EditText makeEditText = inflatedView.findViewById(R.id.make_editText);
        makeEditText.setText(lens.getMake());
        final EditText modelEditText = inflatedView.findViewById(R.id.model_editText);
        modelEditText.setText(lens.getModel());
        final EditText serialNumberEditText = inflatedView.findViewById(R.id.serialNumber_editText);
        serialNumberEditText.setText(lens.getSerialNumber());


        //APERTURE INCREMENTS BUTTON
        newApertureIncrements = lens.getApertureIncrements();
        final TextView apertureIncrementsTextView = inflatedView.findViewById(R.id.increment_text);
        apertureIncrementsTextView.setText(
                getResources().getStringArray(R.array.StopIncrements)[lens.getApertureIncrements()]);

        final LinearLayout apertureIncrementLayout = inflatedView.findViewById(R.id.increment_layout);
        apertureIncrementLayout.setOnClickListener(view -> {
            final int checkedItem = newApertureIncrements;
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(R.string.ChooseIncrements));
            builder.setSingleChoiceItems(R.array.StopIncrements, checkedItem,
                    (dialogInterface, i) -> {
                        newApertureIncrements = i;
                        apertureIncrementsTextView.setText(
                                getResources().getStringArray(R.array.StopIncrements)[i]);

                        //Shutter speed increments were changed, make update
                        //Check if the new increments include both min and max values.
                        //Otherwise reset them to null
                        boolean minFound = false, maxFound = false;
                        switch (newApertureIncrements) {
                            case 1:
                                displayedApertureValues = getActivity().getResources()
                                        .getStringArray(R.array.ApertureValuesHalf);
                                break;
                            case 2:
                                displayedApertureValues = getActivity().getResources()
                                        .getStringArray(R.array.ApertureValuesFull);
                                break;
                            case 0:
                            default:
                                displayedApertureValues = getActivity().getResources()
                                        .getStringArray(R.array.ApertureValuesThird);
                                break;
                        }
                        for (final String string : displayedApertureValues) {
                            if (!minFound && string.equals(newMinAperture)) minFound = true;
                            if (!maxFound && string.equals(newMaxAperture)) maxFound = true;
                            if (minFound && maxFound) break;
                        }
                        //If either one wasn't found in the new values array, null them.
                        if (!minFound || !maxFound) {
                            newMinAperture = null;
                            newMaxAperture = null;
                            updateApertureRangeTextView();
                        }

                        dialogInterface.dismiss();
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            builder.create().show();
        });

        //APERTURE RANGE BUTTON
        newMinAperture = lens.getMinAperture();
        newMaxAperture = lens.getMaxAperture();
        apertureRangeTextView = inflatedView.findViewById(R.id.aperture_range_text);
        updateApertureRangeTextView();

        final LinearLayout apertureRangeLayout = inflatedView.findViewById(R.id.aperture_range_layout);
        apertureRangeLayout.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_double_numberpicker, null);
            final NumberPicker maxAperturePicker =
                    dialogView.findViewById(R.id.number_picker_one);
            final NumberPicker minAperturePicker =
                    dialogView.findViewById(R.id.number_picker_two);

            final int color = Utilities.isAppThemeDark(getActivity()) ?
                    ContextCompat.getColor(getActivity(), R.color.light_grey) :
                    ContextCompat.getColor(getActivity(), R.color.grey);
            final ImageView dash = dialogView.findViewById(R.id.dash);
            Utilities.setColorFilter(dash.getDrawable().mutate(), color);

            //To prevent text edit
            minAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            maxAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

            initialiseApertureRangePickers(minAperturePicker, maxAperturePicker);

            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseApertureRange));
            builder.setPositiveButton(getResources().getString(R.string.OK), null);
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
            //Override the positiveButton to check the range before accepting.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                if ((minAperturePicker.getValue() == displayedApertureValues.length - 1 &&
                        maxAperturePicker.getValue() != displayedApertureValues.length - 1)
                        ||
                        (minAperturePicker.getValue() != displayedApertureValues.length - 1 &&
                                maxAperturePicker.getValue() == displayedApertureValues.length - 1)) {
                    // No min or max shutter was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxAperture),
                            Toast.LENGTH_LONG).show();
                } else {
                    if (minAperturePicker.getValue() == displayedApertureValues.length - 1 &&
                            maxAperturePicker.getValue() == displayedApertureValues.length - 1) {
                        newMinAperture = null;
                        newMaxAperture = null;
                    } else if (minAperturePicker.getValue() < maxAperturePicker.getValue()) {
                        newMinAperture = displayedApertureValues[minAperturePicker.getValue()];
                        newMaxAperture = displayedApertureValues[maxAperturePicker.getValue()];
                    } else {
                        newMinAperture = displayedApertureValues[maxAperturePicker.getValue()];
                        newMaxAperture = displayedApertureValues[minAperturePicker.getValue()];
                    }
                    updateApertureRangeTextView();
                }
                dialog.dismiss();
            });
        });


        //FOCAL LENGTH RANGE BUTTON
        newMinFocalLength = lens.getMinFocalLength();
        newMaxFocalLength = lens.getMaxFocalLength();
        focalLengthRangeTextView = inflatedView.findViewById(R.id.focal_length_range_text);
        updateFocalLengthRangeTextView();

        final LinearLayout focalLengthRangeLayout = inflatedView.findViewById(R.id.focal_length_range_layout);
        focalLengthRangeLayout.setOnClickListener(view -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_double_numberpicker_buttons, null);
            final NumberPicker minFocalLengthPicker =
                    dialogView.findViewById(R.id.number_picker_one);
            final NumberPicker maxFocalLengthPicker =
                    dialogView.findViewById(R.id.number_picker_two);

            final int jumpAmount = 50;
            final LinearLayout minFocalLengthFastRewind =
                    dialogView.findViewById(R.id.picker_one_fast_rewind);
            final LinearLayout minFocalLengthFastForward =
                    dialogView.findViewById(R.id.picker_one_fast_forward);
            final LinearLayout maxFocalLengthFastRewind =
                    dialogView.findViewById(R.id.picker_two_fast_rewind);
            final LinearLayout maxFocalLengthFastForward =
                    dialogView.findViewById(R.id.picker_two_fast_forward);

            final int color = Utilities.isAppThemeDark(getActivity()) ?
                    ContextCompat.getColor(getActivity(), R.color.light_grey) :
                    ContextCompat.getColor(getActivity(), R.color.grey);
            final List<ImageView> imageViewList = new ArrayList<>();
            imageViewList.add(dialogView.findViewById(R.id.picker_one_fast_rewind_image));
            imageViewList.add(dialogView.findViewById(R.id.picker_one_fast_forward_image));
            imageViewList.add(dialogView.findViewById(R.id.picker_two_fast_rewind_image));
            imageViewList.add(dialogView.findViewById(R.id.picker_two_fast_forward_image));
            imageViewList.add(dialogView.findViewById(R.id.dash));
            for (final ImageView iv : imageViewList)
                Utilities.setColorFilter(iv.getDrawable().mutate(), color);

            //To prevent text edit
            minFocalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            maxFocalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

            initialiseFocalLengthRangePickers(minFocalLengthPicker, maxFocalLengthPicker);

            minFocalLengthFastRewind.setOnClickListener(view14 ->
                    minFocalLengthPicker.setValue(minFocalLengthPicker.getValue() - jumpAmount));
            minFocalLengthFastForward.setOnClickListener(view13 ->
                    minFocalLengthPicker.setValue(minFocalLengthPicker.getValue() + jumpAmount));
            maxFocalLengthFastRewind.setOnClickListener(view12 ->
                    maxFocalLengthPicker.setValue(maxFocalLengthPicker.getValue() - jumpAmount));
            maxFocalLengthFastForward.setOnClickListener(view1 ->
                    maxFocalLengthPicker.setValue(maxFocalLengthPicker.getValue() + jumpAmount));

            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseFocalLengthRange));
            builder.setPositiveButton(getResources().getString(R.string.OK), null);
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();

            //Override the positiveButton to check the range before accepting.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                //Set the max and min focal lengths. Check which is smaller and set it to be
                //min and vice versa.
                if (minFocalLengthPicker.getValue() == 0 || maxFocalLengthPicker.getValue() == 0) {
                    newMinFocalLength = 0;
                    newMaxFocalLength = 0;
                } else if (minFocalLengthPicker.getValue() < maxFocalLengthPicker.getValue()) {
                    newMaxFocalLength = maxFocalLengthPicker.getValue();
                    newMinFocalLength = minFocalLengthPicker.getValue();
                } else {
                    newMaxFocalLength = minFocalLengthPicker.getValue();
                    newMinFocalLength = maxFocalLengthPicker.getValue();
                }
                updateFocalLengthRangeTextView();
                dialog.dismiss();
            });
        });


        //FINALISE BUILDING THE DIALOG
        alert.setPositiveButton(positiveButton, null);

        alert.setNegativeButton(R.string.Cancel, (dialog, whichButton) -> {
            final Intent intent = new Intent();
            getTargetFragment().onActivityResult(
                    getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
        });
        final AlertDialog dialog = alert.create();
        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
        // We override the positive button onClick so that we can dismiss the dialog
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            final String make = makeEditText.getText().toString();
            final String model = modelEditText.getText().toString();
            final String serialNumber = serialNumberEditText.getText().toString();

            if (make.length() == 0 && model.length() == 0) {
                // No make or model was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel),
                        Toast.LENGTH_SHORT).show();
            } else if (make.length() > 0 && model.length() == 0) {
                // No model was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
            } else if (make.length() == 0) {
                // No make was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
            } else {
                //All the required information was given. Save.
                lens.setMake(make);
                lens.setModel(model);
                lens.setSerialNumber(serialNumber);
                lens.setApertureIncrements(newApertureIncrements);
                lens.setMinAperture(newMinAperture);
                lens.setMaxAperture(newMaxAperture);
                lens.setMinFocalLength(newMinFocalLength);
                lens.setMaxFocalLength(newMaxFocalLength);

                // Return the new entered name to the calling activity
                final Intent intent = new Intent();
                intent.putExtra(ExtraKeys.LENS, lens);
                dialog.dismiss();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, intent
                );
            }
        });
        return dialog;
    }

    /**
     * Called when the aperture range dialog is opened.
     * Sets the values for the NumberPickers.
     *
     * @param minAperturePicker NumberPicker associated with the minimum aperture value
     * @param maxAperturePicker NumberPicker associated with the maximum aperture value
     */
    private void initialiseApertureRangePickers(final NumberPicker minAperturePicker,
                                                final NumberPicker maxAperturePicker){
        switch (newApertureIncrements) {
            case 1:
                displayedApertureValues = getActivity().getResources()
                        .getStringArray(R.array.ApertureValuesHalf);
                break;
            case 2:
                displayedApertureValues = getActivity().getResources()
                        .getStringArray(R.array.ApertureValuesFull);
                break;
            case 0:
            default:
                displayedApertureValues = getActivity().getResources()
                        .getStringArray(R.array.ApertureValuesThird);
                break;
        }
        if (displayedApertureValues[0].equals(getResources().getString(R.string.NoValue))) {
            Collections.reverse(Arrays.asList(displayedApertureValues));
        }
        minAperturePicker.setMinValue(0);
        maxAperturePicker.setMinValue(0);
        minAperturePicker.setMaxValue(displayedApertureValues.length-1);
        maxAperturePicker.setMaxValue(displayedApertureValues.length-1);
        minAperturePicker.setDisplayedValues(displayedApertureValues);
        maxAperturePicker.setDisplayedValues(displayedApertureValues);
        minAperturePicker.setValue(displayedApertureValues.length-1);
        maxAperturePicker.setValue(displayedApertureValues.length-1);
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if (displayedApertureValues[i].equals(newMinAperture)) {
                minAperturePicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if (displayedApertureValues[i].equals(newMaxAperture)) {
                maxAperturePicker.setValue(i);
                break;
            }
        }
    }

    /**
     * Called when the focal length range dialog is opened.
     * Sets the values for the NumberPickers.
     *
     * @param minFocalLengthPicker NumberPicker associated with the minimum focal length
     * @param maxFocalLengthPicker NumberPicker associated with the maximum focal length
     */
    private void initialiseFocalLengthRangePickers(final NumberPicker minFocalLengthPicker,
                                                   final NumberPicker maxFocalLengthPicker) {
        minFocalLengthPicker.setMinValue(0);
        maxFocalLengthPicker.setMinValue(0);
        minFocalLengthPicker.setMaxValue(MAX_FOCAL_LENGTH);
        maxFocalLengthPicker.setMaxValue(MAX_FOCAL_LENGTH);
        minFocalLengthPicker.setValue(50);
        maxFocalLengthPicker.setValue(50);
        for (int i = 0; i <= MAX_FOCAL_LENGTH; ++i) {
            if (i == newMinFocalLength) {
                minFocalLengthPicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i <= MAX_FOCAL_LENGTH; ++i) {
            if (i == newMaxFocalLength) {
                maxFocalLengthPicker.setValue(i);
                break;
            }
        }
    }

    /**
     * Update the aperture range button's text
     */
    private void updateApertureRangeTextView(){
        apertureRangeTextView.setText(newMinAperture == null || newMaxAperture == null ?
                getResources().getString(R.string.ClickToSet) :
                "f/" + newMaxAperture + " - " + "f/" + newMinAperture
        );
    }

    /**
     * Update the focal length range button's text
     */
    private void updateFocalLengthRangeTextView(){
        final String text;
        if (newMinFocalLength == 0 || newMaxFocalLength == 0) {
            text = getResources().getString(R.string.ClickToSet);
        } else if (newMinFocalLength == newMaxFocalLength) {
            text = "" + newMinFocalLength;
        } else {
            text = newMinFocalLength + " - " + newMaxFocalLength;
        }
        focalLengthRangeTextView.setText(text);
    }

}
