package com.tommihirvonen.exifnotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.tommihirvonen.exifnotes.Datastructures.Frame;
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Fragments.FramesFragment;
import com.tommihirvonen.exifnotes.Activities.LocationPickActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class EditFrameInfoDialog extends DialogFragment {

    String title;
    String positiveButton;
    long camera_id;
    Frame frame;
    ArrayList<Lens> mountableLenses;
    FilmDbHelper database;

    TextView b_location;
    TextView b_lens;

    final static int PLACE_PICKER_REQUEST = 1;
    final static int ADD_LENS = 2;

    public static final String TAG = "EditFrameInfoDialogFragment";


    public EditFrameInfoDialog() {
        // Empty constructor required for DialogFragment
    }

    NumberPicker countPicker;
    NumberPicker shutterPicker;
    NumberPicker aperturePicker;

    String[] displayedShutterValues;
    String[] displayedApertureValues;

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        // Let's check what values the user has set for the shutter and aperture increments
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String shutterIncrements = prefs.getString("ShutterIncrements", "third");
        String apertureIncrements = prefs.getString("ApertureIncrements", "third");

        title = getArguments().getString("TITLE");
        positiveButton = getArguments().getString("POSITIVE_BUTTON");
        frame = getArguments().getParcelable("FRAME");
        if (frame == null) frame = new Frame();

        database = new FilmDbHelper(getActivity());
        camera_id = database.getRoll(frame.getRollId()).getCamera_id();
        mountableLenses = database.getMountableLenses(database.getCamera(camera_id));

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(R.layout.frame_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        // Set ScrollIndicators
        FrameLayout rootLayout = (FrameLayout) inflator.findViewById(R.id.root);
        NestedScrollView nestedScrollView = (NestedScrollView) inflator.findViewById(R.id.nested_scroll_view);
        Utilities.setScrollIndicators(rootLayout, nestedScrollView,
                ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflator);

        final TextView et_note = (TextView) inflator.findViewById(R.id.txt_note);
        et_note.setText(frame.getNote());
        b_location = (TextView) inflator.findViewById(R.id.btn_location);

        b_lens = (TextView) inflator.findViewById(R.id.btn_lens);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);
        final Button b_addLens = (Button) inflator.findViewById(R.id.btn_add_lens);

        countPicker = (NumberPicker) inflator.findViewById(R.id.countPicker);
        shutterPicker = (NumberPicker) inflator.findViewById(R.id.shutterPicker);
        aperturePicker = (NumberPicker) inflator.findViewById(R.id.aperturePicker);

        // Shutter values in 1/3 increments
        final String[] shutterValuesThird = new String[]{getActivity().getString(R.string.NoValue), "B", "30\"", "25\"", "20\"", "15\"", "13\"", "10\"", "8\"", "6\"", "5\"", "4\"",
                                                    "3.2\"", "2.5\"", "2\"", "1.6\"", "1.3\"", "1\"", "0.8\"", "0.6\"", "1/2", "0.4\"", "0.3\"",
                                                    "1/4", "1/5", "1/6", "1/8", "1/10", "1/13", "1/15", "1/20", "1/25",
                                                    "1/30", "1/40", "1/50", "1/60", "1/80", "1/100", "1/125", "1/160", "1/200",
                                                    "1/250", "1/320", "1/400", "1/500", "1/640", "1/800", "1/1000", "1/1250",
                                                    "1/1600", "1/2000", "1/2500", "1/3200", "1/4000", "1/5000", "1/6400", "1/8000"};
        // Shutter values in 1/2 increments
        final String[] shutterValuesHalf = new String[]{ getActivity().getString(R.string.NoValue), "B", "30\"", "20\"", "15\"", "10\"", "8\"", "6\"", "4\"", "3\"", "2\"", "1.5\"",
                                                    "1\"", "0.7\"", "1/2", "1/3", "1/4", "1/6", "1/8", "1/10", "1/15", "1/20",
                                                    "1/30", "1/45", "1/60", "1/90", "1/125", "1/180", "1/250", "1/350",
                                                    "1/500", "1/750", "1/1000", "1/1500", "1/2000", "1/3000", "1/4000", "1/6000", "1/8000" };
        // Shutter values in full stop increments
        final String[] shutterValuesFull = new String[]{ getActivity().getString(R.string.NoValue), "B", "30\"", "15\"", "8\"", "4\"", "2\"", "1\"", "1/2", "1/4", "1/8",
                                                        "1/15", "1/30", "1/60", "1/125", "1/250", "1/500", "1/1000", "1/2000", "1/4000", "1/8000" };

        // Set the increments according to settings
        switch (shutterIncrements) {
            case "third":
                displayedShutterValues = shutterValuesThird;
                break;
            case "half":
                displayedShutterValues = shutterValuesHalf;
                break;
            case "full":
                displayedShutterValues = shutterValuesFull;
                break;
            default:
                displayedShutterValues = shutterValuesThird;
                break;
        }
        // By reversing the order we can reverse the order in the numberpicker too
        Collections.reverse(Arrays.asList(displayedShutterValues));

        shutterPicker.setMinValue(0);
        shutterPicker.setMaxValue(displayedShutterValues.length - 1);
        shutterPicker.setDisplayedValues(displayedShutterValues);
        // With the following command we can avoid popping up the keyboard
        shutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        shutterPicker.setValue(displayedShutterValues.length - 1);
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (displayedShutterValues[i].equals(frame.getShutter()) ) {
                shutterPicker.setValue(i);
                break;
            }
        }

        final String[] apertureValuesThird = new String[]{getActivity().getString(R.string.NoValue), "1.0", "1.1", "1.2", "1.4", "1.6", "1.8", "2.0", "2.2", "2.5",
                                                        "2.8", "3.2", "3.5", "4.0", "4.5", "5.0", "5.6", "6.3", "7.1", "8", "9",
                                                        "10", "11", "13", "14", "16", "18", "20", "22", "25", "29", "32", "36",
                                                        "42", "45", "50", "57", "64"};
        final String[] apertureValuesHalf = new String[]{ getActivity().getString(R.string.NoValue), "1.0", "1.2", "1.4", "1.8", "2.0", "2.5", "2.8", "3.5",
                                                            "4.0", "4.5", "5.6", "6.7", "8", "9.5", "11", "13", "16", "19",
                                                            "22", "27", "32", "38", "45", "64" };
        final String[] apertureValuesFull = new String[]{ getActivity().getString(R.string.NoValue), "1.0", "1.4", "2.0", "2.8", "4.0", "5.6", "8", "11",
                                                            "16", "22", "32", "45", "64" };

        // Set the increments according to settings
        switch (apertureIncrements) {
            case "third":
                displayedApertureValues = apertureValuesThird;
                break;
            case "half":
                displayedApertureValues = apertureValuesHalf;
                break;
            case "full":
                displayedApertureValues = apertureValuesFull;
                break;
            default:
                displayedApertureValues = apertureValuesThird;
                break;
        }
        // By reversing the order we can reverse the order in the numberpicker too
        Collections.reverse(Arrays.asList(displayedApertureValues));

        aperturePicker.setMinValue(0);
        aperturePicker.setMaxValue(displayedApertureValues.length - 1);
        aperturePicker.setDisplayedValues(displayedApertureValues);
        aperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        aperturePicker.setValue(displayedApertureValues.length - 1);
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if ( displayedApertureValues[i].equals(frame.getAperture()) ) {
                aperturePicker.setValue(i);
                break;
            }
        }

        countPicker.setMinValue(0);
        countPicker.setMaxValue(100);
        countPicker.setValue(frame.getCount());
        countPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);


        if ( frame.getLensId() > 0 ) {
            Lens currentLens = database.getLens(frame.getLensId());
            b_lens.setText(currentLens.getMake() + " " + currentLens.getModel());
        }
        else b_lens.setText(getResources().getString(R.string.NoLens));

        // LENS PICK DIALOG
        b_lens.setClickable(true);
        b_lens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> listItems = new ArrayList<>();
                listItems.add(getResources().getString(R.string.NoLens));
                for (int i = 0; i < mountableLenses.size(); ++i) {
                    listItems.add(mountableLenses.get(i).getMake() + " " + mountableLenses.get(i).getModel());
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedLens);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        b_lens.setText(listItems.get(which));
                        if (which > 0) frame.setLensId(mountableLenses.get(which - 1).getId());
                        else if (which == 0) frame.setLensId(-1);
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
        b_addLens.setClickable(true);
        b_addLens.setOnClickListener(new View.OnClickListener() {
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

        // The date is in format "YYYY-M-D HH:MM"

        int temp_year;
        int temp_month;
        int temp_day;

        if ( frame.getDate().length() > 0 ) {
            final ArrayList<String> dateValue = Utilities.splitDate(frame.getDate());
            temp_year = Integer.parseInt(dateValue.get(0));
            temp_month = Integer.parseInt(dateValue.get(1));
            temp_day = Integer.parseInt(dateValue.get(2));
            b_date.setText(temp_year + "-" + temp_month + "-" + temp_day);
        } else {
            frame.setDate(FramesFragment.getCurrentTime());

            ArrayList<String> dateValue = Utilities.splitDate(frame.getDate());
            temp_year = Integer.parseInt(dateValue.get(0));
            temp_month = Integer.parseInt(dateValue.get(1));
            temp_day = Integer.parseInt(dateValue.get(2));
            b_date.setText(temp_year + "-" + temp_month + "-" + temp_day);
        }
        b_date.setClickable(true);

        final int i_year = temp_year;
        final int i_month = temp_month;
        final int i_day = temp_day;

        b_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DATE PICKER DIALOG IMPLEMENTATION HERE
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String newDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                        b_date.setText(newDate);
                        frame.setDate(newDate + " " + b_time.getText().toString());
                    }
                    // One month has to be subtracted from the default shown month, otherwise
                    // the date picker shows one month forward.
                }, i_year, (i_month - 1), i_day);

                dialog.show();

            }
        });

        // TIME PICK DIALOG
        int temp_hours;
        int temp_minutes;

        if ( frame.getDate().length() > 0 ) {
            ArrayList<String> timeValue = Utilities.splitTime(frame.getDate());
            temp_hours = Integer.parseInt(timeValue.get(0));
            temp_minutes = Integer.parseInt(timeValue.get(1));
            if (temp_minutes < 10) b_time.setText(temp_hours + ":0" + temp_minutes);
            else b_time.setText(temp_hours + ":" + temp_minutes);
        } else {
            ArrayList<String> timeValue = Utilities.splitTime(frame.getDate());
            temp_hours = Integer.parseInt(timeValue.get(0));
            temp_minutes = Integer.parseInt(timeValue.get(1));
            if (temp_minutes < 10) b_time.setText(temp_hours + ":0" + temp_minutes);
            else b_time.setText(temp_hours + ":" + temp_minutes);
        }
        b_time.setClickable(true);

        final int hours = temp_hours;
        final int minutes = temp_minutes;

        b_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TIME PICKER DIALOG IMPLEMENTATION HERE
                TimePickerDialog dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String newTime;
                        if (minute < 10) {
                            newTime = hourOfDay + ":0" + minute;
                        } else newTime = hourOfDay + ":" + minute;
                        b_time.setText(newTime);
                        frame.setDate(b_date.getText().toString() + " " + newTime);
                    }
                }, hours, minutes, true);

                dialog.show();

            }
        });

        // LOCATION PICK DIALOG
        b_location.setText(frame.getLocation());
        b_location.setClickable(true);
        b_location.setOnClickListener(new View.OnClickListener() {
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
                                b_location.setText("");
                                frame.setLocation("");
                                break;

                            // Reacquire/Edit on map. LocationPickActivity!
                            case 1:
                                Intent intent = new Intent(getActivity(), LocationPickActivity.class);
                                intent.putExtra("LOCATION", frame.getLocation());
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

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
        });

        alert.setPositiveButton(positiveButton, null);

        final AlertDialog dialog = alert.create();

        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();


        // We override the positive button onClick so that we can dismiss the dialog
        // only if the note does not contain illegal characters
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frame.setShutter( displayedShutterValues[shutterPicker.getValue()]);
                frame.setAperture(displayedApertureValues[aperturePicker.getValue()]);
                frame.setCount(countPicker.getValue());
                frame.setNote(et_note.getText().toString());

                // PARSE THE DATE
                frame.setDate(b_date.getText().toString() + " " + b_time.getText().toString());

                // Return the new entered name to the calling activity
                Intent intent = new Intent();
                intent.putExtra("FRAME", frame);
                dialog.dismiss();
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);

            }
        });

        return dialog;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if ( requestCode == PLACE_PICKER_REQUEST && resultCode == Activity.RESULT_OK ) {
            if (data.hasExtra("LATITUDE") && data.hasExtra("LONGITUDE")) {
                frame.setLocation("" + data.getStringExtra("LATITUDE") + " " + data.getStringExtra("LONGITUDE"));
                b_location.setText(frame.getLocation());
            }
        }



        if ( requestCode == ADD_LENS && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            Lens lens = data.getParcelableExtra("LENS");

            if (lens.getMake().length() != 0 && lens.getModel().length() != 0) {
                long rowId = database.addLens(lens);
                lens.setId(rowId);
                database.addMountable(database.getCamera(camera_id), lens);
                mountableLenses.add(lens);
                b_lens.setText(lens.getMake() + " " + lens.getModel());
                frame.setLensId(lens.getId());
            }
        }
    }
}

