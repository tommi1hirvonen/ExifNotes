package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

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
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Dialog to select a file in external storage
 */
public class FileChooserDialog extends DialogFragment {

    /**
     * Reference to the implementing class's listener
     */
    private FileChooserDialog.OnChosenFileListener callback;

    /**
     * Used to display the current working directory's path
     */
    private TextView currentDirectoryTextView;

    /**
     * Path of the current working directory
     */
    private String currentDirectory = "";

    /**
     * Contains all the files and subdirectories in the current working directory
     */
    private List<FileOrDirectory> fileOrDirectoryList = null;

    /**
     * Used to adapt List of files and subdirectories to the dialog's ListView
     */
    private ArrayAdapter<FileOrDirectory> listAdapter = null;

    /**
     * ListView element in the dialog containing files and folders
     */
    private ListView listView;

    /**
     * File filter used to filter displayed files
     */
    private FileFilter fileFilter;

    /**
     * The interface to be implemented in the calling/implementing class
     */
    public interface OnChosenFileListener{
        void onChosenFile(String filePath);
    }

    /**
     * Empty default constructor is needed for the DialogFragment
     */
    public FileChooserDialog(){

    }

    /**
     * Private class to holds both directories and files
     */
    private class FileOrDirectory {
        final String path;
        final boolean directory; //true if dir, false if file
        FileOrDirectory(String path, boolean directory){
            this.path = path;
            this.directory = directory;
        }
    }

    /**
     * Custom constructor for the FileChooserDialog class.
     * Attach the listener and filename filter to this class's member.
     *
     * @param filenameMustEndWith String, with which any file that is shown, should end. Null if all files are allowed. Directories are always allowed.
     * @param listener listener from the implementing class
     * @return new FileChooserDialog
     */
    public static FileChooserDialog newInstance(final String filenameMustEndWith, OnChosenFileListener listener) {
        FileChooserDialog dialog = new FileChooserDialog();
        dialog.callback = listener;
        dialog.fileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                // If filenameMustEndWith is null, allow all files (return true). Otherwise allow directories and files ending with filenameMustEndWith.
                return filenameMustEndWith == null || file.isDirectory() || file.getName().endsWith(filenameMustEndWith);
            }
        };
        return dialog;
    }

    /**
     * Called when the dialog is to be created.
     * Get the current directory, its subdirectories and files.
     * Inflate the dialog view and initialize the UI (Buttons, ListView etc.).
     * Add onClick listeners.
     *
     * @param savedInstanceState possible saved state in case the fragment was resumed
     * @return the inflated dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        currentDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

        fileOrDirectoryList = getFileDirs(currentDirectory);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.dialog_directory_chooser, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(inflatedView);

        LinearLayout linearLayout = inflatedView.findViewById(
                R.id.dir_chooser_dialog_top_element);

        int primaryColor = Utilities.getPrimaryUiColor(getActivity());
        linearLayout.setBackgroundColor(primaryColor);

        // Color scrollIndicatorDown according to the app's theme
        View scrollIndicatorDown = inflatedView.findViewById(R.id.scrollIndicatorDown);
        int color = Utilities.isAppThemeDark(getActivity()) ?
                ContextCompat.getColor(getActivity(), R.color.white) :
                ContextCompat.getColor(getActivity(), R.color.black);
        scrollIndicatorDown.setBackgroundColor(color);
        // If the theme is dark and UI color is grey, show scrollIndicatorUp.
        // The Dialog title background and ListView background colors are identical, so
        // separate them with the scroll indicator.
        if (Utilities.isAppThemeDark(getActivity()) &&
                preferences.getString(PreferenceConstants.KEY_UI_COLOR, "").equals("#424242,#212121")) {
            View scrollIndicatorUp = inflatedView.findViewById(R.id.scrollIndicatorUp);
            scrollIndicatorUp.setVisibility(View.VISIBLE);
        }

        currentDirectoryTextView = inflatedView.findViewById(R.id.current_directory_textview);
        currentDirectoryTextView.setText(currentDirectory);

        ImageView backButton = inflatedView.findViewById(R.id.subdirectory_back_button);
        backButton.setClickable(true);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentDirectory.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/"));
                    updateDirectory();
                }
            }
        });

        //Set up the ListAdapter, ListView and listener
        listAdapter = createListAdapter(fileOrDirectoryList);
        listView = inflatedView.findViewById(R.id.subdirectories_listview);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileOrDirectory selectedFileOrDirectory = fileOrDirectoryList.get(position);
                if (selectedFileOrDirectory.directory) {
                    currentDirectory += "/" + selectedFileOrDirectory.path;
                    updateDirectory();
                } else {
                    dismiss();
                    callback.onChosenFile(currentDirectory + "/" + selectedFileOrDirectory.path);
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onChosenFile("");
            }
        });

        dialogBuilder.setCancelable(true);

        return dialogBuilder.create();
    }


    /**
     * Update the list of directories and the current directory text.
     */
    private void updateDirectory() {
        fileOrDirectoryList.clear();
        fileOrDirectoryList.addAll(getFileDirs(currentDirectory));
        currentDirectoryTextView.setText(currentDirectory);
        listAdapter.notifyDataSetChanged();
        listView.setSelection(0);
    }

    /**
     * Creates an ArrayAdapter for the ListView containing all the possible directories
     *
     * @param items List of subdirectories
     * @return inflated view v
     */
    private ArrayAdapter<FileOrDirectory> createListAdapter(List<FileOrDirectory> items) {

        return new ArrayAdapter<FileOrDirectory>(getActivity(),
                android.R.layout.simple_list_item_1,
                items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                final FileOrDirectory fileOrDirectory = getItem(position);
                FileChooserDialog.ViewHolder holder;

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(
                            R.layout.item_directory, parent, false);
                    holder = new FileChooserDialog.ViewHolder();
                    holder.directoryTextView = convertView.findViewById(R.id.tv_directory_name);
                    holder.folderImageView = convertView.findViewById(R.id.iv_folder);
                    convertView.setTag(holder);
                } else {
                    holder = (FileChooserDialog.ViewHolder) convertView.getTag();
                }

                if (fileOrDirectory != null) {
                    holder.directoryTextView.setText(fileOrDirectory.path);
                    if (fileOrDirectory.directory) {
                        Utilities.setColorFilter(holder.folderImageView.getDrawable().mutate(),
                                ContextCompat.getColor(getContext(), R.color.grey));
                        holder.folderImageView.setVisibility(View.VISIBLE);
                    } else {
                        holder.folderImageView.setVisibility(View.INVISIBLE);
                    }
                }
                return convertView;
            }
        };
    }

    /**
     * Class to hold the view objects in the ArrayAdapter and improve performance
     */
    private static class ViewHolder{
        TextView directoryTextView;
        ImageView folderImageView;
    }

    /**
     * Gets all the directories and files in the searched directory
     * @param directoryPath the directory whose subdirectories and files are to be listed
     * @return a List containing all the subdirectories and files
     */
    private List<FileOrDirectory> getFileDirs(String directoryPath) {
        List<FileOrDirectory> fileOrDirectories = new ArrayList<>();
//        try {
            File directoryFile = new File(directoryPath);
            // If the file/directory doesn't exist or it is not a directory, return empty list.
            if (!directoryFile.exists() || !directoryFile.isDirectory()) {
                return fileOrDirectories;
            }
            // Otherwise, iterate the files and directories in the location.
            for (File file : directoryFile.listFiles(fileFilter)) {
                if (file.isDirectory()) {
                    fileOrDirectories.add(new FileOrDirectory(file.getName(), true));
                } else {
                    fileOrDirectories.add(new FileOrDirectory(file.getName(), false));
                }
            }
//        }
//        catch (Exception e) {
//            Toast.makeText(getActivity(), getResources().getString(R.string.CouldNotReadDirectories) + " " + directoryPath,
//                    Toast.LENGTH_LONG).show();
//        }
        Collections.sort(fileOrDirectories, new Comparator<FileOrDirectory>() {
            public int compare(FileOrDirectory o1, FileOrDirectory o2) {
                if (o1.directory == o2.directory) {
                    return o1.path.compareToIgnoreCase(o2.path);
                } else {
                    if (o1.directory) return -1;
                    else return 1;
                }
            }
        });
        return fileOrDirectories;
    }

}
