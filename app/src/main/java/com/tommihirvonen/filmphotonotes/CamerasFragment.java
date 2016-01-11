package com.tommihirvonen.filmphotonotes;// Copyright 2015
// Tommi Hirvonen

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CamerasFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener{

    public static final String ARG_PAGE = "ARG_PAGE";

    TextView mainTextView;

    ListView mainListView;

    CameraAdapter mArrayAdapter;

    ArrayList<Camera> mCameraList;

    FilmDbHelper database;



    public static CamerasFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        CamerasFragment fragment = new CamerasFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        database = new FilmDbHelper(getContext());
        mCameraList = database.getAllCameras();

        final View view = linf.inflate(R.layout.cameras_fragment, container, false);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_cameras);
        fab.setOnClickListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String UIColor = prefs.getString("UIColor", "#ef6c00,#e65100");
        List<String> colors = Arrays.asList(UIColor.split(","));
        String primaryColor = colors.get(0);
        String secondaryColor = colors.get(1);

        // Also change the floating action button color. Use the darker secondaryColor for this.
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(secondaryColor)));

        mainTextView = (TextView) view.findViewById(R.id.no_added_cameras);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.main_cameraslistview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new CameraAdapter(getActivity(), android.R.layout.simple_list_item_1, mCameraList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        if ( mCameraList.size() >= 1 ) mainTextView.setVisibility(View.GONE);

        mArrayAdapter.notifyDataSetChanged();

        return view;
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab_lenses:
                //showLensNameDialog();
                break;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

}
