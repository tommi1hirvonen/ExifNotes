package com.tommihirvonen.exifnotes.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.PreferenceActivity
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.*

/**
 * PreferenceFragment is shown in PreferenceActivity.
 * It is responsible for displaying all the preference options.
 */
class PreferenceFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    private inner class ExportPictures : ActivityResultContracts.CreateDocument() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/zip"
            return intent
        }
    }

    private inner class ExportDatabase : ActivityResultContracts.CreateDocument() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            return intent
        }
    }

    private val exportPicturesResultLauncher = registerForActivityResult(ExportPictures()) { resultUri ->
        resultUri?.let { exportComplementaryPictures(it) }
    }

    private val importPicturesResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
        resultUri?.let { importComplementaryPictures(it) }
    }

    private val exportDatabaseResultLauncher = registerForActivityResult(ExportDatabase()) { resultUri ->
        resultUri?.let { exportDatabase(it) }
    }

    private val importDatabaseResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
        resultUri?.let { importDatabase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notice that all we need to do is invoke the addPreferencesFromResource(..) method,
        // where we simply provide the reference to the preferences.xml file
        // and Android takes care of the rest for rendering the activity
        // and also saving the values for you.
        addPreferencesFromResource(R.xml.fragment_preference)

        // Set summaries
        val uiColor = findPreference<UIColorDialogPreference>(PreferenceConstants.KEY_UI_COLOR)!!
        uiColor.summary = uiColor.selectedColorName

        // OnClickListener to start complementary pictures export.
        val exportComplementaryPictures = findPreference<Preference>(PreferenceConstants.KEY_EXPORT_COMPLEMENTARY_PICTURES)!!
        exportComplementaryPictures.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val date = DateTime.fromCurrentTime().dateAsText
            val title = "Exif_Notes_Complementary_Pictures_$date.zip"
            exportPicturesResultLauncher.launch(title)
            true
        }

        // OnClickListener to start complementary pictures import
        val importComplementaryPictures = findPreference<Preference>(PreferenceConstants.KEY_IMPORT_COMPLEMENTARY_PICTURES)!!
        importComplementaryPictures.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Show additional message about importing complementary pictures using a separate dialog.
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.ImportDatabaseTitle)
            builder.setMessage(R.string.ImportComplementaryPicturesVerification)
            builder.setPositiveButton(R.string.Continue) { _: DialogInterface?, _: Int ->
                importPicturesResultLauncher.launch(arrayOf("application/zip"))
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            builder.create().show()
            true
        }

        // OnClickListener to start database export
        val exportDatabase = findPreference<Preference>(PreferenceConstants.KEY_EXPORT_DATABASE)!!
        exportDatabase.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Show additional message about exporting the SQL database using a separate dialog.
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.ExportDatabaseTitle)
            builder.setMessage(R.string.ExportDatabaseVerification)
            builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                val date = DateTime.fromCurrentTime().dateAsText
                val filename = "Exif_Notes_Database_$date.db"
                exportDatabaseResultLauncher.launch(filename)
            }
            builder.create().show()
            true
        }

        // OnClickListener to start database import
        val importDatabase = findPreference<Preference>(PreferenceConstants.KEY_IMPORT_DATABASE)!!
        importDatabase.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Show additional message about importing the database using a separate dialog.
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.ImportDatabaseTitle)
            builder.setMessage(R.string.ImportDatabaseVerification)
            builder.setPositiveButton(R.string.Continue) { _: DialogInterface?, _: Int ->
                importDatabaseResultLauncher.launch(arrayOf("*/*"))
            }
            builder.setNegativeButton(resources.getString(R.string.No)) { _: DialogInterface?, _: Int -> }
            builder.create().show()
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is UIColorDialogPreference -> {
                val dialogFragment = UIColorPreferenceDialogFragment(preference.getKey())
                // Here we can safely suppress deprecation warnings, because we aren't
                // actually communicating/passing data between two fragments.
                // PreferenceDialogFragmentCompat requires target fragment to be set,
                // which is we setTargetFragment() needs to be called. Currently there
                // seems to be no other workaround.
                @Suppress("DEPRECATION")
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(parentFragmentManager, null)
            }
            is AboutDialogPreference -> {
                Utilities.showAboutDialog(requireActivity())
            }
            is HelpDialogPreference -> {
                Utilities.showHelpDialog(requireActivity())
            }
            else -> {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    // TODO: Check that the exiftool and photos path end with a slash.
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == PreferenceConstants.KEY_DARK_THEME) {
            requireActivity().recreate()
            val preferenceActivity = requireActivity() as PreferenceActivity
            var resultCode = preferenceActivity.resultCode
            // Preserve previously put result code(s)
            resultCode = resultCode or PreferenceActivity.RESULT_THEME_CHANGED
            preferenceActivity.resultCode = resultCode
        }
    }

    private fun importComplementaryPictures(picturesUri: Uri) {
        val filePath: String = try {
            val inputStream = requireContext().contentResolver.openInputStream(picturesUri)
            val outputDir = requireContext().externalCacheDir
            val outputFile = File.createTempFile("pictures", ".zip", outputDir)
            val outputStream: OutputStream = FileOutputStream(outputFile)
            IOUtils.copy(inputStream, outputStream)
            inputStream!!.close()
            outputStream.close()
            outputFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val (success, completedEntries) = ComplementaryPicturesManager
                    .importComplementaryPictures(requireActivity(), File(filePath))

            if (success) {
                if (completedEntries == 0) {
                    Toast.makeText(activity, R.string.NoPicturesImported, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, resources.getQuantityString(R.plurals.ComplementaryPicturesImported, completedEntries, completedEntries), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, R.string.ErrorImportingComplementaryPictures, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importDatabase(databaseUri: Uri) {
        try {
            // Copy the content from the Uri to a cached File so it can be read as a File.

            // Check the extension of the given file.
            val cursor = requireContext().contentResolver.query(databaseUri,
                    null, null, null, null)
            cursor!!.moveToFirst()
            val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            if (FilenameUtils.getExtension(name) != "db") {
                Toast.makeText(context, "Not a valid .db file!", Toast.LENGTH_SHORT).show()
                return
            }
            cursor.close()

            // Copy file for database import.
            val inputStream = requireContext().contentResolver.openInputStream(databaseUri)
            val outputDir = requireContext().externalCacheDir
            val outputFile = File.createTempFile("database", ".db", outputDir)
            val outputStream: OutputStream = FileOutputStream(outputFile)
            IOUtils.copy(inputStream, outputStream)
            inputStream!!.close()
            outputStream.close()
            val filePath = outputFile.absolutePath
            val extension = FilenameUtils.getExtension(outputFile.name)

            //If the length of filePath is 0, then the user canceled the import.
            if (filePath.isNotEmpty() && extension == "db") {
                val database = database
                val importSuccess: Boolean = try {
                    database.importDatabase(requireActivity(), filePath)
                } catch (e: IOException) {
                    Toast.makeText(activity,
                            resources.getString(R.string.ErrorImportingDatabaseFrom) +
                                    filePath,
                            Toast.LENGTH_LONG).show()
                    return
                }
                if (importSuccess) {

                    // Set the parent activity's result code
                    val preferenceActivity = activity as PreferenceActivity?
                    var result = preferenceActivity!!.resultCode

                    // Preserve the previously put result code(s)
                    result = result or PreferenceActivity.RESULT_DATABASE_IMPORTED
                    preferenceActivity.resultCode = result
                    Toast.makeText(activity,
                            resources.getString(R.string.DatabaseImported),
                            Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun exportComplementaryPictures(picturesUri: Uri) {
        val outputStream = requireContext().contentResolver.openOutputStream(picturesUri)
        val inputDir = requireContext().externalCacheDir
        val inputFile = File.createTempFile("complementary_pictures", ".zip", inputDir)

        viewLifecycleOwner.lifecycleScope.launch {
            val (success, completedEntries) = ComplementaryPicturesManager
                    .exportComplementaryPictures(requireActivity(), inputFile)

            if (success) {
                if (completedEntries == 0) {
                    Toast.makeText(activity, R.string.NoPicturesExported, Toast.LENGTH_LONG).show()
                } else {
                    try {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        withContext(Dispatchers.IO) {
                            val inputStream: InputStream = FileInputStream(inputFile)
                            IOUtils.copy(inputStream, outputStream)
                            inputStream.close()
                            outputStream?.close()
                        }
                        Toast.makeText(activity, resources.getQuantityString(R.plurals.ComplementaryPicturesExported, completedEntries, completedEntries), Toast.LENGTH_LONG).show()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(activity, R.string.ErrorExportingComplementaryPictures, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(activity,
                        R.string.ErrorExportingComplementaryPictures, Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun exportDatabase(destinationUri: Uri) {
        try {
            val outputStream = requireContext().contentResolver.openOutputStream(destinationUri)
            val databaseFile = Database.getDatabaseFile(requireActivity())
            val inputStream: InputStream = FileInputStream(databaseFile)
            IOUtils.copy(inputStream, outputStream)
            inputStream.close()
            outputStream!!.close()
            Toast.makeText(activity,
                    resources.getString(R.string.DatabaseCopiedSuccessfully),
                    Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity,
                    resources.getString(R.string.ErrorExportingDatabase),
                    Toast.LENGTH_LONG).show()
        }
    }

}