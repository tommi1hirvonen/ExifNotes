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
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ApiException
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.adapters.PlacePredictionAdapter
import com.tommihirvonen.exifnotes.databinding.FragmentLocationPickBinding
import com.tommihirvonen.exifnotes.utilities.LocationPickResponse
import com.tommihirvonen.exifnotes.geocoder.GeocoderRequestBuilder
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.setNavigationResult
import com.tommihirvonen.exifnotes.utilities.snackbar
import com.tommihirvonen.exifnotes.viewmodels.Animate
import com.tommihirvonen.exifnotes.viewmodels.LocationPickViewModel
import com.tommihirvonen.exifnotes.viewmodels.LocationPickViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationPickFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    @Inject
    lateinit var geocoderRequestBuilder: GeocoderRequestBuilder

    private val arguments by navArgs<LocationPickFragmentArgs>()

    private val model by viewModels<LocationPickViewModel> {
        LocationPickViewModelFactory(requireActivity().application, geocoderRequestBuilder,
            arguments.location, arguments.formattedAddress)
    }

    private var googleMap: GoogleMap? = null

    private var marker: Marker? = null

    private var fragmentRestored = false

    private lateinit var binding: FragmentLocationPickBinding

    private var mapType = 0

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val placesClient by lazy { Places.createClient(requireContext()) }

    private val predictionsAdapter = PlacePredictionAdapter()

    private val predictionHandler = Handler(Looper.getMainLooper())

    private val sessionToken by lazy { AutocompleteSessionToken.newInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentRestored = savedInstanceState != null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLocationPickBinding.inflate(layoutInflater)
        binding.toolbar.visibility = if (arguments.showToolbar) View.VISIBLE else View.GONE
        binding.viewmodel = model.observable

        binding.fab.setOnClickListener(onLocationSet)
        binding.fabCurrentLocation.setOnClickListener(onRequestCurrentLocation)

        // Initialize place predictions
        val layoutManager = LinearLayoutManager(requireContext())
        binding.placePredictions.layoutManager = LinearLayoutManager(requireContext())
        binding.placePredictions.adapter = predictionsAdapter
        binding.placePredictions.addItemDecoration(DividerItemDecoration(requireContext(), layoutManager.orientation))
        predictionsAdapter.onPlaceClickListener = { place ->
            // User selected a place suggestion. Run a geocoding request with the place id.
            val text = place.getPrimaryText(null).toString()
            binding.searchBar.setText(text)
            binding.searchView.clearFocusAndHideKeyboard()
            binding.searchView.hide()
            lifecycleScope.launch { model.submitPlaceId(place.placeId) }
        }

        binding.searchBar.setOnClickListener { binding.searchBar.expand(binding.searchView) }
        binding.searchBar.setNavigationOnClickListener {
            val text = binding.searchBar.text
            if (text.isNotEmpty()) {
                binding.searchBar.setText("")
            } else {
                findNavController().navigateUp()
            }
        }

        binding.searchView.setupWithSearchBar(binding.searchBar)
        binding.searchView.editText.setOnEditorActionListener { _, _, event ->
            // Check that the event is nothing other than enter (submit)
            if (event == null) {
                return@setOnEditorActionListener false
            }
            // Run a geocoding request with the raw query.
            val query = binding.searchView.text.toString().trim()
            val text = binding.searchView.text.toString()
            binding.searchBar.setText(text)
            binding.searchView.hide()
            if (query.isNotEmpty()) {
                lifecycleScope.launch { model.submitQuery(query) }
            }
            false
        }

        // Hook up place predictions to SearchView text onChange.
        binding.searchView.editText.doOnTextChanged { text, _, _, _ ->
            getPlacePredictions(text.toString())
        }

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

        val (locationData, address) = model.location.value to model.formattedAddress
        // If the formatted address is not yet, set the model location to start address query.
        if (locationData?.location != null && address == null) {
            lifecycleScope.launch { model.setLocation(locationData.location) }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.let { googleMap ->

            // Set map padding so that the map icons aren't hidden by the bottom sheet.
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val cardViewWidth = binding.bottomBar.width +
                        binding.bottomBar.marginLeft +
                        binding.bottomBar.marginRight
                googleMap.setPadding(cardViewWidth, 0, 0, 0)
            } else {
                val cardViewHeight = binding.bottomBar.height +
                        binding.bottomBar.marginBottom +
                        binding.bottomBar.marginTop
                googleMap.setPadding(0, 0, 0, cardViewHeight)
            }

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

            if (!fragmentRestored) {
                binding.root.snackbar(R.string.TapOnMap,
                    binding.bottomBar, Snackbar.LENGTH_SHORT)
            }

            model.location.observe(viewLifecycleOwner) { (location, animate) ->
                if (location == null) {
                    return@observe
                }

                if (marker == null) {
                    marker = googleMap.addMarker(MarkerOptions().position(location))
                } else {
                    marker?.position = location
                }

                if (fragmentRestored) {
                    fragmentRestored = false
                    return@observe
                }

                when (animate) {
                    Animate.MOVE -> googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                    Animate.ANIMATE -> googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                    Animate.NONE -> {}
                }
            }
        }
    }

    override fun onMapClick(latLng: LatLng) {
        lifecycleScope.launch { model.setLocation(latLng) }
    }

    private fun getPlacePredictions(query: String) {
        predictionHandler.removeCallbacksAndMessages(null)
        if (query.isBlank()) {
            return
        }
        binding.progressBarPredictions.isIndeterminate = true
        predictionHandler.postDelayed({
            val bounds = googleMap?.projection?.visibleRegion?.latLngBounds
            val builder = FindAutocompletePredictionsRequest
                .builder()
                .setSessionToken(sessionToken)
                .setQuery(query)
            if (bounds != null) {
                val bias = RectangularBounds.newInstance(bounds)
                builder.locationBias = bias
            }
            val request = builder.build()
            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                predictionsAdapter.setPredictions(response.autocompletePredictions)
                binding.progressBarPredictions.isIndeterminate = false
            }.addOnFailureListener { exception ->
                binding.progressBarPredictions.isIndeterminate = false
                if (exception is ApiException) {
                    Toast.makeText(requireContext(),
                        "Place not found: ${exception.statusCode}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }, 350)
    }

    private val onLocationSet: (View) -> Unit = {
        model.location.value?.location?.let {
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
                    lifecycleScope.launch { model.setLocation(position, Animate.ANIMATE) }
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

    private fun setMapType(mapType: Int) {
        this.mapType = mapType
        googleMap?.mapType = mapType
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = preferences.edit()
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType)
        editor.apply()
    }
}