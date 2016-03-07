package com.mcproject.rounak.mcapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class BTArrayAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<String> values;

    public BTArrayAdapter(Context context, List<String> values) {
        super(context, R.layout.row_layout, values);
        this.context = context;
        this.values = values;
    }

    public View getView(int position, View convert, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("ViewHolder") View row = inflater.inflate(R.layout.row_layout, parent, false);
        TextView neighbor = (TextView) row.findViewById(R.id.neighbor);
        TextView rssi = (TextView) row.findViewById(R.id.rssi);

        String[] unpacked = values.get(position).split(":::");
        String nStr = unpacked[0];
        String dStr = unpacked[1];

        neighbor.setText(nStr);
        rssi.setText(dStr);

        return row;
    }

}
