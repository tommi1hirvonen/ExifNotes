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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogFilmBinding
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities

class EditFilmStockDialog : DialogFragment() {

    private lateinit var binding: DialogFilmBinding

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        binding = DialogFilmBinding.inflate(layoutInflater)

        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButtonText = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        val filmStock = requireArguments().getParcelable(ExtraKeys.FILM_STOCK) ?: FilmStock()

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(binding.root)
                .setCustomTitle(Utilities.buildCustomDialogTitleTextView(requireActivity(), title))

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(requireActivity())) {
            listOf(binding.dividerView1, binding.dividerView2, binding.dividerView3, binding.dividerView4)
                    .forEach { it.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white)) }
        }

        binding.manufacturerEditText.setText(filmStock.make)
        binding.filmStockEditText.setText(filmStock.model)
        binding.isoEditText.setText(filmStock.iso.toString())
        binding.isoEditText.filters = arrayOf<InputFilter>(IsoInputFilter())

        try {
            binding.spinnerFilmType.setSelection(filmStock.type)
        } catch (ignore: ArrayIndexOutOfBoundsException) {}

        try {
            binding.spinnerFilmProcess.setSelection(filmStock.process)
        } catch (ignore: ArrayIndexOutOfBoundsException) {}

        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, Intent())
        }.setPositiveButton(positiveButtonText, null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val manufacturerName = binding.manufacturerEditText.text.toString()
            val filmStockName = binding.filmStockEditText.text.toString()
            if (manufacturerName.isEmpty() || filmStockName.isEmpty()) {
                Toast.makeText(requireActivity(), R.string.ManufacturerOrFilmStockNameCannotBeEmpty,
                        Toast.LENGTH_SHORT).show()
            } else {
                filmStock.make = manufacturerName
                filmStock.model = filmStockName
                try {
                    filmStock.iso = binding.isoEditText.text.toString().toInt()
                } catch (ignored: NumberFormatException) {
                    filmStock.iso = 0
                }
                filmStock.type = binding.spinnerFilmType.selectedItemPosition
                filmStock.process = binding.spinnerFilmProcess.selectedItemPosition

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