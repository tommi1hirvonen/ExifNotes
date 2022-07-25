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

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ActivitySettingsBinding
import com.tommihirvonen.exifnotes.fragments.PreferenceFragment
import com.tommihirvonen.exifnotes.utilities.*

/**
 * PreferenceActivity contains the PreferenceFragment for editing the app's settings
 * and preferences.
 */
class PreferenceActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the activity was recreated, get the saved result code
        savedInstanceState?.let { resultCode = it.getInt(ExtraKeys.RESULT_CODE) }

        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(R.id.fragment_layout, PreferenceFragment()).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Save the result code so that it can be set for this activity's result when recreated
        super.onSaveInstanceState(outState)
        outState.putInt(ExtraKeys.RESULT_CODE, resultCode)
    }

}