package com.tommihirvonen.exifnotes.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.ArrayList;

// Copyright 2015
// Tommi Hirvonen

/**
 * CameraAdapter acts as an ArrayAdapter to link an ArrayList and a ListView of cameras together.
 */
public class CameraAdapter extends ArrayAdapter<Camera> {

    // This CameraAdapter acts as an ArrayAdapter to link an array and a list view together

    public CameraAdapter(Context context, int textViewResourceId, ArrayList<Camera> cameras) {
        super(context, textViewResourceId, cameras);
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
        tvCameraName.setText(camera.getMake() + " " + camera.getModel());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getContext().getResources().getString(R.string.MountsTo));
        for ( Lens lens : mountableLenses ) {
            stringBuilder.append("\n- " + lens.getMake() + " " + lens.getModel());
        }
        String mountables_string = stringBuilder.toString();
        tvMountables.setText(mountables_string);

        return convertView;
    }
}
