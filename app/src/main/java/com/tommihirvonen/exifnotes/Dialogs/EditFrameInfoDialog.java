package com.tommihirvonen.exifnotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.Datastructures.Filter;
import com.tommihirvonen.exifnotes.Datastructures.Frame;
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Activities.LocationPickActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class EditFrameInfoDialog extends DialogFragment {

    public static final String TAG = "EditFrameInfoDialogFragment";
    final static int PLACE_PICKER_REQUEST = 1;
    final static int ADD_LENS = 2;
    final static int ADD_FILTER = 3;

    String title;
    String positiveButton;
    long cameraId;
    Camera camera;
    Frame frame;
    List<Lens> mountableLenses;
    List<Filter> mountableFilters;
    FilmDbHelper database;

    TextView locationTextView;
    TextView lensTextView;
    TextView filterTextView;

    //These variables are used so that the object itself is not updated
    //unless the user presses ok.
    long newLensId;
    String newDate;
    String newLocation;
    long newFilterId;
    int apertureIncrements;
    int shutterIncrements;

    NumberPicker countPicker;
    NumberPicker shutterPicker;
    NumberPicker aperturePicker;
    NumberPicker focalLengthPicker;
    NumberPicker exposureCompPicker;
    NumberPicker noOfExposuresPicker;

    Utilities utilities;

    String[] displayedShutterValues;
    String[] displayedApertureValues;

    public EditFrameInfoDialog() {
        // Empty constructor required for DialogFragment
    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        utilities = new Utilities(getActivity());

        title = getArguments().getString("TITLE");
        positiveButton = getArguments().getString("POSITIVE_BUTTON");
        frame = getArguments().getParcelable("FRAME");
        if (frame == null) frame = new Frame();

        database = FilmDbHelper.getInstance(getActivity());
        cameraId = database.getRoll(frame.getRollId()).getCameraId();
        camera = database.getCamera(cameraId);
        mountableLenses = database.getMountableLenses(camera);

        shutterIncrements = camera.getShutterIncrements();
        apertureIncrements = 0;
        if (frame.getLensId() > 0) {
            apertureIncrements = database.getLens(frame.getLensId()).getApertureIncrements();
        }

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(
                R.layout.frame_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

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




        //LENS BUTTON
        lensTextView = (TextView) inflator.findViewById(R.id.btn_lens);
        if (frame.getLensId() > 0) {
            Lens currentLens = database.getLens(frame.getLensId());
            lensTextView.setText(currentLens.getMake() + " " + currentLens.getModel());
        }
        else lensTextView.setText(getResources().getString(R.string.NoLens));

        // LENS PICK DIALOG
        newLensId = frame.getLensId();
        lensTextView.setClickable(true);
        lensTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedItem = 0; // default option is 'no lens' (first one the list)
                final List<String> listItems = new ArrayList<>();
                listItems.add(getResources().getString(R.string.NoLens));
                for (int i = 0; i < mountableLenses.size(); ++i) {
                    listItems.add(mountableLenses.get(i).getMake() + " " +
                            mountableLenses.get(i).getModel());

                    //If the id's match, set the initial checkedItem.
                    // Account for the 'no lens' option with the + 1
                    if (mountableLenses.get(i).getId() == newLensId) checkedItem = i + 1;
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedLens);
                builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        lensTextView.setText(listItems.get(which));
                        if (which > 0) {
                            newLensId = mountableLenses.get(which - 1).getId();
                            initialiseFocalLengthPicker();
                            apertureIncrements = database.getLens(newLensId).getApertureIncrements();
                        }
                        else if (which == 0) {
                            newLensId = -1;
                            focalLengthPicker.setMinValue(0);
                            focalLengthPicker.setMaxValue(0);
                            focalLengthPicker.setValue(0);
                            apertureIncrements = 0;
                        }
                        initialiseAperturePicker();
                        initialiseFilters();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // LENS ADD DIALOG
        final Button addLensButton = (Button) inflator.findViewById(R.id.btn_add_lens);
        addLensButton.setClickable(true);
        addLensButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                EditLensInfoDialog dialog = new EditLensInfoDialog();
                dialog.setTargetFragment(EditFrameInfoDialog.this, ADD_LENS);
                Bundle arguments = new Bundle();
                arguments.putString("TITLE", getResources().getString(R.string.NewLens));
                arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditLensInfoDialog.TAG);
            }
        });

        // DATE PICK DIALOG
        final TextView dateTextView = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView timeTextView = (TextView) inflator.findViewById(R.id.btn_time);
        if (frame.getDate() == null) frame.setDate(Utilities.getCurrentTime());
        List<String> dateValue = Utilities.splitDate(frame.getDate());
        int tempYear = Integer.parseInt(dateValue.get(0));
        int tempMonth = Integer.parseInt(dateValue.get(1));
        int tempDay = Integer.parseInt(dateValue.get(2));
        dateTextView.setText(tempYear + "-" + tempMonth + "-" + tempDay);

        dateTextView.setClickable(true);

        newDate = frame.getDate();

        dateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DATE PICKER DIALOG IMPLEMENTATION HERE
                List<String> dateValue = Utilities.splitDate(newDate);
                int year = Integer.parseInt(dateValue.get(0));
                int month = Integer.parseInt(dateValue.get(1));
                int day = Integer.parseInt(dateValue.get(2));
                DatePickerDialog dialog = new DatePickerDialog(
                        getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String newInnerDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                        dateTextView.setText(newInnerDate);
                        newDate = newInnerDate + " " + timeTextView.getText().toString();
                    }
                    // One month has to be subtracted from the default shown month, otherwise
                    // the date picker shows one month forward.
                }, year, (month - 1), day);

                dialog.show();

            }
        });

        // TIME PICK DIALOG
        List<String> timeValue = Utilities.splitTime(frame.getDate());
        int tempHours = Integer.parseInt(timeValue.get(0));
        int tempMinutes = Integer.parseInt(timeValue.get(1));
        if (tempMinutes < 10) timeTextView.setText(tempHours + ":0" + tempMinutes);
        else timeTextView.setText(tempHours + ":" + tempMinutes);

        timeTextView.setClickable(true);

        timeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TIME PICKER DIALOG IMPLEMENTATION HERE
                List<String> timeValue = Utilities.splitTime(newDate);
                int hours = Integer.parseInt(timeValue.get(0));
                int minutes = Integer.parseInt(timeValue.get(1));
                TimePickerDialog dialog = new TimePickerDialog(
                        getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String newTime;
                        if (minute < 10) {
                            newTime = hourOfDay + ":0" + minute;
                        } else newTime = hourOfDay + ":" + minute;
                        timeTextView.setText(newTime);
                        newDate = dateTextView.getText().toString() + " " + newTime;
                    }
                }, hours, minutes, true);

                dialog.show();

            }
        });

        //NOTES FIELD
        final TextView noteTextView = (TextView) inflator.findViewById(R.id.txt_note);
        noteTextView.setText(frame.getNote());

        //SHUTTER SPEED PICKER
        shutterPicker = (NumberPicker) inflator.findViewById(R.id.shutterPicker);
        initialiseShutterPicker();
        // With the following command we can avoid popping up the keyboard
        shutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        //COUNT PICKER
        countPicker = (NumberPicker) inflator.findViewById(R.id.countPicker);
        countPicker.setMinValue(0);
        countPicker.setMaxValue(100);
        countPicker.setValue(frame.getCount());
        countPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        //APERTURE PICKER
        aperturePicker = (NumberPicker) inflator.findViewById(R.id.aperturePicker);
        initialiseAperturePicker();
        aperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        // LOCATION PICK DIALOG
        locationTextView = (TextView) inflator.findViewById(R.id.btn_location);
        newLocation = frame.getLocation();
        locationTextView.setText(frame.getLocation());
        locationTextView.setClickable(true);
        locationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // LOCATION PICKER DIALOG IMPLEMENTATION HERE
                final List<String> listItems = new ArrayList<>();
                listItems.add(getResources().getString(R.string.Clear));
                listItems.add(getResources().getString(R.string.Reacquire));
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseAction));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        switch (which) {
                            // Clear
                            case 0:
                                locationTextView.setText("");
                                newLocation = "";
                                break;

                            // Reacquire/Edit on map. LocationPickActivity!
                            case 1:
                                Intent intent = new Intent(getActivity(), LocationPickActivity.class);
                                intent.putExtra("LOCATION", newLocation);
                                startActivityForResult(intent, PLACE_PICKER_REQUEST);
                                break;
                        }
                    }
                });
                builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        //FOCAL LENGTH PICKER
        focalLengthPicker = (NumberPicker) inflator.findViewById(R.id.focalLengthPicker);
        //--------------------------------------------------------
        //This little trick is used to fix a bug which Google hasn't been able to fix in five years.
        //https://code.google.com/p/android/issues/detail?id=35482
        //Seriously, what the hell!?
        //Initially the NumberPicker shows the wrong value, but when the Picker is first scrolled,
        //the displayed value changes to the correct one.
        Field field = null;
        try {
            field = NumberPicker.class.getDeclaredField("mInputText");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if (field != null) {
            field.setAccessible(true);
            EditText inputText = null;
            try {
                inputText = (EditText) field.get(focalLengthPicker);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (inputText != null) inputText.setFilters(new InputFilter[0]);
        }
        //--------------------------------------------------------
        if (frame.getLensId() > 0) {
            focalLengthPicker.setMinValue(database.getLens(frame.getLensId()).getMinFocalLength());
            focalLengthPicker.setMaxValue(database.getLens(frame.getLensId()).getMaxFocalLength());
            focalLengthPicker.setValue(frame.getFocalLength());
        }
        focalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        //EXPOSURE COMP PICKER
        exposureCompPicker = (NumberPicker) inflator.findViewById(R.id.exposureCompPicker);
        if (field != null) {
            field.setAccessible(true);
            EditText inputText = null;
            try {
                inputText = (EditText) field.get(exposureCompPicker);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (inputText != null) inputText.setFilters(new InputFilter[0]);
        }
        exposureCompPicker.setMinValue(0);
        exposureCompPicker.setMaxValue(Utilities.compValues.length-1);
        exposureCompPicker.setDisplayedValues(Utilities.compValues);
        exposureCompPicker.setValue(9);
        if (frame.getExposureComp() != null) {
            for (int i = 0; i < Utilities.compValues.length; ++i) {
                if (frame.getExposureComp().equals(Utilities.compValues[i])) {
                    exposureCompPicker.setValue(i);
                    break;
                }
            }
        }
        exposureCompPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        //NO OF EXPOSURES PICKER
        noOfExposuresPicker = (NumberPicker) inflator.findViewById(R.id.noOfExposuresPicker);
        noOfExposuresPicker.setMinValue(1);
        noOfExposuresPicker.setMaxValue(10);
        noOfExposuresPicker.setValue(1);
        if (frame.getNoOfExposures() > 1) {
            noOfExposuresPicker.setValue(frame.getNoOfExposures());
        }
        noOfExposuresPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        //FILTER BUTTON
        filterTextView = (TextView) inflator.findViewById(R.id.btn_filter);
        if (frame.getFilterId() > 0) {
            Filter currentFilter = database.getFilter(frame.getFilterId());
            filterTextView.setText(currentFilter.getMake() + " " + currentFilter.getModel());
        }
        else {
            filterTextView.setText(getResources().getString(R.string.NoFilter));
        }

        // FILTER PICK DIALOG
        newFilterId = frame.getFilterId();
        filterTextView.setClickable(true);
        filterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedItem = 0; //default option is 0, no filter
                final List<String> listItems = new ArrayList<>();
                listItems.add(getResources().getString(R.string.NoFilter));
                if (newLensId > 0) {
                    mountableFilters = database.getMountableFilters(database.getLens(newLensId));
                    for (int i = 0; i < mountableFilters.size(); ++i) {
                        listItems.add(mountableFilters.get(i).getMake() + " " +
                                mountableFilters.get(i).getModel());
                        if (mountableFilters.get(i).getId() == newFilterId) checkedItem = i + 1;
                    }
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedFilter);
                builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        filterTextView.setText(listItems.get(which));
                        if (which > 0) {
                            newFilterId = mountableFilters.get(which - 1).getId();
                        }
                        else if (which == 0) {
                            newFilterId = -1;
                        }
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // FILTER ADD DIALOG
        final Button addFilterButton = (Button) inflator.findViewById(R.id.btn_add_filter);
        addFilterButton.setClickable(true);
        addFilterButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                if (newLensId <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.SelectLensToAddFilters),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                EditFilterInfoDialog dialog = new EditFilterInfoDialog();
                dialog.setTargetFragment(EditFrameInfoDialog.this, ADD_FILTER);
                Bundle arguments = new Bundle();
                arguments.putString("TITLE", getResources().getString(R.string.NewFilter));
                arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditFilterInfoDialog.TAG);
            }
        });






        //FINALISE BUILDING THE DIALOG

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
        });

        alert.setPositiveButton(positiveButton, null);

        final AlertDialog dialog = alert.create();

        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();


        // We override the positive button onClick so that we can dismiss the dialog
        // only if the note does not contain illegal characters
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frame.setShutter( displayedShutterValues[shutterPicker.getValue()]);
                frame.setAperture(displayedApertureValues[aperturePicker.getValue()]);
                frame.setCount(countPicker.getValue());
                frame.setNote(noteTextView.getText().toString());

                // PARSE THE DATE
                frame.setDate(newDate);

                frame.setLensId(newLensId);
                frame.setLocation(newLocation);
                frame.setFilterId(newFilterId);
                frame.setExposureComp(Utilities.compValues[exposureCompPicker.getValue()]);
                frame.setNoOfExposures(noOfExposuresPicker.getValue());
                frame.setFocalLength(focalLengthPicker.getValue());

                // Return the new entered name to the calling activity
                Intent intent = new Intent();
                intent.putExtra("FRAME", frame);
                dialog.dismiss();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, intent);

            }
        });

        return dialog;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data.hasExtra("LATITUDE") && data.hasExtra("LONGITUDE")) {
                newLocation = "" + data.getStringExtra("LATITUDE") + " " +
                        data.getStringExtra("LONGITUDE");
                locationTextView.setText(newLocation);
            }
        }

        if (requestCode == ADD_LENS && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            Lens lens = data.getParcelableExtra("LENS");

            long rowId = database.addLens(lens);
            lens.setId(rowId);
            database.addMountable(database.getCamera(cameraId), lens);
            mountableLenses.add(lens);
            lensTextView.setText(lens.getMake() + " " + lens.getModel());
            newLensId = lens.getId();
            apertureIncrements = lens.getApertureIncrements();
            initialiseAperturePicker();
            initialiseFocalLengthPicker();
            initialiseFilters();
        }

        if (requestCode == ADD_FILTER && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            Filter filter = data.getParcelableExtra("FILTER");
            long rowId = database.addFilter(filter);
            filter.setId(rowId);
            database.addMountableFilterLens(filter, database.getLens(newLensId));
            mountableFilters.add(filter);
            filterTextView.setText(filter.getMake() + " " + filter.getModel());
            newFilterId = filter.getId();
        }
    }

    @SuppressWarnings("ManualArrayToCollectionCopy")
    private void initialiseAperturePicker(){
        switch (apertureIncrements) {
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
            // By reversing the order we can reverse the order in the NumberPicker too
            Collections.reverse(Arrays.asList(displayedApertureValues));
        }

        //If no lens is selected, end here
        if (newLensId <= 0) return;

        //Otherwise continue to set min and max apertures
        Lens lens = database.getLens(newLensId);
        List<String> apertureValuesList = new ArrayList<>();
        int minIndex = 0;
        int maxIndex = displayedApertureValues.length-1;
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if (lens.getMinAperture().equals(displayedApertureValues[i])) {
                minIndex = i;
            }
            if (lens.getMaxAperture().equals(displayedApertureValues[i])) {
                maxIndex = i;
            }
        }
        apertureValuesList.add(getResources().getString(R.string.NoValue));
        for (int i = minIndex; i <= maxIndex; ++i) {
            apertureValuesList.add(displayedApertureValues[i]);
        }
        displayedApertureValues = apertureValuesList.toArray(new String[0]);
        //Set the displayed values to null. If we set the displayed values
        //and the maxValue is smaller than the length of the new displayed values array,
        //ArrayIndexOutOfBounds is thrown.
        //Also if we set maxValue and the currently displayed values array length is smaller,
        //ArrayIndexOutOfBounds is thrown.
        //Setting displayed values to null solves this problem.
        aperturePicker.setDisplayedValues(null);
        aperturePicker.setMinValue(0);
        aperturePicker.setMaxValue(displayedApertureValues.length-1);
        aperturePicker.setDisplayedValues(displayedApertureValues);
        aperturePicker.setValue(0);
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if (frame.getAperture().equals(displayedApertureValues[i])) {
                aperturePicker.setValue(i);
            }
        }
    }

    @SuppressWarnings("ManualArrayToCollectionCopy")
    private void initialiseShutterPicker(){
        // Set the increments according to settings
        switch (shutterIncrements) {
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
        // By reversing the order we can reverse the order in the NumberPicker too
        Collections.reverse(Arrays.asList(displayedShutterValues));
        List<String> shutterValuesList = new ArrayList<>();
        int minIndex = 0;
        int maxIndex = displayedShutterValues.length-1;
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (camera.getMinShutter().equals(displayedShutterValues[i])) {
                minIndex = i;
            }
            if (camera.getMaxShutter().equals(displayedShutterValues[i])) {
                maxIndex = i;
            }
        }
        shutterValuesList.add(getResources().getString(R.string.NoValue));
        for (int i = minIndex; i <= maxIndex; ++i) {
            shutterValuesList.add(displayedShutterValues[i]);
        }
        shutterValuesList.add("B");
        displayedShutterValues = shutterValuesList.toArray(new String[0]);
        shutterPicker.setMinValue(0);
        shutterPicker.setMaxValue(displayedShutterValues.length-1);
        shutterPicker.setDisplayedValues(displayedShutterValues);
        shutterPicker.setValue(0);
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (frame.getShutter().equals(displayedShutterValues[i])) {
                shutterPicker.setValue(i);
            }
        }
    }

    private void initialiseFocalLengthPicker(){
        Lens lens = database.getLens(newLensId);
        if (focalLengthPicker.getValue() > lens.getMaxFocalLength()) {
            focalLengthPicker.setValue(lens.getMaxFocalLength());
        } else if (focalLengthPicker.getValue() < lens.getMinFocalLength()) {
            focalLengthPicker.setValue(lens.getMinFocalLength());
        }
        focalLengthPicker.setMinValue(lens.getMinFocalLength());
        focalLengthPicker.setMaxValue(lens.getMaxFocalLength());
    }

    private void initialiseFilters(){
        //Update mountable filters
        if (newLensId <= 0) {
            if (mountableFilters != null) mountableFilters.clear();
            filterTextView.setText(getResources().getString(R.string.NoFilter));
            newFilterId = -1;
        } else {
            mountableFilters = database.getMountableFilters(database.getLens(newLensId));
            //If the new list contains the current filter, do nothing (return)
            for (Filter filter : mountableFilters) {
                if (filter.getId() == newFilterId) {
                    return;
                }
            }
            //Else reset the filter
            filterTextView.setText(getResources().getString(R.string.NoFilter));
            newFilterId = -1;
        }
    }
}

