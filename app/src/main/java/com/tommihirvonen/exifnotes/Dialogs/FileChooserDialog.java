package com.tommihirvonen.exifnotes.Dialogs;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

// Copyright 2017
// Tommi Hirvonen

public class FileChooserDialog extends DialogFragment {

    /**
     * The interface to be implemented in the calling class
     */
    public interface OnChosenFileListener{
        void onChosenFile(String filePath);
    }

    /**
     * Empty default constructor is needed for the DialogFragment
     */
    public FileChooserDialog(){

    }

    private class FileDir{
        String path;
        boolean directory; //true if dir, false if file
        FileDir(String path, boolean directory){
            this.path = path;
            this.directory = directory;
        }
    }

    FileChooserDialog.OnChosenFileListener mCallback;
    private TextView mCurrentDirView;
    private String mDir = "";
    private List<FileDir> mFileDirs = null;
    private ArrayAdapter<FileDir> mListAdapter = null;

    /**
     * Custom constructor for the FileChooserDialog class.
     *
     * @param listener OnChosenFileListener from the activity
     * @return a new FileChooserDialog
     */
    public static FileChooserDialog newInstance(FileChooserDialog.OnChosenFileListener listener) {
        FileChooserDialog dialog = new FileChooserDialog();
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

        mFileDirs = getFileDirs(mDir);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(R.layout.directory_chooser_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(inflatedView);

        LinearLayout linearLayout = (LinearLayout) inflatedView.findViewById(R.id.dir_chooser_dialog_top_element);
        linearLayout.setBackgroundColor(Color.parseColor(primaryColor));

        mCurrentDirView = (TextView) inflatedView.findViewById(R.id.current_directory_textview);
        mCurrentDirView.setText(mDir);

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
        mListAdapter = createListAdapter(mFileDirs);
        ListView listView = (ListView) inflatedView.findViewById(R.id.subdirectories_listview);
        listView.setAdapter(mListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileDir selectedFileDir = mFileDirs.get(position);
                if ( selectedFileDir.directory ) {
                    mDir += "/" + selectedFileDir.path;
                    updateDirectory();
                } else {
                    dismiss();
                    mCallback.onChosenFile(mDir + "/" + selectedFileDir.path);
                }
            }
        });


        dialogBuilder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.onChosenFile("");
            }
        });


        dialogBuilder.setCancelable(true);

        return dialogBuilder.create();
    }


    /**
     * Update the list of directories and the current directory text.
     */
    private void updateDirectory() {
        mFileDirs.clear();
        mFileDirs.addAll(getFileDirs(mDir));
        mCurrentDirView.setText(mDir);
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Creates an ArrayAdapter for the ListView containing all the possible directories
     *
     * @param items List of subdirectories
     * @return inflated view v
     */
    private ArrayAdapter<FileDir> createListAdapter(List<FileDir> items) {

        return new ArrayAdapter<FileDir>(getActivity(), android.R.layout.simple_list_item_1, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

//                String text = getItem(position);
                FileDir fileDir = getItem(position);

                FileChooserDialog.ViewHolder holder;

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_directory, parent, false);
                    holder = new FileChooserDialog.ViewHolder();
                    holder.tvDirectory = (TextView) convertView.findViewById(R.id.tv_directory_name);
                    holder.ivFolder = (ImageView) convertView.findViewById(R.id.iv_folder);
                    convertView.setTag(holder);
                } else {
                    holder = (FileChooserDialog.ViewHolder) convertView.getTag();
                }

                if ( fileDir != null ) {
                    holder.tvDirectory.setText(fileDir.path);
                    if (fileDir.directory) {
                        holder.ivFolder.getDrawable().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);
                        holder.ivFolder.setVisibility(View.VISIBLE);
                    } else {
                        holder.ivFolder.setVisibility(View.INVISIBLE);
                    }
                }
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
    private List<FileDir> getFileDirs(String dir) {
        List<FileDir> fileDirs = new ArrayList<>();
        try {
            File dirFile = new File(dir);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return fileDirs;
            }

            for (File file : dirFile.listFiles()) {
                if ( file.isDirectory() ) {
                    fileDirs.add( new FileDir(file.getName(), true) );
                } else {
                    fileDirs.add( new FileDir(file.getName(), false) );
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(getActivity(), R.string.CouldNotReadDirectories + " " + dir, Toast.LENGTH_LONG).show();
        }

        Collections.sort(fileDirs, new Comparator<FileDir>() {
            public int compare(FileDir o1, FileDir o2) {
                if (o1.directory == o2.directory) {
                    return o1.path.compareTo(o2.path);
                } else {
                    if (o1.directory) return -1;
                    else return 1;
                }
            }
        });

        return fileDirs;
    }


}
