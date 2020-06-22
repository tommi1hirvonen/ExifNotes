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
import com.tommihirvonen.exifnotes.dialogs.TermsOfUseDialog
import com.tommihirvonen.exifnotes.fragments.RollsFragment
import com.tommihirvonen.exifnotes.fragments.RollsFragment.OnRollSelectedListener
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants
import com.tommihirvonen.exifnotes.utilities.Utilities

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

        /**
         * Tag for database import request. Used when the PreferenceActivity is launched.
         */
        const val PREFERENCE_ACTIVITY_REQUEST = 8

        private const val FRAMES_ACTIVITY_REQUEST = 10
    }

    /**
     * Value to store whether location services should be enabled or not.
     * Determined by the location permissions granted to the app by the user.
     */
    private var locationPermissionsGranted = false

    private var darkThemeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {

        // Delete all complementary pictures, which are not linked to any frame.
        // Do this each time the app is launched to keep the storage consumption to a minimum.
        // If savedInstanceState is not null, then the activity is being recreated. In this case,
        // don't delete pictures.
        if (savedInstanceState == null) ComplementaryPicturesManager.deleteUnusedPictures(this)

        // Set the activity's UI
        if (Utilities.isAppThemeDark(baseContext)) {
            setTheme(R.style.AppTheme_Dark)
            darkThemeEnabled = true
        }

        // The point at which super.onCreate() is called is important.
        // Calling it at the end of the method resulted in the back button not appearing
        // when action mode was enabled.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TermsOfUseDialog(this).show()
        Utilities.setUiColor(this, false)
        if (supportActionBar != null) supportActionBar!!.title = "  " + resources.getString(R.string.MainActivityTitle)

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

    public override fun onResume() {
        super.onResume()
        // When resuming we have to change the UI colours again.
        if (Utilities.isAppThemeDark(baseContext) && !darkThemeEnabled ||
                !Utilities.isAppThemeDark(baseContext) && darkThemeEnabled) {
            recreate()
            return
        }
        val primaryColor = Utilities.getPrimaryUiColor(baseContext)
        val secondaryColor = Utilities.getSecondaryUiColor(baseContext)
        Utilities.setSupportActionBarColor(this, primaryColor)
        Utilities.setStatusBarColor(this, secondaryColor)
    }

    public override fun onStart() {
        // This method is called when MainActivity is started, in other words when the
        // application is started. All the files created in FramesFragment.setShareIntentExportRoll
        // in the application's external storage directory are deleted. This way we can keep
        // the number of files stored to a minimum.

        //Delete all the files created in FramesFragment.setShareIntentExportRoll
        val externalStorageDir = getExternalFilesDir(null)
        Utilities.purgeDirectory(externalStorageDir)
        val externalCacheDir = externalCacheDir
        Utilities.purgeDirectory(externalCacheDir)
        super.onStart()
    }

    override fun onRollSelected(rollId: Long) {
        val framesActivityIntent = Intent(this, FramesActivity::class.java)
        framesActivityIntent.putExtra(ExtraKeys.ROLL_ID, rollId)
        framesActivityIntent.putExtra(ExtraKeys.LOCATION_ENABLED, locationPermissionsGranted)
        framesActivityIntent.putExtra(ExtraKeys.OVERRIDE_PENDING_TRANSITION, true)
        startActivityForResult(framesActivityIntent, FRAMES_ACTIVITY_REQUEST)
    }

    /**
     * This method is called if GPS is not enabled.
     * Prompt the user to jump to settings to enable GPS.
     */
    private fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(this)
        // Setting Dialog Title
        alertDialog.setTitle(R.string.GPSSettings)
        // Setting Dialog Message
        alertDialog.setMessage(R.string.GPSNotEnabled)
        // On pressing Settings button
        alertDialog.setPositiveButton(R.string.GoToSettings) { _: DialogInterface?, _: Int ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        // On pressing disable button
        alertDialog.setNegativeButton(R.string.DisableInApp) { dialogInterface: DialogInterface, _: Int ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            val prefsEditor = prefs.edit()
            prefsEditor.putBoolean(PreferenceConstants.KEY_GPS_UPDATE, false)
            prefsEditor.apply()
            dialogInterface.dismiss()
        }
        // On pressing cancel button
        alertDialog.setNeutralButton(R.string.Cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        // Showing Alert Message
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
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // PreferenceActivity and FramesActivity are started for result
        // and the possible result is captured here.

        // The result code is compared using bitwise operators to determine
        // whether a new database was imported, the app theme was changed or both.
        when (requestCode) {
            PREFERENCE_ACTIVITY_REQUEST, FRAMES_ACTIVITY_REQUEST -> {

                // If a new database was imported, update the contents of RollsFragment.
                if (resultCode and PreferenceActivity.RESULT_DATABASE_IMPORTED ==
                        PreferenceActivity.RESULT_DATABASE_IMPORTED) {
                    val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (fragment is RollsFragment) {
                        fragment.updateFragment(true)
                    }
                }
                // If the app theme was changed, recreate activity.
                if (resultCode and PreferenceActivity.RESULT_THEME_CHANGED ==
                        PreferenceActivity.RESULT_THEME_CHANGED) {
                    recreate()
                }
                return
            }
        }

        // Call super in case the result was not handled here
        super.onActivityResult(requestCode, resultCode, data)
    }

}