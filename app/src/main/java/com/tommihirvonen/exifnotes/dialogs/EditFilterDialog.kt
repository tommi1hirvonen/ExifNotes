package com.tommihirvonen.exifnotes.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogFilterBinding
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.buildCustomDialogTitleTextView

/**
 * Dialog to edit a Filter's information
 */
class EditFilterDialog : DialogFragment() {

    companion object {
        /**
         * Public constant used to tag this fragment when it is created
         */
        const val TAG = "EditFilterDialog"
    }

    private lateinit var binding: DialogFilterBinding

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        binding = DialogFilterBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(activity)
        val title = requireArguments().getString(ExtraKeys.TITLE)
        val titleView = buildCustomDialogTitleTextView(title)
        builder.setCustomTitle(titleView)
        builder.setView(binding.root)
        val filter = requireArguments().getParcelable(ExtraKeys.FILTER) ?: Filter()
        binding.makeEditText.setText(filter.make)
        binding.modelEditText.setText(filter.model)
        val positiveButton = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        builder.setPositiveButton(positiveButton, null)
        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            setFragmentResult("EditFilterDialog", Bundle())
        }
        val dialog = builder.create()

        // SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        dialog.show()

        // We override the positive button onClick so that we can dismiss the dialog
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val make = binding.makeEditText.text.toString()
            val model = binding.modelEditText.text.toString()
            if (make.isEmpty() && model.isEmpty()) {
                // No make or model was set
                Toast.makeText(activity, resources.getString(R.string.NoMakeOrModel),
                        Toast.LENGTH_SHORT).show()
            } else if (make.isNotEmpty() && model.isEmpty()) {
                // No model was set
                Toast.makeText(activity, resources.getString(R.string.NoModel), Toast.LENGTH_SHORT).show()
            } else if (make.isEmpty()) {
                // No make was set
                Toast.makeText(activity, resources.getString(R.string.NoMake), Toast.LENGTH_SHORT).show()
            } else {
                filter.make = make
                filter.model = model
                // Return the new entered name to the calling activity
                val bundle = Bundle()
                bundle.putParcelable("FILTER", filter)
                setFragmentResult("EditFilterDialog", bundle)
                dialog.dismiss()
            }
        }
        return dialog
    }

}