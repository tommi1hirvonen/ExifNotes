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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.entities.RollFilterMode
import com.tommihirvonen.exifnotes.core.sortableDateTime
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import com.tommihirvonen.exifnotes.data.repositories.RollRepository
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.RollData
import com.tommihirvonen.exifnotes.viewmodels.RollsMapViewModel
import com.tommihirvonen.exifnotes.viewmodels.RollsMapViewModelFactory
import com.tommihirvonen.exifnotes.viewmodels.RollsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Activity to display all the frames from a list of rolls on a map.
 */
@AndroidEntryPoint
class RollsMapFragment : Fragment(), OnMapReadyCallback {

    @Inject lateinit var rollRepository: RollRepository
    @Inject lateinit var frameRepository: FrameRepository

    private val rollsModel by activityViewModels<RollsViewModel>()
    private val filterMode by lazy { rollsModel.rollFilterMode.value }
    private val model by viewModels<RollsMapViewModel> {
        RollsMapViewModelFactory(
            requireActivity().application,
            rollRepository,
            frameRepository,
            filterMode)
    }

    private var rollSelections = emptyList<RollData>()

    /**
     * GoogleMap object to show the map and to hold all the markers for all frames
     */
    private var googleMap: GoogleMap? = null

    private val markerList = mutableListOf<Marker>()
    private lateinit var binding: FragmentRollsMapBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var fragmentRestored = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentRestored = savedInstanceState != null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentRollsMapBinding.inflate(layoutInflater)

        val title = when(val filter = filterMode) {
            is RollFilterMode.Active -> requireContext().resources.getString(R.string.ActiveRolls)
            is RollFilterMode.Archived -> requireContext().resources.getString(R.string.ArchivedRolls)
            is RollFilterMode.All -> requireContext().resources.getString(R.string.AllRolls)
            is RollFilterMode.Favorites -> requireContext().resources.getString(R.string.Favorites)
            is RollFilterMode.HasLabel -> filter.label.name
        }
        binding.topAppBar.title = title
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.topAppBar.setOnMenuItemClickListener(onMenuItemSelected)

        // Set the bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val peekHeightOffset = resources.getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight).toFloat()
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(view: View, i: Int) {}
            override fun onSlide(view: View, v: Float) {
                val orientation = resources.configuration.orientation
                val isTablet = resources.getBoolean(R.bool.isTablet)
                if (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) {
                    val offset = (binding.bottomSheet.height - peekHeightOffset) * v + peekHeightOffset
                    when (bottomSheetBehavior.state) {
                        BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                            googleMap?.setPadding(0, 0, 0, offset.roundToInt())
                        }
                        BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED, BottomSheetBehavior.STATE_HIDDEN -> { }
                    }
                }
            }
        })

        binding.fabMarkers.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.fabFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.fabMap.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.fabMap)
            popupMenu.inflate(R.menu.menu_map_fragment)
            // Get map type from preferences
            val sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(requireActivity().baseContext)
            when (sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL)) {
                GoogleMap.MAP_TYPE_NORMAL -> popupMenu.menu.findItem(R.id.menu_item_normal).isChecked = true
                GoogleMap.MAP_TYPE_HYBRID -> popupMenu.menu.findItem(R.id.menu_item_hybrid).isChecked = true
                GoogleMap.MAP_TYPE_SATELLITE -> popupMenu.menu.findItem(R.id.menu_item_satellite).isChecked = true
                GoogleMap.MAP_TYPE_TERRAIN -> popupMenu.menu.findItem(R.id.menu_item_terrain).isChecked = true
                else -> popupMenu.menu.findItem(R.id.menu_item_normal).isChecked = true
            }
            popupMenu.setOnMenuItemClickListener(onMenuItemSelected)
            popupMenu.show()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        observeThenClearNavigationResult(ExtraKeys.FRAME, frameRepository::updateFrame)
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
            else -> false
        }
    }

    private fun showFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val listItems = rollSelections.map { it.roll.name }.toTypedArray()
        val checkedItems = rollSelections.map { it.selected }.toBooleanArray()
        builder.setMultiChoiceItems(listItems, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setNegativeButton(R.string.Cancel) { _, _ -> }
        builder.setPositiveButton(R.string.FilterNoColon) { _, _ ->
            val newSelections = rollSelections.mapIndexed { index, roll ->
                roll.roll to checkedItems[index]
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
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val peekHeightOffset = resources.getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight)
        val orientation = resources.configuration.orientation
        val isTablet = resources.getBoolean(R.bool.isTablet)
        if (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) {
            googleMap?.setPadding(0, 0, 0, peekHeightOffset)
        }

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
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.rolls.collect { rolls ->
                    rollSelections = rolls

                    markerList.onEach { it.remove() }.clear()

                    val listRolls = rolls.filter(RollData::selected).map { it.roll to it.marker }
                    val adapter = RollMarkerAdapter(requireContext(), listRolls)
                    binding.rollsListView.adapter = adapter
                    adapter.notifyDataSetChanged()

                    rolls.filter(RollData::selected).forEach { data ->
                        val bitmap = data.marker ?: return@forEach
                        data.frames.forEach frames@ { frame ->
                            val position = frame.location ?: return@frames
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
                    if (!fragmentRestored) {
                        if (markerList.isNotEmpty()) {
                            val builder = LatLngBounds.Builder()
                            markerList.map(Marker::getPosition).forEach(builder::include)
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
                            binding.root.snackbar(R.string.NoFramesToShow, binding.bottomSheet)
                        }
                    }
                }
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
                dateTimeTextView.text = frame.date.sortableDateTime
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
                val title = "" + requireActivity().getString(R.string.EditFrame) + frame.count
                val action = RollsMapFragmentDirections.rollsMapFrameEditAction(frame, title, null)
                findNavController().navigate(action)
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