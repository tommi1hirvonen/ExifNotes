/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.di.location

import android.Manifest
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tommihirvonen.exifnotes.screens.settings.SettingsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var lastLocation: Location? = null
        private set

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest
        .Builder(Priority.PRIORITY_HIGH_ACCURACY, 10*1000.toLong())
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation = locationResult.lastLocation
        }
    }

    private val preferenceChangeListener =
        OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsViewModel.KEY_GPS_UPDATE) {
                appLocationEnabled = getShouldRequestLocationUpdates()
                if (!appLocationEnabled) {
                    pStopLocationUpdates()
                } else if (isRequestingLocation) {
                    pStartLocationUpdates()
                }
            }
        }

    private var isRequestingLocation = false
    private var appLocationEnabled = getShouldRequestLocationUpdates()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun getShouldRequestLocationUpdates() = sharedPreferences.getBoolean(
        SettingsViewModel.KEY_GPS_UPDATE,
        true
    )

    fun startLocationUpdates() {
        isRequestingLocation = true
        pStartLocationUpdates()
    }

    fun stopLocationUpdates() {
        isRequestingLocation = false
        pStopLocationUpdates()
    }

    private fun pStartLocationUpdates() {
        if (!appLocationEnabled) {
            return
        }

        val fineLocationPermissions = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (fineLocationPermissions != PackageManager.PERMISSION_GRANTED
            && coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        isRequestingLocation = true
        if (lastLocation == null) {
            locationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let { lastLocation = it }
            }
        }
    }

    private fun pStopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }
}