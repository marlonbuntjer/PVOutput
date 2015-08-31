package com.github.marlonbuntjer.pvoutput;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Marlon Buntjer on 23-6-2015.
 */
public class StringArrayAdapter extends ArrayAdapter<String[]> {
    private final Context context;
    private final List<String[]> data;
    private final int layoutResourceId;

    public StringArrayAdapter(Context context, int layoutResourceId, List<String[]> data) {
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

        return row;
    }

    static class ViewHolder {
        TextView textView1;
        TextView textView2;
        TextView textView3;
    }
}