/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentLocationPickBinding
import com.tommihirvonen.exifnotes.datastructures.LocationPickResponse
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Geocoder
import com.tommihirvonen.exifnotes.utilities.decimalString
import com.tommihirvonen.exifnotes.utilities.setNavigationResult
import com.tommihirvonen.exifnotes.utilities.snackbar
import com.tommihirvonen.exifnotes.viewmodels.LocationPickViewModel
import com.tommihirvonen.exifnotes.viewmodels.LocationPickViewModelFactory
import kotlinx.coroutines.launch

class LocationPickFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    MenuProvider, SearchView.OnQueryTextListener {

    private val arguments by navArgs<LocationPickFragmentArgs>()

    private val model by viewModels<LocationPickViewModel> {
        LocationPickViewModelFactory(arguments.location, arguments.formattedAddress)
    }

    private var googleMap: GoogleMap? = null

    private var marker: Marker? = null

    private var fragmentRestored = false

    private lateinit var binding: FragmentLocationPickBinding

    private var mapType = 0

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentRestored = savedInstanceState != null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLocationPickBinding.inflate(layoutInflater)

        binding.fab.setOnClickListener(onLocationSet)
        binding.fabCurrentLocation.setOnClickListener(onRequestCurrentLocation)

        binding.topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.topAppBar.addMenuProvider(this)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL)

        binding.fabMap.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.fabMap)
            popupMenu.inflate(R.menu.menu_map_fragment)
            // Get map type from preferences
            when (sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL)) {
                GoogleMap.MAP_TYPE_NORMAL -> popupMenu.menu.findItem(R.id.menu_item_normal).isChecked = true
                GoogleMap.MAP_TYPE_HYBRID -> popupMenu.menu.findItem(R.id.menu_item_hybrid).isChecked = true
                GoogleMap.MAP_TYPE_SATELLITE -> popupMenu.menu.findItem(R.id.menu_item_satellite).isChecked = true
                GoogleMap.MAP_TYPE_TERRAIN -> popupMenu.menu.findItem(R.id.menu_item_terrain).isChecked = true
                else -> popupMenu.menu.findItem(R.id.menu_item_normal).isChecked = true
            }
            popupMenu.setOnMenuItemClickListener(onPopupMenuItemSelected)
            popupMenu.show()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // If the formatted address is set, display it
        val (location, formattedAddress) = model
        if (location != null && formattedAddress?.isNotEmpty() == true) {
            binding.formattedAddress.text = formattedAddress
        } else if (location != null) {
            binding.progressBar.visibility = View.VISIBLE
            // Start a coroutine to asynchronously fetch the formatted address.
            lifecycleScope.launch {
                val (_, addressResult) = Geocoder(requireContext()).getData(location.decimalString)
                binding.progressBar.visibility = View.INVISIBLE
                model.formattedAddress = if (addressResult.isNotEmpty()) {
                    binding.formattedAddress.text = addressResult
                    addressResult
                } else {
                    binding.formattedAddress.setText(R.string.AddressNotFound)
                    null
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    private val onLocationSet: (View) -> Unit = {
        model.location?.let {
            val response = LocationPickResponse(it, model.formattedAddress)
            setNavigationResult(response, ExtraKeys.LOCATION)
            findNavController().navigateUp()
        } ?: binding.root.snackbar(R.string.NoLocation, binding.bottomBar,
            Snackbar.LENGTH_SHORT)
    }

    @SuppressLint("MissingPermission")
    private val onRequestCurrentLocation = { _: View ->
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location: android.location.Location? ->
                if (location != null) {
                    val position = LatLng(location.latitude, location.longitude)
                    onMapClick(position)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                }
            }
        }
    }

    private val onPopupMenuItemSelected = { item: MenuItem ->
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Store the currently set location to outState.
        model.location?.let {
            outState.putParcelable(ExtraKeys.LOCATION, LatLng(it.latitude, it.longitude))
            outState.putString(ExtraKeys.FORMATTED_ADDRESS, model.formattedAddress)
        }
    }

    override fun onMapReady(googleMap_: GoogleMap) {
        googleMap = googleMap_
        googleMap?.let { googleMap ->
            googleMap.uiSettings.isMyLocationButtonEnabled = false
            googleMap.setOnMapClickListener(this)
            // If night mode is enabled, stylize the map with the custom dark theme.
            when (resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    googleMap.setMapStyle(MapStyleOptions(resources.getString(R.string.style_json)))
                }
            }
            googleMap.mapType = mapType
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
                googleMap.isMyLocationEnabled = true
            }
            model.location?.let { latLngLocation ->
                marker = googleMap.addMarker(MarkerOptions().position(latLngLocation))
                if (!fragmentRestored) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngLocation, 15f))
                    binding.root.snackbar(
                        R.string.TapOnMap, binding.bottomBar,
                        Snackbar.LENGTH_SHORT)
                }
            }
        }
    }

    override fun onMapClick(latLng: LatLng) {
        // Get the formatted address
        binding.formattedAddress.text = ""
        binding.progressBar.visibility = View.VISIBLE
        val latitude = "" + latLng.latitude
        val longitude = "" + latLng.longitude
        val query = "$latitude $longitude"

        // Start a coroutine to asynchronously fetch the formatted address.
        lifecycleScope.launch {
            val (_, addressResult) = Geocoder(requireContext()).getData(query)
            binding.progressBar.visibility = View.INVISIBLE
            model.formattedAddress = if (addressResult.isNotEmpty()) {
                binding.formattedAddress.text = addressResult
                addressResult
            } else {
                binding.formattedAddress.setText(R.string.AddressNotFound)
                null
            }
        }

        // if the location was cleared before editing -> add marker to selected location
        if (marker == null) {
            marker = googleMap?.addMarker(MarkerOptions().position(latLng))
        } else {
            marker?.position = latLng
        }

        model.location = latLng
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Retrieve the SearchView and plug it into SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        // The SearchView's query hint is localization dependant by default.
        // Replace it with the English text.
        searchView.queryHint = getString(R.string.SearchWEllipsis)
        searchView.setOnQueryTextListener(this)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        // When the user enters the search string, use a coroutine to get
        // the formatted address and coordinates. Also move the marker if the result was valid.
        binding.formattedAddress.text = ""
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val (position, addressResult) = Geocoder(requireContext()).getData(query)
            if (position != null) {
                // marker is null, if the search was made before the marker has been added
                // -> add marker to selected location
                if (marker == null) {
                    marker = googleMap?.addMarker(MarkerOptions().position(position))
                } else {
                    // otherwise just set the location
                    marker?.position = position
                }
                model.location = position
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                binding.formattedAddress.text = addressResult
                model.formattedAddress = addressResult
            } else {
                binding.formattedAddress.setText(R.string.AddressNotFound)
                model.formattedAddress = null
            }
            binding.progressBar.visibility = View.INVISIBLE
        }

        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        // Do nothing
        return false
    }

    private fun setMapType(mapType: Int) {
        this.mapType = mapType
        googleMap?.mapType = mapType
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = preferences.edit()
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType)
        editor.apply()
    }
}