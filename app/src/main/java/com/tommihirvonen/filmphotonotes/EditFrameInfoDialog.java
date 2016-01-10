package com.tommihirvonen.filmphotonotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class EditFrameInfoDialog extends DialogFragment {


    int _id;
    String lens;
    String date;
    int position;
    int count;
    String shutter;
    String aperture;
    String note;
    String location;
    ArrayList<Lens> lensList;
    FilmDbHelper database;

    TextView b_location;

    final static int PLACE_PICKER_REQUEST = 1;


    static EditFrameInfoDialog newInstance(int _id, String lens, int position, int count, String date, String shutter, String aperture, String note, String location) {
        EditFrameInfoDialog f = new EditFrameInfoDialog();
        Bundle args = new Bundle();
        args.putInt("_id", _id);
        args.putString("lens", lens);
        args.putInt("position", position);
        args.putInt("count", count);
        args.putString("date", date);
        args.putString("shutter", shutter);
        args.putString("aperture", aperture);
        args.putString("note", note);
        args.putString("location", location);
        f.setArguments(args);
        return f;
    }

    public static final String TAG = "EditFrameInfoDialogFragment";


    private OnEditSetCallback callback;




    public interface OnEditSetCallback {
        void onEditSet(int _id, String new_lens, int position, int new_count, String new_date, String new_shutter, String new_aperture, String new_note, String location);
    }


    public EditFrameInfoDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnEditSetCallback) activity;
        }
        catch(ClassCastException e) {
            e.printStackTrace();
        }
    }




    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        // Let's check what values the user has set for the shutter and aperture increments
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String shutterIncrements = prefs.getString("ShutterIncrements", "third");
        String apertureIncrements = prefs.getString("ApertureIncrements", "third");

        lens = getArguments().getString("lens");
        position = getArguments().getInt("position");
        date = getArguments().getString("date");
        count = getArguments().getInt("count");
        shutter = getArguments().getString("shutter");
        aperture = getArguments().getString("aperture");
        _id = getArguments().getInt("_id");
        note = getArguments().getString("note");
        location = getArguments().getString("location");

        database = new FilmDbHelper(getActivity());
        lensList = database.getAllLenses();

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.frame_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());


        alert.setTitle("" + getActivity().getString(R.string.EditFrame) + count);

        alert.setView(inflator);

        final TextView et_note = (TextView) inflator.findViewById(R.id.txt_note);
        et_note.setText(note);
        b_location = (TextView) inflator.findViewById(R.id.btn_location);

        final TextView b_lens = (TextView) inflator.findViewById(R.id.btn_lens);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);

        final NumberPicker countPicker = (NumberPicker) inflator.findViewById(R.id.countPicker);
        final NumberPicker shutterPicker = (NumberPicker) inflator.findViewById(R.id.shutterPicker);
        final NumberPicker aperturePicker = (NumberPicker) inflator.findViewById(R.id.aperturePicker);

        // Shutter values in 1/3 increments
        final String[] shutterValuesThird = new String[]{getContext().getString(R.string.NoValue), "B", "30", "25", "20", "15", "13", "10", "8", "6", "5", "4",
                                                    "3", "2.5", "2", "1.6", "1.3", "1", "0.8", "0,6", "1/2", "0.4", "1/3",
                                                    "1/4", "1/5", "1/6", "1/8", "1/10", "1/13", "1/15", "1/20", "1/25",
                                                    "1/30", "1/40", "1/50", "1/60", "1/80", "1/100", "1/125", "1/160", "1/200",
                                                    "1/250", "1/320", "1/400", "1/500", "1/640", "1/800", "1/1000", "1/1250",
                                                    "1/1600", "1/2000", "1/2500", "1/3200", "1/4000", "1/5000", "1/6400", "1/8000"};
        // Shutter values in 1/2 increments
        final String[] shutterValuesHalf = new String[]{ getContext().getString(R.string.NoValue), "B", "30", "22", "15", "12", "8", "6", "4", "3", "2", "1.5",
                                                    "1", "1/1.5", "1/2", "1/3", "1/4", "1/6", "1/8", "1/12", "1/15", "1/22",
                                                    "1/30", "1/45", "1/60", "1/95", "1/125", "1/180", "1/250", "1/375",
                                                    "1/500", "1/750", "1/1000", "1/1500", "1/2000", "1/3000", "1/4000", "1/6000", "1/8000" };
        // Shutter values in full stop increments
        final String[] shutterValuesFull = new String[]{ getContext().getString(R.string.NoValue), "B", "30", "15", "8", "4", "2", "1", "1/2", "1/4", "1/8",
                                                        "1/15", "1/30", "1/60", "1/125", "1/250", "1/500", "1/1000", "1/2000", "1/4000", "1/8000" };
        final String[] displayedShutterValues;

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
        shutterPicker.setValue(0);
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (displayedShutterValues[i].equals(shutter) ) {
                shutterPicker.setValue(i);
                break;
            }
        }

        final String[] apertureValuesThird = new String[]{getContext().getString(R.string.NoValue), "1.0", "1.1", "1.2", "1.4", "1.6", "1.8", "2.0", "2.2", "2.5",
                                                        "2.8", "3.2", "3.5", "4.0", "4.5", "5.0", "5.6", "6.3", "7.1", "8", "9",
                                                        "10", "11", "13", "14", "16", "18", "20", "22", "25", "29", "32", "36",
                                                        "42", "45", "50", "57", "64"};
        final String[] apertureValuesHalf = new String[]{ getContext().getString(R.string.NoValue), "1.0", "1.2", "1.4", "1.7", "2.0", "2.6", "2.8", "3.5",
                                                            "4.0", "4.5", "5.6", "6.7", "8", "9.5", "11", "13", "16", "19",
                                                            "22", "27", "32", "38", "45", "64" };
        final String[] apertureValuesFull = new String[]{ getContext().getString(R.string.NoValue), "1.0", "1.4", "2.0", "2.8", "4.0", "5.6", "8", "11",
                                                            "16", "22", "32", "45", "64" };
        final String[] displayedApertureValues;

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
        aperturePicker.setValue(0);
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if ( displayedApertureValues[i].equals(aperture) ) {
                aperturePicker.setValue(i);
                break;
            }
        }

        // This way we can also reverse the order of the count picker.
        String[] temp = new String[101];
        for ( int i = 0; i <= 100; ++i ) temp[i] = "" + i;
        Collections.reverse(Arrays.asList(temp));
        final String[] displayedCountValues = temp;
        countPicker.setMinValue(0);
        countPicker.setMaxValue(100);
        countPicker.setDisplayedValues(displayedCountValues);
        countPicker.setValue(0);
        for ( int i = 0; i <= 100; ++i ) {
            if ( displayedCountValues[i].equals(Integer.toString(count)) ) {
                countPicker.setValue(i);
            }
        }
        countPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        b_lens.setText(lens);

        // LENS PICK DIALOG
        b_lens.setClickable(true);
        b_lens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> listItems = new ArrayList<>();
                listItems.add("" + getActivity().getString(R.string.NoLens));
                for (int i = 0; i < lensList.size(); ++i) {
                    listItems.add(lensList.get(i).getName());
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedLens);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        b_lens.setText(listItems.get(which));
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

        // DATE PICK DIALOG

        // The date is in format "YYYY-M-D HH:MM"

        ArrayList<String> dateValue = splitDate(date);
        final int i_year = Integer.parseInt(dateValue.get(0));
        final int i_month = Integer.parseInt(dateValue.get(1));
        final int i_day = Integer.parseInt(dateValue.get(2));
        b_date.setText(i_year + "-" + i_month + "-" + i_day);
        b_date.setClickable(true);

        b_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DATE PICKER DIALOG IMPLEMENTATION HERE
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String newDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                        b_date.setText(newDate);
                    }
                    // One month has to be subtracted from the default shown month, otherwise
                    // the date picker shows one month forward.
                }, i_year, (i_month - 1), i_day);

                dialog.show();

            }
        });

        // TIME PICK DIALOG
        ArrayList<String> timeValue = splitTime(date);
        final int hours = Integer.parseInt(timeValue.get(0));
        final int minutes = Integer.parseInt(timeValue.get(1));
        if (minutes < 10) b_time.setText(hours + ":0" + minutes);
        else b_time.setText(hours + ":" + minutes);
        b_time.setClickable(true);

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
                        }
                        else newTime = hourOfDay + ":" + minute;
                        b_time.setText(newTime);
                    }
                }, hours, minutes, true);

                dialog.show();

            }
        });

        // LOCATION PICK DIALOG
        b_location.setText(location);
        b_location.setClickable(true);
        b_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // LOCATION PICKER DIALOG IMPLEMENTATION HERE
                final List<String> listItems = new ArrayList<>();
                listItems.add("Clear");
                listItems.add("Reacquire/Edit on map");
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Choose action");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        switch (which){
                            // Clear
                            case 0:
                                b_location.setText("");
                                break;

                            // Reacquire/Edit on map. PlacePicker!
                            case 1:
                                Intent intent = new Intent(getActivity(), LocationPickActivity.class);
                                intent.putExtra("LOCATION", location);
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


        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                lens = b_lens.getText().toString();
                shutter = displayedShutterValues[shutterPicker.getValue()];
                aperture = displayedApertureValues[aperturePicker.getValue()];
                count = Integer.parseInt(displayedCountValues[countPicker.getValue()]);
                note = et_note.getText().toString();

                // PARSE THE DATE
                date = b_date.getText().toString() + " " + b_time.getText().toString();

                if(lens.length() != 0) {
                    // Return the new entered name to the calling activity
                    callback.onEditSet(_id, lens, position, count, date, shutter, aperture, note, location);
                }
            }
        });

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        return alert.create();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == PLACE_PICKER_REQUEST && resultCode == Activity.RESULT_OK ) {
            if (data.hasExtra("LATITUDE") && data.hasExtra("LONGITUDE")) {
                location = "" + data.getStringExtra("LATITUDE") + " " + data.getStringExtra("LONGITUDE");
                b_location.setText(location);
            }
        }
    }

    private ArrayList<String> splitDate(String input) {
        String[] items = input.split(" ");
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        String[] items2 = itemList.get(0).split("-");
        itemList = new ArrayList<>(Arrays.asList(items2));
        // { YYYY, M, D }
        return itemList;
    }

    private ArrayList<String> splitTime(String input) {
        String[] items = input.split(" ");
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        String[] items2 = itemList.get(1).split(":");
        itemList = new ArrayList<>(Arrays.asList(items2));
        // { HH, MM }
        return itemList;
    }

}
