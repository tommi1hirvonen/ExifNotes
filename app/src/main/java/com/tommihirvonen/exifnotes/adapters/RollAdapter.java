package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.List;

/**
 * LensAdapter links an ArrayList of Rolls and a ListView together.
 */
public class RollAdapter extends RecyclerView.Adapter<RollAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClicked(int position);
    }

    private List<Roll> rollList;

    private final Context context;

    private final OnItemClickListener clickListener;

    private final FilmDbHelper database;

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        TextView nameTextView;
        TextView dateTextView;
        TextView noteTextView;
        TextView photosTextView;
        TextView cameraTextView;
        ViewHolder(View itemView) {
            super(itemView);
            linearLayout = itemView.findViewById(R.id.item_roll_layout);
            nameTextView = itemView.findViewById(R.id.tv_roll_name);
            dateTextView = itemView.findViewById(R.id.tv_roll_date);
            noteTextView = itemView.findViewById(R.id.tv_roll_note);
            photosTextView = itemView.findViewById(R.id.tv_photos);
            cameraTextView = itemView.findViewById(R.id.tv_camera);
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onItemClicked(getAdapterPosition());
                }
            });
            linearLayout.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                    contextMenu.add(0, R.id.menu_item_edit, getAdapterPosition(), R.string.Edit);
                    contextMenu.add(0, R.id.menu_item_delete, getAdapterPosition(), R.string.Delete);
                    final boolean archived = rollList.get(getAdapterPosition()).getArchived();
                    if (archived) {
                        contextMenu.add(0, R.id.menu_item_activate, getAdapterPosition(), R.string.Activate);
                    } else {
                        contextMenu.add(0, R.id.menu_item_archive, getAdapterPosition(), R.string.Archive);
                    }
                }
            });
        }
    }

    public RollAdapter(Context context, List<Roll> rolls, OnItemClickListener clickListener) {
        this.context = context;
        this.rollList = rolls;
        this.clickListener = clickListener;
        this.database = FilmDbHelper.getInstance(context);
    }

    @Override
    public RollAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roll, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Roll roll = rollList.get(position);
        if (roll != null) {
            final String rollName = roll.getName();
            final String date = roll.getDate();
            final String note = roll.getNote();
            long cameraId = roll.getCameraId();
            int numberOfFrames = database.getNumberOfFrames(roll);

            // Populate the data into the template view using the data object
            holder.nameTextView.setText(rollName);
            holder.dateTextView.setText(date);
            holder.noteTextView.setText(note);
            holder.cameraTextView.setText(database.getCamera(cameraId).getMake() + " " +
                    database.getCamera(cameraId).getModel());
            if (numberOfFrames == 1)
                holder.photosTextView.setText("" + numberOfFrames + " " + context.getString(R.string.Photo));
            else if (numberOfFrames == 0)
                holder.photosTextView.setText(context.getString(R.string.NoPhotos));
            else
                holder.photosTextView.setText("" + numberOfFrames + " " + context.getString(R.string.Photos));

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
                holder.linearLayout.setBackgroundColor(0x15000000);
            }
            // If the roll is active, apply the default alphas (background alpha is 0.0).
            else {
                holder.nameTextView.setAlpha(lightFade);
                holder.dateTextView.setAlpha(noFade);
                holder.noteTextView.setAlpha(noFade);
                holder.photosTextView.setAlpha(noFade);
                holder.cameraTextView.setAlpha(noFade);
                holder.linearLayout.setBackgroundColor(0x00000000);
            }
        }
    }

    public void setRollList(List<Roll> newRollList) {
        this.rollList = newRollList;
    }

    @Override
    public int getItemCount() {
        return rollList.size();
    }

}
