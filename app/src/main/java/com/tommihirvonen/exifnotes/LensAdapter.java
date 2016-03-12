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

public class LensAdapter extends ArrayAdapter<Lens> {

    // This LensAdapter acts as an ArrayAdapter to link an array and a list view together

    public LensAdapter(Context context, int textViewResourceId, ArrayList<Lens> lenses) {
        super(context, textViewResourceId, lenses);
    }

    FilmDbHelper database = new FilmDbHelper(getContext());

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Lens lens = getItem(position);
        ArrayList<Camera> mountableCameras = database.getMountableCameras(lens);

        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView tvLensName = (TextView) convertView.findViewById(R.id.tv_gear_name);
        TextView tvMountables = (TextView) convertView.findViewById(R.id.tv_mountables);
        // Populate the data into the template view using the data object
        tvLensName.setText(lens.getName());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getContext().getResources().getString(R.string.MountsTo));
        for ( Camera camera : mountableCameras ) {
            stringBuilder.append("\n- " + camera.getName());
        }
        String mountables_string = stringBuilder.toString();
        tvMountables.setText(mountables_string);

        return convertView;
    }
}
