package com.tommihirvonen.exifnotes.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Build
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.utilities.Utilities
import com.tommihirvonen.exifnotes.utilities.packageInfo

/**
 * This dialog is shown when the user first opens the application.
 * If the user agrees to the terms of use, access to the app is granted.
 * If the user disagrees with the terms of use, the app is closed.
 */
class TermsOfUseDialog(private val activity: Activity) {

    /**
     * Shows the dialog to the user if the user has not previously agreed
     * to the terms of use. Also shows the user what's new with this version of the app.
     */
    fun show() {
        val versionInfo = activity.packageInfo
        val versionName = versionInfo?.versionName ?: ""
        @Suppress("DEPRECATION")
        val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) versionInfo?.longVersionCode ?: 0
                else versionInfo?.versionCode?.toLong() ?: 0

        // the termsOfUseKey changes every time you increment the version number in the build.gradle script
        val prefix = "TERMS_OF_USE_"
        val termsOfUseKey = prefix + versionCode
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val hasBeenShown = prefs.getBoolean(termsOfUseKey, false)
        if (!hasBeenShown) {
            // Show the terms of use
            val title = activity.getString(R.string.app_name)
            //Includes the updates as well so users know what changed.
            val message = "${activity.getString(R.string.AboutAndTermsOfUse, versionName)}\n\n\n${activity.getString(R.string.Updates)}"
            val spannableString = SpannableString(message)
            Linkify.addLinks(spannableString, Linkify.WEB_URLS)
            val builder = AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(title)
                    .setMessage(spannableString)
                    .setPositiveButton(R.string.Agree) { dialogInterface: DialogInterface, _: Int ->
                        // Mark this version as read.
                        val editor = prefs.edit()
                        editor.putBoolean(termsOfUseKey, true)
                        editor.apply()
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton(R.string.Decline) { _: DialogInterface?, _: Int ->
                        // Close the activity as they have declined the EULA
                        activity.finish()
                    }
            val dialog = builder.create()
            dialog.show()
            //The dialog needs to be shown first. Otherwise textView will be null.
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            textView.textSize = 14f
            textView.movementMethod = LinkMovementMethod.getInstance()
        }
    }

}