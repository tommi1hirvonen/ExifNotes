package com.tommihirvonen.exifnotes.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.dialogs.DirectoryChooserDialog;
import com.tommihirvonen.exifnotes.dialogs.FileChooserDialog;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.AppThemeDialogPreference;
import com.tommihirvonen.exifnotes.utilities.ComplementaryPicturesManager;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.UIColorDialogPreference;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * PreferenceFragment is shown in PreferenceActivity.
 * It is responsible for displaying all the preference options.
 */
public class PreferenceFragment extends android.preference.PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Get the preferences from resources. Set the UI and add listeners
     * for database export and import options.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notice that all we need to do is invoke the addPreferencesFromResource(..) method,
        // where we simply provide the reference to the preferences.xml file
        // and Android takes care of the rest for rendering the activity
        // and also saving the values for you.
        addPreferencesFromResource(R.xml.fragment_preference);

        // Set summaries for the list preferences
        AppThemeDialogPreference appTheme = (AppThemeDialogPreference) findPreference(PreferenceConstants.KEY_APP_THEME);
        appTheme.setSummary(appTheme.getAppTheme());

        UIColorDialogPreference UIColor = (UIColorDialogPreference) findPreference(PreferenceConstants.KEY_UI_COLOR);
        UIColor.setSummary(UIColor.getSelectedColorName());

        // OnClickListener to start complementary pictures export.
        final Preference exportComplementaryPictures = findPreference(PreferenceConstants.KEY_EXPORT_COMPLEMENTARY_PICTURES);
        exportComplementaryPictures.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DirectoryChooserDialog.newInstance(new DirectoryChooserDialog.OnChosenDirectoryListener() {
                    @Override
                    public void onChosenDirectory(String directory) {
                        // directory is empty if the export was canceled.
                        if (directory.length() == 0) return;

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
                        ComplementaryPicturesManager.exportComplementaryPictures(getActivity(),
                                new File(directory), new ComplementaryPicturesManager.ZipFileCreatorAsyncTask.ProgressListener() {
                            @Override
                            public void onProgressChanged(int progressPercentage, int completed, int total) {
                                progressBar.setProgress(progressPercentage);
                                final String progressText = "" + completed + "/" + total;
                                progressTextView.setText(progressText);
                            }
                            @Override
                            public void onCompleted(boolean success, int completedEntries, File zipFile) {
                                dialog.dismiss();
                                if (success) {
                                    if (completedEntries == 0)
                                        Toast.makeText(getActivity(), R.string.NoPicturesExported, Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(getActivity(),
                                            getResources().getQuantityString(
                                                    R.plurals.ComplementaryPicturesExportedTo,
                                                    completedEntries, completedEntries)
                                                    + zipFile.getName(), Toast.LENGTH_LONG).show();
                                }
                                else Toast.makeText(getActivity(),
                                        R.string.ErrorExportingComplementaryPictures, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).show(getFragmentManager(), "DirChooserDialogTag");

                return true;
            }
        });

        // OnClickListener to start complementary pictures import
        final Preference importComplementaryPictures = findPreference(PreferenceConstants.KEY_IMPORT_COMPLEMENTARY_PICTURES);
        importComplementaryPictures.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Show additional message about importing complementary pictures using a separate dialog.
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.ImportDatabaseTitle);
                builder.setMessage(R.string.ImportComplementaryPicturesVerification);
                builder.setPositiveButton(R.string.Continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        FileChooserDialog.newInstance(".zip", new FileChooserDialog.OnChosenFileListener() {
                            @Override
                            public void onChosenFile(final String filePath) {
                                // filePath is empty if the import was canceled
                                if (filePath.length() == 0) return;

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
                                        new File(filePath), new ComplementaryPicturesManager.ZipFileReaderAsyncTask.ProgressListener() {
                                            @Override
                                            public void onProgressChanged(int progressPercentage, int completed, int total) {
                                                progressBar.setProgress(progressPercentage);
                                                final String progressText = "" + completed + "/" + total;
                                                progressTextView.setText(progressText);
                                            }

                                            @Override
                                            public void onCompleted(boolean success, int completedEntries) {
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
                                                        R.string.ErrorImportingComplementaryPicturesFrom,
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        }).show(getFragmentManager(), "FileChooserDialogTag");

                    }
                });
                builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do nothing
                    }
                });
                builder.create().show();

                return true;
            }
        });

        // OnClickListener to start database export
        final Preference exportDatabase = findPreference(PreferenceConstants.KEY_EXPORT_DATABASE);
        exportDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Show additional message about exporting the SQL database using a separate dialog.
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.ExportDatabaseTitle);
                builder.setMessage(R.string.ExportDatabaseVerification);
                builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        DirectoryChooserDialog.newInstance(new DirectoryChooserDialog.OnChosenDirectoryListener() {
                            @Override
                            public void onChosenDirectory(String directory) {
                                //dir is empty if the export was canceled.
                                //Otherwise proceed
                                if (directory.length() > 0) {
                                    //Export the files to the given path
                                    //Inform the user if something went wrong
                                    final String date = Utilities.getCurrentTime().split("\\s+")[0];
                                    final String filename = "Exif_Notes_Database_" + date + ".db";
                                    final File databaseFile = FilmDbHelper.getDatabaseFile(getActivity());
                                    final File outputFile = new File(new File(directory), filename);
                                    try {
                                        Utilities.copyFile(databaseFile, outputFile);
                                    } catch (IOException e){
                                        Toast.makeText(getActivity(),
                                                getResources().getString(R.string.ErrorExportingDatabase),
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    Toast.makeText(getActivity(),
                                            getResources().getString(R.string.DatabaseCopiedToFile) + filename,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }).show(getFragmentManager(), "DirChooserDialogTag");
                    }
                });
                builder.create().show();;

                return true;
            }
        });

        // OnClickListener to start database import
        final Preference importDatabase = findPreference(PreferenceConstants.KEY_IMPORT_DATABASE);
        importDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Show additional message about importing the database using a separate dialog.
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.ImportDatabaseTitle);
                builder.setMessage(R.string.ImportDatabaseVerification);
                builder.setPositiveButton(R.string.Continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FileChooserDialog.newInstance(".db", new FileChooserDialog.OnChosenFileListener() {
                            @Override
                            public void onChosenFile(String filePath) {
                                //If the length of filePath is 0, then the user canceled the import.
                                if (filePath.length() > 0) {
                                    FilmDbHelper database = FilmDbHelper.getInstance(getActivity());
                                    boolean importSuccess;
                                    try {
                                        importSuccess = database.importDatabase(getActivity(), filePath);
                                    } catch (IOException e) {
                                        Toast.makeText(getActivity(),
                                                getResources().getString(R.string.ErrorImportingDatabaseFrom) +
                                                        filePath,
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    if (importSuccess) {

                                        // Set the parent activity's result code
                                        PreferenceActivity preferenceActivity =
                                                (PreferenceActivity) getActivity();
                                        int resultCode = preferenceActivity.getResultCode();

                                        // Preserve the previously put result code(s)
                                        resultCode = resultCode | PreferenceActivity.RESULT_DATABASE_IMPORTED;
                                        preferenceActivity.setResultCode(resultCode);

                                        Toast.makeText(getActivity(),
                                                getResources().getString(R.string.DatabaseImported),
                                                Toast.LENGTH_LONG).show();

                                    }
                                }
                            }
                        }).show(getFragmentManager(), "FileChooserDialogTag");
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.No),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing
                    }
                });
                builder.create().show();

                return true;
            }
        });
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PreferenceConstants.KEY_APP_THEME)) {
            getActivity().recreate();
            PreferenceActivity preferenceActivity = (PreferenceActivity) getActivity();
            int resultCode = preferenceActivity.getResultCode();
            // Preserve previously put result code(s)
            resultCode = resultCode | PreferenceActivity.RESULT_THEME_CHANGED;
            preferenceActivity.setResultCode(resultCode);
        }
    }
}
