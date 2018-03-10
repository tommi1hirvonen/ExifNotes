package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * FrameAdapter acts as an adapter to link an ArrayList of Frames and a RecyclerView together.
 */
public class FrameAdapter extends RecyclerView.Adapter<FrameAdapter.ViewHolder> {

    /**
     * Interface for implementing classes.
     * Used to send onItemClicked messages back to implementing Activities and/or Fragments.
     */
    public interface FrameAdapterListener {

        void onItemClick(int position);

        void onItemLongClick(int position);

    }

    /**
     * Reference to the main list of Frames received from implementing class.
     */
    private List<Frame> frameList;

    /**
     * Reference to Activity's context. Used to get resources.
     */
    private final Context context;

    /**
     * Reference to the singleton database.
     */
    private final FilmDbHelper database;

    /**
     * The color of the frame ImageView depending on the current app theme (light or dark).
     */
    private final int backgroundFrameColor;

    /**
     * Reference to the implementing class's OnItemClickListener.
     */
    private final FrameAdapterListener listener;

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
        ConstraintLayout constraintLayout;
        TextView countTextView;
        TextView frameTextView;
        TextView frameTextView2;
        TextView shutterTextView;
        TextView apertureTextView;
        TextView noteTextView;
        ImageView frameImageView;
        ImageView clockImageView;
        ImageView apertureImageView;
        ViewHolder(View itemView) {
            super(itemView);
            constraintLayout = itemView.findViewById(R.id.item_frame_layout);
            countTextView = itemView.findViewById(R.id.tvCount);
            frameTextView = itemView.findViewById(R.id.tvFrameText);
            frameTextView2 = itemView.findViewById(R.id.tvFrameText2);
            shutterTextView = itemView.findViewById(R.id.tvShutter);
            apertureTextView = itemView.findViewById(R.id.tvAperture);
            noteTextView = itemView.findViewById(R.id.tv_frame_note);
            frameImageView = itemView.findViewById(R.id.background_frame);
            clockImageView = itemView.findViewById(R.id.drawable_clock);
            apertureImageView = itemView.findViewById(R.id.drawable_aperture);
            constraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
            constraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    listener.onItemLongClick(getAdapterPosition());
                    return true;
                }
            });
            // With these commands we can color the black png images grey. Very nice! I like!
            frameImageView.getDrawable().mutate().setColorFilter(
                    backgroundFrameColor, PorterDuff.Mode.SRC_IN
            );
            clockImageView.getDrawable().mutate().setColorFilter(
                    ContextCompat.getColor(context, R.color.grey), PorterDuff.Mode.SRC_IN
            );
            apertureImageView.getDrawable().mutate().setColorFilter(
                    ContextCompat.getColor(context, R.color.grey), PorterDuff.Mode.SRC_IN
            );
        }

    }

    /**
     * Constructor for this adapter.
     *
     * @param context implementing Activity's context
     * @param frames list of Frames from the implementing class
     * @param listener implementing class's OnItemClickListener
     */
    public FrameAdapter(Context context, List<Frame> frames,
                        FrameAdapterListener listener) {
        this.listener = listener;
        this.frameList = frames;
        this.context = context;
        this.database = FilmDbHelper.getInstance(context);
        this.backgroundFrameColor = Utilities.isAppThemeDark(context) ?
                ContextCompat.getColor(context, R.color.background_frame_dark_grey) :
                ContextCompat.getColor(context, R.color.background_frame_light_grey);
        this.selectedItems = new SparseBooleanArray();
    }

    /**
     * Invoked by LayoutManager to create new Views.
     *
     * @param parent view's parent ViewGroup
     * @param viewType not used
     * @return inflated view
     */
    @Override
    public FrameAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_frame_constraint, parent, false);
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
        Frame frame = frameList.get(position);
        if (frame != null) {
            holder.frameTextView.setText(frame.getDate());
            holder.countTextView.setText("" + frame.getCount());
            if (frame.getLensId() > 0) {
                Lens lens = database.getLens(frame.getLensId());
                holder.frameTextView2.setText(lens.getName());
            } else {
                holder.frameTextView2.setText(context.getResources().getString(R.string.NoLens));
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

        // If the frame is selected, set the background color to light grey.
        if (selectedItems.get(position, false)) {
            holder.constraintLayout.setBackgroundColor(0x20000000);
        }
        // Otherwise set it to transparent black.
        // This needs to be done, because RecyclerView recyclers its item views.
        // If it recycles a selected item, the background will be unintentionally grey.
        else {
            holder.constraintLayout.setBackgroundColor(0x00000000);
        }
    }

    /**
     * Method to get the item count of the FrameAdapter.
     *
     * @return the size of the main frameList
     */
    @Override
    public int getItemCount() {
        return frameList.size();
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
