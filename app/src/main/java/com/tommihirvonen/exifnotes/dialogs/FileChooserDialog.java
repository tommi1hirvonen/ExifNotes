package com.tommihirvonen.exifnotes.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.io.File;
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
     * Custom constructor for the FileChooserDialog class. Attach the listener
     * to this class's member.
     *
     * @param listener OnChosenFileListener from the activity
     * @return a new FileChooserDialog
     */
    public static FileChooserDialog newInstance(FileChooserDialog.OnChosenFileListener listener) {
        FileChooserDialog dialog = new FileChooserDialog();
        dialog.callback = listener;
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
        ListView listView = inflatedView.findViewById(R.id.subdirectories_listview);
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

//                String text = getItem(position);
                FileOrDirectory fileOrDirectory = getItem(position);

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
                        holder.folderImageView.getDrawable().mutate().setColorFilter(
                                ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);
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
        try {
            File directoryFile = new File(directoryPath);
            if (!directoryFile.exists() || !directoryFile.isDirectory()) {
                return fileOrDirectories;
            }

            for (File file : directoryFile.listFiles()) {
                if (file.isDirectory()) {
                    fileOrDirectories.add(new FileOrDirectory(file.getName(), true));
                } else {
                    fileOrDirectories.add(new FileOrDirectory(file.getName(), false));
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(getActivity(), R.string.CouldNotReadDirectories + " " + directoryPath,
                    Toast.LENGTH_LONG).show();
        }

        Collections.sort(fileOrDirectories, new Comparator<FileOrDirectory>() {
            public int compare(FileOrDirectory o1, FileOrDirectory o2) {
                if (o1.directory == o2.directory) {
                    return o1.path.compareTo(o2.path);
                } else {
                    if (o1.directory) return -1;
                    else return 1;
                }
            }
        });

        return fileOrDirectories;
    }


}
