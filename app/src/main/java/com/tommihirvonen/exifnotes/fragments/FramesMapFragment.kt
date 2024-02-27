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
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentFramesMapBinding
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.sortableDateTime
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.FramesViewModel
import com.tommihirvonen.exifnotes.viewmodels.State
import com.tommihirvonen.exifnotes.viewmodels.ViewModelUtility
import kotlinx.coroutines.launch

/**
 * Activity to display all the frames from a list of rolls on a map.
 */
class FramesMapFragment : Fragment(), OnMapReadyCallback {

    private val model by navGraphViewModels<FramesViewModel>(R.id.frames_navigation)

    /**
     * GoogleMap object to show the map and to hold all the markers for all frames
     */
    private var googleMap: GoogleMap? = null

    private val markers = mutableListOf<Marker>()

    private var firstDraw = true

    private lateinit var binding: FragmentFramesMapBinding

    private var fragmentRestored = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentRestored = savedInstanceState != null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentFramesMapBinding.inflate(layoutInflater)
        binding.viewmodel = model
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.topAppBar.setOnMenuItemClickListener(onMenuItemSelected)

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
        observeThenClearNavigationResult(ExtraKeys.FRAME, model::submitFrame)
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // If the app's theme is dark, stylize the map with the custom night mode
        when (resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                googleMap?.setMapStyle(MapStyleOptions(resources.getString(R.string.style_json)))
            }
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        googleMap?.mapType = prefs.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL)
        googleMap?.setInfoWindowAdapter(InfoWindowAdapterSingleRoll())
        googleMap?.setOnInfoWindowClickListener(OnInfoWindowClickListener())

        startPostponedEnterTransition()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.frames.collect { state ->
                    if (state !is State.Success) {
                        return@collect
                    }
                    val frames = state.data
                    markers.onEach { it.remove() }.clear()
                    val bitmap = ViewModelUtility.getMarkerBitmaps(requireContext()).first()
                        ?: return@collect
                    frames.forEach frames@ { frame ->
                        val position = frame.location ?: return@frames
                        val rollName = model.roll.value.name
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
                        markers.add(marker)
                    }
                    if (!fragmentRestored) {
                        if (markers.isNotEmpty() && firstDraw) {
                            firstDraw = false
                            val builder = LatLngBounds.Builder()
                            markers.map(Marker::getPosition).forEach(builder::include)
                            val bounds = builder.build()
                            val width = resources.displayMetrics.widthPixels
                            val height = resources.displayMetrics.heightPixels
                            val padding = (width * 0.12).toInt() // offset from edges of the map 12% of screen
                            // We use this command where the map's dimensions are specified.
                            // This is because on some devices, the map's layout may not have yet occurred
                            // (map size is 0).
                            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)
                            googleMap?.moveCamera(cameraUpdate)
                        } else if (markers.isEmpty()) {
                            binding.root.snackbar(R.string.NoFramesToShow)
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

    private inner class InfoWindowAdapterSingleRoll : InfoWindowAdapter {
        override fun getInfoWindow(marker: Marker): View? {
            return null
        }

        override fun getInfoContents(marker: Marker): View? {
            return if (marker.tag is Frame) {
                val frame = marker.tag as Frame? ?: return null
                @SuppressLint("InflateParams")
                val view = layoutInflater.inflate(R.layout.info_window, null)
                val frameCountTextView = view.findViewById<TextView>(R.id.frame_count)
                val dateTimeTextView = view.findViewById<TextView>(R.id.date_time)
                val lensTextView = view.findViewById<TextView>(R.id.lens)
                val noteTextView = view.findViewById<TextView>(R.id.note)
                val frameCountText = "#" + frame.count
                frameCountTextView.text = frameCountText
                dateTimeTextView.text = frame.date.sortableDateTime
                lensTextView.text = frame.lens?.name
                    ?: if (frame.roll.camera?.isNotFixedLens == true) getString(R.string.NoLens)
                    else ""
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
                val action = FramesMapFragmentDirections.framesMapFrameEditAction(frame, title, null)
                findNavController().navigate(action)
            }
        }
    }

}