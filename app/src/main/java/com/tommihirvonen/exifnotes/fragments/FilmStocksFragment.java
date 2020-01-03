package com.tommihirvonen.exifnotes.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.adapters.GearAdapter;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.dialogs.EditFilmStockDialog;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FilmStocksFragment extends Fragment implements View.OnClickListener {

    private FilmDbHelper database;
    private List<FilmStock> filmStocks;
    private boolean fragmentVisible = false;
    private TextView noAddedFilmsTextView;
    private RecyclerView filmStocksRecyclerView;
    private GearAdapter filmStockAdapter;
    private static final int ADD_FILM_STOCK = 1;
    private static final int EDIT_FILM_STOCK = 2;
    public static final int SORT_MODE_NAME = 1;
    public static final int SORT_MODE_ISO = 2;
    private int sortMode = SORT_MODE_NAME;

    public int getSortMode() {
        return sortMode;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        database = FilmDbHelper.getInstance(getActivity());
        filmStocks = database.getAllFilmStocks();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        @SuppressLint("InflateParams")
        final View view = inflater.inflate(R.layout.fragment_films, null);
        final FloatingActionButton floatingActionButton = view.findViewById(R.id.fab_films);
        floatingActionButton.setOnClickListener(this);

        // Also change the floating action button color. Use the darker secondaryColor for this.
        final int secondaryColor = Utilities.getSecondaryUiColor(getActivity());
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        noAddedFilmsTextView = view.findViewById(R.id.no_added_film_stocks);
        filmStocksRecyclerView = view.findViewById(R.id.films_recycler_view);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        filmStocksRecyclerView.setLayoutManager(layoutManager);
        filmStocksRecyclerView.addItemDecoration(
                new DividerItemDecoration(
                        filmStocksRecyclerView.getContext(), layoutManager.getOrientation()
                )
        );
        filmStockAdapter = new GearAdapter(getActivity(), filmStocks);
        filmStocksRecyclerView.setAdapter(filmStockAdapter);
        if (!filmStocks.isEmpty()) noAddedFilmsTextView.setVisibility(View.GONE);
        filmStockAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onResume() {
        fragmentVisible = true;
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        fragmentVisible = false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_films) {
            final EditFilmStockDialog dialog = new EditFilmStockDialog();
            dialog.setTargetFragment(this, ADD_FILM_STOCK);
            final Bundle arguments = new Bundle();
            arguments.putString(ExtraKeys.TITLE, getResources().getString(R.string.AddNewFilmStock));
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.Add));
            dialog.setArguments(arguments);
            dialog.show(getFragmentManager().beginTransaction(), null);
        }
    }


    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (fragmentVisible) {

            final int position = item.getOrder();
            final FilmStock filmStock = filmStocks.get(position);

            switch (item.getItemId()) {

                case R.id.menu_item_delete:

                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(
                            getResources().getString(R.string.DeleteFilmStock) + " " + filmStock.getName()
                    );
                    if (database.isFilmStockBeingUsed(filmStock)) {
                        builder.setMessage(R.string.FilmStockIsInUseConfirmation);
                    }
                    builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {/*Do nothing*/});
                    builder.setPositiveButton(R.string.OK, (dialog, which) -> {
                       database.deleteFilmStock(filmStock);
                       filmStocks.remove(position);
                       if (filmStocks.isEmpty()) noAddedFilmsTextView.setVisibility(View.VISIBLE);
                       filmStockAdapter.notifyDataSetChanged();
                    });
                    builder.create().show();

                    return true;

                case R.id.menu_item_edit:

                    final EditFilmStockDialog dialog = new EditFilmStockDialog();
                    dialog.setTargetFragment(this, EDIT_FILM_STOCK);
                    final Bundle arguments = new Bundle();
                    arguments.putString(ExtraKeys.TITLE, getResources().getString(R.string.EditFilmStock));
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, getResources().getString(R.string.OK));
                    arguments.putParcelable(ExtraKeys.FILM_STOCK, filmStock);
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), null);

                    return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch (requestCode) {

            case ADD_FILM_STOCK:

                if (resultCode == Activity.RESULT_OK) {
                    final FilmStock filmStock = data.getParcelableExtra(ExtraKeys.FILM_STOCK);
                    final long rowId = database.addFilmStock(filmStock);
                    filmStock.setId(rowId);
                    filmStocks.add(filmStock);
                    sortFilmStocks();
                    final int position = filmStocks.indexOf(filmStock);
                    filmStockAdapter.notifyItemInserted(position);
                    filmStocksRecyclerView.scrollToPosition(position);
                }

                break;

            case EDIT_FILM_STOCK:

                if (resultCode == Activity.RESULT_OK) {
                    final FilmStock filmStock = data.getParcelableExtra(ExtraKeys.FILM_STOCK);
                    database.updateFilmStock(filmStock);
                    final int oldPosition = filmStocks.indexOf(filmStock);
                    sortFilmStocks();
                    final int newPosition = filmStocks.indexOf(filmStock);
                    filmStockAdapter.notifyItemChanged(oldPosition);
                    filmStockAdapter.notifyItemMoved(oldPosition, newPosition);
                }

                break;

        }
    }

    private void sortFilmStocks() {
        setSortMode(sortMode, false);
    }

    public void setSortMode(final int sortMode_, final boolean notifyDataSetChanged) {
        if (sortMode_ == SORT_MODE_NAME) {
            sortMode = SORT_MODE_NAME;
            Collections.sort(filmStocks, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        } else if (sortMode_ == SORT_MODE_ISO) {
            sortMode = SORT_MODE_ISO;
            Collections.sort(filmStocks, (o1, o2) -> Integer.compare(o1.getIso(), o2.getIso()));
        }
        if (notifyDataSetChanged) {
            filmStockAdapter.notifyDataSetChanged();
        }
    }

    public void filterFilmStocks(@NonNull final List<String> manufacturers) {
        filmStocks = database.getAllFilmStocks();
        if (!manufacturers.isEmpty()) {
            final Iterator<FilmStock> iterator = filmStocks.iterator();
            while (iterator.hasNext()) {
                final FilmStock filmStock = iterator.next();
                if (!manufacturers.contains(filmStock.getMake())) iterator.remove();
            }
        }
        sortFilmStocks();
        filmStockAdapter.setGearList(filmStocks);
        filmStockAdapter.notifyDataSetChanged();
    }

}
