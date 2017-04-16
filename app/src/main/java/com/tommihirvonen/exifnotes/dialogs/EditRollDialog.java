package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to edit Roll's information
 */
public class EditRollDialog extends DialogFragment {

    /**
     * Public constant used to tag the fragment when created
     */
    public static final String TAG = "EditRollDialog";

    /**
     * Holds the information of the edited Roll
     */
    private Roll roll;

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * Holds all the cameras in the database
     */
    private List<Camera> cameraList;

    /**
     * The Button showing the currently selected camera
     */
    private TextView cameraTextView;


    //These variables are used so that the object itself is not updated
    //unless the user presses ok.

    /**
     * Database id of the currently selected camera
     */
    private long newCameraId;

    /**
     * Currently selected datetime in format 'YYYY-M-D H:MM'
     */
    private String newDate;

    /**
     * Currently selected film format, corresponding values defined in res/values/array.xml
     */
    private int newFormat;

    /**
     * Currently selected ISO
     */
    private int newIso;

    /**
     * Currently set note
     */
    private String newNote;

    /**
     * Currently selected push or pull value in format 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    private String newPushPull;

    /**
     * Empty constructor
     */
    public EditRollDialog() {

    }

    /**
     * Called when the DialogFragment is ready to create the dialog.
     * Inflate the fragment. Get the edited roll and list of cameras.
     * Initialize the UI objects and display the roll's information.
     * Add listeners to buttons to open new dialogs to change the roll's information.
     *
     * @param SavedInstanceState possible saved state in case the DialogFragment was resumed
     * @return inflated dialog ready to be shown
     */
    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        roll = getArguments().getParcelable("ROLL");
        if (roll == null) roll = new Roll();

        newCameraId = roll.getCameraId();
        newIso = roll.getIso();
        newPushPull = roll.getPushPull();

        database = FilmDbHelper.getInstance(getActivity());
        cameraList = database.getAllCameras();

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.roll_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = (FrameLayout) inflatedView.findViewById(R.id.root);
            NestedScrollView nestedScrollView = (NestedScrollView) inflatedView.findViewById(
                    R.id.nested_scroll_view);
            Utilities.setScrollIndicators(getActivity(), rootLayout, nestedScrollView,
                    ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
        }

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflatedView);

        //==========================================================================================
        //DIVIDERS

        // Color the dividers white if the app's theme is dark
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String theme = preferences.getString("AppTheme", "LIGHT");
        if (theme.equals("DARK")) {
            List<View> dividerList = new ArrayList<>();
            dividerList.add(inflatedView.findViewById(R.id.divider_view1));
            dividerList.add(inflatedView.findViewById(R.id.divider_view2));
            dividerList.add(inflatedView.findViewById(R.id.divider_view3));
            dividerList.add(inflatedView.findViewById(R.id.divider_view4));
            dividerList.add(inflatedView.findViewById(R.id.divider_view5));
            dividerList.add(inflatedView.findViewById(R.id.divider_view6));
            dividerList.add(inflatedView.findViewById(R.id.divider_view7));
            for (View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
            ((ImageView) inflatedView.findViewById(R.id.add_camera)).getDrawable().mutate()
                    .setColorFilter(
                            ContextCompat.getColor(getActivity(), R.color.white),
                            PorterDuff.Mode.SRC_IN
                    );
        }
        //==========================================================================================

        // NAME EDIT TEXT
        final EditText nameEditText = (EditText) inflatedView.findViewById((R.id.name_editText));
        nameEditText.setText(roll.getName());
        // Place the cursor at the end of the input field
        nameEditText.setSelection(nameEditText.getText().length());


        //==========================================================================================
        // CAMERA PICK DIALOG
        cameraTextView = (TextView) inflatedView.findViewById(R.id.camera_text);
        if (roll.getCameraId() > 0) cameraTextView.setText(
                database.getCamera(roll.getCameraId())
                        .getMake() + " " + database.getCamera(roll.getCameraId()).getModel());
        else cameraTextView.setText("");

        final LinearLayout cameraLayout = (LinearLayout) inflatedView.findViewById(R.id.camera_layout);
        cameraLayout.setOnClickListener(new View.OnClickListener() {
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
        //==========================================================================================



        //==========================================================================================
        // CAMERA ADD DIALOG
        final ImageView addCameraImageView = (ImageView) inflatedView.findViewById(R.id.add_camera);
        addCameraImageView.setClickable(true);
        addCameraImageView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                EditCameraDialog dialog = new EditCameraDialog();
                dialog.setTargetFragment(EditRollDialog.this, CamerasFragment.ADD_CAMERA);
                Bundle arguments = new Bundle();
                arguments.putString("TITLE", getResources().getString( R.string.NewCamera));
                arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditCameraDialog.TAG);
            }
        });
        //==========================================================================================


        //==========================================================================================
        // DATE PICK DIALOG
        final TextView dateTextView = (TextView) inflatedView.findViewById(R.id.date_text);

        // Declare timeTextView here, so that we can use it inside the date listener
        final TextView timeTextView = (TextView) inflatedView.findViewById(R.id.time_text);

        if (roll.getDate() == null) roll.setDate(Utilities.getCurrentTime());
        List<String> dateValue = Utilities.splitDate(roll.getDate());
        int tempYear = Integer.parseInt(dateValue.get(0));
        int tempMonth = Integer.parseInt(dateValue.get(1));
        int tempDay = Integer.parseInt(dateValue.get(2));
        dateTextView.setText(tempYear + "-" + tempMonth + "-" + tempDay);

        newDate = roll.getDate();

        final LinearLayout dateLayout = (LinearLayout) inflatedView.findViewById(R.id.date_layout);
        dateLayout.setOnClickListener(new View.OnClickListener() {
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
        //==========================================================================================



        //==========================================================================================
        // TIME PICK DIALOG

        List<String> timeValue = Utilities.splitTime(roll.getDate());
        int tempHours = Integer.parseInt(timeValue.get(0));
        int tempMinutes = Integer.parseInt(timeValue.get(1));
        if (tempMinutes < 10) timeTextView.setText(tempHours + ":0" + tempMinutes);
        else timeTextView.setText(tempHours + ":" + tempMinutes);

        final LinearLayout timeLayout = (LinearLayout) inflatedView.findViewById(R.id.time_layout);
        timeLayout.setOnClickListener(new View.OnClickListener() {
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
        //==========================================================================================



        //==========================================================================================
        //ISO PICKER
        final TextView isoTextView = (TextView) inflatedView.findViewById(R.id.iso_text);
        isoTextView.setText(
                 newIso == 0 ? "" : String.valueOf(newIso)
        );

        final LinearLayout isoLayout = (LinearLayout) inflatedView.findViewById(R.id.iso_layout);
        isoLayout.setOnClickListener(new View.OnClickListener() {
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
                builder.setTitle(getResources().getString(R.string.ChooseISO));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newIso = Integer.parseInt(Utilities.isoValues[isoPicker.getValue()]);
                        isoTextView.setText(newIso == 0 ? "" : String.valueOf(newIso));
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
        //==========================================================================================




        //==========================================================================================
        //PUSH PULL PICKER
        final TextView pushPullTextView = (TextView) inflatedView.findViewById(R.id.push_pull_text);
        pushPullTextView.setText(
                newPushPull == null || newPushPull.equals("0") ? "" : newPushPull
        );

        final LinearLayout pushPullLayout = (LinearLayout) inflatedView.findViewById(R.id.push_pull_layout);
        pushPullLayout.setOnClickListener(new View.OnClickListener() {
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
                builder.setTitle(getResources().getString(R.string.ChoosePushOrPull));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            newPushPull = Utilities.compValues[pushPullPicker.getValue()];
                            pushPullTextView.setText(
                                    newPushPull.equals("0") ? "" : newPushPull
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
        //==========================================================================================



        //==========================================================================================
        //FORMAT PICK DIALOG
        final TextView formatTextView = (TextView) inflatedView.findViewById(R.id.format_text);
        if (roll.getFormat() == 0) roll.setFormat(0);
        formatTextView.setText(getResources().getStringArray(R.array.FilmFormats)[roll.getFormat()]);
        newFormat = roll.getFormat();

        final LinearLayout formatLayout = (LinearLayout) inflatedView.findViewById(R.id.format_layout);
        formatLayout.setOnClickListener(new View.OnClickListener() {
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
        //==========================================================================================


        //==========================================================================================
        // NOTE DIALOG
        newNote = roll.getNote();
        final TextView noteTextView = (TextView) inflatedView.findViewById(R.id.note_text);
        noteTextView.setText(
                newNote == null ? "" : newNote
        );
        final LinearLayout noteLayout = (LinearLayout) inflatedView.findViewById(R.id.note_layout);
        noteLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.EditDescriptionOrNote));
                final EditText noteEditText = new EditText(getActivity());
                builder.setView(noteEditText);
                noteEditText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                noteEditText.setSingleLine(false);
                noteEditText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                noteEditText.setText(newNote);
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newNote = noteEditText.getText().toString();
                                noteTextView.setText(newNote);
                            }
                        });
                builder.setNegativeButton(getResources().getString(R.string.Cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Do nothing
                            }
                        });
                builder.create().show();
            }
        });
        //==========================================================================================



        //FINALISE SETTING UP THE DIALOG

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
                    roll.setNote(newNote);
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

    /**
     * Executed when an activity or fragment, which is started for result, sends an onActivityResult
     * signal to this fragment.
     *
     * Handle EditCameraDialog's result.
     *
     * @param requestCode the request code that was set for the intent
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed intent
     */
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
