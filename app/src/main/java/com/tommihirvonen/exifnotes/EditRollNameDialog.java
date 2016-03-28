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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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
    String title;
    String positiveButton;
    FilmDbHelper database;
    ArrayList<Camera> mCameraList;
    public static final String TAG = "EditNameDialogFragment";

    public EditRollNameDialog () {

    }

    TextView b_camera;


    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        oldName = getArguments().getString("OLD_NAME");
        oldNote = getArguments().getString("OLD_NOTE");
        rollId = getArguments().getInt("ROLL_ID");
        camera_id = getArguments().getInt("CAMERA_ID", -1);
        date = getArguments().getString("DATE");
        title = getArguments().getString("TITLE");
        positiveButton = getArguments().getString("POSITIVE_BUTTON");

        database = new FilmDbHelper(getActivity());
        mCameraList = database.getAllCameras();

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.roll_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(title);
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById((R.id.txt_name));
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_note);

        b_camera = (TextView) inflator.findViewById(R.id.btn_camera);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);
        final Button b_addCamera = (Button) inflator.findViewById(R.id.btn_add_camera);

        // CAMERA PICK DIALOG
        b_camera.setClickable(true);
        if ( camera_id != -1 ) b_camera.setText(database.getCamera(camera_id).getMake() + " " + database.getCamera(camera_id).getModel());
        else b_camera.setText(R.string.ClickToSelect);
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

        // CAMERA ADD DIALOG
        b_addCamera.setClickable(true);
        b_addCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditGearInfoDialog dialog = new EditGearInfoDialog();
                dialog.setTargetFragment(EditRollNameDialog.this, CamerasFragment.ADD_CAMERA);
                Bundle arguments = new Bundle();
                arguments.putString("TITLE", getResources().getString( R.string.NewCamera));
                arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditGearInfoDialog.TAG);
            }
        });

        // DATE PICK DIALOG
        int temp_year;
        int temp_month;
        int temp_day;

        if ( date.length() > 0 ) {
            final ArrayList<String> dateValue = FramesFragment.splitDate(date);
            temp_year = Integer.parseInt(dateValue.get(0));
            temp_month = Integer.parseInt(dateValue.get(1));
            temp_day = Integer.parseInt(dateValue.get(2));
            b_date.setText(temp_year + "-" + temp_month + "-" + temp_day);
        } else {
            date = FramesFragment.getCurrentTime();

            ArrayList<String> dateValue = FramesFragment.splitDate(date);
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
                        date = newDate + " " + b_time.getText().toString();
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

        if ( date.length() > 0 ) {
            ArrayList<String> timeValue = FramesFragment.splitTime(date);
            temp_hours = Integer.parseInt(timeValue.get(0));
            temp_minutes = Integer.parseInt(timeValue.get(1));
            if (temp_minutes < 10) b_time.setText(temp_hours + ":0" + temp_minutes);
            else b_time.setText(temp_hours + ":" + temp_minutes);
        } else {
            ArrayList<String> timeValue = FramesFragment.splitTime(date);
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

        alert.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = et1.getText().toString();
                String newNote = et2.getText().toString();
                if( newName.length() != 0 && camera_id != -1 ) {
                    Intent intent = new Intent();
                    intent.putExtra("ROLL_ID", rollId);
                    intent.putExtra("NAME", newName);
                    intent.putExtra("NOTE", newNote);
                    intent.putExtra("CAMERA_ID", camera_id);
                    intent.putExtra("DATE", date);
                    //callback.OnNameEdited(rollId, newName, newNote, camera_id);
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                } else if ( newName.length() == 0 && camera_id != -1 ) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoName), Toast.LENGTH_SHORT).show();
                } else if ( newName.length() != 0 && camera_id == -1 ) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoCamera), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoNameOrCamera), Toast.LENGTH_SHORT).show();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case CamerasFragment.ADD_CAMERA:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    String inputTextMake = data.getStringExtra("MAKE");
                    String inputTextModel = data.getStringExtra("MODEL");

                    if (inputTextMake.length() != 0 && inputTextModel.length() != 0) {

                        // Check if a camera with the same name already exists
                        for (int i = 0; i < mCameraList.size(); ++i) {
                            if (inputTextMake.equals(mCameraList.get(i).getMake()) && inputTextModel.equals(mCameraList.get(i).getModel())) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.CameraSameName), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }

                        //Check if there are illegal character in the camera name
                        String ReservedChars = "|\\?*<\":>/";
                        for (int i = 0; i < inputTextMake.length(); ++i) {
                            Character c = inputTextMake.charAt(i);
                            if (ReservedChars.contains(c.toString())) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.CameraMakeIllegalCharacter) + " " + c.toString(), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }
                        for (int i = 0; i < inputTextModel.length(); ++i) {
                            Character c = inputTextModel.charAt(i);
                            if (ReservedChars.contains(c.toString())) {
                                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.CameraModelIllegalCharacter) + " " + c.toString(), Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }
                        }

                        Camera camera = new Camera();
                        camera.setMake(inputTextMake);
                        camera.setModel(inputTextModel);
                        database.addCamera(camera);
                        // When we get the last added lens from the database we get the row id value.
                        camera = database.getLastCamera();
                        mCameraList.add(camera);

                        b_camera.setText(camera.getMake() + " " + camera.getModel());
                        camera_id = camera.getId();
                    }

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // After Cancel code.
                    // Do nothing.
                    return;
                }
                break;
        }
    }
}
