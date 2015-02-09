// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.blinkbox.java.book.exceptions.BBBEPubException;
import com.blinkbox.java.book.factory.BBBEPubFactory;
import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.BookItem;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.util.EncryptionUtil;
import com.blinkboxbooks.android.util.LogUtils;
import com.crashlytics.android.Crashlytics;

import java.io.File;

/**
 * Controller for loading books
 */
public class BookFileLoader extends AsyncTaskLoader<BookItem> {

    private static final String TAG = BookFileLoader.class.getSimpleName();

    /**
     * Load an ePub book given a Book object
     *
     * @param context
     * @param book
     */
    public static BBBEPubBook loadBook(Context context, Book book) {
        String filePath = book.file_path;
        char[] encryptionKey = null;

        if (book.enc_key != null) {
            encryptionKey = EncryptionUtil.decryptEncryptionKey(book.enc_key);
        }

        // Load the ePub book
        BBBEPubBook epub = null;
        try {
            epub = BBBEPubFactory.getInstance().createFromURL(context, filePath, encryptionKey);
        } catch (BBBEPubException e) {
            LogUtils.e(TAG, e.getMessage(), e);

            // See Bug ALA-1810
            // This is potentially a serious problem that we don't currently understand why it happens so frequently.
            // So we log any information that might be useful to help us resolve the problem in the future.
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("loadBook (ISBN=" + book.isbn + ") FAILED: " + filePath);
            File file = new File(filePath);

            if (file != null) {
                if (file.exists()) {
                    debugInfo.append(" FILE EXISTS size = " + file.getTotalSpace() + " can read = " + file.canRead());
                } else {
                    debugInfo.append(" ***FILE DOES NOT EXIST***");

                    File parentFile = file.getParentFile();
                    // Check if the parent directory exists
                    if (parentFile != null && parentFile.exists()) {
                        debugInfo.append(" PARENT DIRECTORY DOES EXIST");
                    } else {
                        debugInfo.append(" ***PARENT DOES NOT EXIST***");

                        // Check if the books directory exists
                        File externalFilesDir = context.getExternalFilesDir(null);

                        if (externalFilesDir != null && externalFilesDir.exists()) {
                            debugInfo.append(" EXTERNAL FILES DIR DOES EXIST and can read = " + externalFilesDir.canRead());
                        } else {
                            debugInfo.append(" ***EXTERNAL FILES DIR DOES NOT EXIST***");
                        }
                    }
                }
            }

            Crashlytics.log("Invalid epub :"+ debugInfo.toString());
            Crashlytics.logException(e);
        }
        return epub;
    }

    private final Context mContext;
    private final Uri mUri;

    public BookFileLoader(Context context, Uri uri) {
        super(context);

        mContext = context;
        mUri = uri;
    }

    @Override
    public BookItem loadInBackground() {
        Book book = BookHelper.getBookFromUri(mUri);

        if(book == null) {
            return null;
        }

        Bookmark lastPosition = BookmarkHelper.getBookmark(Bookmarks.TYPE_LAST_POSITION, book.id);

        LogUtils.d(TAG, "Opening book id " + book.id + " ISBN: " + book.isbn + " at " + book.file_path);

        if (lastPosition == null || book.state == BBBContract.Books.BOOK_STATE_FINISHED) {
            lastPosition = BookmarkHelper.createBookmark(book.id);
            lastPosition.type = Bookmarks.TYPE_LAST_POSITION;
            lastPosition.percentage = 0;
            lastPosition.isbn = book.isbn;
            BookmarkHelper.updateBookmark(lastPosition, true);
        } else {
            LogUtils.d(BookFileLoader.class.getSimpleName(), "Bookmark last position " + lastPosition.position + " (" + lastPosition.percentage + "%) " + lastPosition.id + " cloud " + lastPosition.cloud_id);
        }

        BBBEPubBook epub = loadBook(mContext, book);

        Bookmark[] bookmarksResult = createBookmarksOfType(Bookmarks.TYPE_BOOKMARK,book.id);
        Bookmark[] highlightsResult = createBookmarksOfType(Bookmarks.TYPE_HIGHLIGHT,book.id);

        return new BookItem(book, lastPosition, BookmarkHelper.convertBookmarksToString(bookmarksResult), BookmarkHelper.convertBookmarksToString(highlightsResult), epub);
    }

    private Bookmark[] createBookmarksOfType(int type, long bookId) {
        final Uri uri = Bookmarks.buildBookmarkTypeUri(type, bookId);
        Cursor cursor = BBBApplication.getApplication().getContentResolver().query(uri, null, null, null, null);
        try {
            final int count = cursor.getCount();
            Bookmark[] bookmarksResult = new Bookmark[count];
            for (int i = 0; i < count && cursor.moveToNext(); i++) {
                bookmarksResult[i] = BookmarkHelper.createBookmark(cursor);
            }
            return bookmarksResult;
        } finally {
            cursor.close();
        }
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }
}
