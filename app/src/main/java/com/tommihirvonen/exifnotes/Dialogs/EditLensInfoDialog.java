package com.tommihirvonen.exifnotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

//Copyright 2016
//Tommi Hirvonen

public class EditLensInfoDialog extends DialogFragment {

    public static final String TAG = "LensInfoDialogFragment";

    public EditLensInfoDialog(){

    }

    Lens lens;

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams") final View inflator = linf.inflate(R.layout.lens_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        lens = getArguments().getParcelable("LENS");
        if (lens == null) lens = new Lens();

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_make);
        et1.setText(lens.getMake());
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_model);
        et2.setText(lens.getModel());
        final EditText et3 = (EditText) inflator.findViewById(R.id.txt_serial_number);
        et3.setText(lens.getSerialNumber());



        alert.setPositiveButton(positiveButton, null);

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
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
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lens.setMake(et1.getText().toString());
                lens.setModel(et2.getText().toString());
                lens.setSerialNumber(et3.getText().toString());

                if (lens.getMake().length() > 0 && lens.getModel().length() > 0) {
                    // Return the new entered name to the calling activity
                    Intent intent = new Intent();
                    intent.putExtra("LENS", lens);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                } else if (lens.getMake().length() == 0 && lens.getModel().length() == 0) {
                    // No make or model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel), Toast.LENGTH_SHORT).show();
                } else if (lens.getMake().length() > 0 && lens.getModel().length() == 0) {
                    // No model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
                } else if (lens.getMake().length() == 0 && lens.getModel().length() > 0) {
                    // No make was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
                }
            }
        });
        return dialog;
    }

}
