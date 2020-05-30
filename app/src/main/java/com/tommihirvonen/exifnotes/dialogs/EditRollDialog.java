package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.DateTime;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.utilities.DateTimeLayoutManager;
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

    private static final int REQUEST_CODE_ADD_CAMERA = 1;
    private static final int REQUEST_CODE_SELECT_FILM_STOCK = 2;

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

    private TextView isoTextView;

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
    public Dialog onCreateDialog (final Bundle SavedInstanceState) {

        database = FilmDbHelper.getInstance(getActivity());

        final String title = getArguments().getString(ExtraKeys.TITLE);
        final String positiveButton = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        roll = getArguments().getParcelable(ExtraKeys.ROLL);
        if (roll == null) roll = new Roll();

        newCamera = database.getCamera(roll.getCameraId());
        newIso = roll.getIso();
        newPushPull = roll.getPushPull();
        cameraList = database.getAllCameras();
        newFilmStock = database.getFilmStock(roll.getFilmStockId());

        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.dialog_roll, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        final NestedScrollView nestedScrollView = inflatedView.findViewById(
                R.id.nested_scroll_view);
        nestedScrollView.setOnScrollChangeListener(
                new Utilities.ScrollIndicatorNestedScrollViewListener(getActivity(),
                        nestedScrollView,
                        inflatedView.findViewById(R.id.scrollIndicatorUp),
                        inflatedView.findViewById(R.id.scrollIndicatorDown)));

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
            for (final View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.add_camera)).getDrawable().mutate(),
                    ContextCompat.getColor(getActivity(), R.color.white));
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.clear_film_stock)).getDrawable().mutate(),
                    ContextCompat.getColor(getActivity(), R.color.white));
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.clear_date_unloaded)).getDrawable().mutate(),
                    ContextCompat.getColor(getActivity(), R.color.white));
            Utilities.setColorFilter(((ImageView) inflatedView.findViewById(R.id.clear_date_developed)).getDrawable().mutate(),
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
        filmStockClearImageView.setOnClickListener(v -> {
            newFilmStock = null;
            nameEditText.setHint("");
            filmStockTextView.setText("");
            filmStockClearImageView.setVisibility(View.GONE);
        });

        final LinearLayout filmStockLayout = inflatedView.findViewById(R.id.film_stock_layout);
        filmStockLayout.setOnClickListener(v -> {
            final SelectFilmStockDialog dialog = new SelectFilmStockDialog();
            dialog.setTargetFragment(EditRollDialog.this, REQUEST_CODE_SELECT_FILM_STOCK);
            dialog.show(getParentFragmentManager().beginTransaction(), null);
        });
        //==========================================================================================


        //==========================================================================================
        // CAMERA PICK DIALOG
        cameraTextView = inflatedView.findViewById(R.id.camera_text);
        if (newCamera != null) cameraTextView.setText(newCamera.getName());
        else cameraTextView.setText("");

        final LinearLayout cameraLayout = inflatedView.findViewById(R.id.camera_layout);
        cameraLayout.setOnClickListener(v -> {
            final List<String> listItems = new ArrayList<>();
            int checkedItem = 0; // Default checked item is 'No camera'
            listItems.add(getResources().getString(R.string.NoCamera));
            for (int i = 0; i < cameraList.size(); ++i) {
                listItems.add(cameraList.get(i).getMake() + " " + cameraList.get(i).getModel());
                if (cameraList.get(i).equals(newCamera)) checkedItem = i + 1;
            }
            final CharSequence[] items = listItems.toArray(new CharSequence[0]);
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.UsedCamera);
            builder.setSingleChoiceItems(items, checkedItem, (dialogInterface, which) -> {
                // listItems also contains the No camera option
                if (which > 0) {
                    cameraTextView.setText(listItems.get(which));
                    newCamera = cameraList.get(which - 1);
                } else {
                    cameraTextView.setText("");
                    newCamera = null;
                }
                dialogInterface.dismiss();
            });
            builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
                // Do nothing
            });
            final AlertDialog alert1 = builder.create();
            alert1.show();
        });
        //==========================================================================================



        //==========================================================================================
        // CAMERA ADD DIALOG
        final ImageView addCameraImageView = inflatedView.findViewById(R.id.add_camera);
        addCameraImageView.setClickable(true);
        addCameraImageView.setOnClickListener(v -> {
            noteEditText.clearFocus();
            nameEditText.clearFocus();
            final EditCameraDialog dialog = new EditCameraDialog();
            dialog.setTargetFragment(EditRollDialog.this, REQUEST_CODE_ADD_CAMERA);
            final Bundle arguments = new Bundle();
            arguments.putString(ExtraKeys.TITLE, getResources().getString( R.string.NewCamera));
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
            dialog.setArguments(arguments);
            dialog.show(getParentFragmentManager().beginTransaction(), EditCameraDialog.TAG);
        });
        //==========================================================================================


        //==========================================================================================
        // DATE & TIME LOADED PICK DIALOG

        // DATE

        if (roll.getDate() == null) {
            roll.setDate(DateTime.Companion.fromCurrentTime());
        }
        final DateTime dateTimeLoaded = roll.getDate();
        final TextView dateTextView = inflatedView.findViewById(R.id.date_text);
        final TextView timeTextView = inflatedView.findViewById(R.id.time_text);
        dateTextView.setText(dateTimeLoaded.getDateAsText());
        timeTextView.setText(dateTimeLoaded.getTimeAsText());
        final LinearLayout dateLayout = inflatedView.findViewById(R.id.date_layout);
        final LinearLayout timeLayout = inflatedView.findViewById(R.id.time_layout);
        final DateTimeLayoutManager dateLoadedManager = new DateTimeLayoutManager(getActivity(), dateLayout,
                timeLayout, dateTextView, timeTextView, dateTimeLoaded, null);

        //==========================================================================================


        //==========================================================================================
        // DATE & TIME UNLOADED PICK DIALOG

        // DATE
        final TextView dateUnloadedTextView = inflatedView.findViewById(R.id.date_unloaded_text);
        final TextView timeUnloadedTextView = inflatedView.findViewById(R.id.time_unloaded_text);
        DateTime dateTimeUnloaded = null;
        if (roll.getUnloaded() != null) {
            dateTimeUnloaded = roll.getUnloaded();
            dateUnloadedTextView.setText(dateTimeUnloaded.getDateAsText());
            timeUnloadedTextView.setText(dateTimeUnloaded.getTimeAsText());
        }
        final LinearLayout dateUnloadedLayout = inflatedView.findViewById(R.id.date_unloaded_layout);
        final LinearLayout timeUnloadedLayout = inflatedView.findViewById(R.id.time_unloaded_layout);
        final ImageView clearDateUnloaded = inflatedView.findViewById(R.id.clear_date_unloaded);
        final DateTimeLayoutManager dateUnloadedManager = new DateTimeLayoutManager(getActivity(), dateUnloadedLayout,
                timeUnloadedLayout, dateUnloadedTextView, timeUnloadedTextView, dateTimeUnloaded,
                clearDateUnloaded);

        //==========================================================================================


        //==========================================================================================
        // DATE & TIME DEVELOPED PICK DIALOG

        final TextView dateDevelopedTextView = inflatedView.findViewById(R.id.date_developed_text);
        final TextView timeDevelopedTextView = inflatedView.findViewById(R.id.time_developed_text);
        DateTime dateTimeDeveloped = null;
        if (roll.getDeveloped() != null) {
            dateTimeDeveloped = roll.getDeveloped();
            dateDevelopedTextView.setText(dateTimeDeveloped.getDateAsText());
            timeDevelopedTextView.setText(dateTimeDeveloped.getTimeAsText());
        }
        final LinearLayout dateDevelopedLayout = inflatedView.findViewById(R.id.date_developed_layout);
        final LinearLayout timeDevelopedLayout = inflatedView.findViewById(R.id.time_developed_layout);
        final ImageView clearDateDeveloped = inflatedView.findViewById(R.id.clear_date_developed);
        final DateTimeLayoutManager dateDevelopedManager = new DateTimeLayoutManager(getActivity(), dateDevelopedLayout,
                timeDevelopedLayout, dateDevelopedTextView, timeDevelopedTextView, dateTimeDeveloped,
                clearDateDeveloped);

        //==========================================================================================



        //==========================================================================================
        //ISO PICKER
        isoTextView = inflatedView.findViewById(R.id.iso_text);
        isoTextView.setText(
                 newIso == 0 ? "" : String.valueOf(newIso)
        );

        final LinearLayout isoLayout = inflatedView.findViewById(R.id.iso_layout);
        isoLayout.setOnClickListener(view -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
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
                    (dialogInterface, i) -> {
                        newIso = Integer.parseInt(isoValues[isoPicker.getValue()]);
                        isoTextView.setText(newIso == 0 ? "" : String.valueOf(newIso));
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
        });
        //==========================================================================================




        //==========================================================================================
        //PUSH PULL PICKER
        final TextView pushPullTextView = inflatedView.findViewById(R.id.push_pull_text);
        pushPullTextView.setText(
                newPushPull == null || newPushPull.equals("0") ? "" : newPushPull
        );

        final LinearLayout pushPullLayout = inflatedView.findViewById(R.id.push_pull_layout);
        pushPullLayout.setOnClickListener(view -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            final View dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null);
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
                    (dialogInterface, i) -> {
                        newPushPull = compValues[pushPullPicker.getValue()];
                        pushPullTextView.setText(
                                newPushPull.equals("0") ? "" : newPushPull
                        );
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
        });
        //==========================================================================================



        //==========================================================================================
        //FORMAT PICK DIALOG
        final TextView formatTextView = inflatedView.findViewById(R.id.format_text);
        if (roll.getFormat() == 0) roll.setFormat(0);
        formatTextView.setText(getResources().getStringArray(R.array.FilmFormats)[roll.getFormat()]);
        newFormat = roll.getFormat();

        final LinearLayout formatLayout = inflatedView.findViewById(R.id.format_layout);
        formatLayout.setOnClickListener(view -> {
            final int checkedItem = newFormat;
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(R.string.ChooseFormat));
            builder.setSingleChoiceItems(R.array.FilmFormats, checkedItem,
                    (dialogInterface, i) -> {
                        newFormat = i;
                        formatTextView.setText(getResources().getStringArray(R.array.FilmFormats)[i]);
                        dialogInterface.dismiss();
                    });
            builder.setNegativeButton(getResources().getString(R.string.Cancel),
                    (dialogInterface, i) -> {
                        //Do nothing
                    });
            builder.create().show();
        });
        //==========================================================================================



        //FINALISE SETTING UP THE DIALOG

        alert.setPositiveButton(positiveButton, null);
        alert.setNegativeButton(R.string.Cancel, (dialog, which) -> {
            //dialog.cancel();
            final Intent intent = new Intent();
            getTargetFragment().onActivityResult(
                    getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
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
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

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
                roll.setDate(dateLoadedManager.getDateTime());
                roll.setUnloaded(dateUnloadedManager.getDateTime());
                roll.setDeveloped(dateDevelopedManager.getDateTime());
                roll.setIso(newIso);
                roll.setPushPull(newPushPull);
                roll.setFormat(newFormat);
                roll.setFilmStockId(newFilmStock != null ? newFilmStock.getId() : 0);

                final Intent intent = new Intent();
                intent.putExtra(ExtraKeys.ROLL, roll);
                dialog.dismiss();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, intent);
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.NoName),
                        Toast.LENGTH_SHORT).show();
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
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {

            case REQUEST_CODE_ADD_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.
                    newCamera = data.getParcelableExtra(ExtraKeys.CAMERA);
                    final long rowId = database.addCamera(newCamera);
                    newCamera.setId(rowId);
                    cameraList.add(newCamera);
                    cameraTextView.setText(newCamera.getName());

                }
                break;

            case REQUEST_CODE_SELECT_FILM_STOCK:

                if (resultCode != Activity.RESULT_OK) return;

                newFilmStock = data.getParcelableExtra(ExtraKeys.FILM_STOCK);
                filmStockTextView.setText(newFilmStock.getName());
                nameEditText.setHint(newFilmStock.getName());
                filmStockClearImageView.setVisibility(View.VISIBLE);
                // If the film stock ISO is defined, set the ISO
                if (newFilmStock.getIso() != 0) {
                    newIso = newFilmStock.getIso();
                    isoTextView.setText(newIso == 0 ? "" : String.valueOf(newIso));
                }
                break;

        }
    }

}
