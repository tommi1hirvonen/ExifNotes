package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.List;

/**
 * LensAdapter links an ArrayList of Rolls and a ListView together.
 */
public class RollAdapter extends ArrayAdapter<Roll> {

    /**
     * Reference to the singleton database
     */
    private final FilmDbHelper database;

    /**
     * {@inheritDoc}
     *  @param context
     * @param rolls
     */
    public RollAdapter(Context context, List<Roll> rolls) {
        super(context, android.R.layout.simple_list_item_1, rolls);
        database = FilmDbHelper.getInstance(context);
    }

    /**
     * This function inflates a view in the ListView to display the Roll's information.
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

        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_roll, parent, false);
            holder = new ViewHolder();
            holder.nameTextView = convertView.findViewById(R.id.tv_roll_name);
            holder.dateTextView = convertView.findViewById(R.id.tv_roll_date);
            holder.noteTextView = convertView.findViewById(R.id.tv_roll_note);
            holder.photosTextView = convertView.findViewById(R.id.tv_photos);
            holder.cameraTextView = convertView.findViewById(R.id.tv_camera);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the data item for this position
        if (roll != null) {
            final String rollName = roll.getName();
            final String date = roll.getDate();
            final String note = roll.getNote();
            long cameraId = roll.getCameraId();
            int numberOfFrames = database.getNumberOfFrames(getItem(position));

            // Populate the data into the template view using the data object
            holder.nameTextView.setText(rollName);
            holder.dateTextView.setText(date);
            holder.noteTextView.setText(note);
            holder.cameraTextView.setText(database.getCamera(cameraId).getMake() + " " +
                    database.getCamera(cameraId).getModel());
            if (numberOfFrames == 1)
                holder.photosTextView.setText("" + numberOfFrames + " " + getContext().getString(R.string.Photo));
            else if (numberOfFrames == 0)
                holder.photosTextView.setText(getContext().getString(R.string.NoPhotos));
            else
                holder.photosTextView.setText("" + numberOfFrames + " " + getContext().getString(R.string.Photos));

            final float noFade = 1.0f;
            final float lightFade = 0.9f;
            final float moderateFade = 0.5f;
            final float heavyFade = 0.4f;

            // If the roll is archived, fade the text somewhat and apply a slightly grey background.
            if (roll.getArchived()) {
                holder.nameTextView.setAlpha(heavyFade);
                holder.dateTextView.setAlpha(moderateFade);
                holder.noteTextView.setAlpha(moderateFade);
                holder.photosTextView.setAlpha(moderateFade);
                holder.cameraTextView.setAlpha(moderateFade);
                convertView.setBackgroundColor(0x15000000);
            }
            // If the roll is active, apply the default alphas (background alpha is 0.0).
            else {
                holder.nameTextView.setAlpha(lightFade);
                holder.dateTextView.setAlpha(noFade);
                holder.noteTextView.setAlpha(noFade);
                holder.photosTextView.setAlpha(noFade);
                holder.cameraTextView.setAlpha(noFade);
                convertView.setBackgroundColor(0x00000000);
            }
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView dateTextView;
        TextView noteTextView;
        TextView photosTextView;
        TextView cameraTextView;
    }

}
