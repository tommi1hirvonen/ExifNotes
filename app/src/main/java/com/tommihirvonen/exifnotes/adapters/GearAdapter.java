package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Gear;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;

import java.util.Collections;
import java.util.List;

/**
 * GearAdapter acts as an adapter between a List of gear and a RecyclerView.
 */
public class GearAdapter extends RecyclerView.Adapter<GearAdapter.ViewHolder> {

    /**
     * Reference to the main list of gear received from implementing class.
     */
    private List<? extends Gear> gearList;

    /**
     * Reference to Activity's context. Used to get resources.
     */
    private final Context context;

    /**
     * Reference to the singleton database.
     */
    private final FilmDbHelper database;

    // Since we have to manually add the menu items to the context menus to pass the item position
    // to the implementing class, menu item ids are declared here instead of a menu resource file.
    // No menu resource file can be referenced here, so we use public constants instead.
    public static final int MENU_ITEM_SELECT_MOUNTABLE_LENSES = 1;
    public static final int MENU_ITEM_SELECT_MOUNTABLE_CAMERAS = 2;
    public static final int MENU_ITEM_SELECT_MOUNTABLE_FILTERS = 3;
    public static final int MENU_ITEM_EDIT = 4;
    public static final int MENU_ITEM_DELETE = 5;

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout linearLayout;
        final TextView nameTextView;
        final TextView mountablesTextView;
        ViewHolder(final View itemView) {
            super(itemView);
            linearLayout = itemView.findViewById(R.id.item_gear_layout);
            nameTextView = itemView.findViewById(R.id.tv_gear_name);
            mountablesTextView = itemView.findViewById(R.id.tv_mountables);
            // Instead of short click perform long click to activate the OnCreateContextMenuListener.
            linearLayout.setOnClickListener(View::performLongClick);
            linearLayout.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> {
                final Gear gear = gearList.get(getAdapterPosition());
                final String gearName = gear.getName();
                contextMenu.setHeaderTitle(gearName);

                // Use the order parameter (3rd parameter) of the ContextMenu.add() method
                // to pass the position of the list item which was clicked.
                // This can be used in the implementing class to retrieve the items position.
                if (gear instanceof Camera || gear instanceof Filter) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_LENSES,
                            getAdapterPosition(), R.string.SelectMountableLenses);
                // If the piece of gear is a lens, add two menu items.
                } else if (gear instanceof Lens) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_CAMERAS,
                            getAdapterPosition(), R.string.SelectMountableCameras);
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_FILTERS,
                            getAdapterPosition(), R.string.SelectMountableFilters);
                }
                // Add the additional menu items common for all types of gear.
                contextMenu.add(0, MENU_ITEM_EDIT,
                        getAdapterPosition(), R.string.Edit);
                contextMenu.add(0, MENU_ITEM_DELETE,
                        getAdapterPosition(), R.string.Delete);
            });
        }
    }

    /**
     * Constructor for this adapter,
     *
     * @param context activity's context
     * @param gearList list of gear from implementing class
     */
    public GearAdapter(final Context context, final List<? extends Gear> gearList) {
        this.gearList = gearList;
        this.context = context;
        this.database = FilmDbHelper.getInstance(context);
        // Used to make the RecyclerView perform better and to make our custom animations
        // work more reliably. Now we can use notifyDataSetChanged(), which works well
        // with possible custom animations.
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
    public GearAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gear, parent, false);
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
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Gear gear = gearList.get(position);
        if (gear != null) {
            final String gearName = gear.getName();
            List<? extends Gear> mountables1 = Collections.emptyList();
            List<? extends Gear> mountables2 = Collections.emptyList();
            // If the type of gear is lens, then get both mountable types.
            if (gear instanceof Lens) {
                mountables1 = database.getLinkedCameras((Lens) gear);
                mountables2 = database.getLinkedFilters((Lens) gear);
            } else if (gear instanceof Camera) {
                mountables1 = database.getLinkedLenses((Camera) gear);
            } else if (gear instanceof Filter) {
                mountables1 = database.getLinkedLenses((Filter) gear);
            }

            final StringBuilder stringBuilder = new StringBuilder();

            if (gear instanceof FilmStock) {

                final FilmStock filmStock = (FilmStock) gear;
                stringBuilder.append("ISO:").append("\t\t\t\t\t\t\t").append(filmStock.getIso()).append("\n")
                        .append("Type:").append("\t\t\t\t\t\t").append(filmStock.typeName(context)).append("\n")
                        .append("Process:").append("\t\t\t").append(filmStock.processName(context));

            } else {
                stringBuilder.append(context.getString(R.string.MountsTo));
                for (final Gear g : mountables1) {
                    stringBuilder.append("\n- ").append(g.getName());
                }
                // If the second list of mountables is not empty, add a line change and additional mountables.
                if (!mountables2.isEmpty()) stringBuilder.append("\n");
                // For loop not iterated, if mountables2 is empty.
                for (final Gear g : mountables2) {
                    stringBuilder.append("\n- ").append(g.getName());
                }
            }

            holder.nameTextView.setText(gearName);

            holder.mountablesTextView.setText(stringBuilder.toString());
        }
    }

    /**
     * Public setter to update reference of gearList
     *
     * @param newGearList the new list of Gear
     */
    public void setGearList(final List<? extends Gear> newGearList) {
        gearList = newGearList;
    }

    /**
     * Method to get the item count of the FrameAdapter.
     *
     * @return the size of the main frameList
     */
    @Override
    public int getItemCount() {
        return gearList.size();
    }

    /**
     * Implemented because hasStableIds has been set to true.
     *
     * @param position position of the item
     * @return stable id
     */
    @Override
    public long getItemId(final int position) {
        return gearList.get(position).getId();
    }

}