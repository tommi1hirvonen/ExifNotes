package com.tommihirvonen.exifnotes.Dialogs;

//Copyright 2016
//Tommi Hirvonen

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

import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

public class EditCameraInfoDialog extends DialogFragment {

    public static final String TAG = "CameraInfoDialogFragment";

    Camera camera;

    public EditCameraInfoDialog(){

    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        final View inflator = linf.inflate(R.layout.camera_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        String title = getArguments().getString("TITLE");
        String positiveButton = getArguments().getString("POSITIVE_BUTTON");
        camera = getArguments().getParcelable("CAMERA");
        if (camera == null) camera = new Camera();


        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_make);
        et1.setText(camera.getMake());
        final EditText et2 = (EditText) inflator.findViewById(R.id.txt_model);
        et2.setText(camera.getModel());
        final EditText et3 = (EditText) inflator.findViewById(R.id.txt_serial_number);
        et3.setText(camera.getSerialNumber());

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
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
        // We override the positive button onClick so that we can dismiss the dialog
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                camera.setMake(et1.getText().toString());
                camera.setModel(et2.getText().toString());
                camera.setSerialNumber(et3.getText().toString());

                if (camera.getMake().length() > 0 && camera.getModel().length() > 0) {
                    // Return the new entered name to the calling activity
                    Intent intent = new Intent();
                    intent.putExtra("CAMERA", camera);
                    dialog.dismiss();
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                } else if (camera.getMake().length() == 0 && camera.getModel().length() == 0) {
                    // No make or model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMakeOrModel), Toast.LENGTH_SHORT).show();
                } else if (camera.getMake().length() > 0 && camera.getModel().length() == 0) {
                    // No model was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoModel), Toast.LENGTH_SHORT).show();
                } else if (camera.getMake().length() == 0 && camera.getModel().length() > 0) {
                    // No make was set
                    Toast.makeText(getActivity(), getResources().getString(R.string.NoMake), Toast.LENGTH_SHORT).show();
                }
            }
        });
        return dialog;
    }

}
