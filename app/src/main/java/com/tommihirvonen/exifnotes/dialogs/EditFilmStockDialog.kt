package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities

class EditFilmStockDialog : DialogFragment() {

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButtonText = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        val filmStock = requireArguments().getParcelable(ExtraKeys.FILM_STOCK) ?: FilmStock()

        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_film, null)
        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(view)
                .setCustomTitle(Utilities.buildCustomDialogTitleTextView(requireActivity(), title))

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(requireActivity())) {
            listOf<View>(
                    view.findViewById(R.id.divider_view1), view.findViewById(R.id.divider_view2),
                    view.findViewById(R.id.divider_view3), view.findViewById(R.id.divider_view4)
            ).forEach { it.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white)) }
        }

        val manufacturerEditText = view.findViewById<EditText>(R.id.manufacturer_editText)
        val filmStockEditText = view.findViewById<EditText>(R.id.filmStock_editText)
        val isoEditText = view.findViewById<EditText>(R.id.iso_editText)
        manufacturerEditText.setText(filmStock.make)
        filmStockEditText.setText(filmStock.model)
        isoEditText.setText(filmStock.iso.toString())
        isoEditText.filters = arrayOf<InputFilter>(IsoInputFilter())

        val filmTypeSpinner = view.findViewById<Spinner>(R.id.spinner_film_type)
        try {
            filmTypeSpinner.setSelection(filmStock.type)
        } catch (ignore: ArrayIndexOutOfBoundsException) {}

        val filmProcessSpinner = view.findViewById<Spinner>(R.id.spinner_film_process)
        try {
            filmProcessSpinner.setSelection(filmStock.process)
        } catch (ignore: ArrayIndexOutOfBoundsException) {}

        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, Intent())
        }.setPositiveButton(positiveButtonText, null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val manufacturerName = manufacturerEditText.text.toString()
            val filmStockName = filmStockEditText.text.toString()
            if (manufacturerName.isEmpty() || filmStockName.isEmpty()) {
                Toast.makeText(requireActivity(), R.string.ManufacturerOrFilmStockNameCannotBeEmpty,
                        Toast.LENGTH_SHORT).show()
            } else {
                filmStock.make = manufacturerName
                filmStock.model = filmStockName
                try {
                    filmStock.iso = isoEditText.text.toString().toInt()
                } catch (ignored: NumberFormatException) {
                    filmStock.iso = 0
                }
                filmStock.type = filmTypeSpinner.selectedItemPosition
                filmStock.process = filmProcessSpinner.selectedItemPosition

                dialog.dismiss()
                val intent = Intent()
                intent.putExtra(ExtraKeys.FILM_STOCK, filmStock)
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            }
        }

        return dialog
    }

    /**
     * Private InputFilter class used to make sure user entered ISO values are between 0 and 1000000
     */
    private inner class IsoInputFilter : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int,
                            dend: Int): CharSequence? {
            try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in 0..1000000) return null
            } catch (ignored: NumberFormatException) { }
            return ""
        }
    }
}