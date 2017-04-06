package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.LocationPickActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dialog to edit Frame's information
 */
public class EditFrameInfoDialog extends DialogFragment {

    /**
     * Public constant used to tag the fragment when created
     */
    public static final String TAG = "EditFrameInfoDialogFragment";

    /**
     * Constant passed to LocationPickActivity for result
     */
    private final static int PLACE_PICKER_REQUEST = 1;

    /**
     * Constant passed to EditLensInfoDialog for result
     */
    private final static int ADD_LENS = 2;

    /**
     * Constant passed to EditFilterInfoDialog for result
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
    private Frame frame;

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
    private long newLensId;

    /**
     * Currently selected datetime in format 'YYYY-M-D H:MM'
     */
    private String newDate;

    /**
     * Currently selected latitude longitude location in format '12,3456... 12,3456...'
     */
    private String newLocation;

    /**
     * Database id of the currently selected filter
     */
    private long newFilterId;

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
    private int newFrameCount;

    /**
     * Currently selected shutter speed value in format 1/X, Y" or B, where X and Y are numbers
     */
    private String newShutter;

    /**
     * Currently selected aperture value, number only
     */
    private String newAperture;

    /**
     * Currently selected focal length
     */
    private int newFocalLength;

    /**
     * Currently selected exposure compensation in format
     * 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    private String newExposureComp;

    /**
     * Currently selected number of exposures (multiple exposure)
     */
    private int newNoOfExposures;

    /**
     * Button used to display the current aperture value
     */
    private Button apertureButton;

    /**
     * Button used to display the current focal length value
     */
    private Button focalLengthButton;

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
    public EditFrameInfoDialog() {
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

        String title = getArguments().getString("TITLE");
        final String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        frame = getArguments().getParcelable("FRAME");
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
                R.layout.frame_info_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = (FrameLayout) inflatedView.findViewById(R.id.root);
            NestedScrollView nestedScrollView = (NestedScrollView) inflatedView.findViewById(
                    R.id.nested_scroll_view);
            Utilities.setScrollIndicators(rootLayout, nestedScrollView,
                    ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM);
        }
        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflatedView);




        //==========================================================================================
        //LENS BUTTON
        lensTextView = (TextView) inflatedView.findViewById(R.id.btn_lens);
        if (frame.getLensId() > 0) {
            Lens currentLens = database.getLens(frame.getLensId());
            lensTextView.setText(currentLens.getMake() + " " + currentLens.getModel());
        }
        else lensTextView.setText(getResources().getString(R.string.NoLens));
        //==========================================================================================



        //==========================================================================================
        // LENS PICK DIALOG
        newLensId = frame.getLensId();
        lensTextView.setClickable(true);
        lensTextView.setOnClickListener(new View.OnClickListener() {
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
                        lensTextView.setText(listItems.get(which));
                        if (which > 0) {
                            newLensId = mountableLenses.get(which - 1).getId();
                            Lens lens = database.getLens(newLensId);
                            if (newFocalLength > lens.getMaxFocalLength()) {
                                newFocalLength = lens.getMaxFocalLength();
                            } else if (newFocalLength < lens.getMinFocalLength()) {
                                newFocalLength = lens.getMinFocalLength();
                            }
                            focalLengthButton.setText(String.valueOf(newFocalLength));
                            apertureIncrements = database.getLens(newLensId).getApertureIncrements();

                            //Check the aperture value's validity against the new lens' properties.
                            checkApertureValueValidity();
                        }
                        else if (which == 0) {
                            newLensId = -1;
                            newFocalLength = 0;
                            focalLengthButton.setText(String.valueOf(newFocalLength));
                            apertureIncrements = 0;
                        }

                        initialiseFilters();
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
        final Button addLensButton = (Button) inflatedView.findViewById(R.id.btn_add_lens);
        addLensButton.setClickable(true);
        addLensButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                EditLensInfoDialog dialog = new EditLensInfoDialog();
                dialog.setTargetFragment(EditFrameInfoDialog.this, ADD_LENS);
                Bundle arguments = new Bundle();
                arguments.putString("TITLE", getResources().getString(R.string.NewLens));
                arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditLensInfoDialog.TAG);
            }
        });
        //==========================================================================================



        //==========================================================================================
        // DATE PICK DIALOG
        final TextView dateTextView = (TextView) inflatedView.findViewById(R.id.btn_date);
        final TextView timeTextView = (TextView) inflatedView.findViewById(R.id.btn_time);
        if (frame.getDate() == null) frame.setDate(Utilities.getCurrentTime());
        List<String> dateValue = Utilities.splitDate(frame.getDate());
        int tempYear = Integer.parseInt(dateValue.get(0));
        int tempMonth = Integer.parseInt(dateValue.get(1));
        int tempDay = Integer.parseInt(dateValue.get(2));
        dateTextView.setText(tempYear + "-" + tempMonth + "-" + tempDay);

        dateTextView.setClickable(true);

        newDate = frame.getDate();

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
        //==========================================================================================



        //==========================================================================================
        // TIME PICK DIALOG
        List<String> timeValue = Utilities.splitTime(frame.getDate());
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
        //==========================================================================================



        //==========================================================================================
        //NOTES FIELD
        final TextView noteTextView = (TextView) inflatedView.findViewById(R.id.txt_note);
        noteTextView.setText(frame.getNote());
        //==========================================================================================



        //==========================================================================================
        //COUNT BUTTON
        newFrameCount = frame.getCount();
        final Button frameCountButton = (Button) inflatedView.findViewById(R.id.btn_frame_count);
        frameCountButton.setText(String.valueOf(newFrameCount));
        frameCountButton.setOnClickListener(new View.OnClickListener() {
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
                                frameCountButton.setText(String.valueOf(newFrameCount));
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
        final Button shutterButton = (Button) inflatedView.findViewById(R.id.btn_shutter);
        updateShutterButton(shutterButton);
        shutterButton.setOnClickListener(new View.OnClickListener() {
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
                                updateShutterButton(shutterButton);
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
        apertureButton = (Button) inflatedView.findViewById(R.id.btn_aperture);
        updateApertureButton();
        apertureButton.setOnClickListener(new View.OnClickListener() {
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
                                updateApertureButton();
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
        focalLengthButton = (Button) inflatedView.findViewById(R.id.btn_focal_length);
        focalLengthButton.setText(String.valueOf(newFocalLength));
        focalLengthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.single_numberpicker_dialog, null);
                final NumberPicker focalLengthPicker = (NumberPicker) dialogView.findViewById(R.id.number_picker);

                focalLengthPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

                initialiseFocalLengthPicker(focalLengthPicker);

                builder.setView(dialogView);
                builder.setTitle(getResources().getString(R.string.ChooseFocalLength));
                builder.setPositiveButton(getResources().getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newFocalLength = focalLengthPicker.getValue();
                                focalLengthButton.setText(String.valueOf(newFocalLength));
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
        final Button exposureCompButton = (Button) inflatedView.findViewById(R.id.btn_exposure_comp);
        exposureCompButton.setText(
                newExposureComp == null || newExposureComp.equals("0") ? "±0" : newExposureComp
        );
        exposureCompButton.setOnClickListener(new View.OnClickListener() {
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
                                exposureCompButton.setText(
                                        newExposureComp == null || newExposureComp.equals("0") ? "±0" : newExposureComp
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
        final Button noOfExposuresButton = (Button) inflatedView.findViewById(R.id.btn_no_of_exposures);
        noOfExposuresButton.setText(String.valueOf(newNoOfExposures));
        noOfExposuresButton.setOnClickListener(new View.OnClickListener() {
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
                                noOfExposuresButton.setText(String.valueOf(newNoOfExposures));
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
        locationTextView = (TextView) inflatedView.findViewById(R.id.btn_location);
        newLocation = frame.getLocation();
        updateLocationButton();
        locationTextView.setClickable(true);
        locationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // LOCATION PICKER DIALOG IMPLEMENTATION HERE
                final List<String> listItems = new ArrayList<>();
                listItems.add(getResources().getString(R.string.Clear));
                listItems.add(getResources().getString(R.string.Reacquire));
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ChooseAction));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        switch (which) {
                            // Clear
                            case 0:
                                newLocation = "";
                                updateLocationButton();
                                break;

                            // Reacquire/Edit on map. LocationPickActivity!
                            case 1:
                                Intent intent = new Intent(getActivity(), LocationPickActivity.class);
                                intent.putExtra("LOCATION", newLocation);
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
        //==========================================================================================


        //==========================================================================================
        //FILTER BUTTON
        filterTextView = (TextView) inflatedView.findViewById(R.id.btn_filter);
        if (frame.getFilterId() > 0) {
            Filter currentFilter = database.getFilter(frame.getFilterId());
            filterTextView.setText(currentFilter.getMake() + " " + currentFilter.getModel());
        }
        else {
            filterTextView.setText(getResources().getString(R.string.NoFilter));
        }

        // FILTER PICK DIALOG
        newFilterId = frame.getFilterId();
        filterTextView.setClickable(true);
        filterTextView.setOnClickListener(new View.OnClickListener() {
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
                        filterTextView.setText(listItems.get(which));
                        if (which > 0) {
                            newFilterId = mountableFilters.get(which - 1).getId();
                        }
                        else if (which == 0) {
                            newFilterId = -1;
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
        final Button addFilterButton = (Button) inflatedView.findViewById(R.id.btn_add_filter);
        addFilterButton.setClickable(true);
        addFilterButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitTransaction")
            @Override
            public void onClick(View v) {
                if (newLensId <= 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.SelectLensToAddFilters),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                EditFilterInfoDialog dialog = new EditFilterInfoDialog();
                dialog.setTargetFragment(EditFrameInfoDialog.this, ADD_FILTER);
                Bundle arguments = new Bundle();
                arguments.putString("TITLE", getResources().getString(R.string.NewFilter));
                arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
                dialog.setArguments(arguments);
                dialog.show(getFragmentManager().beginTransaction(), EditFilterInfoDialog.TAG);
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
                frame.setShutter(newShutter);
                frame.setAperture(newAperture);
                frame.setCount(newFrameCount);
                frame.setNote(noteTextView.getText().toString());

                // PARSE THE DATE
                frame.setDate(newDate);

                frame.setLensId(newLensId);
                frame.setLocation(newLocation);
                frame.setFilterId(newFilterId);
                frame.setExposureComp(newExposureComp);
                frame.setNoOfExposures(newNoOfExposures);
                frame.setFocalLength(newFocalLength);

                // Return the new entered name to the calling activity
                Intent intent = new Intent();
                intent.putExtra("FRAME", frame);
                dialog.dismiss();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, intent);

            }
        });

        return dialog;
    }


    /**
     * Executed when an activity or fragment, which is started for result, sends an onActivityResult
     * signal to this fragment.
     *
     * Handle LocationPickActivity, EditLensInfoDialog or EditFilterInfoDialog's result.
     *
     * @param requestCode the request code that was set for the intent
     * @param resultCode the result code to tell whether the user picked ok or cancel
     * @param data the extra data attached to the passed intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data.hasExtra("LATITUDE") && data.hasExtra("LONGITUDE")) {
                newLocation = "" + data.getStringExtra("LATITUDE") + " " +
                        data.getStringExtra("LONGITUDE");
                updateLocationButton();
            }
        }

        if (requestCode == ADD_LENS && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            Lens lens = data.getParcelableExtra("LENS");

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
            focalLengthButton.setText(String.valueOf(newFocalLength));
            initialiseFilters();
        }

        if (requestCode == ADD_FILTER && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            Filter filter = data.getParcelableExtra("FILTER");
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
     * Called when the focal length dialog is opened. Set the values for the NumberPicker.
     *
     * @param focalLengthPicker NumberPicker associated with the focal length
     */
    private void initialiseFocalLengthPicker(NumberPicker focalLengthPicker){
        Lens lens = null;
        if (newLensId > 0) lens = database.getLens(newLensId);
        int minValue;
        int maxValue;
        if (lens != null) {
            minValue = lens.getMinFocalLength();
            maxValue = lens.getMaxFocalLength();
        } else {
            minValue = 0;
            maxValue = 1500;
        }
        focalLengthPicker.setMinValue(minValue);
        focalLengthPicker.setMaxValue(maxValue);
        if (newFocalLength > maxValue) {
            focalLengthPicker.setValue(maxValue);
        } else if (newFocalLength < minValue) {
            focalLengthPicker.setValue(minValue);
        } else {
            focalLengthPicker.setValue(newFocalLength);
        }
    }

    /**
     * Update the filter button's text and currently selected filter.
     * If the currently selected lens does not mount with
     * the currently selected filter, then reset the filter.
     */
    private void initialiseFilters(){
        //Update mountable filters
        if (newLensId <= 0) {
            filterTextView.setText(getResources().getString(R.string.NoFilter));
            newFilterId = -1;
        } else {
            List<Filter> mountableFilters = database.getMountableFilters(database.getLens(newLensId));
            //If the new list contains the current filter, do nothing (return)
            for (Filter filter : mountableFilters) {
                if (filter.getId() == newFilterId) {
                    return;
                }
            }
            //Else reset the filter
            filterTextView.setText(getResources().getString(R.string.NoFilter));
            newFilterId = -1;
        }
    }

    /**
     * Updates the shutter speed value button's text.
     *
     * @param button the button whose text should be updated
     */
    private void updateShutterButton(Button button){
        if (button != null) button.setText(newShutter);
    }

    /**
     * Updates the aperture value button's text.
     */
    private void updateApertureButton(){
        if (apertureButton != null) {
            if (newAperture.contains("<") || newAperture.contains(">")) {
                apertureButton.setText(newAperture);
            } else {
                String newText = "f/" + newAperture;
                apertureButton.setText(newText);
            }
        }
    }

    /**
     * Updates the location button's text.
     */
    private void updateLocationButton(){
        locationTextView.setText(
                newLocation == null || newLocation.length() == 0 ?
                        getResources().getString(R.string.ClickToSet) :
                        Utilities.getReadableLocationFromString(newLocation)
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
            updateApertureButton();
        }
    }

}