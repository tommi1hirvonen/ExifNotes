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

package com.tommihirvonen.exifnotes.utilities

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.preferences.PreferenceConstants
import java.io.*

/**
 *
 * Function to set the ActionBar and StatusBar colours of an AppCompatActivity.
 * This function should be called in the onCreate() of every activity.
 *
 * @param displayHomeAsUp whether the back navigation icon should displayed in the ActionBar
 */
fun AppCompatActivity.setUiColor(displayHomeAsUp: Boolean) {
    supportActionBar?.let {
        it.displayOptions = ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_SHOW_TITLE
        it.elevation = 0f
        it.setDisplayHomeAsUpEnabled(displayHomeAsUp)
    }
    setSupportActionBarColor(primaryUiColor)
    setStatusBarColor(secondaryUiColor)
}

/**
 * Function to color the ActionBar of an AppCompatActivity
 *
 * @param color the color to which the ActionBar is colored
 */
fun AppCompatActivity.setSupportActionBarColor(color: Int) = supportActionBar?.setBackgroundDrawable(ColorDrawable(color))

/**
 * Function to set the status bar color
 *
 * @param color the color to be set to the status bar
 */
fun Activity.setStatusBarColor(color: Int) {
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = color
}

fun Fragment.setStatusBarColor(color: Int) = requireActivity().setStatusBarColor(color)

val Context.packageInfo: PackageInfo? get() {
    try {
        return packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

/**
 * Returns a List containing the ui color codes in String format.
 */
internal fun Context.getUiColorList(): List<String> {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val uiColor = prefs.getString("UIColor", "#00838F,#006064") ?: "#00838F,#006064"
    return uiColor.split(",")
}

val Context.primaryUiColor: Int get() = Color.parseColor(this.getUiColorList()[0])
val Context.secondaryUiColor: Int get() = Color.parseColor(this.getUiColorList()[1])
val Fragment.secondaryUiColor: Int get() = requireContext().secondaryUiColor

val Context.isAppThemeDark: Boolean get() =
    PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PreferenceConstants.KEY_DARK_THEME, false)

/**
 * Removes potential illegal characters from a string to make it a valid file name.
 */
fun String.illegalCharsRemoved(): String = replace("[|\\\\?*<\":>/]".toRegex(), "_")

/**
 * Remove all files in a directory. Subdirectories are skipped.
 */
fun File.purgeDirectory() = this.listFiles()?.filterNot { it.isDirectory }?.forEach { it.delete() }

/**
 * Method to build a custom AlertDialog title TextView. This way we can imitate
 * the default AlertDialog title and its padding.
 */
fun Fragment.buildCustomDialogTitleTextView(titleText: String?): TextView {
    val context = requireContext()
    val titleTextView = TextView(context)
    TextViewCompat.setTextAppearance(titleTextView, android.R.style.TextAppearance_DeviceDefault_DialogWindowTitle)
    val dpi = context.resources.displayMetrics.density
    titleTextView.setPadding((20 * dpi).toInt(), (20 * dpi).toInt(), (20 * dpi).toInt(), (10 * dpi).toInt())
    titleTextView.text = titleText ?: ""
    titleTextView.gravity = Gravity.LEFT
    return titleTextView
}

fun File.makeDirsIfNotExists() { if (!isDirectory) mkdirs() }