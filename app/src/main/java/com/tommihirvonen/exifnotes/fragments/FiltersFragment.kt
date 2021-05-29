package com.tommihirvonen.exifnotes.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.GearActivity
import com.tommihirvonen.exifnotes.adapters.GearAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentFiltersBinding
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditFilterDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.database
import com.tommihirvonen.exifnotes.utilities.secondaryUiColor

/**
 * Fragment to display all filters from the database along with details
 */
class FiltersFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentFiltersBinding

    /**
     * Adapter used to adapt filterList to binding.filtersRecyclerView
     */
    private lateinit var filterAdapter: GearAdapter

    /**
     * Contains all filters from the database
     */
    private lateinit var filterList: MutableList<Filter>

    private var fragmentVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        fragmentVisible = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        fragmentVisible = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentFiltersBinding.inflate(inflater, container, false)
        filterList = database.allFilters.toMutableList()
        filterList.sort()
        binding.fabFilters.setOnClickListener(this)

        // Also change the floating action button color. Use the darker secondaryColor for this.
        binding.fabFilters.backgroundTintList = ColorStateList.valueOf(secondaryUiColor)

        val layoutManager = LinearLayoutManager(activity)
        binding.filtersRecyclerView.layoutManager = layoutManager
        binding.filtersRecyclerView.addItemDecoration(DividerItemDecoration(binding.filtersRecyclerView.context,
                layoutManager.orientation))

        // Create an ArrayAdapter for the ListView
        filterAdapter = GearAdapter(requireActivity(), filterList)

        // Set the ListView to use the ArrayAdapter
        binding.filtersRecyclerView.adapter = filterAdapter
        if (filterList.size >= 1) binding.noAddedFilters.visibility = View.GONE
        filterAdapter.notifyDataSetChanged()
        return binding.root
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab_filters) {
            val dialog = EditFilterDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewFilter))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditFilterDialog.TAG)
            dialog.setFragmentResultListener("EditFilterDialog") { _, bundle ->
                val filter: Filter = bundle.getParcelable(ExtraKeys.FILTER)
                    ?: return@setFragmentResultListener
                if (filter.make?.isNotEmpty() == true && filter.model?.isNotEmpty() == true) {
                    binding.noAddedFilters.visibility = View.GONE
                    database.addFilter(filter)
                    filterList.add(filter)
                    filterList.sort()
                    val listPos = filterList.indexOf(filter)
                    filterAdapter.notifyItemInserted(listPos)

                    // When the lens is added jump to view the last entry
                    binding.filtersRecyclerView.scrollToPosition(listPos)
                }
            }
        }
    }

    @SuppressLint("CommitTransaction")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (fragmentVisible) {

            // Use the getOrder() method to unconventionally get the clicked item's position.
            // This is set to work correctly in the Adapter class.
            val position = item.order
            val filter = filterList[position]
            when (item.itemId) {
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_LENSES -> {
                    showSelectMountableLensesDialog(position, fixedLensCameras = false)
                    return true
                }
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_CAMERAS -> {
                    showSelectMountableLensesDialog(position, fixedLensCameras = true)
                }
                GearAdapter.MENU_ITEM_DELETE -> {

                    // Check if the filter is being used with one of the rolls.
                    if (database.isFilterBeingUsed(filter)) {
                        Toast.makeText(activity, resources.getString(R.string.FilterNoColon) +
                                " " + filter.name + " " +
                                resources.getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show()
                        return true
                    }
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(resources.getString(R.string.ConfirmFilterDelete)
                            + " \'" + filter.name + "\'?"
                    )
                    builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        database.deleteFilter(filter)

                        // Remove the filter from the filterList. Do this last!
                        filterList.removeAt(position)
                        if (filterList.size == 0) binding.noAddedFilters.visibility = View.VISIBLE
                        filterAdapter.notifyItemRemoved(position)

                        // Update the LensesFragment through the parent activity.
                        val gearActivity = requireActivity() as GearActivity
                        gearActivity.updateFragments()
                    }
                    builder.create().show()
                    return true
                }

                GearAdapter.MENU_ITEM_EDIT -> {
                    val dialog = EditFilterDialog()
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditFilter))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.FILTER, filter)
                    dialog.arguments = arguments
                    dialog.show(parentFragmentManager.beginTransaction(), EditFilterDialog.TAG)
                    dialog.setFragmentResultListener("EditFilterDialog") { _, bundle ->
                        val filter1: Filter = bundle.getParcelable(ExtraKeys.FILTER)
                            ?: return@setFragmentResultListener
                        if (filter1.make?.isNotEmpty() == true && filter1.model?.isNotEmpty() == true && filter1.id > 0) {
                            database.updateFilter(filter1)
                            val oldPos = filterList.indexOf(filter1)
                            filterList.sort()
                            val newPos = filterList.indexOf(filter1)
                            filterAdapter.notifyItemChanged(oldPos)
                            filterAdapter.notifyItemMoved(oldPos, newPos)
                            binding.filtersRecyclerView.scrollToPosition(newPos)

                            // Update the LensesFragment through the parent activity.
                            val gearActivity = requireActivity() as GearActivity
                            gearActivity.updateFragments()
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    /**
     * Show dialog where the user can select which lenses can be mounted to the picked filter.
     *
     * @param position indicates the position of the picked filter in filterList
     */
    private fun showSelectMountableLensesDialog(position: Int, fixedLensCameras: Boolean) {
        val filter = filterList[position]
        val mountableLenses = database.getLinkedLenses(filter)
        val allLenses =
            if (fixedLensCameras) {
                database.getCameras(onlyFixedLensCameras = true).mapNotNull {
                    it.lens?.make = it.make
                    it.lens?.model = it.model
                    it.lens
                }
            } else {
                database.getLenses()
            }

        // Create a list where the mountable selections are saved.
        val lensSelections = allLenses.map { lens ->
            MountableState(lens, mountableLenses.any { it.id == lens.id })
        }.toMutableList()

        // Make a list of strings for all the lens names to be shown in the multi choice list.
        // If the lens is actually a fixed-lens camera, show the camera name instead.
        val listItems = allLenses.map { it.name }.toTypedArray<CharSequence>()
        // Create a bool array for preselected items in the multi choice list.
        val booleans = lensSelections.map { it.beforeState }.toBooleanArray()

        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.SelectMountableLenses)
                .setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    lensSelections[which].afterState = isChecked
                }
                .setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->

                    // Go through new mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && it.afterState }.forEach {
                        database.addLensFilterLink(filter, it.gear as Lens)
                    }

                    // Go through removed mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        database.deleteLensFilterLink(filter, it.gear as Lens)
                    }

                    filterAdapter.notifyItemChanged(position)

                    // Update the LensesFragment through the parent activity.
                    val myActivity = requireActivity() as GearActivity
                    myActivity.updateFragments()
                }
                .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Public method to update the contents of this fragment's ListView
     */
    fun updateFragment() {
        filterAdapter.notifyDataSetChanged()
    }

}