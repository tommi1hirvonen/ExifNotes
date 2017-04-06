package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;

import java.util.List;

/**
 * FilterAdapter links an ArrayList of Filters and a ListView together.
 */
public class FilterAdapter extends ArrayAdapter<Filter> {

    private final FilmDbHelper database = FilmDbHelper.getInstance(getContext());

    public FilterAdapter(Context context, List<Filter> filters) {
        super(context, android.R.layout.simple_list_item_1, filters);
    }

    /**
     * This function inflates a view in the ListView to display a Filter's information.
     *
     * @param position the position of the item in the list.
     * @param convertView the view to be inflated
     * @param parent the parent to which the view will eventually be attached.
     * @return the inflated view to be showed in the ListView
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        Filter filter = getItem(position);
        List<Lens> mountableLenses = database.getMountableLenses(filter);

        // Check if an existing view is being used, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gear, parent, false);
        }
        // Lookup view for data population
        TextView nameTextView = (TextView) convertView.findViewById(R.id.tv_gear_name);
        TextView mountablesTextView = (TextView) convertView.findViewById(R.id.tv_mountables);

        // Populate the data into the template view using the data object
        if (filter != null) nameTextView.setText(filter.getMake() + " " + filter.getModel());

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
