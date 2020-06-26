package com.tommihirvonen.exifnotes.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.GearActivity
import com.tommihirvonen.exifnotes.adapters.GearAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentLensesBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditLensDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities
import com.tommihirvonen.exifnotes.utilities.database

/**
 * Fragment to display all lenses from the database along with details
 */
class LensesFragment : Fragment(), View.OnClickListener {

    companion object {
        /**
         * Constant passed to EditLensDialog for result
         */
        private const val ADD_LENS = 1

        /**
         * Constant passed to EditLensDialog for result
         */
        private const val EDIT_LENS = 2
    }

    private lateinit var binding: FragmentLensesBinding

    /**
     * Adapter used to adapt lensList to binding.lensesRecyclerView
     */
    private lateinit var lensAdapter: GearAdapter

    /**
     * Contains all lenses from the database
     */
    private lateinit var lensList: MutableList<Lens>

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
                              savedInstanceState: Bundle?): View? {
        binding = FragmentLensesBinding.inflate(inflater, container, false)
        lensList = database.allLenses.toMutableList()
        lensList.sort()
        binding.fabLenses.setOnClickListener(this)
        val secondaryColor = Utilities.getSecondaryUiColor(requireActivity())

        // Also change the floating action button color. Use the darker secondaryColor for this.
        binding.fabLenses.backgroundTintList = ColorStateList.valueOf(secondaryColor)

        val layoutManager = LinearLayoutManager(activity)
        binding.lensesRecyclerView.layoutManager = layoutManager
        binding.lensesRecyclerView.addItemDecoration(DividerItemDecoration(binding.lensesRecyclerView.context,
                layoutManager.orientation))
        lensAdapter = GearAdapter(requireActivity(), lensList)
        binding.lensesRecyclerView.adapter = lensAdapter
        if (lensList.size >= 1) binding.noAddedLenses.visibility = View.GONE
        lensAdapter.notifyDataSetChanged()
        return binding.root
    }

    @SuppressLint("CommitTransaction")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (fragmentVisible) {

            // Use the getOrder() method to unconventionally get the clicked item's position.
            // This is set to work correctly in the Adapter class.
            val position = item.order
            val lens = lensList[position]
            when (item.itemId) {
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_CAMERAS -> {
                    showSelectMountableCamerasDialog(position)
                    return true
                }
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_FILTERS -> {
                    showSelectMountableFiltersDialog(position)
                    return true
                }
                GearAdapter.MENU_ITEM_DELETE -> {

                    // Check if the lens is being used with one of the frames.
                    if (database.isLensInUse(lens)) {
                        Toast.makeText(activity, resources.getString(R.string.LensNoColon) +
                                " " + lens.name + " " +
                                resources.getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show()
                        return true
                    }
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(resources.getString(R.string.ConfirmLensDelete)
                            + " \'" + lens.name + "\'?"
                    )
                    builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        database.deleteLens(lens)

                        // Remove the lens from the lensList. Do this last!
                        lensList.removeAt(position)
                        if (lensList.size == 0) binding.noAddedLenses.visibility = View.VISIBLE
                        lensAdapter.notifyItemRemoved(position)

                        // Update the CamerasFragment through the parent activity.
                        val gearActivity = requireActivity() as GearActivity
                        gearActivity.updateFragments()
                    }
                    builder.create().show()
                    return true
                }
                GearAdapter.MENU_ITEM_EDIT -> {
                    val dialog = EditLensDialog()
                    dialog.setTargetFragment(this, EDIT_LENS)
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditLens))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.LENS, lens)
                    arguments.putInt(ExtraKeys.POSITION, position)
                    dialog.arguments = arguments
                    dialog.show(parentFragmentManager.beginTransaction(), EditLensDialog.TAG)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Show EditLensDialog to add a new lens to the database
     */
    @SuppressLint("CommitTransaction")
    private fun showLensNameDialog() {
        val dialog = EditLensDialog()
        dialog.setTargetFragment(this, ADD_LENS)
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewLens))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditLensDialog.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ADD_LENS -> if (resultCode == Activity.RESULT_OK) {
                // After Ok code.
                val lens: Lens = data?.getParcelableExtra(ExtraKeys.LENS) ?: return
                if (lens.make?.isNotEmpty() == true && lens.model?.isNotEmpty() == true) {
                    binding.noAddedLenses.visibility = View.GONE
                    lens.id = database.addLens(lens)
                    lensList.add(lens)
                    lensList.sort()
                    val listPos = lensList.indexOf(lens)
                    lensAdapter.notifyItemInserted(listPos)

                    // When the lens is added jump to view the last entry
                    binding.lensesRecyclerView.scrollToPosition(listPos)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // After Cancel code.
                // Do nothing.
                return
            }
            EDIT_LENS -> if (resultCode == Activity.RESULT_OK) {
                val lens: Lens = data?.getParcelableExtra(ExtraKeys.LENS) ?: return
                if (lens.make?.isNotEmpty() == true && lens.model?.isNotEmpty() == true && lens.id > 0) {
                    database.updateLens(lens)
                    val oldPos = lensList.indexOf(lens)
                    lensList.sort()
                    val newPos = lensList.indexOf(lens)
                    lensAdapter.notifyItemChanged(oldPos)
                    lensAdapter.notifyItemMoved(oldPos, newPos)
                    binding.lensesRecyclerView.scrollToPosition(newPos)

                    // Update the LensesFragment through the parent activity.
                    val gearActivity = requireActivity() as GearActivity
                    gearActivity.updateFragments()
                } else {
                    Toast.makeText(activity, "Something went wrong :(", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                return
            }
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab_lenses) {
            showLensNameDialog()
        }
    }

    /**
     * Show dialog where the user can select which cameras can be mounted to the picked lens.
     *
     * @param position indicates the position of the picked lens in lensList
     */
    private fun showSelectMountableCamerasDialog(position: Int) {
        val lens = lensList[position]
        val mountableCameras = database.getLinkedCameras(lens)
        val allCameras = database.allCameras

        // Create a list where the mountable selections are saved.
        val cameraSelections = allCameras.map {
            MountableState(it, mountableCameras.contains(it))
        }.toMutableList()

        // Make a list of strings for all the camera names to be shown in the multi choice list.
        val listItems = allCameras.map { it.name }.toTypedArray<CharSequence>()
        // Create a bool array for preselected items in the multi choice list.
        val booleans = cameraSelections.map { it.beforeState }.toBooleanArray()

        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.SelectMountableCameras)
                .setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    cameraSelections[which].afterState = isChecked
                }
                .setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->

                    // Go through new mountable combinations.
                    cameraSelections.filter { it.afterState != it.beforeState && it.afterState }.forEach {
                        database.addCameraLensLink(it.gear as Camera, lens)
                    }

                    // Go through removed mountable combinations.
                    cameraSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        database.deleteCameraLensLink(it.gear as Camera, lens)
                    }

                    lensAdapter.notifyItemChanged(position)

                    // Update the CamerasFragment through the parent activity.
                    val gearActivity = requireActivity() as GearActivity
                    gearActivity.updateFragments()
                }
                .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Show dialog where the user can select which filters can be mounted to the picked lens.
     *
     * @param position indicates the position of the picked lens in lensList
     */
    private fun showSelectMountableFiltersDialog(position: Int) {
        val lens = lensList[position]
        val mountableFilters = database.getLinkedFilters(lens)
        val allFilters = database.allFilters

        // Create a list where the mountable selections are saved.
        val filterSelections = allFilters.map {
            MountableState(it, mountableFilters.contains(it))
        }.toMutableList()

        // Make a list of strings for all the lens names to be shown in the multi choice list.
        val listItems = allFilters.map { it.name }.toTypedArray<CharSequence>()
        // Create a bool array for preselected items in the multi choice list.
        val booleans = filterSelections.map { it.beforeState }.toBooleanArray()

        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.SelectMountableFilters)
                .setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    filterSelections[which].afterState = isChecked
                }
                .setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->

                    // Go through new mountable combinations.
                    filterSelections.filter { it.afterState != it.beforeState && it.afterState }.forEach {
                        database.addLensFilterLink(it.gear as Filter, lens)
                    }

                    // Go through removed mountable combinations.
                    filterSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        database.deleteLensFilterLink(it.gear as Filter, lens)
                    }

                    lensAdapter.notifyItemChanged(position)

                    // Update the FiltersFragment through the parent activity.
                    val gearActivity = requireActivity() as GearActivity
                    gearActivity.updateFragments()
                }
                .setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    /**
     * Public method to update the contents of this fragment's ListView
     */
    fun updateFragment() {
        lensAdapter.notifyDataSetChanged()
    }

}