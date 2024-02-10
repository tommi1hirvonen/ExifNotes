/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.PreferenceActivity
import com.tommihirvonen.exifnotes.data.Database
import com.tommihirvonen.exifnotes.preferences.*
import com.tommihirvonen.exifnotes.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import java.io.*
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * PreferenceFragment is shown in PreferenceActivity.
 * It is responsible for displaying all the preference options.
 */
@AndroidEntryPoint
class PreferenceFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var database: Database

    private inner class ExportPictures : CreateDocument("application/zip") {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/zip"
            return intent
        }
    }

    private inner class ExportDatabase : CreateDocument("*/*") {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            return intent
        }
    }

    private val exportPicturesResultLauncher =
        registerForActivityResult(ExportPictures()) { resultUri ->
            resultUri?.let { exportComplementaryPictures(it) }
        }

    private val importPicturesResultLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
            resultUri?.let { importComplementaryPictures(it) }
        }

    private val exportDatabaseResultLauncher =
        registerForActivityResult(ExportDatabase()) { resultUri ->
            resultUri?.let { exportDatabase(it) }
        }

    private val importDatabaseResultLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { resultUri ->
            resultUri?.let { importDatabase(it) }
        }

    private var currentListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun onResume() {
        super.onResume()
        currentListener = preferenceChangeListener
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(currentListener)
    }

    override fun onPause() {
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(currentListener)
        currentListener = null
        super.onPause()
    }

    /**
     * Creates a new OnSharedPreferenceChangeListener to detect app theme changes.
     */
    private val preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener get() =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences, key: String? ->
            if (key == PreferenceConstants.KEY_APP_THEME) {
                val appThemePreference = findPreference<ListPreference>(PreferenceConstants.KEY_APP_THEME)
                appThemePreference?.summary = appThemePreference?.entry
                when (appThemePreference?.value) {
                    "DEFAULT" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    "LIGHT" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "DARK" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notice that all we need to do is invoke the addPreferencesFromResource(..) method,
        // where we simply provide the reference to the preferences.xml file
        // and Android takes care of the rest for rendering the activity
        // and also saving the values for you.
        addPreferencesFromResource(R.xml.fragment_preference)

        // Update the summary for the theme preference.
        val appThemePreference = findPreference<ListPreference>(PreferenceConstants.KEY_APP_THEME)
        appThemePreference?.summary = appThemePreference?.entry

        // OnClickListener to start complementary pictures export.
        val exportComplementaryPictures = findPreference<Preference>(PreferenceConstants.KEY_EXPORT_COMPLEMENTARY_PICTURES)!!
        exportComplementaryPictures.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val date = LocalDateTime.now().sortableDate
            val title = "Exif_Notes_Complementary_Pictures_$date.zip"
            exportPicturesResultLauncher.launch(title)
            true
        }

        // OnClickListener to start complementary pictures import
        val importComplementaryPictures = findPreference<Preference>(PreferenceConstants.KEY_IMPORT_COMPLEMENTARY_PICTURES)!!
        importComplementaryPictures.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Show additional message about importing complementary pictures using a separate dialog.
            val builder = MaterialAlertDialogBuilder(requireActivity())
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
            val builder = MaterialAlertDialogBuilder(requireActivity())
            builder.setTitle(R.string.ExportDatabaseTitle)
            builder.setMessage(R.string.ExportDatabaseVerification)
            builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                val date = LocalDateTime.now().sortableDate
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
            val builder = MaterialAlertDialogBuilder(requireActivity())
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
            is AboutDialogPreference -> { showAboutDialog() }
            is HelpDialogPreference -> { showHelpDialog() }
            is VersionHistoryDialogPreference -> { showVersionHistoryDialog() }
            is PrivacyPolicyDialogPreference -> { showPrivacyPolicyDialog() }
            is ThirdPartyLicensesDialogPreference -> { showThirdPartyLicensesDialog() }
            is LicenseDialogPreference -> { showLicenseDialog() }
            else -> { super.onDisplayPreferenceDialog(preference) }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    private fun importComplementaryPictures(picturesUri: Uri) {
        val data = Data.Builder()
            .putString(ExtraKeys.TARGET_URI, picturesUri.toString())
            .build()
        val request = OneTimeWorkRequestBuilder<ComplementaryPicturesImportWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(requireContext()).enqueue(request)
        view?.snackbar(R.string.StartedImportingComplementaryPictures)
    }

    private fun importDatabase(databaseUri: Uri) {
        try {
            // Copy the content from the Uri to a cached File so it can be read as a File.

            // Check the extension of the given file.
            val cursor = requireContext().contentResolver.query(databaseUri,
                    null, null, null, null)
            cursor!!.moveToFirst()
            val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            if (File(name).extension != "db") {
                view?.snackbar("Not a valid .db file!")
                return
            }
            cursor.close()

            // Copy file for database import.
            val inputStream = requireContext().contentResolver.openInputStream(databaseUri)
            val outputDir = requireContext().externalCacheDir
            val outputFile = File.createTempFile("database", ".db", outputDir)
            val outputStream: OutputStream = FileOutputStream(outputFile)
            inputStream!!.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            val filePath = outputFile.absolutePath
            val extension = outputFile.extension

            //If the length of filePath is 0, then the user canceled the import.
            if (filePath.isNotEmpty() && extension == "db") {
                val importSuccess: Boolean = try {
                    database.importDatabase(requireActivity(), filePath)
                } catch (e: IOException) {
                    val message = resources.getString(R.string.ErrorImportingDatabaseFrom) +
                            filePath
                    view?.snackbar(message)
                    return
                }
                if (importSuccess) {

                    // Set the parent activity's result code
                    val preferenceActivity = activity as PreferenceActivity?
                    var result = preferenceActivity!!.resultCode

                    // Preserve the previously put result code(s)
                    result = result or PreferenceActivity.RESULT_DATABASE_IMPORTED
                    preferenceActivity.resultCode = result
                    view?.snackbar(R.string.DatabaseImported)
                } else {
                    view?.snackbar("Import failed")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun exportComplementaryPictures(picturesUri: Uri) {
        val complementaryPictureFilenames = database.complementaryPictureFilenames.toTypedArray()
        val data = Data.Builder()
            .putString(ExtraKeys.TARGET_URI, picturesUri.toString())
            .putStringArray(ExtraKeys.FILENAMES, complementaryPictureFilenames)
            .build()
        val request = OneTimeWorkRequestBuilder<ComplementaryPicturesExportWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(requireContext()).enqueue(request)
        view?.snackbar(R.string.StartedExportingComplementaryPictures)
    }

    private fun exportDatabase(destinationUri: Uri) {
        try {
            val outputStream = requireContext().contentResolver.openOutputStream(destinationUri)
            val databaseFile = Database.getDatabaseFile(requireActivity())
            val inputStream: InputStream = FileInputStream(databaseFile)
            inputStream.copyTo(outputStream!!)
            inputStream.close()
            outputStream.close()
            view?.snackbar(R.string.DatabaseCopiedSuccessfully)
        } catch (e: IOException) {
            e.printStackTrace()
            view?.snackbar(R.string.ErrorExportingDatabase)
        }
    }

    private fun showAboutDialog() {
        val title = this.resources.getString(R.string.app_name)
        val versionInfo = requireContext().packageInfo
        val versionName = if (versionInfo != null) versionInfo.versionName else ""
        val about = this.resources.getString(R.string.AboutAndTermsOfUse, versionName)
        GeneralMaterialDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(about)
            .create()
            .show()
    }

    private fun showVersionHistoryDialog() {
        val title = this.resources.getString(R.string.VersionHistory)
        val versionHistory = this.resources.getString(R.string.VersionHistoryStatement)
        GeneralMaterialDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(versionHistory)
            .create()
            .show()
    }

    private fun showHelpDialog() {
        val title = resources.getString(R.string.Help)
        val message = resources.getString(R.string.main_help)
        GeneralMaterialDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .create()
            .show()
    }

    private fun showPrivacyPolicyDialog() {
        val title = resources.getString(R.string.PrivacyPolicy)
        val message = resources.getText(R.string.PrivacyPolicyStatement)
        GeneralMaterialDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .create()
            .show()
    }

    private fun showLicenseDialog() {
        val webView = WebView(requireContext())
        webView.loadUrl("file:///android_asset/license.html")
        GeneralDialogBuilder(requireContext())
            .setView(webView)
            .create()
            .show()
    }

    private fun showThirdPartyLicensesDialog() {
        val webView = WebView(requireContext())
        // Interactive html file containing notices/licenses for used dependencies.
        // The file is generated using the Gradle plugin com.jaredsburrows.license.
        webView.loadUrl("file:///android_asset/open_source_licenses.html")
        GeneralDialogBuilder(requireContext())
            .setView(webView)
            .create()
            .show()
    }

}