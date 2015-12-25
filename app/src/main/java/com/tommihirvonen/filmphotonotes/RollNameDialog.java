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



public class RollNameDialog extends DialogFragment
        //implements View.OnClickListener
{

    public static final String TAG = "SetNameDialogFragment";

    //private EditText txtName;
    //private Button btnDone;

    private onNameSetCallback callback;

    public interface onNameSetCallback {
        void onNameSet(String newName);
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
            Log.e(TAG, "The MainActivity should implement the onNameSetCallback interface");
            e.printStackTrace();
        }
    }


    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {


        LayoutInflater linf = getActivity().getLayoutInflater();
        final View inflator = linf.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.NewRoll);

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_name);


        alert.setPositiveButton(R.string.Add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String name = et1.getText().toString();

                //do operations using s1
                if(!name.isEmpty()) {
                    // Return the new entered name to the calling activity
                    callback.onNameSet(name);
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

