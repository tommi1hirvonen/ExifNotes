package com.tommihirvonen.exifnotes.utilities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.FrameSortMode;
import com.tommihirvonen.exifnotes.datastructures.Gear;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.RollSortMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Class containing utility functions.
 * Class mimics a static class.
 */
public final class Utilities {

    /**
     * Limit instantiation -> empty private constructor
     */
    private Utilities() { }

    /**
     * Shows a general dialog containing a title and a message.
     *
     * @param activity the calling activity
     * @param title the title of the dialog
     * @param message the message of the dialog
     */
    public static void showGeneralDialog(Activity activity, String title, String message){
        AlertDialog.Builder generalDialogBuilder = new AlertDialog.Builder(activity);
        generalDialogBuilder.setTitle(title);
        generalDialogBuilder.setMessage(message);

        generalDialogBuilder.setNegativeButton(R.string.Close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //Do nothing
            }
        });

        AlertDialog generalDialog = generalDialogBuilder.create();
        generalDialog.show();
        //The dialog needs to be shown first. Otherwise textView will be null.
        TextView textView = generalDialog.findViewById(android.R.id.message);
        textView.setTextSize(14);
    }

    /**
     * Function to set the ActionBar and StatusBar colours of an AppCompatActivity.
     * This function should be called in the onCreate() of every activity.
     *
     * @param activity AppCompatActivity whose ui elements should be coloured
     */
    public static void setUiColor(AppCompatActivity activity, boolean displayHomeAsUp) {
        int primaryColor = getPrimaryUiColor(activity);
        int secondaryColor = getSecondaryUiColor(activity);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            activity.getSupportActionBar().setElevation(0);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(displayHomeAsUp);
        }
        setSupportActionBarColor(activity, primaryColor);
        setStatusBarColor(activity, secondaryColor);
    }

    /**
     * Function to color the ActionBar of an AppCompatActivity
     *
     * @param activity the activity whose ActionBar is colored
     * @param color the color to which the ActionBar is colored
     */
    public static void setSupportActionBarColor(AppCompatActivity activity, int color) {
        if (activity.getSupportActionBar() != null)
            activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
    }

    /**
     * Function to set the status bar color
     *
     * @param activity the base activity
     * @param color the color to be set to the status bar
     */
    public static void setStatusBarColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().setStatusBarColor(color);
        }
    }

    /**
     * Get the primary color of the app's ui
     *
     * @param context the base context of the application
     * @return the primary color as an integer
     */
    public static int getPrimaryUiColor(Context context) {
        List<String> colors = getUiColorList(context);
        return Color.parseColor(colors.get(0));
    }

    /**
     * Get the secondary color of the app's ui
     *
     * @param context the base context of the application
     * @return the secondary color as an integer
     */
    public static int getSecondaryUiColor(Context context) {
        List<String> colors = getUiColorList(context);
        return Color.parseColor(colors.get(1));
    }

    /**
     * Function to get a List containing the ui color codes in String format.
     *
     * @param context the base context of the application
     * @return List containing the ui color codes in String format
     */
    private static List<String> getUiColorList(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String UIColor = prefs.getString("UIColor", "#00838F,#006064");
        return Arrays.asList(UIColor.split(","));
    }

    /**
     * Function to test whether the app's current theme is set to light or dark.
     *
     * @param context application's context
     * @return true if the app's theme is set to dark, false otherwise
     */
    public static boolean isAppThemeDark(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PreferenceConstants.KEY_APP_THEME, PreferenceConstants.VALUE_APP_THEME_LIGHT)
                        .equals(PreferenceConstants.VALUE_APP_THEME_DARK);
    }

    /**
     * This function is used to fix a bug which Google hasn't been able to fix in five years
     * (at the time of writing).
     * https://code.google.com/p/android/issues/detail?id=35482
     *
     * Initially the NumberPicker shows the wrong value, but when the Picker is first scrolled,
     * the displayed value changes to the correct one. This function fixes this and shows
     * the correct value immediately.
     *
     * @param numberPicker the NumberPicker to be fixed
     * @return reference to the fixed NumberPicker
     */
    public static NumberPicker fixNumberPicker(NumberPicker numberPicker) {
        Field field = null;
        try {
            // Disregard IDE warning "Cannot resolve field mInputText'".
            // This function seems to work despite the warning.
            field = NumberPicker.class.getDeclaredField("mInputText");
        } catch (NoSuchFieldException ignore) {

        }
        if (field != null) {
            field.setAccessible(true);
            EditText inputText = null;
            try {
                inputText = (EditText) field.get(numberPicker);
            } catch (IllegalAccessException ignore) {

            }
            if (inputText != null) inputText.setFilters(new InputFilter[0]);
        }
        return numberPicker;
    }

    /**
     * This function writes a text file.
     *
     * @param file the file to be written to
     * @param text the text to be written in that file
     * @return false if something went wrong, true otherwise
     */
    public static boolean writeTextFile(File file, String text){
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        try {
            outputStreamWriter.write(text);
            outputStreamWriter.flush();
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

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
    public static List<String> splitDate(String input) {
        String[] items = input.split(" ");
        List<String> itemList = new ArrayList<>(Arrays.asList(items));
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
    public static List<String> splitTime(String input) {
        String[] items = input.split(" ");
        List<String> itemList = new ArrayList<>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        String[] items2 = itemList.get(1).split(":");
        itemList = new ArrayList<>(Arrays.asList(items2));
        // { HH, MM }
        return itemList;
    }

    /**
     * This function deletes all the files in a directory
     *
     * @param directory the directory whose files are to be deleted
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void purgeDirectory(File directory) {
        // Return if the given File is null
        // (for example no read/write access or storage is not mounted).
        if (directory == null) return;
        for(File file: directory.listFiles()) {
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
     * @param context the application's / activity's context to get SharedPreferences
     * @param content the NestedScrollView element
     * @param indicators ScrollIndicators in bitwise or format,
     *                   for example ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_BOTTOM
     */
    public static void setScrollIndicators(Context context, ViewGroup root,
                                           final NestedScrollView content, final int indicators) {

        int color = isAppThemeDark(context) ?
                ContextCompat.getColor(context, R.color.white) :
                ContextCompat.getColor(context, R.color.black);

        // Set up scroll indicators (if present).
        View indicatorUp = root.findViewById(R.id.scrollIndicatorUp);
        View indicatorDown = root.findViewById(R.id.scrollIndicatorDown);

        if (indicatorUp != null) indicatorUp.setBackgroundColor(color);
        if (indicatorDown != null) indicatorDown.setBackgroundColor(color);

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

    /**
     * Method to build a custom AlertDialog title TextView. This way we can imitate
     * the default AlertDialog title and its padding.
     *
     * @param context Context of the application
     * @param titleText the text to be displayed by the generated TextView
     * @return generated TextView object
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("RtlHardcoded")
    public static TextView buildCustomDialogTitleTextView(Context context, String titleText){
        TextView titleTextView = new TextView(context);
        if (Build.VERSION.SDK_INT < 23) titleTextView.setTextAppearance(
                context, android.R.style.TextAppearance_DeviceDefault_DialogWindowTitle);
        else titleTextView.setTextAppearance(
                android.R.style.TextAppearance_DeviceDefault_DialogWindowTitle);
        float dpi = context.getResources().getDisplayMetrics().density;
        titleTextView.setPadding((int)(20*dpi), (int)(20*dpi), (int)(20*dpi), (int)(10*dpi));
        titleTextView.setText(titleText);
        titleTextView.setGravity(Gravity.LEFT);
        return titleTextView;
    }

    /**
     * This function is called when the user has selected a sorting criteria.
     * Sort the frame list depending on the sorting criteria defined in SharedPreferences.
     *
     * @param context reference to the parent activity
     * @param sortMode enum type referencing the frame sort mode
     * @param database reference to the application's database
     * @param listToSort reference to the frame list that is to be sorted
     */
    public static void sortFrameList(final Context context, FrameSortMode sortMode, final FilmDbHelper database, List<Frame> listToSort) {
        switch (sortMode){
            case FRAME_COUNT:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame frame1, Frame frame2) {
                        // Negative to reverse the sorting order
                        int count1 = frame1.getCount();
                        int count2 = frame2.getCount();
                        int result;
                        if (count1 < count2) result = -1;
                        else result = 1;
                        return result;
                    }
                });
                break;

            case DATE:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame frame1, Frame frame2) {
                        String date1 = frame1.getDate();
                        String date2 = frame2.getDate();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat format =
                                new SimpleDateFormat("yyyy-M-d H:m");
                        Date d1 = null;
                        Date d2 = null;
                        try {
                            d1 = format.parse(date1);
                            d2 = format.parse(date2);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        int result;
                        long diff = 0;
                        //Handle possible NullPointerException
                        if (d1 != null && d2 != null) diff = d1.getTime() - d2.getTime();
                        if (diff < 0 ) result = -1;
                        else result = 1;

                        return result;
                    }
                });
                break;

            case F_STOP:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame frame1, Frame frame2) {

                        final String[] allApertureValues = context.getResources().getStringArray(R.array.AllApertureValues);
                        String aperture1 = frame1.getAperture();
                        aperture1 = aperture1 != null ? aperture1 : "";
                        String aperture2 = frame2.getAperture();
                        aperture2 = aperture2 != null ? aperture2 : "";
                        int pos1 = 0;
                        int pos2 = 0;
                        for (int i = 0; i < allApertureValues.length; ++i){
                            if (aperture1.equals(allApertureValues[i])) pos1 = i;
                            if (aperture2.equals(allApertureValues[i])) pos2 = i;
                        }
                        int result;
                        if (pos1 < pos2) result = -1;
                        else result = 1;
                        return result;
                    }
                });
                break;

            case SHUTTER_SPEED:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame frame1, Frame frame2) {

                        final String[] allShutterValues = context.getResources().getStringArray(R.array.AllShutterValues);
                        //Shutter speed strings need to be modified so that the sorting
                        //works properly.
                        String shutter1 = frame1.getShutter();
                        shutter1 = shutter1 != null ? shutter1.replace("\"", "") : "";
                        String shutter2 = frame2.getShutter();
                        shutter2 = shutter2 != null ? shutter2.replace("\"", "") : "";
                        int pos1 = 0;
                        int pos2 = 0;
                        for (int i = 0; i < allShutterValues.length; ++i){
                            if (shutter1.equals(allShutterValues[i])) pos1 = i;
                            if (shutter2.equals(allShutterValues[i])) pos2 = i;
                        }
                        int result;
                        if (pos1 < pos2) result = -1;
                        else result = 1;
                        return result;
                    }
                });
                break;

            case LENS:
                Collections.sort(listToSort, new Comparator<Frame>() {
                    @Override
                    public int compare(Frame frame1, Frame frame2) {
                        final Lens lens1 = database.getLens(frame1.getLensId());
                        final Lens lens2 = database.getLens(frame2.getLensId());
                        final String name1 = lens1 != null ? lens1.getName() : "";
                        final String name2 = lens2 != null ? lens2.getName() : "";
                        return name1.compareTo(name2);
                    }
                });
                break;
        }
    }

    /**
     * Called when the user has selected a sorting criteria.
     *
     * @param sortMode SortMode enum type
     * @param database reference to the application's database
     * @param listToSort reference to the List that should be sorted
     */
    public static void sortRollList(RollSortMode sortMode, final FilmDbHelper database, List<Roll> listToSort) {
        switch (sortMode){

            case DATE: default:
                Collections.sort(listToSort, new Comparator<Roll>() {
                    @Override
                    public int compare(Roll roll1, Roll roll2) {
                        String date1 = roll1.getDate();
                        String date2 = roll2.getDate();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat format =
                                new SimpleDateFormat("yyyy-M-d H:m");
                        Date d1 = null;
                        Date d2 = null;
                        try {
                            d1 = format.parse(date1);
                            d2 = format.parse(date2);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        int result;
                        long diff = 0;
                        //Handle possible NullPointerException
                        if (d1 != null && d2 != null) diff = d1.getTime() - d2.getTime();
                        if (diff < 0 ) result = 1;
                        else result = -1;

                        return result;
                    }
                });
                break;

            case NAME:
                Collections.sort(listToSort, new Comparator<Roll>() {
                    @Override
                    public int compare(Roll roll1, Roll roll2) {
                        final String name1 = roll1.getName() != null ? roll1.getName() : "";
                        final String name2 = roll2.getName() != null ? roll2.getName() : "";
                        return name1.compareTo(name2);
                    }
                });
                break;

            case CAMERA:
                Collections.sort(listToSort, new Comparator<Roll>() {
                    @Override
                    public int compare(Roll roll1, Roll roll2) {
                        final Camera camera1 = database.getCamera(roll1.getCameraId());
                        final Camera camera2 = database.getCamera(roll2.getCameraId());
                        final String name1 = camera1 != null ? camera1.getName() : "";
                        final String name2 = camera2 != null ? camera2.getName() : "";
                        return name1.compareTo(name2);
                    }
                });
                break;
        }
    }

    /**
     * Utility function to sort a list of Gear by name.
     *
     * @param gearList reference to the List that should be sorted.
     */
    public static void sortGearList(List<? extends Gear> gearList) {
        Collections.sort(gearList, new Comparator<Gear>() {
            @Override
            public int compare(Gear g1, Gear g2) {
                return g1.getName().compareTo(g2.getName());
            }
        });
    }

    /**
     * This function creates a string containing the ExifTool commands for the frames
     * of the specified roll.
     *
     * @param context application's context
     * @param roll Roll object of which the commands should be created
     * @return String containing the ExifTool commands
     */
    public static String createExifToolCmdsString(Context context, Roll roll) {

        final FilmDbHelper database = FilmDbHelper.getInstance(context);

        StringBuilder stringBuilder = new StringBuilder();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String artistName = prefs.getString("ArtistName", "");
        String copyrightInformation = prefs.getString("CopyrightInformation", "");
        String exiftoolPath = prefs.getString("ExiftoolPath", "");
        String picturesPath = prefs.getString("PicturesPath", "");
        final boolean ignoreWarnings = prefs.getBoolean("IgnoreWarnings", false);

        String ignoreWarningsOption = "-m";
        String exiftoolCmd = "exiftool";
        String artistTag = "-Artist=";
        String copyrightTag = "-Copyright=";
        String cameraMakeTag = "-Make=";
        String cameraModelTag = "-Model=";
        String lensMakeTag = "-LensMake=";
        String lensModelTag = "-LensModel=";
        String lensTag = "-Lens=";
        String dateTag = "-DateTime=";
        String dateTimeOriginalTag = "-DateTimeOriginal=";
        String shutterTag = "-ShutterSpeedValue=";
        String exposureTimeTag = "-ExposureTime=";
        String apertureTag = "-ApertureValue=";
        String fNumberTag = "-FNumber=";
        String commentTag = "-UserComment=";
        String imageDescriptionTag = "-ImageDescription=";
        String gpsLatTag = "-GPSLatitude=";
        String gpsLatRefTag = "-GPSLatitudeRef=";
        String gpsLngTag = "-GPSLongitude=";
        String gpsLngRefTag = "-GPSLongitudeRef=";
        String exposureCompTag = "-ExposureCompensation=";
        String focalLengthTag = "-FocalLength=";
        String isoTag = "-ISO=";
        String serialNumberTag = "-SerialNumber=";
        String lensSerialNumberTag = "-LensSerialNumber=";

        String fileEnding = prefs.getString("FileEnding", ".jpg");
        //Check that fileEnding begins with a dot.
        if (fileEnding.charAt(0) != '.') fileEnding = "." + fileEnding;

        String quote = "\"";
        String space = " ";
        String lineSep = "\r\n";

        List<Frame> frameList = database.getAllFramesFromRoll(roll);
        final Camera camera = database.getCamera(roll.getCameraId());

        for (Frame frame : frameList) {

            Lens lens = null;
            if (frame.getLensId() > 0) lens = database.getLens(frame.getLensId());

            //ExifTool path
            if (exiftoolPath.length() > 0) stringBuilder.append(exiftoolPath);
            //ExifTool command
            stringBuilder.append(exiftoolCmd).append(space);
            //Ignore warnings
            if (ignoreWarnings) stringBuilder.append(ignoreWarningsOption).append(space);
            if (camera != null) {
                //CameraMakeTag
                stringBuilder.append(cameraMakeTag).append(quote).append(camera.getMake()).append(quote).append(space);
                //CameraModelTag
                stringBuilder.append(cameraModelTag).append(quote).append(camera.getModel()).append(quote).append(space);
                //SerialNumber
                if (camera.getSerialNumber() != null && camera.getSerialNumber().length() > 0)
                    stringBuilder.append(serialNumberTag).append(quote).append(camera.getSerialNumber()).append(quote).append(space);
            }
            if (lens != null) {
                //LensMakeTag
                stringBuilder.append(lensMakeTag).append(quote).append(lens.getMake()).append(quote).append(space);
                //LensModelTag
                stringBuilder.append(lensModelTag).append(quote).append(lens.getModel()).append(quote).append(space);
                //LensTag
                stringBuilder.append(lensTag).append(quote).append(lens.getMake()).append(space).append(lens.getModel()).append(quote).append(space);
                //LensSerialNumber
                if (lens.getSerialNumber() != null && lens.getSerialNumber().length() > 0)
                    stringBuilder.append(lensSerialNumberTag).append(quote).append(lens.getSerialNumber()).append(quote).append(space);
            }
            if (frame.getDate() != null) {
                //DateTime
                stringBuilder.append(dateTag).append(quote).append(frame.getDate().replace("-", ":")).append(quote).append(space);
                //DateTimeOriginal
                stringBuilder.append(dateTimeOriginalTag).append(quote).append(frame.getDate().replace("-", ":")).append(quote).append(space);
            }
            //ShutterSpeedValue & ExposureTime
            if (frame.getShutter() != null) {
                stringBuilder.append(shutterTag).append(quote).append(frame.getShutter().replace("\"", "")).append(quote).append(space);
                stringBuilder.append(exposureTimeTag).append(quote).append(frame.getShutter().replace("\"", "")).append(quote).append(space);
            }

            //ApertureValue & FNumber
            if (frame.getAperture() != null) {
                stringBuilder.append(apertureTag).append(quote).append(frame.getAperture()).append(quote).append(space);
                stringBuilder.append(fNumberTag).append(quote).append(frame.getAperture()).append(quote).append(space);
            }
            //UserComment & ImageDescription
            if (frame.getNote() != null && frame.getNote().length() > 0) {
                stringBuilder.append(commentTag).append(quote).append(Normalizer.normalize(frame.getNote(), Normalizer.Form.NFC).replace("\"", "'")).append(quote).append(space);
                stringBuilder.append(imageDescriptionTag).append(quote).append(Normalizer.normalize(frame.getNote(), Normalizer.Form.NFC).replace("\"", "'")).append(quote).append(space);
            }
            //GPSLatitude & GPSLongitude & GPSLatitudeRef & GPSLongitudeRef
            if (frame.getLocation() != null && frame.getLocation().length() > 0) {
                String latString = frame.getLocation().substring(0, frame.getLocation().indexOf(" "));
                String lngString = frame.getLocation().substring(frame.getLocation().indexOf(" ") + 1, frame.getLocation().length());
                String latRef;
                if (latString.substring(0, 1).equals("-")) {
                    latRef = "S";
                    latString = latString.substring(1, latString.length());
                } else latRef = "N";
                String lngRef;
                if (lngString.substring(0, 1).equals("-")) {
                    lngRef = "W";
                    lngString = lngString.substring(1, lngString.length());
                } else lngRef = "E";
                latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
                List<String> latStringList = Arrays.asList(latString.split(":"));
                lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
                List<String> lngStringList = Arrays.asList(lngString.split(":"));

                stringBuilder.append(gpsLatTag).append(quote).append(latStringList.get(0)).append(space).append(latStringList.get(1)).append(space).append(latStringList.get(2)).append(quote).append(space);
                stringBuilder.append(gpsLatRefTag).append(quote).append(latRef).append(quote).append(space);
                stringBuilder.append(gpsLngTag).append(quote).append(lngStringList.get(0)).append(space).append(lngStringList.get(1)).append(space).append(lngStringList.get(2)).append(quote).append(space);
                stringBuilder.append(gpsLngRefTag).append(quote).append(lngRef).append(quote).append(space);
            }
            //ExposureCompensation
            if (frame.getExposureComp() != null) stringBuilder.append(exposureCompTag).append(quote).append(frame.getExposureComp()).append(quote).append(space);
            //FocalLength
            if (frame.getFocalLength() > 0) stringBuilder.append(focalLengthTag).append(quote).append(frame.getFocalLength()).append(quote).append(space);
            //ISO
            if (roll.getIso() > 0) stringBuilder.append(isoTag).append(quote).append(roll.getIso()).append(quote).append(space);



            //Artist
            if (artistName.length() > 0) stringBuilder.append(artistTag).append(quote).append(artistName).append(quote).append(space);
            //Copyright
            if (copyrightInformation.length() > 0) stringBuilder.append(copyrightTag).append(quote).append(copyrightInformation).append(quote).append(space);
            //Path to pictures
            if (picturesPath.contains(" ") || fileEnding.contains(" ")) stringBuilder.append(quote);
            if (picturesPath.length() > 0) stringBuilder.append(picturesPath);
            //File ending
            stringBuilder.append("*_").append(frame.getCount()).append(fileEnding);
            if (picturesPath.contains(" ") || fileEnding.contains(" ")) stringBuilder.append(quote);
            //Double new line
            stringBuilder.append(lineSep).append(lineSep);

        }

        return stringBuilder.toString();
    }

    /**
     * This function creates a string which contains csv information about the roll.
     *
     * @param context application's context
     * @param roll Roll object from which the csv information should be created
     * @return String containing the csv information
     */
    public static String createCsvString(Context context, Roll roll) {

        FilmDbHelper database = FilmDbHelper.getInstance(context);
        List<Frame> frameList = database.getAllFramesFromRoll(roll);
        Camera camera = database.getCamera(roll.getCameraId());

        final String separator = ",";
        final String separatorReplacement = ";";
        StringBuilder stringBuilder = new StringBuilder();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String artistName = prefs.getString("ArtistName", "");
        String copyrightInformation = prefs.getString("CopyrightInformation", "");

        //Roll and camera information
        stringBuilder.append("Roll name: ").append(roll.getName()).append("\n");
        stringBuilder.append("Added: ").append(roll.getDate()).append("\n");
        stringBuilder.append("ISO: ").append(roll.getIso()).append("\n");
        stringBuilder.append("Format: ").append(context.getResources().getStringArray(R.array.FilmFormats)[roll.getFormat()]).append("\n");
        stringBuilder.append("Push/pull: ").append(roll.getPushPull()).append("\n");
        stringBuilder.append("Camera: ").append(camera != null ? camera.getName() : "").append("\n");
        stringBuilder.append("Serial number: ").append(camera != null && camera.getSerialNumber() != null ? camera.getSerialNumber() : "").append("\n");
        stringBuilder.append("Notes: ").append(roll.getNote()).append("\n");
        stringBuilder.append("Artist name: ").append(artistName).append("\n");
        stringBuilder.append("Copyright: ").append(copyrightInformation).append("\n");

        //Column headers
        stringBuilder
                .append("Frame Count").append(separator)
                .append("Date").append(separator)
                .append("Lens").append(separator)
                .append("Lens serial number").append(separator)
                .append("Shutter").append(separator)
                .append("Aperture").append(separator)
                .append("Focal length").append(separator)
                .append("Exposure compensation").append(separator)
                .append("Notes").append(separator)
                .append("No of exposures").append(separator)
                .append("Filter").append(separator)
                .append("Location").append(separator)
                .append("Address").append("\n");

        for (Frame frame : frameList) {

            Lens lens = null;
            if (frame.getLensId() > 0) lens = database.getLens(frame.getLensId());

            //FrameCount
            stringBuilder.append(frame.getCount());
            stringBuilder.append(separator);

            //Date
            stringBuilder.append(frame.getDate());
            stringBuilder.append(separator);

            //Lens make and model
            if (lens != null) stringBuilder.append(lens.getMake()).append(" ").append(lens.getModel());
            stringBuilder.append(separator);

            //Lens serial number
            if (lens != null && lens.getSerialNumber() != null && lens.getSerialNumber().length() > 0)
                stringBuilder.append(lens.getSerialNumber());
            stringBuilder.append(separator);

            // /Shutter speed
            if (frame.getShutter() != null) stringBuilder.append(frame.getShutter());
            stringBuilder.append(separator);

            //Aperture
            if (frame.getAperture() != null)
                stringBuilder.append("f").append(frame.getAperture());
            stringBuilder.append(separator);

            //Focal length
            if (frame.getFocalLength() > 0) stringBuilder.append(frame.getFocalLength());
            stringBuilder.append(separator);

            //Exposure compensation
            if (frame.getExposureComp() != null && frame.getExposureComp().length() > 1)
                stringBuilder.append(frame.getExposureComp());
            stringBuilder.append(separator);

            //Note
            if (frame.getNote() != null && frame.getNote().length() > 0) stringBuilder.append(frame.getNote());
            stringBuilder.append(separator);

            //Number of exposures
            stringBuilder.append(frame.getNoOfExposures());
            stringBuilder.append(separator);
            
            //Filters
            if (frame.getFilters().size() > 0) {
                final StringBuilder filterBuilder = new StringBuilder();
                for (int i = 0; i < frame.getFilters().size(); ++i) {
                    filterBuilder.append(frame.getFilters().get(i).getName());
                    if (i < frame.getFilters().size() - 1) filterBuilder.append("|");
                }
                stringBuilder.append(filterBuilder.toString());
            }

            //Location
            if (frame.getLocation() != null && frame.getLocation().length() > 0) {
                String latString = frame.getLocation().substring(0, frame.getLocation().indexOf(" "));
                String lngString = frame.getLocation().substring(frame.getLocation().indexOf(" ") + 1, frame.getLocation().length());
                String latRef;
                if (latString.substring(0, 1).equals("-")) {
                    latRef = "S";
                    latString = latString.substring(1, latString.length());
                } else latRef = "N";
                String lngRef;
                if (lngString.substring(0, 1).equals("-")) {
                    lngRef = "W";
                    lngString = lngString.substring(1, lngString.length());
                } else lngRef = "E";
                latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
                List<String> latStringList = Arrays.asList(latString.split(":"));
                lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
                List<String> lngStringList = Arrays.asList(lngString.split(":"));

                String space = " ";

                stringBuilder.append(latStringList.get(0)).append("°").append(space).append(latStringList.get(1)).append("\'").append(space).append(latStringList.get(2).replace(',', '.')).append("\"").append(space);

                stringBuilder.append(latRef).append(space);

                stringBuilder.append(lngStringList.get(0)).append("°").append(space).append(lngStringList.get(1)).append("\'").append(space).append(lngStringList.get(2).replace(',', '.')).append("\"").append(space);

                stringBuilder.append(lngRef);
            }
            stringBuilder.append(separator);

            //Address
            if (frame.getFormattedAddress() != null && frame.getFormattedAddress().length() > 0) {
                String formattedAddress = frame.getFormattedAddress();
                // Replace commas with semicolons, because comma is reserved for separator
                stringBuilder.append(formattedAddress.replace(separator, separatorReplacement));
            }

            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    /**
     * Creates a location string in human readable format from a location string in decimal format.
     *
     * @param location location string in decimal format
     * @return location string in human readable degrees format
     */
    public static String getReadableLocationFromString(String location) {

        //If the location is empty, return null
        if (location == null || location.length() == 0) return null;

        StringBuilder stringBuilder = new StringBuilder();

        String latString = location.substring(0, location.indexOf(" "));
        String lngString = location.substring(location.indexOf(" ") + 1, location.length());

        String latRef;
        if (latString.substring(0, 1).equals("-")) {
            latRef = "S";
            latString = latString.substring(1, latString.length());
        } else latRef = "N";

        String lngRef;
        if (lngString.substring(0, 1).equals("-")) {
            lngRef = "W";
            lngString = lngString.substring(1, lngString.length());
        } else lngRef = "E";

        latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
        List<String> latStringList = Arrays.asList(latString.split(":"));

        lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
        List<String> lngStringList = Arrays.asList(lngString.split(":"));

        String space = " ";

        stringBuilder.append(latStringList.get(0)).append("°").append(space).append(latStringList.get(1)).append("\'").append(space).append(latStringList.get(2).replace(',', '.')).append("\"").append(space);
        stringBuilder.append(latRef).append(space);
        stringBuilder.append(lngStringList.get(0)).append("°").append(space).append(lngStringList.get(1)).append("\'").append(space).append(lngStringList.get(2).replace(',', '.')).append("\"").append(space);
        stringBuilder.append(lngRef);

        return stringBuilder.toString();
    }

    /**
     * This function is used to convert a Location to a string.
     *
     * @param location Location to be converted
     * @return the converted string
     */
    public static String locationStringFromLocation(final Location location) {
        if (location != null)
            return (Location.convert(location.getLatitude(), Location.FORMAT_DEGREES) + " " +
                    Location.convert(location.getLongitude(), Location.FORMAT_DEGREES)).replace(",", ".");
        else return "";
    }

    /**
     * Gets the current date and time.
     *
     * @return Date and time as a string in format YYYY-M-D H:MM
     */
    public static String getCurrentTime() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        String currentTime;
        if (minute < 10) {
            currentTime = year + "-" + month + "-" + day + " " + hour + ":0" + minute;
        } else currentTime = year + "-" + month + "-" + day + " " + hour + ":" + minute;
        return currentTime;
    }

    /**
     * Creates the specified toFile as a byte for byte copy of the
     * fromFile. If toFile already exists, then it
     * will be replaced with a copy of fromFile. The name and path
     * of toFile will be that of toFile.
     *
     * @param fromFile the file to copy from
     * @param toFile the file to copy to
     */
    @SuppressWarnings("ThrowFromFinallyBlock")
    public static void copyFile(File fromFile, File toFile) throws IOException {
        // Check that the destination folder exists. Create if not.
        if (!toFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            toFile.getParentFile().mkdirs();
        }
        final FileInputStream fromFileInputStream = new FileInputStream(fromFile);
        final FileOutputStream toFileOutputStream = new FileOutputStream(toFile);
        FileChannel fromChannel = null;
        FileChannel toChannel = null;
        try {
            fromChannel = fromFileInputStream.getChannel();
            toChannel = toFileOutputStream.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }

}
