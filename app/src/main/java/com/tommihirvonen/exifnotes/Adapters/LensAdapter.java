package com.tommihirvonen.exifnotes.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.Datastructures.Filter;
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.ArrayList;

// Copyright 2015
// Tommi Hirvonen

/**
 * LensAdapter acts as an ArrayAdapter to link an ArrayList and a ListView of lenses together.
 */
public class LensAdapter extends ArrayAdapter<Lens> {

    // This LensAdapter acts as an ArrayAdapter to link an array and a list view together

    public LensAdapter(Context context, int textViewResourceId, ArrayList<Lens> lenses) {
        super(context, textViewResourceId, lenses);
    }

    private FilmDbHelper database = new FilmDbHelper(getContext());

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

        Lens lens = getItem(position);
        ArrayList<Camera> mountableCameras = database.getMountableCameras(lens);
        ArrayList<Filter> mountableFilters = database.getMountableFilters(lens);

        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView tvLensName = (TextView) convertView.findViewById(R.id.tv_gear_name);
        TextView tvMountables = (TextView) convertView.findViewById(R.id.tv_mountables);

        // Populate the data into the template view using the data object
        tvLensName.setText(lens.getMake() + " " + lens.getModel());

        //Build string to display mountable cameras and filters
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getContext().getResources().getString(R.string.MountsTo));
        for ( Camera camera : mountableCameras ) {
            stringBuilder.append("\n- " + camera.getMake() + " " + camera.getModel());
        }
        //Add extra line change if there are mountable filters
        if ( mountableFilters.size() > 0 ) stringBuilder.append("\n");
        for (Filter filter : mountableFilters) {
            stringBuilder.append("\n- " + filter.getMake() + " " + filter.getModel());
        }
        String mountables_string = stringBuilder.toString();
        tvMountables.setText(mountables_string);

        return convertView;
    }
}
