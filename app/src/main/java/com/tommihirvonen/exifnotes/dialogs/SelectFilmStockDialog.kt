package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilmManufacturerAdapter
import com.tommihirvonen.exifnotes.adapters.FilmManufacturerAdapter.OnFilmStockSelectedListener
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper
import com.tommihirvonen.exifnotes.utilities.Utilities.ScrollIndicatorRecyclerViewListener

class SelectFilmStockDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        @SuppressLint("InflateParams")
        val view = inflater.inflate(R.layout.dialog_select_film_stock, null)
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.SelectFilmStock)
        builder.setView(view)
        builder.setNegativeButton(R.string.Cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        val database = FilmDbHelper.getInstance(requireContext())
        val manufacturers = database.allFilmManufacturers
        val manufacturersRecyclerView: RecyclerView = view.findViewById(R.id.recycler_view_manufacturers)
        val layoutManager = LinearLayoutManager(requireActivity())
        manufacturersRecyclerView.layoutManager = layoutManager
        manufacturersRecyclerView.addItemDecoration(DividerItemDecoration(
                manufacturersRecyclerView.context, layoutManager.orientation))
        manufacturersRecyclerView.addOnScrollListener(
                ScrollIndicatorRecyclerViewListener(
                        requireContext(),
                        manufacturersRecyclerView,
                        view.findViewById(R.id.scrollIndicatorUp),
                        view.findViewById(R.id.scrollIndicatorDown)))
        val dialog = builder.create()

        val adapter = FilmManufacturerAdapter(requireContext(), manufacturers,
                OnFilmStockSelectedListener { filmStock: FilmStock? ->
                    dialog.dismiss()
                    val intent = Intent()
                    intent.putExtra(ExtraKeys.FILM_STOCK, filmStock)
                    targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                })

        manufacturersRecyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
        return dialog
    }

}