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
import android.widget.Button;
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
    Button shutterRangeButton;

    String newMinShutter;
    String newMaxShutter;

    public EditCameraInfoDialog(){

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        utilities = new Utilities(getActivity());

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.camera_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        camera = getArguments().getParcelable("CAMERA");
        if (camera == null) camera = new Camera();

        newMinShutter = camera.getMinShutter();
        newMaxShutter = camera.getMaxShutter();

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

        //EDIT TEXT FIELDS
        final EditText makeEditText = (EditText) inflatedView.findViewById(R.id.txt_make);
        makeEditText.setText(camera.getMake());
        final EditText modelEditText = (EditText) inflatedView.findViewById(R.id.txt_model);
        modelEditText.setText(camera.getModel());
        final EditText serialNumberEditText = (EditText) inflatedView.findViewById(R.id.txt_serial_number);
        serialNumberEditText.setText(camera.getSerialNumber());

        //SHUTTER SPEED INCREMENTS BUTTON
        newShutterIncrements = camera.getShutterIncrements();
        shutterSpeedIncrementsTextView = (TextView) inflatedView.findViewById(R.id.btn_shutterSpeedIncrements);
        shutterSpeedIncrementsTextView.setClickable(true);
        shutterSpeedIncrementsTextView.setText(
                getResources().getStringArray(R.array.StopIncrements)[camera.getShutterIncrements()]);
        shutterSpeedIncrementsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int checkedItem = newShutterIncrements;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseIncrements));
                builder.setSingleChoiceItems(R.array.StopIncrements, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newShutterIncrements = i;
                        shutterSpeedIncrementsTextView.setText(
                                getResources().getStringArray(R.array.StopIncrements)[i]);

                        //Shutter speed increments were changed, make update
                        //Check if the new increments include both min and max values.
                        //Otherwise reset them to null
                        boolean minFound = false, maxFound = false;
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
                        for (String string : displayedShutterValues) {
                            if (!minFound && string.equals(newMinShutter)) minFound = true;
                            if (!maxFound && string.equals(newMaxShutter)) maxFound = true;
                            if (minFound && maxFound) break;
                        }
                        //If either one wasn't found in the new values array, null them.
                        if (!minFound || !maxFound) {
                            newMinShutter = null;
                            newMaxShutter = null;
                            updateShutterRangeButton();
                        }

                        dialogInterface.dismiss();
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

        //SHUTTER RANGE BUTTON
        shutterRangeButton = (Button) inflatedView.findViewById(R.id.btn_shutterRange);
        updateShutterRangeButton();
        shutterRangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.double_numberpicker_dialog, null);
                final NumberPicker minShutterPicker = (NumberPicker) dialogView.findViewById(R.id.number_picker_one);
                final NumberPicker maxShutterPicker = (NumberPicker) dialogView.findViewById(R.id.number_picker_two);

                //To prevent text edit
                minShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                maxShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

                initialiseShutterRangePickers(minShutterPicker, maxShutterPicker);

                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseShutterRange));
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
                        if ((minShutterPicker.getValue() == displayedShutterValues.length-1 &&
                                maxShutterPicker.getValue() != displayedShutterValues.length-1)
                                ||
                                (minShutterPicker.getValue() != displayedShutterValues.length-1 &&
                                        maxShutterPicker.getValue() == displayedShutterValues.length-1)) {
                            // No min or max shutter was set
                            Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxShutter),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            if (minShutterPicker.getValue() == displayedShutterValues.length-1 &&
                                    maxShutterPicker.getValue() == displayedShutterValues.length-1) {
                                newMinShutter = null;
                                newMaxShutter = null;
                            } else if (minShutterPicker.getValue() < maxShutterPicker.getValue()) {
                                newMinShutter = displayedShutterValues[minShutterPicker.getValue()];
                                newMaxShutter = displayedShutterValues[maxShutterPicker.getValue()];
                            } else {
                                newMinShutter = displayedShutterValues[maxShutterPicker.getValue()];
                                newMaxShutter = displayedShutterValues[minShutterPicker.getValue()];
                            }
                            updateShutterRangeButton();
                        }
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
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel), Toast.LENGTH_SHORT).show();
                } else if (make.length() > 0 && model.length() == 0) {
                    // No model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
                } else if (make.length() == 0 && model.length() > 0) {
                    // No make was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
                } else {
                    camera.setMake(make); camera.setModel(model);
                    camera.setSerialNumber(serialNumber);
                    camera.setShutterIncrements(newShutterIncrements);
                    camera.setMinShutter(newMinShutter);
                    camera.setMaxShutter(newMaxShutter);

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

    private void initialiseShutterRangePickers(NumberPicker minShutterPicker,
                                               NumberPicker maxShutterPicker) {
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
            if (displayedShutterValues[i].equals(newMinShutter)) {
                minShutterPicker.setValue(i);
                break;
            }
        }
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (displayedShutterValues[i].equals(newMaxShutter)) {
                maxShutterPicker.setValue(i);
                break;
            }
        }
    }

    private void updateShutterRangeButton(){
        shutterRangeButton.setText(newMinShutter == null || newMaxShutter == null ?
                getResources().getString(R.string.ClickToSet) :
                newMinShutter + " - " + newMaxShutter
        );
    }

}
