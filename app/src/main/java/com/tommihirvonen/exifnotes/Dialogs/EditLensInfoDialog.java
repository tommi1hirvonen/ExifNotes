package com.tommihirvonen.exifnotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.NumberPicker;
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

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_make);
        et1.setText(lens.getMake());
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_model);
        et2.setText(lens.getModel());
        final EditText et3 = (EditText) inflator.findViewById(R.id.txt_serial_number);
        et3.setText(lens.getSerialNumber());

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
        final NumberPicker minAperturePicker = (NumberPicker) inflator.findViewById(R.id.minAperturePicker);
        final NumberPicker maxAperturePicker = (NumberPicker) inflator.findViewById(R.id.maxAperturePicker);
        final String[] allApertureValues = utilities.allApertureValues;
        Collections.reverse(Arrays.asList(allApertureValues));
        minAperturePicker.setMinValue(0);
        maxAperturePicker.setMinValue(0);
        minAperturePicker.setMaxValue(allApertureValues.length-1);
        maxAperturePicker.setMaxValue(allApertureValues.length-1);
        minAperturePicker.setDisplayedValues(allApertureValues);
        maxAperturePicker.setDisplayedValues(allApertureValues);
        minAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        maxAperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        minAperturePicker.setValue(allApertureValues.length-1);
        maxAperturePicker.setValue(allApertureValues.length-1);
        for (int i = 0; i < allApertureValues.length; ++i) {
            if (allApertureValues[i].equals(lens.getMinAperture())) {
                minAperturePicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i < allApertureValues.length; ++i) {
            if (allApertureValues[i].equals(lens.getMaxAperture())) {
                maxAperturePicker.setValue(i);
                break;
            }
        }

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
                } else if ((minAperturePicker.getValue() == allApertureValues.length-1 && maxAperturePicker.getValue() != allApertureValues.length-1)
                        ||
                        (minAperturePicker.getValue() != allApertureValues.length-1 && maxAperturePicker.getValue() == allApertureValues.length-1)){
                    // No min or max shutter was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxAperture), Toast.LENGTH_LONG).show();
                } else {
                    lens.setMake(make);
                    lens.setModel(model);
                    lens.setSerialNumber(serialNumber);

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

                    if ( minAperturePicker.getValue() == allApertureValues.length-1 && maxAperturePicker.getValue() == allApertureValues.length-1 ) {
                        lens.setMinAperture(null);
                        lens.setMaxAperture(null);
                    }
                    if ( minAperturePicker.getValue() < maxAperturePicker.getValue() ) {
                        lens.setMinAperture(allApertureValues[minAperturePicker.getValue()]);
                        lens.setMaxAperture(allApertureValues[maxAperturePicker.getValue()]);
                    } else {
                        lens.setMinAperture(allApertureValues[maxAperturePicker.getValue()]);
                        lens.setMaxAperture(allApertureValues[minAperturePicker.getValue()]);
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

}
