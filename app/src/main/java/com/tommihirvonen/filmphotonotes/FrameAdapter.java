package com.tommihirvonen.filmphotonotes;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Copyright 2015
// Tommi Hirvonen

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
        TextView tvShutter = (TextView) convertView.findViewById(R.id.tvShutter);
        TextView tvAperture = (TextView) convertView.findViewById(R.id.tvAperture);
        TextView tvNote = (TextView) convertView.findViewById(R.id.tv_frame_note);

        // With these commands we can color the black png images grey. Very nice! I like!
        ImageView clock = (ImageView) convertView.findViewById(R.id.drawable_clock);
        clock.getDrawable().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);
        ImageView aperture = (ImageView) convertView.findViewById(R.id.drawable_aperture);
        aperture.getDrawable().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);

        // Populate the data into the template view using the data object
        tvFrameText.setText(frame.getDate());
        tvCount.setText("" + frame.getCount());
        //              ^ a trick to show an integer in TextView
        tvFrameText2.setText(frame.getLens());
        tvNote.setText(frame.getNote());

        // If the aperture is empty, then don't show anything.
        if( !frame.getAperture().equals(getContext().getString(R.string.NoValue)) ) tvAperture.setText("f/" + frame.getAperture());
        else tvAperture.setText("");

        // If the shutter is empty, then don't show anything.
        if ( !frame.getShutter().equals(getContext().getString(R.string.NoValue)))  tvShutter.setText(frame.getShutter());
        else tvShutter.setText("");
        return convertView;
    }

}
