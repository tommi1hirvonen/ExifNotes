package com.tommihirvonen.filmphotonotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

// Copyright 2015
// Tommi Hirvonen

public class LensAdapter extends ArrayAdapter<Lens> {

    // This LensAdapter acts as an ArrayAdapter to link an array and a list view together

    public LensAdapter(Context context, int textViewResourceId, ArrayList<Lens> lenses) {
        super(context, textViewResourceId, lenses);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String lens = getItem(position).getName();
        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_lens, parent, false);
        }
        // Lookup view for data population
        TextView tvLensName = (TextView) convertView.findViewById(R.id.tv_lens_name);
        // Populate the data into the template view using the data object
        tvLensName.setText(lens);

        return convertView;
    }
}
