package com.tommihirvonen.exifnotes.Dialogs;

// Copyright 2015
// Tommi Hirvonen

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DirectoryChooserDialog extends DialogFragment {

    /**
     * The interface to be implemented in the calling class
     */
    public interface OnChosenDirectoryListener{
        void onChosenDir(String dir);
    }

    /**
     * Empty default constructor is needed for the DialogFragment
     */
    public DirectoryChooserDialog(){

    }

    OnChosenDirectoryListener mCallback;
    private TextView mCurrentDirView;
    private String mDir = "";
    private List<String> mSubdirs = null;
    private ArrayAdapter<String> mListAdapter = null;

    /**
     * Custom constructor for the DirectoryChooserDialog class.
     *
     * @param listener OnChosenDirectoryListener from the activity
     * @return a new DirectoryChooserDialog
     */
    public static DirectoryChooserDialog newInstance(OnChosenDirectoryListener listener) {
        DirectoryChooserDialog dialog = new DirectoryChooserDialog();
        dialog.mCallback = listener;
        return dialog;
    }

    /**
     * Called when the (dialog)fragment is first attached to its context (the calling activity).
     * @param activity the calling activity
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
    }

    /**
     * Called when the dialog is to be created.
     * @param savedInstanceState possible saved state in case the fragment was resumed
     * @return the inflated dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        // Get preferences to determine UI color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);

        mDir = Environment.getExternalStorageDirectory().getAbsolutePath();

        mSubdirs = getDirectories(mDir);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(R.layout.directory_chooser_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(inflatedView);

        LinearLayout linearLayout = (LinearLayout) inflatedView.findViewById(R.id.dir_chooser_dialog_top_element);
        linearLayout.setBackgroundColor(Color.parseColor(primaryColor));

        mCurrentDirView = (TextView) inflatedView.findViewById(R.id.current_directory_textview);
        mCurrentDirView.setText(mDir);

        ImageView newDirButton = (ImageView) inflatedView.findViewById(R.id.new_directory_button);
        newDirButton.setClickable(true);

        newDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(getActivity());

                // Show new folder name input dialog
                new AlertDialog.Builder(getActivity()).
                        setTitle(getActivity().getResources().getString(R.string.NewFolderName)).
                        setView(input).setPositiveButton(getActivity().getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable newDir = input.getText();
                        String newDirName = newDir.toString();
                        // Create new directory
                        if (createSubDir(mDir + "/" + newDirName)) {
                            // Navigate into the new directory
                            mDir += "/" + newDirName;
                            updateDirectory();
                        } else {
                            Toast.makeText(
                                    getActivity(), "Failed to create '" + newDirName +
                                            "' folder", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton(getActivity().getResources().getString(R.string.Cancel), null).show();
            }
        });

        ImageView backButton = (ImageView) inflatedView.findViewById(R.id.subdirectory_back_button);
        backButton.setClickable(true);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mDir.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    mDir = mDir.substring(0, mDir.lastIndexOf("/"));
                    updateDirectory();
                }
            }
        });

        //Set up the ListAdapter, ListView and listener
        mListAdapter = createListAdapter(mSubdirs);
        ListView listView = (ListView) inflatedView.findViewById(R.id.subdirectories_listview);
        listView.setAdapter(mListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = mSubdirs.get(position);
                mDir += "/" + text;
                updateDirectory();
            }
        });


        dialogBuilder.setPositiveButton(R.string.ExportHere, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.onChosenDir(mDir);
            }
        });
        dialogBuilder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.onChosenDir("");
            }
        });


        dialogBuilder.setCancelable(true);

        return dialogBuilder.create();
    }


    /**
     * Update the list of directories and the current directory text.
     */
    private void updateDirectory() {
        mSubdirs.clear();
        mSubdirs.addAll(getDirectories(mDir));
        mCurrentDirView.setText(mDir);
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Creates an ArrayAdapter for the ListView containing all the possible directories
     *
     * @param items List of subdirectories
     * @return inflated view v
     */
    private ArrayAdapter<String> createListAdapter(List<String> items) {

        return new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                String text = getItem(position);

                ViewHolder holder;

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_directory, parent, false);
                    holder = new ViewHolder();
                    holder.tvDirectory = (TextView) convertView.findViewById(R.id.tv_directory_name);
                    holder.ivFolder = (ImageView) convertView.findViewById(R.id.iv_folder);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.tvDirectory.setText(text);
                holder.ivFolder.getDrawable().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);

                return convertView;
            }
        };
    }

    /**
     * Class to hold the view objects in the ArrayAdapter and improve performance
     */
    static class ViewHolder{
        TextView tvDirectory;
        ImageView ivFolder;
    }

    /**
     * Gets all the directories in the searched directory
     * @param dir the directory whose subdirectories are to be listed
     * @return a List containing all the subdirectories
     */
    private List<String> getDirectories(String dir) {
        List<String> dirs = new ArrayList<>();
        try {
            File dirFile = new File(dir);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return dirs;
            }

            for (File file : dirFile.listFiles()) {
                if ( file.isDirectory() ) {
                    dirs.add( file.getName() );
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(getActivity(), R.string.CouldNotReadDirectories + " " + dir, Toast.LENGTH_LONG).show();
        }

        Collections.sort(dirs, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        return dirs;
    }

    /**
     * Creates a new directory
     * @param newDir path of the new folder
     * @return returns true if creating a new directory was successful, false otherwise
     */
    private boolean createSubDir(String newDir) {
        File newDirFile = new File(newDir);
        return !newDirFile.exists() && newDirFile.mkdir();
    }

}
