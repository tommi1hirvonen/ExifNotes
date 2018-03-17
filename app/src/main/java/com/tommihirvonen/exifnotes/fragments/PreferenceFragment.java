package com.tommihirvonen.exifnotes.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.activities.PreferenceActivity;
import com.tommihirvonen.exifnotes.dialogs.DirectoryChooserDialog;
import com.tommihirvonen.exifnotes.dialogs.FileChooserDialog;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.UIColorDialogPreference;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
        Preference appTheme = findPreference(PreferenceConstants.KEY_APP_THEME);
        appTheme.setSummary(((ListPreference) appTheme).getEntry());

        UIColorDialogPreference UIColor = (UIColorDialogPreference) findPreference(PreferenceConstants.KEY_UI_COLOR);
        UIColor.setSummary(UIColor.getSelectedColorName());

        Preference exportDatabase = findPreference(PreferenceConstants.KEY_EXPORT_DATABASE);
        exportDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final FileInputStream databaseFile;
                try {
                    databaseFile = new FileInputStream(FilmDbHelper.getDatabaseFile(getActivity()));
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.ErrorBuildingDatabaseFile),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                DirectoryChooserDialog directoryChooserDialog = DirectoryChooserDialog.newInstance(
                        new DirectoryChooserDialog.OnChosenDirectoryListener() {
                    @Override
                    public void onChosenDirectory(String directory) {
                        //dir is empty if the export was canceled.
                        //Otherwise proceed
                        if (directory.length() > 0) {
                            //Export the files to the given path
                            //Inform the user if something went wrong
                            directory = directory + "/" + FilmDbHelper.DATABASE_NAME;
                            FileOutputStream outputFile;
                            try {
                                outputFile = new FileOutputStream(directory);
                            } catch (FileNotFoundException e) {
                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.ErrorBuildingOutputFile) + directory,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                Utilities.copyFile(databaseFile, outputFile);
                            } catch (IOException e){
                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.ErrorCopyingFile),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Toast.makeText(getActivity(),
                                    getResources().getString(R.string.DatabaseCopiedTo) + directory,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                directoryChooserDialog.show(getFragmentManager(), "DirChooserDialogTag");


                return true;
            }
        });

        Preference importDatabase = findPreference(PreferenceConstants.KEY_IMPORT_DATABASE);
        importDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.ImportDatabaseTitle));
                builder.setMessage(getResources().getString(R.string.ImportDatabaseVerification));
                builder.setPositiveButton(getResources().getString(R.string.Continue),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FileChooserDialog fileChooserDialog = FileChooserDialog.newInstance(
                                new FileChooserDialog.OnChosenFileListener() {
                            @Override
                            public void onChosenFile(String filePath) {
                                //If the length of filePath is 0, then the user canceled the import.
                                if (filePath.length()>0) {
                                    FilmDbHelper database = FilmDbHelper.getInstance(getActivity());
                                    boolean importSuccess;
                                    try {
                                        importSuccess = database.importDatabase(getActivity(), filePath);
                                    } catch (IOException e) {
                                        Toast.makeText(getActivity(),
                                                getResources().getString(R.string.ErrorImportingDatabaseFrom) +
                                                        filePath,
                                                Toast.LENGTH_SHORT).show();
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
                        });
                        fileChooserDialog.show(getFragmentManager(), "FileChooserDialogTag");
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

                return false;
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
        // Set summaries for the list preferences
        Preference appTheme = findPreference(PreferenceConstants.KEY_APP_THEME);
        appTheme.setSummary(((ListPreference) appTheme).getEntry());

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
