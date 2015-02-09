// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.list;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MergeCursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.SparseArray;

import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.BookItem;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.Query;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.util.LogUtils;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

/*
 * A loader that queries the {@link ContentResolver} and returns a {@link Cursor}.
 * This class implements the {@link Loader} protocol in a standard way for
 * querying cursors, building on {@link AsyncTaskLoader} to perform the cursor
 * query on a background thread so that it does not block the application's UI.
 * 
 * <p>A LibraryLoader must be built with the full information for the query to
 * perform.
 */
public class LibraryLoader extends AsyncTaskLoader<List<BookItem>> {

    final ForceLoadContentObserver mObserver;
    List<BookItem> mBookItems;
    private List<Query> mQueryList;

    /**
     * Creates an empty unspecified CursorLoader. You must follow this with
     * calls to {@link #setQueryList(List<Query>)} to specify the query to
     * perform.
     */
    public LibraryLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    /**
     * Creates a fully-specified LibraryLoader.
     */
    public LibraryLoader(Context context, List<Query> queryList) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        mQueryList = queryList;
    }

    public void setQueryList(List<Query> queryList) {
        mQueryList = queryList;
    }

    /* Runs on a worker thread */
    @Override
    public List<BookItem> loadInBackground() {

        Cursor cursor = null;

        List<Cursor> cursorList = new ArrayList<Cursor>();

        for (Query query : mQueryList) {
            cursor = getContext().getContentResolver().query(query.uri, query.projection, query.selection, query.selectionArgs, query.sortOrder);

            if (cursor != null) {
                // Ensure the cursor window is filled
                cursor.getCount();
                // registerContentObserver(cursor, mObserver);
                cursorList.add(cursor);
            }
        }

        Cursor[] cursorArray = new Cursor[cursorList.size()];
        List<BookItem> bookItemList;
        if (cursorList.size() > 0) {
            bookItemList = createBookItems(new MergeCursor(cursorList.toArray(cursorArray)));
            for (Cursor c : cursorList) {
                c.close();
            }
        } else {
            // Return an empty book list
            bookItemList = new ArrayList<BookItem>();
        }

        return bookItemList;
    }


    /**
     * Registers an observer to get notifications from the content provider
     * when the cursor needs to be refreshed.
     */
    void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(mObserver);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(List<BookItem> bookItems) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }
        mBookItems = bookItems;

        if (isStarted()) {
            super.deliverResult(bookItems);
        }
    }


    /**
     * Starts an asynchronous load of the book list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     * <p/>
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mBookItems != null) {
            deliverResult(mBookItems);
        }
        if (takeContentChanged() || mBookItems == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
    }

    /**
     * Takes a list of books from the Cursor and arranges them into a list of BookItem objects
     */
    private List<BookItem> createBookItems(Cursor cursor) {

        // Check for error cases
        if (cursor == null || cursor.isClosed()) {
            String error = String.format("Trying to create a new library item list with %s cursor.", cursor == null ? "null" : "closed");
            Crashlytics.logException(new Exception(error));
            LogUtils.stack();
            List<BookItem> bookItems = new ArrayList<BookItem>();
            return bookItems;
        }

        cursor.moveToFirst();

        Book book;
        Bookmark bookmark;
        BookItem bookItem;

        List<BookItem> booksList = new ArrayList<BookItem>();

        while (!cursor.isAfterLast()) {
            book = BookHelper.createBook(cursor);
            bookmark = BookmarkHelper.createBookmark(cursor);

            bookItem = new BookItem(book, bookmark, "", "", null);
            booksList.add(bookItem);
            cursor.moveToNext();
        }

        return booksList;
    }

}
