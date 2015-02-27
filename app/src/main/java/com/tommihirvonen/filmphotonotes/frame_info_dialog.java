package com.tommihirvonen.filmphotonotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

/**
 * Created by Tommi on 27.2.2015.
 */
public class frame_info_dialog extends DialogFragment {

    String title;

    static frame_info_dialog newInstance(String title) {
        frame_info_dialog f = new frame_info_dialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        f.setArguments(args);
        return f;
    }

    public static final String TAG = "SetNameDialogFragment";


    //private EditText txtName;
    //private Button btnDone;

    private OnInfoSettedCallback callback;

    public interface OnInfoSettedCallback {
        void onInfoSetted(String newName);
    }


    public frame_info_dialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnInfoSettedCallback) activity;
        }
        catch(ClassCastException e) {
            Log.e(TAG, "The Roll_Info should implement the OnInfoSettedCallback interface");
            e.printStackTrace();
        }
    }




    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        title = getArguments().getString("title");

        LayoutInflater linf = getActivity().getLayoutInflater();
        final View inflator = linf.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(title);

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_name);
        et1.setHint("Used lens");


        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String lens = et1.getText().toString();

                //do operations using s1
                if(!lens.isEmpty()) {
                    // Return the new entered name to the calling activity
                    callback.onInfoSetted(lens);
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

}
