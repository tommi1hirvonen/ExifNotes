package com.tommihirvonen.exifnotes.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.Datastructures.Roll;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.ArrayList;

// Copyright 2015
// Tommi Hirvonen

/**
 * RollAdapter acts as an ArrayAdapter to link an ArrayList and a ListView of rolls together.
 */
public class RollAdapter extends ArrayAdapter<Roll> {

    // This RollAdapter acts as an ArrayAdapter to link an array and a list view together

    private FilmDbHelper database;

    public RollAdapter(Context context,int textViewResourceId, ArrayList<Roll> rolls) {
        super(context, textViewResourceId, rolls);
        database = new FilmDbHelper(context);
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
    public  View getView(int position, View convertView, @NonNull ViewGroup parent) {

        Roll roll = getItem(position);
        // Get the data item for this position
        if (roll != null) {
            String rollName = roll.getName();
            String date = roll.getDate();
            String note = roll.getNote();
            long camera_id = roll.getCamera_id();
            int numberOfFrames = database.getNumberOfFrames(getItem(position));
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_roll, parent, false);
            }
            // Lookup view for data population
            TextView tvRollName = (TextView) convertView.findViewById(R.id.tv_roll_name);
            TextView tvRollDate = (TextView) convertView.findViewById(R.id.tv_roll_date);
            TextView tvRollNote = (TextView) convertView.findViewById(R.id.tv_roll_note);
            TextView tvPhotos = (TextView) convertView.findViewById(R.id.tv_photos);
            TextView tvCamera = (TextView) convertView.findViewById(R.id.tv_camera);

            // Populate the data into the template view using the data object
            tvRollName.setText(rollName);
            tvRollDate.setText(date);
            tvRollNote.setText(note);
            tvCamera.setText(database.getCamera(camera_id).getMake() + " " +
                    database.getCamera(camera_id).getModel());
            if (numberOfFrames == 1)
                tvPhotos.setText("" + numberOfFrames + " " + getContext().getString(R.string.Photo));
            else if (numberOfFrames == 0)
                tvPhotos.setText(getContext().getString(R.string.NoPhotos));
            else
                tvPhotos.setText("" + numberOfFrames + " " + getContext().getString(R.string.Photos));
        }
        return convertView;
    }

}
