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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.fragments.FramesFragment
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.*

/**
 * Activity to contain the fragment for frames
 */
class FramesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val requestingLocationUpdates = prefs.getBoolean(PreferenceConstants.KEY_GPS_UPDATE, true)

        val permissionAccessCoarseLocation = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val permissionAccessFineLocation = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val locationEnabled = requestingLocationUpdates
                && (permissionAccessCoarseLocation || permissionAccessFineLocation)

        // Get the arguments from the intent from MainActivity.
        val intent = intent
        val roll = intent.getParcelableExtra<Roll>(ExtraKeys.ROLL)
        if (roll == null) finish()

        // The point at which super.onCreate() is called is important.
        // Calling it at the end of the method resulted in the back button not appearing
        // when action mode was enabled.
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

        // Use the same activity layout as in MainActivity.
        setContentView(R.layout.activity_main)
        val container = findViewById<FrameLayout>(R.id.fragment_container)
        container.transitionName = "roll_transition_${roll?.id}"

        if (findViewById<View?>(R.id.fragment_container) != null && savedInstanceState == null) {

            // Pass the arguments from MainActivity on to FramesFragment.
            val framesFragment = FramesFragment()
            val arguments = Bundle()
            arguments.putParcelable(ExtraKeys.ROLL, roll)
            arguments.putBoolean(ExtraKeys.LOCATION_ENABLED, locationEnabled)
            framesFragment.arguments = arguments
            supportFragmentManager.beginTransaction().add(R.id.fragment_container,
                    framesFragment, FramesFragment.FRAMES_FRAGMENT_TAG).commit()
        }
    }

}