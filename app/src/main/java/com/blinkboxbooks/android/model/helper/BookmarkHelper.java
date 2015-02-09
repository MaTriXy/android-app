// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model.helper;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.util.LongSparseArray;
import android.text.Html;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookmark;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.provider.BBBAsyncQueryHandler;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.provider.BBBProviderHelper;
import com.blinkboxbooks.android.sync.Synchroniser;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.BBBTextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for converting a Bookmark object to and from a Cursor
 */
public class BookmarkHelper {

    private static final int MAX_CONTENT_LENGTH_FOR_DIALOG = 175;

    public static String NO_CFI = "epubcfi()";

    public static JSONObject getUpdateByJSON(String deviceName) {
        JSONObject object = new JSONObject();

        String clientId = AccountController.getInstance().getClientId();

        try {
            object.put(BBBApiConstants.PARAM_CLIENT_ID, clientId);
            object.put(BBBApiConstants.PARAM_CLIENT_NAME, deviceName);
        } catch (JSONException e) {
        }

        return object;
    }

    /**
     * Convert given {@link com.blinkboxbooks.android.model.Bookmark} array to
     * comma-separated  CFIs in single quotes.
     * <p/>
     * </br>
     * Sample:  'epubcfi(/6/28!/4/94/1:0)', 'epubcfi(/6/28!/4/140/2/2/1:0)', 'epubcfi(/6/28!/4/196/3:36)’
     *
     * @param bookmarks {@link com.blinkboxbooks.android.model.Bookmark} Array
     * @return {@link java.lang.String}
     */
    public static String convertBookmarksToString(Bookmark[] bookmarks) {
        String bookmarkArrayString = "";
        int count = bookmarks.length;
        for (int i = 0; i < count; i++) {
            String cfi = bookmarks[i].position;
            bookmarkArrayString += "'" + cfi + "'";
            if (i < count - 1) {
                bookmarkArrayString += ", ";
            }
        }
        return bookmarkArrayString;
    }


    /**
     * Get all bookmarks for for a given book id
     *
     * @param bookId book id
     * @return {@link com.blinkboxbooks.android.model.Bookmark} Array
     */
    public static Bookmark[] getAllBookmarks(long bookId) {

        Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, bookId);

        Cursor cursor = BBBApplication.getApplication().getContentResolver().query(uri, null, null, null, null);
        int count = cursor.getCount();
        Bookmark[] result = new Bookmark[count];
        for (int i = 0; i < count && cursor.moveToNext(); i++) {
            result[i] = BookmarkHelper.createBookmark(cursor);
        }
        cursor.close();
        return result;
    }

    /**
     * Get all highlights for for a given book id
     *
     * @param bookId book id
     * @return {@link com.blinkboxbooks.android.model.Bookmark} Array
     */
    public static Bookmark[] getAllHighlights(long bookId) {

        Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_HIGHLIGHT, bookId);

        Cursor cursor = BBBApplication.getApplication().getContentResolver().query(uri, null, null, null, null);
        int count = cursor.getCount();
        Bookmark[] result = new Bookmark[count];
        for (int i = 0; i < count && cursor.moveToNext(); i++) {
            result[i] = BookmarkHelper.createBookmark(cursor);
        }
        cursor.close();
        return result;
    }


    /**
     * Shows a confirmation dialog to delete all the bookmarks belonging to a particular book
     *
     * @param activity
     * @param book_id
     */
    public static void deleteAllBookmarks(Activity activity, final long book_id) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(activity);

        builder.setTitle(activity.getString(R.string.delete));
        builder.setMessage(activity.getString(R.string.dialog_are_you_sure_you_want_to_remove_all_bookmarks));
        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteAllBookmarks(book_id);
            }
        }).show();
    }

    /**
     * Deletes all the bookmarks belonging to a particular book
     *
     * @param book_id
     */
    public static void deleteAllBookmarks(long book_id) {
        Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, book_id);
        BBBAsyncQueryHandler.getInstance().startDelete(0, null, uri, null, null);
        AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_BOOKMARKS);
    }

    /**
     * Deletes bookmarks by cfi
     *
     * @param book_id
     */
    public static void deleteBookmarkByCFI(long book_id, String cfi) {
        Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, book_id);
        BBBAsyncQueryHandler.getInstance().startDelete(0, null, uri, Bookmarks.BOOKMARK_POSITION + "=?", new String[]{
                cfi
        });
        AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_BOOKMARKS);
    }

    /**
     * Deletes highlight by cfi
     *
     * @param book_id
     * @param cfi
     */
    public static void deleteHighlightByCFI(long book_id, String cfi) {
        Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_HIGHLIGHT, book_id);
        BBBAsyncQueryHandler.getInstance().startDelete(0, null, uri, Bookmarks.BOOKMARK_POSITION + "=?", new String[]{
                cfi
        });
        AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_BOOKMARKS);
    }

    /**
     * Shows a confirmation dialog to delete a bookmark
     *
     * @param activity
     * @param bookmark
     */
    public static void deleteBookmark(Activity activity, final Bookmark bookmark) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(activity);

        String content = bookmark.content == null ? "" : Html.fromHtml(bookmark.content).toString();

        if (content.length() > MAX_CONTENT_LENGTH_FOR_DIALOG) {
            content = content.substring(0, MAX_CONTENT_LENGTH_FOR_DIALOG - 1).trim() + "...";
        }

        builder.setTitle(activity.getString(R.string.title_delete_this_bookmark));
        builder.setMessage(String.format(activity.getString(R.string.dialog_are_you_sure_you_want_to_delete_this_bookmark), content));
        builder.setNegativeButton(R.string.button_keep, null);
        builder.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteBookmark(bookmark.id);
            }
        }).show();
    }

    /**
     * Shows a confirmation dialog to delete a highlight
     *
     * @param activity
     * @param highlight
     */
    public static void deleteHighlight(Activity activity, final Bookmark highlight) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(activity);

        String content = highlight.content == null ? "" : Html.fromHtml(highlight.content).toString();

        if (content.length() > MAX_CONTENT_LENGTH_FOR_DIALOG) {
            content = content.substring(0, MAX_CONTENT_LENGTH_FOR_DIALOG - 1).trim() + "...";
        }

        builder.setTitle(activity.getString(R.string.title_delete_this_highlight));
        builder.setMessage(String.format(activity.getString(R.string.dialog_are_you_sure_you_want_to_delete_this_highlight), content));
        builder.setNegativeButton(R.string.button_keep, null);
        builder.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteBookmark(highlight.id);
            }
        }).show();
    }

    /**
     * Delete a lastPosition
     *
     * @param bookmark_id The id of the lastPosition to delete
     */
    public static void deleteBookmark(long bookmark_id) {
        Uri uri = Bookmarks.buildBookmarkIdUri(bookmark_id);
        BBBAsyncQueryHandler.getInstance().startDelete(0, null, uri, null, null);
        AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_BOOKMARKS);
    }

    /**
     * Delete a highlight attached to a note
     *
     * @param bookmark_id The id of the lastPosition
     */
    public static void deleteHighlightFromNote(long bookmark_id) {
        Uri uri = Bookmarks.buildBookmarkIdUri(bookmark_id);
        ContentValues contentValues = new ContentValues();
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_TYPE, Bookmarks.TYPE_NOTE);
        contentValues.putNull(BBBContract.BookmarksColumns.BOOKMARK_COLOR);
        BBBAsyncQueryHandler.getInstance().startUpdate(0, null, uri, contentValues, null, null);
    }

    /**
     * Delete a note attached to a highlight
     *
     * @param bookmark_id The id of the lastPosition
     */
    public static void deleteNoteFromHighlight(long bookmark_id) {
        Uri uri = Bookmarks.buildBookmarkIdUri(bookmark_id);
        ContentValues contentValues = new ContentValues();
        contentValues.putNull(BBBContract.BookmarksColumns.BOOKMARK_ANNOTATION);
        BBBAsyncQueryHandler.getInstance().startUpdate(0, null, uri, contentValues, null, null);
    }

    /**
     * Update the last position lastPosition
     *
     * @param bookmark
     */
    public static void updateBookmark(Bookmark bookmark, boolean synchronous) {
        Uri uri = Bookmarks.buildBookmarkTypeUri(bookmark.type, bookmark.book_id);

        if (synchronous) {
            ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
            contentResolver.insert(uri, getContentValues(bookmark));
        } else {
            BBBAsyncQueryHandler.getInstance().startInsert(0, null, uri, getContentValues(bookmark));
        }
    }

    /**
     * Returns true if the given userId has any bookmarks
     *
     * @param userId
     * @return
     */
    public static boolean hasBookmarks(String userId) {
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        Uri uri = Bookmarks.buildBookmarkSynchronizationUri(userId);
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        boolean hasBookmarks = cursor.getCount() > 0;
        cursor.close();
        return hasBookmarks;
    }

    /**
     * Copy over bookmarks from an existing user
     *
     * @param from
     * @param bookIdMap
     * @param contentResolver
     */
    public static void copyBookmarks(String from, LongSparseArray<Long> bookIdMap, ContentResolver contentResolver) {
        Uri fromUri = Bookmarks.buildBookmarkSynchronizationUri(from);
        Cursor cursor = contentResolver.query(fromUri, null, null, null, null);
        while (cursor.moveToNext()) {
            Bookmark bookmark = createBookmark(cursor);
            bookmark.id = 0;
            bookmark.book_id = bookIdMap.get(bookmark.book_id);
            updateBookmark(bookmark, false);
        }
        cursor.close();
    }

    /**
     * Get a lastPosition of a specific type
     *
     * @param bookmarkType
     * @param bookid
     * @return
     */
    public static Bookmark getBookmark(int bookmarkType, long bookid) {
        Bookmark bookmark = null;
        Uri uri = Bookmarks.buildBookmarkTypeUri(bookmarkType, bookid);
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);

        if (cursor.moveToFirst()) {
            bookmark = createBookmark(cursor);
        }

        cursor.close();
        return bookmark;
    }

    public static String getBookmarkType(int bookmarkType) {

        switch (bookmarkType) {
            case Bookmarks.TYPE_BOOKMARK:
                return BBBApiConstants.BOOKMARK_TYPE_BOOKMARK;
            case Bookmarks.TYPE_NOTE:
                return BBBApiConstants.BOOKMARK_TYPE_NOTE;
            case Bookmarks.TYPE_HIGHLIGHT:
                return BBBApiConstants.BOOKMARK_TYPE_HIGHLIGHT;
            case Bookmarks.TYPE_LAST_POSITION:
                return BBBApiConstants.BOOKMARK_TYPE_LAST_READ_POSITION;
            default:
                if (BuildConfig.DEBUG) {
                    throw new RuntimeException("invalid lastPosition type " + bookmarkType);
                } else {
                    return BBBApiConstants.BOOKMARK_TYPE_BOOKMARK;
                }
        }
    }

    public static int getBookmarkType(String bookmarkType) {

        if (BBBApiConstants.BOOKMARK_TYPE_BOOKMARK.equals(bookmarkType)) {
            return Bookmarks.TYPE_BOOKMARK;
        } else if (BBBApiConstants.BOOKMARK_TYPE_HIGHLIGHT.equals(bookmarkType)) {
            return Bookmarks.TYPE_HIGHLIGHT;
        } else if (BBBApiConstants.BOOKMARK_TYPE_LAST_READ_POSITION.equals(bookmarkType)) {
            return Bookmarks.TYPE_LAST_POSITION;
        } else {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException("invalid lastPosition type " + bookmarkType);
            } else {
                return Bookmarks.TYPE_BOOKMARK;
            }
        }
    }

    /**
     * Create a new lastPosition for a given book id
     *
     * @param book_id
     * @return {@link Bookmark}
     */
    public static Bookmark createBookmark(long book_id) {
        Bookmark bookmark = new Bookmark();

        String clientId = AccountController.getInstance().getClientId();
        if (clientId != null) {
            bookmark.update_by = BBBTextUtils.getIdFromGuid(clientId);
        }
        bookmark.update_date = System.currentTimeMillis();
        bookmark.cloud_id = null;
        bookmark.book_id = book_id;
        bookmark.position = "";

        return bookmark;
    }

    /**
     * Creates a Bookmark object from a BBBBookmark object
     *
     * @param bbbBookmark the BBBBookmark object
     * @return the Bookmark object
     */
    public static Bookmark createBookmark(BBBBookmark bbbBookmark) {
        Bookmark bookmark = new Bookmark();

        bookmark.annotation = bbbBookmark.annotation;
        bookmark.cloud_id = bbbBookmark.id;
        bookmark.content = bbbBookmark.preview;
        bookmark.isbn = bbbBookmark.book;
        bookmark.name = bbbBookmark.name;
        bookmark.style = bbbBookmark.style;
        bookmark.update_by = bbbBookmark.updatedByClient == null ? bbbBookmark.createdByClient : bbbBookmark.updatedByClient;

        String updatedDate = bbbBookmark.updatedDate == null ? bbbBookmark.createdDate : bbbBookmark.updatedDate;

        if (updatedDate != null) {
            bookmark.update_date = BBBCalendarUtil.parseDate(updatedDate, BBBCalendarUtil.FORMAT_TIME_STAMP).getTimeInMillis();
        }

        bookmark.position = bbbBookmark.position;
        bookmark.percentage = bbbBookmark.readingPercentage;
        bookmark.type = getBookmarkType(bbbBookmark.bookmarkType);
        bookmark.color = bbbBookmark.colour;

        if (NO_CFI.equals(bookmark.position)) {
            bookmark.position = "";
        }

        return bookmark;
    }

    /**
     * Create Bookmark object from Cursor.
     *
     * @param cursor the Cursor to read from
     * @return the Bookmark object or null if the cursor does not contain lastPosition data
     */
    public static Bookmark createBookmark(Cursor cursor) {
        Bookmark bookmark = new Bookmark();

        bookmark.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

        bookmark.cloud_id = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_CLOUD_ID);
        bookmark.book_id = BBBProviderHelper.getInt(cursor, BBBContract.BookmarksColumns.BOOKMARK_BOOK_ID);
        bookmark.name = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_NAME);
        bookmark.content = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_CONTENT);
        bookmark.type = BBBProviderHelper.getInt(cursor, BBBContract.BookmarksColumns.BOOKMARK_TYPE);
        bookmark.annotation = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_ANNOTATION);
        bookmark.style = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_STYLE);
        bookmark.update_by = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_UPDATE_BY);
        bookmark.update_date = BBBProviderHelper.getLong(cursor, BBBContract.BookmarksColumns.BOOKMARK_UPDATE_DATE);
        bookmark.percentage = BBBProviderHelper.getInt(cursor, BBBContract.BookmarksColumns.BOOKMARK_PERCENTAGE);
        bookmark.state = BBBProviderHelper.getInt(cursor, BBBContract.BookmarksColumns.BOOKMARK_STATE);
        bookmark.color = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_COLOR);
        bookmark.position = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_POSITION);
        bookmark.isbn = BBBProviderHelper.getString(cursor, BBBContract.BookmarksColumns.BOOKMARK_ISBN);

        return bookmark;
    }

    /**
     * Returns a ContentValues instance (a map) for this Bookmark instance. This is useful for inserting a Bookmark into a database.
     *
     * @param bookmark The Bookmark object for which we want ContentValues
     * @return the ContentValues object
     */
    public static ContentValues getContentValues(Bookmark bookmark) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_CLOUD_ID, bookmark.cloud_id);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_BOOK_ID, bookmark.book_id);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_NAME, bookmark.name);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_CONTENT, bookmark.content);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_TYPE, bookmark.type);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_ANNOTATION, bookmark.annotation);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_STYLE, bookmark.style);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_UPDATE_BY, bookmark.update_by);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_UPDATE_DATE, bookmark.update_date);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_PERCENTAGE, bookmark.percentage);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_STATE, bookmark.state);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_COLOR, bookmark.color);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_ISBN, bookmark.isbn);
        contentValues.put(BBBContract.BookmarksColumns.BOOKMARK_POSITION, bookmark.position);

        return contentValues;
    }

    /**
     * Returns the book id for a cloud bookmark uri.
     * @param uri
     * @return the id of the book that the bookmark is associated with, or -1 if not found.
     */
    public static long getBookIdFromBookmarkCloudUri(Uri uri) {
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        final Cursor cursor = contentResolver.query(uri, new String[] { Bookmarks.BOOKMARK_BOOK_ID }, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }
        return -1L;
    }
}
