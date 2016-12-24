package com.tommihirvonen.exifnotes.Utilities;

// Copyright 2015
// Tommi Hirvonen

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class contains utility functions.
 */
public class Utilities {

    /**
     * This function shows a general dialog containing a title and a message.
     *
     * @param activity the calling activity
     * @param title the title of the dialog
     * @param message the message of the dialog
     */
    public static void showGeneralDialog(Activity activity, String title, String message){
        AlertDialog.Builder generalDialogBuilder = new AlertDialog.Builder(activity);
        generalDialogBuilder.setTitle(title);
        generalDialogBuilder.setMessage(message);

        generalDialogBuilder.setNeutralButton(R.string.Close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog generalDialog = generalDialogBuilder.create();
        generalDialog.show();
        //The dialog needs to be shown first. Otherwise textView will be null.
        TextView textView = (TextView) generalDialog.findViewById(android.R.id.message);
        textView.setTextSize(14);
    }

    /**
     * This function writes a text file.
     * @param file the file to be written to
     * @param text the text to be written in that file
     * @return false if something went wrong, true otherwise
     */
    public static boolean writeTextFile(File file, String text){
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        try {
            osw.write(text);
            osw.flush();
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *
     * This function checks the input string for illegal characters.
     *
     * @param input the string to be checked
     * @return String containing a list of the illegal characters found. If no illegal
     * characters were found, the String will be empty.
     */
//    public static String checkReservedChars(String input){
//        String ReservedChars = "|\\?*<\":>/";
//        StringBuilder resultBuilder = new StringBuilder();
//        for ( int i = 0; i < input.length(); ++i ) {
//            Character c = input.charAt(i);
//            if ( ReservedChars.contains(c.toString()) ) {
//                if (resultBuilder.toString().length() > 0) resultBuilder.append(", ");
//                resultBuilder.append(c.toString());
//            }
//        }
//        return resultBuilder.toString();
//    }

    /**
     *
     * This function replaces illegal characters from the input string to make
     * a valid file name string.
     *
     * @param input the string to be handled
     * @return String where the illegal characters are replaced with an underscore
     */
    public static String replaceIllegalChars(String input){
        return input.replaceAll("[|\\\\?*<\":>/]", "_");
    }

    /**
     * Splits a datetime into an ArrayList with date.
     *
     * @param input Datetime string in format YYYY-M-D HH:MM
     * @return ArrayList with three members: { YYYY, M, D }
     */
    public static ArrayList<String> splitDate(String input) {
        String[] items = input.split(" ");
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        String[] items2 = itemList.get(0).split("-");
        itemList = new ArrayList<>(Arrays.asList(items2));
        // { YYYY, M, D }
        return itemList;
    }

    /**
     * Splits a datetime into an ArrayList with time.
     *
     * @param input Datetime string in format YYYY-M-D HH:MM
     * @return ArrayList with two members: { HH, MM }
     */
    public static ArrayList<String> splitTime(String input) {
        String[] items = input.split(" ");
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        String[] items2 = itemList.get(1).split(":");
        itemList = new ArrayList<>(Arrays.asList(items2));
        // { HH, MM }
        return itemList;
    }

    /**
     * This function deletes all the files in a directory
     *
     * @param dir the directory whose files are to be deleted
     */
    public static void purgeDirectory(File dir) {
        for(File file: dir.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    /**
     * Legacy method to imitate the ScrollIndicators introduced in Marshmallow.
     * This method seems to be more reliable than the native ScrollIndicator methods.
     * Plus it works across all the targeted Android versions.
     *
     * @param root the root view containing the NestedScrollView element
     * @param content the NestedScrollView element
     * @param indicators ScrollIndicators in bitwise or format, for example ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM
     */
    public static void setScrollIndicators(ViewGroup root, final NestedScrollView content,
                                            final int indicators) {

        // Set up scroll indicators (if present).
        View indicatorUp = root.findViewById(R.id.scrollIndicatorUp);
        View indicatorDown = root.findViewById(R.id.scrollIndicatorDown);

        // First, remove the indicator views if we're not set to use them
        if (indicatorUp != null && (indicators & ViewCompat.SCROLL_INDICATOR_TOP) == 0) {
            root.removeView(indicatorUp);
            indicatorUp = null;
        }
        if (indicatorDown != null && (indicators & ViewCompat.SCROLL_INDICATOR_BOTTOM) == 0) {
            root.removeView(indicatorDown);
            indicatorDown = null;
        }

        if (indicatorUp != null || indicatorDown != null) {
            final View top = indicatorUp;
            final View bottom = indicatorDown;

            if (content != null) {
                // We're just showing the ScrollView, set up listener.
                content.setOnScrollChangeListener(
                        new NestedScrollView.OnScrollChangeListener() {
                            @Override
                            public void onScrollChange(NestedScrollView v, int scrollX,
                                                       int scrollY,
                                                       int oldScrollX, int oldScrollY) {
                                manageScrollIndicators(v, top, bottom);
                            }
                        });
                // Set up the indicators following layout.
                content.post(new Runnable() {
                    @Override
                    public void run() {
                        manageScrollIndicators(content, top, bottom);
                    }
                });
            } else {
                // We don't have any content to scroll, remove the indicators.
                if (top != null) {
                    root.removeView(top);
                }
                if (bottom != null) {
                    root.removeView(bottom);
                }
            }
        }
    }

    /**
     * Sets the ScrollIndicator visibility according to the scroll state of the
     * passed NestedScrollView.
     *
     * @param v View of the NestedScrollView
     * @param upIndicator View of the top ScrollIndicator
     * @param downIndicator View of the bottom ScrollIndicator
     */
    private static void manageScrollIndicators(View v, View upIndicator, View downIndicator) {
        // Using canScrollVertically methods only results in severe depression.
        // Instead we use getScrollY methods and avoid the headache entirely.
        // Besides, these methods work the same way on all devices.
        if (upIndicator != null) {
            if (v.getScrollY() == 0) {
                upIndicator.setVisibility(View.INVISIBLE);
            } else {
                upIndicator.setVisibility(View.VISIBLE);
            }
        }
        if (downIndicator != null) {
            // To get the actual height of the entire NestedScrollView, we have to do the following.
            // The ScrollView always has one child. Getting its height returns the true height
            // of the ScrollView.
            NestedScrollView nestedScrollView = (NestedScrollView) v;
            if ( v.getScrollY() == nestedScrollView.getChildAt(0).getHeight() - v.getHeight() ) {
                downIndicator.setVisibility(View.INVISIBLE);
            } else {
                downIndicator.setVisibility(View.VISIBLE);
            }
        }
    }

}
