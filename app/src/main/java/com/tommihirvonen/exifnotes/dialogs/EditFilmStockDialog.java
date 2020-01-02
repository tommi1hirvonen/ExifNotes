package com.tommihirvonen.exifnotes.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

public class EditFilmStockDialog extends DialogFragment {

    private FilmStock filmStock;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        final String title = getArguments().getString(ExtraKeys.TITLE);
        final String positiveButtonText = getArguments().getString(ExtraKeys.POSITIVE_BUTTON);
        filmStock = getArguments().getParcelable(ExtraKeys.FILM_STOCK);
        if (filmStock == null) filmStock = new FilmStock();

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_film, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setCustomTitle(Utilities.buildCustomDialogTitleTextView(getActivity(), title));

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(getActivity())) {
            final List<View> dividerList = new ArrayList<>();
            dividerList.add(view.findViewById(R.id.divider_view1));
            dividerList.add(view.findViewById(R.id.divider_view2));
            for (final View v : dividerList) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }
        }

        final EditText manufacturerEditText = view.findViewById(R.id.manufacturer_editText);
        final EditText filmStockEditText = view.findViewById(R.id.filmStock_editText);
        final EditText isoEditText = view.findViewById(R.id.iso_editText);
        manufacturerEditText.setText(filmStock.getMake());
        filmStockEditText.setText(filmStock.getModel());
        isoEditText.setText(Integer.toString(filmStock.getIso()));
        isoEditText.setFilters(new InputFilter[]{ new IsoInputFilter() });

        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {
            final Intent intent = new Intent();
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
        });
        builder.setPositiveButton(positiveButtonText, null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String manufacturerName = manufacturerEditText.getText().toString();
            final String filmStockName = filmStockEditText.getText().toString();
            if (manufacturerName.length() == 0 || filmStockName.length() == 0) {
                Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT).show();
            } else {
                filmStock.setMake(manufacturerName);
                filmStock.setModel(filmStockName);
                filmStock.setIso(Integer.parseInt(isoEditText.getText().toString()));
                dialog.dismiss();
                final Intent intent = new Intent();
                intent.putExtra(ExtraKeys.FILM_STOCK, filmStock);
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
            }
        });

        return dialog;
    }

    /**
     * Private InputFilter class used to make sure user entered ISO values are between 0 and 1000000
     */
    private class IsoInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (input >= 0 && input <= 1_000_000) return null;
            } catch (NumberFormatException e) {

            }
            return "";
        }
    }

}
