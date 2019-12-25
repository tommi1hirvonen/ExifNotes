package com.tommihirvonen.exifnotes.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
    public SimpleEula(Activity activity) {
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
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo;
    }

    /**
     * Shows the eula dialog to the user if the user has not previously agreed
     * to the license agreement. Also shows the user what's new with this version of the app.
     */
    public void show() {
        PackageInfo versionInfo = getPackageInfo();

        // the eulaKey changes every time you increment the version number in the build.gradle script
        String eulaPrefix = "eula_";
        //noinspection deprecation
        final String eulaKey = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
                        eulaPrefix + versionInfo.getLongVersionCode() :
                        eulaPrefix + versionInfo.versionCode;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean hasBeenShown = prefs.getBoolean(eulaKey, false);

        if(!hasBeenShown){

            // Show the Eula
            String title = activity.getString(R.string.app_name) + " v" + versionInfo.versionName;

            //Includes the updates as well so users know what changed.
            String message = activity.getString(R.string.Eula) + "\n\n" +
                    activity.getString(R.string.Updates);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Agree, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Mark this version as read.
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(eulaKey, true);
                            editor.apply();
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.Decline, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the activity as they have declined the EULA
                            activity.finish();
                        }

                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            //The dialog needs to be shown first. Otherwise textView will be null.
            TextView textView = dialog.findViewById(android.R.id.message);
            textView.setTextSize(14);
        }
    }

}
