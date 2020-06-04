package com.tommihirvonen.exifnotes.fragments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.SupportErrorDialogFragment
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants

abstract class LocationUpdatesFragment : Fragment(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    companion object {
        private const val REQUEST_RESOLVE_ERROR = 1001
        private const val ERROR_DIALOG = 3
        private const val DIALOG_ERROR = "dialog_error"
    }

    var locationPermissionsGranted = false
    private var googleApiClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    var requestingLocationUpdates = false
    var lastLocation: Location? = null
    private var resolvingError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionsGranted = requireArguments().getBoolean(ExtraKeys.LOCATION_ENABLED, true)
        // Activate GPS locating if the user has granted permission.
        if (locationPermissionsGranted) {

            // Create an instance of GoogleAPIClient for latlng_location services.
            if (googleApiClient == null) {
                googleApiClient = GoogleApiClient.Builder(requireActivity())
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build()
            }
            // Create locationRequest to update the current latlng_location.
            locationRequest = LocationRequest()
            // 10 seconds
            locationRequest?.interval = 10 * 1000.toLong()
            // 1 second
            locationRequest?.fastestInterval = 1000
            locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
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

    override fun onStart() {
        super.onStart()
        if (locationPermissionsGranted) googleApiClient?.connect()
    }

    override fun onStop() {
        if (locationPermissionsGranted) googleApiClient?.disconnect()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
        //Check if GPSUpdate preference has been changed meanwhile
        requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true)
        if (locationPermissionsGranted && googleApiClient?.isConnected == true && requestingLocationUpdates) {
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    override fun onPause() {
        if (locationPermissionsGranted) stopLocationUpdates()
        super.onPause()
    }

    override fun onConnected(p0: Bundle?) {
        if (ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (googleApiClient != null) {
                LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation
                        .addOnSuccessListener { location: Location? -> if (location != null) lastLocation = location }
            }
            if (requestingLocationUpdates) {
                startLocationUpdates()
            }
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        if (locationPermissionsGranted) googleApiClient?.connect()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        if (result.hasResolution() && !resolvingError) {
            try {
                resolvingError = true
                result.startResolutionForResult(activity, REQUEST_RESOLVE_ERROR)
            } catch (e: IntentSender.SendIntentException) {
                // There was an error with the resolution intent. Try again.
                googleApiClient?.connect()
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.errorCode)
            resolvingError = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_RESOLVE_ERROR -> {
                resolvingError = false
                if (resultCode == Activity.RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    googleApiClient?.let {
                        if (!it.isConnecting && !it.isConnected) it.connect()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (googleApiClient != null) {
                LocationServices.getFusedLocationProviderClient(requireActivity())
                        .requestLocationUpdates(locationRequest, locationCallback, null)
            }
        }
    }

    private fun stopLocationUpdates() {
        if (googleApiClient?.isConnected == true) {
            LocationServices.getFusedLocationProviderClient(requireActivity()).removeLocationUpdates(locationCallback)
        }
    }

    private fun showErrorDialog(errorCode: Int) {
        // Create a fragment for the error dialog
        val dialogFragment = ErrorDialogFragment()
        // Pass the error that should be displayed
        val args = Bundle()
        args.putInt(DIALOG_ERROR, errorCode)
        dialogFragment.arguments = args
        dialogFragment.setTargetFragment(this, ERROR_DIALOG)
        dialogFragment.show(parentFragmentManager, "errordialog")
    }

    inner class ErrorDialogFragment : SupportErrorDialogFragment() {
        override fun onSaveInstanceState(outState: Bundle) {
            setTargetFragment(null, -1)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if (savedInstanceState != null) {
                setTargetFragment(
                        requireActivity().supportFragmentManager.findFragmentByTag(FramesFragment.FRAMES_FRAGMENT_TAG),
                        ERROR_DIALOG)
            }
            // Get the error code and retrieve the appropriate dialog
            val errorCode = this.requireArguments().getInt(DIALOG_ERROR)
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.activity, errorCode, REQUEST_RESOLVE_ERROR)
        }

        override fun onDismiss(dialog: DialogInterface) {
            resolvingError = false
            super.onDismiss(dialog)
        }
    }

}