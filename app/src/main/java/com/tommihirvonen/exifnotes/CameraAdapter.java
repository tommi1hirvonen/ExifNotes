package com.tommihirvonen.exifnotes;

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

    FilmDbHelper database = new FilmDbHelper(getContext());

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Camera camera = getItem(position);
        ArrayList<Lens> mountableLenses = database.getMountableLenses(camera);

        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView tvCameraName = (TextView) convertView.findViewById(R.id.tv_gear_name);
        TextView tvMountables = (TextView) convertView.findViewById(R.id.tv_mountables);
        // Populate the data into the template view using the data object
        tvCameraName.setText(camera.getName());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getContext().getResources().getString(R.string.MountsTo));
        for ( Lens lens : mountableLenses ) {
            stringBuilder.append("\n- " + lens.getName());
        }
        String mountables_string = stringBuilder.toString();
        tvMountables.setText(mountables_string);

        return convertView;
    }
}
