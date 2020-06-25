package com.tommihirvonen.exifnotes.adapters

import android.content.Context
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemGearBinding
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.utilities.database

/**
 * GearAdapter acts as an adapter between a List of gear and a RecyclerView.
 */
class GearAdapter(private val context: Context, var gearList: List<Gear>)
    : RecyclerView.Adapter<GearAdapter.ViewHolder>() {

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
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemGearBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Instead of short click perform long click to activate the OnCreateContextMenuListener.
            binding.itemGearLayout.setOnClickListener { obj: View -> obj.performLongClick() }
            binding.itemGearLayout.setOnCreateContextMenuListener { contextMenu: ContextMenu, _: View?, _: ContextMenuInfo? ->
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemGearBinding.inflate(inflater, parent, false))
    }

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
                mountables1 = context.database.getLinkedCameras(gear)
                mountables2 = context.database.getLinkedFilters(gear)
            }
            is Camera -> {
                mountables1 = context.database.getLinkedLenses(gear)
            }
            is Filter -> {
                mountables1 = context.database.getLinkedLenses(gear)
            }
        }
        val stringBuilder = StringBuilder()
        if (gear is FilmStock) {
            stringBuilder.append("ISO:").append("\t\t\t\t\t\t\t").append(gear.iso).append("\n")
                    .append("Type:").append("\t\t\t\t\t\t").append(gear.getTypeName(context)).append("\n")
                    .append("Process:").append("\t\t\t").append(gear.getProcessName(context))
        } else {
            stringBuilder.append(context.getString(R.string.MountsTo))
            if (mountables1.isNotEmpty()) {
                stringBuilder.append(mountables1.joinToString(separator = "\n-", prefix = "\n-") { it.name })
            }
            // If the second list of mountables is not empty, add a line change and additional mountables.
            if (mountables2.isNotEmpty()) {
                if (mountables1.isNotEmpty()) stringBuilder.append("\n")
                stringBuilder.append(mountables2.joinToString(separator = "\n-", prefix = "\n-") { it.name })
            }
        }
        holder.binding.tvGearName.text = gearName
        holder.binding.tvMountables.text = stringBuilder.toString()
    }

    override fun getItemCount(): Int = gearList.size

    override fun getItemId(position: Int): Long = gearList[position].id

}