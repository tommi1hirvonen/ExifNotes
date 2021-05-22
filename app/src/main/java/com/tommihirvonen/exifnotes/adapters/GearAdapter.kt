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

fun List<Gear>.toStringList(): String =
    if (this.isEmpty()) "" else this.joinToString(separator = "\n-", prefix = "\n-") { it.name }

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
                val gear = gearList[bindingAdapterPosition]
                val gearName = gear.name
                contextMenu.setHeaderTitle(gearName)

                // Use the order parameter (3rd parameter) of the ContextMenu.add() method
                // to pass the position of the list item which was clicked.
                // This can be used in the implementing class to retrieve the items position.
                if (gear is Camera && gear.isNotFixedLens) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_LENSES,
                        bindingAdapterPosition, R.string.SelectMountableLenses)
                } else if (gear is Camera && gear.isFixedLens) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_FILTERS,
                        bindingAdapterPosition, R.string.SelectMountableFilters)
                } else if (gear is Filter) {
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_LENSES,
                        bindingAdapterPosition, R.string.SelectMountableLenses)
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_CAMERAS,
                        bindingAdapterPosition, R.string.SelectMountableCameras)
                } else if (gear is Lens) {
                    // If the piece of gear is a lens, add two menu items.
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_CAMERAS,
                        bindingAdapterPosition, R.string.SelectMountableCameras)
                    contextMenu.add(0, MENU_ITEM_SELECT_MOUNTABLE_FILTERS,
                        bindingAdapterPosition, R.string.SelectMountableFilters)
                }
                // Add the additional menu items common for all types of gear.
                contextMenu.add(0, MENU_ITEM_EDIT,
                    bindingAdapterPosition, R.string.Edit)
                contextMenu.add(0, MENU_ITEM_DELETE,
                    bindingAdapterPosition, R.string.Delete)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemGearBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gear = gearList[position]
        holder.binding.tvGearName.text = gear.name
        val stringBuilder = StringBuilder()
        if (gear is FilmStock) {
            stringBuilder.append("ISO:").append("\t\t\t\t\t\t\t").append(gear.iso).append("\n")
                .append("Type:").append("\t\t\t\t\t\t").append(gear.getTypeName(context)).append("\n")
                .append("Process:").append("\t\t\t").append(gear.getProcessName(context))
        } else if (gear is Camera && gear.isFixedLens) {
            gear.lens?.let {
                val filters = context.database.getLinkedFilters(it)
                stringBuilder
                    .append(context.getString(R.string.FixedLens))
                    .append(": ").append(it.name).append("\n\n")
                    .append(context.getString(R.string.FiltersNoCap)).append(":")
                    .append(filters.toStringList())
            }
        } else if (gear is Camera) {
            val lenses = context.database.getLinkedLenses(gear)
            stringBuilder.append(context.getString(R.string.LensesNoCap)).append(":")
                .append(lenses.toStringList())
        } else if (gear is Lens) {
            val cameras = context.database.getLinkedCameras(gear)
            val filters = context.database.getLinkedFilters(gear)
            stringBuilder.append(context.getString(R.string.CamerasNoCap)).append(":")
                .append(cameras.toStringList()).append("\n\n")
                .append(context.getString(R.string.FiltersNoCap)).append(":")
                .append(filters.toStringList())
        } else if (gear is Filter) {
            // Lenses, including lenses for fixed-lens cameras.
            val lensesAndCameras = context.database.getLinkedLenses(gear)
            // Map fixed-lens cameras.
            val cameras = lensesAndCameras.mapNotNull { context.database.getCameraByLensId(it.id) }
            // The rest are plain lenses.
            val lenses = lensesAndCameras.filterNot { lens -> cameras.any { camera -> lens == camera.lens } }
            stringBuilder.append(context.getString(R.string.LensesNoCap)).append(":")
                .append(lenses.toStringList()).append("\n\n")
                .append(context.getString(R.string.CamerasNoCap)).append(":")
                .append(cameras.toStringList())
        }

        holder.binding.tvMountables.text = stringBuilder.toString()
    }

    override fun getItemCount(): Int = gearList.size

    override fun getItemId(position: Int): Long = gearList[position].id

}