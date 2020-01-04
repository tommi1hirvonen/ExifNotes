package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;

import java.util.List;

public class FilmManufacturerAdapter extends RecyclerView.Adapter<FilmManufacturerAdapter.ViewHolder> {

    private Context context;
    private List<String> manufacturers;
    private boolean[] expandedManufacturers;
    private FilmDbHelper database;
    private OnFilmStockSelectedListener listener;

    public interface OnFilmStockSelectedListener {
        void onFilmStockSelected(FilmStock filmStock);
    }

    public FilmManufacturerAdapter(@NonNull final Context context,
                                   @NonNull final List<String> manufacturers,
                                   @NonNull final OnFilmStockSelectedListener listener) {
        this.context = context;
        this.manufacturers = manufacturers;
        this.listener = listener;
        expandedManufacturers = new boolean[manufacturers.size()];
        database = FilmDbHelper.getInstance(context);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView manufacturerTextView;
        final RelativeLayout manufacturerLayout;
        final LinearLayout expandLayout;
        final ImageView expandButton;
        final RecyclerView filmStocksRecyclerView;
        ViewHolder(final View itemView) {
            super(itemView);
            manufacturerTextView = itemView.findViewById(R.id.text_view_manufacturer_name);
            manufacturerLayout = itemView.findViewById(R.id.layout_manufacturer);
            expandLayout = itemView.findViewById(R.id.layout_expand);
            expandButton = itemView.findViewById(R.id.image_view_expand);
            filmStocksRecyclerView = itemView.findViewById(R.id.recycler_view_film_stocks);

        }
    }

    @NonNull
    @Override
    public FilmManufacturerAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.item_film_manufacturer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final String manufacturer = manufacturers.get(position);

        final List<FilmStock> filmStocks = database.getAllFilmStocks(manufacturer);
        final FilmStockAdapter adapter = new FilmStockAdapter(filmStocks);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        holder.filmStocksRecyclerView.setLayoutManager(layoutManager);
        holder.filmStocksRecyclerView.addItemDecoration(new DividerItemDecoration(
                holder.filmStocksRecyclerView.getContext(), layoutManager.getOrientation()));
        holder.filmStocksRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        holder.manufacturerTextView.setText(manufacturer);
        holder.manufacturerLayout.setOnClickListener(v -> {
            final boolean show = !expandedManufacturers[position];
            toggleArrow(holder.expandButton, show);
            toggleLayout(holder.expandLayout, show);
            expandedManufacturers[position] = show;
        });
    }

    @Override
    public int getItemCount() {
        return manufacturers.size();
    }

    private static void toggleLayout(final LinearLayout layout, final boolean isExpanded) {
        if (isExpanded) expand(layout);
        else collapse(layout);
    }

    private static void toggleArrow(final View view, final boolean isExpanded) {
        if (isExpanded) view.animate().setDuration(200).rotation(180);
        else view.animate().setDuration(200).rotation(0);
    }

    private static void expand(final View view) {
        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int actualHeight = view.getMeasuredHeight();
        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);
        final Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                view.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (actualHeight * interpolatedTime);
                view.requestLayout();
            }
        };
        animation.setDuration((long) (actualHeight / view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(animation);
    }

    private static void collapse(final View view) {
        final int actualHeight = view.getMeasuredHeight();
        final Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = actualHeight - (int) (actualHeight * interpolatedTime);
                    view.requestLayout();
                }
            }
        };
        animation.setDuration((long) (actualHeight/ view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(animation);
    }

    private class FilmStockAdapter extends RecyclerView.Adapter<FilmStockAdapter.ViewHolder> {
        private List<FilmStock> filmStocks;
        FilmStockAdapter(@NonNull final List<FilmStock> filmStocks) {
            this.filmStocks = filmStocks;
        }
        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView filmStockTextView;
            final LinearLayout filmStockLayout;
            ViewHolder(final View itemView) {
                super(itemView);
                filmStockTextView = itemView.findViewById(R.id.text_view_film_stock);
                filmStockLayout = itemView.findViewById(R.id.layout_film_stock);
            }
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(context).inflate(R.layout.item_film_stock, parent, false);
            return new FilmStockAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final FilmStock filmStock = filmStocks.get(position);
            holder.filmStockTextView.setText(filmStock.getName());
            holder.filmStockLayout.setOnClickListener(v -> listener.onFilmStockSelected(filmStock));
        }

        @Override
        public int getItemCount() {
            return filmStocks.size();
        }
    }

}
