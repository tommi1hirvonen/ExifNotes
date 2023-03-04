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

package com.tommihirvonen.exifnotes.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ActivityLocationPickBinding
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*
import kotlinx.coroutines.*

/**
 * Allows the user to select a location for a frame on a map.
 */
class LocationPickActivity : AppCompatActivity(), OnMapReadyCallback, OnMapClickListener,
    SearchView.OnQueryTextListener, MenuProvider {
    /**
     * GoogleMap object to show the map and marker
     */
    private var googleMap: GoogleMap? = null

    /**
     * Marker object to hold the marker added/moved by the user.
     */
    private var marker: Marker? = null
    
    /**
     * String to hold the current formatted address
     */
    private var formattedAddress: String? = null

    /**
     * Holds the current location.
     */
    private var latLngLocation: LatLng? = null

    /**
     * Member to indicate whether this activity was continued or not.
     * Some animations will only be activated if this value is false.
     */
    private var continueActivity = false

    /**
     * Holds reference to the GoogleMap map type
     */
    private var mapType = 0

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var binding: ActivityLocationPickBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the device is locked, this activity can be shown regardless.
        // This way the user doesn't have to unlock the device with authentication
        // just to access this activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            val window = window
            @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        if (savedInstanceState != null) continueActivity = true

        binding = ActivityLocationPickBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.fab.setOnClickListener(onLocationSet)
        binding.fabCurrentLocation.setOnClickListener(onRequestCurrentLocation)

        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.addMenuProvider(this)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL)

        binding.fabMap.setOnClickListener {
            val popupMenu = PopupMenu(this, binding.fabMap)
            popupMenu.inflate(R.menu.menu_map_fragment)
            // Get map type from preferences
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
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // If the activity is continued, then savedInstanceState is not null.
        // Get the location from there.
        val location: LatLng?
        if (savedInstanceState != null) {
            location = savedInstanceState.parcelable(ExtraKeys.LOCATION)
            formattedAddress = savedInstanceState.getString(ExtraKeys.FORMATTED_ADDRESS)
        } else {
            // Else get the location from Intent.
            val intent = intent
            location = intent.parcelable(ExtraKeys.LOCATION)
            formattedAddress = intent.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
        }
        if (location != null) {
            latLngLocation = location
            // If the formatted address is set, display it
            if (formattedAddress != null && formattedAddress?.isNotEmpty() == true) {
                binding.formattedAddress.text = formattedAddress
            } else {
                binding.progressBar.visibility = View.VISIBLE
                // Start a coroutine to asynchronously fetch the formatted address.
                lifecycleScope.launch {
                    val (_, addressResult) = Geocoder(this@LocationPickActivity)
                        .getData(location.decimalString)
                    binding.progressBar.visibility = View.INVISIBLE
                    formattedAddress = if (addressResult.isNotEmpty()) {
                        binding.formattedAddress.text = addressResult
                        addressResult
                    } else {
                        binding.formattedAddress.setText(R.string.AddressNotFound)
                        null
                    }
                }
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private val onLocationSet = { _: View ->
        latLngLocation?.let {
            val intent = Intent()
            intent.putExtra(ExtraKeys.LOCATION, LatLng(it.latitude, it.longitude))
            intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, formattedAddress)
            setResult(Activity.RESULT_OK, intent)
            finish()
        } ?: binding.root.snackbar(R.string.NoLocation, binding.bottomBar,
            Snackbar.LENGTH_SHORT)
    }

    @SuppressLint("MissingPermission")
    private val onRequestCurrentLocation = { _: View ->
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location: android.location.Location? ->
                if (location != null) {
                    val position = LatLng(location.latitude, location.longitude)
                    onMapClick(position)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                }
            }
        }
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

    /**
     * Sets the GoogleMap map type
     *
     * @param mapType One of the map type constants from class GoogleMap
     */
    private fun setMapType(mapType: Int) {
        this.mapType = mapType
        googleMap?.mapType = mapType
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType)
        editor.apply()
    }

    override fun onMapReady(googleMap_: GoogleMap) {
        googleMap = googleMap_
        googleMap?.let { googleMap ->
            googleMap.uiSettings.isMyLocationButtonEnabled = false
            googleMap.setOnMapClickListener(this)
            // If night mode is enabled, stylize the map with the custom dark theme.
            when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    googleMap.setMapStyle(MapStyleOptions(resources.getString(R.string.style_json)))
                }
            }
            googleMap.mapType = mapType
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                googleMap.isMyLocationEnabled = true
            }
            latLngLocation?.let {latLngLocation ->
                marker = googleMap.addMarker(MarkerOptions().position(latLngLocation))
                if (!continueActivity) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngLocation, 15f))
                    binding.root.snackbar(R.string.TapOnMap, binding.bottomBar,
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
            val (_, addressResult) = Geocoder(this@LocationPickActivity).getData(query)
            binding.progressBar.visibility = View.INVISIBLE
            formattedAddress = if (addressResult.isNotEmpty()) {
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
        latLngLocation = latLng
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        // When the user enters the search string, use a coroutine to get
        // the formatted address and coordinates. Also move the marker if the result was valid.
        binding.formattedAddress.text = ""
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val (position, addressResult) = Geocoder(this@LocationPickActivity).getData(query)
            if (position != null) {
                // marker is null, if the search was made before the marker has been added
                // -> add marker to selected location
                if (marker == null) {
                    marker = googleMap?.addMarker(MarkerOptions().position(position))
                } else {
                    // otherwise just set the location
                    marker?.position = position
                }
                latLngLocation = position
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                binding.formattedAddress.text = addressResult
                formattedAddress = addressResult
            } else {
                binding.formattedAddress.setText(R.string.AddressNotFound)
                formattedAddress = null
            }
            binding.progressBar.visibility = View.INVISIBLE
        }

        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        // Do nothing
        return false
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Store the currently set location to outState.
        latLngLocation?.let {
            outState.putParcelable(ExtraKeys.LOCATION, LatLng(it.latitude, it.longitude))
            outState.putString(ExtraKeys.FORMATTED_ADDRESS, formattedAddress)
        }
    }
}