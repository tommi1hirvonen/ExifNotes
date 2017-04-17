package com.tommihirvonen.exifnotes.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

public class EditFrameDialogInterface extends EditFrameDialog {

    public interface OnPositiveButtonClickedListener {
        void onPositiveButtonClicked(int requestCode, int resultCode, Intent data);
    }

    private OnPositiveButtonClickedListener callback;

    public EditFrameDialogInterface(){
        super();
    }

    public void setOnPositiveButtonClickedListener (OnPositiveButtonClickedListener listener) {
        callback = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog (Bundle SavedInstanceState) {

        final AlertDialog dialog = (AlertDialog) super.onCreateDialog(SavedInstanceState);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalizeFrame();

                // Return the new entered name to the calling activity
                Intent intent = new Intent();
                intent.putExtra("FRAME", frame);
                dialog.dismiss();
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
