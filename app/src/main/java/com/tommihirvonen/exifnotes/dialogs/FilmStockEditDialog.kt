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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogFilmBinding
import com.tommihirvonen.exifnotes.entities.FilmStock
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.setNavigationResult
import com.tommihirvonen.exifnotes.viewmodels.FilmStockEditViewModel
import com.tommihirvonen.exifnotes.viewmodels.FilmStockEditViewModelFactory

class FilmStockEditDialog : DialogFragment() {

    private val arguments by navArgs<FilmStockEditDialogArgs>()

    private val editModel by viewModels<FilmStockEditViewModel> {
        val filmStock = arguments.filmStock?.copy() ?: FilmStock()
        FilmStockEditViewModelFactory(requireActivity().application, filmStock)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        val binding = DialogFilmBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setTitle(arguments.title)
            .setPositiveButton(arguments.positiveButtonText, null)
            .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        binding.viewmodel = editModel.observable
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (editModel.validate()) {
                setNavigationResult(editModel.filmStock, ExtraKeys.FILM_STOCK)
                findNavController().navigateUp()
            }
        }
        return dialog
    }
}