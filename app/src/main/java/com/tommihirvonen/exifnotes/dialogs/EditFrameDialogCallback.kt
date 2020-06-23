package com.tommihirvonen.exifnotes.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tommihirvonen.exifnotes.utilities.ExtraKeys

class EditFrameDialogCallback(val onPositiveButtonClick: (Intent) -> Unit) : EditFrameDialog() {

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {
        // Get the dialog from parent class, but edit its positive and negative button listeners.
        // Pass the result to the calling class through its interface.
        val dialog = super.onCreateDialog(SavedInstanceState) as AlertDialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(object : OnPositiveButtonClickListener(dialog) {
                    override fun onClick(view: View) {
                        super.onClick(view)
                        // Return the new entered name to the calling activity
                        val intent = Intent()
                        intent.putExtra(ExtraKeys.FRAME, frame)
                        onPositiveButtonClick(intent)
                    }
                })
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { dialog.dismiss() }
        return dialog
    }

}