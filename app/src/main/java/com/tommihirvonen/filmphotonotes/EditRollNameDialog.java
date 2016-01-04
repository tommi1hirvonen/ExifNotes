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

public class EditRollNameDialog extends DialogFragment {

    public String oldName;
    public String oldNote;
    public int rollId;

    public static final String TAG = "EditNameDialogFragment";

    private OnNameEditedCallback callback;

    public interface OnNameEditedCallback {
        void OnNameEdited(int rollId, String newName, String newNote);
    }

    public EditRollNameDialog () {

    }

    // Android doesn't like fragments to be created with arguments. This is a workaround.
    public void setOldName (int rollId, String oldName, String oldNote) {
        this.rollId = rollId;
        this.oldName = oldName;
        this.oldNote = oldNote;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnNameEditedCallback) activity;
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

        alert.setTitle(R.string.EditRoll);
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById((R.id.txt_name));
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_note);
        // Show old name on the input field by default
        et1.setText(oldName);
        et2.setText(oldNote);
        // Place the cursor at the end of the input field
        et1.setSelection(et1.getText().length());

        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = et1.getText().toString();
                String newNote = et2.getText().toString();
                if (newName.length() != 0) {
                    callback.OnNameEdited(rollId, newName, newNote);
                }
            }
        });

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

}
