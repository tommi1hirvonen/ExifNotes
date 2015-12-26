package com.tommihirvonen.filmphotonotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

// Copyright 2015
// Tommi Hirvonen

public class LensNameDialog extends DialogFragment {

    public static final String TAG = "SetLensNameDialogFrag";

    private onLensNameSetCallback callback;

    public interface onLensNameSetCallback {
        void onLensNameSet(String newName);
    }

    public LensNameDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (onLensNameSetCallback) activity;
        }
        catch(ClassCastException e) {
            Log.e(TAG, "The LensesActivity should implement the callback interface");
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.NewLens);

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_name);


        alert.setPositiveButton(R.string.Add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String name = et1.getText().toString();

                //do operations using s1
                if(!name.isEmpty()) {
                    // Return the new entered name to the calling activity
                    callback.onLensNameSet(name);
                }
            }
        });

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

}
