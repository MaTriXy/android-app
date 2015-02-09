// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.list;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;

/**
 * Creates views for a list of highlights
 */
public class NoteCursorAdapter extends BookmarkCursorAdapter {

    /**
     * Handles Note click events
     */
    public interface NoteClickListener {
        void onDeleteHighlightClicked(long bookmarkId);

        void onDeleteButtonClicked(int type, long bookmarkId);

        void onEditButtonClicked(int type, long bookmarkId);
    }

    private final NoteClickListener noteClickListener;

    public NoteCursorAdapter(Activity activity, Cursor c, int flags, int resourceId, NoteClickListener listener) {
        super(activity, c, flags, resourceId);
        noteClickListener = listener;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        final int type = cursor.getInt(cursor.getColumnIndex(BBBContract.BookmarksColumns.BOOKMARK_TYPE));
        String name = cursor.getString(cursor.getColumnIndex(BBBContract.BookmarksColumns.BOOKMARK_NAME));
        int percentage = cursor.getInt(cursor.getColumnIndex(BBBContract.BookmarksColumns.BOOKMARK_PERCENTAGE));
        String content = cursor.getString(cursor.getColumnIndex(BBBContract.BookmarksColumns.BOOKMARK_CONTENT));
        //We do not currently support notes within highlights but this will probably come back so this is commented out for now
        //String annotation = cursor.getString(cursor.getColumnIndex(BookmarksColumns.BOOKMARK_ANNOTATION));

        ImageView imageViewHighlight = (ImageView) view.findViewById(R.id.imageview_highlight);
        TextView textViewTitle = (TextView) view.findViewById(R.id.textview_bookmark_title);
        TextView textViewProgress = (TextView) view.findViewById(R.id.textview_bookmark_progress);
        TextView textViewContent = (TextView) view.findViewById(R.id.textview_bookmark_content);
        ImageButton imageButtonDeleteHighlight = (ImageButton) view.findViewById(R.id.imagebutton_delete);

        //We do not currently support notes within highlights but this will probably come back so this is commented out for now
        //ImageButton imageButtonEdit = (ImageButton) view.findViewById(R.id.imagebutton_editnote);
        //ImageButton imageButtonDelete = (ImageButton) view.findViewById(R.id.imagebutton_deletenote);
        //TextView textViewNote = (TextView) view.findViewById(R.id.textview_note);

        textViewTitle.setText(context.getString(R.string.bookmark_title, name));
        textViewProgress.setText(context.getString(R.string.bookmark_progress, percentage));
        textViewContent.setText(Html.fromHtml(content));
        //We do not currently support notes within highlights but this will probably come back so this is commented out for now
        //textViewNote.setText(annotation);

        if (type == Bookmarks.TYPE_HIGHLIGHT) {
            imageViewHighlight.setVisibility(View.VISIBLE);
            imageButtonDeleteHighlight.setVisibility(View.VISIBLE);
            imageButtonDeleteHighlight.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    noteClickListener.onDeleteHighlightClicked(id);
                }
            });
        } else {
            imageViewHighlight.setVisibility(View.GONE);
            imageButtonDeleteHighlight.setVisibility(View.GONE);
        }

        //We do not currently support notes within highlights but this will probably come back so this is commented out for now
        //imageButtonEdit.setOnClickListener(new OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        noteClickListener.onEditButtonClicked(type, id);
        //    }
        //});
        //imageButtonDelete.setOnClickListener(new OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        noteClickListener.onDeleteButtonClicked(type, id);
        //    }
        //});
    }
}
