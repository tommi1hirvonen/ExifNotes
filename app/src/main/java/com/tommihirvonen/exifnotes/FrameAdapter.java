package com.tommihirvonen.exifnotes;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

// Copyright 2015
// Tommi Hirvonen

public class FrameAdapter extends ArrayAdapter<Frame> {

    // This FrameAdapter acts as an ArrayAdapter to link an array and a list view together

    public FrameAdapter(Context context,int textViewResourceId, ArrayList<Frame> frames) {
        super(context, textViewResourceId, frames);
        database = new FilmDbHelper(context);
    }

    FilmDbHelper database;

    @Override
    public  View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Frame frame = getItem(position);

        ViewHolder holder;

        // Check if an existing view is being reused, otherwise inflate the view
        if ( convertView == null ) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_frame, parent, false);
            holder = new ViewHolder();
            holder.tvCount = (TextView) convertView.findViewById(R.id.tvCount);
            holder.tvFrameText = (TextView) convertView.findViewById(R.id.tvFrameText);
            holder.tvFrameText2 = (TextView) convertView.findViewById(R.id.tvFrameText2);
            holder.tvShutter = (TextView) convertView.findViewById(R.id.tvShutter);
            holder.tvAperture = (TextView) convertView.findViewById(R.id.tvAperture);
            holder.tvNote = (TextView) convertView.findViewById(R.id.tv_frame_note);
            holder.clock = (ImageView) convertView.findViewById(R.id.drawable_clock);
            holder.aperture = (ImageView) convertView.findViewById(R.id.drawable_aperture);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
//        // Lookup view for data population
//        TextView tvCount = (TextView) convertView.findViewById(R.id.tvCount);
//        TextView tvFrameText = (TextView) convertView.findViewById(R.id.tvFrameText);
//        TextView tvFrameText2 = (TextView) convertView.findViewById(R.id.tvFrameText2);
//        TextView tvShutter = (TextView) convertView.findViewById(R.id.tvShutter);
//        TextView tvAperture = (TextView) convertView.findViewById(R.id.tvAperture);
//        TextView tvNote = (TextView) convertView.findViewById(R.id.tv_frame_note);

        // With these commands we can color the black png images grey. Very nice! I like!
//        ImageView clock = (ImageView) convertView.findViewById(R.id.drawable_clock);
        holder.clock.getDrawable().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);
//        ImageView aperture = (ImageView) convertView.findViewById(R.id.drawable_aperture);
        holder.aperture.getDrawable().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);

        // Populate the data into the template view using the data object
        holder.tvFrameText.setText(frame.getDate());
        holder.tvCount.setText("" + frame.getCount());
        if ( frame.getLensId() != -1 ) holder.tvFrameText2.setText( database.getLens(frame.getLensId()).getMake() + " " + database.getLens(frame.getLensId()).getModel() );
        else holder.tvFrameText2.setText(getContext().getString(R.string.NoLens));
        holder.tvNote.setText(frame.getNote());

        // If the aperture is empty, then don't show anything.
        if( !frame.getAperture().equals(getContext().getString(R.string.NoValue)) ) holder.tvAperture.setText("f/" + frame.getAperture());
        else holder.tvAperture.setText("");

        // If the shutter is empty, then don't show anything.
        if ( !frame.getShutter().equals(getContext().getString(R.string.NoValue)))  holder.tvShutter.setText(frame.getShutter());
        else holder.tvShutter.setText("");
        return convertView;
    }

    static class ViewHolder{
        TextView tvCount;
        TextView tvFrameText;
        TextView tvFrameText2;
        TextView tvShutter;
        TextView tvAperture;
        TextView tvNote;
        ImageView clock;
        ImageView aperture;
    }

}
