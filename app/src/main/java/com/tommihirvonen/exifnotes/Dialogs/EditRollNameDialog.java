package com.tommihirvonen.exifnotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.Datastructures.Roll;
import com.tommihirvonen.exifnotes.Fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class EditRollNameDialog extends DialogFragment {

    Roll roll;

    String title;
    String positiveButton;
    FilmDbHelper database;
    ArrayList<Camera> mCameraList;
    public static final String TAG = "EditNameDialogFragment";

    public EditRollNameDialog () {

    }

    TextView b_camera;
    TextView b_format;

    //These variables are used so that the object itself is not updated
    //unless the user presses ok.
    long newCameraId;
    String newDate;
    int newFormat;

    NumberPicker isoPicker;
    NumberPicker pushPullPicker;


    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        title = getArguments().getString("TITLE");
        positiveButton = getArguments().getString("POSITIVE_BUTTON");
        roll = getArguments().getParcelable("ROLL");
        if (roll == null) roll = new Roll();

        newCameraId = roll.getCamera_id();


        database = new FilmDbHelper(getActivity());
        mCameraList = database.getAllCameras();

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(R.layout.roll_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = (FrameLayout) inflator.findViewById(R.id.root);
            NestedScrollView nestedScrollView = (NestedScrollView) inflator.findViewById(R.id.nested_scroll_view);
            Utilities.setScrollIndicators(rootLayout, nestedScrollView,
                    ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
        }

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById((R.id.txt_name));
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_note);

        b_camera = (TextView) inflator.findViewById(R.id.btn_camera);
        final TextView b_date = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView b_time = (TextView) inflator.findViewById(R.id.btn_time);
        final Button b_addCamera = (Button) inflator.findViewById(R.id.btn_add_camera);

        // CAMERA PICK DIALOG
        b_camera.setClickable(true);
        if ( roll.getCamera_id() > 0 ) b_camera.setText(database.getCamera(roll.getCamera_id()).getMake() + " " + database.getCamera(roll.getCamera_id()).getModel());
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
                        newCameraId = mCameraList.get(which).getId();
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
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                EditCameraInfoDialog dialog = new EditCameraInfoDialog();
                dialog.setTargetFragment(EditRollNameDialog.this, CamerasFragment.ADD_CAMERA);
                Bundle arguments = new Bundle();
                arguments.putString("TITLE", getResources().getString( R.string.NewCamera));
                arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditCameraInfoDialog.TAG);
            }
        });

        // DATE PICK DIALOG
        if (roll.getDate() == null) roll.setDate(Utilities.getCurrentTime());
        ArrayList<String> dateValue = Utilities.splitDate(roll.getDate());
        int temp_year = Integer.parseInt(dateValue.get(0));
        int temp_month = Integer.parseInt(dateValue.get(1));
        int temp_day = Integer.parseInt(dateValue.get(2));
        b_date.setText(temp_year + "-" + temp_month + "-" + temp_day);

        b_date.setClickable(true);

        newDate = roll.getDate();

        b_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DATE PICKER DIALOG IMPLEMENTATION HERE
                ArrayList<String> dateValue = Utilities.splitDate(newDate);
                int i_year = Integer.parseInt(dateValue.get(0));
                int i_month = Integer.parseInt(dateValue.get(1));
                int i_day = Integer.parseInt(dateValue.get(2));
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String newInnerDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                        b_date.setText(newInnerDate);
                        newDate = newInnerDate + " " + b_time.getText().toString();
                    }
                    // One month has to be subtracted from the default shown month, otherwise
                    // the date picker shows one month forward.
                }, i_year, (i_month - 1), i_day);

                dialog.show();

            }
        });

        // TIME PICK DIALOG
        ArrayList<String> timeValue = Utilities.splitTime(roll.getDate());
        int temp_hours = Integer.parseInt(timeValue.get(0));
        int temp_minutes = Integer.parseInt(timeValue.get(1));
        if (temp_minutes < 10) b_time.setText(temp_hours + ":0" + temp_minutes);
        else b_time.setText(temp_hours + ":" + temp_minutes);

        b_time.setClickable(true);

        b_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TIME PICKER DIALOG IMPLEMENTATION HERE
                ArrayList<String> timeValue = Utilities.splitTime(newDate);
                int hours = Integer.parseInt(timeValue.get(0));
                int minutes = Integer.parseInt(timeValue.get(1));
                TimePickerDialog dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String newTime;
                        if (minute < 10) {
                            newTime = hourOfDay + ":0" + minute;
                        } else newTime = hourOfDay + ":" + minute;
                        b_time.setText(newTime);
                        newDate = b_date.getText().toString() + " " + newTime;
                    }
                }, hours, minutes, true);

                dialog.show();

            }
        });

        //ISO PICKER
        isoPicker = (NumberPicker) inflator.findViewById(R.id.isoPicker);
        isoPicker.setMinValue(0);
        isoPicker.setMaxValue(Utilities.isoValues.length-1);
        isoPicker.setDisplayedValues(Utilities.isoValues);
        isoPicker.setValue(0);
        for (int i = 0; i < Utilities.isoValues.length; ++i) {
            if (roll.getIso() == Integer.parseInt(Utilities.isoValues[i])) {
                isoPicker.setValue(i);
                break;
            }
        }

        //PUSH PULL PICKER
        pushPullPicker = (NumberPicker) inflator.findViewById(R.id.pushPullPicker);
        pushPullPicker.setMinValue(0);
        pushPullPicker.setMaxValue(Utilities.compValues.length-1);
        pushPullPicker.setDisplayedValues(Utilities.compValues);
        pushPullPicker.setValue(9);
        if (roll.getPushPull() != null) {
            for (int i = 0; i < Utilities.compValues.length; ++i) {
                if (roll.getPushPull().equals(Utilities.compValues[i])) {
                    pushPullPicker.setValue(i);
                    break;
                }
            }
        }

        //FORMAT PICK DIALOG
        b_format = (TextView) inflator.findViewById(R.id.btn_format);
        b_format.setClickable(true);
        if (roll.getFormat() == 0) roll.setFormat(0);
        b_format.setText(getResources().getStringArray(R.array.FilmFormats)[roll.getFormat()]);
        newFormat = roll.getFormat();
        b_format.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseFormat));
                builder.setItems(R.array.FilmFormats, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newFormat = i;
                        b_format.setText(getResources().getStringArray(R.array.FilmFormats)[i]);
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



        //FINALISE SETTING UP THE DIALOG

        // Show old name on the input field by default
        et1.setText(roll.getName());
        et2.setText(roll.getNote());
        // Place the cursor at the end of the input field
        et1.setSelection(et1.getText().length());

        alert.setPositiveButton(positiveButton, null);
        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.cancel();
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
        });
        final AlertDialog dialog = alert.create();
        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();

        // We override the positive button onClick so that we can dismiss the dialog
        // only when both roll name and camera are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = et1.getText().toString();

                if (name.length() == 0 && newCameraId > 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoName), Toast.LENGTH_SHORT).show();
                } else if (name.length() > 0 && newCameraId <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoCamera), Toast.LENGTH_SHORT).show();
                } else if (name.length() == 0 && newCameraId <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoNameOrCamera), Toast.LENGTH_SHORT).show();
                } else {
                    roll.setName(name);
                    roll.setNote(et2.getText().toString());
                    roll.setCamera_id(newCameraId);
                    roll.setDate(newDate);
                    roll.setIso(Integer.parseInt(Utilities.isoValues[isoPicker.getValue()]));
                    roll.setPushPull(Utilities.compValues[pushPullPicker.getValue()]);
                    roll.setFormat(newFormat);

                    Intent intent = new Intent();
                    intent.putExtra("ROLL", roll);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                }

            }
        });

        return dialog;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case CamerasFragment.ADD_CAMERA:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    Camera camera = data.getParcelableExtra("CAMERA");

                    if (camera.getMake().length() > 0 && camera.getModel().length() > 0) {

                        long rowId = database.addCamera(camera);
                        camera.setId(rowId);
                        mCameraList.add(camera);

                        b_camera.setText(camera.getMake() + " " + camera.getModel());
                        newCameraId = camera.getId();
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
