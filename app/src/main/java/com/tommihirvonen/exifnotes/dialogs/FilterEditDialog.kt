/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogFilterBinding
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.viewmodels.FilterEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.FilterEditViewModelFactory

/**
 * Dialog to edit a Filter's information
 */
class FilterEditDialog : DialogFragment() {

    val filter by lazy { requireArguments().getParcelable(ExtraKeys.FILTER) ?: Filter() }

    val model by lazy {
        val factory = FilterEditViewModelFactory(requireActivity().application, filter.copy())
        ViewModelProvider(this, factory)[FilterEditViewModel::class.java]
    }

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        val binding = DialogFilterBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val title = requireArguments().getString(ExtraKeys.TITLE)
        builder.setTitle(title)
        builder.setView(binding.root)
        binding.viewmodel = model.observable
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
            if (model.validate()) {
                filter.make = model.filter.make
                filter.model = model.filter.model
                // Return the new entered name to the calling activity
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.FILTER, filter)
                setFragmentResult("EditFilterDialog", bundle)
                dialog.dismiss()
            }
        }
        return dialog
    }

}