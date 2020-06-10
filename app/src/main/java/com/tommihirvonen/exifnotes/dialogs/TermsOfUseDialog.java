package com.tommihirvonen.exifnotes.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

/**
 * This dialog is shown when the user first opens the application.
 * If the user agrees to the terms of use, access to the app is granted.
 * If the user disagrees with the terms of use, the app is closed.
 */
public class TermsOfUseDialog {

    /**
     * Reference to the calling activity used to get SharedPreferences and attach the dialog
     */
    private final Activity activity;

    /**
     * Constructor to get the calling activity
     *
     * @param activity parent activity
     */
    public TermsOfUseDialog(final Activity activity) {
        this.activity = activity;
    }

    /**
     * Shows the dialog to the user if the user has not previously agreed
     * to the terms of use. Also shows the user what's new with this version of the app.
     */
    public void show() {
        final PackageInfo versionInfo = Utilities.getPackageInfo(activity);

        String versionName = "";
        long versionCode = 0;
        if (versionInfo != null) {
            versionName = versionInfo.versionName;
            versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
                    versionInfo.getLongVersionCode() : versionInfo.versionCode;
        }

        // the termsOfUseKey changes every time you increment the version number in the build.gradle script
        final String prefix = "TERMS_OF_USE_";
        final String termsOfUseKey = prefix + versionCode;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean hasBeenShown = prefs.getBoolean(termsOfUseKey, false);

        if(!hasBeenShown){

            // Show the terms of use
            final String title = activity.getString(R.string.app_name);

            //Includes the updates as well so users know what changed.
            final String message = activity.getString(R.string.AboutAndTermsOfUse, versionName)
                    + "\n\n\n" + activity.getString(R.string.Updates);

            final SpannableString spannableString = new SpannableString(message);
            Linkify.addLinks(spannableString, Linkify.WEB_URLS);

            final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(title)
                    .setMessage(spannableString)
                    .setPositiveButton(R.string.Agree, (dialogInterface, i) -> {
                        // Mark this version as read.
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(termsOfUseKey, true);
                        editor.apply();
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(R.string.Decline, (dialog, which) -> {
                        // Close the activity as they have declined the EULA
                        activity.finish();
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
            //The dialog needs to be shown first. Otherwise textView will be null.
            final TextView textView = dialog.findViewById(android.R.id.message);
            textView.setTextSize(14);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

}
