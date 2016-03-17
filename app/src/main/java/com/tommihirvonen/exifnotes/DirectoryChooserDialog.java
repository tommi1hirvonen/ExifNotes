package com.tommihirvonen.exifnotes;

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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DirectoryChooserDialog {

    private boolean m_isNewFolderEnabled = true;
    private String m_sdcardDirectory = "";
    private Context m_context;
    private TextView m_currentDirView;
    private TextView m_titleView;

    private String m_dir = "";
    private List<String> m_subdirs = null;
    private ChosenDirectoryListener m_chosenDirectoryListener = null;
    private ArrayAdapter<String> m_listAdapter = null;

    //////////////////////////////////////////////////////
    // Callback interface for selected directory
    //////////////////////////////////////////////////////
    public interface ChosenDirectoryListener {
        public void onChosenDir(String chosenDir);
    }

    public DirectoryChooserDialog(Context context, ChosenDirectoryListener chosenDirectoryListener) {
        m_context = context;
        m_sdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        m_chosenDirectoryListener = chosenDirectoryListener;

        try {
            m_sdcardDirectory = new File(m_sdcardDirectory).getCanonicalPath();
        }
        catch (IOException ioe) {
        }
    }

    ///////////////////////////////////////////////////////////////////////
    // setNewFolderEnabled() - enable/disable new folder button
    ///////////////////////////////////////////////////////////////////////

    public void setNewFolderEnabled(boolean isNewFolderEnabled) {
        m_isNewFolderEnabled = isNewFolderEnabled;
    }

    public boolean getNewFolderEnabled() {
        return m_isNewFolderEnabled;
    }

    ///////////////////////////////////////////////////////////////////////
    // chooseDirectory() - load directory chooser dialog for initial
    // default sdcard directory
    ///////////////////////////////////////////////////////////////////////

    public void chooseDirectory() {
        // Initial directory is sdcard directory
        chooseDirectory(m_sdcardDirectory);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // chooseDirectory(String dir) - load directory chooser dialog for initial
    // input 'dir' directory
    ////////////////////////////////////////////////////////////////////////////////

    public void chooseDirectory(String dir) {
        File dirFile = new File(dir);
        if (! dirFile.exists() || ! dirFile.isDirectory()) {
            dir = m_sdcardDirectory;
        }

        try {
            dir = new File(dir).getCanonicalPath();
        }
        catch (IOException ioe) {
            return;
        }

        m_dir = dir;
        m_subdirs = getDirectories(dir);

        class DirectoryOnClickListener implements DialogInterface.OnClickListener {
            public void onClick(DialogInterface dialog, int item) {
                // Navigate into the sub-directory
                String text = ((AlertDialog) dialog).getListView().getAdapter().getItem(item).toString();
                if ( text.equals("..") ) m_dir = m_dir.substring(0, m_dir.lastIndexOf("/"));
                else m_dir += "/" + text;
                updateDirectory();
            }
        }

        AlertDialog.Builder dialogBuilder =
                createDirectoryChooserDialog(dir, m_subdirs, new DirectoryOnClickListener());

        dialogBuilder.setPositiveButton(m_context.getResources().getString(R.string.OK), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Current directory chosen
                if (m_chosenDirectoryListener != null) {
                    // Call registered listener supplied with the chosen directory
                    m_chosenDirectoryListener.onChosenDir(m_dir);
                }
            }
        }).setNegativeButton(m_context.getResources().getString(R.string.Cancel), null);

        final AlertDialog dirsDialog = dialogBuilder.create();

        dirsDialog.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Back button pressed
                    if ( m_dir.equals(m_sdcardDirectory) ) {
                        // The very top level directory, do nothing
                        return false;
                    }
                    else {
                        // Navigate back to an upper directory
                        m_dir = new File(m_dir).getParent();
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

    private boolean createSubDir(String newDir) {
        File newDirFile = new File(newDir);
        if (! newDirFile.exists() ) {
            return newDirFile.mkdir();
        }

        return false;
    }

    private List<String> getDirectories(String dir) {
        List<String> dirs = new ArrayList<>();

        try {
            File dirFile = new File(dir);
            if (! dirFile.exists() || ! dirFile.isDirectory()) {
                return dirs;
            }
            dirs.add("..");
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

    private AlertDialog.Builder createDirectoryChooserDialog(String title, List<String> listItems,
                                                             OnClickListener onClickListener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(m_context);

        // Create custom view for AlertDialog title containing
        // current directory TextView and possible 'New folder' button.
        // Current directory TextView allows long directory path to be wrapped to multiple lines.
        LinearLayout titleLayout = new LinearLayout(m_context);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(m_context);
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        m_currentDirView = new TextView(m_context);
        m_titleView = new TextView(m_context);

        m_currentDirView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            m_currentDirView.setTextAppearance(m_context, android.R.style.TextAppearance_Medium);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            m_currentDirView.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        m_currentDirView.setTextColor(ContextCompat.getColor(m_context, android.R.color.white));
        m_currentDirView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        m_currentDirView.setText(title);
        m_currentDirView.setPadding(25, 5, 25, 25);
        m_currentDirView.setBackgroundColor(Color.parseColor(primaryColor));

        m_titleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            m_titleView.setTextAppearance(m_context, android.R.style.TextAppearance_Medium);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            m_titleView.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        m_titleView.setTextColor(ContextCompat.getColor(m_context, android.R.color.white));
        m_titleView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        m_titleView.setText(m_context.getResources().getString(R.string.CurrentDirectory));
        m_titleView.setPadding(25, 25, 25, 5);
        m_titleView.setBackgroundColor(Color.parseColor(primaryColor));

        /*Button newDirButton = new Button(m_context);
        newDirButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        newDirButton.setText("New folder");
        newDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(m_context);

                // Show new folder name input dialog
                new AlertDialog.Builder(m_context).
                        setTitle("New folder name").
                        setView(input).setPositiveButton("OK", new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable newDir = input.getText();
                        String newDirName = newDir.toString();
                        // Create new directory
                        if (createSubDir(m_dir + "/" + newDirName)) {
                            // Navigate into the new directory
                            m_dir += "/" + newDirName;
                            updateDirectory();
                        } else {
                            Toast.makeText(
                                    m_context, "Failed to create '" + newDirName +
                                            "' folder", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
            }
        });

        if (! m_isNewFolderEnabled) {
            newDirButton.setVisibility(View.GONE);
        }*/

        titleLayout.addView(m_titleView);
        titleLayout.addView(m_currentDirView);
        //titleLayout.addView(newDirButton);

        dialogBuilder.setCustomTitle(titleLayout);

        m_listAdapter = createListAdapter(listItems);

        dialogBuilder.setSingleChoiceItems(m_listAdapter, -1, onClickListener);
        dialogBuilder.setCancelable(false);

        return dialogBuilder;
    }

    private void updateDirectory() {
        m_subdirs.clear();
        m_subdirs.addAll( getDirectories(m_dir) );
        m_currentDirView.setText(m_dir);

        m_listAdapter.notifyDataSetChanged();
    }

    private ArrayAdapter<String> createListAdapter(List<String> items) {
        return new ArrayAdapter<String>(m_context,
                android.R.layout.select_dialog_item, android.R.id.text1, items) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                if (v instanceof TextView) {
                    // Enable list item (directory) text wrapping
                    TextView tv = (TextView) v;
                    tv.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
                    tv.setEllipsize(null);
                }
                return v;
            }
        };
    }
}