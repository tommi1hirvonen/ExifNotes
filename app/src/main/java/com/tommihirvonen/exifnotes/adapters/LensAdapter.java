package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.List;

/**
 * LensAdapter links an ArrayList of Lenses and a ListView together.
 */
public class LensAdapter extends ArrayAdapter<Lens> {

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database = FilmDbHelper.getInstance(getContext());

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param textViewResourceId
     * @param lenses
     */
    public LensAdapter(Context context, int textViewResourceId, List<Lens> lenses) {
        super(context, textViewResourceId, lenses);
    }

    /**
     * This function inflates a view in the ListView to display a Lens' information.
     *
     * @param position the position of the item in the list.
     * @param convertView the view to be inflated
     * @param parent the parent to which the view will eventually be attached.
     * @return the inflated view to be showed in the ListView
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        Lens lens = getItem(position);
        List<Camera> mountableCameras = database.getMountableCameras(lens);
        List<Filter> mountableFilters = database.getMountableFilters(lens);

        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView nameTextView = (TextView) convertView.findViewById(R.id.tv_gear_name);
        TextView mountablesTextView = (TextView) convertView.findViewById(R.id.tv_mountables);

        // Populate the data into the template view using the data object
        if (lens != null) {
            nameTextView.setText(lens.getMake() + " " + lens.getModel());

            //Build string to display mountable cameras and filters
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getContext().getResources().getString(R.string.MountsTo));
            for (Camera camera : mountableCameras) {
                stringBuilder.append("\n- ").append(camera.getMake()).append(" ").append(camera.getModel());
            }
            //Add extra line change if there are mountable filters
            if (mountableFilters.size() > 0) stringBuilder.append("\n");
            for (Filter filter : mountableFilters) {
                stringBuilder.append("\n- ").append(filter.getMake()).append(" ").append(filter.getModel());
            }
            String mountablesString = stringBuilder.toString();
            mountablesTextView.setText(mountablesString);
        }
        return convertView;
    }
}
