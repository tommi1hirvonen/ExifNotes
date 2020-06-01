package com.tommihirvonen.exifnotes.adapters

import android.animation.ValueAnimator
import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper
import java.util.*

/**
 * RollAdapter acts as an adapter between a List of rolls and a RecyclerView.
 *
 * @property context Reference to Activity's context. Used to get resources.
 * @property rollList Reference to the main list of Rolls received from implementing class.
 * @property listener Reference to the implementing class's OnItemClickListener.
 */
class RollAdapter(private val context: Context,
        private var rollList: List<Roll>,
        private val listener: RollAdapterListener) : RecyclerView.Adapter<RollAdapter.ViewHolder>() {

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
    interface RollAdapterListener {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
    }

    /**
     * Reference to the singleton database.
     */
    private val database: FilmDbHelper = FilmDbHelper.getInstance(context)

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
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val topLayout: View = itemView.findViewById(R.id.item_roll_top_layout)
        val layout: ConstraintLayout = itemView.findViewById(R.id.item_roll_layout)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_roll_name)
        val filmStockTextView: TextView = itemView.findViewById(R.id.tv_film_stock)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_roll_date)
        val noteTextView: TextView = itemView.findViewById(R.id.tv_roll_note)
        val photosTextView: TextView = itemView.findViewById(R.id.tv_photos)
        val cameraTextView: TextView = itemView.findViewById(R.id.tv_camera)
        val checkBox: ImageView = itemView.findViewById(R.id.checkbox)

        init {
            layout.setOnClickListener { listener.onItemClick(adapterPosition) }
            layout.setOnLongClickListener {
                listener.onItemLongClick(adapterPosition)
                true
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
                .inflate(R.layout.item_roll_constraint, parent, false)
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
        val roll = rollList[position]
        val cameraId = roll.cameraId
        val filmStockId = roll.filmStockId
        val numberOfFrames = database.getNumberOfFrames(roll)

        // Populate the data into the template view using the data object
        holder.nameTextView.text = roll.name
        holder.dateTextView.text = roll.date?.dateTimeAsText
        holder.noteTextView.text = roll.note

        if (filmStockId > 0) {
            holder.filmStockTextView.text = database.getFilmStock(filmStockId).name
            holder.filmStockTextView.visibility = View.VISIBLE
        } else {
            holder.filmStockTextView.text = ""
            holder.filmStockTextView.visibility = View.GONE
        }

        if (cameraId > 0) {
            holder.cameraTextView.text = database.getCamera(cameraId)?.name
        } else {
            holder.cameraTextView.text = context.resources.getString(R.string.NoCamera)
        }

        if (numberOfFrames == 0) {
            holder.photosTextView.text = context.resources.getString(R.string.NoPhotos)
        } else {
            holder.photosTextView.text = context.resources.getQuantityString(
                    R.plurals.PhotosAmount, numberOfFrames, numberOfFrames)
        }

        val noFade = 1.0f
        val lightFade = 0.9f
        val moderateFade = 0.5f
        val heavyFade = 0.4f

        // If the roll is archived, fade the text somewhat
        if (roll.archived) {
            holder.nameTextView.alpha = heavyFade
            holder.filmStockTextView.alpha = heavyFade
            holder.dateTextView.alpha = moderateFade
            holder.noteTextView.alpha = moderateFade
            holder.photosTextView.alpha = moderateFade
            holder.cameraTextView.alpha = moderateFade
        } else {
            holder.nameTextView.alpha = lightFade
            holder.filmStockTextView.alpha = lightFade
            holder.dateTextView.alpha = noFade
            holder.noteTextView.alpha = noFade
            holder.photosTextView.alpha = noFade
            holder.cameraTextView.alpha = noFade
        }
        holder.itemView.isActivated = selectedItems[position, false]
        applyCheckBoxAnimation(holder, position)
    }

    private fun applyCheckBoxAnimation(holder: ViewHolder, position: Int) {
        if (selectedItems[position, false]) {
            // First set the check box to be visible. This is the state it will be left in after
            // the animation has finished.
            holder.checkBox.visibility = View.VISIBLE

            // Also set a slightly grey background to be visible.
            holder.topLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.background_selected))

            // If the item is selected or all items are being selected and the item was not previously selected
            if (currentSelectedIndex == position || animateAll && !animationItemsIndex[position, false]) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_up)
                holder.checkBox.startAnimation(animation)
                val fromColor = ContextCompat.getColor(context, R.color.transparent)
                val toColor = ContextCompat.getColor(context, R.color.background_selected)
                val colorAnimation = ValueAnimator.ofArgb(fromColor, toColor)
                colorAnimation.duration = 500
                colorAnimation.addUpdateListener { animator: ValueAnimator ->
                    holder.topLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
                resetCurrentSelectedIndex()
            }
        } else {
            // First set the check box to be gone. This is the state it will be left in after
            // the animation has finished.
            holder.checkBox.visibility = View.GONE

            // Hide the slightly grey background
            holder.topLayout.setBackgroundResource(0)

            // If the item is deselected or all selections are undone and the item was previously selected
            if (currentSelectedIndex == position || reverseAllAnimations && animationItemsIndex[position, false]) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.scale_down)
                holder.checkBox.startAnimation(animation)
                val fromColor = ContextCompat.getColor(context, R.color.background_selected)
                val toColor = ContextCompat.getColor(context, R.color.transparent)
                val colorAnimation = ValueAnimator.ofArgb(fromColor, toColor)
                colorAnimation.duration = 500
                colorAnimation.addUpdateListener { animator: ValueAnimator ->
                    holder.topLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
                resetCurrentSelectedIndex()
            }
        }
    }

    /**
     * Method to update the reference to the main list of rolls.
     *
     * @param newRollList reference to the new list of rolls
     */
    fun setRollList(newRollList: List<Roll>) {
        rollList = newRollList
    }

    /**
     * Method to get the item count of the FrameAdapter.
     *
     * @return the size of the main frameList
     */
    override fun getItemCount(): Int {
        return rollList.size
    }

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
        for (i in 0 until itemCount) selectedItems.put(i, true)
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
     * Called in RollsFragment after all selections have been undone.
     */
    fun resetAnimationIndex() {
        reverseAllAnimations = false
        animationItemsIndex.clear()
    }

    /**
     * Called in RollsFragment after all items have been selected using toggleSelectionAll().
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
    val selectedItemPositions: List<Int> get() {
            val items: MutableList<Int> = ArrayList()
            for (i in 0 until selectedItems.size()) items.add(selectedItems.keyAt(i))
            return items
        }

    /**
     * Implemented because hasStableIds has been set to true.
     *
     * @param position position of the item
     * @return stable id
     */
    override fun getItemId(position: Int): Long {
        return rollList[position].id
    }

}