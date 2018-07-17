package com.deutschgmail.nate.deutschaudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by MIXTAPE on 12/20/2017.
 */

public class MusicAdapter extends BaseAdapter {
    private ArrayList<GenericDisplay> listData;
    private LayoutInflater layoutInflater;
    private String filter;

    public MusicAdapter(Context aContext, ArrayList<GenericDisplay> listData) {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.music_row, null);
            holder = new ViewHolder();
            holder.titleView = (TextView) convertView.findViewById(R.id.entryTitle);
            holder.subtitleView = (TextView) convertView.findViewById(R.id.entrySubtitle);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.titleView.setText(listData.get(position).getTitle());
        holder.subtitleView.setText(listData.get(position).getSubtitle());
        return convertView;
    }

    static class ViewHolder {
        TextView titleView;
        TextView subtitleView;
    }
}