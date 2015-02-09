// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.MenuListItem;

/**
 * A list adapter that shows the main menu contents
 */
public class MenuAdapter extends ArrayAdapter<MenuListItem> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_MAX_COUNT = 2;

    private final int mTextViewResourceId;
    private int mNumItemsCurrentlyReading;
    private int mNumItemsMyLibrary;
    private Context mContext;

    public MenuAdapter(Context context, int textViewResourceId, MenuListItem[] objects) {
        super(context, textViewResourceId, objects);
        mTextViewResourceId = textViewResourceId;
        mContext = context;
    }

    public MenuAdapter(Context context, int textViewResourceId, MenuListItem[] objects, int numItemsCurrentlyReading, int numItemsMyLibrary) {
        super(context, textViewResourceId, objects);
        mTextViewResourceId = textViewResourceId;
        mContext = context;
        mNumItemsCurrentlyReading = numItemsCurrentlyReading;
        mNumItemsMyLibrary = numItemsMyLibrary;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).header)
            return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @Override
    public boolean isEnabled(int position) {
        return !getItem(position).header;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        MenuListItem menuItem = getItem(position);
        int type = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();
            switch (type) {
                case TYPE_HEADER:
                    convertView = View.inflate(getContext(), R.layout.menu_list_header, null);
                    break;
                default:
                    convertView = View.inflate(getContext(), mTextViewResourceId, null);
                    holder.imageViewNoNetwork = (ImageView) convertView.findViewById(android.R.id.icon);
                    break;
            }
            holder.textviewTitle = (TextView) convertView.findViewById(android.R.id.text1);
            holder.textViewAdditional = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (type == TYPE_HEADER) {
            holder.textviewTitle.setText(menuItem.title);
            holder.textViewAdditional.setText(menuItem.additional == null ? "" : menuItem.additional);
        } else {

            // Special case handling for library items
            if (menuItem.title.equals(mContext.getString(R.string.currently_reading))) {
                holder.textviewTitle.setText(menuItem.title + " (" + mNumItemsCurrentlyReading + ")");
            } else if (menuItem.title.equals(mContext.getString(R.string.my_library_lower_l))) {
                holder.textviewTitle.setText(menuItem.title + " (" + mNumItemsMyLibrary + ")");
            } else {
                holder.textviewTitle.setText(menuItem.title);
            }
            holder.textviewTitle.setEnabled(menuItem.enabled);
            holder.textViewAdditional.setText(menuItem.additional == null ? "" : menuItem.additional);
            holder.textViewAdditional.setEnabled(menuItem.enabled);
            if (menuItem.selected) {
                holder.textviewTitle.setSelected(true);
                holder.textviewTitle.requestFocus();
            }
            holder.textviewTitle.setCompoundDrawablesWithIntrinsicBounds(menuItem.iconResourceId, 0, 0, 0);
            holder.imageViewNoNetwork.setVisibility(menuItem.enabled || !menuItem.showNetworkError ? View.GONE : View.VISIBLE);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView textviewTitle;
        TextView textViewAdditional;
        ImageView imageViewNoNetwork;
    }
}
