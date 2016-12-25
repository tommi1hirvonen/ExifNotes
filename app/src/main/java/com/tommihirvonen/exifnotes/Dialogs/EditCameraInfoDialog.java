package com.tommihirvonen.exifnotes.Dialogs;

//Copyright 2016
//Tommi Hirvonen

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

import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.Arrays;
import java.util.Collections;

public class EditCameraInfoDialog extends DialogFragment {

    public static final String TAG = "CameraInfoDialogFragment";

    Camera camera;
    Utilities utilities;

    public EditCameraInfoDialog(){

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        utilities = new Utilities(getActivity());

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(R.layout.camera_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        camera = getArguments().getParcelable("CAMERA");
        if (camera == null) camera = new Camera();


        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflator);

        //EDIT TEXT FIELDS
        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_make);
        et1.setText(camera.getMake());
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_model);
        et2.setText(camera.getModel());
        final EditText et3 = (EditText) inflator.findViewById(R.id.txt_serial_number);
        et3.setText(camera.getSerialNumber());

        //SHUTTER RANGE NUMBER PICKERS
        final NumberPicker minShutterPicker = (NumberPicker) inflator.findViewById(R.id.minShutterPicker);
        final NumberPicker maxShutterPicker = (NumberPicker) inflator.findViewById(R.id.maxShutterPicker);
        final String[] allShutterValuesNoBulb = utilities.allShutterValuesNoBulb;
        Collections.reverse(Arrays.asList(allShutterValuesNoBulb));
        minShutterPicker.setMinValue(0);
        maxShutterPicker.setMinValue(0);
        minShutterPicker.setMaxValue(allShutterValuesNoBulb.length-1);
        maxShutterPicker.setMaxValue(allShutterValuesNoBulb.length-1);
        minShutterPicker.setDisplayedValues(allShutterValuesNoBulb);
        maxShutterPicker.setDisplayedValues(allShutterValuesNoBulb);
        minShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        maxShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        minShutterPicker.setValue(allShutterValuesNoBulb.length-1);
        maxShutterPicker.setValue(allShutterValuesNoBulb.length-1);
        for (int i = 0; i < allShutterValuesNoBulb.length; ++i) {
            if (allShutterValuesNoBulb[i].equals(camera.getMinShutter())) {
                minShutterPicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i < allShutterValuesNoBulb.length; ++i) {
            if (allShutterValuesNoBulb[i].equals(camera.getMaxShutter())) {
                maxShutterPicker.setValue(i);
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
                } else if ((minShutterPicker.getValue() == allShutterValuesNoBulb.length-1 && maxShutterPicker.getValue() != allShutterValuesNoBulb.length-1)
                            ||
                            (minShutterPicker.getValue() != allShutterValuesNoBulb.length-1 && maxShutterPicker.getValue() == allShutterValuesNoBulb.length-1)){
                    // No min or max shutter was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxShutter), Toast.LENGTH_LONG).show();
                } else {
                    camera.setMake(make); camera.setModel(model);
                    camera.setSerialNumber(serialNumber);
                    if (minShutterPicker.getValue() < maxShutterPicker.getValue()) {
                        camera.setMinShutter(allShutterValuesNoBulb[minShutterPicker.getValue()]);
                        camera.setMaxShutter(allShutterValuesNoBulb[maxShutterPicker.getValue()]);
                    } else {
                        camera.setMinShutter(allShutterValuesNoBulb[maxShutterPicker.getValue()]);
                        camera.setMaxShutter(allShutterValuesNoBulb[minShutterPicker.getValue()]);
                    }

                    // Return the new entered name to the calling activity
                    Intent intent = new Intent();
                    intent.putExtra("CAMERA", camera);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                }

            }
        });
        return dialog;
    }

}
