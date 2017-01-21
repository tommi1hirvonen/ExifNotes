package com.tommihirvonen.exifnotes.Fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.Dialogs.DirectoryChooserDialog;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

// Copyright 2015
// Tommi Hirvonen

public class PreferenceFragment extends android.preference.PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


    // Notice that all we need to do is invoke the addPreferencesFromResource(..) method,
    // where we simply provide the reference to the preferences.xml file
    // and Android takes care of the rest for rendering the activity
    // and also saving the values for you.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_preference);

        // Set summaries for the list preferences
        Preference UIColor = findPreference("UIColor");
        UIColor.setSummary(((ListPreference) UIColor).getEntry());

        Preference exportDatabase = findPreference("ExportDatabase");
        exportDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final FileInputStream databaseFile;
                try {
                    databaseFile = new FileInputStream(FilmDbHelper.getDatabaseFile(getActivity()));
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.ErrorBuildingDatabaseFile), Toast.LENGTH_SHORT).show();
                    return false;
                }

                DirectoryChooserDialog dirChooserDialog = DirectoryChooserDialog.newInstance(new DirectoryChooserDialog.OnChosenDirectoryListener() {
                    @Override
                    public void onChosenDir(String dir) {
                        //dir is empty if the export was canceled.
                        //Otherwise proceed
                        if (dir.length() > 0) {
                            //Export the files to the given path
                            //Inform the user if something went wrong
                            dir = dir + "/" + FilmDbHelper.DATABASE_NAME;
                            FileOutputStream outputFile;
                            try {
                                outputFile = new FileOutputStream(dir);
                            } catch (FileNotFoundException e) {
                                Toast.makeText(getActivity(), getResources().getString(R.string.ErrorBuildingOutputFile) + dir, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                Utilities.copyFile(databaseFile, outputFile);
                            } catch (IOException e){
                                Toast.makeText(getActivity(), getResources().getString(R.string.ErrorCopyingFile), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Toast.makeText(getActivity(), getResources().getString(R.string.DatabaseCopiedTo) + dir, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dirChooserDialog.show(getFragmentManager(), "DirChooserDialogTag");


                return true;
            }
        });

        Preference importDatabase = findPreference("ImportDatabase");
        importDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getActivity(), "Feature coming soon!", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    // TODO: Check that the exiftool and photos path end with a slash.

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Set summaries for the list preferences
//        Preference artistName = findPreference("ArtistName");
//        artistName.setSummary(((EditTextPreference) artistName).getText());
        Preference UIColor = findPreference("UIColor");
        UIColor.setSummary(((ListPreference) UIColor).getEntry());
    }
}
