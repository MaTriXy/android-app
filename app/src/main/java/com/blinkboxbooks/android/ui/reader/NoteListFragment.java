// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.ui.reader;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.list.BookmarkCursorAdapter;
import com.blinkboxbooks.android.list.NoteCursorAdapter;
import com.blinkboxbooks.android.list.NoteCursorAdapter.NoteClickListener;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Display a list of the users notes
 */
public class NoteListFragment extends HighlightListFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mTextViewEmptyList.setText(R.string.you_do_not_have_any_notes_currently);
        return view;
    }

    @Override
    protected BookmarkCursorAdapter createListAdapter() {
        return new NoteCursorAdapter(getActivity(), null, 0, R.layout.list_item_highlight, new NoteClickListener() {
            @Override
            public void onDeleteButtonClicked(final int type, final long bookmarkId) {
                BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());
                builder.setMessage(R.string.dialog_are_you_sure_you_want_to_delete_this_note);
                builder.setNegativeButton(android.R.string.no, null);
                builder.setPositiveButton(android.R.string.yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (type == Bookmarks.TYPE_HIGHLIGHT) {
                            BookmarkHelper.deleteNoteFromHighlight(bookmarkId);
                        } else {
                            BookmarkHelper.deleteBookmark(bookmarkId);
                        }
                    }
                }).show();
            }

            @Override
            public void onEditButtonClicked(int type, long bookmarkId) {

            }

            @Override
            public void onDeleteHighlightClicked(final long bookmarkId) {
                BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());
                builder.setMessage(R.string.dialog_are_you_sure_you_want_to_delete_this_highlight);
                builder.setNegativeButton(android.R.string.no, null);
                builder.setPositiveButton(android.R.string.yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BookmarkHelper.deleteHighlightFromNote(bookmarkId);
                    }
                }).show();
            }
        });
    }

    @Override
    protected Uri createBookmarksUri(long bookId) {
        return Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_NOTE, bookId);
    }
}
