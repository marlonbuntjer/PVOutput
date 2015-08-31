package com.github.marlonbuntjer.pvoutput;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Marlon Buntjer on 23-6-2015.
 */
public class StringArrayAdapterDaily extends ArrayAdapter<String[]> {
    private final Context context;
    private final List<String[]> data;
    private final int layoutResourceId;

    public StringArrayAdapterDaily(Context context, int layoutResourceId, List<String[]> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.data = data;
        this.layoutResourceId = layoutResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.textView1 = (TextView) row.findViewById(R.id.firstColumn);
            holder.imageView1 = (ImageView) row.findViewById(R.id.weatherIcon);
            holder.textView2 = (TextView) row.findViewById(R.id.secondColumn);
            holder.textView3 = (TextView) row.findViewById(R.id.thirdColumn);


            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        String[] record = data.get(position);

        holder.textView1.setText(record[0]);
        holder.textView2.setText(record[1]);
        holder.textView3.setText(record[2]);


        int weatherIcon = 0;
        switch (record[3]) {
            case "Fine":
                weatherIcon = R.drawable.ic_fine;
                break;
            case "Partly Cloudy":
                weatherIcon = R.drawable.ic_partly_cloudy;
                break;
            case "Mostly Cloudy":
                weatherIcon = R.drawable.ic_mostly_cloudy;
                break;
            case "Cloudy":
                weatherIcon = R.drawable.ic_cloudy;
                break;
            case "Showers":
                weatherIcon = R.drawable.ic_showers;
                break;
            case "Snow":
                weatherIcon = R.drawable.ic_snow;
                break;
        }
        holder.imageView1.setImageResource(weatherIcon);


        return row;
    }

    static class ViewHolder {
        TextView textView1;
        ImageView imageView1;
        TextView textView2;
        TextView textView3;
    }
}

