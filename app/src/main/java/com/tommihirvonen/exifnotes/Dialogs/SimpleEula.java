package com.tommihirvonen.exifnotes.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.tommihirvonen.exifnotes.R;

// Copyright 2015
// Tommi Hirvonen

/**
 * SimpleEula is shown when the user first opens the application on their phone.
 * If the user agrees to the license agreement, access to the app is granted.
 * If the user disagrees with the license agreement, the app is closed.
 */
public class SimpleEula {

    private Activity mActivity;

    public SimpleEula(Activity context) {
        mActivity = context;
    }

    /**
     * Gets the information of the app's package. Is used to display the version code.
     * @return  PackageInfo regarding the current version of the app.
     */
    private PackageInfo getPackageInfo() {
        PackageInfo pi = null;
        try {
            pi = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pi;
    }

    /**
     * Shows the eula dialog to the user if the user has not previously agreed
     * to the license agreement. Also shows the user what's new with this version of the app.
     */
    public void show() {
        PackageInfo versionInfo = getPackageInfo();

        // the eulaKey changes every time you increment the version number in the AndroidManifest.xml
        String eulaPrefix = "eula_";
        final String eulaKey = eulaPrefix + versionInfo.versionCode;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean hasBeenShown = prefs.getBoolean(eulaKey, false);

        if(!hasBeenShown){

            // Show the Eula
            String title = mActivity.getString(R.string.app_name) + " v" + versionInfo.versionName;

            //Includes the updates as well so users know what changed.
            String message = mActivity.getString(R.string.Updates) + "\n\n" + mActivity.getString(R.string.Eula);

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
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
                            mActivity.finish();
                        }

                    });
            builder.create().show();
        }
    }

}
