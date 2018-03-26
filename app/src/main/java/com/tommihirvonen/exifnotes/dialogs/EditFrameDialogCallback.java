package com.tommihirvonen.exifnotes.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.tommihirvonen.exifnotes.utilities.ExtraKeys;

/**
 * Class extended from EditFrameDialog.
 * Instead of target fragment, this class passes the result through an interface.
 */
public class EditFrameDialogCallback extends EditFrameDialog {

    /**
     * Interface implemented by the calling class
     */
    public interface OnPositiveButtonClickedListener {
        void onPositiveButtonClicked(int requestCode, int resultCode, Intent data);
    }

    /**
     * Reference to the implementing class's interface
     */
    private OnPositiveButtonClickedListener callback;

    /**
     * Call parent class constructor
     */
    public EditFrameDialogCallback(){
        super();
    }

    /**
     * Method to set the reference to the calling class's interface
     * @param listener calling class's listener interface
     */
    public void setOnPositiveButtonClickedListener (OnPositiveButtonClickedListener listener) {
        callback = listener;
    }

    /**
     * Get the dialog from parent class, but edit its positive and negative button listeners.
     * Pass the result to the calling class through its interface.
     *
     * @param SavedInstanceState possible saved state in case the DialogFragment was resumed
     * @return dialog with edited positive and negative button click listeners
     */
    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        final AlertDialog dialog = (AlertDialog) super.onCreateDialog(SavedInstanceState);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new OnPositiveButtonClickListener(dialog) {
            @Override
            public void onClick(View v) {
                super.onClick(v);
                // Return the new entered name to the calling activity
                Intent intent = new Intent();
                intent.putExtra(ExtraKeys.FRAME, frame);
                callback.onPositiveButtonClicked(getTargetRequestCode(), Activity.RESULT_OK, intent);
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        return dialog;
    }

}
