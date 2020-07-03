package com.tommihirvonen.exifnotes.adapters

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemFrameConstraintBinding
import com.tommihirvonen.exifnotes.datastructures.Frame
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager

/**
 * FrameAdapter acts as an adapter to link an ArrayList of Frames and a RecyclerView together.
 *
 * @property context Reference to Activity's context. Used to get resources.
 * @property frameList Reference to the main list of Frames received from implementing class.
 * @property listener Reference to the implementing class's OnItemClickListener.
 */
class FrameAdapter(private val context: Context,
        private val frameList: List<Frame>,
        private val listener: FrameAdapterListener) : RecyclerView.Adapter<FrameAdapter.ViewHolder>() {

    init {
        // Used to make the RecyclerView perform better and to make our custom animations
        // work more reliably. Now we can use notifyDataSetChanged(), which works well
        // with our custom animations.
        setHasStableIds(true)
    }

    /**
     * Interface for implementing classes.
     * Used to send onItemClicked messages back to implementing Activities and/or Fragments.
     */
    interface FrameAdapterListener {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
    }

    /**
     * Used to hold the positions of selected items in the RecyclerView.
     */
    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    /**
     * Used to pass the selected item's position to onBindViewHolder(),
     * so that animations are started only when the item is actually selected.
     */
    private var currentSelectedIndex = -1

    /**
     * Helper boolean used to indicate when all item selections are being undone.
     */
    private var reverseAllAnimations = false

    /**
     * Helper boolean used to indicate when all items are being selected.
     */
    private var animateAll = false

    /**
     * Helper array to keep track of animation statuses.
     */
    private val animationItemsIndex: SparseBooleanArray = SparseBooleanArray()

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemFrameConstraintBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemFrameLayout.setOnClickListener { listener.onItemClick(adapterPosition) }
            binding.itemFrameLayout.setOnLongClickListener {
                listener.onItemLongClick(adapterPosition)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemFrameConstraintBinding.inflate(inflater, parent, false))

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val frame = frameList[position]
        holder.binding.tvFrameText.text = frame.date?.dateTimeAsText
        holder.binding.tvCount.text = "${frame.count}"
        holder.binding.tvFrameText2.text = frame.lens?.name ?: context.resources.getString(R.string.NoLens)
        holder.binding.tvFrameNote.text = frame.note
        holder.binding.tvAperture.text = frame.aperture?.let { "f/$it" } ?: ""
        holder.binding.tvShutter.text = frame.shutter

        holder.binding.pictureImageView.visibility = View.INVISIBLE
        holder.binding.brokenPictureImageView.visibility = View.INVISIBLE
        frame.pictureFilename?.let {
            val pictureFile = ComplementaryPicturesManager.getPictureFile(context, it)
            if (pictureFile.exists()) holder.binding.pictureImageView.visibility = View.VISIBLE
            else holder.binding.brokenPictureImageView.visibility = View.VISIBLE
        }

        holder.itemView.isActivated = selectedItems[position, false]
        applyCheckBoxAnimation(holder, position)
    }

    /**
     * Applies check box animations when an item is selected or deselected.
     *
     * @param holder reference to the item's holder
     * @param position position of the item
     */
    private fun applyCheckBoxAnimation(holder: ViewHolder, position: Int) {
        if (selectedItems[position, false]) {
            // First set the check box to be visible. This is the state it will be left in after
            // the animation has finished.
            holder.binding.checkbox.visibility = View.VISIBLE
            // Also set a slightly grey background to be visible.
            holder.binding.greyBackground.visibility = View.VISIBLE

            // If the item is selected or all items are being selected and the item was not previously selected
            if (currentSelectedIndex == position || animateAll && !animationItemsIndex[position, false]) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_up)
                holder.binding.checkbox.startAnimation(animation)
                val animation1 = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                holder.binding.greyBackground.startAnimation(animation1)
                resetCurrentSelectedIndex()
            }
        } else {
            // First set the check box to be gone. This is the state it will be left in after
            // the animation has finished.
            holder.binding.checkbox.visibility = View.GONE
            // Hide the slightly grey background
            holder.binding.greyBackground.visibility = View.GONE

            // If the item is deselected or all selections are undone and the item was previously selected
            if (currentSelectedIndex == position || reverseAllAnimations && animationItemsIndex[position, false]) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_down)
                holder.binding.checkbox.startAnimation(animation)
                val animation1 = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                holder.binding.greyBackground.startAnimation(animation1)
                resetCurrentSelectedIndex()
            }
        }
    }

    override fun getItemCount(): Int = frameList.size

    /**
     * Sets an items selection status.
     *
     * @param position position of the item
     */
    fun toggleSelection(position: Int) {
        currentSelectedIndex = position
        if (selectedItems[position, false]) {
            selectedItems.delete(position)
            animationItemsIndex.delete(position)
        } else {
            selectedItems.put(position, true)
            animationItemsIndex.put(position, true)
        }
        notifyDataSetChanged()
    }

    /**
     * Selects all items
     */
    fun toggleSelectionAll() {
        resetCurrentSelectedIndex()
        selectedItems.clear()
        animateAll = true
        for (i in 0 until itemCount) {
            selectedItems.put(i, true)
        }
        notifyDataSetChanged()
    }

    /**
     * Clears all selections
     */
    fun clearSelections() {
        reverseAllAnimations = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    /**
     * Called in FramesFragment after all selections have been undone.
     */
    fun resetAnimationIndex() {
        reverseAllAnimations = false
        animationItemsIndex.clear()
    }

    /**
     * Called in FramesFragment after all items have been selected using toggleSelectionAll().
     * Sets animateAll back to false and updates animationItemsIndex to be in line with selectedItems.
     */
    fun resetAnimateAll() {
        animateAll = false
        for (i in 0 until itemCount) {
            animationItemsIndex.put(i, true)
        }
    }

    /**
     * When the selection/deselection action has been consumed, the index of the (de)selected
     * item is reset.
     */
    private fun resetCurrentSelectedIndex() {
        currentSelectedIndex = -1
    }

    /**
     *
     * @return the number of selected items
     */
    val selectedItemCount: Int get() = selectedItems.size()

    /**
     *
     * @return List containing the positions of selected items.
     */
    val selectedItemPositions: List<Int>
        get() {
            val items: MutableList<Int> = ArrayList()
            for (i in 0 until selectedItems.size()) items.add(selectedItems.keyAt(i))
            return items
        }

    // Implemented because hasStableIds has been set to true.
    override fun getItemId(position: Int): Long = frameList[position].id

}