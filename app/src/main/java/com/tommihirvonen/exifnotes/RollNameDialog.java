package com.tommihirvonen.exifnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class RollNameDialog extends DialogFragment {

    public static final String TAG = "SetNameDialogFragment";


    FilmDbHelper database;
    ArrayList<Camera> mCameraList;
    int camera_id = -1;


    public RollNameDialog() {
        // Empty constructor required for DialogFragment
    }

    TextView b_camera;
    String date;


    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        database = new FilmDbHelper(getActivity());
        mCameraList = database.getAllCameras();

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.roll_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.NewRoll);

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_name);
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_note);

        b_camera = (TextView) inflator.findViewById(R.id.btn_camera);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);

        if ( SavedInstanceState != null ) {
            b_camera.setText(SavedInstanceState.getString("CAMERA_NAME"));
            camera_id = SavedInstanceState.getInt("CAMERA_ID");
        }

        // CAMERA PICK DIALOG
        b_camera.setClickable(true);
        b_camera.setText(R.string.ClickToSelect);
        b_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> listItems = new ArrayList<>();
                for (int i = 0; i < mCameraList.size(); ++i) {
                    listItems.add(mCameraList.get(i).getMake() + " " + mCameraList.get(i).getModel());
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedCamera);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        b_camera.setText(listItems.get(which));
                        camera_id = mCameraList.get(which).getId();
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

        date = FramesFragment.getCurrentTime();

        ArrayList<String> dateValue = FrameInfoDialog.splitDate(date);
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
                        date = newDate + " " + b_time.getText().toString();
                    }
                    // One month has to be subtracted from the default shown month, otherwise
                    // the date picker shows one month forward.
                }, i_year, (i_month - 1), i_day);

                dialog.show();

            }
        });

        // TIME PICK DIALOG
        ArrayList<String> timeValue = FrameInfoDialog.splitTime(date);
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
                        } else newTime = hourOfDay + ":" + minute;
                        b_time.setText(newTime);
                        date = b_date.getText().toString() + " " + newTime;
                    }
                }, hours, minutes, true);

                dialog.show();

            }
        });


        alert.setPositiveButton(R.string.Add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String name = et1.getText().toString();
                String note = et2.getText().toString();

                // If the name is not empty and camera id is not -1 then callback
                if( name.length() != 0 && camera_id != -1 ) {
                    // Return the new entered name to the calling activity
                    Intent intent = new Intent();
                    intent.putExtra("NAME", name);
                    intent.putExtra("NOTE", note);
                    intent.putExtra("DATE", date);
                    intent.putExtra("CAMERA_ID", camera_id);
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                } else if ( name.length() == 0 && camera_id != -1 ) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoName), Toast.LENGTH_SHORT).show();
                } else if ( name.length() != 0 && camera_id == -1 ) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoCamera), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoNameOrCamera), Toast.LENGTH_SHORT).show();
                }
            }
        });

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("CAMERA_NAME", b_camera.getText().toString());
        outState.putInt("CAMERA_ID", camera_id);
    }
}

