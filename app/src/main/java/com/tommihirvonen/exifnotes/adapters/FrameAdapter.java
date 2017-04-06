package com.tommihirvonen.exifnotes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.List;

/**
 * FrameAdapter acts as an ArrayAdapter to link an ArrayList and a ListView of frames together.
 */
public class FrameAdapter extends ArrayAdapter<Frame> {

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * Call super constructor and get reference to the database.
     *
     * @param context {@inheritDoc}
     * @param textViewResourceId {@inheritDoc}
     * @param frames {@inheritDoc}
     */
    public FrameAdapter(Context context,int textViewResourceId, List<Frame> frames) {
        super(context, textViewResourceId, frames);
        database = FilmDbHelper.getInstance(context);
    }

    /**
     * This function inflates a view in the ListView to display a Frame's information.
     *
     * @param position the position of the item in the list.
     * @param convertView the view to be inflated
     * @param parent the parent to which the view will eventually be attached.
     * @return the inflated view to be showed in the ListView
     */
    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public  View getView(int position, View convertView, @NonNull ViewGroup parent) {

        // TODO: IMPLEMENT ASYNCTASK TO INFLATE THE VIEW. THIS WAY WE CAN REDUCE THE LOAD ON THE MAIN THREAD FOR SMOOTHER UI TRANSITION.

        // Get the data item for this position
        Frame frame = getItem(position);

        ViewHolder holder;

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_frame_relative, parent, false);
            holder = new ViewHolder();
            holder.countTextView = (TextView) convertView.findViewById(R.id.tvCount);
            holder.frameTextView = (TextView) convertView.findViewById(R.id.tvFrameText);
            holder.frameTextView2 = (TextView) convertView.findViewById(R.id.tvFrameText2);
            holder.shutterTextView = (TextView) convertView.findViewById(R.id.tvShutter);
            holder.apertureTextView = (TextView) convertView.findViewById(R.id.tvAperture);
            holder.noteTextView = (TextView) convertView.findViewById(R.id.tv_frame_note);
            holder.clockImageView = (ImageView) convertView.findViewById(R.id.drawable_clock);
            holder.apertureImageView = (ImageView) convertView.findViewById(R.id.drawable_aperture);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // With these commands we can color the black png images grey. Very nice! I like!
        holder.clockImageView.getDrawable().mutate().setColorFilter(
                ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);
        holder.apertureImageView.getDrawable().mutate().setColorFilter(
                ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);

        // Populate the data into the template view using the data object
        if (frame != null) {
            holder.frameTextView.setText(frame.getDate());
            holder.countTextView.setText("" + frame.getCount());
            if (frame.getLensId() > 0) {
                Lens lens = database.getLens(frame.getLensId());
                holder.frameTextView2.setText(lens.getMake() + " " + lens.getModel());
            } else {
                holder.frameTextView2.setText(getContext().getString(R.string.NoLens));
            }
            holder.noteTextView.setText(frame.getNote());

            // If the apertureImageView is empty, then don't show anything.
            if (!frame.getAperture().contains("<"))
                holder.apertureTextView.setText("f/" + frame.getAperture());
            else holder.apertureTextView.setText("");

            // If the shutter is empty, then don't show anything.
            if (!frame.getShutter().contains("<")) holder.shutterTextView.setText(frame.getShutter());
            else holder.shutterTextView.setText("");
        }
        return convertView;
    }

    private static class ViewHolder{
        TextView countTextView;
        TextView frameTextView;
        TextView frameTextView2;
        TextView shutterTextView;
        TextView apertureTextView;
        TextView noteTextView;
        ImageView clockImageView;
        ImageView apertureImageView;
    }

}
