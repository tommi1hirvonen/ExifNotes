package com.tommihirvonen.exifnotes;

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

    public EditRollNameDialog () {

    }

    // Android doesn't like fragments to be created with arguments. This is a workaround.
    public void setOldName (int rollId, String oldName, String oldNote, int camera_id) {
        this.rollId = rollId;
        this.oldName = oldName;
        this.oldNote = oldNote;
        this.camera_id = camera_id;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        if ( SavedInstanceState != null ) camera_id = SavedInstanceState.getInt("CAMERA_ID");

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
        b_camera.setText(database.getCamera(camera_id).getMake() + " " + database.getCamera(camera_id).getModel());
        b_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<String> listItems = new ArrayList<>();
                for (int i = 0; i < mCameraList.size(); ++i) {
                    listItems.add(mCameraList.get(i).getMake() + " " + mCameraList.get(i).getModel());
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
                    Intent intent = new Intent();
                    intent.putExtra("ROLL_ID", rollId);
                    intent.putExtra("NEWNAME", newName);
                    intent.putExtra("NEWNOTE", newNote);
                    intent.putExtra("CAMERA_ID", camera_id);
                    //callback.OnNameEdited(rollId, newName, newNote, camera_id);
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                }
            }
        });

        alert.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.cancel();
                Intent intent = new Intent();
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("CAMERA_ID", camera_id);
    }
}
