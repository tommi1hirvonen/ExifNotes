package com.tommihirvonen.exifnotes.Adapters;

// Copyright 2015
// Tommi Hirvonen

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.Datastructures.Filter;
import com.tommihirvonen.exifnotes.R;

import java.util.ArrayList;

public class FilterAdapter extends ArrayAdapter<Filter> {

    public FilterAdapter(Context context, int resource, ArrayList<Filter> filters) {
        super(context, resource, filters);
    }

    /**
     * This function inflates a view in the ListView.
     *
     * @param position the position of the item in the list.
     * @param convertView the view to be inflated
     * @param parent the parent to which the view will eventually be attached.
     * @return the inflated view to be showed in the ListView
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        Filter filter = getItem(position);

        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView tvFilterName = (TextView) convertView.findViewById(R.id.tv_gear_name);
        TextView tvMountables = (TextView) convertView.findViewById(R.id.tv_mountables);

        // Populate the data into the template view using the data object
        tvFilterName.setText(filter.getMake() + " " + filter.getModel());
        tvMountables.setText("");

        return convertView;
    }
}
