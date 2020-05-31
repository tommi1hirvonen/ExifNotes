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

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dialog to edit Camera's information
 */
public class EditCameraDialog extends DialogFragment {

    /**
     * Public constant used to tag the fragment when created
     */
    public static final String TAG = "EditCameraDialog";

    /**
     * Holds the information of the edited Camera
     */
    private Camera camera;

    /**
     * Stores the currently selected shutter speed value increment setting
     */
    private int newShutterIncrements;

    /**
     * Stores the currently selected exposure compensation increment setting
     */
    private int newExposureCompIncrements;

    /**
     * Stores the currently displayed shutter speed values.
     * Changes depending on the currently selected shutter increments
     */
    private String[] displayedShutterValues;

    /**
     * Reference to the TextView to display the shutter speed range
     */
    private TextView shutterRangeTextView;

    /**
     * Currently selected minimum shutter speed (shortest duration)
     */
    private String newMinShutter;

    /**
     * Currently selected maximum shutter speed (longest duration)
     */
    private String newMaxShutter;

    /**
     * Empty constructor
     */
    public EditCameraDialog(){

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (final Bundle SavedInstanceState) {

        // Inflate the fragment. Get the edited camera.
        // Initialize the UI objects and display the camera's information.
        // Add listeners to Buttons to open new dialogs to change the camera's settings.

        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.dialog_camera, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        final String title = getArguments().getString(ExtraKeys.TITLE);
        final String positiveButton = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        camera = getArguments().getParcelable(ExtraKeys.CAMERA);
        if (camera == null) camera = new Camera();

        newMinShutter = camera.getMinShutter();
        newMaxShutter = camera.getMaxShutter();

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
        if (Utilities.isAppThemeDark(getActivity().getApplicationContext())) {
            final List<View> dividerList = new ArrayList<>();
            dividerList.add(inflatedView.findViewById(R.id.divider_view1));
            dividerList.add(inflatedView.findViewById(R.id.divider_view2));
            dividerList.add(inflatedView.findViewById(R.id.divider_view3));
            dividerList.add(inflatedView.findViewById(R.id.divider_view4));
            for (final View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
        }
        //==========================================================================================

        //EDIT TEXT FIELDS
        final EditText makeEditText = inflatedView.findViewById(R.id.make_editText);
        makeEditText.setText(camera.getMake());
        final EditText modelEditText = inflatedView.findViewById(R.id.model_editText);
        modelEditText.setText(camera.getModel());
        final EditText serialNumberEditText = inflatedView.findViewById(R.id.serialNumber_editText);
        serialNumberEditText.setText(camera.getSerialNumber());

        //SHUTTER SPEED INCREMENTS BUTTON
        newShutterIncrements = camera.getShutterIncrements();
        final TextView shutterSpeedIncrementsTextView = inflatedView.findViewById(R.id.increment_text);
        shutterSpeedIncrementsTextView.setText(
                getResources().getStringArray(R.array.StopIncrements)[camera.getShutterIncrements()]);

        final LinearLayout shutterSpeedIncrementLayout = inflatedView.findViewById(R.id.increment_layout);
        shutterSpeedIncrementLayout.setOnClickListener(view -> {
            final int checkedItem = newShutterIncrements;
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(R.string.ChooseIncrements));
            builder.setSingleChoiceItems(R.array.StopIncrements, checkedItem, (dialogInterface, i) -> {
                newShutterIncrements = i;
                shutterSpeedIncrementsTextView.setText(
                        getResources().getStringArray(R.array.StopIncrements)[i]);

                //Shutter speed increments were changed, make update
                //Check if the new increments include both min and max values.
                //Otherwise reset them to null
                boolean minFound = false, maxFound = false;
                switch (newShutterIncrements) {
                    case 1:
                        displayedShutterValues = getActivity().getResources()
                                .getStringArray(R.array.ShutterValuesHalf);
                        break;
                    case 2:
                        displayedShutterValues = getActivity().getResources()
                                .getStringArray(R.array.ShutterValuesFull);
                        break;
                    case 0:
                    default:
                        displayedShutterValues = getActivity().getResources()
                                .getStringArray(R.array.ShutterValuesThird);
                        break;
                }
                for (final String string : displayedShutterValues) {
                    if (!minFound && string.equals(newMinShutter)) minFound = true;
                    if (!maxFound && string.equals(newMaxShutter)) maxFound = true;
                    if (minFound && maxFound) break;
                }
                //If either one wasn't found in the new values array, null them.
                if (!minFound || !maxFound) {
                    newMinShutter = null;
                    newMaxShutter = null;
                    updateShutterRangeTextView();
                }

                dialogInterface.dismiss();
            });
            builder.setNegativeButton(getResources().getString(R.string.Cancel), (dialogInterface, i) -> {
                //Do nothing
            });
            builder.create().show();
        });

        //SHUTTER RANGE BUTTON
        shutterRangeTextView = inflatedView.findViewById(R.id.shutter_range_text);
        updateShutterRangeTextView();
        final LinearLayout shutterRangeLayout = inflatedView.findViewById(R.id.shutter_range_layout);
        shutterRangeLayout.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_double_numberpicker, null);
            final NumberPicker minShutterPicker = dialogView.findViewById(R.id.number_picker_one);
            final NumberPicker maxShutterPicker = dialogView.findViewById(R.id.number_picker_two);

            final int color = Utilities.isAppThemeDark(getActivity().getApplicationContext()) ?
                    ContextCompat.getColor(getActivity(), R.color.light_grey) :
                    ContextCompat.getColor(getActivity(), R.color.grey);
            final ImageView dash = dialogView.findViewById(R.id.dash);
            Utilities.setColorFilter(dash.getDrawable().mutate(), color);

            //To prevent text edit
            minShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            maxShutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

            initialiseShutterRangePickers(minShutterPicker, maxShutterPicker);

            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseShutterRange));
            builder.setPositiveButton(getResources().getString(R.string.OK), null);
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
            //Override the positiveButton to check the range before accepting.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                if ((minShutterPicker.getValue() == displayedShutterValues.length - 1 &&
                        maxShutterPicker.getValue() != displayedShutterValues.length - 1)
                        ||
                        (minShutterPicker.getValue() != displayedShutterValues.length - 1 &&
                                maxShutterPicker.getValue() == displayedShutterValues.length - 1)) {
                    // No min or max shutter was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMinOrMaxShutter),
                            Toast.LENGTH_LONG).show();
                } else {
                    if (minShutterPicker.getValue() == displayedShutterValues.length - 1 &&
                            maxShutterPicker.getValue() == displayedShutterValues.length - 1) {
                        newMinShutter = null;
                        newMaxShutter = null;
                    } else if (minShutterPicker.getValue() < maxShutterPicker.getValue()) {
                        newMinShutter = displayedShutterValues[minShutterPicker.getValue()];
                        newMaxShutter = displayedShutterValues[maxShutterPicker.getValue()];
                    } else {
                        newMinShutter = displayedShutterValues[maxShutterPicker.getValue()];
                        newMaxShutter = displayedShutterValues[minShutterPicker.getValue()];
                    }
                    updateShutterRangeTextView();
                }
                dialog.dismiss();
            });
        });


        //EXPOSURE COMPENSATION INCREMENTS BUTTON
        newExposureCompIncrements = camera.getExposureCompIncrements();
        final TextView exposureCompIncrementsTextView = inflatedView.findViewById(R.id.exposure_comp_increment_text);
        exposureCompIncrementsTextView.setText(
                getResources().getStringArray(R.array.ExposureCompIncrements)[camera.getExposureCompIncrements()]);
        final LinearLayout exposureCompIncrementLayout = inflatedView.findViewById(R.id.exposure_comp_increment_layout);
        exposureCompIncrementLayout.setOnClickListener(view -> {
            final int checkedItem = newExposureCompIncrements;
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(R.string.ChooseIncrements));
            builder.setSingleChoiceItems(R.array.ExposureCompIncrements, checkedItem, (dialogInterface, i) -> {
                newExposureCompIncrements = i;
                exposureCompIncrementsTextView.setText(
                        getResources().getStringArray(R.array.ExposureCompIncrements)[i]);
                dialogInterface.dismiss();
            });
            builder.setNegativeButton(getResources().getString(R.string.Cancel), (dialogInterface, i) -> {
                //Do nothing
            });
            builder.create().show();
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
                Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel), Toast.LENGTH_SHORT).show();
            } else if (make.length() > 0 && model.length() == 0) {
                // No model was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
            } else if (make.length() == 0) {
                // No make was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
            } else {
                camera.setMake(make); camera.setModel(model);
                camera.setSerialNumber(serialNumber);
                camera.setShutterIncrements(newShutterIncrements);
                camera.setMinShutter(newMinShutter);
                camera.setMaxShutter(newMaxShutter);
                camera.setExposureCompIncrements(newExposureCompIncrements);

                // Return the new entered name to the calling activity
                final Intent intent = new Intent();
                intent.putExtra(ExtraKeys.CAMERA, camera);
                dialog.dismiss();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, intent);
            }

        });
        return dialog;
    }

    /**
     * Called when the shutter speed range dialog is opened.
     * Sets the values for the NumberPickers.
     *
     * @param minShutterPicker NumberPicker associated with the minimum shutter speed
     * @param maxShutterPicker NumberPicker associated with the maximum shutter speed
     */
    private void initialiseShutterRangePickers(final NumberPicker minShutterPicker,
                                               final NumberPicker maxShutterPicker) {
        switch (newShutterIncrements) {
            case 1:
                displayedShutterValues = getActivity().getResources()
                        .getStringArray(R.array.ShutterValuesHalf);
                break;
            case 2:
                displayedShutterValues = getActivity().getResources()
                        .getStringArray(R.array.ShutterValuesFull);
                break;
            case 0:
            default:
                displayedShutterValues = getActivity().getResources()
                        .getStringArray(R.array.ShutterValuesThird);
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

    /**
     * Update the shutter speed range Button to display the currently selected shutter speed range.
     */
    private void updateShutterRangeTextView(){
        shutterRangeTextView.setText(newMinShutter == null || newMaxShutter == null ?
                getResources().getString(R.string.ClickToSet) :
                newMinShutter + " - " + newMaxShutter
        );
    }

}
