package com.tommihirvonen.exifnotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Dialog to select a directory in external storage
 */
public class DirectoryChooserDialog extends DialogFragment {

    /**
     * Reference to the implementing class's listener
     */
    OnChosenDirectoryListener callback;

    /**
     * Used to display the current working directory's path
     */
    private TextView currentDirectoryTextView;

    /**
     *  Path of the current working directory
     */
    private String currentDirectory = "";

    /**
     * contains all the subdirectories in the current working directory
     */
    private List<String> subdirectories = null;

    /**
     * Used to adapt List of subdirectories to the dialog's ListView
     */
    private ArrayAdapter<String> subdirectoryAdapter = null;

    /**
     * The interface to be implemented in the calling/implementing class
     */
    public interface OnChosenDirectoryListener{
        void onChosenDirectory(String directory);
    }

    /**
     * Empty default constructor is needed for the DialogFragment
     */
    public DirectoryChooserDialog(){

    }

    /**
     * Custom constructor for the DirectoryChooserDialog class.
     *
     * @param listener OnChosenDirectoryListener from the activity
     * @return a new DirectoryChooserDialog
     */
    public static DirectoryChooserDialog newInstance(OnChosenDirectoryListener listener) {
        DirectoryChooserDialog dialog = new DirectoryChooserDialog();
        dialog.callback = listener;
        return dialog;
    }

    /**
     * Called when the (dialog)fragment is first attached to its context (the calling activity).
     *
     * @param activity the calling activity
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
    }

    /**
     * Called when the dialog is to be created.
     * Get the current directory and its subdirectories.
     * Inflate the dialog view and initialize the UI (Buttons, ListView etc.).
     * Add onClick listeners.
     *
     * @param savedInstanceState possible saved state in case the fragment was resumed
     * @return the inflated dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        currentDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

        subdirectories = getDirectories(currentDirectory);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View inflatedView = layoutInflater.inflate(
                R.layout.directory_chooser_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(inflatedView);

        LinearLayout linearLayout = (LinearLayout) inflatedView.findViewById(
                R.id.dir_chooser_dialog_top_element);

        int primaryColor = Utilities.getPrimaryUiColor(getActivity());
        linearLayout.setBackgroundColor(primaryColor);

        currentDirectoryTextView = (TextView) inflatedView.findViewById(R.id.current_directory_textview);
        currentDirectoryTextView.setText(currentDirectory);

        ImageView newDirectoryButton = (ImageView) inflatedView.findViewById(R.id.new_directory_button);
        newDirectoryButton.setClickable(true);

        newDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(getActivity());

                // Show new folder name input dialog
                new AlertDialog.Builder(getActivity()).
                        setTitle(getActivity().getResources().getString(R.string.NewFolderName)).
                        setView(input).setPositiveButton(getActivity().getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newDirectoryName = input.getText().toString();
                        // Create new directory
                        if (createSubDirectory(currentDirectory + "/" + newDirectoryName)) {
                            // Navigate into the new directory
                            currentDirectory += "/" + newDirectoryName;
                            updateDirectory();
                        } else {
                            Toast.makeText(
                                    getActivity(), "Failed to create '" + newDirectoryName +
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
                if (!currentDirectory.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/"));
                    updateDirectory();
                }
            }
        });

        //Set up the ListAdapter, ListView and listener
        subdirectoryAdapter = createListAdapter(subdirectories);
        ListView listView = (ListView) inflatedView.findViewById(R.id.subdirectories_listview);
        listView.setAdapter(subdirectoryAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = subdirectories.get(position);
                currentDirectory += "/" + text;
                updateDirectory();
            }
        });


        dialogBuilder.setPositiveButton(R.string.ExportHere, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onChosenDirectory(currentDirectory);
            }
        });
        dialogBuilder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onChosenDirectory("");
            }
        });


        dialogBuilder.setCancelable(true);

        return dialogBuilder.create();
    }


    /**
     * Update the list of directories and the current directory text.
     */
    private void updateDirectory() {
        subdirectories.clear();
        subdirectories.addAll(getDirectories(currentDirectory));
        currentDirectoryTextView.setText(currentDirectory);
        subdirectoryAdapter.notifyDataSetChanged();
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
                    convertView = LayoutInflater.from(getContext()).inflate(
                            R.layout.item_directory, parent, false);
                    holder = new ViewHolder();
                    holder.directoryTextView = (TextView) convertView.findViewById(R.id.tv_directory_name);
                    holder.folderImageView = (ImageView) convertView.findViewById(R.id.iv_folder);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.directoryTextView.setText(text);
                holder.folderImageView.getDrawable().mutate().setColorFilter(ContextCompat.getColor(
                        getContext(), R.color.grey), PorterDuff.Mode.SRC_IN);

                return convertView;
            }
        };
    }

    /**
     * Class to hold the view objects in the ArrayAdapter and improve performance
     */
    static class ViewHolder{
        TextView directoryTextView;
        ImageView folderImageView;
    }

    /**
     * Gets all the directories in the searched directory
     *
     * @param directoryPath the directory whose subdirectories are to be listed
     * @return a List containing all the subdirectories
     */
    private List<String> getDirectories(String directoryPath) {
        List<String> directories = new ArrayList<>();
        try {
            File directoryFile = new File(directoryPath);
            if (!directoryFile.exists() || !directoryFile.isDirectory()) {
                return directories;
            }

            for (File file : directoryFile.listFiles()) {
                if ( file.isDirectory() ) {
                    directories.add( file.getName() );
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(getActivity(), R.string.CouldNotReadDirectories + " " +
                    directoryPath, Toast.LENGTH_LONG).show();
        }

        Collections.sort(directories, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        return directories;
    }

    /**
     * Creates a new directory
     *
     * @param newDirectory path of the new folder
     * @return returns true if creating a new directory was successful, false otherwise
     */
    private boolean createSubDirectory(String newDirectory) {
        File newDirectoryFile = new File(newDirectory);
        return !newDirectoryFile.exists() && newDirectoryFile.mkdir();
    }

}
