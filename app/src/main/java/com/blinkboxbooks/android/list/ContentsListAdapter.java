// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.list;

import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.ui.reader.TableOfContentsFragment.TOCEntry;

import java.util.List;

/**
 * A list adapter that shows the table of content.
 */
public class ContentsListAdapter extends ArrayAdapter<TOCEntry> {

    private final int padding;
    private final int mTextViewResource;

    public ContentsListAdapter(Context context, int textViewResourceId, List<TOCEntry> objects) {
        super(context, textViewResourceId, objects);
        mTextViewResource = textViewResourceId;
        padding = context.getResources().getDimensionPixelOffset(R.dimen.gap_medium);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(getContext(), mTextViewResource, null);
            holder.textviewLabel = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TOCEntry tocEntry = getItem(position);
        holder.textviewLabel.setPadding(tocEntry.depth * padding, 0, 0, 0);
        holder.textviewLabel.setText(tocEntry.label);
        holder.textviewLabel.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG|Paint.ANTI_ALIAS_FLAG);
        holder.textviewLabel.setEnabled(tocEntry.active);
        return convertView;
    }

    private static class ViewHolder {
        TextView textviewLabel;
    }
}
