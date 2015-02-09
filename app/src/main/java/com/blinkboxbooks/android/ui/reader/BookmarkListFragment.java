// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.ui.reader;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.list.BookmarkCursorAdapter;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.provider.BBBContract.BookmarksColumns;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * This class displays a list of bookmarks for a given book id
 */
public class BookmarkListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String PARAM_BOOK = "book";
    private static final int BOOKMARK_LOADER_ID = 0;

    private Uri mBookmarksUri;
    private CursorAdapter mAdapter;
    private ListView mListView;

    protected TextView mTextViewEmptyList;
    protected Book mBook;

    protected CursorAdapter createListAdapter() {
        return new BookmarkCursorAdapter(getActivity(), null, 0, R.layout.list_item_bookmark);
    }

    protected Uri createBookmarksUri(long bookId) {
        return Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, bookId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_bookmark_list, container, false);

        mTextViewEmptyList = (TextView) layout.findViewById(android.R.id.empty);

        mListView = (ListView) layout.findViewById(android.R.id.list);
        mListView.setEmptyView(mTextViewEmptyList);

        mBook = (Book) getArguments().getSerializable(PARAM_BOOK);
        mBookmarksUri = createBookmarksUri(mBook.id);

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Prepare the loader.
        getLoaderManager().initLoader(BOOKMARK_LOADER_ID, getArguments(), this);

        mAdapter = createListAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                final Bookmark bookmark = BookmarkHelper.createBookmark(cursor);
                BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());
                builder.setTitle(R.string.title_save_your_reading_position);
                builder.setMessage(getString(R.string.dialog_keep_reading_position));
                builder.setNegativeButton(R.string.dont_save, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        goToBookmark(bookmark, false);
                    }
                });

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        goToBookmark(bookmark, true);
                    }
                }).show();
            }
        });
    }

    private void goToBookmark(Bookmark bookmark, boolean keepCurrentReadingPosition) {
        Intent intent = new Intent(getActivity(), ReaderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Uri uri = Uri.parse("bbb://app/reader/" + ReaderActivity.ACTION_GOTO_BOOKMARK);
        intent.setData(uri);

        intent.putExtra(ReaderActivity.PARAM_BOOKMARK, bookmark);
        intent.putExtra(ReaderActivity.PARAM_KEEP_READING_POSITION, keepCurrentReadingPosition);

        startActivity(intent);
    }

    /*
     * (non-Javadoc)
     * @see android.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == BOOKMARK_LOADER_ID) {
            return new CursorLoader(getActivity(), mBookmarksUri, null, null, null, BookmarksColumns.BOOKMARK_UPDATE_DATE + " DESC");
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see android.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.setNotificationUri(getActivity().getContentResolver(), Bookmarks.CONTENT_URI);

        // Swap the new cursor in. (The framework will take care of closing the old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    /*
     * (non-Javadoc)
     * @see android.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished() above is about to be closed. We need
        // to make sure we are no longer using it.
        mAdapter.swapCursor(null);
    }
}
