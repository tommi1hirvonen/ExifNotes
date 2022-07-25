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
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.dialogs.TermsOfUseDialog
import com.tommihirvonen.exifnotes.fragments.RollsFragment
import com.tommihirvonen.exifnotes.fragments.RollsFragment.OnRollSelectedListener
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*

/**
 * MainActivity is the first activity to be called when the app is launched.
 * It contains the RollsFragment and FramesFragment fragments.
 * The activity switches between these two fragments.
 */
class MainActivity : AppCompatActivity(), OnRollSelectedListener {

    companion object {
        /**
         * Tag for permission request
         */
        private const val MY_MULTIPLE_PERMISSIONS_REQUEST = 1
    }

    /**
     * Value to store whether location services should be enabled or not.
     * Determined by the location permissions granted to the app by the user.
     */
    private var locationPermissionsGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {

        // Delete all complementary pictures, which are not linked to any frame.
        // Do this each time the app is launched to keep the storage consumption to a minimum.
        // If savedInstanceState is not null, then the activity is being recreated. In this case,
        // don't delete pictures.
        if (savedInstanceState == null) ComplementaryPicturesManager.deleteUnusedPictures(this)


        // The point at which super.onCreate() is called is important.
        // Calling it at the end of the method resulted in the back button not appearing
        // when action mode was enabled.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TermsOfUseDialog(this).show()

        // Check that the application has write permission to the phone's external storage
        // and access to location services.
        val permissionWriteExternalStorage = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val permissionAccessCoarseLocation = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val permissionAccessFineLocation = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!permissionWriteExternalStorage || !permissionAccessCoarseLocation || !permissionAccessFineLocation) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_MULTIPLE_PERMISSIONS_REQUEST)
        } else {
            locationPermissionsGranted = true
        }

        // Get from DefaultSharedPreferences whether the user has enabled
        // location updates in the app's settings.
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true)

        // Getting GPS status
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        // Show a dialog to go to settings only if GPS is not enabled in system settings
        // but location updates are enabled in the app's settings.
        if (!isGPSEnabled && requestingLocationUpdates) showSettingsAlert()


        // Set the Fragment to the activity's fragment container

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById<View?>(R.id.fragment_container) != null) {
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return
            }
            // Create a new Fragment to be placed in the activity layout
            val firstFragment = RollsFragment()
            // Add the fragment to the 'fragment_container' FrameLayout
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, firstFragment, RollsFragment.ROLLS_FRAGMENT_TAG).commit()
        }
    }

    public override fun onStart() {
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

    override fun onRollSelected(roll: Roll) {
        val framesActivityIntent = Intent(this, FramesActivity::class.java)
        framesActivityIntent.putExtra(ExtraKeys.ROLL, roll)
        framesActivityIntent.putExtra(ExtraKeys.LOCATION_ENABLED, locationPermissionsGranted)
        framesActivityIntent.putExtra(ExtraKeys.OVERRIDE_PENDING_TRANSITION, true)
        startActivity(framesActivityIntent)
    }

    /**
     * This method is called if GPS is not enabled.
     * Prompt the user to jump to device settings to enable GPS.
     */
    private fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(R.string.GPSSettings)
        alertDialog.setMessage(R.string.GPSNotEnabled)
        // Navigate to the device's settings.
        alertDialog.setPositiveButton(R.string.GoToSettings) { _: DialogInterface?, _: Int ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        // Disable location tracking in the application's settings.
        alertDialog.setNegativeButton(R.string.DisableInApp) { dialogInterface: DialogInterface, _: Int ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            val prefsEditor = prefs.edit()
            prefsEditor.putBoolean(PreferenceConstants.KEY_GPS_UPDATE, false)
            prefsEditor.apply()
            dialogInterface.dismiss()
        }
        alertDialog.setNeutralButton(R.string.Cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        // If request is cancelled, the result arrays are empty. Thus we check
        if (requestCode == MY_MULTIPLE_PERMISSIONS_REQUEST) {
            // the length of grantResults first.

            //Check location permissions
            if (grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionsGranted = true
            }

            //Check write permissions
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                //In case write permission was denied, inform the user.
                Toast.makeText(this, R.string.NoWritePermission, Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}