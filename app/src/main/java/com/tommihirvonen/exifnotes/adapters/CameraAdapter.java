package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;

import java.util.List;

/**
 * CameraAdapter links an ArrayList of Cameras and a ListView together.
 */
public class CameraAdapter extends ArrayAdapter<Camera> {

    /**
     * Reference to the singleton database
     */
    private final FilmDbHelper database = FilmDbHelper.getInstance(getContext());

    /**
     * {@inheritDoc}
     *  @param context
     * @param cameras
     */
    public CameraAdapter(Context context, List<Camera> cameras) {
        super(context, android.R.layout.simple_list_item_1, cameras);
    }

    /**
     * This function inflates a view in the ListView to display a Camera's information.
     *
     * @param position the position of the item in the list.
     * @param convertView the view to be inflated
     * @param parent the parent to which the view will eventually be attached.
     * @return the inflated view to be showed in the ListView
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        Camera camera = getItem(position);
        List<Lens> mountableLenses = database.getMountableLenses(camera);

        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView nameTextView = (TextView) convertView.findViewById(R.id.tv_gear_name);
        TextView mountablesTextView = (TextView) convertView.findViewById(R.id.tv_mountables);
        // Populate the data into the template view using the data object
        if (camera != null) nameTextView.setText(camera.getMake() + " " + camera.getModel());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getContext().getResources().getString(R.string.MountsTo));
        for (Lens lens : mountableLenses) {
            stringBuilder.append("\n- ").append(lens.getMake()).append(" ").append(lens.getModel());
        }
        String mountablesString = stringBuilder.toString();
        mountablesTextView.setText(mountablesString);

        return convertView;
    }
}
