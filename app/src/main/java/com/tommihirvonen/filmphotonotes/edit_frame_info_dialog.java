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
public class edit_frame_info_dialog extends DialogFragment {


    String title;
    String lens;
    int count;

    static edit_frame_info_dialog newInstance(String title, String lens, int count) {
        edit_frame_info_dialog f = new edit_frame_info_dialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("lens", lens);
        args.putInt("count", count);
        f.setArguments(args);
        return f;
    }

    public static final String TAG = "SetLensDialogFragment";


    //private EditText txtName;
    //private Button btnDone;

    private OnEditSettedCallback callback;

    public interface OnEditSettedCallback {
        void onEditSetted(String newName, int frame_count);
    }


    public edit_frame_info_dialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnEditSettedCallback) activity;
        }
        catch(ClassCastException e) {
            Log.e(TAG, "The Roll_Info should implement the OnEditSettedCallback interface");
            e.printStackTrace();
        }
    }




    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        title = getArguments().getString("title");
        lens = getArguments().getString("lens");
        count = getArguments().getInt("count");

        LayoutInflater linf = getActivity().getLayoutInflater();
        final View inflator = linf.inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(title);

        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.txt_name);
        et1.setHint("Used lens");
        et1.setText(lens);


        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String lens = et1.getText().toString();

                //do operations using s1
                if(!lens.isEmpty()) {
                    // Return the new entered name to the calling activity
                    callback.onEditSetted(lens, count);
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
