// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.provider.BBBContract.Authors;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.provider.BBBContract.BookmarksColumns;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.provider.BBBContract.BooksColumns;
import com.blinkboxbooks.android.provider.BBBContract.Libraries;
import com.blinkboxbooks.android.provider.BBBContract.LibrariesColumns;
import com.blinkboxbooks.android.provider.BBBContract.NavPoints;
import com.blinkboxbooks.android.provider.BBBContract.ReaderSettings;
import com.blinkboxbooks.android.provider.BBBContract.ReaderSettingsColumns;
import com.blinkboxbooks.android.provider.BBBContract.Sections;
import com.blinkboxbooks.android.provider.BBBContract.SyncState;
import com.blinkboxbooks.android.util.SelectionBuilder;

import java.util.ArrayList;

/**
 * Provider that stores {@link BBBContract} data.
 */
public class BBBProvider extends ContentProvider {

    private BBBDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int LIBRARY = 1;
    private static final int LIBRARY_ID = 2;
    private static final int LIBRARY_ACCOUNT = 3;

    private static final int BOOKS = 100;
    private static final int BOOKS_ID = 101;
    private static final int BOOKS_ACCOUNT = 102;
    private static final int BOOKS_ALL = 103;
    private static final int BOOKS_STATUS = 104;
    private static final int BOOKS_RECENT_PURCHASE = 105;
    private static final int BOOKS_SYNCHRONIZATION = 106;
    private static final int BOOKS_SERVER_ID = 107;
    private static final int BOOKS_DOWNLOAD_ID = 108;
    private static final int BOOKS_READING_STATUS_ID = 109;
    private static final int BOOKS_ACTIVE_ACCOUNT = 110;

    private static final int AUTHOR_ID = 201;

    private static final int NAVPOINT_ID = 301;

    private static final int SECTION_ID = 401;

    private static final int BOOKMARKS_TYPE = 500;
    private static final int BOOKMARK_ID = 501;
    private static final int BOOKMARKS_SYNCHRONIZATION = 502;
    private static final int BOOKMARKS_SERVER_ID = 503;

    private static final int READER_SETTINGS = 601;

    private static final int SEARCH_SUGGEST = 900;


    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = BBBContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, "libraries", LIBRARY);
        matcher.addURI(authority, "libraries/account/*", LIBRARY_ACCOUNT);
        matcher.addURI(authority, "libraries/id/*", LIBRARY_ID);
        matcher.addURI(authority, "books/", BOOKS);
        matcher.addURI(authority, "books/account/active", BOOKS_ACTIVE_ACCOUNT);
        matcher.addURI(authority, "books/account/id/*", BOOKS_ID);
        matcher.addURI(authority, "books/account/*/all", BOOKS_ALL);
        matcher.addURI(authority, "books/account/*/status/*", BOOKS_STATUS);
        matcher.addURI(authority, "books/account/*/recent_purchase/*", BOOKS_RECENT_PURCHASE);
        matcher.addURI(authority, "books/account/*/synchronization", BOOKS_SYNCHRONIZATION);
        matcher.addURI(authority, "books/account/*/server_id/*", BOOKS_SERVER_ID);
        matcher.addURI(authority, "books/account/*", BOOKS_ACCOUNT);
        matcher.addURI(authority, "books/download/id/*", BOOKS_DOWNLOAD_ID);
        matcher.addURI(authority, "books/reading_status/id/*", BOOKS_READING_STATUS_ID);
        matcher.addURI(authority, "authors/id/*", AUTHOR_ID);
        matcher.addURI(authority, "navpoints/id/*", NAVPOINT_ID);
        matcher.addURI(authority, "sections/id/*", SECTION_ID);
        matcher.addURI(authority, "bookmarks/id/*", BOOKMARK_ID);
        matcher.addURI(authority, "bookmarks/type/*/book_id/*", BOOKMARKS_TYPE);
        matcher.addURI(authority, "bookmarks/account/*/synchronization", BOOKMARKS_SYNCHRONIZATION);
        matcher.addURI(authority, "bookmarks/account/*/server_id/*", BOOKMARKS_SERVER_ID);
        matcher.addURI(authority, "reader_settings/account/*", READER_SETTINGS);

        return matcher;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new BBBDatabase(getContext());
        return true;
    }

	/*
     * private void deleteDatabase() { // TODO: wait for content provider
	 * operations to finish, then tear down mOpenHelper.close(); Context context
	 * = getContext(); BBBDatabase.deleteDatabase(context); mOpenHelper = new
	 * BBBDatabase(getContext()); }
	 */

    /*
     * (non-Javadoc)
     *
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LIBRARY:
            case LIBRARY_ACCOUNT:
                return Libraries.CONTENT_TYPE;
            case LIBRARY_ID:
                return Libraries.CONTENT_ITEM_TYPE;
            case BOOKS:
            case BOOKS_ACCOUNT:
            case BOOKS_ALL:
            case BOOKS_ACTIVE_ACCOUNT:
                return Books.CONTENT_TYPE;
            case BOOKS_ID:
                return Books.CONTENT_ITEM_TYPE;
            case AUTHOR_ID:
                return Authors.CONTENT_TYPE;
            case NAVPOINT_ID:
                return NavPoints.CONTENT_TYPE;
            case SECTION_ID:
                return Sections.CONTENT_TYPE;
            case BOOKMARK_ID:
                return Bookmarks.CONTENT_TYPE;
            case BOOKS_STATUS:
                return Books.CONTENT_TYPE;
            case BOOKS_RECENT_PURCHASE:
                return Books.CONTENT_TYPE;
            case READER_SETTINGS:
                return ReaderSettings.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.ContentProvider#query(android.net.Uri,
     * java.lang.String[], java.lang.String, java.lang.String[],
     * java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS_STATUS: {
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
            }

            case BOOKS_RECENT_PURCHASE: {
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                return builder.where(selection, selectionArgs).query(db, projection, Books.PURCHASE_DATE_SORT);

            }
            default: {
                // Most cases are handled with expanded SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
            }

            case SEARCH_SUGGEST: {
                return null;
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}.
     *
     * @param uri
     * @param match
     * @return
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {

        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case BOOKS: {
                return builder.table(Books.TABLE_NAME);
            }
            case BOOKS_ACCOUNT: {
                final String account = Books.getUserAccount(uri);
                return builder.table(Books.BOOKS_JOIN_LIBRARIES).where(LibrariesColumns.LIBRARY_ACCOUNT + "=? ", account);
            }
            case BOOKS_ALL: {
                final String account = Books.getUserAccount(uri);
                return builder.table(Books.BOOKS_JOIN_LIBRARIES_BOOKMARKS).where("libraries." + LibrariesColumns.LIBRARY_ACCOUNT + "=? "
                        + "AND books." + BooksColumns.BOOK_SYNC_STATE + "!=? "
                        + "AND (" + BookmarksColumns.BOOKMARK_TYPE + "=? OR " + BookmarksColumns.BOOKMARK_TYPE + " is NULL)", account, Integer.toString(Books.STATE_DELETED), Integer.toString(BBBContract.Bookmarks.TYPE_LAST_POSITION));
            }
            case BOOKS_ACTIVE_ACCOUNT: {
                final String account = AccountController.getInstance().getUserId();
                return builder.table(Books.BOOKS_JOIN_LIBRARIES_BOOKMARKS).where("libraries." + LibrariesColumns.LIBRARY_ACCOUNT + "=? "
                        + "AND books." + BooksColumns.BOOK_SYNC_STATE + "!=? "
                        + "AND (" + BookmarksColumns.BOOKMARK_TYPE + "=? OR " + BookmarksColumns.BOOKMARK_TYPE + " is NULL)", account, Integer.toString(Books.STATE_DELETED), Integer.toString(BBBContract.Bookmarks.TYPE_LAST_POSITION));
            }
            case BOOKS_SYNCHRONIZATION: {
                final String account = Books.getUserAccount(uri);
                return builder.table(Books.BOOKS_JOIN_LIBRARIES).where(LibrariesColumns.LIBRARY_ACCOUNT + "=? "
                        + "AND books." + BooksColumns.BOOK_SYNC_STATE + "!=? "
                        + "AND books." + BooksColumns.BOOK_IS_EMBEDDED + "==?"
                        , account, Integer.toString(Books.STATE_NORMAL), "0");
            }
            case BOOKS_ID: {
                final String bookId = Books.getBookId(uri);
                return builder.table(Books.TABLE_NAME).where(BaseColumns._ID + "=?", bookId);
            }
            case BOOKS_DOWNLOAD_ID: {
                final String bookId = Books.getBookId(uri);
                return builder.table(Books.TABLE_NAME).where(BaseColumns._ID + "=?", bookId);
            }
            case AUTHOR_ID: {
                final String authorId = Authors.getAuthorId(uri);
                return builder.table(Authors.TABLE_NAME).where(BaseColumns._ID + "=?", authorId);
            }
            case NAVPOINT_ID: {
                final String navpointId = NavPoints.getNavpointId(uri);
                return builder.table(NavPoints.TABLE_NAME).where(BaseColumns._ID + "=?", navpointId);
            }
            case SECTION_ID: {
                final String sectionId = Sections.getSectionId(uri);
                return builder.table(Sections.TABLE_NAME).where(BaseColumns._ID + "=?", sectionId);
            }
            case BOOKMARK_ID: {
                final String bookmarkId = Bookmarks.getBookmarkId(uri);
                return builder.table(Bookmarks.TABLE_NAME).where(BaseColumns._ID + "=?", bookmarkId);
            }
            case BOOKS_STATUS: {
                final String bookStatus = Books.getBookStatus(uri);
                final String account = Books.getUserAccount(uri);
                return builder.table(Books.BOOKS_JOIN_LIBRARIES_BOOKMARKS).where("libraries." + LibrariesColumns.LIBRARY_ACCOUNT + "=? "
                        + "AND books." + BooksColumns.BOOK_STATE + "=? "
                        + "AND books." + BooksColumns.BOOK_SYNC_STATE + "!=? "
                        + "AND (" + BookmarksColumns.BOOKMARK_TYPE + "=? OR " + BookmarksColumns.BOOKMARK_TYPE + " is NULL)", account, bookStatus, Integer.toString(Books.STATE_DELETED), Integer.toString(BBBContract.Bookmarks.TYPE_LAST_POSITION));

            }

            case BOOKS_RECENT_PURCHASE: {
                final String purchaseDate = Books.getBookPurchaseDate(uri);
                final String account = Books.getUserAccount(uri);

                return builder.table(Books.BOOKS_JOIN_LIBRARIES).where("libraries." + LibrariesColumns.LIBRARY_ACCOUNT + "=? "
                        + "AND books. " + BooksColumns.BOOK_STATE + "=? "
                        + "AND " + BooksColumns.BOOK_PURCHASE_DATE + ">=? ", account, Integer.toString(Books.BOOK_STATE_UNREAD), purchaseDate);

            }
            case BOOKMARKS_TYPE: {
                final String bookId = Bookmarks.getBookId(uri);
                final String bookmarkType = Bookmarks.getBookmarkType(uri);
                // returns bookmarks of a specific type, including highlights with annotations when searching for notes
                if (Integer.toString(Bookmarks.TYPE_LAST_POSITION).equals(bookmarkType)) {
                    return builder.table(Bookmarks.TABLE_NAME).where(BookmarksColumns.BOOKMARK_BOOK_ID + "=? "
                            + "AND " + BookmarksColumns.BOOKMARK_STATE + "!=? "
                            + "AND " + BookmarksColumns.BOOKMARK_TYPE + "=? "
                            , bookId, Integer.toString(Books.STATE_DELETED), bookmarkType);
                } else {
                    return builder.table(Bookmarks.TABLE_NAME).where(BookmarksColumns.BOOKMARK_BOOK_ID + "=? "
                            + "AND " + BookmarksColumns.BOOKMARK_STATE + "!=? "
                            + "AND (" + BookmarksColumns.BOOKMARK_TYPE + "=? "
                            + "OR " + Integer.toString(Bookmarks.TYPE_NOTE) + "=" + bookmarkType
                            + " AND " + BookmarksColumns.BOOKMARK_TYPE + "=? "
                            + " AND " + BookmarksColumns.BOOKMARK_ANNOTATION + " is NOT NULL)"
                            , bookId, Integer.toString(Books.STATE_DELETED), bookmarkType, Integer.toString(Bookmarks.TYPE_HIGHLIGHT));

                }

            }
            case BOOKMARKS_SYNCHRONIZATION: {
                final String account = Books.getUserAccount(uri);
                return builder.table(Bookmarks.BOOKMARKS_JOIN_BOOKS_LIBRARIES).where(LibrariesColumns.LIBRARY_ACCOUNT + "=? "
                        + "AND " + BookmarksColumns.BOOKMARK_STATE + "!=? "
                        , account, Integer.toString(Books.STATE_NORMAL));
            }
            case BOOKMARKS_SERVER_ID:
            case LIBRARY:
            case LIBRARY_ACCOUNT:
            case LIBRARY_ID:
            case BOOKS_SERVER_ID:
            case BOOKS_READING_STATUS_ID:
            case READER_SETTINGS: {
                return buildSimpleSelection(uri);
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.ContentProvider#delete(android.net.Uri,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        final SelectionBuilder builder = buildSimpleSelection(uri);
        ContentValues contentValues = new ContentValues();

        int retVal;

        switch (match) {
            case BOOKS_ID:
                contentValues.put(BooksColumns.BOOK_SYNC_STATE, SyncState.STATE_DELETED);
                retVal = builder.where(selection, selectionArgs).update(db, contentValues);
                break;
            case BOOKMARK_ID: //falls through
            case BOOKMARKS_TYPE:
                contentValues.put(BookmarksColumns.BOOKMARK_STATE, SyncState.STATE_DELETED);
                retVal = builder.where(selection, selectionArgs).update(db, contentValues);

                uri = Bookmarks.CONTENT_URI;
                break;
            default:
                retVal = builder.where(selection, selectionArgs).delete(db);
        }

        getContext().getContentResolver().notifyChange(uri, null, false);
        return retVal;
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     *
     * @param uri {@link Uri}
     * @return {@link SelectionBuilder}
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LIBRARY: {
                return builder.table(Libraries.TABLE_NAME);
            }
            case LIBRARY_ACCOUNT: {
                final String account = Libraries.getAccount(uri);
                return builder.table(Libraries.TABLE_NAME).where(LibrariesColumns.LIBRARY_ACCOUNT + "=? ", account);
            }
            case LIBRARY_ID: {
                final String libraryId = Libraries.getLibraryId(uri);
                return builder.table(Libraries.TABLE_NAME).where(BaseColumns._ID + "=?", libraryId);
            }
            case BOOKS_ACCOUNT: {
                return builder.table(Books.TABLE_NAME);
            }
            case BOOKS_ID:
            case BOOKS_READING_STATUS_ID:
            case BOOKS_DOWNLOAD_ID: {
                final String bookId = Books.getBookId(uri);
                return builder.table(Books.TABLE_NAME).where(BaseColumns._ID + "=?", bookId);
            }
            case BOOKS_SERVER_ID: {
                final String serverId = Books.getBookServerId(uri);
                return builder.table(Books.TABLE_NAME).where(BooksColumns.BOOK_SERVER_ID + "=?", serverId);
            }
            case BOOKMARK_ID: {
                final String bookmarkId = Bookmarks.getBookmarkId(uri);
                return builder.table(Bookmarks.TABLE_NAME).where(BaseColumns._ID + "=?", bookmarkId);
            }
            case BOOKMARKS_SERVER_ID: {
                final String serverId = Bookmarks.getCloudId(uri);
                return builder.table(Bookmarks.TABLE_NAME).where(BookmarksColumns.BOOKMARK_CLOUD_ID + "=?", serverId);
            }
            case READER_SETTINGS: {
                final String account = ReaderSettings.getAccount(uri);
                return builder.table(ReaderSettings.TABLE_NAME).where(ReaderSettingsColumns.READER_ACCOUNT + "=?", account);
            }
            case BOOKMARKS_TYPE: {
                return buildExpandedSelection(uri, match);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#applyBatch(java.util.ArrayList)
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
                db.yieldIfContendedSafely();
            }

            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.ContentProvider#insert(android.net.Uri,
     * android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LIBRARY_ACCOUNT: {
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                int updated = builder.update(db, values);
                if (updated == 0) {
                    db.insertOrThrow(Libraries.TABLE_NAME, null, values);
                }
                getContext().getContentResolver().notifyChange(uri, null, false);
                return uri;
            }
            case BOOKS_ACCOUNT: {
                long bookId = db.insertOrThrow(Books.TABLE_NAME, null, values);
                getContext().getContentResolver().notifyChange(uri, null, false);
                return Books.buildBookIdUri(bookId);
            }
            case BOOKS_SERVER_ID: {
                final SelectionBuilder builder = buildSimpleSelection(uri);
                int updated = builder.update(db, values);
                if (updated == 0) {
                    db.insertOrThrow(Books.TABLE_NAME, null, values);
                }
                getContext().getContentResolver().notifyChange(uri, null, false);
                return uri;
            }
            case BOOKMARKS_SERVER_ID: {
                final SelectionBuilder builder;
                // Always overwrite last position with the same ISBN
                if (values != null && values.getAsInteger(Bookmarks.BOOKMARK_TYPE) == Bookmarks.TYPE_LAST_POSITION) {
                    builder = new SelectionBuilder().table(Bookmarks.TABLE_NAME).where(
                            BookmarksColumns.BOOKMARK_ISBN + "=? "
                                    + "AND " + BookmarksColumns.BOOKMARK_TYPE + "=? ",
                            values.getAsString(Bookmarks.BOOKMARK_ISBN), String.valueOf(Bookmarks.TYPE_LAST_POSITION)
                    );
                } else {
                    builder = buildSimpleSelection(uri);
                }
                int updated = builder.update(db, values);
                if (updated == 0) {
                    db.insertOrThrow(Bookmarks.TABLE_NAME, null, values);
                }
                getContext().getContentResolver().notifyChange(uri, null, false);
                return uri;
            }
            case READER_SETTINGS: {
                final SelectionBuilder builder = buildSimpleSelection(uri);
                int updated = builder.update(db, values);
                if (updated == 0) {
                    db.insertOrThrow(ReaderSettings.TABLE_NAME, null, values);
                }
                getContext().getContentResolver().notifyChange(uri, null, false);
                return uri;
            }
            case BOOKMARKS_TYPE: {
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                final String bookmarkType = Bookmarks.getBookmarkType(uri);
                if (Long.valueOf(bookmarkType) == Bookmarks.TYPE_LAST_POSITION) {
                    values.put(BookmarksColumns.BOOKMARK_STATE, SyncState.STATE_UPDATED);
                    int updated = builder.update(db, values);
                    if (updated == 0) {
                        values.put(BookmarksColumns.BOOKMARK_STATE, SyncState.STATE_ADDED);
                        db.insertOrThrow(Bookmarks.TABLE_NAME, null, values);
                    }
                } else {
                    values.put(BookmarksColumns.BOOKMARK_STATE, SyncState.STATE_ADDED);
                    long bookmarkId = db.insertOrThrow(Bookmarks.TABLE_NAME, null, values);
                    uri = Bookmarks.buildBookmarkIdUri(bookmarkId);
                }
                getContext().getContentResolver().notifyChange(uri, null, false);
                return uri;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS_READING_STATUS_ID: {
                values.put(BooksColumns.BOOK_SYNC_STATE, SyncState.STATE_UPDATED);
                break;
            }
        }
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        getContext().getContentResolver().notifyChange(uri, null, false);
        return retVal;
    }

    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

    /**
     * A test package can call this to get a handle to the database underlying
     * NotePadProvider, so it can insert test data into the database. The test
     * case class is responsible for instantiating the provider in a test
     * context; {@link android.test.ProviderTestCase2} does this during the call
     * to setUp()
     *
     * @return a handle to the database helper object for the provider's data.
     */
    public BBBDatabase getOpenHelperForTest() {
        return mOpenHelper;
    }
}