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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

public class EditRollNameDialog extends DialogFragment {

    public String oldName;
    public String oldNote;
    public int rollId;
    int camera_id;

    FilmDbHelper database;
    ArrayList<Camera> mCameraList;

    public static final String TAG = "EditNameDialogFragment";

    private OnNameEditedCallback callback;

    public interface OnNameEditedCallback {
        void OnNameEdited(int rollId, String newName, String newNote, int camera_id);
    }

    public EditRollNameDialog () {

    }

    // Android doesn't like fragments to be created with arguments. This is a workaround.
    public void setOldName (int rollId, String oldName, String oldNote, int camera_id) {
        this.rollId = rollId;
        this.oldName = oldName;
        this.oldNote = oldNote;
        this.camera_id = camera_id;
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

        database = new FilmDbHelper(getActivity());
        mCameraList = database.getAllCameras();

        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.EditRoll);
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById((R.id.txt_name));
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_note);

        final TextView b_camera = (TextView) inflator.findViewById(R.id.btn_camera);

        // CAMERA PICK DIALOG
        b_camera.setClickable(true);
        b_camera.setText(database.getCamera(camera_id).getName());
        b_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> listItems = new ArrayList<>();
                for (int i = 0; i < mCameraList.size(); ++i) {
                    listItems.add(mCameraList.get(i).getName());
                }
                final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.UsedCamera);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // listItems also contains the No lens option
                        b_camera.setText(listItems.get(which));
                        camera_id = mCameraList.get(which).getId();
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
                    callback.OnNameEdited(rollId, newName, newNote, camera_id);
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
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        return dialog;
    }

}
