package com.tommihirvonen.filmphotonotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

// Copyright 2015
// Tommi Hirvonen

public class RollNameDialog extends DialogFragment {

    public static final String TAG = "SetNameDialogFragment";

    private onNameSetCallback callback;

    public interface onNameSetCallback {
        void onNameSet(String newName, String newNote);
    }


    public RollNameDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (onNameSetCallback) activity;
        }
        catch(ClassCastException e) {
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

        alert.setTitle(R.string.NewRoll);

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_name);
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_note);


        alert.setPositiveButton(R.string.Add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String name = et1.getText().toString();
                String note = et2.getText().toString();

                if(name.length() != 0) {
                    // Return the new entered name to the calling activity
                    callback.onNameSet(name, note);
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

