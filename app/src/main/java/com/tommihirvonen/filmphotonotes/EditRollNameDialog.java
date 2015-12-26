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

public class EditRollNameDialog extends DialogFragment {

    public String oldName;

    public static final String TAG = "EditNameDialogFragment";

    private OnNameEditedCallback callback;

    public interface OnNameEditedCallback {
        void OnNameEdited(String newName, String oldName);
    }

    public EditRollNameDialog () {

    }

    // Android doesn't like fragments to be created with arguments. This is a workaround.
    public void setOldName (String oldName) {
        this.oldName = oldName;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnNameEditedCallback) activity;
        }
        catch(ClassCastException e) {
            Log.e(TAG, "The MainActivity should implement the OnNameEditedCallback interface");
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

        alert.setTitle(R.string.RenameRoll);
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById((R.id.txt_name));
        // Show old name on the input field by default
        et1.setText(oldName);
        // Place the cursor at the end of the input field
        et1.setSelection(et1.getText().length());

        alert.setPositiveButton(R.string.Rename, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = et1.getText().toString();
                if (!newName.isEmpty()) {
                    callback.OnNameEdited(newName, oldName);
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
