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

    public static final String TAG = "EditNameDialogFragment";

    Roll roll;
    String title;
    String positiveButton;
    FilmDbHelper database;
    List<Camera> cameraList;
    TextView cameraTextView;
    TextView formatTextView;

    //These variables are used so that the object itself is not updated
    //unless the user presses ok.
    long newCameraId;
    String newDate;
    int newFormat;
    int newIso;
    String newPushPull;

    public EditRollNameDialog () {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        title = getArguments().getString("TITLE");
        positiveButton = getArguments().getString("POSITIVE_BUTTON");
        roll = getArguments().getParcelable("ROLL");
        if (roll == null) roll = new Roll();

        newCameraId = roll.getCameraId();
        newIso = roll.getIso();
        newPushPull = roll.getPushPull();

        database = FilmDbHelper.getInstance(getActivity());
        cameraList = database.getAllCameras();

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(
                R.layout.roll_info_dialog, null);
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

        final EditText nameEditText = (EditText) inflator.findViewById((R.id.txt_name));
        final EditText noteEditText = (EditText) inflator.findViewById(R.id.txt_note);

        cameraTextView = (TextView) inflator.findViewById(R.id.btn_camera);
        final TextView dateTextView = (TextView) inflator.findViewById(R.id.btn_date);
        final TextView timeTextView = (TextView) inflator.findViewById(R.id.btn_time);
        final Button addCameraButton = (Button) inflator.findViewById(R.id.btn_add_camera);

        // CAMERA PICK DIALOG
        cameraTextView.setClickable(true);
        if (roll.getCameraId() > 0) cameraTextView.setText(
                database.getCamera(roll.getCameraId())
                        .getMake() + " " + database.getCamera(roll.getCameraId()).getModel());
        else cameraTextView.setText(R.string.ClickToSelect);
        cameraTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> listItems = new ArrayList<>();
                int checkedItem = -1;
                for (int i = 0; i < cameraList.size(); ++i) {
                    listItems.add(cameraList.get(i).getMake() + " " + cameraList.get(i).getModel());
                    if (cameraList.get(i).getId() == newCameraId) checkedItem = i;
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedCamera);
                builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // listItems also contains the No lens option
                        cameraTextView.setText(listItems.get(which));
                        newCameraId = cameraList.get(which).getId();
                        dialogInterface.dismiss();
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
        addCameraButton.setClickable(true);
        addCameraButton.setOnClickListener(new View.OnClickListener() {
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
        List<String> dateValue = Utilities.splitDate(roll.getDate());
        int tempYear = Integer.parseInt(dateValue.get(0));
        int tempMonth = Integer.parseInt(dateValue.get(1));
        int tempDay = Integer.parseInt(dateValue.get(2));
        dateTextView.setText(tempYear + "-" + tempMonth + "-" + tempDay);

        dateTextView.setClickable(true);

        newDate = roll.getDate();

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
        List<String> timeValue = Utilities.splitTime(roll.getDate());
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

        //ISO PICKER
        final Button isoButton = (Button) inflator.findViewById(R.id.btn_iso);
        isoButton.setText(String.valueOf(newIso));
        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker isoPicker = (NumberPicker) dialogView.findViewById(R.id.number_picker);
                isoPicker.setMinValue(0);
                isoPicker.setMaxValue(Utilities.isoValues.length-1);
                isoPicker.setDisplayedValues(Utilities.isoValues);
                isoPicker.setValue(0);
                for (int i = 0; i < Utilities.isoValues.length; ++i) {
                    if (newIso == Integer.parseInt(Utilities.isoValues[i])) {
                        isoPicker.setValue(i);
                        break;
                    }
                }
                //To prevent text edit
                isoPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                builder.setView(dialogView);
                builder.setTitle("Choose ISO");
                builder.setPositiveButton(getResources().getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newIso = Integer.parseInt(Utilities.isoValues[isoPicker.getValue()]);
                        isoButton.setText(String.valueOf(newIso));
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Do nothing
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


        //PUSH PULL PICKER
        final Button pushPullButton = (Button) inflator.findViewById(R.id.btn_push_pull);
        pushPullButton.setText(
                newPushPull == null || newPushPull.equals("0") ? "±0" : newPushPull
        );
        pushPullButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker pushPullPicker =
                        Utilities.fixNumberPicker((NumberPicker) dialogView.findViewById(R.id.number_picker));
                pushPullPicker.setMinValue(0);
                pushPullPicker.setMaxValue(Utilities.compValues.length-1);
                pushPullPicker.setDisplayedValues(Utilities.compValues);
                pushPullPicker.setValue(9);
                if (newPushPull != null) {
                    for (int i = 0; i < Utilities.compValues.length; ++i) {
                        if (newPushPull.equals(Utilities.compValues[i])) {
                            pushPullPicker.setValue(i);
                            break;
                        }
                    }
                }
                //To prevent text edit
                pushPullPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                builder.setView(dialogView);
                builder.setTitle("Choose push/pull");
                builder.setPositiveButton(getResources().getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            newPushPull = Utilities.compValues[pushPullPicker.getValue()];
                            pushPullButton.setText(
                                    newPushPull.equals("0") ? "±0" : newPushPull
                            );
                        }
                });
                builder.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Do nothing
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        //FORMAT PICK DIALOG
        formatTextView = (TextView) inflator.findViewById(R.id.btn_format);
        formatTextView.setClickable(true);
        if (roll.getFormat() == 0) roll.setFormat(0);
        formatTextView.setText(getResources().getStringArray(R.array.FilmFormats)[roll.getFormat()]);
        newFormat = roll.getFormat();
        formatTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int checkedItem = newFormat;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseFormat));
                builder.setSingleChoiceItems(R.array.FilmFormats, checkedItem,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newFormat = i;
                        formatTextView.setText(getResources().getStringArray(R.array.FilmFormats)[i]);
                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
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
        nameEditText.setText(roll.getName());
        noteEditText.setText(roll.getNote());
        // Place the cursor at the end of the input field
        nameEditText.setSelection(nameEditText.getText().length());

        alert.setPositiveButton(positiveButton, null);
        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.cancel();
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
        // only when both roll name and camera are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = nameEditText.getText().toString();

                if (name.length() == 0 && newCameraId > 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoName),
                            Toast.LENGTH_SHORT).show();
                } else if (name.length() > 0 && newCameraId <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoCamera),
                            Toast.LENGTH_SHORT).show();
                } else if (name.length() == 0 && newCameraId <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoNameOrCamera),
                            Toast.LENGTH_SHORT).show();
                } else {
                    roll.setName(name);
                    roll.setNote(noteEditText.getText().toString());
                    roll.setCameraId(newCameraId);
                    roll.setDate(newDate);
                    roll.setIso(newIso);
                    roll.setPushPull(newPushPull);
                    roll.setFormat(newFormat);

                    Intent intent = new Intent();
                    intent.putExtra("ROLL", roll);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(
                            getTargetRequestCode(), Activity.RESULT_OK, intent);
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
                        cameraList.add(camera);

                        cameraTextView.setText(camera.getMake() + " " + camera.getModel());
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
