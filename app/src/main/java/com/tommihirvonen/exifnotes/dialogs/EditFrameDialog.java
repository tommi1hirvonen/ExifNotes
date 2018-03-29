package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.icu.text.UnicodeSetSpanner;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
     * Constant passed to takePictureIntent for result
     */
    private static final int CAPTURE_IMAGE_REQUEST = 4;

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
     * Reference to the complementary picture's layout. If this view becomes visible,
     * load the complementary picture.
     */
    private LinearLayout pictureLayout;

    /**
     * ImageView used to display complementary image taken with the phone's camera
     */
    private ImageView pictureImageView;

    /**
     * TextView used to display text related to the complementary picture
     */
    private TextView pictureTextView;

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
     * Currently set formatted address for location
     */
    private String newFormattedAddress;

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
     * Currently selected filename of the complementary picture
     */
    private String newPictureFilename;

    /**
     * Used to temporarily store the possible new picture name. newPictureFilename is only set,
     * if the user presses ok in the camera activity. If the user cancels the camera activity,
     * then this member's value is ignored and newPictureFilename's value isn't changed.
     */
    private String tempPictureFilename;

    /**
     * TextView used to display the current aperture value
     */
    private TextView apertureTextView;

    /**
     * TextView used to display the current shutter speed value
     */
    private TextView shutterTextView;

    /**
     * TextView used to display the current frame count
     */
    private TextView frameCountTextView;

    /**
     * TextView used to display the current exposure compensation value
     */
    private TextView exposureCompTextView;

    /**
     * TextView used to display the current number of exposures value
     */
    private TextView noOfExposuresTextView;

    /*
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
     * Reference to the nested scroll view inside the dialog. A scroll listener is
     * attached to this scroll view.
     */
    private NestedScrollView nestedScrollView;

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
                R.layout.dialog_frame, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        nestedScrollView = inflatedView.findViewById(R.id.nested_scroll_view);

        // Set ScrollIndicators only if Material Design is used with the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout rootLayout = inflatedView.findViewById(R.id.root);
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
            dividerList.add(inflatedView.findViewById(R.id.divider_view12));
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
        //LENS TEXT
        lensTextView = inflatedView.findViewById(R.id.lens_text);
        if (frame.getLensId() > 0) {
            Lens currentLens = database.getLens(frame.getLensId());
            lensTextView.setText(currentLens.getName());
        }
        else lensTextView.setText("");

        // LENS PICK DIALOG
        newLensId = frame.getLensId();
        final LinearLayout lensLayout = inflatedView.findViewById(R.id.lens_layout);
        lensLayout.setOnClickListener(new LensLayoutOnClickListener());

        // LENS ADD DIALOG
        final ImageView addLensImageView = inflatedView.findViewById(R.id.add_lens);
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
        // DATE PICK DIALOG
        final TextView dateTextView = inflatedView.findViewById(R.id.date_text);
        final TextView timeTextView = inflatedView.findViewById(R.id.time_text);
        if (frame.getDate() == null) frame.setDate(Utilities.getCurrentTime());
        final List<String> dateValue = Utilities.splitDate(frame.getDate());
        final int tempYear = Integer.parseInt(dateValue.get(0));
        final int tempMonth = Integer.parseInt(dateValue.get(1));
        final int tempDay = Integer.parseInt(dateValue.get(2));
        final String dateText = tempYear + "-" + tempMonth + "-" + tempDay;
        dateTextView.setText(dateText);

        newDate = frame.getDate();

        final LinearLayout dateLayout = inflatedView.findViewById(R.id.date_layout);
        dateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DATE PICKER DIALOG IMPLEMENTATION HERE
                final List<String> dateValue = Utilities.splitDate(newDate);
                final int year = Integer.parseInt(dateValue.get(0));
                final int month = Integer.parseInt(dateValue.get(1));
                final int day = Integer.parseInt(dateValue.get(2));
                DatePickerDialog dialog = new DatePickerDialog(
                        getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        final String newInnerDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
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
        // TIME PICK DIALOG
        final List<String> timeValue = Utilities.splitTime(frame.getDate());
        final int tempHours = Integer.parseInt(timeValue.get(0));
        final int tempMinutes = Integer.parseInt(timeValue.get(1));
        final String timeText;
        if (tempMinutes < 10) timeText = tempHours + ":0" + tempMinutes;
        else timeText = tempHours + ":" + tempMinutes;
        timeTextView.setText(timeText);

        final LinearLayout timeLayout = inflatedView.findViewById(R.id.time_layout);
        timeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TIME PICKER DIALOG IMPLEMENTATION HERE
                final List<String> timeValue = Utilities.splitTime(newDate);
                final int hours = Integer.parseInt(timeValue.get(0));
                final int minutes = Integer.parseInt(timeValue.get(1));
                TimePickerDialog dialog = new TimePickerDialog(
                        getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        final String newTime;
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
        //NOTES FIELD
        noteEditText = inflatedView.findViewById(R.id.note_editText);
        noteEditText.setSingleLine(false);
        noteEditText.setText(frame.getNote());
        noteEditText.setSelection(noteEditText.getText().length());



        //==========================================================================================
        //COUNT BUTTON
        newFrameCount = frame.getCount();
        frameCountTextView = inflatedView.findViewById(R.id.frame_count_text);
        frameCountTextView.setText(String.valueOf(newFrameCount));
        final LinearLayout frameCountLayout = inflatedView.findViewById(R.id.frame_count_layout);
        frameCountLayout.setOnClickListener(new FrameCountLayoutOnClickListener());



        //==========================================================================================
        //SHUTTER SPEED BUTTON
        newShutter = frame.getShutter();
        shutterTextView = inflatedView.findViewById(R.id.shutter_text);
        updateShutterTextView();
        final LinearLayout shutterLayout = inflatedView.findViewById(R.id.shutter_layout);
        shutterLayout.setOnClickListener(new ShutterLayoutOnClickListener());



        //==========================================================================================
        //APERTURE BUTTON
        newAperture = frame.getAperture();
        apertureTextView = inflatedView.findViewById(R.id.aperture_text);
        updateApertureTextView();
        final LinearLayout apertureLayout = inflatedView.findViewById(R.id.aperture_layout);
        apertureLayout.setOnClickListener(new ApertureLayoutOnClickListener());



        //==========================================================================================
        //FOCAL LENGTH BUTTON
        newFocalLength = frame.getFocalLength();
        focalLengthTextView = inflatedView.findViewById(R.id.focal_length_text);
        updateFocalLengthTextView();
        final LinearLayout focalLengthLayout = inflatedView.findViewById(R.id.focal_length_layout);
        focalLengthLayout.setOnClickListener(new FocalLengthLayoutOnClickListener());



        //==========================================================================================
        //EXPOSURE COMP BUTTON
        newExposureComp = frame.getExposureComp();
        exposureCompTextView = inflatedView.findViewById(R.id.exposure_comp_text);
        exposureCompTextView.setText(
                newExposureComp == null || newExposureComp.equals("0") ? "" : newExposureComp
        );
        final LinearLayout exposureCompLayout = inflatedView.findViewById(R.id.exposure_comp_layout);
        exposureCompLayout.setOnClickListener(new ExposureCompLayoutOnClickListener());



        //==========================================================================================
        //NO OF EXPOSURES BUTTON

        //Check that the number is bigger than zero.
        newNoOfExposures = frame.getNoOfExposures() > 0 ? frame.getNoOfExposures() : 1;
        noOfExposuresTextView = inflatedView.findViewById(R.id.no_of_exposures_text);
        noOfExposuresTextView.setText(String.valueOf(newNoOfExposures));
        final LinearLayout noOfExposuresLayout = inflatedView.findViewById(R.id.no_of_exposures_layout);
        noOfExposuresLayout.setOnClickListener(new NoOfExposuresLayoutOnClickListener());



        //==========================================================================================
        // LOCATION PICK DIALOG
        locationTextView = inflatedView.findViewById(R.id.location_text);
        newLocation = frame.getLocation();
        newFormattedAddress = frame.getFormattedAddress();
        updateLocationTextView();

        final ProgressBar locationProgressBar = inflatedView.findViewById(R.id.location_progress_bar);

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

        final ImageView clearLocation = inflatedView.findViewById(R.id.clear_location);
        clearLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newLocation = null;
                newFormattedAddress = null;
                updateLocationTextView();
            }
        });
        final LinearLayout locationLayout = inflatedView.findViewById(R.id.location_layout);
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
        //FILTER BUTTON
        filterTextView = inflatedView.findViewById(R.id.filter_text);
        if (frame.getFilterId() > 0) {
            Filter currentFilter = database.getFilter(frame.getFilterId());
            filterTextView.setText(currentFilter.getName());
        }
        else {
            filterTextView.setText("");
        }

        // FILTER PICK DIALOG
        newFilterId = frame.getFilterId();
        final LinearLayout filterLayout = inflatedView.findViewById(R.id.filter_layout);
        filterLayout.setOnClickListener(new FilterLayoutOnClickListener());

        // FILTER ADD DIALOG
        final ImageView addFilterImageView = inflatedView.findViewById(R.id.add_filter);
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
        //COMPLEMENTARY PICTURE

        newPictureFilename = frame.getPictureFilename();
        pictureLayout = inflatedView.findViewById(R.id.picture_layout);
        pictureImageView = inflatedView.findViewById(R.id.iv_picture);
        pictureTextView = inflatedView.findViewById(R.id.picture_text);
        pictureLayout.setOnClickListener(new PictureLayoutOnClickListener());
        // Set the scroll change listener AFTER pictureLayout has been assigned with findViewById.
        // Reference is made to pictureLayout in OnScrollChangeListener, and if the OnScrollChangeListener
        // class is instantiated before pictureLayout has been set, the reference will be null (probably).
        nestedScrollView.setOnScrollChangeListener(new OnScrollChangeListener());



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
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new OnPositiveButtonClickListener(dialog) {
            @Override
            public void onClick(View v) {
                super.onClick(v);
                // Return the new entered name to the calling activity
                Intent intent = new Intent();
                intent.putExtra(ExtraKeys.FRAME, frame);
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
     * Method to finalize the member that is passed to the target fragment.
     * Also used to delete possibly unused older complementary pictures.
     */
    private void onDialogDismiss(){
        frame.setShutter(newShutter);
        frame.setAperture(newAperture);
        frame.setCount(newFrameCount);
        frame.setNote(noteEditText.getText().toString());
        frame.setDate(newDate);
        frame.setLensId(newLensId);
        frame.setLocation(newLocation);
        frame.setFormattedAddress(newFormattedAddress);
        frame.setFilterId(newFilterId);
        frame.setExposureComp(newExposureComp);
        frame.setNoOfExposures(newNoOfExposures);
        frame.setFocalLength(newFocalLength);
        frame.setPictureFilename(newPictureFilename);
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
            lensTextView.setText(lens.getName());
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
            filterTextView.setText(filter.getName());
            newFilterId = filter.getId();
        }

        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            // The user has taken a new complementary picture. Update the possible new filename,
            // notify gallery app and set the complementary picture bitmap.
            newPictureFilename = tempPictureFilename;

            // Compress the image
            final File pictureFile = getPictureFile(newPictureFilename);
            if (pictureFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
                bitmap = getResizedBitmap(bitmap);
                //noinspection ResultOfMethodCallIgnored
                pictureFile.delete();
                try {
                    FileOutputStream out = new FileOutputStream(pictureFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Don't add picture to gallery. Besides, the pictures are stored to the app's external
            // storage directory, which isn't visible to other apps.
            setComplementaryPicture();
        }

    }

    /**
     * Used to scale down a bitmap for reduced storage usage.
     *
     * @param image the bitmap to be scaled
     * @return scaled bitmap where the size of the longer side is maxSize
     */
    public Bitmap getResizedBitmap(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final int maxSize = 1024;

        // If the specified max size is bigger than either width or height, return original bitmap.
        if (maxSize > Math.max(width, height)) return image;

        // Otherwise continue scaling while maintaining original aspect ratio.
        final float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    /**
     * Set the complementary picture ImageView with the newly selected/taken picture
     */
    private void setComplementaryPicture() {

        // If the picture filename was not set, set text and return. Otherwise continue
        if (newPictureFilename == null) {
            pictureTextView.setText(R.string.ClickToAdd);
            return;
        }

        final File pictureFile = getPictureFile(newPictureFilename);

        // If the picture file exists, set the picture ImageView.
        if (pictureFile.exists()) {

            // Set the visibilities first, so that the views in general are displayed
            // when the user scrolls down.
            pictureTextView.setVisibility(View.GONE);
            pictureImageView.setVisibility(View.VISIBLE);

            // Load the bitmap on a background thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Get the target ImageView height
                    final int targetH = (int) getResources().getDimension(R.dimen.ComplementaryPictureImageViewHeight);

                    // Get the dimensions of the bitmap
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
                    final int photoH = options.outHeight;

                    // Determine how much to scale down the image
                    final int scale = photoH / targetH;

                    // Decode the image file into a Bitmap sized to fill the view
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = scale;
                    options.inPurgeable = true;
                    final Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);

                    // Do UI changes on the UI thread.
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pictureImageView.setImageBitmap(bitmap);
                            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_fast);
                            pictureImageView.startAnimation(animation);
                        }
                    });
                }
            }).start();

        }
        // The file does not exist. Show error message in the TextView.
        else {
            pictureTextView.setText(R.string.PictureSetButNotFound);
        }
    }

    private File getPictureFile(final String fileName) {
        // Get the absolute path to the picture file.
        return new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
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

            // Add all displayed shutter values to the final list of displayed shutter values
            // since no min and max shutter speeds were defined.
            shutterValuesList.addAll(Arrays.asList(displayedShutterValues));

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
     */
    private void updateShutterTextView(){
        if (shutterTextView != null) {
            if (newShutter.contains("<") || newShutter.contains(">")) {
                shutterTextView.setText("");
            } else {
                shutterTextView.setText(newShutter);
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




    /**
     * Class used by this class AlertDialog class and its subclasses. Implemented for positive button
     * onClick events.
     */
    protected class OnPositiveButtonClickListener implements View.OnClickListener {
        private AlertDialog dialog;
        OnPositiveButtonClickListener(AlertDialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View view) {
            onDialogDismiss();
            dialog.dismiss();
        }
    }

    /**
     * Scroll change listener used to detect when the pictureLayout is visible.
     * Only then will the complementary picture be loaded.
     */
    private class OnScrollChangeListener implements NestedScrollView.OnScrollChangeListener {
        private boolean pictureLoaded = false;
        @Override
        public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            Rect scrollBounds = new Rect();
            nestedScrollView.getHitRect(scrollBounds);
            if (pictureLayout.getLocalVisibleRect(scrollBounds) && !pictureLoaded) {
                setComplementaryPicture();
                pictureLoaded = true;
            }
        }
    }

    //==============================================================================================
    // LISTENER CLASSES USED TO OPEN NEW DIALOGS AFTER ONCLICK EVENTS

    /**
     * Listener class attached to shutter speed layout.
     * Opens a new dialog to display shutter speed options.
     */
    private class ShutterLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
            final NumberPicker shutterPicker = dialogView.findViewById(R.id.number_picker);

            initialiseShutterPicker(shutterPicker);

            shutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseShutterSpeed));
            builder.setPositiveButton(getResources().getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            newShutter = displayedShutterValues[shutterPicker.getValue()];
                            updateShutterTextView();
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
    }

    /**
     * Listener class attached to aperture value layout.
     * Opens a new dialog to display aperture value options.
     */
    private class ApertureLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
            final NumberPicker aperturePicker = dialogView.findViewById(R.id.number_picker);

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
    }

    /**
     * Listener class attached to frame count layout.
     * Opens a new dialog to display frame count options.
     */
    private class FrameCountLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
            final NumberPicker frameCountPicker = dialogView.findViewById(R.id.number_picker);
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
    }

    /**
     * Listener class attached to lens layout.
     * Opens a new dialog to display lens options for current camera.
     * Check the validity of aperture value, focal length and filter after lens has been changed.
     */
    private class LensLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
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
    }

    /**
     * Listener class attached to filter layout.
     * Opens a new dialog to display filter options for current lens.
     */
    private class FilterLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
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
    }

    /**
     * Listener class attached to focal length layout.
     * Opens a new dialog to display focal length options.
     */
    private class FocalLengthLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.dialog_seek_bar, null);
            final SeekBar focalLengthSeekBar = dialogView.findViewById(R.id.seek_bar);
            final TextView focalLengthTextView = dialogView.findViewById(R.id.value_text_view);

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

            final TextView increaseFocalLength = dialogView.findViewById(R.id.increase_focal_length);
            final TextView decreaseFocalLength = dialogView.findViewById(R.id.decrease_focal_length);
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
    }

    /**
     * Listener class attached to exposure compensation layout.
     * Opens a new dialog to display exposure compensation options.
     */
    private class ExposureCompLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
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
    }

    /**
     * Listener class attached to number of exposures layout.
     * Opens a new dialog to display number of exposures options.
     */
    private class NoOfExposuresLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
            final NumberPicker noOfExposuresPicker = dialogView.findViewById(R.id.number_picker);

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
    }

    /**
     * Listener class attached to complementary picture layout.
     * Shows various actions regarding the complementary picture.
     */
    private class PictureLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            AlertDialog.Builder pictureActionDialogBuilder = new AlertDialog.Builder(getActivity());
            final String[] items;

            // If a complementary picture was not set, set only the two first options
            if (newPictureFilename == null) {
                items = new String[]{
                        getString(R.string.TakeNewComplementaryPicture),
                        getString(R.string.SelectPictureFromGallery)
                };

            }
            // If a complementary picture was set, show additional two options
            else {
                items = new String[]{
                        getString(R.string.TakeNewComplementaryPicture),
                        getString(R.string.SelectPictureFromGallery),
                        getString(R.string.AddPictureToGallery),
                        getString(R.string.Clear)
                };
            }

            // Add the items and the listener
            pictureActionDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {

                        // Take a new complementary picture
                        case 0:
                            dialogInterface.dismiss();
                            startPictureActivity();
                            break;

                        // Select picture from gallery
                        case 1:
                            // TODO Implement selecting a complementary picture from gallery
                            Toast.makeText(getActivity(), R.string.UpcomingFeatureStayTuned, Toast.LENGTH_SHORT).show();
                            break;

                        // Add the picture to gallery
                        case 2:
                            final File publicPictureDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
                            final File copyFromFile = getPictureFile(newPictureFilename);
                            final File copyToFile = new File(publicPictureDirectory, newPictureFilename);
                            try {
                                Utilities.copyFile(copyFromFile, copyToFile);
                                galleryAddPicture(copyToFile);
                                Toast.makeText(getActivity(), R.string.PictureAddedToGallery, Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(getActivity(), R.string.ErrorAddingPictureToGallery,Toast.LENGTH_LONG).show();
                            }
                            dialogInterface.dismiss();
                            break;

                        // Clear the complementary picture
                        case 3:
                            newPictureFilename = null;
                            pictureImageView.setVisibility(View.GONE);
                            pictureTextView.setVisibility(View.VISIBLE);
                            pictureTextView.setText(R.string.ClickToAdd);
                            dialogInterface.dismiss();
                            break;

                        default:
                            break;
                    }
                }
            });
            pictureActionDialogBuilder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            pictureActionDialogBuilder.create().show();
        }

        /**
         * Notify the gallery application, that a new picture has been added to external storage
         */
        private void galleryAddPicture(File pictureFile) {
            final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(pictureFile);
            mediaScanIntent.setData(contentUri);
            getActivity().sendBroadcast(mediaScanIntent);
        }

        /**
         * Starts a camera activity to take a new complementary picture
         */
        private void startPictureActivity() {
            // Check if the camera feature is available
            if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                Toast.makeText(getActivity(), R.string.NoCameraFeatureWasFound, Toast.LENGTH_SHORT).show();
                return;
            }
            // Advance with taking the picture
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                // Create the file where the photo should go
                final File pictureFile = createPictureFile();
                Uri photoURI;
                //Android Nougat requires that the file is given via FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    photoURI = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext()
                            .getPackageName() + ".provider", pictureFile);
                } else {
                    photoURI = Uri.fromFile(pictureFile);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
            }
        }

        /**
         *
         * @return reference to the new file where the picture should be saved
         */
        private File createPictureFile() {
            // Create a unique name for the new picture file
            final String pictureFilename = UUID.randomUUID().toString() + ".jpg";
            // Create a reference to the picture file
            final File picture = getPictureFile(pictureFilename);
            // Get reference to the destination folder by the file's parent
            final File pictureStorageDirectory = picture.getParentFile();
            // If the destination folder does not exist, create it
            if (!pictureStorageDirectory.exists()) {
                //noinspection ResultOfMethodCallIgnored
                pictureStorageDirectory.mkdirs(); // also create possible non-existing parent directories -> mkdirs()
            }
            tempPictureFilename = pictureFilename;
            // Return the File
            return picture;
        }
    }

}