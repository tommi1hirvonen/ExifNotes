/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentRollsMapBinding
import com.tommihirvonen.exifnotes.datastructures.Frame
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.datastructures.RollFilterMode
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.RollsMapViewModel
import com.tommihirvonen.exifnotes.viewmodels.RollsMapViewModelFactory
import kotlin.math.roundToInt

/**
 * Activity to display all the frames from a list of rolls on a map.
 */
class RollsMapFragment : Fragment(), OnMapReadyCallback {

    private val filterMode by lazy {
        val activity = requireActivity()
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity.baseContext)
        RollFilterMode.fromValue(
            prefs.getInt(PreferenceConstants.KEY_VISIBLE_ROLLS, RollFilterMode.ACTIVE.value))
    }

    private val model by lazy {
        val activity = requireActivity()
        val factory = RollsMapViewModelFactory(activity.application, filterMode)
        ViewModelProvider(this, factory)[RollsMapViewModel::class.java]
    }

    private var rollSelections = emptyList<Pair<Roll, Boolean>>()

    /**
     * GoogleMap object to show the map and to hold all the markers for all frames
     */
    private var googleMap: GoogleMap? = null

    private val markerList = mutableListOf<Marker>()
    private lateinit var binding: FragmentRollsMapBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireParentFragment().childFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentRollsMapBinding.inflate(layoutInflater)

        val title = when (filterMode) {
            RollFilterMode.ACTIVE -> resources.getString(R.string.ActiveRolls)
            RollFilterMode.ARCHIVED -> resources.getString(R.string.ArchivedRolls)
            RollFilterMode.ALL -> resources.getString(R.string.AllRolls)
        }
        binding.topAppBar.title = title
        binding.topAppBar.setNavigationOnClickListener {
            requireParentFragment().childFragmentManager.popBackStack()
        }
        binding.topAppBar.setOnMenuItemClickListener(onMenuItemSelected)

        // Set the bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val peekHeightOffset = resources.getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight).toFloat()
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(view: View, i: Int) {}
            override fun onSlide(view: View, v: Float) {
                val offset = binding.bottomSheet.height * v + peekHeightOffset - peekHeightOffset * v
                when (bottomSheetBehavior.state) {
                    BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                        googleMap?.setPadding(0, 0, 0, offset.roundToInt())
                    }
                    BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_EXPANDED,
                    BottomSheetBehavior.STATE_HALF_EXPANDED, BottomSheetBehavior.STATE_HIDDEN -> { }
                }
            }
        })

        // Get map type from preferences
        val sharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(requireActivity().baseContext)
        val mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL)

        val menu = binding.topAppBar.menu
        when (mapType) {
            GoogleMap.MAP_TYPE_NORMAL -> menu.findItem(R.id.menu_item_normal).isChecked = true
            GoogleMap.MAP_TYPE_HYBRID -> menu.findItem(R.id.menu_item_hybrid).isChecked = true
            GoogleMap.MAP_TYPE_SATELLITE -> menu.findItem(R.id.menu_item_satellite).isChecked = true
            GoogleMap.MAP_TYPE_TERRAIN -> menu.findItem(R.id.menu_item_terrain).isChecked = true
            else -> menu.findItem(R.id.menu_item_normal).isChecked = true
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
    }

    private val onMenuItemSelected = { item: MenuItem ->
        when (item.itemId) {
            R.id.menu_item_normal -> {
                item.isChecked = true
                setMapType(GoogleMap.MAP_TYPE_NORMAL)
                true
            }
            R.id.menu_item_hybrid -> {
                item.isChecked = true
                setMapType(GoogleMap.MAP_TYPE_HYBRID)
                true
            }
            R.id.menu_item_satellite -> {
                item.isChecked = true
                setMapType(GoogleMap.MAP_TYPE_SATELLITE)
                true
            }
            R.id.menu_item_terrain -> {
                item.isChecked = true
                setMapType(GoogleMap.MAP_TYPE_TERRAIN)
                true
            }
            R.id.menu_item_filter -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                val builder = MaterialAlertDialogBuilder(requireContext())
                val listItems = rollSelections.map { it.first.name }.toTypedArray()
                val checkedItems = rollSelections.map { it.second }.toBooleanArray()
                builder.setMultiChoiceItems(listItems, checkedItems) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                    checkedItems[which] = isChecked
                }
                builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
                builder.setPositiveButton(R.string.FilterNoColon) { _: DialogInterface?, _: Int ->
                    val newSelections = rollSelections.mapIndexed { index, roll ->
                        roll.first to checkedItems[index]
                    }.filter { it.second }.map { it.first }
                    model.setSelections(newSelections)
                }
                builder.setNeutralButton(R.string.DeselectAll, null)
                val dialog = builder.create()
                dialog.show()
                // Override the neutral button onClick listener after the dialog is shown.
                // This way the dialog isn't dismissed when the button is pressed.
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    // Deselect all items
                    val listView = dialog.listView
                    var i = 0
                    while (i < listView.count) {
                        listView.setItemChecked(i, false)
                        i++
                    }
                    checkedItems.fill(false)
                }
                true
            }
            else -> false
        }
    }

    override fun onMapReady(googleMap_: GoogleMap) {
        googleMap = googleMap_

        val peekHeightOffset = resources.getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight)
        googleMap?.setPadding(0, 0, 0, peekHeightOffset)

        // If the app's theme is dark, stylize the map with the custom night mode
        when (resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                googleMap?.setMapStyle(MapStyleOptions(resources.getString(R.string.style_json)))
            }
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        googleMap?.mapType = prefs.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL)
        googleMap?.setInfoWindowAdapter(InfoWindowAdapterMultipleRolls())
        googleMap?.setOnInfoWindowClickListener(OnInfoWindowClickListener())

        startPostponedEnterTransition()
        model.rolls.observe(viewLifecycleOwner) { rolls ->
            rollSelections = rolls.map { it.roll to it.selected }

            markerList.forEach { it.remove() }
            markerList.clear()

            val listRolls = rolls.filter { it.selected }.map { it.roll to it.marker }
            val adapter = RollMarkerAdapter(requireContext(), listRolls)
            binding.rollsListView.adapter = adapter
            adapter.notifyDataSetChanged()

            rolls.filter { it.selected }.forEach { data ->
                val bitmap = data.marker ?: return@forEach
                data.frames.forEach frames@ { frame ->
                    val location = frame.location ?: return@frames
                    val position = location.latLng ?: return@frames
                    val rollName = data.roll.name
                    val frameCount = "#" + frame.count
                    val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
                    val marker = googleMap?.addMarker(MarkerOptions()
                        .icon(bitmapDescriptor)
                        .position(position)
                        .title(rollName)
                        .snippet(frameCount)
                        .anchor(0.5f, 1.0f)) // Since we use a custom marker icon, set offset.
                        ?: return@frames
                    marker.tag = frame
                    markerList.add(marker)
                }
            }
            if (markerList.isNotEmpty()) {
                val builder = LatLngBounds.Builder()
                markerList.forEach { builder.include(it.position) }
                val bounds = builder.build()
                val width = resources.displayMetrics.widthPixels
                val height = resources.displayMetrics.heightPixels
                val padding = (width * 0.12).toInt() // offset from edges of the map 12% of screen
                // We use this command where the map's dimensions are specified.
                // This is because on some devices, the map's layout may not have yet occurred
                // (map size is 0).
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)
                googleMap?.moveCamera(cameraUpdate)
            } else {
                Toast.makeText(requireContext(), resources.getString(R.string.NoFramesToShow), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Sets the GoogleMap map type
     *
     * @param mapType One of the map type constants from class GoogleMap
     */
    private fun setMapType(mapType: Int) {
        googleMap?.mapType = mapType
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = preferences.edit()
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType)
        editor.apply()
    }

    private inner class InfoWindowAdapterMultipleRolls : InfoWindowAdapter {
        override fun getInfoWindow(marker: Marker): View? {
            return null
        }

        override fun getInfoContents(marker: Marker): View? {
            return if (marker.tag is Frame) {
                val frame = marker.tag as Frame? ?: return null
                @SuppressLint("InflateParams")
                val view = layoutInflater.inflate(R.layout.info_window_all_frames, null)
                val rollTextView = view.findViewById<TextView>(R.id.roll_name)
                val cameraTextView = view.findViewById<TextView>(R.id.camera)
                val frameCountTextView = view.findViewById<TextView>(R.id.frame_count)
                val dateTimeTextView = view.findViewById<TextView>(R.id.date_time)
                val lensTextView = view.findViewById<TextView>(R.id.lens)
                val noteTextView = view.findViewById<TextView>(R.id.note)
                rollTextView.text = frame.roll.name
                cameraTextView.text = frame.roll.camera?.name ?: getString(R.string.NoCamera)
                val frameCountText = "#" + frame.count
                frameCountTextView.text = frameCountText
                dateTimeTextView.text = frame.date?.dateTimeAsText ?: ""
                lensTextView.text = frame.lens?.name ?: getString(R.string.NoLens)
                noteTextView.text = frame.note
                view
            } else {
                null
            }
        }
    }

    private inner class OnInfoWindowClickListener : GoogleMap.OnInfoWindowClickListener {
        override fun onInfoWindowClick(marker: Marker) {
            if (marker.tag is Frame) {
                val frame = marker.tag as Frame? ?: return
                val fragment = FrameEditFragment()
                val arguments = Bundle()
                val title = "" + requireActivity().getString(R.string.EditFrame) + frame.count
                val positiveButton = requireActivity().resources.getString(R.string.OK)
                arguments.putString(ExtraKeys.TITLE, title)
                arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton)
                arguments.putParcelable(ExtraKeys.FRAME, frame)
                fragment.arguments = arguments
                requireParentFragment().childFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_right)
                    .setReorderingAllowed(true)
                    .add(R.id.rolls_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
                fragment.setFragmentResultListener("EditFrameDialog") { _, bundle ->
                    val frame1: Frame = bundle.getParcelable(ExtraKeys.FRAME)
                        ?: return@setFragmentResultListener
                    database.updateFrame(frame1)
                }
            }
        }
    }

    private class RollMarkerAdapter(
            context: Context,
            private val rolls: List<Pair<Roll, Bitmap?>>)
        : ArrayAdapter<Pair<Roll, Bitmap?>>(context, android.R.layout.simple_list_item_1, rolls) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val pair = rolls[position]
            val roll = pair.first
            val markerBitmap = pair.second
            val holder: ViewHolder
            val view = if (convertView != null) {
                holder = convertView.tag as ViewHolder
                convertView
            } else {
                val tempView = LayoutInflater.from(context).inflate(R.layout.item_roll_map_activity, parent, false)
                holder = ViewHolder()
                holder.rollNameTextView = tempView.findViewById(R.id.roll_text_view)
                holder.markerImageView = tempView.findViewById(R.id.marker_image_view)
                tempView.tag = holder
                tempView
            }
            holder.rollNameTextView.text = roll.name
            holder.markerImageView.setImageBitmap(markerBitmap)
            return view
        }

        override fun isEnabled(position: Int): Boolean {
            // Disable onClick events for the adapter. We only display the visible rolls.
            return false
        }

        private class ViewHolder {
            lateinit var rollNameTextView: TextView
            lateinit var markerImageView: ImageView
        }

    }

}