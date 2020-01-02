package com.tommihirvonen.exifnotes.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.tommihirvonen.exifnotes.R;

/**
 * SimpleEula is shown when the user first opens the application on their phone.
 * If the user agrees to the license agreement, access to the app is granted.
 * If the user disagrees with the license agreement, the app is closed.
 */
public class SimpleEula {

    /**
     * Reference to the calling activity used to get SharedPreferences and attach the dialog
     */
    private final Activity activity;

    /**
     * Constructor to get the calling activity
     *
     * @param activity parent activity
     */
    public SimpleEula(final Activity activity) {
        this.activity = activity;
    }

    /**
     * Gets the information of the app's package. Used to display the version code.
     *
     * @return  PackageInfo regarding the current version of the app.
     */
    private PackageInfo getPackageInfo() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = activity.getPackageManager().getPackageInfo(
                    activity.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo;
    }

    /**
     * Shows the eula dialog to the user if the user has not previously agreed
     * to the license agreement. Also shows the user what's new with this version of the app.
     */
    public void show() {
        final PackageInfo versionInfo = getPackageInfo();

        // the eulaKey changes every time you increment the version number in the build.gradle script
        final String eulaPrefix = "eula_";
        final String eulaKey = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
                        eulaPrefix + versionInfo.getLongVersionCode() :
                        eulaPrefix + versionInfo.versionCode;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean hasBeenShown = prefs.getBoolean(eulaKey, false);

        if(!hasBeenShown){

            // Show the Eula
            final String title = activity.getString(R.string.app_name) + " v" + versionInfo.versionName;

            //Includes the updates as well so users know what changed.
            final String message = activity.getString(R.string.Eula) + "\n\n" +
                    activity.getString(R.string.Updates);

            final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Agree, (dialogInterface, i) -> {
                        // Mark this version as read.
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(eulaKey, true);
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
        }
    }

}
