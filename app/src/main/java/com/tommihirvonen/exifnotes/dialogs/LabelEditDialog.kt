/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
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
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Label
import com.tommihirvonen.exifnotes.databinding.DialogLabelBinding
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.setNavigationResult
import com.tommihirvonen.exifnotes.viewmodels.LabelEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.LabelEditViewModelFactory

class LabelEditDialog : DialogFragment() {

    private val arguments by navArgs<LabelEditDialogArgs>()

    private val model by viewModels<LabelEditViewModel> {
        val label = arguments.label?.copy() ?: Label()
        LabelEditViewModelFactory(requireActivity().application, label)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        val binding = DialogLabelBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(arguments.title)
        builder.setView(binding.root)
        binding.viewmodel = model.observable
        builder.setPositiveButton(arguments.positiveButton, null)
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
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
                setNavigationResult(model.label, ExtraKeys.LABEL)
                findNavController().navigateUp()
            }
        }
        return dialog
    }
}