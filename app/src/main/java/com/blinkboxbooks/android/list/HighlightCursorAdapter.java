// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.list;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;

/**
 * Creates views for a list of highlights
 */
public class HighlightCursorAdapter extends CursorAdapter {

    private Activity mActivity;
    private LayoutInflater mInflater;

    private static class ViewHolder {
        TextView mTextViewTitle;
        TextView mTextViewProgress;
        TextView mTextViewContent;
        View mImageButtonDelete;

        private ViewHolder(View view) {
            mTextViewTitle = (TextView) view.findViewById(R.id.textview_highlight_title);
            mTextViewProgress = (TextView) view.findViewById(R.id.textview_highlight_progress);
            mTextViewContent = (TextView) view.findViewById(R.id.textview_highlight_content);
            mImageButtonDelete = view.findViewById(R.id.imagebutton_delete);
        }

        private void setHighlight(final Activity context, final Bookmark bookmark) {
            mTextViewTitle.setText(context.getString(R.string.bookmark_title, bookmark.name == null ? "" : bookmark.name));
            mTextViewProgress.setText(context.getString(R.string.bookmark_progress, bookmark.percentage));
            mTextViewContent.setText(bookmark.content == null ? "" : Html.fromHtml(bookmark.content));

            mImageButtonDelete.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    BookmarkHelper.deleteHighlight(context, bookmark);
                }
            });
        }
    }

    public HighlightCursorAdapter(Activity activity, Cursor c, int flags) {
        super(activity, c, flags);
        mActivity = activity;
        mInflater = LayoutInflater.from(mActivity);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final Bookmark bookmark = BookmarkHelper.createBookmark(cursor);
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.setHighlight(mActivity, bookmark);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.list_item_highlight, parent, false);
        v.setTag(new ViewHolder(v));
        bindView(v, context, cursor);
        return v;
    }
}
