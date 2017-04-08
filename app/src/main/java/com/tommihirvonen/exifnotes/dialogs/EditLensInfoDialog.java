package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dialog to edit Lens's information
 */
public class EditLensInfoDialog extends DialogFragment {

    /**
     * Public constant used to tag the fragment when created
     */
    public static final String TAG = "LensInfoDialogFragment";

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
     * Reference to the utilities class
     */
    private Utilities utilities;

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
     * Reference to the Button to edit the aperture value range
     */
    private Button apertureRangeButton;

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
     * Reference to the Button to edit the focal length range
     */
    private Button focalLengthRangeButton;

    /**
     * Empty constructor
     */
    public EditLensInfoDialog(){

    }

    /**
     * Called when the DialogFragment is ready to create the dialog.
     * Inflate the fragment. Get the edited lens.
     * Initialize the UI objects and display lens's information.
     * Add listeners to buttons to open new dialogs to change the lens's setting.
     *
     * @param SavedInstanceState possible saved state in case the DialogFragment was resumed
     * @return inflated dialog ready to be shown
     */
    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getApplicationContext()
        );

        utilities = new Utilities(getActivity());

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView =
                layoutInflater.inflate(R.layout.lens_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        lens = getArguments().getParcelable("LENS");
        if (lens == null) lens = new Lens();

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = (FrameLayout) inflatedView.findViewById(R.id.root);
            NestedScrollView nestedScrollView = (NestedScrollView) inflatedView.findViewById(
                    R.id.nested_scroll_view);
            Utilities.setScrollIndicators(rootLayout, nestedScrollView,
                    ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
        }

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflatedView);

        final EditText makeEditText = (EditText) inflatedView.findViewById(R.id.txt_make);
        makeEditText.setText(lens.getMake());
        final EditText modelEditText = (EditText) inflatedView.findViewById(R.id.txt_model);
        modelEditText.setText(lens.getModel());
        final EditText serialNumberEditText = (EditText) inflatedView.findViewById(R.id.txt_serial_number);
        serialNumberEditText.setText(lens.getSerialNumber());

        //APERTURE INCREMENTS BUTTON
        newApertureIncrements = lens.getApertureIncrements();
        final TextView apertureIncrementsTextView = (TextView) inflatedView.findViewById(R.id.btn_apertureIncrements);
        apertureIncrementsTextView.setClickable(true);
        apertureIncrementsTextView.setText(
                getResources().getStringArray(R.array.StopIncrements)[lens.getApertureIncrements()]);
        apertureIncrementsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int checkedItem = newApertureIncrements;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseIncrements));
                builder.setSingleChoiceItems(R.array.StopIncrements, checkedItem,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newApertureIncrements = i;
                        apertureIncrementsTextView.setText(
                                getResources().getStringArray(R.array.StopIncrements)[i]);

                        //Shutter speed increments were changed, make update
                        //Check if the new increments include both min and max values.
                        //Otherwise reset them to null
                        boolean minFound = false, maxFound = false;
                        switch (newApertureIncrements) {
                            case 0:
                                displayedApertureValues = utilities.apertureValuesThird;
                                break;
                            case 1:
                                displayedApertureValues = utilities.apertureValuesHalf;
                                break;
                            case 2:
                                displayedApertureValues = utilities.apertureValuesFull;
                                break;
                            default:
                                displayedApertureValues = utilities.apertureValuesThird;
                                break;
                        }
                        for (String string : displayedApertureValues) {
                            if (!minFound && string.equals(newMinAperture)) minFound = true;
                            if (!maxFound && string.equals(newMaxAperture)) maxFound = true;
                            if (minFound && maxFound) break;
                        }
                        //If either one wasn't found in the new values array, null them.
                        if (!minFound || !maxFound) {
                            newMinAperture = null;
                            newMaxAperture = null;
                            updateApertureRangeButton();
                        }

                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Do nothing
                    }
                });
                builder.create().show();
            }
        });

        //APERTURE RANGE BUTTON
        newMinAperture = lens.getMinAperture();
        newMaxAperture = lens.getMaxAperture();
        apertureRangeButton = (Button) inflatedView.findViewById(R.id.btn_apertureRange);
        updateApertureRangeButton();
        apertureRangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.double_numberpicker_dialog, null);
                final NumberPicker maxAperturePicker =
                        (NumberPicker) dialogView.findViewById(R.id.number_picker_one);
                final NumberPicker minAperturePicker =
                        (NumberPicker) dialogView.findViewById(R.id.number_picker_two);

                int color = prefs.getString("AppTheme", "LIGHT").equals("DARK") ?
                        ContextCompat.getColor(getActivity(), R.color.light_grey) :
                        ContextCompat.getColor(getActivity(), R.color.grey);
                ImageView dash = (ImageView) dialogView.findViewById(R.id.dash);
                dash.getDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);

                //To prevent text edit
                minAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                maxAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

                initialiseApertureRangePickers(minAperturePicker, maxAperturePicker);

                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseApertureRange));
                builder.setPositiveButton(getResources().getString(R.string.OK), null);
                builder.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Do nothing
                            }
                        });
                final AlertDialog dialog = builder.create();
                dialog.show();
                //Override the positiveButton to check the range before accepting.
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ((minAperturePicker.getValue() == displayedApertureValues.length-1 &&
                                maxAperturePicker.getValue() != displayedApertureValues.length-1)
                                ||
                                (minAperturePicker.getValue() != displayedApertureValues.length-1 &&
                                        maxAperturePicker.getValue() == displayedApertureValues.length-1)){
                            // No min or max shutter was set
                            Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxAperture),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            if (minAperturePicker.getValue() == displayedApertureValues.length-1 &&
                                    maxAperturePicker.getValue() == displayedApertureValues.length-1) {
                                newMinAperture = null;
                                newMaxAperture = null;
                            } else if (minAperturePicker.getValue() < maxAperturePicker.getValue()) {
                                newMinAperture = displayedApertureValues[minAperturePicker.getValue()];
                                newMaxAperture = displayedApertureValues[maxAperturePicker.getValue()];
                            } else {
                                newMinAperture = displayedApertureValues[maxAperturePicker.getValue()];
                                newMaxAperture = displayedApertureValues[minAperturePicker.getValue()];
                            }
                            updateApertureRangeButton();
                        }
                        dialog.dismiss();
                    }
                });
            }
        });


        //FOCAL LENGTH RANGE BUTTON
        newMinFocalLength = lens.getMinFocalLength();
        newMaxFocalLength = lens.getMaxFocalLength();
        focalLengthRangeButton = (Button) inflatedView.findViewById(R.id.btn_focalLengthRange);
        updateFocalLengthRangeButton();
        focalLengthRangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.double_numberpicker_dialog_buttons, null);
                final NumberPicker minFocalLengthPicker =
                        (NumberPicker) dialogView.findViewById(R.id.number_picker_one);
                final NumberPicker maxFocalLengthPicker =
                        (NumberPicker) dialogView.findViewById(R.id.number_picker_two);

                final int jumpAmount = 50;
                final LinearLayout minFocalLengthFastRewind =
                        (LinearLayout) dialogView.findViewById(R.id.picker_one_fast_rewind);
                final LinearLayout minFocalLengthFastForward =
                        (LinearLayout) dialogView.findViewById(R.id.picker_one_fast_forward);
                final LinearLayout maxFocalLengthFastRewind =
                        (LinearLayout) dialogView.findViewById(R.id.picker_two_fast_rewind);
                final LinearLayout maxFocalLengthFastForward =
                        (LinearLayout) dialogView.findViewById(R.id.picker_two_fast_forward);

                int color = prefs.getString("AppTheme", "LIGHT").equals("DARK") ?
                        ContextCompat.getColor(getActivity(), R.color.light_grey) :
                        ContextCompat.getColor(getActivity(), R.color.grey);
                List<ImageView> imageViewList = new ArrayList<>();
                imageViewList.add((ImageView) dialogView.findViewById(R.id.picker_one_fast_rewind_image));
                imageViewList.add((ImageView) dialogView.findViewById(R.id.picker_one_fast_forward_image));
                imageViewList.add((ImageView) dialogView.findViewById(R.id.picker_two_fast_rewind_image));
                imageViewList.add((ImageView) dialogView.findViewById(R.id.picker_two_fast_forward_image));
                imageViewList.add((ImageView) dialogView.findViewById(R.id.dash));
                for (ImageView iv : imageViewList)
                    iv.getDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);

                //To prevent text edit
                minFocalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                maxFocalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

                initialiseFocalLengthRangePickers(minFocalLengthPicker, maxFocalLengthPicker);

                minFocalLengthFastRewind.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        minFocalLengthPicker.setValue(minFocalLengthPicker.getValue()-jumpAmount);
                    }
                });
                minFocalLengthFastForward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        minFocalLengthPicker.setValue(minFocalLengthPicker.getValue()+jumpAmount);
                    }
                });
                maxFocalLengthFastRewind.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        maxFocalLengthPicker.setValue(maxFocalLengthPicker.getValue()-jumpAmount);
                    }
                });
                maxFocalLengthFastForward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        maxFocalLengthPicker.setValue(maxFocalLengthPicker.getValue()+jumpAmount);
                    }
                });

                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseFocalLengthRange));
                builder.setPositiveButton(getResources().getString(R.string.OK), null);
                builder.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Do nothing
                            }
                        });
                final AlertDialog dialog = builder.create();
                dialog.show();

                //Override the positiveButton to check the range before accepting.
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                        updateFocalLengthRangeButton();
                        dialog.dismiss();
                    }
                });
            }
        });


        //FINALISE BUILDING THE DIALOG
        alert.setPositiveButton(positiveButton, null);

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
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
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String make = makeEditText.getText().toString();
                String model = modelEditText.getText().toString();
                String serialNumber = serialNumberEditText.getText().toString();

                if (make.length() == 0 && model.length() == 0) {
                    // No make or model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel),
                            Toast.LENGTH_SHORT).show();
                } else if (make.length() > 0 && model.length() == 0) {
                    // No model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
                } else if (make.length() == 0 && model.length() > 0) {
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
                    Intent intent = new Intent();
                    intent.putExtra("LENS", lens);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(
                            getTargetRequestCode(), Activity.RESULT_OK, intent
                    );
                }
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
    private void initialiseApertureRangePickers(NumberPicker minAperturePicker,
                                                NumberPicker maxAperturePicker){
        switch (newApertureIncrements) {
            case 0:
                displayedApertureValues = utilities.apertureValuesThird;
                break;
            case 1:
                displayedApertureValues = utilities.apertureValuesHalf;
                break;
            case 2:
                displayedApertureValues = utilities.apertureValuesFull;
                break;
            default:
                displayedApertureValues = utilities.apertureValuesThird;
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
    private void initialiseFocalLengthRangePickers(NumberPicker minFocalLengthPicker,
                                                   NumberPicker maxFocalLengthPicker) {
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
    private void updateApertureRangeButton(){
        apertureRangeButton.setText(newMinAperture == null || newMaxAperture == null ?
                getResources().getString(R.string.ClickToSet) :
                "f/" + newMaxAperture + " - " + "f/" + newMinAperture
        );
    }

    /**
     * Update the focal length range button's text
     */
    private void updateFocalLengthRangeButton(){
        focalLengthRangeButton.setText(newMinFocalLength == 0 || newMaxFocalLength == 0 ?
                getResources().getString(R.string.ClickToSet) :
                newMinFocalLength + " - " + newMaxFocalLength
        );
    }

}
