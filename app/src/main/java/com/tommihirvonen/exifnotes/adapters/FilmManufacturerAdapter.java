package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;

import java.util.List;

public class FilmManufacturerAdapter extends RecyclerView.Adapter<FilmManufacturerAdapter.ViewHolder> {

    private Context context;
    private List<String> manufacturers;
    private final SparseBooleanArray expandedManufacturers = new SparseBooleanArray();
    private final SparseBooleanArray expandAnimations = new SparseBooleanArray();
    private int currentExpandedIndex = -1;
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
        database = FilmDbHelper.getInstance(context);
        setHasStableIds(true);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView manufacturerTextView;
        final View manufacturerLayout;
        final View expandLayout;
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
        holder.filmStocksRecyclerView.setAdapter(adapter);
        holder.manufacturerTextView.setText(manufacturer);
        holder.manufacturerLayout.setOnClickListener(v -> {
            toggleManufacturer(position);
            expandOrCollapseManufacturer(holder, position);
        });
        expandOrCollapseManufacturer(holder, position);
    }

    @Override
    public int getItemCount() {
        return manufacturers.size();
    }

    @Override
    public long getItemId(int position) {
        return manufacturers.get(position).hashCode();
    }

    private void toggleManufacturer(final int position) {
        currentExpandedIndex = position;
        if (expandedManufacturers.get(position, false)) {
            expandedManufacturers.delete(position);
            expandAnimations.delete(position);
        } else {
            expandedManufacturers.put(position, true);
            expandAnimations.put(position, true);
        }
    }

    private void expandOrCollapseManufacturer(final ViewHolder holder, final int position) {
        final boolean animate = currentExpandedIndex == position;
        if (expandedManufacturers.get(position, false)) {
            toggleArrow(holder.expandButton, true, animate);
            toggleLayout(holder.expandLayout, true, animate);
        } else {
            toggleArrow(holder.expandButton, false, animate);
            toggleLayout(holder.expandLayout, false, animate);
        }
        if (animate) currentExpandedIndex = -1;
    }

    private static void toggleLayout(final View view, final boolean isExpanded,
                                     final boolean animate) {
        if (isExpanded) {
            expand(view, animate);
        } else {
            collapse(view, animate);
        }
    }

    private static void toggleArrow(final View view, final boolean isExpanded, final boolean animate) {
        if (isExpanded && animate) {
            view.animate().setDuration(200).rotation(180);
        } else if (isExpanded) {
            view.setRotation(180);
        } else if (animate) {
            view.animate().setDuration(200).rotation(0);
        } else {
            view.setRotation(0);
        }
    }

    private static void expand(final View view, final boolean animate) {
        if (animate) {
            // Call view.measure() before calling view.getMeasuredHeight()
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            final int actualHeight = view.getMeasuredHeight();
            final int currentHeight = view.getLayoutParams().height;
            view.setVisibility(View.VISIBLE);
            final Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    // interpolatedTime == 1 => the animation has reached its end
                    if (interpolatedTime == 1) {
                        view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    } else {
                        view.getLayoutParams().height = (int) (currentHeight + (actualHeight - currentHeight) * interpolatedTime);
                    }
                    view.requestLayout();
                }
            };
            animation.setDuration((long) (actualHeight / view.getContext().getResources().getDisplayMetrics().density));
            view.startAnimation(animation);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    private static void collapse(final View view, final boolean animate) {
        if (animate) {
            final int actualHeight = view.getMeasuredHeight();
            final Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    // interpolatedTime == 1 => the animation has reached its end
                    if (interpolatedTime == 1) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.getLayoutParams().height = actualHeight - (int) (actualHeight * interpolatedTime);
                        view.requestLayout();
                    }
                }
            };
            animation.setDuration((long) (actualHeight / view.getContext().getResources().getDisplayMetrics().density));
            view.startAnimation(animation);
        } else {
            view.setVisibility(View.GONE);
            view.getLayoutParams().height = 0;
        }
    }

    private class FilmStockAdapter extends RecyclerView.Adapter<FilmStockAdapter.ViewHolder> {
        private List<FilmStock> filmStocks;
        FilmStockAdapter(@NonNull final List<FilmStock> filmStocks) {
            this.filmStocks = filmStocks;
            setHasStableIds(true);
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
            holder.filmStockTextView.setText(filmStock.getModel());
            holder.filmStockLayout.setOnClickListener(v -> listener.onFilmStockSelected(filmStock));
        }

        @Override
        public int getItemCount() {
            return filmStocks.size();
        }

        @Override
        public long getItemId(int position) {
            return filmStocks.get(position).getId();
        }
    }

}
