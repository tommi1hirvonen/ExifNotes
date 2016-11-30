package com.tommihirvonen.exifnotes.Utilities;

// Copyright 2015
// Tommi Hirvonen

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
     * This function checks the input string for illegal characters.
     *
     * @param input the string to be checked
     * @return String containing a list of the illegal characters found. If no illegal
     * characters were found, the String will be empty.
     */
    public static String checkReservedChars(String input){
        String ReservedChars = "|\\?*<\":>/";
        StringBuilder resultBuilder = new StringBuilder();
        for ( int i = 0; i < input.length(); ++i ) {
            Character c = input.charAt(i);
            if ( ReservedChars.contains(c.toString()) ) {
                if (resultBuilder.toString().length() > 0) resultBuilder.append(", ");
                resultBuilder.append(c.toString());
            }
        }
        return resultBuilder.toString();
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

}
