package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
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

import java.util.ArrayList;
import java.util.List;

/**
 * RollAdapter acts as an adapter between a List of rolls and a RecyclerView.
 */
public class RollAdapter extends RecyclerView.Adapter<RollAdapter.ViewHolder> {

    /**
     * Interface for implementing classes.
     * Used to send onItemClicked messages back to implementing Activities and/or Fragments.
     */
    public interface RollAdapterListener {

        void onItemClick(int position);

        void onItemLongClick(int position);

    }

    /**
     * Reference to the main list of Rolls received from implementing class.
     */
    private List<Roll> rollList;

    /**
     * Reference to Activity's context. Used to get resources.
     */
    private final Context context;

    /**
     * Reference to the implementing class's OnItemClickListener.
     */
    private final RollAdapterListener listener;

    /**
     * Reference to the singleton database.
     */
    private final FilmDbHelper database;

    /**
     * Used to hold the positions of selected items in the RecyclerView.
     */
    private SparseBooleanArray selectedItems;

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
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
                    listener.onItemClick(getAdapterPosition());
                }
            });
            linearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    listener.onItemLongClick(getAdapterPosition());
                    return true;
                }
            });
        }
    }

    /**
     * Constructor for this adapter.
     *
     * @param context activity's context
     * @param rolls list of rolls from the implementing class
     * @param listener implementing class's OnItemClickListener
     */
    public RollAdapter(Context context, List<Roll> rolls, RollAdapterListener listener) {
        this.context = context;
        this.rollList = rolls;
        this.listener = listener;
        this.database = FilmDbHelper.getInstance(context);
        selectedItems = new SparseBooleanArray();
    }

    /**
     * Invoked by LayoutManager to create new Views.
     *
     * @param parent view's parent ViewGroup
     * @param viewType not used
     * @return inflated view
     */
    @Override
    public RollAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roll, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Invoked by LayoutManager to replace the contents of a View.
     * Here we get the element from our dataset at the specified position
     * and set the ViewHolder views to display said elements data.
     *
     * @param holder reference to the recyclable ViewHolder
     * @param position position of the current item
     */
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
            holder.cameraTextView.setText(database.getCamera(cameraId).getName());
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

            // Set the selection status by later overriding the background color
            if (selectedItems.get(position, false)) {
                holder.linearLayout.setBackgroundColor(0x3500838F);
            }

        }
    }

    /**
     * Method to update the reference to the main list of rolls.
     *
     * @param newRollList reference to the new list of rolls
     */
    public void setRollList(List<Roll> newRollList) {
        this.rollList = newRollList;
    }

    /**
     * Method to get the item count of the FrameAdapter.
     *
     * @return the size of the main frameList
     */
    @Override
    public int getItemCount() {
        return rollList.size();
    }

    /**
     * Sets an items selection status.
     *
     * @param position position of the item
     */
    public void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    /**
     * Selects all items
     */
    public void toggleSelectionAll() {
        selectedItems.clear();
        for (int i = 0; i < getItemCount(); ++i) selectedItems.put(i, true);
        notifyItemRangeChanged(0, getItemCount());
    }

    /**
     * Clears all selections
     */
    public void clearSelections() {
        selectedItems.clear();
        notifyItemRangeChanged(0, getItemCount());
    }

    /**
     *
     * @return the number of selected items
     */
    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    /**
     *
     * @return List containing the positions of selected items.
     */
    public List<Integer> getSelectedItemPositions() {
        final List<Integer> items = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); ++i) items.add(selectedItems.keyAt(i));
        return items;
    }

}
