package com.tommihirvonen.exifnotes.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tommihirvonen.exifnotes.utilities.ExtraKeys

class EditFrameDialogCallback(val positiveButtonClicked: (Intent) -> Unit) : EditFrameDialog() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.positiveImageView.setOnClickListener {
            commitChanges()
            val intent = Intent()
            intent.putExtra(ExtraKeys.FRAME, frame)
            positiveButtonClicked(intent)
            dismiss()
        }
        super.onViewCreated(view, savedInstanceState)
    }

}