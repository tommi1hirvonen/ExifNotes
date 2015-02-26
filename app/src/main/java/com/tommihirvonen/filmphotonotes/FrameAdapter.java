package com.tommihirvonen.filmphotonotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class FrameAdapter extends ArrayAdapter<Frame> {

    public FrameAdapter(Context context, ArrayList<Frame> frames) {
        super(context, 0, frames);
    }

    @Override
    public  View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Frame frame = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if ( convertView == null ) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_frame, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);

        // Populate the data into the template view using the data object
        tvName.setText(frame.count + "   " + frame.date + "   " + frame.lens);
        return convertView;
    }

}
