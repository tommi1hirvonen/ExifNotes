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

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilmManufacturerAdapter
import com.tommihirvonen.exifnotes.entities.FilmStock
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.data.database
import com.tommihirvonen.exifnotes.utilities.setNavigationResult

class SelectFilmStockDialog : DialogFragment() {

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_select_film_stock, null)
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.SelectFilmStock)
        builder.setView(view)
        builder.setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
        val manufacturersRecyclerView: RecyclerView = view.findViewById(R.id.recycler_view_manufacturers)
        val layoutManager = LinearLayoutManager(requireActivity())
        manufacturersRecyclerView.layoutManager = layoutManager
        manufacturersRecyclerView.addItemDecoration(DividerItemDecoration(
                manufacturersRecyclerView.context, layoutManager.orientation))
        val dialog = builder.create()
        val adapter = FilmManufacturerAdapter(requireContext(), onFilmStockSelected)
        manufacturersRecyclerView.adapter = adapter
        val filmStocks = database.filmStocks
        adapter.setFilmStocks(filmStocks)
        adapter.notifyDataSetChanged()
        return dialog
    }

    private val onFilmStockSelected = { filmStock: FilmStock? ->
        setNavigationResult(filmStock, ExtraKeys.SELECT_FILM_STOCK)
        findNavController().navigateUp()
    }

}