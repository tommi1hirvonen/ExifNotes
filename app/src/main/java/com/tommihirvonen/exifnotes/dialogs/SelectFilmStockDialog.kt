package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.FilmManufacturerAdapter
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
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
        val manufacturersRecyclerView: RecyclerView = view.findViewById(R.id.recycler_view_manufacturers)
        val layoutManager = LinearLayoutManager(requireActivity())
        manufacturersRecyclerView.layoutManager = layoutManager
        manufacturersRecyclerView.addItemDecoration(DividerItemDecoration(
                manufacturersRecyclerView.context, layoutManager.orientation))
        manufacturersRecyclerView.addOnScrollListener(
                ScrollIndicatorRecyclerViewListener(
                        manufacturersRecyclerView,
                        view.findViewById(R.id.scrollIndicatorUp),
                        view.findViewById(R.id.scrollIndicatorDown)))
        val dialog = builder.create()

        val adapter = FilmManufacturerAdapter(requireContext()) { filmStock: FilmStock? ->
            dialog.dismiss()
            val bundle = Bundle()
            bundle.putParcelable(ExtraKeys.FILM_STOCK, filmStock)
            setFragmentResult("SelectFilmStockDialog", bundle)
        }

        manufacturersRecyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
        return dialog
    }

}