package com.tommihirvonen.filmphotonotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Tommi on 23.2.2015.
 */



public class set_custom_dialog extends DialogFragment
        //implements View.OnClickListener
{

    public static final String TAG = "SetNameDialogFragment";

    //private EditText txtName;
    //private Button btnDone;

    private OnNameSettedCallback callback;

    public interface OnNameSettedCallback {
        void onNameSetted(String newName);
    }


    public set_custom_dialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnNameSettedCallback) activity;
        }
        catch(ClassCastException e) {
            Log.e(TAG, "The MainActivity should implement the OnNameSettedCallback interface");
            e.printStackTrace();
        }
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.custom_dialog, container);
//        txtName = (EditText) view.findViewById(R.id.txt_name);
//        btnDone = (Button) view.findViewById(R.id.btn_done);
//
//        // Set the dialog's title
//        getDialog().setTitle("Add new roll");
//
//        btnDone.setOnClickListener(this);
//
//        txtName.requestFocus();
//        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
//
//        return view;
//    }

    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//
//        // Get the layout inflater
//        LayoutInflater inflater = getActivity().getLayoutInflater();
//        builder.setTitle("Add new roll");
//
//
//        // Inflate and set the layout for the dialog
//        // Pass null as the parent view because its going in the dialog layout
//        builder.setView(inflater.inflate(R.layout.custom_dialog, null))
//
//                // Add action buttons
//                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//
//                        Editable name = txtName.getText();
//
//                        if(!TextUtils.isEmpty(name)) {
//                            // Return the new entered name to the calling activity
//                            callback.onNameSetted(name.toString());
//
//                        }
//                        else
//                            Toast.makeText(getActivity(), "You should enter your name !", Toast.LENGTH_LONG).show();
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        set_custom_dialog.this.getDialog().cancel();
//                    }
//                });
//        AlertDialog dialog = builder.create();
//        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
//        //return builder.create();
//        return dialog;

        LayoutInflater linf = getActivity().getLayoutInflater();
        final View inflator = linf.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle("Add new roll");

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_name);


        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String name = et1.getText().toString();

                //do operations using s1
                if(!name.isEmpty()) {
                    // Return the new entered name to the calling activity
                    callback.onNameSetted(name);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }


//    @Override
//    public void onClick(View v) {
//        Editable name = txtName.getText();
//        if(!TextUtils.isEmpty(name)) {
//            // Return the new entered name to the calling activity
//            callback.onNameSetted(name.toString());
//            this.dismiss();
//        }
//        else
//            Toast.makeText(getActivity(), "You should enter your name !", Toast.LENGTH_LONG).show();
//    }
}

