// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.ui.reader;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.list.HighlightCursorAdapter;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;

/**
 * Displays a list of the users highlights
 */
public class HighlightListFragment extends BookmarkListFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mTextViewEmptyList.setText(R.string.you_do_not_have_any_highlights_currently);
        return view;
    }

    @Override
    protected CursorAdapter createListAdapter() {
        return new HighlightCursorAdapter(getActivity(), null, 0);
    }

    @Override
    protected Uri createBookmarksUri(long bookId) {
        return Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_HIGHLIGHT, bookId);
    }
}
