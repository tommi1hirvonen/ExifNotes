package com.tommihirvonen.exifnotes.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.tommihirvonen.exifnotes.activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.UIColorDialogPreference;
import com.tommihirvonen.exifnotes.utilities.UIColorPreferenceDialogFragment;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * PreferenceFragment is shown in PreferenceActivity.
 * It is responsible for displaying all the preference options.
 */
public class PreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_IMPORT_COMPLEMENTARY_PICTURES = 1;

    private static final int REQUEST_IMPORT_DATABASE = 2;

    private static final int REQUEST_EXPORT_COMPLEMENTARY_PICTURES = 3;

    private static final int REQUEST_EXPORT_DATABASE = 4;

    /**
     * Get the preferences from resources. Set the UI and add listeners
     * for database export and import options.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notice that all we need to do is invoke the addPreferencesFromResource(..) method,
        // where we simply provide the reference to the preferences.xml file
        // and Android takes care of the rest for rendering the activity
        // and also saving the values for you.
        addPreferencesFromResource(R.xml.fragment_preference);

        // Set summaries
        final UIColorDialogPreference UIColor = findPreference(PreferenceConstants.KEY_UI_COLOR);
        UIColor.setSummary(UIColor.getSelectedColorName());

        // OnClickListener to start complementary pictures export.
        final Preference exportComplementaryPictures = findPreference(PreferenceConstants.KEY_EXPORT_COMPLEMENTARY_PICTURES);
        exportComplementaryPictures.setOnPreferenceClickListener(preference -> {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            final String date = Utilities.getCurrentTime().split("\\s+")[0];
            final String title = "Exif_Notes_Complementary_Pictures_" + date + ".zip";
            intent.putExtra(Intent.EXTRA_TITLE, title);
            startActivityForResult(intent, REQUEST_EXPORT_COMPLEMENTARY_PICTURES);
            return true;
        });

        // OnClickListener to start complementary pictures import
        final Preference importComplementaryPictures = findPreference(PreferenceConstants.KEY_IMPORT_COMPLEMENTARY_PICTURES);
        importComplementaryPictures.setOnPreferenceClickListener(preference -> {

            // Show additional message about importing complementary pictures using a separate dialog.
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.ImportDatabaseTitle);
            builder.setMessage(R.string.ImportComplementaryPicturesVerification);
            builder.setPositiveButton(R.string.Continue, (dialogInterface, i) -> {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                startActivityForResult(intent, REQUEST_IMPORT_COMPLEMENTARY_PICTURES);
            });
            builder.setNegativeButton(R.string.Cancel, (dialogInterface, i) -> {
                // Do nothing
            });
            builder.create().show();

            return true;
        });

        // OnClickListener to start database export
        final Preference exportDatabase = findPreference(PreferenceConstants.KEY_EXPORT_DATABASE);
        exportDatabase.setOnPreferenceClickListener(preference -> {

            // Show additional message about exporting the SQL database using a separate dialog.
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.ExportDatabaseTitle);
            builder.setMessage(R.string.ExportDatabaseVerification);
            builder.setPositiveButton(R.string.OK, (dialogInterface, i) -> {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                final String date = Utilities.getCurrentTime().split("\\s+")[0];
                final String filename = "Exif_Notes_Database_" + date + ".db";
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                startActivityForResult(intent, REQUEST_EXPORT_DATABASE);
            });
            builder.create().show();

            return true;
        });

        // OnClickListener to start database import
        final Preference importDatabase = findPreference(PreferenceConstants.KEY_IMPORT_DATABASE);
        importDatabase.setOnPreferenceClickListener(preference -> {

            // Show additional message about importing the database using a separate dialog.
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.ImportDatabaseTitle);
            builder.setMessage(R.string.ImportDatabaseVerification);
            builder.setPositiveButton(R.string.Continue, (dialog, which) -> {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_IMPORT_DATABASE);
            });
            builder.setNegativeButton(getResources().getString(R.string.No),
                    (dialog, which) -> {
                        //Do nothing
                    });
            builder.create().show();

            return true;
        });
    }

    @Override
    public void onDisplayPreferenceDialog(final Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof UIColorDialogPreference) {
            dialogFragment = UIColorPreferenceDialogFragment.newInstance(preference.getKey());
        }
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        switch (requestCode) {

            case REQUEST_IMPORT_COMPLEMENTARY_PICTURES:

                if (resultCode == Activity.RESULT_OK) {

                    final String filePath;

                    try {
                        final Uri picturesUri = data.getData();
                        final InputStream inputStream = getContext().getContentResolver()
                                .openInputStream(picturesUri);
                        final File outputDir = getContext().getExternalCacheDir();
                        final File outputFile = File.createTempFile("pictures", ".zip", outputDir);
                        final OutputStream outputStream = new FileOutputStream(outputFile);
                        IOUtils.copy(inputStream, outputStream);
                        inputStream.close();
                        outputStream.close();
                        filePath = outputFile.getAbsolutePath();
                    } catch (final IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    // Show a dialog with progress bar, elapsed time, completed zip entries and total zip entries.
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    @SuppressLint("InflateParams")
                    final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_progress, null);
                    builder.setView(view);
                    final ProgressBar progressBar = view.findViewById(R.id.progress_bar);
                    final TextView messageTextView = view.findViewById(R.id.textview_1);
                    messageTextView.setText(R.string.ImportingComplementaryPicturesPleaseWait);
                    final TextView progressTextView = view.findViewById(R.id.textview_2);
                    final Chronometer chronometer = view.findViewById(R.id.elapsed_time);
                    progressBar.setMax(100);
                    progressBar.setProgress(0);
                    progressTextView.setText("");
                    final AlertDialog dialog = builder.create();
                    dialog.setCancelable(false);
                    dialog.show();
                    chronometer.start();
                    ComplementaryPicturesManager.importComplementaryPictures(getActivity(),
                            new File(filePath),
                            new ComplementaryPicturesManager.ZipFileReaderAsyncTask.ProgressListener() {
                                @Override
                                public void onProgressChanged(final int progressPercentage,
                                                              final int completed, final int total) {
                                    progressBar.setProgress(progressPercentage);
                                    final String progressText = "" + completed + "/" + total;
                                    progressTextView.setText(progressText);
                                }

                                @Override
                                public void onCompleted(final boolean success, final int completedEntries) {
                                    dialog.dismiss();
                                    if (success) {
                                        if (completedEntries == 0)
                                            Toast.makeText(getActivity(),
                                                    R.string.NoPicturesImported, Toast.LENGTH_LONG).show();
                                        else
                                            Toast.makeText(getActivity(),
                                                    getResources().getQuantityString(
                                                            R.plurals.ComplementaryPicturesImported,
                                                            completedEntries, completedEntries),
                                                    Toast.LENGTH_LONG).show();
                                    }
                                    else Toast.makeText(getActivity(),
                                            R.string.ErrorImportingComplementaryPictures,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }

                break;



            case REQUEST_IMPORT_DATABASE:

                if (resultCode == Activity.RESULT_OK) {

                    try {
                        // Copy the content from the Uri to a cached File so it can be read as a File.
                        final Uri databaseUri = data.getData();

                        // Check the extension of the given file.
                        final Cursor cursor = getContext().getContentResolver().query(databaseUri,
                                null, null, null, null);
                        cursor.moveToFirst();
                        final String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        if (!FilenameUtils.getExtension(name).equals("db")) {
                            Toast.makeText(getContext(), "Not a valid .db file!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        cursor.close();

                        // Copy file for database import.
                        final InputStream inputStream = getContext().getContentResolver()
                                .openInputStream(databaseUri);
                        final File outputDir = getContext().getExternalCacheDir();
                        final File outputFile = File.createTempFile("database", ".db", outputDir);
                        final OutputStream outputStream = new FileOutputStream(outputFile);
                        IOUtils.copy(inputStream, outputStream);
                        inputStream.close();
                        outputStream.close();
                        final String filePath = outputFile.getAbsolutePath();
                        final String extension = FilenameUtils.getExtension(outputFile.getName());

                        //If the length of filePath is 0, then the user canceled the import.
                        if (filePath.length() > 0 && extension.equals("db")) {
                            final FilmDbHelper database = FilmDbHelper.getInstance(getActivity());
                            final boolean importSuccess;
                            try {
                                importSuccess = database.importDatabase(getActivity(), filePath);
                            } catch (final IOException e) {
                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.ErrorImportingDatabaseFrom) +
                                                filePath,
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (importSuccess) {

                                // Set the parent activity's result code
                                final PreferenceActivity preferenceActivity =
                                        (PreferenceActivity) getActivity();
                                int resultCode_ = preferenceActivity.getResultCode();

                                // Preserve the previously put result code(s)
                                resultCode_ = resultCode_ | PreferenceActivity.RESULT_DATABASE_IMPORTED;
                                preferenceActivity.setResultCode(resultCode_);

                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.DatabaseImported),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), "Import failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }

                }

                break;

            case REQUEST_EXPORT_COMPLEMENTARY_PICTURES:

                if (resultCode == Activity.RESULT_OK) {

                    try {
                        final Uri picturesUri = data.getData();
                        final OutputStream outputStream = getContext().getContentResolver()
                                .openOutputStream(picturesUri);
                        final File inputDir = getContext().getExternalCacheDir();
                        final File inputFile = File.createTempFile("complementary_pictures", ".zip", inputDir);

                        // Show a dialog with progress bar, elapsed time, completed zip entries and total zip entries.
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        @SuppressLint("InflateParams")
                        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_progress, null);
                        builder.setView(view);
                        final ProgressBar progressBar = view.findViewById(R.id.progress_bar);
                        final TextView messageTextView = view.findViewById(R.id.textview_1);
                        messageTextView.setText(R.string.ExportingComplementaryPicturesPleaseWait);
                        final TextView progressTextView = view.findViewById(R.id.textview_2);
                        final Chronometer chronometer = view.findViewById(R.id.elapsed_time);
                        progressBar.setMax(100);
                        progressBar.setProgress(0);
                        progressTextView.setText("");
                        final AlertDialog dialog = builder.create();
                        dialog.setCancelable(false);
                        dialog.show();
                        chronometer.start();
                        ComplementaryPicturesManager.exportComplementaryPictures(getActivity(), inputFile,
                                new ComplementaryPicturesManager.ZipFileCreatorAsyncTask.ProgressListener() {
                                @Override
                                public void onProgressChanged(final int progressPercentage,
                                                              final int completed, final int total) {
                                    progressBar.setProgress(progressPercentage);
                                    final String progressText = "" + completed + "/" + total;
                                    progressTextView.setText(progressText);
                                }
                                @Override
                                public void onCompleted(final boolean success, final int completedEntries,
                                                        final File zipFile) {
                                    dialog.dismiss();
                                    if (success) {
                                        if (completedEntries == 0) {
                                            Toast.makeText(getActivity(), R.string.NoPicturesExported,
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            try {
                                                final InputStream inputStream = new FileInputStream(zipFile);
                                                IOUtils.copy(inputStream, outputStream);
                                                inputStream.close();
                                                outputStream.close();
                                                Toast.makeText(getActivity(),
                                                        getResources().getQuantityString(
                                                                R.plurals.ComplementaryPicturesExported,
                                                                completedEntries, completedEntries), Toast.LENGTH_LONG).show();
                                            } catch (final IOException e) {
                                                e.printStackTrace();
                                                Toast.makeText(getActivity(),
                                                        R.string.ErrorExportingComplementaryPictures,
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    } else {
                                        Toast.makeText(getActivity(),
                                                R.string.ErrorExportingComplementaryPictures, Toast.LENGTH_LONG).show();
                                    }
                                }
                        });

                    } catch (final IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(),
                                R.string.ErrorExportingComplementaryPictures, Toast.LENGTH_LONG).show();
                    }
                }

                break;


            case REQUEST_EXPORT_DATABASE:

                if (resultCode == Activity.RESULT_OK) {
                    try {
                        final Uri destinationUri = data.getData();
                        final OutputStream outputStream = getContext().getContentResolver()
                                .openOutputStream(destinationUri);
                        final File databaseFile = FilmDbHelper.getDatabaseFile(getActivity());
                        final InputStream inputStream = new FileInputStream(databaseFile);
                        IOUtils.copy(inputStream, outputStream);
                        inputStream.close();
                        outputStream.close();
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.DatabaseCopiedSuccessfully),
                                Toast.LENGTH_LONG).show();
                    } catch (final IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.ErrorExportingDatabase),
                                Toast.LENGTH_LONG).show();
                    }
                }

                break;

        }

    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {

    }

    /**
     * Register OnSharedPreferenceChangeListener
     */
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Unregister OnSharedPreferenceChangeListener
     */
    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    // TODO: Check that the exiftool and photos path end with a slash.

    /**
     * When the UIColor preference is changed, update the summary.
     * Also set the parent activity's result code, if the app's theme was changed.
     *
     * @param sharedPreferences {@inheritDoc}
     * @param key {@inheritDoc}
     */
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equals(PreferenceConstants.KEY_DARK_THEME)) {
            getActivity().recreate();
            final PreferenceActivity preferenceActivity = (PreferenceActivity) getActivity();
            int resultCode = preferenceActivity.getResultCode();
            // Preserve previously put result code(s)
            resultCode = resultCode | PreferenceActivity.RESULT_THEME_CHANGED;
            preferenceActivity.setResultCode(resultCode);
        }
    }
}
