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
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentFramesMapBinding
import com.tommihirvonen.exifnotes.datastructures.Frame
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.FramesViewModel
import com.tommihirvonen.exifnotes.viewmodels.ViewModelUtility

/**
 * Activity to display all the frames from a list of rolls on a map.
 */
class FramesMapFragment : Fragment(), OnMapReadyCallback {

    // The ViewModel has been instantiated using a factory by the parent fragment.
    private val model by viewModels<FramesViewModel>(ownerProducer = { requireParentFragment() })

    /**
     * GoogleMap object to show the map and to hold all the markers for all frames
     */
    private var googleMap: GoogleMap? = null

    private val markers = mutableListOf<Marker>()

    private var firstDraw = true

    private lateinit var binding: FragmentFramesMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireParentFragment().childFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentFramesMapBinding.inflate(layoutInflater)

        binding.topAppBar.title = model.roll.name
        binding.topAppBar.setNavigationOnClickListener {
            requireParentFragment().childFragmentManager.popBackStack()
        }
        binding.topAppBar.setOnMenuItemClickListener(onMenuItemSelected)

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
            else -> false
        }
    }

    override fun onMapReady(googleMap_: GoogleMap) {
        googleMap = googleMap_

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

        model.frames.observe(viewLifecycleOwner) { frames ->
            markers.forEach { it.remove() }
            markers.clear()

            val bitmap = ViewModelUtility.getMarkerBitmaps(requireContext()).first()
                ?: return@observe
            frames.forEach frames@ { frame ->
                val location = frame.location ?: return@frames
                val position = location.latLng ?: return@frames
                val rollName = model.roll.name
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
            if (markers.isNotEmpty() && firstDraw) {
                firstDraw = false
                val builder = LatLngBounds.Builder()
                markers.forEach { builder.include(it.position) }
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
                dateTimeTextView.text = frame.date?.dateTimeAsText ?: ""
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
                    .add(R.id.frames_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
                fragment.setFragmentResultListener("EditFrameDialog") { _, bundle ->
                    val frame1: Frame = bundle.getParcelable(ExtraKeys.FRAME)
                        ?: return@setFragmentResultListener
                    model.updateFrame(frame1)
                }
            }
        }
    }

}