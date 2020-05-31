package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.DateTime;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager;
import com.tommihirvonen.exifnotes.utilities.DateTimeLayoutManager;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.activities.LocationPickActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.GeocodingAsyncTask;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.ceil;
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
     * Constant passed to selectPictureIntent for result
     */
    private static final int SELECT_PICTURE_REQUEST = 5;

    /**
     * Reference to the camera used to take this frame
     */
    @Nullable
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

    private DateTimeLayoutManager dateTimeLayoutManager;

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
    private TextView filtersTextView;

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
     * Currently selected lens
     */
    @Nullable
    private Lens newLens;

    /**
     * Currently selected latitude longitude location in format '12,3456... 12,3456...'
     */
    @Nullable
    private String newLocation;

    /**
     * Currently set formatted address for location
     */
    @Nullable
    private String newFormattedAddress;

    /**
     * Currently selected filter(s)
     */
    private List<Filter> newFilters;

    /**
     * Currently selected lens's aperture increment setting
     */
    private int apertureIncrements = 0;

    /**
     * The shutter speed increment setting of the camera used
     */
    private int shutterIncrements = 0;

    /**
     * The exposure compensation increment setting of the camera used
     */
    private int exposureCompIncrements = 0;

    /**
     * Currently selected frame count number
     */
    private int newFrameCount;

    /**
     * Currently selected shutter speed value in format 1/X, Y" or B, where X and Y are numbers
     */
    @Nullable
    private String newShutter;

    /**
     * Currently selected aperture value, number only
     */
    @Nullable
    private String newAperture;

    /**
     * Currently selected focal length
     */
    private int newFocalLength;

    /**
     * Currently selected exposure compensation in format
     * 0, +/-X or +/-Y/Z where X, Y and Z are numbers
     */
    @Nullable
    private String newExposureComp;

    /**
     * Currently selected number of exposures (multiple exposure)
     */
    private int newNoOfExposures;

    /**
     * Currently selected light source
     */
    private int newLightSource;

    /**
     * Currently selected filename of the complementary picture
     */
    @Nullable
    private String newPictureFilename;

    /**
     * Used to temporarily store the possible new picture name. newPictureFilename is only set,
     * if the user presses ok in the camera activity. If the user cancels the camera activity,
     * then this member's value is ignored and newPictureFilename's value isn't changed.
     */
    @Nullable
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

    /**
     * Button used to display the current focal length value
     */
    private TextView focalLengthTextView;

    /**
     * CheckBox for toggling whether flash was used or not
     */
    private CheckBox flashCheckBox;

    /**
     * TextView used to display the current light source
     */
    private TextView lightSourceTextView;

    /**
     * Reference to the EditText used to edit notes
     */
    private EditText noteEditText;

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
     * Stores the currently displayed exposure compensation values.
     * Changes depending on the film's camera.
     */
    private String[] displayedExposureCompValues;

    /**
     * Empty constructor
     */
    public EditFrameDialog() {
        // Empty constructor required for DialogFragment
    }

    @NonNull
    @Override
    public Dialog onCreateDialog (final Bundle SavedInstanceState) {

        // Inflate the fragment. Get the edited frame and used camera.
        // Initialize UI objects and display the frame's information.
        // Add listeners to buttons to open new dialogs to change the frame's information.

        final String title = getArguments().getString(ExtraKeys.TITLE);
        final String positiveButton = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        frame = getArguments().getParcelable(ExtraKeys.FRAME);
        if (frame == null) frame = new Frame();

        database = FilmDbHelper.getInstance(getActivity());
        final Roll roll  = database.getRoll(frame.getRollId());
        if (roll != null) {
            camera = database.getCamera(roll.getCameraId());
            if (camera != null) {
                mountableLenses = database.getLinkedLenses(camera);
                shutterIncrements = camera.getShutterIncrements();
                exposureCompIncrements = camera.getExposureCompIncrements();
            } else {
                mountableLenses = database.getAllLenses();
            }
        }

        final Lens lens = database.getLens(frame.getLensId());
        if (lens != null) apertureIncrements = lens.getApertureIncrements();

        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.dialog_frame, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        final NestedScrollView nestedScrollView = inflatedView.findViewById(R.id.nested_scroll_view);
        final OnScrollChangeListener listener = new OnScrollChangeListener(getActivity(),
                        nestedScrollView,
                        inflatedView.findViewById(R.id.scrollIndicatorUp),
                        inflatedView.findViewById(R.id.scrollIndicatorDown));

        nestedScrollView.setOnScrollChangeListener(listener);

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflatedView);



        //==========================================================================================
        //DIVIDERS

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(getActivity())) {
            final List<View> dividerList = new ArrayList<>();
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
            for (final View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.add_lens)).getDrawable().mutate(),
                    ContextCompat.getColor(getActivity(), R.color.white));
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.add_filter)).getDrawable().mutate(),
                    ContextCompat.getColor(getActivity(), R.color.white));
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.clear_location)).getDrawable().mutate(),
                    ContextCompat.getColor(getActivity(), R.color.white));
        }



        //==========================================================================================
        //LENS TEXT
        lensTextView = inflatedView.findViewById(R.id.lens_text);
        if (lens != null) lensTextView.setText(lens.getName());
        else lensTextView.setText("");

        // LENS PICK DIALOG
        newLens = database.getLens(frame.getLensId());
        final LinearLayout lensLayout = inflatedView.findViewById(R.id.lens_layout);
        lensLayout.setOnClickListener(new LensLayoutOnClickListener());

        // LENS ADD DIALOG
        final ImageView addLensImageView = inflatedView.findViewById(R.id.add_lens);
        addLensImageView.setClickable(true);
        addLensImageView.setOnClickListener(v -> {
            noteEditText.clearFocus();
            final EditLensDialog dialog = new EditLensDialog();
            dialog.setTargetFragment(EditFrameDialog.this, ADD_LENS);
            final Bundle arguments = new Bundle();
            arguments.putString(ExtraKeys.TITLE, getResources().getString(R.string.NewLens));
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
            dialog.setArguments(arguments);
            dialog.show(getParentFragmentManager().beginTransaction(), EditLensDialog.TAG);
        });



        //==========================================================================================
        // DATE & TIME PICK DIALOG

        final LinearLayout dateLayout = inflatedView.findViewById(R.id.date_layout);
        final LinearLayout timeLayout = inflatedView.findViewById(R.id.time_layout);
        final TextView dateTextView = inflatedView.findViewById(R.id.date_text);
        final TextView timeTextView = inflatedView.findViewById(R.id.time_text);

        if (frame.getDate() == null) {
            frame.setDate(DateTime.Companion.fromCurrentTime());
        }

        final DateTime dateTime = frame.getDate();
        dateTextView.setText(dateTime.getDateAsText());
        timeTextView.setText(dateTime.getTimeAsText());

        dateTimeLayoutManager = new DateTimeLayoutManager(
                getActivity(), dateLayout, timeLayout, dateTextView, timeTextView, dateTime, null
        );



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
            new GeocodingAsyncTask((output, formatted_address) -> {
                locationProgressBar.setVisibility(View.INVISIBLE);
                if (formatted_address.length() > 0 ) {
                    newFormattedAddress = formatted_address;
                } else {
                    newFormattedAddress = null;
                }
                updateLocationTextView();
            }).execute(newLocation, getResources().getString(R.string.google_maps_key));
        }

        final ImageView clearLocation = inflatedView.findViewById(R.id.clear_location);
        clearLocation.setOnClickListener(view -> {
            newLocation = null;
            newFormattedAddress = null;
            updateLocationTextView();
        });
        final LinearLayout locationLayout = inflatedView.findViewById(R.id.location_layout);
        locationLayout.setOnClickListener(view -> {
            final Intent intent = new Intent(getActivity(), LocationPickActivity.class);
            intent.putExtra(ExtraKeys.LOCATION, newLocation);
            intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, newFormattedAddress);
            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        });


        //==========================================================================================
        //FILTER BUTTON
        filtersTextView = inflatedView.findViewById(R.id.filter_text);
        newFilters = frame.getFilters();
        updateFiltersTextView();

        // FILTER PICK DIALOG
        final LinearLayout filterLayout = inflatedView.findViewById(R.id.filter_layout);
        filterLayout.setOnClickListener(new FilterLayoutOnClickListener());

        // FILTER ADD DIALOG
        final ImageView addFilterImageView = inflatedView.findViewById(R.id.add_filter);
        addFilterImageView.setClickable(true);
        addFilterImageView.setOnClickListener(v -> {
            if (newLens == null) {
                Toast.makeText(getActivity(), getResources().getString(R.string.SelectLensToAddFilters),
                        Toast.LENGTH_LONG).show();
                return;
            }
            noteEditText.clearFocus();
            final EditFilterDialog dialog = new EditFilterDialog();
            dialog.setTargetFragment(EditFrameDialog.this, ADD_FILTER);
            final Bundle arguments = new Bundle();
            arguments.putString(ExtraKeys.TITLE, getResources().getString(R.string.NewFilter));
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
            dialog.setArguments(arguments);
            dialog.show(getParentFragmentManager().beginTransaction(), EditFilterDialog.TAG);
        });



        //==========================================================================================
        //COMPLEMENTARY PICTURE

        newPictureFilename = frame.getPictureFilename();
        pictureLayout = inflatedView.findViewById(R.id.picture_layout);
        pictureImageView = inflatedView.findViewById(R.id.iv_picture);
        pictureTextView = inflatedView.findViewById(R.id.picture_text);
        pictureLayout.setOnClickListener(new PictureLayoutOnClickListener());


        //==========================================================================================
        //FLASH

        flashCheckBox = inflatedView.findViewById(R.id.flash_checkbox);
        flashCheckBox.setChecked(frame.getFlashUsed());
        final View flashUsedLayout = inflatedView.findViewById(R.id.flash_layout);
        flashUsedLayout.setOnClickListener((view) -> {
            if (flashCheckBox.isChecked()) flashCheckBox.setChecked(false);
            else flashCheckBox.setChecked(true);
        });



        //==========================================================================================
        //LIGHT SOURCE

        newLightSource = frame.getLightSource();
        lightSourceTextView = inflatedView.findViewById(R.id.light_source_text);
        String lightSource;
        try {
            lightSource = getResources().getStringArray(R.array.LightSource)[newLightSource];
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            lightSource = getResources().getString(R.string.ClickToSet);
        }
        lightSourceTextView.setText(
                newLightSource == 0 ?
                        getResources().getString(R.string.ClickToSet) :
                        lightSource
        );
        final LinearLayout lightSourceLayout = inflatedView.findViewById(R.id.light_source_layout);
        lightSourceLayout.setOnClickListener(new LightSourceLayoutOnClickListener());



        //==========================================================================================
        //FINALISE BUILDING THE DIALOG

        alert.setNegativeButton(R.string.Cancel, (dialog, whichButton) -> {
            final Intent intent = new Intent();
            getTargetFragment().onActivityResult(
                    getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
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
            public void onClick(final View v) {
                super.onClick(v);
                // Return the new entered name to the calling activity
                final Intent intent = new Intent();
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
    private int calculateProgress (final int focalLength, final int minValue, final int maxValue) {
        // progress = (newFocalLength - minValue) / (maxValue - minValue) * 100
        final double result1 = focalLength - minValue;
        final double result2 = maxValue - minValue;
        // No variables of type int can be used if parts of the calculation
        // result in fractions.
        final double progressDouble = result1 / result2 * 100;
        return (int) round(progressDouble);
    }

    /**
     * Updates the filters TextView
     */
    private void updateFiltersTextView() {
        final StringBuilder filtersStringBuilder = new StringBuilder();
        for (int i = 0; i < newFilters.size(); ++i) {
            final Filter filter = newFilters.get(i);
            filtersStringBuilder.append("-").append(filter.getName())
                    // Append line change only if we are not at the last element.
                    .append(i == newFilters.size() - 1 ? "" : "\n");
        }
        filtersTextView.setText(filtersStringBuilder.toString());
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
        frame.setDate(dateTimeLayoutManager.getDateTime());
        frame.setLensId(newLens == null ? 0 : newLens.getId());
        frame.setLocation(newLocation);
        frame.setFormattedAddress(newFormattedAddress);
        frame.setExposureComp(newExposureComp);
        frame.setNoOfExposures(newNoOfExposures);
        frame.setFocalLength(newFocalLength);
        frame.setPictureFilename(newPictureFilename);
        frame.setFilters(newFilters);
        frame.setLightSource(newLightSource);
        frame.setFlashUsed(flashCheckBox.isChecked());
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

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
            newLens = data.getParcelableExtra(ExtraKeys.LENS);
            final long rowId = database.addLens(newLens);
            newLens.setId(rowId);
            if (camera != null) {
                database.addCameraLensLink(camera, newLens);
                mountableLenses.add(newLens);
            }
            lensTextView.setText(newLens.getName());
            apertureIncrements = newLens.getApertureIncrements();
            checkApertureValueValidity();
            if (newFocalLength > newLens.getMaxFocalLength()) {
                newFocalLength = newLens.getMaxFocalLength();
            } else if (newFocalLength < newLens.getMinFocalLength()) {
                newFocalLength = newLens.getMinFocalLength();
            }
            updateFocalLengthTextView();
            resetFilters();
        }

        if (requestCode == ADD_FILTER && resultCode == Activity.RESULT_OK) {

            // After Ok code.
            final Filter filter = data.getParcelableExtra(ExtraKeys.FILTER);
            final long rowId = database.addFilter(filter);
            filter.setId(rowId);
            if (newLens != null) database.addLensFilterLink(filter, newLens);
            newFilters.add(filter);
            updateFiltersTextView();
        }

        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
            pictureTextView.setText(R.string.LoadingPicture);
            pictureTextView.setVisibility(View.VISIBLE);
            // Decode and compress the picture on a background thread.
            new Thread(() -> {
                // The user has taken a new complementary picture. Update the possible new filename,
                // notify gallery app and set the complementary picture bitmap.
                newPictureFilename = tempPictureFilename;

                // Compress the picture file
                try {
                    ComplementaryPicturesManager.compressPictureFile(getActivity(), newPictureFilename);
                } catch (IOException e) {
                    Toast.makeText(getActivity(), R.string.ErrorCompressingComplementaryPicture, Toast.LENGTH_SHORT).show();
                }
                // Set the complementary picture ImageView on the UI thread.
                getActivity().runOnUiThread(this::setComplementaryPicture);
            }).start();
            
        }

        if (requestCode == SELECT_PICTURE_REQUEST && resultCode == Activity.RESULT_OK) {

            final Uri selectedPictureUri = data.getData();
            if (selectedPictureUri != null) {
                // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
                pictureTextView.setText(R.string.LoadingPicture);
                pictureTextView.setVisibility(View.VISIBLE);
                // Decode and compress the selected file on a background thread.
                new Thread(() -> {
                    // Create the placeholder file in the complementary pictures directory.
                    final File pictureFile = ComplementaryPicturesManager.createNewPictureFile(getActivity());
                    try {
                        // Get the compressed bitmap from the Uri.
                        final Bitmap pictureBitmap = ComplementaryPicturesManager
                                .getCompressedBitmap(getActivity(), selectedPictureUri);
                        try {
                            // Save the compressed bitmap to the placeholder file.
                            ComplementaryPicturesManager.saveBitmapToFile(pictureBitmap, pictureFile);
                            // Update the member reference and set the complementary picture.
                            newPictureFilename = pictureFile.getName();
                            // Set the complementary picture ImageView on the UI thread.
                            getActivity().runOnUiThread(this::setComplementaryPicture);
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), R.string.ErrorSavingSelectedPicture, Toast.LENGTH_SHORT).show();
                        }
                    } catch (FileNotFoundException e) {
                        Toast.makeText(getActivity(), R.string.ErrorLocatingSelectedPicture, Toast.LENGTH_SHORT).show();
                    }
                }).start();
            }

        }

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

        final File pictureFile = ComplementaryPicturesManager.getPictureFile(getActivity(), newPictureFilename);

        // If the picture file exists, set the picture ImageView.
        if (pictureFile.exists()) {

            // Set the visibilities first, so that the views in general are displayed
            // when the user scrolls down.
            pictureTextView.setVisibility(View.GONE);
            pictureImageView.setVisibility(View.VISIBLE);

            // Load the bitmap on a background thread
            new Thread(() -> {
                // Get the target ImageView height.
                // Because the complementary picture ImageView uses subclass SquareImageView,
                // the ImageView width should also be its height. Because the ImageView's
                // width is match_parent, we get the dialog's width instead.
                // If there is a problem getting the dialog window, use the resource dimension instead.
                final int targetH = getDialog().getWindow() != null ?
                        getDialog().getWindow().getDecorView().getWidth() :
                        (int) getResources().getDimension(R.dimen.ComplementaryPictureImageViewHeight);

                // Rotate the complementary picture ImageView if necessary
                int rotationTemp = 0;
                try {
                    final ExifInterface exifInterface = new ExifInterface(pictureFile.getAbsolutePath());
                    int orientation = exifInterface
                            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationTemp = 90;
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationTemp = 180;
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationTemp = 270;
                } catch (IOException ignore) {}
                final int rotation = rotationTemp;

                // Get the dimensions of the bitmap
                final BitmapFactory.Options options = new BitmapFactory.Options();
                // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
                // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
                final int photoH = options.outHeight;

                // Determine how much to scale down the image
                final int scale = photoH / targetH;

                // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
                options.inJustDecodeBounds = false;
                options.inSampleSize = scale;

                // Decode the image file into a Bitmap sized to fill the view
                final Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);

                // Do UI changes on the UI thread.
                getActivity().runOnUiThread(() -> {
                    pictureImageView.setRotation(rotation);
                    pictureImageView.setImageBitmap(bitmap);
                    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_fast);
                    pictureImageView.startAnimation(animation);
                });
            }).start();

        }
        // The file does not exist. Show error message in the TextView.
        else {
            pictureTextView.setText(R.string.PictureSetButNotFound);
        }
    }



    /**
     * Set the displayed aperture values depending on the lens's aperture increments
     * and its max and min aperture values. If no lens is selected, default to third stop
     * increments and don't limit the aperture values from either end.
     */
    @SuppressWarnings("ManualArrayToCollectionCopy")
    private void setDisplayedApertureValues() {
        //Get the array of displayed aperture values according to the set increments.
        switch (apertureIncrements) {
            default: case 0:
                displayedApertureValues = getActivity().getResources()
                        .getStringArray(R.array.ApertureValuesThird);
                break;
            case 1:
                displayedApertureValues = getActivity().getResources()
                        .getStringArray(R.array.ApertureValuesHalf);
                break;
            case 2:
                displayedApertureValues = getActivity().getResources()
                        .getStringArray(R.array.ApertureValuesFull);
                break;
        }
        //Reverse the order if necessary.
        if (displayedApertureValues[0].equals(getResources().getString(R.string.NoValue))) {
            // By reversing the order we can reverse the order in the NumberPicker too
            Collections.reverse(Arrays.asList(displayedApertureValues));
        }

        final List<String> apertureValuesList = new ArrayList<>(); //to store the temporary values
        int minIndex = 0;
        int maxIndex = displayedApertureValues.length-1; //we begin the maxIndex from the last element

        //Set the min and max values only if a lens is selected and they are set for the lens.
        //Otherwise the displayedApertureValues array will be left alone
        //(all aperture values available, since min and max were not defined).
        if (newLens != null && newLens.getMinAperture() != null && newLens.getMaxAperture() != null) {
            for (int i = 0; i < displayedApertureValues.length; ++i) {
                if (newLens.getMinAperture().equals(displayedApertureValues[i])) {
                    minIndex = i;
                }
                if (newLens.getMaxAperture().equals(displayedApertureValues[i])) {
                    maxIndex = i;
                }
            }

            //Add the values between and min and max to the temp list from the initial array.
            for (int i = minIndex; i <= maxIndex; ++i) {
                apertureValuesList.add(displayedApertureValues[i]);
            }

            //Add the <empty> option to the end of the temp list.
            apertureValuesList.add(getResources().getString(R.string.NoValue));

            //Copy the temp list over the initial array.
            displayedApertureValues = apertureValuesList.toArray(new String[0]);
        }
    }

    /**
     * Called when the shutter speed value dialog is opened.
     * Set the values for the NumberPicker
     *
     * @param shutterPicker NumberPicker associated with the shutter speed value
     */
    @SuppressWarnings("ManualArrayToCollectionCopy")
    private void initialiseShutterPicker(final NumberPicker shutterPicker){
        // Set the increments according to settings
        switch (shutterIncrements) {
            default: case 0:
                displayedShutterValues = getActivity().getResources()
                        .getStringArray(R.array.ShutterValuesThird);
                break;
            case 1:
                displayedShutterValues = getActivity().getResources()
                        .getStringArray(R.array.ShutterValuesHalf);
                break;
            case 2:
                displayedShutterValues = getActivity().getResources()
                        .getStringArray(R.array.ShutterValuesFull);
                break;
        }
        //Reverse the order if necessary
        if (displayedShutterValues[0].equals(getResources().getString(R.string.NoValue))) {
            // By reversing the order we can reverse the order in the NumberPicker too
            Collections.reverse(Arrays.asList(displayedShutterValues));
        }
        final List<String> shutterValuesList = new ArrayList<>();
        int minIndex = 0;
        int maxIndex = displayedShutterValues.length-1;

        //Set the min and max values only if they are set for the camera.
        //Otherwise the displayedShutterValues array will be left alone (apart from bulb)
        //(all shutter values available, since min and max were not defined).
        if (camera != null && camera.getMinShutter() != null && camera.getMaxShutter() != null) {
            for (int i = 0; i < displayedShutterValues.length; ++i) {
                if (camera.getMinShutter().equals(displayedShutterValues[i])) {
                    minIndex = i;
                }
                if (camera.getMaxShutter().equals(displayedShutterValues[i])) {
                    maxIndex = i;
                }
            }

            //Add the values between and min and max to the temp list from the initial array.
            for (int i = minIndex; i <= maxIndex; ++i) {
                shutterValuesList.add(displayedShutterValues[i]);
            }

            //Also add the bulb mode option.
            shutterValuesList.add("B");

            //Add the no value option to the end.
            shutterValuesList.add(getResources().getString(R.string.NoValue));

            displayedShutterValues = shutterValuesList.toArray(new String[0]);

        } else {

            // Add all displayed shutter values to the final list of displayed shutter values
            // since no min and max shutter speeds were defined.
            shutterValuesList.addAll(Arrays.asList(displayedShutterValues));

            //If no min and max were set for the shutter speed, then only add bulb.
            //add B between 30s and NoValue
            shutterValuesList.add(shutterValuesList.size()-1, "B");

            displayedShutterValues = shutterValuesList.toArray(new String[0]);
        }

        shutterPicker.setMinValue(0);
        shutterPicker.setMaxValue(displayedShutterValues.length-1);
        shutterPicker.setDisplayedValues(displayedShutterValues);
        shutterPicker.setValue(displayedShutterValues.length-1);
        for (int i = 0; i < displayedShutterValues.length; ++i) {
            if (newShutter != null && newShutter.equals(displayedShutterValues[i])) {
                shutterPicker.setValue(i);
            }
        }
    }

    /**
     * Reset filters and update the filter button's text.
     */
    private void resetFilters(){
        newFilters.clear();
        filtersTextView.setText("");
    }

    /**
     * Updates the shutter speed value TextView's text.
     */
    private void updateShutterTextView(){
        if (shutterTextView != null) {
            if (newShutter == null) {
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
            if (newAperture == null) {
                apertureTextView.setText("");
            } else {
                final String newText = "f/" + newAperture;
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
        setDisplayedApertureValues();
        boolean apertureFound = false;
        for (final String string : displayedApertureValues) {
            if (string.equals(newAperture)) {
                apertureFound = true;
                break;
            }
        }
        if (!apertureFound) {
            newAperture = null;
            updateApertureTextView();
        }
    }

    /**
     * Class used by this class AlertDialog class and its subclasses. Implemented for positive button
     * onClick events.
     */
    class OnPositiveButtonClickListener implements View.OnClickListener {
        private final AlertDialog dialog;
        OnPositiveButtonClickListener(final AlertDialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(final View view) {
            onDialogDismiss();
            dialog.dismiss();
        }
    }

    /**
     * Scroll change listener used to detect when the pictureLayout is visible.
     * Only then will the complementary picture be loaded.
     */
    private class OnScrollChangeListener extends Utilities.ScrollIndicatorNestedScrollViewListener {
        private boolean pictureLoaded = false;

        OnScrollChangeListener(@NonNull Context context, @NonNull NestedScrollView nestedScrollView,
                               @NonNull View indicatorUp, @NonNull View indicatorDown) {
            super(context, nestedScrollView, indicatorUp, indicatorDown);
        }

        @Override
        public void onScrollChange(final NestedScrollView v, final int scrollX, final int scrollY,
                                   final int oldScrollX, final int oldScrollY) {
            super.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY);

            final Rect scrollBounds = new Rect();
            v.getHitRect(scrollBounds);
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
        public void onClick(final View view) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
            final NumberPicker shutterPicker = Utilities.fixNumberPicker(
                    dialogView.findViewById(R.id.number_picker)
            );

            initialiseShutterPicker(shutterPicker);

            shutterPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseShutterSpeed));
            builder.setPositiveButton(getResources().getString(R.string.OK),
                    (dialogInterface, i) -> {
                        newShutter = shutterPicker.getValue() != shutterPicker.getMaxValue() ?
                                displayedShutterValues[shutterPicker.getValue()] :
                                null;
                        updateShutterTextView();
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Listener class attached to aperture value layout.
     * Opens a new dialog to display aperture value options.
     */
    private class ApertureLayoutOnClickListener implements View.OnClickListener {

        /**
         * Variable to denote whether a custom aperture is being used or a predefined one.
         */
        private boolean manualOverride = false;

        @Override
        public void onClick(final View view) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker_custom, null);
            final NumberPicker aperturePicker = dialogView.findViewById(R.id.number_picker);
            final EditText editText = dialogView.findViewById(R.id.edit_text);
            final SwitchCompat customApertureSwitch = dialogView.findViewById(R.id.custom_aperture_switch);

            // Initialise the aperture value NumberPicker. The return value is true, if the
            // aperture value corresponds to a predefined value. The return value is false,
            // if the aperture value is a custom value.
            final boolean apertureValueMatch = initialiseAperturePicker(aperturePicker);

            aperturePicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseApertureValue));

            // If the aperture value did not match to any predefined aperture values, enable custom input.
            if (!apertureValueMatch) {
                customApertureSwitch.setChecked(true);
                editText.setText(newAperture);
                aperturePicker.setVisibility(View.INVISIBLE);
                editText.setVisibility(View.VISIBLE);
                manualOverride = true;
            } else {
                // Otherwise set manualOverride to false. This has to be done in case manual
                // override was enabled in a previous onClick event and the value of manualOverride
                // was left to true.
                manualOverride = false;
            }

            // The user can switch between predefined aperture values and custom values using a switch.
            customApertureSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    aperturePicker.setVisibility(View.INVISIBLE);
                    editText.setVisibility(View.VISIBLE);
                    manualOverride = true;
                    final Animation animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_right_alt);
                    final Animation animation2 = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_left_alt);
                    aperturePicker.startAnimation(animation1);
                    editText.startAnimation(animation2);
                } else {
                    editText.setVisibility(View.INVISIBLE);
                    aperturePicker.setVisibility(View.VISIBLE);
                    manualOverride = false;
                    final Animation animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_right_alt);
                    final Animation animation2 = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_left_alt);
                    aperturePicker.startAnimation(animation1);
                    editText.startAnimation(animation2);
                }
            });

            builder.setPositiveButton(getResources().getString(R.string.OK),
                    (dialogInterface, i) -> {
                        // Use the aperture value from the NumberPicker or EditText
                        // depending on whether a custom value was used.
                        if (!manualOverride) {
                            newAperture = aperturePicker.getValue() != aperturePicker.getMaxValue() ?
                                    displayedApertureValues[aperturePicker.getValue()] :
                                    null;
                        }
                        else {
                            final String customAperture = editText.getText().toString();
                            newAperture = customAperture.length() > 0 ? customAperture : null;
                        }
                        updateApertureTextView();
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();

        }

        /**
         * Called when the aperture value dialog is opened.
         * Sets the values for the NumberPicker.
         *
         * @param aperturePicker NumberPicker associated with the aperture value
         * @return true if the current aperture value corresponds to some predefined aperture value,
         *          false if not.
         */
        private boolean initialiseAperturePicker(final NumberPicker aperturePicker){
            setDisplayedApertureValues();
            //If no lens is selected, end here
            if (newLens == null) {
                aperturePicker.setDisplayedValues(null);
                aperturePicker.setMinValue(0);
                aperturePicker.setMaxValue(displayedApertureValues.length-1);
                aperturePicker.setDisplayedValues(displayedApertureValues);
                aperturePicker.setValue(displayedApertureValues.length-1);
                // Null aperture value is empty, which is a known value. Return true.
                if (newAperture == null) return true;
                for (int i = 0; i < displayedApertureValues.length; ++i) {
                    if (newAperture.equals(displayedApertureValues[i])) {
                        aperturePicker.setValue(i);
                        return true;
                    }
                }
                return false;
            }

            //Set the NumberPicker displayed values to null. If we set the displayed values
            //and the maxValue is smaller than the length of the new displayed values array,
            //ArrayIndexOutOfBounds is thrown.
            //Also if we set maxValue and the currently displayed values array length is smaller,
            //ArrayIndexOutOfBounds is thrown.
            //Setting displayed values to null solves this problem.
            aperturePicker.setDisplayedValues(null);
            aperturePicker.setMinValue(0);
            aperturePicker.setMaxValue(displayedApertureValues.length-1);
            aperturePicker.setDisplayedValues(displayedApertureValues);
            aperturePicker.setValue(displayedApertureValues.length-1);
            // Null aperture value is empty, which is a known value. Return true.
            if (newAperture == null) return true;
            for (int i = 0; i < displayedApertureValues.length; ++i) {
                if (newAperture.equals(displayedApertureValues[i])) {
                    aperturePicker.setValue(i);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Listener class attached to frame count layout.
     * Opens a new dialog to display frame count options.
     */
    private class FrameCountLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
            final NumberPicker frameCountPicker = dialogView.findViewById(R.id.number_picker);
            frameCountPicker.setMinValue(0);
            frameCountPicker.setMaxValue(100);
            frameCountPicker.setValue(newFrameCount);
            frameCountPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseFrameCount));
            builder.setPositiveButton(getResources().getString(R.string.OK),
                    (dialogInterface, i) -> {
                        newFrameCount = frameCountPicker.getValue();
                        frameCountTextView.setText(String.valueOf(newFrameCount));
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
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
        public void onClick(final View view) {
            int checkedItem = 0; // default option is 'no lens' (first one the list)
            final List<String> listItems = new ArrayList<>();
            listItems.add(getResources().getString(R.string.NoLens));
            for (int i = 0; i < mountableLenses.size(); ++i) {
                listItems.add(mountableLenses.get(i).getMake() + " " +
                        mountableLenses.get(i).getModel());

                //If the id's match, set the initial checkedItem.
                // Account for the 'no lens' option with the + 1
                if (mountableLenses.get(i).equals(newLens)) {
                    checkedItem = i + 1;
                }
            }
            final CharSequence[] items = listItems.toArray(new CharSequence[0]);
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.UsedLens);
            builder.setSingleChoiceItems(items, checkedItem, (dialog, which) -> {

                // listItems also contains the No lens option

                // Check if the lens was changed
                if (which > 0) {
                    lensTextView.setText(listItems.get(which));
                    newLens = mountableLenses.get(which - 1);
                    if (newFocalLength > newLens.getMaxFocalLength()) {
                        newFocalLength = newLens.getMaxFocalLength();
                    } else if (newFocalLength < newLens.getMinFocalLength()) {
                        newFocalLength = newLens.getMinFocalLength();
                    }
                    focalLengthTextView.setText(
                            newFocalLength == 0 ? "" : String.valueOf(newFocalLength)
                    );
                    apertureIncrements = newLens.getApertureIncrements();

                    //Check the aperture value's validity against the new lens' properties.
                    checkApertureValueValidity();

                    // The lens was changed, reset filters
                    resetFilters();
                }
                // No lens option was selected
                else {
                    lensTextView.setText("");
                    newLens = null;
                    newFocalLength = 0;
                    updateFocalLengthTextView();
                    apertureIncrements = 0;
                    resetFilters();
                }

                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
                // Do nothing
            });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    /**
     * Listener class attached to filter layout.
     * Opens a new dialog to display filter options for current lens.
     */
    private class FilterLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {
            final List<Filter> possibleFilters = newLens != null ?
                    database.getLinkedFilters(newLens) :
                    database.getAllFilters();

            final List<String> listItems = new ArrayList<>();
            for (final Filter filter : possibleFilters) {
                listItems.add(filter.getName());
            }
            final CharSequence[] items = listItems.toArray(new CharSequence[0]);
            final boolean[] booleans = new boolean[possibleFilters.size()];
            for (int i = 0; i < possibleFilters.size(); ++i) {
                booleans[i] = newFilters.contains(possibleFilters.get(i));
            }
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final List<Integer> selectedItemsIndexList = new ArrayList<>();
            for (int i = 0; i < booleans.length; ++i) {
                if (booleans[i]) selectedItemsIndexList.add(i);
            }
            builder.setTitle(R.string.UsedFilter);
            builder.setMultiChoiceItems(items, booleans, (dialogInterface, which, isChecked) -> {
                if (isChecked) {
                    // If the user checked the item, add it to the selected items.
                    selectedItemsIndexList.add(which);
                } else if (selectedItemsIndexList.contains(which)) {
                    // Else, if the item is already in the array, remove it.
                    selectedItemsIndexList.remove(Integer.valueOf(which));
                }
            });
            builder.setPositiveButton(R.string.OK, (dialogInterface, id) -> {
                Collections.sort(selectedItemsIndexList);
                final List<Filter> filters = new ArrayList<>();
                // Iterate through the selected items.
                for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                    final int which = selectedItemsIndexList.get(i);
                    final Filter filter = possibleFilters.get(which);
                    filters.add(filter);
                }
                newFilters = filters;
                updateFiltersTextView();
            });
            builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
                // Do nothing
            });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    /**
     * Listener class attached to focal length layout.
     * Opens a new dialog to display focal length options.
     */
    private class FocalLengthLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_seek_bar, null);
            final SeekBar focalLengthSeekBar = dialogView.findViewById(R.id.seek_bar);
            final TextView focalLengthTextView = dialogView.findViewById(R.id.value_text_view);

            // Get the min and max focal lengths
            final int minValue;
            final int maxValue;
            if (newLens != null) {
                minValue = newLens.getMinFocalLength();
                maxValue = newLens.getMaxFocalLength();
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
                public void onProgressChanged(final SeekBar seekBar, final int i, final boolean b) {
                    final int focalLength = minValue + (maxValue - minValue) * i / 100;
                    focalLengthTextView.setText(String.valueOf(focalLength));
                }

                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {
                    // Do nothing
                }

                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {
                    // Do nothing
                }
            });

            final TextView increaseFocalLength = dialogView.findViewById(R.id.increase_focal_length);
            final TextView decreaseFocalLength = dialogView.findViewById(R.id.decrease_focal_length);
            increaseFocalLength.setOnClickListener(view12 -> {
                int focalLength = Integer.parseInt(focalLengthTextView.getText().toString());
                if (focalLength < maxValue) {
                    ++focalLength;
                    focalLengthSeekBar.setProgress(calculateProgress(focalLength, minValue, maxValue));
                    focalLengthTextView.setText(String.valueOf(focalLength));
                }
            });
            decreaseFocalLength.setOnClickListener(view1 -> {
                int focalLength = Integer.parseInt(focalLengthTextView.getText().toString());
                if (focalLength > minValue) {
                    --focalLength;
                    focalLengthSeekBar.setProgress(calculateProgress(focalLength, minValue, maxValue));
                    focalLengthTextView.setText(String.valueOf(focalLength));
                }
            });


            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseFocalLength));
            builder.setPositiveButton(getResources().getString(R.string.OK),
                    (dialogInterface, i) -> {
                        newFocalLength = Integer.parseInt(focalLengthTextView.getText().toString());
                        updateFocalLengthTextView();
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Listener class attached to exposure compensation layout.
     * Opens a new dialog to display exposure compensation options.
     */
    private class ExposureCompLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
            final NumberPicker exposureCompPicker = Utilities.fixNumberPicker(
                    dialogView.findViewById(R.id.number_picker)
            );

            // Set the displayed exposure compensation increments depending on the camera's settings.
            switch (exposureCompIncrements) {
                case 0: default:
                    displayedExposureCompValues = getActivity().getResources()
                            .getStringArray(R.array.CompValues);
                    break;
                case 1:
                    displayedExposureCompValues = getActivity().getResources()
                            .getStringArray(R.array.CompValuesHalf);
                    break;
            }

            exposureCompPicker.setMinValue(0);
            exposureCompPicker.setMaxValue(displayedExposureCompValues.length-1);
            exposureCompPicker.setDisplayedValues(displayedExposureCompValues);
            exposureCompPicker.setValue((int) ceil(displayedExposureCompValues.length / 2));
            if (newExposureComp != null) {
                for (int i = 0; i < displayedExposureCompValues.length; ++i) {
                    if (newExposureComp.equals(displayedExposureCompValues[i])) {
                        exposureCompPicker.setValue(i);
                        break;
                    }
                }
            }

            exposureCompPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

            builder.setView(dialogView);
            builder.setTitle(getResources().getString(R.string.ChooseExposureComp));
            builder.setPositiveButton(getResources().getString(R.string.OK),
                    (dialogInterface, i) -> {
                        newExposureComp = displayedExposureCompValues[exposureCompPicker.getValue()];
                        exposureCompTextView.setText(
                                newExposureComp == null || newExposureComp.equals("0") ? "" : newExposureComp
                        );
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Listener class attached to number of exposures layout.
     * Opens a new dialog to display number of exposures options.
     */
    private class NoOfExposuresLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
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
                    (dialogInterface, i) -> {
                        newNoOfExposures = noOfExposuresPicker.getValue();
                        noOfExposuresTextView.setText(String.valueOf(newNoOfExposures));
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Listener class attached to complementary picture layout.
     * Shows various actions regarding the complementary picture.
     */
    private class PictureLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {

            final AlertDialog.Builder pictureActionDialogBuilder = new AlertDialog.Builder(getActivity());
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
                        getString(R.string.RotateRight90Degrees),
                        getString(R.string.RotateLeft90Degrees),
                        getString(R.string.Clear)
                };
            }

            // Add the items and the listener
            pictureActionDialogBuilder.setItems(items, (dialogInterface, i) -> {
                switch (i) {

                    // Take a new complementary picture
                    case 0:
                        dialogInterface.dismiss();
                        startPictureActivity();
                        break;

                    // Select picture from gallery
                    case 1:
                        final Intent selectPictureIntent = new Intent(Intent.ACTION_PICK);
                        selectPictureIntent.setType("image/*");
                        startActivityForResult(selectPictureIntent, SELECT_PICTURE_REQUEST);
                        break;

                    // Add the picture to gallery
                    case 2:
                        try {
                            ComplementaryPicturesManager.addPictureToGallery(getActivity(), newPictureFilename);
                            Toast.makeText(getActivity(), R.string.PictureAddedToGallery, Toast.LENGTH_SHORT).show();
                        } catch (final IOException e) {
                            Toast.makeText(getActivity(), R.string.ErrorAddingPictureToGallery,Toast.LENGTH_LONG).show();
                        }
                        dialogInterface.dismiss();
                        break;

                    // Rotate the picture 90 degrees clockwise
                    case 3:
                        rotateComplementaryPictureRight();
                        break;

                    // Rotate the picture 90 degrees counterclockwise
                    case 4:
                        rotateComplementaryPictureLeft();
                        break;

                    // Clear the complementary picture
                    case 5:
                        newPictureFilename = null;
                        pictureImageView.setVisibility(View.GONE);
                        pictureTextView.setVisibility(View.VISIBLE);
                        pictureTextView.setText(R.string.ClickToAdd);
                        dialogInterface.dismiss();
                        break;

                    default:
                        break;
                }
            });
            pictureActionDialogBuilder.setNegativeButton(R.string.Cancel, (dialogInterface, i) -> dialogInterface.dismiss());
            pictureActionDialogBuilder.create().show();
        }

        /**
         * Starts a camera activity to take a new complementary picture
         */
        private void startPictureActivity() {
            // Check if the camera feature is available
            if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(getActivity(), R.string.NoCameraFeatureWasFound, Toast.LENGTH_SHORT).show();
                return;
            }
            // Advance with taking the picture
            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                // Create the file where the photo should go
                final File pictureFile = ComplementaryPicturesManager.createNewPictureFile(getActivity());
                tempPictureFilename = pictureFile.getName();
                final Uri photoURI;
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
         * Rotate the complementary picture ImageView 90 degrees clockwise and
         * set the complementary picture orientation with ComplementaryPicturesManager.
         */
        private void rotateComplementaryPictureRight() {
            try {
                ComplementaryPicturesManager.rotatePictureRight(getActivity(), newPictureFilename);
                pictureImageView.setRotation(pictureImageView.getRotation() + 90);
                final Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_right);
                pictureImageView.startAnimation(animation);
            } catch (final IOException e) {
                Toast.makeText(getActivity(), R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees counterclockwise and set
         * the complementary picture orientation with ComplementaryPicturesManager.
         */
        private void rotateComplementaryPictureLeft() {
            try {
                ComplementaryPicturesManager.rotatePictureLeft(getActivity(), newPictureFilename);
                pictureImageView.setRotation(pictureImageView.getRotation() - 90);
                final Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_left);
                pictureImageView.startAnimation(animation);
            } catch (final IOException e) {
                Toast.makeText(getActivity(), R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * Listener class attached to light source layout.
     * Shows different light source options as a simple list.
     */
    private class LightSourceLayoutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final int checkedItem = newLightSource;
            builder.setSingleChoiceItems(R.array.LightSource, checkedItem, (dialog, which) -> {
                newLightSource = which;
                lightSourceTextView.setText(
                        newLightSource == 0 ?
                                getResources().getString(R.string.ClickToSet) :
                                getResources().getStringArray(R.array.LightSource)[which]
                );
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
                // Do nothing
            });
            builder.create().show();
        }
    }

}