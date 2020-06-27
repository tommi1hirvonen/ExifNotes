package com.tommihirvonen.exifnotes.activities

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.fragments.PreferenceFragment
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities
import com.tommihirvonen.exifnotes.utilities.setColorFilterCompat

/**
 * PreferenceActivity contains the PreferenceFragment for editing the app's settings
 * and preferences.
 */
class PreferenceActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {

    companion object {
        /**
         * Public constant custom result code used to indicate that a database was imported
         */
        const val RESULT_DATABASE_IMPORTED = 0x10

        /**
         * Public constant custom result code used to indicate that the app's theme was changed
         */
        const val RESULT_THEME_CHANGED = 0x20
    }

    /**
     * Member to store the current result code to be passed to the activity which started
     * this activity for result.
     */
    var resultCode = 0x0
        set(value) {
            field = value
            setResult(value)
        }

    /**
     * The ActionBar layout is added manually
     */
    private lateinit var actionbar: Toolbar

    override fun onPostCreate(savedInstanceState: Bundle?) {
        // Set the UI and add listeners.
        overridePendingTransition(R.anim.enter_from_right, R.anim.hold)
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        if (Utilities.isAppThemeDark(baseContext)) {
            setTheme(R.style.Theme_AppCompat)
        }

        super.onPostCreate(savedInstanceState)

        // If the activity was recreated, get the saved result code
        savedInstanceState?.let { resultCode = it.getInt(ExtraKeys.RESULT_CODE) }

        val primaryColor = Utilities.getPrimaryUiColor(baseContext)
        val secondaryColor = Utilities.getSecondaryUiColor(baseContext)
        supportActionBar?.hide()
        prefs.registerOnSharedPreferenceChangeListener(this)
        Utilities.setStatusBarColor(this, secondaryColor)

        // This is a way to get the action bar in Preferences.
        // This is a legacy implementation. All this is needed in order to make
        // the action bar title and icon appear in white. WTF!?
        setContentView(R.layout.activity_settings_legacy)
        if (Utilities.isAppThemeDark(baseContext)) {
            val relativeLayout = findViewById<RelativeLayout>(R.id.rel_layout)
            relativeLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background_dark_grey))
        }
        actionbar = findViewById(R.id.actionbar)
        actionbar.setTitle(R.string.Preferences)
        actionbar.setBackgroundColor(primaryColor)
        actionbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        // And even this shit! Since API 23 (M) this is needed to render the back button white.
        // Do only for M and up, on older devices this will cause Resources$NotFoundException.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            actionbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_ab_back_material)
        }
        actionbar.navigationIcon?.setColorFilterCompat(ContextCompat.getColor(baseContext, R.color.white))
        actionbar.setNavigationOnClickListener { finish() }
        supportFragmentManager.beginTransaction().add(R.id.rel_layout, PreferenceFragment()).commit()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Save the result code so that it can be set for this activity's result when recreated
        super.onSaveInstanceState(outState)
        outState.putInt(ExtraKeys.RESULT_CODE, resultCode)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val primaryColor = Utilities.getPrimaryUiColor(baseContext)
        val secondaryColor = Utilities.getSecondaryUiColor(baseContext)
        Utilities.setStatusBarColor(this, secondaryColor)
        actionbar = findViewById(R.id.actionbar)
        actionbar.setBackgroundColor(primaryColor)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right)
    }

}