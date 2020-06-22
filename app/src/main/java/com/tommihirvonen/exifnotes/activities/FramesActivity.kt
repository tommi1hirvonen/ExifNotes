package com.tommihirvonen.exifnotes.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.fragments.FramesFragment
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities

/**
 * Activity to contain the fragment for frames
 */
class FramesActivity : AppCompatActivity() {

    companion object {
        /**
         * Constants passed to PreferenceActivity
         */
        const val PREFERENCE_ACTIVITY_REQUEST = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Get the arguments from the intent from MainActivity.
        val intent = intent
        val rollId = intent.getLongExtra(ExtraKeys.ROLL_ID, -1)
        val locationEnabled = intent.getBooleanExtra(ExtraKeys.LOCATION_ENABLED, false)
        val overridePendingTransition = intent.getBooleanExtra(ExtraKeys.OVERRIDE_PENDING_TRANSITION, false)
        if (rollId == -1L) finish()

        // If the activity was launched from MainActivity, enable custom transition animations.
        // If the activity was recreated, then custom animations are not enabled during onCreate().
        if (overridePendingTransition) {
            overridePendingTransition(R.anim.enter_from_right, R.anim.hold)
            // Replace the old intent with a modified one, where the OVERRIDE_PENDING_TRANSITION
            // boolean value has been exhausted and set to false.
            intent.putExtra(ExtraKeys.OVERRIDE_PENDING_TRANSITION, false)
            setIntent(intent)
        }
        if (Utilities.isAppThemeDark(baseContext)) setTheme(R.style.AppTheme_Dark)

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
        Utilities.setUiColor(this, true)
        if (findViewById<View?>(R.id.fragment_container) != null && savedInstanceState == null) {

            // Pass the arguments from MainActivity on to FramesFragment.
            val framesFragment = FramesFragment()
            val arguments = Bundle()
            arguments.putLong(ExtraKeys.ROLL_ID, rollId)
            arguments.putBoolean(ExtraKeys.LOCATION_ENABLED, locationEnabled)
            framesFragment.arguments = arguments
            supportFragmentManager.beginTransaction().add(R.id.fragment_container,
                    framesFragment, FramesFragment.FRAMES_FRAGMENT_TAG).commit()

            // Bring the shadow element from the activity's layout to front.
            findViewById<View>(R.id.shadow).bringToFront()
        }
    }

    override fun onResume() {
        super.onResume()
        val primaryColor = Utilities.getPrimaryUiColor(baseContext)
        val secondaryColor = Utilities.getSecondaryUiColor(baseContext)
        Utilities.setSupportActionBarColor(this, primaryColor)
        Utilities.setStatusBarColor(this, secondaryColor)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // The PreferenceActivity is started for result and the result is captured here.
        // The result code is compared using bitwise operators to determine
        // whether a new database was imported, the app theme was changed or both.

        // If a new database was imported, use setResult() to notify MainActivity as well.
        if (requestCode == PREFERENCE_ACTIVITY_REQUEST) {
            // Finish this activity since the selected roll may not be valid anymore.
            if (resultCode and PreferenceActivity.RESULT_DATABASE_IMPORTED == PreferenceActivity.RESULT_DATABASE_IMPORTED) {
                setResult(resultCode)
                finish()
            }
            // If the app theme was changed, recreate activity for changes to take effect.
            if (resultCode and PreferenceActivity.RESULT_THEME_CHANGED == PreferenceActivity.RESULT_THEME_CHANGED) {
                recreate()
            }
            return
        }
        // Call super in case the result was not handled here
        super.onActivityResult(requestCode, resultCode, data)
    }

}