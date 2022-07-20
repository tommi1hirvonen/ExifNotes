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

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants

abstract class LocationUpdatesFragment : Fragment() {

    var locationPermissionsGranted = false
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    var requestingLocationUpdates = false
    var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionsGranted = requireArguments().getBoolean(ExtraKeys.LOCATION_ENABLED, true)

        // Activate GPS locating if the user has granted permission.
        if (locationPermissionsGranted) {

            // Returns null if location permissions were denied.
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            // Create LocationRequest to update the last known location.
            locationRequest = LocationRequest.create().apply {
                interval = 10 * 1000.toLong() // 10 seconds
                fastestInterval = 1000 // 1 second
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }
        }

        // This can be done anyway. It only has effect if locationPermissionsGranted is true.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                lastLocation = locationResult.lastLocation
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true)
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        //Check if GPSUpdate preference has been changed meanwhile
        requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true)
        if (locationPermissionsGranted && requestingLocationUpdates) {
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    override fun onPause() {
        if (locationPermissionsGranted) stopLocationUpdates()
        super.onPause()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Get the last known location immediately when the updates are started.
            if (lastLocation == null) {
                fusedLocationClient?.lastLocation?.addOnSuccessListener { location: Location? ->
                    location?.let { lastLocation = it }
                }
            }
            // Start requesting location updates on set time intervals.
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

}