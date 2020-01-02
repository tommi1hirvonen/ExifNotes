package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.fragments.CamerasFragment;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
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

    private TextView filmStockTextView;

    private ImageView filmStockClearImageView;

    private EditText nameEditText;


    //These variables are used so that the object itself is not updated
    //unless the user presses ok.

    /**
     * Currently selected camera
     */
    @Nullable
    private Camera newCamera;

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
     * Currently selected push or pull value in format 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    private String newPushPull;

    private FilmStock newFilmStock;

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

        database = FilmDbHelper.getInstance(getActivity());

        String title = getArguments().getString(ExtraKeys.TITLE);
        String positiveButton = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        roll = getArguments().getParcelable(ExtraKeys.ROLL);
        if (roll == null) roll = new Roll();

        newCamera = database.getCamera(roll.getCameraId());
        newIso = roll.getIso();
        newPushPull = roll.getPushPull();
        cameraList = database.getAllCameras();
        newFilmStock = database.getFilmStock(roll.getFilmStockId());

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.dialog_roll, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = inflatedView.findViewById(R.id.root);
            NestedScrollView nestedScrollView = inflatedView.findViewById(
                    R.id.nested_scroll_view);
            Utilities.setScrollIndicators(getActivity(), rootLayout, nestedScrollView,
                    ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
        }

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflatedView);

        //==========================================================================================
        //DIVIDERS

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(getActivity())) {
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
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.add_camera)).getDrawable().mutate(),
                    ContextCompat.getColor(getActivity(), R.color.white));
        }
        //==========================================================================================

        // NAME EDIT TEXT
        nameEditText = inflatedView.findViewById((R.id.name_editText));
        nameEditText.setText(roll.getName());
        // Place the cursor at the end of the input field
        nameEditText.setSelection(nameEditText.getText().length());
        nameEditText.setSingleLine(false);


        //==========================================================================================
        // NOTE EDIT TEXT
        final EditText noteEditText = inflatedView.findViewById(R.id.note_editText);
        noteEditText.setSingleLine(false);
        noteEditText.setText(roll.getNote());
        noteEditText.setSelection(noteEditText.getText().length());
        //==========================================================================================


        //==========================================================================================
        // FILM STOCK PICK DIALOG
        filmStockTextView = inflatedView.findViewById(R.id.film_stock_text);
        filmStockClearImageView = inflatedView.findViewById(R.id.clear_film_stock);
        if (newFilmStock != null) {
            filmStockTextView.setText(newFilmStock.getName());
            nameEditText.setHint(newFilmStock.getName());
        } else {
            filmStockTextView.setText("");
            filmStockClearImageView.setVisibility(View.GONE);
        }
        filmStockClearImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newFilmStock = null;
                nameEditText.setHint("");
                filmStockTextView.setText("");
                filmStockClearImageView.setVisibility(View.GONE);
            }
        });

        final LinearLayout filmStockLayout = inflatedView.findViewById(R.id.film_stock_layout);
        filmStockLayout.setOnClickListener(new FilmStockLayoutOnClickListener());
        //==========================================================================================


        //==========================================================================================
        // CAMERA PICK DIALOG
        cameraTextView = inflatedView.findViewById(R.id.camera_text);
        if (newCamera != null) cameraTextView.setText(newCamera.getName());
        else cameraTextView.setText("");

        final LinearLayout cameraLayout = inflatedView.findViewById(R.id.camera_layout);
        cameraLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> listItems = new ArrayList<>();
                int checkedItem = 0; // Default checked item is 'No camera'
                listItems.add(getResources().getString(R.string.NoCamera));
                for (int i = 0; i < cameraList.size(); ++i) {
                    listItems.add(cameraList.get(i).getMake() + " " + cameraList.get(i).getModel());
                    if (cameraList.get(i).equals(newCamera)) checkedItem = i + 1;
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[0]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedCamera);
                builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // listItems also contains the No camera option
                        if (which > 0) {
                            cameraTextView.setText(listItems.get(which));
                            newCamera = cameraList.get(which - 1);
                        } else {
                            cameraTextView.setText("");
                            newCamera = null;
                        }
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
        final ImageView addCameraImageView = inflatedView.findViewById(R.id.add_camera);
        addCameraImageView.setClickable(true);
        addCameraImageView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                noteEditText.clearFocus();
                nameEditText.clearFocus();
                EditCameraDialog dialog = new EditCameraDialog();
                dialog.setTargetFragment(EditRollDialog.this, CamerasFragment.ADD_CAMERA);
                Bundle arguments = new Bundle();
                arguments.putString(ExtraKeys.TITLE, getResources().getString( R.string.NewCamera));
                arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditCameraDialog.TAG);
            }
        });
        //==========================================================================================


        //==========================================================================================
        // DATE PICK DIALOG
        final TextView dateTextView = inflatedView.findViewById(R.id.date_text);

        // Declare timeTextView here, so that we can use it inside the date listener
        final TextView timeTextView = inflatedView.findViewById(R.id.time_text);

        if (roll.getDate() == null) roll.setDate(Utilities.getCurrentTime());
        List<String> dateValue = Utilities.splitDate(roll.getDate() != null ? roll.getDate() : Utilities.getCurrentTime());
        int tempYear = Integer.parseInt(dateValue.get(0));
        int tempMonth = Integer.parseInt(dateValue.get(1));
        int tempDay = Integer.parseInt(dateValue.get(2));
        dateTextView.setText(tempYear + "-" + tempMonth + "-" + tempDay);

        newDate = roll.getDate();

        final LinearLayout dateLayout = inflatedView.findViewById(R.id.date_layout);
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

        final LinearLayout timeLayout = inflatedView.findViewById(R.id.time_layout);
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
        final TextView isoTextView = inflatedView.findViewById(R.id.iso_text);
        isoTextView.setText(
                 newIso == 0 ? "" : String.valueOf(newIso)
        );

        final LinearLayout isoLayout = inflatedView.findViewById(R.id.iso_layout);
        isoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
                final NumberPicker isoPicker = dialogView.findViewById(R.id.number_picker);
                final String[] isoValues = getActivity().getResources().getStringArray(R.array.ISOValues);
                isoPicker.setMinValue(0);
                isoPicker.setMaxValue(isoValues.length-1);
                isoPicker.setDisplayedValues(isoValues);
                isoPicker.setValue(0);
                for (int i = 0; i < isoValues.length; ++i) {
                    if (newIso == Integer.parseInt(isoValues[i])) {
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
                        newIso = Integer.parseInt(isoValues[isoPicker.getValue()]);
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
        final TextView pushPullTextView = inflatedView.findViewById(R.id.push_pull_text);
        pushPullTextView.setText(
                newPushPull == null || newPushPull.equals("0") ? "" : newPushPull
        );

        final LinearLayout pushPullLayout = inflatedView.findViewById(R.id.push_pull_layout);
        pushPullLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
                final NumberPicker pushPullPicker =
                        Utilities.fixNumberPicker(dialogView.findViewById(R.id.number_picker));
                final String[] compValues = getActivity().getResources().getStringArray(R.array.CompValues);
                pushPullPicker.setMinValue(0);
                pushPullPicker.setMaxValue(compValues.length-1);
                pushPullPicker.setDisplayedValues(compValues);
                pushPullPicker.setValue(9);
                if (newPushPull != null) {
                    for (int i = 0; i < compValues.length; ++i) {
                        if (newPushPull.equals(compValues[i])) {
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
                            newPushPull = compValues[pushPullPicker.getValue()];
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
        final TextView formatTextView = inflatedView.findViewById(R.id.format_text);
        if (roll.getFormat() == 0) roll.setFormat(0);
        formatTextView.setText(getResources().getStringArray(R.array.FilmFormats)[roll.getFormat()]);
        newFormat = roll.getFormat();

        final LinearLayout formatLayout = inflatedView.findViewById(R.id.format_layout);
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

                // Check if name is not set and if name can be replaced with the film stock's name.
                if (name.length() == 0 && newFilmStock != null) {
                    name = newFilmStock.getName();
                }
                // Check the length again.
                if (name.length() > 0) {
                    roll.setName(name);
                    roll.setNote(noteEditText.getText().toString());
                    roll.setCameraId(newCamera != null ? newCamera.getId() : 0);
                    roll.setDate(newDate);
                    roll.setIso(newIso);
                    roll.setPushPull(newPushPull);
                    roll.setFormat(newFormat);
                    roll.setFilmStockId(newFilmStock != null ? newFilmStock.getId() : 0);

                    Intent intent = new Intent();
                    intent.putExtra(ExtraKeys.ROLL, roll);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(
                            getTargetRequestCode(), Activity.RESULT_OK, intent);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoName),
                            Toast.LENGTH_SHORT).show();
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
        if (requestCode == CamerasFragment.ADD_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                // After Ok code.

                newCamera = data.getParcelableExtra(ExtraKeys.CAMERA);
                long rowId = database.addCamera(newCamera);
                newCamera.setId(rowId);
                cameraList.add(newCamera);
                cameraTextView.setText(newCamera.getName());

            }
        }
    }

    /**
     * Private class to listen to clicks on the FilmStock layout.
     */
    private class FilmStockLayoutOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            // First show a list of film stock manufacturers.
            final CharSequence[] manufacturers = database.getAllFilmManufacturers().toArray(new CharSequence[0]);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(manufacturers, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    // Once the user has selected a manufacturer,
                    // show a list of film stocks by that manufacturer.
                    final String manufacturer = manufacturers[which].toString();
                    final List<FilmStock> filmStocks = database.getAllFilmStocks(manufacturer);
                    final List<String> filmStockNames = new ArrayList<>();
                    int checkedItem = -1;
                    int counter = 0;
                    for (FilmStock filmStock : filmStocks) {
                        filmStockNames.add(filmStock.getName());
                        if (newFilmStock != null && newFilmStock.getId() == filmStock.getId()) {
                            checkedItem = counter;
                        }
                        counter++;
                    }
                    final CharSequence[] filmStockNames_ = filmStockNames.toArray(new CharSequence[0]);
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
                    builder1.setSingleChoiceItems(filmStockNames_, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            newFilmStock = filmStocks.get(which);
                            filmStockTextView.setText(newFilmStock.getName());
                            nameEditText.setHint(newFilmStock.getName());
                            filmStockClearImageView.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                        }
                    });
                    builder1.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    });
                    builder1.create().show();
                }
            });
            builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                }
            });
            builder.create().show();
        }

    }
}
