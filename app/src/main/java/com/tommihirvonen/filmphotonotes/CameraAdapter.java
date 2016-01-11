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

public class CameraAdapter extends ArrayAdapter<Camera> {

    // This CameraAdapter acts as an ArrayAdapter to link an array and a list view together

    public CameraAdapter(Context context, int textViewResourceId, ArrayList<Camera> cameras) {
        super(context, textViewResourceId, cameras);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String camera = getItem(position).getName();
        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView tvCameraName = (TextView) convertView.findViewById(R.id.tv_gear_name);
        // Populate the data into the template view using the data object
        tvCameraName.setText(camera);

        return convertView;
    }
}
