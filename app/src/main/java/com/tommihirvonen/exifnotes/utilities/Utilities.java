package com.tommihirvonen.exifnotes.utilities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
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
    public static void showGeneralDialog(final Activity activity, final String title, final String message){
        final AlertDialog.Builder generalDialogBuilder = new AlertDialog.Builder(activity);
        generalDialogBuilder.setTitle(title);
        generalDialogBuilder.setMessage(message);

        generalDialogBuilder.setNegativeButton(R.string.Close, (dialog, which) -> {
            //Do nothing
        });

        final AlertDialog generalDialog = generalDialogBuilder.create();
        generalDialog.show();
        //The dialog needs to be shown first. Otherwise textView will be null.
        final TextView textView = generalDialog.findViewById(android.R.id.message);
        textView.setTextSize(14);
    }

    /**
     * Function to set the ActionBar and StatusBar colours of an AppCompatActivity.
     * This function should be called in the onCreate() of every activity.
     *
     * @param activity AppCompatActivity whose ui elements should be coloured
     */
    public static void setUiColor(final AppCompatActivity activity, final boolean displayHomeAsUp) {
        final int primaryColor = getPrimaryUiColor(activity);
        final int secondaryColor = getSecondaryUiColor(activity);
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
    public static void setSupportActionBarColor(final AppCompatActivity activity, final int color) {
        if (activity.getSupportActionBar() != null)
            activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
    }

    /**
     * Function to set the status bar color
     *
     * @param activity the base activity
     * @param color the color to be set to the status bar
     */
    public static void setStatusBarColor(final Activity activity, final int color) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        activity.getWindow().setStatusBarColor(color);
    }

    /**
     * Get the primary color of the app's ui
     *
     * @param context the base context of the application
     * @return the primary color as an integer
     */
    public static int getPrimaryUiColor(final Context context) {
        final List<String> colors = getUiColorList(context);
        return Color.parseColor(colors.get(0));
    }

    /**
     * Get the secondary color of the app's ui
     *
     * @param context the base context of the application
     * @return the secondary color as an integer
     */
    public static int getSecondaryUiColor(final Context context) {
        final List<String> colors = getUiColorList(context);
        return Color.parseColor(colors.get(1));
    }

    /**
     * Function to get a List containing the ui color codes in String format.
     *
     * @param context the base context of the application
     * @return List containing the ui color codes in String format
     */
    private static List<String> getUiColorList(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String UIColor = prefs.getString("UIColor", "#00838F,#006064");
        return Arrays.asList(UIColor.split(","));
    }

    /**
     * Function to test whether the app's current theme is set to light or dark.
     *
     * @param context application's context
     * @return true if the app's theme is set to dark, false otherwise
     */
    public static boolean isAppThemeDark(final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
    public static NumberPicker fixNumberPicker(final NumberPicker numberPicker) {
        Field field = null;
        try {
            // Disregard IDE warning "Cannot resolve field mInputText'".
            // This function seems to work despite the warning.
            //noinspection JavaReflectionMemberAccess
            field = NumberPicker.class.getDeclaredField("mInputText");
        } catch (final NoSuchFieldException ignore) {

        }
        if (field != null) {
            field.setAccessible(true);
            EditText inputText = null;
            try {
                inputText = (EditText) field.get(numberPicker);
            } catch (final IllegalAccessException ignore) {

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
     */
    public static void writeTextFile(final File file, final String text){
        final FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);

        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        try {
            outputStreamWriter.write(text);
            outputStreamWriter.flush();
            outputStreamWriter.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * This function replaces illegal characters from the input string to make
     * a valid file name string.
     *
     * @param input the string to be handled
     * @return String where the illegal characters are replaced with an underscore
     */
    public static String replaceIllegalChars(final String input){
        return input.replaceAll("[|\\\\?*<\":>/]", "_");
    }

    /**
     * Splits a datetime into an ArrayList with date.
     *
     * @param input Datetime string in format YYYY-M-D HH:MM
     * @return ArrayList with three members: { YYYY, M, D }
     */
    public static List<String> splitDate(final String input) {
        final String[] items = input.split(" ");
        List<String> itemList = new ArrayList<>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        final String[] items2 = itemList.get(0).split("-");
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
    public static List<String> splitTime(final String input) {
        final String[] items = input.split(" ");
        List<String> itemList = new ArrayList<>(Arrays.asList(items));
        // { YYYY-M-D, HH:MM }
        final String[] items2 = itemList.get(1).split(":");
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
    public static void purgeDirectory(final File directory) {
        // Return if the given File is null
        // (for example no read/write access or storage is not mounted).
        if (directory == null) return;
        for(final File file: directory.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    /**
     * Class which manages custom Android Marsmallow type scroll indicators based on a RecyclerView
     */
    public static class ScrollIndicatorRecyclerViewListener extends RecyclerView.OnScrollListener {

        @NonNull private final View indicatorUp;
        @NonNull private final View indicatorDown;
        @NonNull private final RecyclerView recyclerView;

        public ScrollIndicatorRecyclerViewListener(@NonNull final Context context,
                                                       @NonNull final RecyclerView recyclerView,
                                                       @NonNull final View indicatorUp,
                                                       @NonNull final View indicatorDown) {
            this.recyclerView = recyclerView;
            this.indicatorUp = indicatorUp;
            this.indicatorDown = indicatorDown;

            final int color = isAppThemeDark(context) ?
                    ContextCompat.getColor(context, R.color.white) :
                    ContextCompat.getColor(context, R.color.black);

            indicatorUp.setBackgroundColor(color);
            indicatorDown.setBackgroundColor(color);

            recyclerView.post(this::toggleIndicators);
        }

        private void toggleIndicators() {
            // If we can't scroll upwards, hide the up scroll indicator. Otherwise show it.
            if (!recyclerView.canScrollVertically(-1)) {
                indicatorUp.setVisibility(View.INVISIBLE);
            } else {
                indicatorUp.setVisibility(View.VISIBLE);
            }
            // If we can't scroll down, hide the down scroll indicator. Otherwise show it.
            if (!recyclerView.canScrollVertically(1)) {
                indicatorDown.setVisibility(View.INVISIBLE);
            } else {
                indicatorDown.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            toggleIndicators();
        }

    }

    /**
     * Class which manages custom Android Marshmallow type scroll indicators inside a NestedScrollView.
     */
    public static class ScrollIndicatorNestedScrollViewListener implements NestedScrollView.OnScrollChangeListener {

        @NonNull private final View indicatorUp;
        @NonNull private final View indicatorDown;
        @NonNull private final NestedScrollView nestedScrollView;

        public ScrollIndicatorNestedScrollViewListener(@NonNull final Context context,
                                                @NonNull final NestedScrollView nestedScrollView,
                                                @NonNull final View indicatorUp,
                                                @NonNull final View indicatorDown) {
            this.nestedScrollView = nestedScrollView;
            this.indicatorUp = indicatorUp;
            this.indicatorDown = indicatorDown;

            final int color = isAppThemeDark(context) ?
                    ContextCompat.getColor(context, R.color.white) :
                    ContextCompat.getColor(context, R.color.black);

            indicatorUp.setBackgroundColor(color);
            indicatorDown.setBackgroundColor(color);

            nestedScrollView.post(this::toggleIndicators);
        }

        private void toggleIndicators() {
            // If we can't scroll upwards, hide the up scroll indicator. Otherwise show it.

            // Using canScrollVertically methods only results in severe depression.
            // Instead we use getScrollY methods and avoid the headache entirely.
            // Besides, these methods work the same way on all devices.
            if (nestedScrollView.getScrollY() == 0) {
                indicatorUp.setVisibility(View.INVISIBLE);
            } else {
                indicatorUp.setVisibility(View.VISIBLE);
            }

            // If we can't scroll down, hide the down scroll indicator. Otherwise show it.

            // To get the actual height of the entire NestedScrollView, we have to do the following.
            // The ScrollView always has one child. Getting its height returns the true height
            // of the ScrollView.
            if (nestedScrollView.getScrollY() ==
                    nestedScrollView.getChildAt(0).getHeight() - nestedScrollView.getHeight() ) {
                indicatorDown.setVisibility(View.INVISIBLE);
            } else {
                indicatorDown.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            toggleIndicators();
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
    @SuppressLint("RtlHardcoded")
    public static TextView buildCustomDialogTitleTextView(final Context context, final String titleText){
        final TextView titleTextView = new TextView(context);
        TextViewCompat.setTextAppearance(titleTextView, android.R.style.TextAppearance_DeviceDefault_DialogWindowTitle);
        final float dpi = context.getResources().getDisplayMetrics().density;
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
    public static void sortFrameList(final Context context, final FrameSortMode sortMode,
                                     final FilmDbHelper database, final List<Frame> listToSort) {
        switch (sortMode){
            case FRAME_COUNT:
                Collections.sort(listToSort, (frame1, frame2) -> {
                    // Negative to reverse the sorting order
                    final int count1 = frame1.getCount();
                    final int count2 = frame2.getCount();
                    final int result;
                    result = Integer.compare(count1, count2);
                    return result;
                });
                break;

            case DATE:
                Collections.sort(listToSort, (frame1, frame2) -> {
                    final String date1 = frame1.getDate();
                    final String date2 = frame2.getDate();
                    @SuppressLint("SimpleDateFormat") final SimpleDateFormat format =
                            new SimpleDateFormat("yyyy-M-d H:m");
                    Date d1 = null;
                    Date d2 = null;
                    try {
                        d1 = format.parse(date1);
                        d2 = format.parse(date2);
                    } catch (final ParseException e) {
                        e.printStackTrace();
                    }

                    final int result;
                    long diff = 0;
                    //Handle possible NullPointerException
                    if (d1 != null && d2 != null) diff = d1.getTime() - d2.getTime();
                    if (diff < 0 ) result = -1;
                    else if (diff > 0) result = 1;
                    else result = 0;

                    return result;
                });
                break;

            case F_STOP:
                Collections.sort(listToSort, (frame1, frame2) -> {

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
                    final int result;
                    result = Integer.compare(pos1, pos2);
                    return result;
                });
                break;

            case SHUTTER_SPEED:
                Collections.sort(listToSort, (frame1, frame2) -> {

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
                    final int result;
                    result = Integer.compare(pos1, pos2);
                    return result;
                });
                break;

            case LENS:
                Collections.sort(listToSort, (frame1, frame2) -> {
                    final Lens lens1 = database.getLens(frame1.getLensId());
                    final Lens lens2 = database.getLens(frame2.getLensId());
                    final String name1 = lens1 != null ? lens1.getName() : "";
                    final String name2 = lens2 != null ? lens2.getName() : "";
                    return name1.compareTo(name2);
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
    public static void sortRollList(final RollSortMode sortMode, final FilmDbHelper database, final List<Roll> listToSort) {
        switch (sortMode){

            case DATE: default:
                Collections.sort(listToSort, (roll1, roll2) -> {
                    final String date1 = roll1.getDate();
                    final String date2 = roll2.getDate();
                    @SuppressLint("SimpleDateFormat") final SimpleDateFormat format =
                            new SimpleDateFormat("yyyy-M-d H:m");
                    Date d1 = null;
                    Date d2 = null;
                    try {
                        d1 = format.parse(date1);
                        d2 = format.parse(date2);
                    } catch (final ParseException e) {
                        e.printStackTrace();
                    }

                    final int result;
                    long diff = 0;
                    //Handle possible NullPointerException
                    if (d1 != null && d2 != null) diff = d1.getTime() - d2.getTime();
                    if (diff < 0 ) result = 1;
                    else if (diff > 0) result = -1;
                    else result = 0;

                    return result;
                });
                break;

            case NAME:
                Collections.sort(listToSort, (roll1, roll2) -> {
                    final String name1 = roll1.getName() != null ? roll1.getName() : "";
                    final String name2 = roll2.getName() != null ? roll2.getName() : "";
                    return name1.compareTo(name2);
                });
                break;

            case CAMERA:
                Collections.sort(listToSort, (roll1, roll2) -> {
                    final Camera camera1 = database.getCamera(roll1.getCameraId());
                    final Camera camera2 = database.getCamera(roll2.getCameraId());
                    final String name1 = camera1 != null ? camera1.getName() : "";
                    final String name2 = camera2 != null ? camera2.getName() : "";
                    return name1.compareTo(name2);
                });
                break;
        }
    }

    /**
     * Utility function to sort a list of Gear by name.
     *
     * @param gearList reference to the List that should be sorted.
     */
    public static void sortGearList(final List<? extends Gear> gearList) {
        Collections.sort(gearList, (Comparator<Gear>) (g1, g2) ->
                g1.getName().compareToIgnoreCase(g2.getName())
        );
    }

    /**
     * This function creates a string containing the ExifTool commands for the frames
     * of the specified roll.
     *
     * @param context application's context
     * @param roll Roll object of which the commands should be created
     * @return String containing the ExifTool commands
     */
    public static String createExifToolCmdsString(final Context context, final Roll roll) {

        final FilmDbHelper database = FilmDbHelper.getInstance(context);

        final StringBuilder stringBuilder = new StringBuilder();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String artistName = prefs.getString("ArtistName", "");
        final String copyrightInformation = prefs.getString("CopyrightInformation", "");
        final String exiftoolPath = prefs.getString("ExiftoolPath", "");
        final String picturesPath = prefs.getString("PicturesPath", "");
        final boolean ignoreWarnings = prefs.getBoolean("IgnoreWarnings", false);

        final String ignoreWarningsOption = "-m";
        final String exiftoolCmd = "exiftool";
        final String artistTag = "-Artist=";
        final String copyrightTag = "-Copyright=";
        final String cameraMakeTag = "-Make=";
        final String cameraModelTag = "-Model=";
        final String lensMakeTag = "-LensMake=";
        final String lensModelTag = "-LensModel=";
        final String lensTag = "-Lens=";
        final String dateTag = "-DateTime=";
        final String dateTimeOriginalTag = "-DateTimeOriginal=";
        final String shutterTag = "-ShutterSpeedValue=";
        final String exposureTimeTag = "-ExposureTime=";
        final String apertureTag = "-ApertureValue=";
        final String fNumberTag = "-FNumber=";
        final String commentTag = "-UserComment=";
        final String imageDescriptionTag = "-ImageDescription=";
        final String gpsLatTag = "-GPSLatitude=";
        final String gpsLatRefTag = "-GPSLatitudeRef=";
        final String gpsLngTag = "-GPSLongitude=";
        final String gpsLngRefTag = "-GPSLongitudeRef=";
        final String exposureCompTag = "-ExposureCompensation=";
        final String focalLengthTag = "-FocalLength=";
        final String isoTag = "-ISO=";
        final String serialNumberTag = "-SerialNumber=";
        final String lensSerialNumberTag = "-LensSerialNumber=";
        final String flashTag = "-Flash=";
        final String lightSourceTag = "-LightSource=";

        String fileEnding = prefs.getString("FileEnding", ".jpg");
        //Check that fileEnding begins with a dot.
        if (fileEnding.charAt(0) != '.') fileEnding = "." + fileEnding;

        final String quote = "\"";
        final String space = " ";
        final String lineSep = "\r\n";

        final List<Frame> frameList = database.getAllFramesFromRoll(roll);
        final Camera camera = database.getCamera(roll.getCameraId());

        for (final Frame frame : frameList) {

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
                stringBuilder.append(cameraMakeTag).append(quote).append(camera.getMake())
                        .append(quote).append(space);
                //CameraModelTag
                stringBuilder.append(cameraModelTag).append(quote).append(camera.getModel())
                        .append(quote).append(space);
                //SerialNumber
                if (camera.getSerialNumber() != null && camera.getSerialNumber().length() > 0)
                    stringBuilder.append(serialNumberTag).append(quote).append(camera.getSerialNumber())
                            .append(quote).append(space);
            }
            if (lens != null) {
                //LensMakeTag
                stringBuilder.append(lensMakeTag).append(quote).append(lens.getMake()).append(quote).append(space);
                //LensModelTag
                stringBuilder.append(lensModelTag).append(quote).append(lens.getModel()).append(quote).append(space);
                //LensTag
                stringBuilder.append(lensTag).append(quote).append(lens.getMake()).append(space)
                        .append(lens.getModel()).append(quote).append(space);
                //LensSerialNumber
                if (lens.getSerialNumber() != null && lens.getSerialNumber().length() > 0)
                    stringBuilder.append(lensSerialNumberTag).append(quote).append(lens.getSerialNumber())
                            .append(quote).append(space);
            }
            if (frame.getDate() != null) {
                //DateTime
                stringBuilder.append(dateTag).append(quote).append(frame.getDate()
                        .replace("-", ":")).append(quote).append(space);
                //DateTimeOriginal
                stringBuilder.append(dateTimeOriginalTag).append(quote).append(frame.getDate()
                        .replace("-", ":")).append(quote).append(space);
            }
            //ShutterSpeedValue & ExposureTime
            if (frame.getShutter() != null) {
                stringBuilder.append(shutterTag).append(quote).append(frame.getShutter()
                        .replace("\"", "")).append(quote).append(space);
                stringBuilder.append(exposureTimeTag).append(quote).append(frame.getShutter()
                        .replace("\"", "")).append(quote).append(space);
            }

            //ApertureValue & FNumber
            if (frame.getAperture() != null) {
                stringBuilder.append(apertureTag).append(quote).append(frame.getAperture()).append(quote).append(space);
                stringBuilder.append(fNumberTag).append(quote).append(frame.getAperture()).append(quote).append(space);
            }
            //UserComment & ImageDescription
            if (frame.getNote() != null && frame.getNote().length() > 0) {
                stringBuilder.append(commentTag).append(quote)
                        .append(Normalizer.normalize(frame.getNote(), Normalizer.Form.NFC)
                                .replace("\"", "'")).append(quote).append(space);
                stringBuilder.append(imageDescriptionTag).append(quote)
                        .append(Normalizer.normalize(frame.getNote(), Normalizer.Form.NFC)
                                .replace("\"", "'")).append(quote).append(space);
            }
            //GPSLatitude & GPSLongitude & GPSLatitudeRef & GPSLongitudeRef
            if (frame.getLocation() != null && frame.getLocation().length() > 0) {
                String latString = frame.getLocation().substring(0, frame.getLocation().indexOf(" "));
                String lngString = frame.getLocation().substring(frame.getLocation().indexOf(" ") + 1);
                final String latRef;
                if (latString.substring(0, 1).equals("-")) {
                    latRef = "S";
                    latString = latString.substring(1);
                } else latRef = "N";
                final String lngRef;
                if (lngString.substring(0, 1).equals("-")) {
                    lngRef = "W";
                    lngString = lngString.substring(1);
                } else lngRef = "E";
                latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
                final List<String> latStringList = Arrays.asList(latString.split(":"));
                lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
                final List<String> lngStringList = Arrays.asList(lngString.split(":"));

                stringBuilder.append(gpsLatTag).append(quote).append(latStringList.get(0))
                        .append(space).append(latStringList.get(1)).append(space)
                        .append(latStringList.get(2)).append(quote).append(space);
                stringBuilder.append(gpsLatRefTag).append(quote).append(latRef).append(quote).append(space);
                stringBuilder.append(gpsLngTag).append(quote).append(lngStringList.get(0))
                        .append(space).append(lngStringList.get(1)).append(space)
                        .append(lngStringList.get(2)).append(quote).append(space);
                stringBuilder.append(gpsLngRefTag).append(quote).append(lngRef).append(quote).append(space);
            }
            //ExposureCompensation
            if (frame.getExposureComp() != null) stringBuilder.append(exposureCompTag)
                    .append(quote).append(frame.getExposureComp()).append(quote).append(space);
            //FocalLength
            if (frame.getFocalLength() > 0) stringBuilder.append(focalLengthTag).append(quote)
                    .append(frame.getFocalLength()).append(quote).append(space);
            //ISO
            if (roll.getIso() > 0) stringBuilder.append(isoTag).append(quote).append(roll.getIso())
                    .append(quote).append(space);
            // Flash
            if (frame.getFlashUsed()) stringBuilder.append(flashTag).append(quote)
                    .append("1").append(quote).append(space);
            // Light source
            final int lightSource;
            switch (frame.getLightSource()) {
                case 1: lightSource = 1;    break; // Daylight
                case 2: lightSource = 9;    break; // Sunny
                case 3: lightSource = 10;   break; // Cloudy
                case 4: lightSource = 11;   break; // Shade
                case 5: lightSource = 2;    break; // Fluorescent
                case 6: lightSource = 3;    break; // Tungsten
                case 7: lightSource = 4;    break; // Flash
                case 0: default: lightSource = 0;  // Unknown
            }
            stringBuilder.append(lightSourceTag).append(quote)
                    .append(lightSource).append(quote).append(space);


            //Artist
            if (artistName.length() > 0) stringBuilder.append(artistTag).append(quote)
                    .append(artistName).append(quote).append(space);
            //Copyright
            if (copyrightInformation.length() > 0) stringBuilder.append(copyrightTag).append(quote)
                    .append(copyrightInformation).append(quote).append(space);
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
    public static String createCsvString(final Context context, final Roll roll) {

        final FilmDbHelper database = FilmDbHelper.getInstance(context);
        final List<Frame> frameList = database.getAllFramesFromRoll(roll);
        final Camera camera = database.getCamera(roll.getCameraId());
        final FilmStock filmStock = database.getFilmStock(roll.getFilmStockId());

        final String separator = ",";
        final String separatorReplacement = ";";
        final StringBuilder stringBuilder = new StringBuilder();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String artistName = prefs.getString("ArtistName", "");
        final String copyrightInformation = prefs.getString("CopyrightInformation", "");

        //Roll and camera information
        stringBuilder.append("Roll name: ").append(roll.getName()).append("\n");
        stringBuilder.append("Added: ").append(roll.getDate()).append("\n");
        stringBuilder.append("Film stock: ").append(filmStock != null ? filmStock.getName() : "").append("\n");
        stringBuilder.append("ISO: ").append(roll.getIso()).append("\n");
        stringBuilder.append("Format: ").append(context.getResources()
                .getStringArray(R.array.FilmFormats)[roll.getFormat()]).append("\n");
        stringBuilder.append("Push/pull: ").append(roll.getPushPull()).append("\n");
        stringBuilder.append("Camera: ").append(camera != null ? camera.getName() : "").append("\n");
        stringBuilder.append("Serial number: ")
                .append(camera != null && camera.getSerialNumber() != null ? camera.getSerialNumber() : "").append("\n");
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
                .append("Address").append(separator)
                .append("Flash").append(separator)
                .append("Light source")
                .append("\n");

        for (final Frame frame : frameList) {

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
                String lngString = frame.getLocation().substring(frame.getLocation().indexOf(" ") + 1);
                final String latRef;
                if (latString.substring(0, 1).equals("-")) {
                    latRef = "S";
                    latString = latString.substring(1);
                } else latRef = "N";
                final String lngRef;
                if (lngString.substring(0, 1).equals("-")) {
                    lngRef = "W";
                    lngString = lngString.substring(1);
                } else lngRef = "E";
                latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
                final List<String> latStringList = Arrays.asList(latString.split(":"));
                lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
                final List<String> lngStringList = Arrays.asList(lngString.split(":"));

                final String space = " ";

                stringBuilder.append(latStringList.get(0)).append("°").append(space)
                        .append(latStringList.get(1)).append("\'").append(space)
                        .append(latStringList.get(2).replace(',', '.'))
                        .append("\"").append(space);

                stringBuilder.append(latRef).append(space);

                stringBuilder.append(lngStringList.get(0)).append("°").append(space)
                        .append(lngStringList.get(1)).append("\'").append(space)
                        .append(lngStringList.get(2).replace(',', '.'))
                        .append("\"").append(space);

                stringBuilder.append(lngRef);
            }
            stringBuilder.append(separator);

            //Address
            if (frame.getFormattedAddress() != null && frame.getFormattedAddress().length() > 0) {
                final String formattedAddress = frame.getFormattedAddress();
                // Replace commas with semicolons, because comma is reserved for separator
                stringBuilder.append(formattedAddress.replace(separator, separatorReplacement));
            }
            stringBuilder.append(separator);

            // Flash
            stringBuilder.append(frame.getFlashUsed());
            stringBuilder.append(separator);

            // Light source
            final String[] lightSources = context.getResources().getStringArray(R.array.LightSource);
            try {
                stringBuilder.append(lightSources[frame.getLightSource()]);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                stringBuilder.append("Error");
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
    public static String getReadableLocationFromString(final String location) {

        //If the location is empty, return null
        if (location == null || location.length() == 0) return null;

        final StringBuilder stringBuilder = new StringBuilder();

        String latString = location.substring(0, location.indexOf(" "));
        String lngString = location.substring(location.indexOf(" ") + 1);

        final String latRef;
        if (latString.substring(0, 1).equals("-")) {
            latRef = "S";
            latString = latString.substring(1);
        } else latRef = "N";

        final String lngRef;
        if (lngString.substring(0, 1).equals("-")) {
            lngRef = "W";
            lngString = lngString.substring(1);
        } else lngRef = "E";

        latString = Location.convert(Double.parseDouble(latString), Location.FORMAT_SECONDS);
        final List<String> latStringList = Arrays.asList(latString.split(":"));

        lngString = Location.convert(Double.parseDouble(lngString), Location.FORMAT_SECONDS);
        final List<String> lngStringList = Arrays.asList(lngString.split(":"));

        final String space = " ";

        stringBuilder.append(latStringList.get(0)).append("°").append(space)
                .append(latStringList.get(1)).append("\'").append(space)
                .append(latStringList.get(2).replace(',', '.'))
                .append("\"").append(space);
        stringBuilder.append(latRef).append(space);
        stringBuilder.append(lngStringList.get(0)).append("°").append(space)
                .append(lngStringList.get(1)).append("\'").append(space)
                .append(lngStringList.get(2).replace(',', '.'))
                .append("\"").append(space);
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
        final int year = c.get(Calendar.YEAR);
        final int month = c.get(Calendar.MONTH) + 1;
        final int day = c.get(Calendar.DAY_OF_MONTH);
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int minute = c.get(Calendar.MINUTE);
        final String currentTime;
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
    public static void copyFile(final File fromFile, final File toFile) throws IOException {
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

    public static void setColorFilter(@NonNull final Drawable drawable, final int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.setColorFilter(new BlendModeColorFilter(color, BlendMode.SRC_IN));
        } else {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

}
