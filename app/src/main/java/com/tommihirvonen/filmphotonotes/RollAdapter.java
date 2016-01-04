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

public class RollAdapter extends ArrayAdapter<Roll> {

    // This RollAdapter acts as an ArrayAdapter to link an array and a list view together

    public RollAdapter(Context context,int textViewResourceId, ArrayList<Roll> rolls) {
        super(context, textViewResourceId, rolls);
    }

    @Override
    public  View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String roll = getItem(position).getName();
        // Check if an existing view is being reused, otherwise inflate the view
        if ( convertView == null ) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_roll, parent, false);
        }
        // Lookup view for data population
        TextView tvRollName = (TextView) convertView.findViewById(R.id.tv_roll_name);

        // Populate the data into the template view using the data object
        tvRollName.setText(roll);

        return convertView;
    }

}
