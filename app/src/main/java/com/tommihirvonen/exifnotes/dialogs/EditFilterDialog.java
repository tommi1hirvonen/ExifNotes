package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to edit a Filter's information
 */
public class EditFilterDialog extends DialogFragment {

    /**
     * Public constant used to tag this fragment when it is created
     */
    public static final String TAG = "EditFilterDialog";

    /**
     * The filter to be edited
     */
    private Filter filter;

    /**
     * Empty constructor
     */
    public EditFilterDialog(){

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (final Bundle SavedInstanceState) {
        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(R.layout.dialog_filter, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        final String title = getArguments().getString(ExtraKeys.TITLE);
        final String positiveButton = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        filter = getArguments().getParcelable(ExtraKeys.FILTER);
        if (filter == null) filter = new Filter();

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));

        alert.setView(inflatedView);


        //==========================================================================================
        //DIVIDERS

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(getActivity())) {
            final List<View> dividerList = new ArrayList<>();
            dividerList.add(inflatedView.findViewById(R.id.divider_view1));
            for (final View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
        }
        //==========================================================================================


        final EditText makeEditText = inflatedView.findViewById(R.id.make_editText);
        makeEditText.setText(filter.getMake());
        final EditText modelEditText = inflatedView.findViewById(R.id.model_editText);
        modelEditText.setText(filter.getModel());


        alert.setPositiveButton(positiveButton, null);

        alert.setNegativeButton(R.string.Cancel, (dialog, whichButton) -> {
            final Intent intent = new Intent();
            getTargetFragment().onActivityResult(
                    getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
        });
        final AlertDialog dialog = alert.create();
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
        // We override the positive button onClick so that we can dismiss the dialog
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            final String make = makeEditText.getText().toString();
            final String model = modelEditText.getText().toString();

            if (make.length() == 0 && model.length() == 0) {
                // No make or model was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel),
                        Toast.LENGTH_SHORT).show();
            } else if (make.length() > 0 && model.length() == 0) {
                // No model was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
            } else if (make.length() == 0) {
                // No make was set
                Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
            } else {
                filter.setMake(make);
                filter.setModel(model);
                // Return the new entered name to the calling activity
                final Intent intent = new Intent();
                intent.putExtra("FILTER", filter);
                dialog.dismiss();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(), Activity.RESULT_OK, intent);
            }
        });
        return dialog;
    }

}
