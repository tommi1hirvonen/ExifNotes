package com.tommihirvonen.exifnotes.Utilities;

// Copyright 2015
// Tommi Hirvonen

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class contains utility functions.
 */
public class Utilities {

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

}
