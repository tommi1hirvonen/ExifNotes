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
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.libraries.places.api.Places
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.databinding.ActivityMainBinding
import com.tommihirvonen.exifnotes.dialogs.TermsOfUseDialog
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager
import com.tommihirvonen.exifnotes.utilities.purgeDirectory
import com.tommihirvonen.exifnotes.utilities.snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity is the first activity to be called when the app is launched.
 * It contains the RollsFragment and FramesFragment fragments.
 * The activity switches between these two fragments.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var database: Database

    companion object {
        /**
         * Tag for permission request
         */
        private const val MY_MULTIPLE_PERMISSIONS_REQUEST = 1
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // If the app theme was set in the app's preferences, override the night mode setting.
        when (prefs.getString(PreferenceConstants.KEY_APP_THEME, "DEFAULT") ?: "DEFAULT") {
            "LIGHT" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "DARK" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        super.onCreate(savedInstanceState)

        // Delete all complementary pictures, which are not linked to any frame.
        // Do this each time the app is launched to keep the storage consumption to a minimum.
        // If savedInstanceState is not null, then the activity is being recreated. In this case,
        // don't delete pictures.
        if (savedInstanceState == null) {
            ComplementaryPicturesManager.deleteUnusedPictures(this, database)
        }

        setContentView(binding.root)

        if (savedInstanceState == null) {
            TermsOfUseDialog(this).show()

            // Check that the application has write permission to the phone's external storage
            // and access to location services.
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            val permissionGrants = permissions.map {
                ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
            if (permissionGrants.contains(false)) {
                ActivityCompat.requestPermissions(this, permissions, MY_MULTIPLE_PERMISSIONS_REQUEST)
            }

            // Get from DefaultSharedPreferences whether the user has enabled
            // location updates in the app's settings.
            val requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true)

            // Getting GPS status
            val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // Show a dialog to go to settings only if GPS is not enabled in system settings
            // but location updates are enabled in the app's settings.
            if (!isGPSEnabled && requestingLocationUpdates) showSettingsAlert()
        }

        if (!Places.isInitialized()) {
            val apiKey = resources.getString(R.string.google_maps_key)
            Places.initialize(applicationContext, apiKey)
        }
    }

    override fun onStart() {
        // This method is called when MainActivity is started, in other words when the
        // application is started. All the files created in FramesFragment.setShareIntentExportRoll
        // in the application's external storage directory are deleted. This way we can keep
        // the number of files stored to a minimum.

        //Delete all the files created in FramesFragment.setShareIntentExportRoll
        val externalStorageDir = getExternalFilesDir(null)
        externalStorageDir?.purgeDirectory()
        externalCacheDir?.purgeDirectory()
        super.onStart()
    }

    /**
     * This method is called if GPS is not enabled.
     * Prompt the user to jump to device settings to enable GPS.
     */
    private fun showSettingsAlert() = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.GPSSettings)
        .setMessage(R.string.GPSNotEnabled)
        // Navigate to the device's settings.
        .setPositiveButton(R.string.GoToSettings) { _: DialogInterface?, _: Int ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        // Disable location tracking in the application's settings.
        .setNegativeButton(R.string.DisableInApp) { dialogInterface: DialogInterface, _: Int ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            val prefsEditor = prefs.edit()
            prefsEditor.putBoolean(PreferenceConstants.KEY_GPS_UPDATE, false)
            prefsEditor.apply()
            dialogInterface.dismiss()
        }
        .setNeutralButton(R.string.Cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        .show()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == MY_MULTIPLE_PERMISSIONS_REQUEST) {
            // If request is cancelled, the result arrays are empty. Thus we check the length of grantResults first.
            // Check write permissions
            val writePermissionIndex = permissions.indexOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (grantResults.getOrNull(writePermissionIndex) == PackageManager.PERMISSION_DENIED) {
                //In case write permission was denied, inform the user.
                binding.root.snackbar(R.string.NoWritePermission)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}