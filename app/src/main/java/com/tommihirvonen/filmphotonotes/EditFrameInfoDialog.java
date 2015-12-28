package com.tommihirvonen.filmphotonotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class EditFrameInfoDialog extends DialogFragment {



    String lens;
    String date;
    int position;
    int count;
    String shutter;
    String aperture;
    ArrayList<String> lensList;


    static EditFrameInfoDialog newInstance(String lens, int position, int count, String date, String shutter, String aperture, ArrayList<String> lensList) {
        EditFrameInfoDialog f = new EditFrameInfoDialog();
        Bundle args = new Bundle();
        args.putString("lens", lens);
        args.putInt("position", position);
        args.putInt("count", count);
        args.putString("date", date);
        args.putString("shutter", shutter);
        args.putString("aperture", aperture);
        args.putStringArrayList("lenses", lensList);
        f.setArguments(args);
        return f;
    }

    public static final String TAG = "SetLensDialogFragment";


    private OnEditSettedCallback callback;




    public interface OnEditSettedCallback {
        void onEditSetted(String new_lens, int position, int new_count, String new_date, String new_shutter, String new_aperture);
    }


    public EditFrameInfoDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnEditSettedCallback) activity;
        }
        catch(ClassCastException e) {
            e.printStackTrace();
        }
    }




    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        lens = getArguments().getString("lens");
        position = getArguments().getInt("position");
        date = getArguments().getString("date");
        count = getArguments().getInt("count");
        shutter = getArguments().getString("shutter");
        aperture = getArguments().getString("aperture");

        lensList = getArguments().getStringArrayList("lenses");

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.frame_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());


        alert.setTitle("" + getActivity().getString(R.string.EditFrame) + count);

        alert.setView(inflator);

        final TextView b_lens = (TextView) inflator.findViewById(R.id.btn_lens);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);

        final NumberPicker countPicker = (NumberPicker) inflator.findViewById(R.id.countPicker);
        final NumberPicker shutterPicker = (NumberPicker) inflator.findViewById(R.id.shutterPicker);
        final NumberPicker aperturePicker = (NumberPicker) inflator.findViewById(R.id.aperturePicker);

        final String[] shutterValues = new String[]{"<empty>", "B", "30", "15", "8", "4", "2", "1", "1/2", "1/4", "1/8", "1/15", "1/30", "1/60"};
        shutterPicker.setMinValue(0);
        shutterPicker.setMaxValue(shutterValues.length - 1);
        shutterPicker.setDisplayedValues(shutterValues);
        shutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        shutterPicker.setValue(0);
        for (int i = 0; i < shutterValues.length; ++i) {
            if ( shutterValues[i].equals(shutter) ) {
                shutterPicker.setValue(i);
                break;
            }
        }

        final String[] apertureValues = new String[]{"<empty>", "1.0", "1.8", "2.8", "4.0", "5.6", "8.0", "11"};
        aperturePicker.setMinValue(0);
        aperturePicker.setMaxValue(apertureValues.length - 1);
        aperturePicker.setDisplayedValues(apertureValues);
        aperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        aperturePicker.setValue(0);
        for (int i = 0; i < apertureValues.length; ++i) {
            if ( apertureValues[i].equals(aperture) ) {
                aperturePicker.setValue(i);
                break;
            }
        }

        countPicker.setMinValue(0);
        countPicker.setMaxValue(100);
        countPicker.setValue(count);

        b_lens.setText(lens);

        // LENS PICK DIALOG
        b_lens.setClickable(true);
        b_lens.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> listItems = new ArrayList<>();
                for (int i = 0; i < lensList.size(); ++i) {
                    listItems.add(lensList.get(i));
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedLens);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        b_lens.setText(lensList.get(which));
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
        b_time.setText(hours + ":" + minutes);
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

        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                lens = b_lens.getText().toString();
                shutter = shutterValues[shutterPicker.getValue()];
                aperture = apertureValues[aperturePicker.getValue()];
                count = countPicker.getValue();

                // PARSE THE DATE
                date = b_date.getText().toString() + " " + b_time.getText().toString();

                if(lens.length() != 0) {
                    // Return the new entered name to the calling activity
                    callback.onEditSetted(lens, position, count, date, shutter, aperture);
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
