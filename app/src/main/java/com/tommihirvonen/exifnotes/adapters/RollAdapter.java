package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
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
    private final SparseBooleanArray selectedItems;

    /**
     * Used to pass the selected item's position to onBindViewHolder(),
     * so that animations are started only when the item is actually selected.
     */
    private int currentSelectedIndex = -1;

    /**
     * Helper boolean used to indicate when all item selections are being undone.
     */
    private boolean reverseAllAnimations = false;

    /**
     * Helper boolean used to indicate when all items are being selected.
     */
    private boolean animateAll = false;

    /**
     * Helper array to keep track of animation statuses.
     */
    private final SparseBooleanArray animationItemsIndex;

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        final ConstraintLayout layout;
        final TextView nameTextView;
        final TextView dateTextView;
        final TextView noteTextView;
        final TextView photosTextView;
        final TextView cameraTextView;
        final ImageView checkBox;
        final View selectedBackground;
        ViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.item_roll_layout);
            nameTextView = itemView.findViewById(R.id.tv_roll_name);
            dateTextView = itemView.findViewById(R.id.tv_roll_date);
            noteTextView = itemView.findViewById(R.id.tv_roll_note);
            photosTextView = itemView.findViewById(R.id.tv_photos);
            cameraTextView = itemView.findViewById(R.id.tv_camera);
            checkBox = itemView.findViewById(R.id.checkbox);
            selectedBackground = itemView.findViewById(R.id.grey_background);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
            layout.setOnLongClickListener(new View.OnLongClickListener() {
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
        this.selectedItems = new SparseBooleanArray();
        this.animationItemsIndex = new SparseBooleanArray();
        // Used to make the RecyclerView perform better and to make our custom animations
        // work more reliably. Now we can use notifyDataSetChanged(), which works well
        // with our custom animations.
        setHasStableIds(true);
    }

    /**
     * Invoked by LayoutManager to create new Views.
     *
     * @param parent view's parent ViewGroup
     * @param viewType not used
     * @return inflated view
     */
    @NonNull
    @Override
    public RollAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roll_constraint, parent, false);
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

            if (cameraId > 0) holder.cameraTextView.setText(database.getCamera(cameraId).getName());
            else holder.cameraTextView.setText("No camera");

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

            // If the roll is archived, fade the text somewhat
            if (roll.getArchived()) {
                holder.nameTextView.setAlpha(heavyFade);
                holder.dateTextView.setAlpha(moderateFade);
                holder.noteTextView.setAlpha(moderateFade);
                holder.photosTextView.setAlpha(moderateFade);
                holder.cameraTextView.setAlpha(moderateFade);
            }
            // If the roll is active, apply the default alphas
            else {
                holder.nameTextView.setAlpha(lightFade);
                holder.dateTextView.setAlpha(noFade);
                holder.noteTextView.setAlpha(noFade);
                holder.photosTextView.setAlpha(noFade);
                holder.cameraTextView.setAlpha(noFade);
            }

            holder.itemView.setActivated(selectedItems.get(position, false));
            applyCheckBoxAnimation(holder, position);
        }
    }

    private void applyCheckBoxAnimation(ViewHolder holder, int position) {
        if (selectedItems.get(position, false)) {
            // First set the check box to be visible. This is the state it will be left in after
            // the animation has finished.
            holder.checkBox.setVisibility(View.VISIBLE);
            // Also set a slightly grey background to be visible.
            holder.selectedBackground.setVisibility(View.VISIBLE);

            // If the item is selected or all items are being selected and the item was not previously selected
            if (currentSelectedIndex == position || animateAll && !animationItemsIndex.get(position, false)) {
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.scale_up);
                holder.checkBox.startAnimation(animation);

                Animation animation1 = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                holder.selectedBackground.startAnimation(animation1);

                resetCurrentSelectedIndex();
            }
        } else {
            // First set the check box to be gone. This is the state it will be left in after
            // the animation has finished.
            holder.checkBox.setVisibility(View.GONE);
            // Hide the slightly grey background
            holder.selectedBackground.setVisibility(View.GONE);

            // If the item is deselected or all selections are undone and the item was previously selected
            if (currentSelectedIndex == position || reverseAllAnimations && animationItemsIndex.get(position, false)) {
                Animation animation = AnimationUtils.loadAnimation(context, R.anim.scale_down);
                holder.checkBox.startAnimation(animation);

                Animation animation1 = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                holder.selectedBackground.startAnimation(animation1);

                resetCurrentSelectedIndex();
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
        currentSelectedIndex = position;
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
            animationItemsIndex.delete(position);
        } else {
            selectedItems.put(position, true);
            animationItemsIndex.put(position, true);
        }
        notifyDataSetChanged();
    }

    /**
     * Selects all items
     */
    public void toggleSelectionAll() {
        resetCurrentSelectedIndex();
        selectedItems.clear();
        animateAll = true;
        for (int i = 0; i < getItemCount(); ++i) selectedItems.put(i, true);
        notifyDataSetChanged();
    }

    /**
     * Clears all selections
     */
    public void clearSelections() {
        reverseAllAnimations = true;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    /**
     * Called in RollsFragment after all selections have been undone.
     */
    public void resetAnimationIndex() {
        reverseAllAnimations = false;
        animationItemsIndex.clear();
    }

    /**
     * Called in RollsFragment after all items have been selected using toggleSelectionAll().
     * Sets animateAll back to false and updates animationItemsIndex to be in line with selectedItems.
     */
    public void resetAnimateAll() {
        animateAll = false;
        for (int i = 0; i < getItemCount(); ++i) {
            animationItemsIndex.put(i, true);
        }
    }

    /**
     * When the selection/deselection action has been consumed, the index of the (de)selected
     * item is reset.
     */
    private void resetCurrentSelectedIndex() {
        currentSelectedIndex = -1;
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

    /**
     * Implemented because hasStableIds has been set to true.
     *
     * @param position position of the item
     * @return stable id
     */
    @Override
    public long getItemId(int position) {
        return rollList.get(position).getId();
    }

}
