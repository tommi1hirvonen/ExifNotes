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
import com.tommihirvonen.exifnotes.datastructures.DateTime;
import com.tommihirvonen.exifnotes.datastructures.FilmStock;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.FrameSortMode;
import com.tommihirvonen.exifnotes.datastructures.Gear;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.RollSortMode;

import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
        return preferences.getBoolean(PreferenceConstants.KEY_DARK_THEME, false);
    }

    /**
     * This function is used to fix a bug which Google hasn't been able to fix since 2012.
     * https://issuetracker.google.com/issues/36952035
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
     * This function deletes all the files in a directory
     *
     * @param directory the directory whose files are to be deleted
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void purgeDirectory(final File directory) {
        // Return if the given File is null
        // (for example no read/write access or storage is not mounted).
        if (directory == null) return;
        final File[] files = directory.listFiles();
        if (files == null) return;
        for (final File file : files) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    /**
     * Class which manages custom Android Marshmallow type scroll indicators based on a RecyclerView
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
     * Sorts a list of Frame objects based on sortMode.
     * This method is called when the user has selected a sorting criteria.
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
                    final DateTime dt1 = frame1.getDate();
                    final DateTime dt2 = frame2.getDate();
                    if (dt1 != null && dt2 != null) {
                        return dt1.compareTo(dt2);
                    } else {
                        return 0;
                    }
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
     * Sorts a list of Roll objects based on sortMode.
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
                    final DateTime dt1 = roll1.getDate();
                    final DateTime dt2 = roll2.getDate();
                    if (dt1 != null & dt2 != null) {
                        // Change the sign to make the order descending.
                        // This is used for rolls.
                        return -dt1.compareTo(dt2);
                    } else {
                        return 0;
                    }
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
     * Sorts a list of Gear by name.
     *
     * @param gearList reference to the List that should be sorted.
     */
    public static void sortGearList(final List<? extends Gear> gearList) {
        Collections.sort(gearList, (Comparator<Gear>) (g1, g2) ->
                g1.getName().compareToIgnoreCase(g2.getName())
        );
    }

    /**
     * Creates a string containing the ExifTool commands for the frames of the specified roll.
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
                stringBuilder.append(dateTag).append(quote).append(frame.getDate().getDateTimeAsText()
                        .replace("-", ":")).append(quote).append(space);
                //DateTimeOriginal
                stringBuilder.append(dateTimeOriginalTag).append(quote).append(frame.getDate().getDateTimeAsText()
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
                                .replace("\"", "'")
                                .replace("\n", " ")).append(quote).append(space);
                stringBuilder.append(imageDescriptionTag).append(quote)
                        .append(Normalizer.normalize(frame.getNote(), Normalizer.Form.NFC)
                                .replace("\"", "'")
                                .replace("\n", " ")).append(quote).append(space);
            }
            //GPSLatitude & GPSLongitude & GPSLatitudeRef & GPSLongitudeRef
            if (frame.getLocation() != null && frame.getLocation().getExifToolLocation() != null) {
                stringBuilder.append(frame.getLocation().getExifToolLocation());
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
                    .append("Fired").append(quote).append(space);
            // Light source
            final String lightSource;
            switch (frame.getLightSource()) {
                case 1: lightSource = "Daylight";    break; // Daylight
                case 2: lightSource = "Fine Weather";    break; // Sunny
                case 3: lightSource = "Cloudy";   break; // Cloudy
                case 4: lightSource = "Shade";   break; // Shade
                case 5: lightSource = "Fluorescent";    break; // Fluorescent
                case 6: lightSource = "Tungsten";    break; // Tungsten
                case 7: lightSource = "Flash";    break; // Flash
                case 0: default: lightSource = "Unknown";  // Unknown
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
     * Creates a string which contains csv information about the roll.
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
        final StringEscapeUtils.Builder stringBuilder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_CSV);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String artistName = prefs.getString("ArtistName", "");
        final String copyrightInformation = prefs.getString("CopyrightInformation", "");

        //Roll and camera information
        stringBuilder.append("Roll name: ").append(roll.getName()).append("\n");
        stringBuilder.append("Loaded on: ").append(roll.getDate() != null ? roll.getDate().getDateTimeAsText() : "").append("\n");
        stringBuilder.append("Unloaded on: ")
                .append(roll.getUnloaded() != null ? roll.getUnloaded().getDateTimeAsText() : "").append("\n");
        stringBuilder.append("Developed on: ")
                .append(roll.getDeveloped() != null ? roll.getDeveloped().getDateTimeAsText() : "").append("\n");
        stringBuilder.append("Film stock: ").append(filmStock != null ? filmStock.getName() : "").append("\n");
        stringBuilder.append("ISO: ").append(String.valueOf(roll.getIso())).append("\n");
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
            if (frame.getLensId() > 0) {
                lens = database.getLens(frame.getLensId());
            }

            //FrameCount
            stringBuilder.append(String.valueOf(frame.getCount()));
            stringBuilder.append(separator);

            //Date
            stringBuilder.append(frame.getDate() != null ? frame.getDate().getDateTimeAsText() : "");
            stringBuilder.append(separator);

            //Lens make and model
            if (lens != null) {
                stringBuilder.escape(lens.getMake()).append(" ").escape(lens.getModel());
            }
            stringBuilder.append(separator);

            //Lens serial number
            if (lens != null && lens.getSerialNumber() != null && lens.getSerialNumber().length() > 0) {
                stringBuilder.escape(lens.getSerialNumber());
            }
            stringBuilder.append(separator);

            // /Shutter speed
            if (frame.getShutter() != null) {
                stringBuilder.append(frame.getShutter());
            }
            stringBuilder.append(separator);

            //Aperture
            if (frame.getAperture() != null) {
                stringBuilder.append("f").append(frame.getAperture());
            }
            stringBuilder.append(separator);

            //Focal length
            if (frame.getFocalLength() > 0) {
                stringBuilder.append(String.valueOf(frame.getFocalLength()));
            }
            stringBuilder.append(separator);

            //Exposure compensation
            if (frame.getExposureComp() != null && frame.getExposureComp().length() > 1) {
                stringBuilder.append(frame.getExposureComp());
            }
            stringBuilder.append(separator);

            //Note
            if (frame.getNote() != null && frame.getNote().length() > 0) {
                stringBuilder.escape(frame.getNote());
            }
            stringBuilder.append(separator);

            //Number of exposures
            stringBuilder.append(String.valueOf(frame.getNoOfExposures()));
            stringBuilder.append(separator);

            //Filters
            if (frame.getFilters().size() > 0) {
                final StringBuilder filterBuilder = new StringBuilder();
                for (int i = 0; i < frame.getFilters().size(); ++i) {
                    filterBuilder.append(frame.getFilters().get(i).getName());
                    if (i < frame.getFilters().size() - 1) filterBuilder.append("|");
                }
                stringBuilder.escape(filterBuilder.toString());
            }
            stringBuilder.append(separator);

            //Location
            if (frame.getLocation() != null) {
                final String location = frame.getLocation().getReadableLocation();
                if (location != null) stringBuilder.escape(location);
            }
            stringBuilder.append(separator);

            //Address
            if (frame.getFormattedAddress() != null && frame.getFormattedAddress().length() > 0) {
                stringBuilder.escape(frame.getFormattedAddress());
            }
            stringBuilder.append(separator);

            // Flash
            stringBuilder.append(String.valueOf(frame.getFlashUsed()));
            stringBuilder.append(separator);

            // Light source
            final String[] lightSources = context.getResources().getStringArray(R.array.LightSource);
            try {
                stringBuilder.escape(lightSources[frame.getLightSource()]);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                stringBuilder.append("Error");
            }

            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
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
    static void copyFile(final File fromFile, final File toFile) throws IOException {
        // Check that the destination folder exists. Create if not.
        if (toFile.getParentFile() != null && !toFile.getParentFile().exists()) {
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

    /**
     * Applies a color filter to a Drawable object.
     *
     * @param drawable the object that should be colored
     * @param color the color that should be used in the form 0xAARRGGBB
     */
    public static void setColorFilter(@NonNull final Drawable drawable, final int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.setColorFilter(new BlendModeColorFilter(color, BlendMode.SRC_IN));
        } else {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

}
