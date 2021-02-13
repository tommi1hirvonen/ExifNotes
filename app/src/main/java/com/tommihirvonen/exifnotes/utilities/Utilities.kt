package com.tommihirvonen.exifnotes.utilities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.preference.DialogPreference
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Roll
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import javax.net.ssl.HttpsURLConnection

/**
 *
 * Function to set the ActionBar and StatusBar colours of an AppCompatActivity.
 * This function should be called in the onCreate() of every activity.
 *
 * @param displayHomeAsUp whether the back navigation icon should displayed in the ActionBar
 */
fun AppCompatActivity.setUiColor(displayHomeAsUp: Boolean) {
    supportActionBar?.let {
        it.displayOptions = ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_SHOW_TITLE
        it.elevation = 0f
        it.setDisplayHomeAsUpEnabled(displayHomeAsUp)
    }
    setSupportActionBarColor(primaryUiColor)
    setStatusBarColor(secondaryUiColor)
}

/**
 * Function to color the ActionBar of an AppCompatActivity
 *
 * @param color the color to which the ActionBar is colored
 */
fun AppCompatActivity.setSupportActionBarColor(color: Int) = supportActionBar?.setBackgroundDrawable(ColorDrawable(color))

/**
 * Function to set the status bar color
 *
 * @param color the color to be set to the status bar
 */
fun Activity.setStatusBarColor(color: Int) {
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = color
}

fun Fragment.setStatusBarColor(color: Int) = requireActivity().setStatusBarColor(color)

val Activity.packageInfo: PackageInfo? get() {
    try {
        return packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

val Context.primaryUiColor: Int get() = Color.parseColor(Utilities.getUiColorList(this)[0])

val Context.secondaryUiColor: Int get() = Color.parseColor(Utilities.getUiColorList(this)[1])
val Fragment.secondaryUiColor: Int get() = requireContext().secondaryUiColor

val Context.isAppThemeDark: Boolean get() =
    PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PreferenceConstants.KEY_DARK_THEME, false)

fun String.illegalCharsRemoved(): String = replace("[|\\\\?*<\":>/]".toRegex(), "_")

class AboutDialogPreference(context: Context?, attrs: AttributeSet?) : DialogPreference(context, attrs)

class HelpDialogPreference(context: Context?, attrs: AttributeSet?) : DialogPreference(context, attrs)

object Utilities {

    /**
     * @param coordinatesOrQuery Latitude and longitude coordinates in decimal format or a search query
     * @param apiKey Google Maps API key
     * @return Pair object where first is latitude and longitude in decimal format and second is
     * the formatted address
     */
    fun getGeocodeData(coordinatesOrQuery: String, apiKey: String): Pair<String, String> {
        val queryUrl = Uri.Builder() // Requests must be made over SSL.
                .scheme("https")
                .authority("maps.google.com")
                .appendPath("maps")
                .appendPath("api")
                .appendPath("geocode")
                .appendPath("json")
                // Use address parameter for both the coordinates and search string.
                .appendQueryParameter("address", coordinatesOrQuery)
                .appendQueryParameter("sensor", "false")
                // Use key parameter to pass the API key credentials.
                .appendQueryParameter("key", apiKey)
                .build().toString()

        try {
            val url = URL(queryUrl)
            val responseBuilder = StringBuilder()
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            val responseCode = conn.responseCode

            // If the connection was successful, add the connection result to the response string
            // one line at a time.
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                var line: String?
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                while (br.readLine().also { line = it } != null) {
                    responseBuilder.append(line)
                }
            }

            val jsonObject = JSONObject(responseBuilder.toString())
            val lng = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lng")
            val lat = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat")
            val formattedAddress = (jsonObject["results"] as JSONArray).getJSONObject(0)
                    .getString("formatted_address")

            return Pair("$lat $lng", formattedAddress)
        } catch (e: Exception) {
            return Pair("", "")
        }
    }

    /**
     * Shows a general dialog containing a title and a message.
     *
     * @param activity the calling activity
     * @param title the title of the dialog
     * @param message the message of the dialog
     */
    private fun showGeneralDialog(activity: Activity, title: String, message: String) {
        val generalDialogBuilder = AlertDialog.Builder(activity)
        generalDialogBuilder.setTitle(title)
        val spannableString = SpannableString(message)
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        generalDialogBuilder.setMessage(spannableString)
        generalDialogBuilder.setNegativeButton(R.string.Close) { _: DialogInterface?, _: Int -> }
        val generalDialog = generalDialogBuilder.create()
        generalDialog.show()
        //The dialog needs to be shown first. Otherwise textView will be null.
        val textView = generalDialog.findViewById<TextView>(android.R.id.message)
        textView.textSize = 14f
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    fun showAboutDialog(activity: Activity) {
        val title = activity.resources.getString(R.string.app_name)
        val versionInfo = activity.packageInfo
        val versionName = if (versionInfo != null) versionInfo.versionName else ""
        val about = activity.resources.getString(R.string.AboutAndTermsOfUse, versionName)
        val versionHistory = activity.resources.getString(R.string.VersionHistory)
        val message = "$about\n\n\n$versionHistory"
        showGeneralDialog(activity, title, message)
    }

    fun showHelpDialog(activity: Activity) {
        val title = activity.resources.getString(R.string.Help)
        val message = activity.resources.getString(R.string.main_help)
        showGeneralDialog(activity, title, message)
    }

    /**
     * Function to get a List containing the ui color codes in String format.
     *
     * @param context the base context of the application
     * @return List containing the ui color codes in String format
     */
    internal fun getUiColorList(context: Context): List<String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val uiColor = prefs.getString("UIColor", "#00838F,#006064") ?: "#00838F,#006064"
        return uiColor.split(",")
    }

    /**
     * This function deletes all the files in a directory
     *
     * @param directory the directory whose files are to be deleted
     */
    fun purgeDirectory(directory: File?) {
        // Return if the given File is null
        // (for example no read/write access or storage is not mounted).
        if (directory == null) return
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (!file.isDirectory) {
                file.delete()
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
    @SuppressLint("RtlHardcoded")
    fun buildCustomDialogTitleTextView(context: Context, titleText: String?): TextView {
        val titleTextView = TextView(context)
        TextViewCompat.setTextAppearance(titleTextView, android.R.style.TextAppearance_DeviceDefault_DialogWindowTitle)
        val dpi = context.resources.displayMetrics.density
        titleTextView.setPadding((20 * dpi).toInt(), (20 * dpi).toInt(), (20 * dpi).toInt(), (10 * dpi).toInt())
        titleTextView.text = titleText ?: ""
        titleTextView.gravity = Gravity.LEFT
        return titleTextView
    }

    /**
     * Creates a string containing the ExifTool commands for the frames of the specified roll.
     *
     * @param context application's context
     * @param roll Roll object of which the commands should be created
     * @return String containing the ExifTool commands
     */
    fun createExifToolCmdsString(context: Context, roll: Roll): String {
        val database = context.database
        val stringBuilder = StringBuilder()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val artistName = prefs.getString("ArtistName", "") ?: ""
        val copyrightInformation = prefs.getString("CopyrightInformation", "") ?: ""
        val exiftoolPath = prefs.getString("ExiftoolPath", "") ?: ""
        val picturesPath = prefs.getString("PicturesPath", "") ?: ""
        val ignoreWarnings = prefs.getBoolean("IgnoreWarnings", false)
        val ignoreWarningsOption = "-m"
        val exiftoolCmd = "exiftool"
        val artistTag = "-Artist="
        val copyrightTag = "-Copyright="
        val cameraMakeTag = "-Make="
        val cameraModelTag = "-Model="
        val lensMakeTag = "-LensMake="
        val lensModelTag = "-LensModel="
        val lensTag = "-Lens="
        val dateTag = "-DateTime="
        val dateTimeOriginalTag = "-DateTimeOriginal="
        val shutterTag = "-ShutterSpeedValue="
        val exposureTimeTag = "-ExposureTime="
        val apertureTag = "-ApertureValue="
        val fNumberTag = "-FNumber="
        val commentTag = "-UserComment="
        val imageDescriptionTag = "-ImageDescription="
        val exposureCompTag = "-ExposureCompensation="
        val focalLengthTag = "-FocalLength="
        val isoTag = "-ISO="
        val serialNumberTag = "-SerialNumber="
        val lensSerialNumberTag = "-LensSerialNumber="
        val flashTag = "-Flash="
        val lightSourceTag = "-LightSource="
        var fileEnding = prefs.getString("FileEnding", ".jpg") ?: ".jpg"

        //Check that fileEnding begins with a dot.
        if (fileEnding.first() != '.') fileEnding = ".$fileEnding"

        val quote = "\""
        val space = " "
        val lineSep = "\r\n"
        val frameList = database.getAllFramesFromRoll(roll)
        val camera = roll.camera
        for (frame in frameList) {
            //ExifTool path
            if (exiftoolPath.isNotEmpty()) stringBuilder.append(exiftoolPath)
            //ExifTool command
            stringBuilder.append(exiftoolCmd).append(space)
            //Ignore warnings
            if (ignoreWarnings) stringBuilder.append(ignoreWarningsOption).append(space)
            if (camera != null) {
                //CameraMakeTag
                stringBuilder.append(cameraMakeTag).append(quote).append(camera.make)
                        .append(quote).append(space)
                //CameraModelTag
                stringBuilder.append(cameraModelTag).append(quote).append(camera.model)
                        .append(quote).append(space)
                //SerialNumber
                val serialNumber = camera.serialNumber
                if (serialNumber?.isNotEmpty() == true) stringBuilder.append(serialNumberTag).append(quote).append(serialNumber)
                        .append(quote).append(space)
            }
            val lens = frame.lens
            if (lens != null) {
                //LensMakeTag
                stringBuilder.append(lensMakeTag).append(quote).append(lens.make).append(quote).append(space)
                //LensModelTag
                stringBuilder.append(lensModelTag).append(quote).append(lens.model).append(quote).append(space)
                //LensTag
                stringBuilder.append(lensTag).append(quote).append(lens.make).append(space)
                        .append(lens.model).append(quote).append(space)
                //LensSerialNumber
                val serialNumber = lens.serialNumber
                if (serialNumber?.isNotEmpty() == true) stringBuilder.append(lensSerialNumberTag).append(quote).append(serialNumber)
                        .append(quote).append(space)
            }
            val date = frame.date
            if (date != null) {
                //DateTime
                stringBuilder.append(dateTag).append(quote).append(date.dateTimeAsText
                        .replace("-", ":")).append(quote).append(space)
                //DateTimeOriginal
                stringBuilder.append(dateTimeOriginalTag).append(quote).append(date.dateTimeAsText
                        .replace("-", ":")).append(quote).append(space)
            }
            //ShutterSpeedValue & ExposureTime
            val shutter = frame.shutter
            if (shutter != null) {
                stringBuilder.append(shutterTag).append(quote).append(shutter
                        .replace("\"", "")).append(quote).append(space)
                stringBuilder.append(exposureTimeTag).append(quote).append(shutter
                        .replace("\"", "")).append(quote).append(space)
            }
            //ApertureValue & FNumber
            val aperture = frame.aperture
            if (aperture != null) {
                stringBuilder.append(apertureTag).append(quote).append(aperture).append(quote).append(space)
                stringBuilder.append(fNumberTag).append(quote).append(aperture).append(quote).append(space)
            }
            //UserComment & ImageDescription
            val note = frame.note
            if (note?.isNotEmpty() == true) {
                stringBuilder.append(commentTag).append(quote)
                        .append(Normalizer.normalize(note, Normalizer.Form.NFC)
                                .replace("\"", "'")
                                .replace("\n", " ")).append(quote).append(space)
                stringBuilder.append(imageDescriptionTag).append(quote)
                        .append(Normalizer.normalize(note, Normalizer.Form.NFC)
                                .replace("\"", "'")
                                .replace("\n", " ")).append(quote).append(space)
            }
            //GPSLatitude & GPSLongitude & GPSLatitudeRef & GPSLongitudeRef
            val location = frame.location
            if (location?.exifToolLocation != null) {
                stringBuilder.append(location.exifToolLocation)
            }
            //ExposureCompensation
            val exposureComp = frame.exposureComp
            if (exposureComp != null) stringBuilder.append(exposureCompTag)
                    .append(quote).append(exposureComp).append(quote).append(space)
            //FocalLength
            val focalLength = frame.focalLength
            if (focalLength > 0) stringBuilder.append(focalLengthTag).append(quote)
                    .append(focalLength).append(quote).append(space)
            //ISO
            val iso = roll.iso
            if (iso > 0) stringBuilder.append(isoTag).append(quote).append(iso)
                    .append(quote).append(space)
            // Flash
            if (frame.flashUsed) stringBuilder.append(flashTag).append(quote)
                    .append("Fired").append(quote).append(space)
            // Light source
            val lightSource: String = when (frame.lightSource) {
                1 -> "Daylight"
                2 -> "Fine Weather"
                3 -> "Cloudy"
                4 -> "Shade"
                5 -> "Fluorescent"
                6 -> "Tungsten"
                7 -> "Flash"
                0 -> "Unknown" // Unknown
                else -> "Unknown"
            }
            stringBuilder.append(lightSourceTag).append(quote)
                    .append(lightSource).append(quote).append(space)

            //Artist
            if (artistName.isNotEmpty()) stringBuilder.append(artistTag).append(quote)
                    .append(artistName).append(quote).append(space)
            //Copyright
            if (copyrightInformation.isNotEmpty()) stringBuilder.append(copyrightTag).append(quote)
                    .append(copyrightInformation).append(quote).append(space)
            //Path to pictures
            if (picturesPath.contains(" ") || fileEnding.contains(" ")) stringBuilder.append(quote)
            if (picturesPath.isNotEmpty()) stringBuilder.append(picturesPath)
            //File ending
            stringBuilder.append("*_").append(frame.count).append(fileEnding)
            if (picturesPath.contains(" ") || fileEnding.contains(" ")) stringBuilder.append(quote)
            //Double new line
            stringBuilder.append(lineSep).append(lineSep)
        }
        return stringBuilder.toString()
    }

    /**
     * Creates a string which contains csv information about the roll.
     *
     * @param context application's context
     * @param roll Roll object from which the csv information should be created
     * @return String containing the csv information
     */
    fun createCsvString(context: Context, roll: Roll): String {
        val database = context.database
        val frameList = database.getAllFramesFromRoll(roll)
        val camera = roll.camera
        val filmStock = roll.filmStock
        val separator = ","
        val stringBuilder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_CSV)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val artistName = prefs.getString("ArtistName", "")
        val copyrightInformation = prefs.getString("CopyrightInformation", "")

        //Roll and camera information
        stringBuilder.append("Roll name: ").append(roll.name).append("\n")
        stringBuilder.append("Loaded on: ").append(roll.date?.dateTimeAsText ?: "").append("\n")
        stringBuilder.append("Unloaded on: ").append(roll.unloaded?.dateTimeAsText ?: "").append("\n")
        stringBuilder.append("Developed on: ").append(roll.developed?.dateTimeAsText ?: "").append("\n")
        stringBuilder.append("Film stock: ").append(filmStock?.name ?: "").append("\n")
        stringBuilder.append("ISO: ").append(roll.iso.toString()).append("\n")
        stringBuilder.append("Format: ").append(context.resources.getStringArray(R.array.FilmFormats)[roll.format]).append("\n")
        stringBuilder.append("Push/pull: ").append(roll.pushPull ?: "").append("\n")
        stringBuilder.append("Camera: ").append(camera?.name ?: "").append("\n")
        stringBuilder.append("Serial number: ").append(camera?.serialNumber ?: "").append("\n")
        stringBuilder.append("Notes: ").append(roll.note ?: "").append("\n")
        stringBuilder.append("Artist name: ").append(artistName).append("\n")
        stringBuilder.append("Copyright: ").append(copyrightInformation).append("\n")

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
                .append("\n")
        for (frame in frameList) {
            stringBuilder.append(frame.count.toString()).append(separator)
                    .append(frame.date?.dateTimeAsText ?: "").append(separator)
                    .escape(frame.lens?.name ?: "").append(separator)
                    .escape(frame.lens?.serialNumber ?: "").append(separator)
                    .append(frame.shutter ?: "").append(separator)

            frame.aperture?.let { stringBuilder.append("f").append(it) }
            stringBuilder.append(separator)

            if (frame.focalLength > 0) stringBuilder.append(frame.focalLength.toString())
            stringBuilder.append(separator)

            frame.exposureComp?.let { if (it.length > 1) stringBuilder.append(it) }
            stringBuilder.append(separator)

            stringBuilder.escape(frame.note ?: "").append(separator)
                    .append(frame.noOfExposures.toString()).append(separator)
                    .escape(frame.filters.joinToString(separator = "|") { it.name }).append(separator)
                    .escape(frame.location?.readableLocation ?: "").append(separator)
                    .escape(frame.formattedAddress ?: "").append(separator)
                    .append(frame.flashUsed.toString()).append(separator)

            val lightSources = context.resources.getStringArray(R.array.LightSource)
            try {
                stringBuilder.escape(lightSources[frame.lightSource])
            } catch (e: ArrayIndexOutOfBoundsException) {
                stringBuilder.append("Error")
            }
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    /**
     * Class which manages custom Android Marshmallow type scroll indicators based on a RecyclerView
     */
    class ScrollIndicatorRecyclerViewListener(private val recyclerView: RecyclerView,
                                              private val indicatorUp: View,
                                              private val indicatorDown: View) : RecyclerView.OnScrollListener() {

        init {
            recyclerView.post { toggleIndicators() }
        }

        private fun toggleIndicators() {
            // If we can't scroll upwards, hide the up scroll indicator. Otherwise show it.
            if (!recyclerView.canScrollVertically(-1)) {
                indicatorUp.visibility = View.INVISIBLE
            } else {
                indicatorUp.visibility = View.VISIBLE
            }
            // If we can't scroll down, hide the down scroll indicator. Otherwise show it.
            if (!recyclerView.canScrollVertically(1)) {
                indicatorDown.visibility = View.INVISIBLE
            } else {
                indicatorDown.visibility = View.VISIBLE
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            toggleIndicators()
        }

    }

    /**
     * Class which manages custom Android Marshmallow type scroll indicators inside a NestedScrollView.
     */
    open class ScrollIndicatorNestedScrollViewListener(private val nestedScrollView: NestedScrollView,
                                                       private val indicatorUp: View?,
                                                       private val indicatorDown: View?) : NestedScrollView.OnScrollChangeListener {

        init {
            nestedScrollView.post { toggleIndicators() }
        }

        private fun toggleIndicators() {
            // If we can't scroll upwards, hide the up scroll indicator. Otherwise show it.

            // Using canScrollVertically methods only results in severe depression.
            // Instead we use getScrollY methods and avoid the headache entirely.
            // Besides, these methods work the same way on all devices.
            if (nestedScrollView.scrollY == 0) {
                indicatorUp?.visibility = View.INVISIBLE
            } else {
                indicatorUp?.visibility = View.VISIBLE
            }

            // If we can't scroll down, hide the down scroll indicator. Otherwise show it.

            // To get the actual height of the entire NestedScrollView, we have to do the following.
            // The ScrollView always has one child. Getting its height returns the true height
            // of the ScrollView.
            if (nestedScrollView.scrollY == nestedScrollView.getChildAt(0).height - nestedScrollView.height) {
                indicatorDown?.visibility = View.INVISIBLE
            } else {
                indicatorDown?.visibility = View.VISIBLE
            }
        }

        override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
            toggleIndicators()
        }

    }
}