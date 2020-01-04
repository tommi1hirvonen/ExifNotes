package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.adapters.FilmManufacturerAdapter;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;

import java.util.List;

public class SelectFilmStockDialog extends DialogFragment{

    public SelectFilmStockDialog() { }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        final View view = inflater.inflate(R.layout.dialog_select_film_stock, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> dialog.dismiss());

        final FilmDbHelper database = FilmDbHelper.getInstance(getContext());
        final List<String> manufacturers = database.getAllFilmManufacturers();
        final RecyclerView manufacturersRecyclerView = view.findViewById(R.id.recycler_view_manufacturers);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        manufacturersRecyclerView.setLayoutManager(layoutManager);
        manufacturersRecyclerView.addItemDecoration(new DividerItemDecoration(
                manufacturersRecyclerView.getContext(), layoutManager.getOrientation()));

        final AlertDialog dialog = builder.create();

        final FilmManufacturerAdapter adapter = new FilmManufacturerAdapter(getContext(), manufacturers,
                filmStock -> {
            dialog.dismiss();
            final Intent intent = new Intent();
            intent.putExtra(ExtraKeys.FILM_STOCK, filmStock);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        });

        manufacturersRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        return dialog;
    }

}
