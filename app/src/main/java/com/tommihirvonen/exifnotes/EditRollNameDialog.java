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

import java.util.ArrayList;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class EditRollNameDialog extends DialogFragment {

    public String oldName;
    public String oldNote;
    public int rollId;
    int camera_id;
    String date;
    FilmDbHelper database;
    ArrayList<Camera> mCameraList;
    public static final String TAG = "EditNameDialogFragment";

    public EditRollNameDialog () {

    }

    // Android doesn't like fragments to be created with arguments. This is a workaround.
    public void setOldName (int rollId, String oldName, String oldNote, int camera_id, String date) {
        this.rollId = rollId;
        this.oldName = oldName;
        this.oldNote = oldNote;
        this.camera_id = camera_id;
        this.date = date;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        if ( SavedInstanceState != null ) camera_id = SavedInstanceState.getInt("CAMERA_ID");

        database = new FilmDbHelper(getActivity());
        mCameraList = database.getAllCameras();

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.roll_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.EditRoll);
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById((R.id.txt_name));
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_note);

        final TextView b_camera = (TextView) inflator.findViewById(R.id.btn_camera);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);

        // CAMERA PICK DIALOG
        b_camera.setClickable(true);
        b_camera.setText(database.getCamera(camera_id).getMake() + " " + database.getCamera(camera_id).getModel());
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

        final ArrayList<String> dateValue = FrameInfoDialog.splitDate(date);
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

        // Show old name on the input field by default
        et1.setText(oldName);
        et2.setText(oldNote);
        // Place the cursor at the end of the input field
        et1.setSelection(et1.getText().length());

        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = et1.getText().toString();
                String newNote = et2.getText().toString();
                if (newName.length() != 0) {
                    Intent intent = new Intent();
                    intent.putExtra("ROLL_ID", rollId);
                    intent.putExtra("NEWNAME", newName);
                    intent.putExtra("NEWNOTE", newNote);
                    intent.putExtra("CAMERA_ID", camera_id);
                    intent.putExtra("DATE", date);
                    //callback.OnNameEdited(rollId, newName, newNote, camera_id);
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                }
            }
        });

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.cancel();
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
        outState.putInt("CAMERA_ID", camera_id);
    }
}
