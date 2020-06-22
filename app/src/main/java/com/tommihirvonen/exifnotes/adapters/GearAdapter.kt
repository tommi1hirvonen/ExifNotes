package com.tommihirvonen.exifnotes.adapters

import android.content.Context
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper

/**
 * GearAdapter acts as an adapter between a List of gear and a RecyclerView.
 */
class GearAdapter(
        private val context: Context,
        var gearList: List<Gear>) : RecyclerView.Adapter<GearAdapter.ViewHolder>() {

    companion object {
        // Since we have to manually add the menu items to the context menus to pass the item position
        // to the implementing class, menu item ids are declared here instead of a menu resource file.
        // No menu resource file can be referenced here, so we use public constants instead.
        const val MENU_ITEM_SELECT_MOUNTABLE_LENSES = 1
        const val MENU_ITEM_SELECT_MOUNTABLE_CAMERAS = 2
        const val MENU_ITEM_SELECT_MOUNTABLE_FILTERS = 3
        const val MENU_ITEM_EDIT = 4
        const val MENU_ITEM_DELETE = 5
    }

    init {
        // Used to make the RecyclerView perform better and to make our custom animations
        // work more reliably. Now we can use notifyDataSetChanged(), which works well
        // with possible custom animations.
        setHasStableIds(true)
    }

    /**
     * Reference to the singleton database.
     */
    private val database: FilmDbHelper = FilmDbHelper.getInstance(context)

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val linearLayout: LinearLayout = itemView.findViewById(R.id.item_gear_layout)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_gear_name)
        val mountablesTextView: TextView = itemView.findViewById(R.id.tv_mountables)
        init {
            // Instead of short click perform long click to activate the OnCreateContextMenuListener.
            linearLayout.setOnClickListener { obj: View -> obj.performLongClick() }
            linearLayout.setOnCreateContextMenuListener { contextMenu: ContextMenu, _: View?, _: ContextMenuInfo? ->
                val gear = gearList[adapterPosition]
                val gearName = gear.name
                contextMenu.setHeaderTitle(gearName)

                // Use the order parameter (3rd parameter) of the ContextMenu.add() method
                // to pass the position of the list item which was clicked.
                // This can be used in the implementing class to retrieve the items position.
                if (gear is Camera || gear is Filter) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_LENSES,
                            adapterPosition, R.string.SelectMountableLenses)
                    // If the piece of gear is a lens, add two menu items.
                } else if (gear is Lens) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_CAMERAS,
                            adapterPosition, R.string.SelectMountableCameras)
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_FILTERS,
                            adapterPosition, R.string.SelectMountableFilters)
                }
                // Add the additional menu items common for all types of gear.
                contextMenu.add(0, MENU_ITEM_EDIT,
                        adapterPosition, R.string.Edit)
                contextMenu.add(0, MENU_ITEM_DELETE,
                        adapterPosition, R.string.Delete)
            }
        }
    }

    /**
     * Invoked by LayoutManager to create new Views.
     *
     * @param parent view's parent ViewGroup
     * @param viewType not used
     * @return inflated view
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gear, parent, false)
        return ViewHolder(view)
    }

    /**
     * Invoked by LayoutManager to replace the contents of a View.
     * Here we get the element from our dataset at the specified position
     * and set the ViewHolder views to display said elements data.
     *
     * @param holder reference to the recyclable ViewHolder
     * @param position position of the current item
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gear = gearList[position]
        val gearName = gear.name
        var mountables1: List<Gear> = emptyList()
        var mountables2: List<Gear> = emptyList()
        // If the type of gear is lens, then get both mountable types.
        // If the second list of mountables is not empty, add a line change and additional mountables.
        // For loop not iterated, if mountables2 is empty.
        when (gear) {
            is Lens -> {
                mountables1 = database.getLinkedCameras(gear)
                mountables2 = database.getLinkedFilters(gear)
            }
            is Camera -> {
                mountables1 = database.getLinkedLenses(gear)
            }
            is Filter -> {
                mountables1 = database.getLinkedLenses(gear)
            }
        }
        val stringBuilder = StringBuilder()
        if (gear is FilmStock) {
            stringBuilder.append("ISO:").append("\t\t\t\t\t\t\t").append(gear.iso).append("\n")
                    .append("Type:").append("\t\t\t\t\t\t").append(gear.getTypeName(context)).append("\n")
                    .append("Process:").append("\t\t\t").append(gear.getProcessName(context))
        } else {
            stringBuilder.append(context.getString(R.string.MountsTo))
            mountables1.forEach { stringBuilder.append("\n- ").append(it.name) }
            // If the second list of mountables is not empty, add a line change and additional mountables.
            if (mountables2.isNotEmpty()) stringBuilder.append("\n")
            mountables2.forEach { stringBuilder.append("\n- ").append(it.name) }
        }
        holder.nameTextView.text = gearName
        holder.mountablesTextView.text = stringBuilder.toString()
    }

    /**
     * Method to get the item count of the FrameAdapter.
     *
     * @return the size of the main frameList
     */
    override fun getItemCount(): Int {
        return gearList.size
    }

    /**
     * Implemented because hasStableIds has been set to true.
     *
     * @param position position of the item
     * @return stable id
     */
    override fun getItemId(position: Int): Long {
        return gearList[position].id
    }

}