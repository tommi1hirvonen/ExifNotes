package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.NonNull;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
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
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.LocationPickActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.GeocodingAsyncTask;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.round;

/**
 * Dialog to edit Frame's information
 */
public class EditFrameDialog extends DialogFragment {

    /**
     * Public constant used to tag the fragment when created
     */
    public static final String TAG = "EditFrameDialog";

    /**
     * Constant passed to LocationPickActivity for result
     */
    private final static int PLACE_PICKER_REQUEST = 1;

    /**
     * Constant passed to EditLensDialog for result
     */
    private final static int ADD_LENS = 2;

    /**
     * Constant passed to EditFilterDialog for result
     */
    private final static int ADD_FILTER = 3;

    /**
     * Database id of the camera used to take this frame
     */
    private long cameraId;

    /**
     * Reference to the camera used to take this frame
     */
    private Camera camera;

    /**
     * Holds the information of the edited frame
     */
    Frame frame;

    /**
     * Holds all the lenses that can be mounted to the used camera
     */
    private List<Lens> mountableLenses;

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * Button used to display the currently selected location
     */
    private TextView locationTextView;

    /**
     * Button used to display the currently selected lens
     */
    private TextView lensTextView;

    /**
     * Button used to display the currently selected filter
     */
    private TextView filterTextView;

    /**
     * Database id of the currently selected lens
     */
    long newLensId;

    /**
     * Currently selected datetime in format 'YYYY-M-D H:MM'
     */
    String newDate;

    /**
     * Currently selected latitude longitude location in format '12,3456... 12,3456...'
     */
    String newLocation;

    /**
     * Currently set formatted address for location
     */
    String newFormattedAddress;

    /**
     * Database id of the currently selected filter
     */
    long newFilterId;

    /**
     * Currently selected lens's aperture increment setting
     */
    private int apertureIncrements;

    /**
     * The shutter speed increment setting of the camera used
     */
    private int shutterIncrements;

    /**
     * Currently selected frame count number
     */
    int newFrameCount;

    /**
     * Currently selected shutter speed value in format 1/X, Y" or B, where X and Y are numbers
     */
    String newShutter;

    /**
     * Currently selected aperture value, number only
     */
    String newAperture;

    /**
     * Currently selected focal length
     */
    int newFocalLength;

    /**
     * Currently selected exposure compensation in format
     * 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    String newExposureComp;

    /**
     * Currently selected number of exposures (multiple exposure)
     */
    int newNoOfExposures;

    /**
     * TextView used to display the current aperture value
     */
    private TextView apertureTextView;

    /**
     * Button used to display the current focal length value
     */
    private TextView focalLengthTextView;

    /**
     * Reference to the EditText used to edit notes
     */
    private EditText noteEditText;

    /**
     * Reference to the utilities class
     */
    private Utilities utilities;

    /**
     * Stores the currently displayed shutter speed values.
     */
    private String[] displayedShutterValues;

    /**
     * Stores the currently displayed aperture values.
     * Changes depending on the currently selected lens.
     */
    private String[] displayedApertureValues;

    /**
     * Empty constructor
     */
    public EditFrameDialog() {
        // Empty constructor required for DialogFragment
    }

    /**
     * Called when the DialogFragment is ready to create the dialog.
     * Inflate the fragment. Get the edited frame and used camera.
     * Initialize UI objects and display the frame's information.
     * Add listeners to buttons to open new dialogs to change the frame's information.
     *
     * @param SavedInstanceState possible saved state in case the DialogFragment was resumed
     * @return inflated dialog ready to be shown
     */
    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        utilities = new Utilities(getActivity());

        String title = getArguments().getString(ExtraKeys.TITLE);
        final String positiveButton = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        frame = getArguments().getParcelable(ExtraKeys.FRAME);
        if (frame == null) frame = new Frame();

        database = FilmDbHelper.getInstance(getActivity());
        cameraId = database.getRoll(frame.getRollId()).getCameraId();
        camera = database.getCamera(cameraId);
        mountableLenses = database.getMountableLenses(camera);

        shutterIncrements = camera.getShutterIncrements();
        apertureIncrements = 0;
        if (frame.getLensId() > 0) {
            apertureIncrements = database.getLens(frame.getLensId()).getApertureIncrements();
        }

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.frame_dialog, null);
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
        if (Utilities.isAppThemeDark(getActivity())) {
            List<View> dividerList = new ArrayList<>();
            dividerList.add(inflatedView.findViewById(R.id.divider_view1));
            dividerList.add(inflatedView.findViewById(R.id.divider_view2));
            dividerList.add(inflatedView.findViewById(R.id.divider_view3));
            dividerList.add(inflatedView.findViewById(R.id.divider_view4));
            dividerList.add(inflatedView.findViewById(R.id.divider_view5));
            dividerList.add(inflatedView.findViewById(R.id.divider_view6));
            dividerList.add(inflatedView.findViewById(R.id.divider_view7));
            dividerList.add(inflatedView.findViewById(R.id.divider_view8));
            dividerList.add(inflatedView.findViewById(R.id.divider_view9));
            dividerList.add(inflatedView.findViewById(R.id.divider_view10));
            dividerList.add(inflatedView.findViewById(R.id.divider_view11));
            for (View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
            ((ImageView) inflatedView.findViewById(R.id.add_lens)).getDrawable().mutate()
                    .setColorFilter(
                            ContextCompat.getColor(getActivity(), R.color.white),
                            PorterDuff.Mode.SRC_IN
                    );
            ((ImageView) inflatedView.findViewById(R.id.add_filter)).getDrawable().mutate()
                    .setColorFilter(
                            ContextCompat.getColor(getActivity(), R.color.white),
                            PorterDuff.Mode.SRC_IN
                    );
            ((ImageView) inflatedView.findViewById(R.id.clear_location)).getDrawable().mutate()
                    .setColorFilter(
                            ContextCompat.getColor(getActivity(), R.color.white),
                            PorterDuff.Mode.SRC_IN
                    );
        }
        //==========================================================================================



        //==========================================================================================
        //LENS TEXT
        lensTextView = (TextView) inflatedView.findViewById(R.id.lens_text);
        if (frame.getLensId() > 0) {
            Lens currentLens = database.getLens(frame.getLensId());
            lensTextView.setText(currentLens.getMake() + " " + currentLens.getModel());
        }
        else lensTextView.setText("");
        //==========================================================================================



        //==========================================================================================
        // LENS PICK DIALOG
        newLensId = frame.getLensId();
        final LinearLayout lensLayout = (LinearLayout) inflatedView.findViewById(R.id.lens_layout);
        lensLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedItem = 0; // default option is 'no lens' (first one the list)
                final List<String> listItems = new ArrayList<>();
                listItems.add(getResources().getString(R.string.NoLens));
                for (int i = 0; i < mountableLenses.size(); ++i) {
                    listItems.add(mountableLenses.get(i).getMake() + " " +
                            mountableLenses.get(i).getModel());

                    //If the id's match, set the initial checkedItem.
                    // Account for the 'no lens' option with the + 1
                    if (mountableLenses.get(i).getId() == newLensId) checkedItem = i + 1;
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedLens);
                builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // listItems also contains the No lens option

                        // Check if the lens was changed
                        if (which > 0 && newLensId != mountableLenses.get(which - 1).getId()) {
                            lensTextView.setText(listItems.get(which));
                            newLensId = mountableLenses.get(which - 1).getId();
                            Lens lens = database.getLens(newLensId);
                            if (newFocalLength > lens.getMaxFocalLength()) {
                                newFocalLength = lens.getMaxFocalLength();
                            } else if (newFocalLength < lens.getMinFocalLength()) {
                                newFocalLength = lens.getMinFocalLength();
                            }
                            focalLengthTextView.setText(
                                    newFocalLength == 0 ? "" : String.valueOf(newFocalLength)
                            );
                            apertureIncrements = database.getLens(newLensId).getApertureIncrements();

                            //Check the aperture value's validity against the new lens' properties.
                            checkApertureValueValidity();

                            // The lens was changed, reset filters
                            resetFilters();
                        }
                        // No lens option was selected
                        else if (which == 0) {
                            lensTextView.setText("");
                            newLensId = -1;
                            newFocalLength = 0;
                            updateFocalLengthTextView();
                            apertureIncrements = 0;
                            resetFilters();
                        }

                        dialog.dismiss();
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
        // LENS ADD DIALOG
        final ImageView addLensImageView = (ImageView) inflatedView.findViewById(R.id.add_lens);
        addLensImageView.setClickable(true);
        addLensImageView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                noteEditText.clearFocus();
                EditLensDialog dialog = new EditLensDialog();
                dialog.setTargetFragment(EditFrameDialog.this, ADD_LENS);
                Bundle arguments = new Bundle();
                arguments.putString(ExtraKeys.TITLE, getResources().getString(R.string.NewLens));
                arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditLensDialog.TAG);
            }
        });
        //==========================================================================================



        //==========================================================================================
        // DATE PICK DIALOG
        final TextView dateTextView = (TextView) inflatedView.findViewById(R.id.date_text);
        final TextView timeTextView = (TextView) inflatedView.findViewById(R.id.time_text);
        if (frame.getDate() == null) frame.setDate(Utilities.getCurrentTime());
        List<String> dateValue = Utilities.splitDate(frame.getDate());
        int tempYear = Integer.parseInt(dateValue.get(0));
        int tempMonth = Integer.parseInt(dateValue.get(1));
        int tempDay = Integer.parseInt(dateValue.get(2));
        dateTextView.setText(tempYear + "-" + tempMonth + "-" + tempDay);

        newDate = frame.getDate();

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
        List<String> timeValue = Utilities.splitTime(frame.getDate());
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
        //NOTES FIELD
        noteEditText = (EditText) inflatedView.findViewById(R.id.note_editText);
        noteEditText.setSingleLine(false);
        noteEditText.setText(frame.getNote());
        noteEditText.setSelection(noteEditText.getText().length());
        //==========================================================================================



        //==========================================================================================
        //COUNT BUTTON
        newFrameCount = frame.getCount();
        final TextView frameCountTextView = (TextView) inflatedView.findViewById(R.id.frame_count_text);
        frameCountTextView.setText(String.valueOf(newFrameCount));
        final LinearLayout frameCountLayout = (LinearLayout) inflatedView.findViewById(R.id.frame_count_layout);
        frameCountLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker frameCountPicker = (NumberPicker) dialogView.findViewById(R.id.number_picker);
                frameCountPicker.setMinValue(0);
                frameCountPicker.setMaxValue(100);
                frameCountPicker.setValue(newFrameCount);
                frameCountPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseFrameCount));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newFrameCount = frameCountPicker.getValue();
                                frameCountTextView.setText(String.valueOf(newFrameCount));
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
        //SHUTTER SPEED BUTTON
        newShutter = frame.getShutter();
        final TextView shutterTextView = (TextView) inflatedView.findViewById(R.id.shutter_text);
        updateShutterTextView(shutterTextView);
        final LinearLayout shutterLayout = (LinearLayout) inflatedView.findViewById(R.id.shutter_layout);
        shutterLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker shutterPicker = (NumberPicker) dialogView.findViewById(R.id.number_picker);

                initialiseShutterPicker(shutterPicker);

                shutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseShutterSpeed));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newShutter = displayedShutterValues[shutterPicker.getValue()];
                                updateShutterTextView(shutterTextView);
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
        //APERTURE BUTTON
        newAperture = frame.getAperture();
        apertureTextView = (TextView) inflatedView.findViewById(R.id.aperture_text);
        updateApertureTextView();
        final LinearLayout apertureLayout = (LinearLayout) inflatedView.findViewById(R.id.aperture_layout);
        apertureLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker aperturePicker = (NumberPicker) dialogView.findViewById(R.id.number_picker);

                initialiseAperturePicker(aperturePicker);

                aperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseApertureValue));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newAperture = displayedApertureValues[aperturePicker.getValue()];
                                updateApertureTextView();
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
        //FOCAL LENGTH BUTTON
        newFocalLength = frame.getFocalLength();
        focalLengthTextView = (TextView) inflatedView.findViewById(R.id.focal_length_text);
        updateFocalLengthTextView();
        final LinearLayout focalLengthLayout = (LinearLayout) inflatedView.findViewById(R.id.focal_length_layout);
        focalLengthLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.seek_bar_dialog, null);
                final SeekBar focalLengthSeekBar = (SeekBar) dialogView.findViewById(R.id.seek_bar);
                final TextView focalLengthTextView = (TextView) dialogView.findViewById(R.id.value_text_view);

                // Get the min and max focal lengths
                Lens lens = null;
                if (newLensId > 0) lens = database.getLens(newLensId);
                final int minValue;
                final int maxValue;
                if (lens != null) {
                    minValue = lens.getMinFocalLength();
                    maxValue = lens.getMaxFocalLength();
                } else {
                    minValue = 0;
                    maxValue = 500;
                }

                // Set the SeekBar progress percent
                if (newFocalLength > maxValue) {
                    focalLengthSeekBar.setProgress(100);
                    focalLengthTextView.setText(String.valueOf(maxValue));
                } else if (newFocalLength < minValue) {
                    focalLengthSeekBar.setProgress(0);
                    focalLengthTextView.setText(String.valueOf(minValue));
                } else if (minValue == maxValue) {
                    focalLengthSeekBar.setProgress(50);
                    focalLengthTextView.setText(String.valueOf(minValue));
                } else {
                    focalLengthSeekBar.setProgress(calculateProgress(newFocalLength, minValue, maxValue));
                    focalLengthTextView.setText(String.valueOf(newFocalLength));
                }

                // When the user scrolls the SeekBar, change the TextView to indicate
                // the current focal length converted from the progress (int i)
                focalLengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        int focalLength = minValue + (maxValue - minValue) * i / 100;
                        focalLengthTextView.setText(String.valueOf(focalLength));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Do nothing
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Do nothing
                    }
                });

                final TextView increaseFocalLength = (TextView) dialogView.findViewById(R.id.increase_focal_length);
                final TextView decreaseFocalLength = (TextView) dialogView.findViewById(R.id.decrease_focal_length);
                increaseFocalLength.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int focalLength = Integer.parseInt(focalLengthTextView.getText().toString());
                        if (focalLength < maxValue) {
                            ++focalLength;
                            focalLengthSeekBar.setProgress(calculateProgress(focalLength, minValue, maxValue));
                            focalLengthTextView.setText(String.valueOf(focalLength));
                        }
                    }
                });
                decreaseFocalLength.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int focalLength = Integer.parseInt(focalLengthTextView.getText().toString());
                        if (focalLength > minValue) {
                            --focalLength;
                            focalLengthSeekBar.setProgress(calculateProgress(focalLength, minValue, maxValue));
                            focalLengthTextView.setText(String.valueOf(focalLength));
                        }
                    }
                });


                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseFocalLength));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newFocalLength = Integer.parseInt(focalLengthTextView.getText().toString());
                                updateFocalLengthTextView();
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
        //EXPOSURE COMP BUTTON
        newExposureComp = frame.getExposureComp();
        final TextView exposureCompTextView = (TextView) inflatedView.findViewById(R.id.exposure_comp_text);
        exposureCompTextView.setText(
                newExposureComp == null || newExposureComp.equals("0") ? "" : newExposureComp
        );
        final LinearLayout exposureCompLayout = (LinearLayout) inflatedView.findViewById(R.id.exposure_comp_layout);
        exposureCompLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker exposureCompPicker = Utilities.fixNumberPicker(
                        (NumberPicker) dialogView.findViewById(R.id.number_picker)
                );

                exposureCompPicker.setMinValue(0);
                exposureCompPicker.setMaxValue(Utilities.compValues.length-1);
                exposureCompPicker.setDisplayedValues(Utilities.compValues);
                exposureCompPicker.setValue(9);
                if (newExposureComp != null) {
                    for (int i = 0; i < Utilities.compValues.length; ++i) {
                        if (newExposureComp.equals(Utilities.compValues[i])) {
                            exposureCompPicker.setValue(i);
                            break;
                        }
                    }
                }

                exposureCompPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseExposureComp));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newExposureComp = Utilities.compValues[exposureCompPicker.getValue()];
                                exposureCompTextView.setText(
                                        newExposureComp == null || newExposureComp.equals("0") ? "" : newExposureComp
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
        //NO OF EXPOSURES BUTTON

        //Check that the number is bigger than zero.
        newNoOfExposures = frame.getNoOfExposures() > 0 ? frame.getNoOfExposures() : 1;
        final TextView noOfExposuresTextView = (TextView) inflatedView.findViewById(R.id.no_of_exposures_text);
        noOfExposuresTextView.setText(String.valueOf(newNoOfExposures));
        final LinearLayout noOfExposuresLayout = (LinearLayout) inflatedView.findViewById(R.id.no_of_exposures_layout);
        noOfExposuresLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker noOfExposuresPicker = (NumberPicker) dialogView.findViewById(R.id.number_picker);

                noOfExposuresPicker.setMinValue(1);
                noOfExposuresPicker.setMaxValue(10);
                noOfExposuresPicker.setValue(1);
                if (newNoOfExposures > 1) {
                    noOfExposuresPicker.setValue(newNoOfExposures);
                }

                noOfExposuresPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseNoOfExposures));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newNoOfExposures = noOfExposuresPicker.getValue();
                                noOfExposuresTextView.setText(String.valueOf(newNoOfExposures));
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
        // LOCATION PICK DIALOG
        locationTextView = (TextView) inflatedView.findViewById(R.id.location_text);
        newLocation = frame.getLocation();
        newFormattedAddress = frame.getFormattedAddress();
        updateLocationTextView();

        final ProgressBar locationProgressBar = (ProgressBar) inflatedView.findViewById(R.id.location_progress_bar);

        // If the formatted address is empty, try to find it
        if (newLocation != null && newLocation.length() > 0 &&
                (newFormattedAddress == null || newFormattedAddress .length() == 0)) {
            // Make the ProgressBar visible to indicate that a query is being executed
            locationProgressBar.setVisibility(View.VISIBLE);
            new GeocodingAsyncTask(new GeocodingAsyncTask.AsyncResponse() {
                @Override
                public void processFinish(String output, String formatted_address) {
                    locationProgressBar.setVisibility(View.INVISIBLE);
                    if (formatted_address.length() > 0 ) {
                        newFormattedAddress = formatted_address;
                    } else {
                        newFormattedAddress = null;
                    }
                    updateLocationTextView();
                }
            }).execute(newLocation);
        }

        final ImageView clearLocation = (ImageView) inflatedView.findViewById(R.id.clear_location);
        clearLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newLocation = null;
                newFormattedAddress = null;
                updateLocationTextView();
            }
        });
        final LinearLayout locationLayout = (LinearLayout) inflatedView.findViewById(R.id.location_layout);
        locationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), LocationPickActivity.class);
                intent.putExtra(ExtraKeys.LOCATION, newLocation);
                intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, newFormattedAddress);
                startActivityForResult(intent, PLACE_PICKER_REQUEST);
            }
        });
        //==========================================================================================


        //==========================================================================================
        //FILTER BUTTON
        filterTextView = (TextView) inflatedView.findViewById(R.id.filter_text);
        if (frame.getFilterId() > 0) {
            Filter currentFilter = database.getFilter(frame.getFilterId());
            filterTextView.setText(currentFilter.getMake() + " " + currentFilter.getModel());
        }
        else {
            filterTextView.setText("");
        }

        // FILTER PICK DIALOG
        newFilterId = frame.getFilterId();
        final LinearLayout filterLayout = (LinearLayout) inflatedView.findViewById(R.id.filter_layout);
        filterLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedItem = 0; //default option is 0, no filter
                final List<String> listItems = new ArrayList<>();
                listItems.add(getResources().getString(R.string.NoFilter));
                final List<Filter> mountableFilters;
                if (newLensId > 0) {
                    mountableFilters = database.getMountableFilters(database.getLens(newLensId));
                    for (int i = 0; i < mountableFilters.size(); ++i) {
                        listItems.add(mountableFilters.get(i).getMake() + " " +
                                mountableFilters.get(i).getModel());
                        if (mountableFilters.get(i).getId() == newFilterId) checkedItem = i + 1;
                    }
                } else {
                    mountableFilters = new ArrayList<>();
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedFilter);
                builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        if (which > 0) {
                            filterTextView.setText(listItems.get(which));
                            newFilterId = mountableFilters.get(which - 1).getId();
                        }
                        else if (which == 0) {
                            resetFilters();
                        }
                        dialog.dismiss();
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

        // FILTER ADD DIALOG
        final ImageView addFilterImageView = (ImageView) inflatedView.findViewById(R.id.add_filter);
        addFilterImageView.setClickable(true);
        addFilterImageView.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                if (newLensId <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.SelectLensToAddFilters),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                noteEditText.clearFocus();
                EditFilterDialog dialog = new EditFilterDialog();
                dialog.setTargetFragment(EditFrameDialog.this, ADD_FILTER);
                Bundle arguments = new Bundle();
                arguments.putString(ExtraKeys.TITLE, getResources().getString(R.string.NewFilter));
                arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditFilterDialog.TAG);
            }
        });
        //==========================================================================================








        //==========================================================================================
        //FINALISE BUILDING THE DIALOG

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
        });

        alert.setPositiveButton(positiveButton, null);

        final AlertDialog dialog = alert.create();

        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();


        //User pressed OK, save.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalizeFrame();

                // Return the new entered name to the calling activity
                Intent intent = new Intent();
                intent.putExtra(ExtraKeys.FRAME, frame);
                dialog.dismiss();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, intent);

            }
        });

        return dialog;
    }

    /**
     * Calculates progress as integer ranging from 0 to 100
     * from current focal length, minimum focal length and maximum focal length.
     * Return 0 if current focal length equals minimum focal length, 100 if current
     * focal length equals maximum focal length, 50 if current focal length is midway
     * in the focal length range and so on...
     *
     * @param focalLength current focal length value
     * @param minValue minimum focal length value
     * @param maxValue maximum focal length value
     * @return integer from 0 to 100 describing progress
     */
    private int calculateProgress (int focalLength, int minValue, int maxValue) {
        // progress = (newFocalLength - minValue) / (maxValue - minValue) * 100
        double result1 = focalLength - minValue;
        double result2 = maxValue - minValue;
        // No variables of type int can be used if parts of the calculation
        // result in fractions.
        double progressDouble = result1 / result2 * 100;
        return (int) round(progressDouble);
    }

    /**
     * Package-private method to finalize the member that is passed to the target fragment
     * Also used in the child class EditFrameDialogCallback
     */
    void finalizeFrame(){
        frame.setShutter(newShutter);
        frame.setAperture(newAperture);
        frame.setCount(newFrameCount);
        frame.setNote(noteEditText.getText().toString());

        // PARSE THE DATE
        frame.setDate(newDate);

        frame.setLensId(newLensId);
        frame.setLocation(newLocation);
        frame.setFormattedAddress(newFormattedAddress);
        frame.setFilterId(newFilterId);
        frame.setExposureComp(newExposureComp);
        frame.setNoOfExposures(newNoOfExposures);
        frame.setFocalLength(newFocalLength);
    }


    /**
     * Executed when an activity or fragment, which is started for result, sends an onActivityResult
     * signal to this fragment.
     *
     * Handle LocationPickActivity, EditLensDialog or EditFilterDialog's result.
     *
     * @param requestCode the request code that was set for the intent
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {

            // Set the location
            if (data.hasExtra(ExtraKeys.LATITUDE) && data.hasExtra(ExtraKeys.LONGITUDE)) {
                newLocation = "" + data.getStringExtra(ExtraKeys.LATITUDE) + " " +
                        data.getStringExtra(ExtraKeys.LONGITUDE);
            }

            // Set the formatted address
            if (data.hasExtra(ExtraKeys.FORMATTED_ADDRESS)) {
                newFormattedAddress = data.getStringExtra(ExtraKeys.FORMATTED_ADDRESS);
            }

            updateLocationTextView();
        }

        if (requestCode == ADD_LENS && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            Lens lens = data.getParcelableExtra(ExtraKeys.LENS);

            long rowId = database.addLens(lens);
            lens.setId(rowId);
            database.addMountable(database.getCamera(cameraId), lens);
            mountableLenses.add(lens);
            lensTextView.setText(lens.getMake() + " " + lens.getModel());
            newLensId = lens.getId();
            apertureIncrements = lens.getApertureIncrements();
            checkApertureValueValidity();
            if (newFocalLength > lens.getMaxFocalLength()) {
                newFocalLength = lens.getMaxFocalLength();
            } else if (newFocalLength < lens.getMinFocalLength()) {
                newFocalLength = lens.getMinFocalLength();
            }
            updateFocalLengthTextView();
            resetFilters();
        }

        if (requestCode == ADD_FILTER && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            Filter filter = data.getParcelableExtra(ExtraKeys.FILTER);
            long rowId = database.addFilter(filter);
            filter.setId(rowId);
            database.addMountableFilterLens(filter, database.getLens(newLensId));
            filterTextView.setText(filter.getMake() + " " + filter.getModel());
            newFilterId = filter.getId();
        }
    }

    /**
     * Called when the aperture value dialog is opened.
     * Sets the values for the NumberPicker.
     *
     * @param aperturePicker NumberPicker associated with the aperture value
     */
    @SuppressWarnings("ManualArrayToCollectionCopy")
    private void initialiseAperturePicker(NumberPicker aperturePicker){
        //Get the array of displayed aperture values according to the set increments.
        switch (apertureIncrements) {
            case 0:
                displayedApertureValues = utilities.apertureValuesThird;
                break;
            case 1:
                displayedApertureValues = utilities.apertureValuesHalf;
                break;
            case 2:
                displayedApertureValues = utilities.apertureValuesFull;
                break;
            default:
                displayedApertureValues = utilities.apertureValuesThird;
                break;
        }
        //Reverse the order if necessary.
        if (displayedApertureValues[0].equals(getResources().getString(R.string.NoValue))) {
            // By reversing the order we can reverse the order in the NumberPicker too
            Collections.reverse(Arrays.asList(displayedApertureValues));
        }

        //If no lens is selected, end here
        if (newLensId <= 0) {
            aperturePicker.setDisplayedValues(null);
            aperturePicker.setMinValue(0);
            aperturePicker.setMaxValue(displayedApertureValues.length-1);
            aperturePicker.setDisplayedValues(displayedApertureValues);
            aperturePicker.setValue(displayedApertureValues.length-1);
            for (int i = 0; i < displayedApertureValues.length; ++i) {
                if (newAperture.equals(displayedApertureValues[i])) {
                    aperturePicker.setValue(i);
                }
            }
            return;
        }

        //Otherwise continue to set min and max apertures
        Lens lens = database.getLens(newLensId);
        List<String> apertureValuesList = new ArrayList<>(); //to store the temporary values
        int minIndex = 0;
        int maxIndex = displayedApertureValues.length-1; //we begin the maxIndex from the last element

        //Set the min and max values only if they are set for the lens.
        //Otherwise the displayedApertureValues array will be left alone
        //(all aperture values available, since min and max were not defined).
        if (lens.getMinAperture() != null && lens.getMaxAperture() != null) {
            for (int i = 0; i < displayedApertureValues.length; ++i) {
                if (lens.getMinAperture().equals(displayedApertureValues[i])) {
                    minIndex = i;
                }
                if (lens.getMaxAperture().equals(displayedApertureValues[i])) {
                    maxIndex = i;
                }
            }
            //Add the <empty> option to the beginning of the temp list.
            apertureValuesList.add(getResources().getString(R.string.NoValue));

            //Add the values between and min and max to the temp list from the initial array.
            for (int i = minIndex; i <= maxIndex; ++i) {
                apertureValuesList.add(displayedApertureValues[i]);
            }

            //Copy the temp list over the initial array.
            displayedApertureValues = apertureValuesList.toArray(new String[0]);
        }

        //Set the displayed values to null. If we set the displayed values
        //and the maxValue is smaller than the length of the new displayed values array,
        //ArrayIndexOutOfBounds is thrown.
        //Also if we set maxValue and the currently displayed values array length is smaller,
        //ArrayIndexOutOfBounds is thrown.
        //Setting displayed values to null solves this problem.
        aperturePicker.setDisplayedValues(null);
        aperturePicker.setMinValue(0);
        aperturePicker.setMaxValue(displayedApertureValues.length-1);
        aperturePicker.setDisplayedValues(displayedApertureValues);
        aperturePicker.setValue(0);
        for (int i = 0; i < displayedApertureValues.length; ++i) {
            if (newAperture.equals(displayedApertureValues[i])) {
                aperturePicker.setValue(i);
            }
        }
    }

    /**
     * Called when the shutter speed value dialog is opened.
     * Set the values for the NumberPicker
     *
     * @param shutterPicker NumberPicker associated with the shutter speed value
     */
    @SuppressWarnings("ManualArrayToCollectionCopy")
    private void initialiseShutterPicker(NumberPicker shutterPicker){
        // Set the increments according to settings
        switch (shutterIncrements) {
            case 0:
                displayedShutterValues = utilities.shutterValuesThird;
                break;
            case 1:
                displayedShutterValues = utilities.shutterValuesHalf;
                break;
            case 2:
                displayedShutterValues = utilities.shutterValuesFull;
                break;
            default:
                displayedShutterValues = utilities.shutterValuesThird;
                break;
        }
        //Reverse the order if necessary
        if (displayedShutterValues[0].equals(getResources().getString(R.string.NoValue))) {
            // By reversing the order we can reverse the order in the NumberPicker too
            Collections.reverse(Arrays.asList(displayedShutterValues));
        }
        List<String> shutterValuesList = new ArrayList<>();
        int minIndex = 0;
        int maxIndex = displayedShutterValues.length-1;

        //Set the min and max values only if they are set for the camera.
        //Otherwise the displayedShutterValues array will be left alone (apart from bulb)
        //(all shutter values available, since min and max were not defined).
        if (camera.getMinShutter() != null && camera.getMaxShutter() != null) {
            for (int i = 0; i < displayedShutterValues.length; ++i) {
                if (camera.getMinShutter().equals(displayedShutterValues[i])) {
                    minIndex = i;
                }
                if (camera.getMaxShutter().equals(displayedShutterValues[i])) {
                    maxIndex = i;
                }
            }
            //Add the no value option to the beginning.
            shutterValuesList.add(0, getResources().getString(R.string.NoValue));

            //Add the values between and min and max to the temp list from the initial array.
            for (int i = minIndex; i <= maxIndex; ++i) {
                shutterValuesList.add(displayedShutterValues[i]);
            }

            //Also add the bulb mode option.
            shutterValuesList.add("B");
            displayedShutterValues = shutterValuesList.toArray(new String[0]);
        } else {

            for (String value : displayedShutterValues)
                shutterValuesList.add(value);

            //If no min and max were set for the shutter speed, then only add bulb.
            shutterValuesList.add(shutterValuesList.size()-1, "B"); //add B between 30s and NoValue

            displayedShutterValues = shutterValuesList.toArray(new String[0]);
        }

        shutterPicker.setMinValue(0);
        shutterPicker.setMaxValue(displayedShutterValues.length-1);
        shutterPicker.setDisplayedValues(displayedShutterValues);
        shutterPicker.setValue(0);
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (newShutter.equals(displayedShutterValues[i])) {
                shutterPicker.setValue(i);
            }
        }
    }

    /**
     * Reset filters and update the filter button's text.
     */
    private void resetFilters(){
        newFilterId = -1;
        filterTextView.setText("");
    }

    /**
     * Updates the shutter speed value TextView's text.
     *
     * @param textView the TextView whose text should be updated
     */
    private void updateShutterTextView(TextView textView){
        if (textView != null) {
            if (newShutter.contains("<") || newShutter.contains(">")) {
                textView.setText("");
            } else {
                textView.setText(newShutter);
            }
        }
    }

    /**
     * Updates the aperture value button's text.
     */
    private void updateApertureTextView(){
        if (apertureTextView != null) {
            if (newAperture.contains("<") || newAperture.contains(">")) {
                apertureTextView.setText("");
            } else {
                String newText = "f/" + newAperture;
                apertureTextView.setText(newText);
            }
        }
    }

    /**
     * Updates the location button's text.
     */
    private void updateLocationTextView(){
        if (newFormattedAddress != null && newFormattedAddress.length() > 0) {
            locationTextView.setText(newFormattedAddress);
        } else if (newLocation != null && newLocation.length() > 0) {
            locationTextView.setText(
                    Utilities.getReadableLocationFromString(newLocation)
                            .replace("N ","N\n").replace("S ", "S\n")
            );
        } else {
            locationTextView.setText(" \n ");
        }
    }

    /**
     * Updates the focal length TextView
     */
    private void updateFocalLengthTextView(){
        focalLengthTextView.setText(
                newFocalLength == 0 ? "" : String.valueOf(newFocalLength)
        );
    }

    /**
     * When the currently selected lens is changed, check the validity of the currently
     * selected aperture value. I.e. it has to be within the new lens's aperture range.
     */
    private void checkApertureValueValidity(){
        //Check the aperture value's validity against the new lens' properties.
        switch (apertureIncrements) {
            case 0:
                displayedApertureValues = utilities.apertureValuesThird;
                break;
            case 1:
                displayedApertureValues = utilities.apertureValuesHalf;
                break;
            case 2:
                displayedApertureValues = utilities.apertureValuesFull;
                break;
            default:
                displayedApertureValues = utilities.apertureValuesThird;
                break;
        }

        boolean apertureFound = false;

        Lens lens = database.getLens(newLensId);
        List<String> apertureValuesList = new ArrayList<>(); //to store the temporary values
        int minIndex = 0;
        int maxIndex = displayedApertureValues.length-1; //we begin the maxIndex from the last element

        //Set the min and max values only if they are set for the lens.
        //Otherwise the displayedApertureValues array will be left alone
        //(all aperture values available, since min and max were not defined).
        if (lens.getMinAperture() != null && lens.getMaxAperture() != null) {
            for (int i = 0; i < displayedApertureValues.length; ++i) {
                if (lens.getMinAperture().equals(displayedApertureValues[i])) {
                    minIndex = i;
                }
                if (lens.getMaxAperture().equals(displayedApertureValues[i])) {
                    maxIndex = i;
                }
            }
            //Add the <empty> option to the beginning of the temp list.
            apertureValuesList.add(getResources().getString(R.string.NoValue));

            //Add the values between and min and max to the temp list from the initial array.
            //noinspection ManualArrayToCollectionCopy
            for (int i = minIndex; i <= maxIndex; ++i) {
                apertureValuesList.add(displayedApertureValues[i]);
            }

            //Copy the temp list over the initial array.
            displayedApertureValues = apertureValuesList.toArray(new String[0]);
        }

        for (String string : displayedApertureValues) {
            if (string.equals(newAperture)) {
                apertureFound = true;
                break;
            }
        }
        if (!apertureFound) {
            newAperture = getResources().getString(R.string.NoValue);
            updateApertureTextView();
        }
    }

}