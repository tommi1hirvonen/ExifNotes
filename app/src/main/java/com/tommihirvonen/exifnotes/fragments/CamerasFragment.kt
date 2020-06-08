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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.GearActivity
import com.tommihirvonen.exifnotes.adapters.GearAdapter
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.MountableState
import com.tommihirvonen.exifnotes.dialogs.EditCameraDialog
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper
import com.tommihirvonen.exifnotes.utilities.Utilities

/**
 * Fragment to display all cameras from the database along with details
 */
class CamerasFragment : Fragment(), View.OnClickListener {

    companion object {
        /**
         * Constant passed to EditCameraDialog for result
         */
        private const val ADD_CAMERA = 1

        /**
         * Constant passed to EditCameraDialog for result
         */
        private const val EDIT_CAMERA = 2
    }

    /**
     * TextView to show that no cameras have been added to the database
     */
    private lateinit var mainTextView: TextView

    /**
     * ListView to show all the cameras in the database along with details
     */
    private lateinit var mainRecyclerView: RecyclerView

    /**
     * Adapter used to adapt cameraList to mainRecyclerView
     */
    private lateinit var cameraAdapter: GearAdapter

    /**
     * Contains all cameras from the database
     */
    private lateinit var cameraList: MutableList<Camera>

    /**
     * Reference to the singleton database
     */
    private lateinit var database: FilmDbHelper

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
        val layoutInflater = requireActivity().layoutInflater
        database = FilmDbHelper.getInstance(activity)
        cameraList = database.allCameras
        cameraList.sort()
        val view = layoutInflater.inflate(R.layout.fragment_cameras, container, false)
        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.fab_cameras)
        floatingActionButton.setOnClickListener(this)
        val secondaryColor = Utilities.getSecondaryUiColor(activity)

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(secondaryColor)
        mainTextView = view.findViewById(R.id.no_added_cameras)

        // Access the ListView
        mainRecyclerView = view.findViewById(R.id.cameras_recycler_view)
        val layoutManager = LinearLayoutManager(activity)
        mainRecyclerView.layoutManager = layoutManager
        mainRecyclerView.addItemDecoration(DividerItemDecoration(mainRecyclerView.context,
                layoutManager.orientation))

        // Create an ArrayAdapter for the ListView
        cameraAdapter = GearAdapter(activity, cameraList)

        // Set the ListView to use the ArrayAdapter
        mainRecyclerView.adapter = cameraAdapter
        if (cameraList.size >= 1) mainTextView.visibility = View.GONE
        cameraAdapter.notifyDataSetChanged()
        return view
    }

    @SuppressLint("CommitTransaction")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (fragmentVisible) {

            // Use the getOrder() method to unconventionally get the clicked item's position.
            // This is set to work correctly in the Adapter class.
            val position = item.order
            val camera = cameraList[position]
            when (item.itemId) {
                GearAdapter.MENU_ITEM_SELECT_MOUNTABLE_LENSES -> {
                    showSelectMountableLensesDialog(position)
                    return true
                }
                GearAdapter.MENU_ITEM_DELETE -> {

                    // Check if the camera is being used with one of the rolls.
                    if (database.isCameraBeingUsed(camera)) {
                        Toast.makeText(activity, resources.getString(R.string.CameraNoColon) +
                                " " + camera.name + " " +
                                resources.getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show()
                        return true
                    }
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(resources.getString(R.string.ConfirmCameraDelete)
                            + " \'" + camera.name + "\'?"
                    )
                    builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                    builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                        database.deleteCamera(camera)

                        // Remove the camera from the cameraList. Do this last!
                        cameraList.removeAt(position)
                        if (cameraList.size == 0) mainTextView.visibility = View.VISIBLE
                        cameraAdapter.notifyItemRemoved(position)

                        // Update the LensesFragment through the parent activity.
                        val gearActivity = requireActivity() as GearActivity
                        gearActivity.updateFragments()
                    }
                    builder.create().show()
                    return true
                }
                GearAdapter.MENU_ITEM_EDIT -> {
                    val dialog = EditCameraDialog()
                    dialog.setTargetFragment(this, EDIT_CAMERA)
                    val arguments = Bundle()
                    arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.EditCamera))
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.OK))
                    arguments.putParcelable(ExtraKeys.CAMERA, camera)
                    dialog.arguments = arguments
                    dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Show EditCameraDialog to add a new camera to the database
     */
    @SuppressLint("CommitTransaction")
    private fun showCameraNameDialog() {
        val dialog = EditCameraDialog()
        dialog.setTargetFragment(this, ADD_CAMERA)
        val arguments = Bundle()
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewCamera))
        arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
        dialog.arguments = arguments
        dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab_cameras) {
            showCameraNameDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ADD_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                // After Ok code.
                val camera: Camera = data?.getParcelableExtra(ExtraKeys.CAMERA) ?: return
                if (camera.make?.isNotEmpty() == true && camera.model?.isNotEmpty() == true) {
                    mainTextView.visibility = View.GONE
                    camera.id = database.addCamera(camera)
                    cameraList.add(camera)
                    Utilities.sortGearList(cameraList)
                    val listPos = cameraList.indexOf(camera)
                    cameraAdapter.notifyItemInserted(listPos)

                    // When the lens is added jump to view the last entry
                    mainRecyclerView.scrollToPosition(listPos)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // After Cancel code.
                // Do nothing.
                return
            }
            EDIT_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                val camera: Camera = data?.getParcelableExtra(ExtraKeys.CAMERA) ?: return
                if (camera.make?.isNotEmpty() == true && camera.model?.isNotEmpty() == true && camera.id > 0) {
                    database.updateCamera(camera)
                    val oldPos = cameraList.indexOf(camera)
                    cameraList.sort()
                    val newPos = cameraList.indexOf(camera)
                    cameraAdapter.notifyItemChanged(oldPos)
                    cameraAdapter.notifyItemMoved(oldPos, newPos)
                    mainRecyclerView.scrollToPosition(newPos)

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

    /**
     * Show dialog where the user can select which lenses can be mounted to the picked camera.
     *
     * @param position indicates the position of the picked camera in cameraList
     */
    private fun showSelectMountableLensesDialog(position: Int) {
        val camera = cameraList[position]
        val mountableLenses = database.getLinkedLenses(camera)
        val allLenses = database.allLenses

        // Create a list where the mountable selections are saved.
        val lensSelections = allLenses.map {
            MountableState(it, mountableLenses.contains(it))
        }.toMutableList()

        // Make a list of strings for all the lens names to be shown in the multi choice list.
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
                        database.addCameraLensLink(camera, it.gear as Lens)
                    }

                    // Go through removed mountable combinations.
                    lensSelections.filter { it.afterState != it.beforeState && !it.afterState }.forEach {
                        database.deleteCameraLensLink(camera, it.gear as Lens)
                    }

                    cameraAdapter.notifyItemChanged(position)

                    // Update the LensesFragment through the parent activity.
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
        cameraAdapter.notifyDataSetChanged()
    }

}