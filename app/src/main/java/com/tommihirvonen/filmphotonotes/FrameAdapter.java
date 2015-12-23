package com.tommihirvonen.filmphotonotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class FrameAdapter extends ArrayAdapter<Frame> {

    // This FrameAdapter acts as an ArrayAdapter to link an array and a list view together

    public FrameAdapter(Context context,int textViewResourceId, ArrayList<Frame> frames) {
        super(context, textViewResourceId, frames);
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
        TextView tvCount = (TextView) convertView.findViewById(R.id.tvCount);
        TextView tvFrameText = (TextView) convertView.findViewById(R.id.tvFrameText);
        TextView tvFrameText2 = (TextView) convertView.findViewById(R.id.tvFrameText2);

        // Populate the data into the template view using the data object
        tvFrameText.setText(frame.getDate());
        tvCount.setText("#" + frame.getCount());
        //              ^ a trick to show an integer in TextView
        tvFrameText2.setText(frame.getLens());
        return convertView;
    }

}
