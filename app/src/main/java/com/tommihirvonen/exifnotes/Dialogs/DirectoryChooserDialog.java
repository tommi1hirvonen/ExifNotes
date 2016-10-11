package com.tommihirvonen.exifnotes.Dialogs;

// Copyright 2015
// Tommi Hirvonen

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.R;

public class DirectoryChooserDialog {

    private boolean mIsNewFolderEnabled = true;
    private String mSdCardDirectory = "";
    private Context mContext;
    private TextView mCurrentDirView;
    private TextView mTitleView;

    private String mDir = "";
    private List<String> mSubdirs = null;
    private ChosenDirectoryListener mChosenDirectoryListener = null;
    private ArrayAdapter<String> mListAdapter = null;

    /**
     * Callback interface for selected directory
     */
    public interface ChosenDirectoryListener {
        void onChosenDir(String chosenDir);
    }

    /**
     * This creates an instance of DirectoryChooserDialog class
     * @param context the context where the dialog is created
     * @param chosenDirectoryListener the listener interface
     */
    public DirectoryChooserDialog(Context context, ChosenDirectoryListener chosenDirectoryListener) {
        mContext = context;
        mSdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        mChosenDirectoryListener = chosenDirectoryListener;

        try {
            mSdCardDirectory = new File(mSdCardDirectory).getCanonicalPath();
        }
        catch (IOException ioe) {
            //If we fail to initialize the class, send empty string to interface
            if (mChosenDirectoryListener != null) mChosenDirectoryListener.onChosenDir("");
        }
    }

    /**
     * Set if the user can create new folders in the dialog
     * @param isNewFolderEnabled true if enabled, false is disabled
     */
    public void setNewFolderEnabled(boolean isNewFolderEnabled) {
        mIsNewFolderEnabled = isNewFolderEnabled;
    }

    /**
     * Gets info on whether creating new folders is enabled
     * @return true if creating new folders is enabled
     */
    public boolean getNewFolderEnabled() {
        return mIsNewFolderEnabled;
    }

    /**
     * Load DirectoryChooserDialog for default sdcard location
     */
    public void chooseDirectory() {
        // Initial directory is sdcard directory
        chooseDirectory(mSdCardDirectory);
    }

    /**
     * Load DirectoryChooserDialog for initial directory set by user
     * @param dir the initial directory
     */
    public void chooseDirectory(String dir) {
        File dirFile = new File(dir);
        if (! dirFile.exists() || ! dirFile.isDirectory()) {
            dir = mSdCardDirectory;
        }

        try {
            dir = new File(dir).getCanonicalPath();
        }
        catch (IOException ioe) {
            return;
        }

        mDir = dir;
        mSubdirs = getDirectories(dir);

        class DirectoryOnClickListener implements DialogInterface.OnClickListener {
            public void onClick(DialogInterface dialog, int item) {
                // Navigate into the sub-directory
                String text = ((AlertDialog) dialog).getListView().getAdapter().getItem(item).toString();
                if ( text.equals("..") ) mDir = mDir.substring(0, mDir.lastIndexOf("/"));
                else mDir += "/" + text;
                updateDirectory();
            }
        }

        AlertDialog.Builder dialogBuilder =
                createDirectoryChooserDialog(dir, mSubdirs, new DirectoryOnClickListener());

        dialogBuilder.setPositiveButton(mContext.getResources().getString(R.string.OK), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Current directory chosen
                if (mChosenDirectoryListener != null) {
                    // Call registered listener supplied with the chosen directory
                    mChosenDirectoryListener.onChosenDir(mDir);
                }
            }
        }).setNegativeButton(mContext.getResources().getString(R.string.Cancel), null);

        final AlertDialog dirsDialog = dialogBuilder.create();

        dirsDialog.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Back button pressed
                    if ( mDir.equals(mSdCardDirectory) ) {
                        // The very top level directory, do nothing
                        return false;
                    }
                    else {
                        // Navigate back to an upper directory
                        mDir = new File(mDir).getParent();
                        updateDirectory();
                    }

                    return true;
                }
                else {
                    return false;
                }
            }
        });

        // Show directory chooser dialog
        dirsDialog.show();
    }

    /**
     * Creates a new directory
     * @param newDir path of the new folder
     * @return returns true if creating a new directory was successful, false otherwise
     */
    private boolean createSubDir(String newDir) {
        File newDirFile = new File(newDir);
        if (! newDirFile.exists() ) {
            return newDirFile.mkdir();
        }

        return false;
    }

    /**
     * Gets all the directories in the searched directory
     * @param dir the directory whose subdirectories are to be lister
     * @return a List containing all the subdirectories
     */
    private List<String> getDirectories(String dir) {
        List<String> dirs = new ArrayList<>();
        try {
            File dirFile = new File(dir);
            if (! dirFile.exists() || ! dirFile.isDirectory()) {
                return dirs;
            }
            //Add the "one folder up" button only if we are not already at the
            //root of the external storage
            if (!dir.equals(Environment.getExternalStorageDirectory().getCanonicalPath())) {
                dirs.add("..");
            }
            for (File file : dirFile.listFiles()) {
                if ( file.isDirectory() ) {
                    dirs.add( file.getName() );
                }
            }
        }
        catch (Exception e) {
        }

        Collections.sort(dirs, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        return dirs;
    }

    /**
     * This function creates and draws the dialog
     *
     * @param title the title of the dialog
     * @param listItems the List containing all the subdirectories of the current dir
     * @param onClickListener the onClickListener
     * @return the inflated Dialog builder
     */
    private AlertDialog.Builder createDirectoryChooserDialog(String title, List<String> listItems,
                                                             OnClickListener onClickListener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);

        // Create custom view for AlertDialog title containing
        // current directory TextView and possible 'New folder' button.
        // Current directory TextView allows long directory path to be wrapped to multiple lines.
        LinearLayout titleLayout = new LinearLayout(mContext);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        mCurrentDirView = new TextView(mContext);
        mTitleView = new TextView(mContext);

        mCurrentDirView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mCurrentDirView.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCurrentDirView.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        mCurrentDirView.setTextColor(ContextCompat.getColor(mContext, android.R.color.white));
        mCurrentDirView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        mCurrentDirView.setText(title);
        mCurrentDirView.setPadding(25, 5, 25, 25);
        mCurrentDirView.setBackgroundColor(Color.parseColor(primaryColor));

        mTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mTitleView.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mTitleView.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        mTitleView.setTextColor(ContextCompat.getColor(mContext, android.R.color.white));
        mTitleView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        mTitleView.setText(mContext.getResources().getString(R.string.CurrentDirectory));
        mTitleView.setPadding(25, 25, 25, 5);
        mTitleView.setBackgroundColor(Color.parseColor(primaryColor));

        Button newDirButton = new Button(mContext);
        newDirButton.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        newDirButton.setText(mContext.getResources().getString(R.string.NewFolder));

        newDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(mContext);

                // Show new folder name input dialog
                new AlertDialog.Builder(mContext).
                        setTitle(mContext.getResources().getString(R.string.NewFolderName)).
                        setView(input).setPositiveButton(mContext.getResources().getString(R.string.OK), new OnClickListener() {
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
                                    mContext, "Failed to create '" + newDirName +
                                            "' folder", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton(mContext.getResources().getString(R.string.Cancel), null).show();
            }
        });

        if (! mIsNewFolderEnabled) {
            newDirButton.setVisibility(View.GONE);
        }

        titleLayout.addView(mTitleView);
        titleLayout.addView(mCurrentDirView);

        dialogBuilder.setCustomTitle(titleLayout);

        mListAdapter = createListAdapter(listItems);

        dialogBuilder.setSingleChoiceItems(mListAdapter, -1, onClickListener);

        titleLayout.addView(newDirButton);

        dialogBuilder.setCancelable(false);

        return dialogBuilder;
    }

    /**
     * Update the list of directories
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
        return new ArrayAdapter<String>(mContext,
                android.R.layout.select_dialog_item, android.R.id.text1, items) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                if (v instanceof TextView) {
                    // Enable list item (directory) text wrapping
                    TextView tv = (TextView) v;

                    //Change the appearance of the text views
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        tv.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        tv.setTextAppearance(android.R.style.TextAppearance_Medium);
                    }
                    tv.getLayoutParams().height = LayoutParams.WRAP_CONTENT;

                    tv.setEllipsize(null);
                }
                return v;
            }
        };
    }
}