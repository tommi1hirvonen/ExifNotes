package com.tommihirvonen.exifnotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.Arrays;
import java.util.Collections;

//Copyright 2016
//Tommi Hirvonen

public class EditLensInfoDialog extends DialogFragment {

    public static final String TAG = "LensInfoDialogFragment";

    public EditLensInfoDialog(){

    }

    Lens lens;
    Utilities utilities;
    int newApertureIncrements;
    String[] displayedApertureValues;

    TextView bApertureIncrements;

    NumberPicker minAperturePicker;
    NumberPicker maxAperturePicker;

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        utilities = new Utilities(getActivity());

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(R.layout.lens_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        lens = getArguments().getParcelable("LENS");
        if (lens == null) lens = new Lens();

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = (FrameLayout) inflator.findViewById(R.id.root);
            NestedScrollView nestedScrollView = (NestedScrollView) inflator.findViewById(R.id.nested_scroll_view);
            Utilities.setScrollIndicators(rootLayout, nestedScrollView,
                    ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
        }

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_make);
        et1.setText(lens.getMake());
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_model);
        et2.setText(lens.getModel());
        final EditText et3 = (EditText) inflator.findViewById(R.id.txt_serial_number);
        et3.setText(lens.getSerialNumber());

        //APERTURE INCREMENTS BUTTON
        newApertureIncrements = lens.getApertureIncrements();
        bApertureIncrements = (TextView) inflator.findViewById(R.id.btn_apertureIncrements);
        bApertureIncrements.setClickable(true);
        bApertureIncrements.setText(getResources().getStringArray(R.array.StopIncrements)[lens.getApertureIncrements()]);
        bApertureIncrements.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseIncrements));
                builder.setItems(R.array.StopIncrements, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newApertureIncrements = i;
                        bApertureIncrements.setText(getResources().getStringArray(R.array.StopIncrements)[i]);
                        initialiseApertureRangePickers();
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Do nothing
                    }
                });
                builder.create().show();
            }
        });

        //FOCAL LENGTH RANGE PICKERS
        final NumberPicker minFocalLengthPicker = (NumberPicker) inflator.findViewById(R.id.minFocalLengthPicker);
        final NumberPicker maxFocalLengthPicker = (NumberPicker) inflator.findViewById(R.id.maxFocalLengthPicker);
        minFocalLengthPicker.setMinValue(0);
        maxFocalLengthPicker.setMinValue(0);
        minFocalLengthPicker.setMaxValue(1500);
        maxFocalLengthPicker.setMaxValue(1500);
        minFocalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        maxFocalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        minFocalLengthPicker.setValue(50);
        maxFocalLengthPicker.setValue(50);
        for (int i = 0; i < 1501; ++i) {
            if ( i == lens.getMinFocalLength() ) {
                minFocalLengthPicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i < 1501; ++i) {
            if (i == lens.getMaxFocalLength()) {
                maxFocalLengthPicker.setValue(i);
                break;
            }
        }

        //APERTURE RANGE PICKERS
        minAperturePicker = (NumberPicker) inflator.findViewById(R.id.minAperturePicker);
        maxAperturePicker = (NumberPicker) inflator.findViewById(R.id.maxAperturePicker);
        initialiseApertureRangePickers();
        minAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        maxAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);


        //FINALISE BUILDING THE DIALOG
        alert.setPositiveButton(positiveButton, null);

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
        });
        final AlertDialog dialog = alert.create();
        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
        // We override the positive button onClick so that we can dismiss the dialog
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String make = et1.getText().toString();
                String model = et2.getText().toString();
                String serialNumber = et3.getText().toString();

                if (make.length() == 0 && model.length() == 0) {
                    // No make or model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel), Toast.LENGTH_SHORT).show();
                } else if (make.length() > 0 && model.length() == 0) {
                    // No model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
                } else if (make.length() == 0 && model.length() > 0) {
                    // No make was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
                } else if ((minAperturePicker.getValue() == displayedApertureValues.length-1 && maxAperturePicker.getValue() != displayedApertureValues.length-1)
                        ||
                        (minAperturePicker.getValue() != displayedApertureValues.length-1 && maxAperturePicker.getValue() == displayedApertureValues.length-1)){
                    // No min or max shutter was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxAperture), Toast.LENGTH_LONG).show();
                } else {
                    lens.setMake(make);
                    lens.setModel(model);
                    lens.setSerialNumber(serialNumber);
                    lens.setApertureIncrements(newApertureIncrements);

                    if ( minFocalLengthPicker.getValue() == 0 || maxFocalLengthPicker.getValue() == 0 ) {
                        lens.setMaxFocalLength(0);
                        lens.setMinFocalLength(0);
                    } else if ( minFocalLengthPicker.getValue() < maxFocalLengthPicker.getValue() ) {
                        lens.setMaxFocalLength(maxFocalLengthPicker.getValue());
                        lens.setMinFocalLength(minFocalLengthPicker.getValue());
                    } else {
                        lens.setMaxFocalLength(minFocalLengthPicker.getValue());
                        lens.setMinFocalLength(maxFocalLengthPicker.getValue());
                    }

                    if ( minAperturePicker.getValue() == displayedApertureValues.length-1 && maxAperturePicker.getValue() == displayedApertureValues.length-1 ) {
                        lens.setMinAperture(null);
                        lens.setMaxAperture(null);
                    }
                    if ( minAperturePicker.getValue() < maxAperturePicker.getValue() ) {
                        lens.setMinAperture(displayedApertureValues[minAperturePicker.getValue()]);
                        lens.setMaxAperture(displayedApertureValues[maxAperturePicker.getValue()]);
                    } else {
                        lens.setMinAperture(displayedApertureValues[maxAperturePicker.getValue()]);
                        lens.setMaxAperture(displayedApertureValues[minAperturePicker.getValue()]);
                    }
                    // Return the new entered name to the calling activity
                    Intent intent = new Intent();
                    intent.putExtra("LENS", lens);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                }
            }
        });
        return dialog;
    }
    
    private void initialiseApertureRangePickers(){
        switch ( newApertureIncrements ) {
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
        if ( displayedApertureValues[0].equals(getResources().getString(R.string.NoValue)) ) {
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
            if (displayedApertureValues[i].equals(lens.getMinAperture())) {
                minAperturePicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if (displayedApertureValues[i].equals(lens.getMaxAperture())) {
                maxAperturePicker.setValue(i);
                break;
            }
        }
    }

}
