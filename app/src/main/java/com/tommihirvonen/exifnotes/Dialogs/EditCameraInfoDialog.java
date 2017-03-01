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

import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.Arrays;
import java.util.Collections;

public class EditCameraInfoDialog extends DialogFragment {

    public static final String TAG = "CameraInfoDialogFragment";

    Camera camera;
    Utilities utilities;
    int newShutterIncrements;
    String[] displayedShutterValues;

    TextView shutterSpeedIncrementsTextView;

    NumberPicker minShutterPicker;
    NumberPicker maxShutterPicker;

    public EditCameraInfoDialog(){

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        utilities = new Utilities(getActivity());

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(
                R.layout.camera_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        camera = getArguments().getParcelable("CAMERA");
        if (camera == null) camera = new Camera();

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = (FrameLayout) inflator.findViewById(R.id.root);
            NestedScrollView nestedScrollView = (NestedScrollView) inflator.findViewById(
                    R.id.nested_scroll_view);
            Utilities.setScrollIndicators(rootLayout, nestedScrollView,
                    ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
        }

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflator);

        //EDIT TEXT FIELDS
        final EditText makeEditText = (EditText) inflator.findViewById(R.id.txt_make);
        makeEditText.setText(camera.getMake());
        final EditText modelEditText = (EditText) inflator.findViewById(R.id.txt_model);
        modelEditText.setText(camera.getModel());
        final EditText serialNumberEditText = (EditText) inflator.findViewById(R.id.txt_serial_number);
        serialNumberEditText.setText(camera.getSerialNumber());

        //SHUTTER SPEED INCREMENTS BUTTON
        newShutterIncrements = camera.getShutterIncrements();
        shutterSpeedIncrementsTextView = (TextView) inflator.findViewById(R.id.btn_shutterSpeedIncrements);
        shutterSpeedIncrementsTextView.setClickable(true);
        shutterSpeedIncrementsTextView.setText(
                getResources().getStringArray(R.array.StopIncrements)[camera.getShutterIncrements()]);
        shutterSpeedIncrementsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseIncrements));
                builder.setItems(R.array.StopIncrements, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newShutterIncrements = i;
                        shutterSpeedIncrementsTextView.setText(
                                getResources().getStringArray(R.array.StopIncrements)[i]);
                        //Shutter speed increments were changed, make changes to shutter range pickers
                        initialiseShutterRangePickers();

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

        //SHUTTER RANGE NUMBER PICKERS
        minShutterPicker = (NumberPicker) inflator.findViewById(R.id.minShutterPicker);
        maxShutterPicker = (NumberPicker) inflator.findViewById(R.id.maxShutterPicker);
        initialiseShutterRangePickers();
        minShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        maxShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);


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
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel), Toast.LENGTH_SHORT).show();
                } else if (make.length() > 0 && model.length() == 0) {
                    // No model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
                } else if (make.length() == 0 && model.length() > 0) {
                    // No make was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
                } else if ((minShutterPicker.getValue() == displayedShutterValues.length-1 &&
                        maxShutterPicker.getValue() != displayedShutterValues.length-1)
                            ||
                            (minShutterPicker.getValue() != displayedShutterValues.length-1 &&
                                    maxShutterPicker.getValue() == displayedShutterValues.length-1)){
                    // No min or max shutter was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxShutter),
                            Toast.LENGTH_LONG).show();
                } else {
                    camera.setMake(make); camera.setModel(model);
                    camera.setSerialNumber(serialNumber);
                    camera.setShutterIncrements(newShutterIncrements);
                    if (minShutterPicker.getValue() == displayedShutterValues.length-1 &&
                            maxShutterPicker.getValue() == displayedShutterValues.length-1) {
                        camera.setMinShutter(null);
                        camera.setMaxShutter(null);
                    } else if (minShutterPicker.getValue() < maxShutterPicker.getValue()) {
                        camera.setMinShutter(displayedShutterValues[minShutterPicker.getValue()]);
                        camera.setMaxShutter(displayedShutterValues[maxShutterPicker.getValue()]);
                    } else {
                        camera.setMinShutter(displayedShutterValues[maxShutterPicker.getValue()]);
                        camera.setMaxShutter(displayedShutterValues[minShutterPicker.getValue()]);
                    }

                    // Return the new entered name to the calling activity
                    Intent intent = new Intent();
                    intent.putExtra("CAMERA", camera);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(
                            getTargetRequestCode(), Activity.RESULT_OK, intent);
                }

            }
        });
        return dialog;
    }

    private void initialiseShutterRangePickers() {
        switch (newShutterIncrements) {
            case 0:
                displayedShutterValues = utilities.shutterValuesThird;
                break;
            case 1:
                displayedShutterValues = utilities.shutterValuesHalf;
                break;
            case 2:
                displayedShutterValues = utilities.shutterValuesFull;
                break;
            default:
                displayedShutterValues = utilities.shutterValuesThird;
                break;
        }
        if (displayedShutterValues[0].equals(getResources().getString(R.string.NoValue))) {
            Collections.reverse(Arrays.asList(displayedShutterValues));
        }
        minShutterPicker.setMinValue(0);
        maxShutterPicker.setMinValue(0);
        minShutterPicker.setMaxValue(displayedShutterValues.length-1);
        maxShutterPicker.setMaxValue(displayedShutterValues.length-1);
        minShutterPicker.setDisplayedValues(displayedShutterValues);
        maxShutterPicker.setDisplayedValues(displayedShutterValues);
        minShutterPicker.setValue(displayedShutterValues.length-1);
        maxShutterPicker.setValue(displayedShutterValues.length-1);
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (displayedShutterValues[i].equals(camera.getMinShutter())) {
                minShutterPicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (displayedShutterValues[i].equals(camera.getMaxShutter())) {
                maxShutterPicker.setValue(i);
                break;
            }
        }
    }

}
