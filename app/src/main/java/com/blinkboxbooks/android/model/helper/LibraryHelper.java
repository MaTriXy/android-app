// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.model.helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.util.LongSparseArray;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.BusinessRules;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Library;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.provider.BBBContract.Libraries;
import com.blinkboxbooks.android.provider.BBBContract.LibrariesColumns;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Helper class for converting a Library object to and from a Cursor
 */
public class LibraryHelper {

    public static final String TEMPLATE_ACCOUNT = "TEMPLATE";

    public static Library getLibraryForUserId(String userId) {
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        return getLibraryForUserId(userId, contentResolver);
    }

    public static Library getLibraryForUserId(String userId, ContentResolver contentResolver) {
        Uri uri = Libraries.buildLibrariesAccountUri(userId);
        Library library = null;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor.moveToFirst()) {
            library = createLibrary(cursor);
        }
        cursor.close();
        return library;
    }

    public static Library copyLibrary(String from, String to, ContentResolver contentResolver) {
        Uri uri = Libraries.buildLibrariesAccountUri(to);
        Library library = new Library();
        library.account = to;
        contentResolver.insert(uri, LibraryHelper.getContentValues(library));

        // Get the library id
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        library = createLibrary(cursor);
        cursor.close();

        LongSparseArray<Long> bookIdMap = new LongSparseArray<Long>();
        Uri accountUri = Books.buildBookAccountUri(to);
        Uri fromUri = Books.buildBookAccountUri(from);
        cursor = contentResolver.query(fromUri, null, null, null, null);
        while (cursor.moveToNext()) {
            Book book = BookHelper.createBook(cursor);
            long bookid = book.id;
            book.library_id = library.id;
            Uri bookUri = contentResolver.insert(accountUri, BookHelper.getContentValues(book));
            long newbookid = Long.valueOf(Books.getBookId(bookUri));
            bookIdMap.put(bookid, newbookid);
        }
        cursor.close();

        BookmarkHelper.copyBookmarks(from, bookIdMap, contentResolver);

        return library;
    }

    public static void confirmResetAnonymousSettings(final Context context) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(context);
        builder.setTitle(context.getString(R.string.dialog_resetting_your_settings_to_default));
        builder.setMessage(context.getString(R.string.dialog_are_you_sure_youd_like_to_reset_your_settings_to_default));
        builder.setNegativeButton(R.string.no_thanks, null);
        builder.setPositiveButton(R.string.yes_please, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String account = BusinessRules.DEFAULT_ACCOUNT_NAME;

                // Reset library
                ContentResolver contentResolver = context.getContentResolver();
                contentResolver.delete(Libraries.buildLibrariesAccountUri(account), null, null);
                LibraryHelper.createAnonymousLibrary();

                // Reset settings
                EPub2ReaderSettingController.getInstance().resetToDefault(account);
                EPub2ReaderSettingController.getInstance().saveReaderSetting();
            }
        }).show();
    }

    /**
     * Get or prepare the Anonymous (not logged in) users library from the template
     *
     * @return library The Anonymous users library object
     */
    public static Library createAnonymousLibrary() {
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        return createAnonymousLibrary(contentResolver);
    }

    /**
     * Get or prepare the Anonymous (not logged in) users library from the template
     *
     * @param contentResolver The content resolver to use
     * @return library The Anonymous users library object
     */
    public static Library createAnonymousLibrary(ContentResolver contentResolver) {
        Uri uri = Libraries.buildLibrariesAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME);
        Cursor cursor = contentResolver.query(uri, null, null, null, null);

        Library library;
        if (cursor.moveToFirst()) {
            library = createLibrary(cursor);
            cursor.close();
        } else {
            cursor.close();

            // Transfer the embedded books from the template library
            library = copyLibrary(TEMPLATE_ACCOUNT, BusinessRules.DEFAULT_ACCOUNT_NAME, contentResolver);
        }
        return library;
    }

    /**
     * Create Library object from Cursor.
     *
     * @param cursor the Cursor to read from
     * @return the Library object or null if the cursor does not Library book data
     */
    public static Library createLibrary(Cursor cursor) {
        Library library = new Library();

        library.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        library.date_created = cursor.getString(cursor.getColumnIndex(LibrariesColumns.LIBRARY_CREATE_DATE));
        library.date_library_last_sync = cursor.getString(cursor.getColumnIndex(LibrariesColumns.LIBRARY_SYNC_DATE));
        library.date_bookmark_last_sync = cursor.getString(cursor.getColumnIndex(LibrariesColumns.LIBRARY_BOOKMARK_SYNC_DATE));
        library.account = cursor.getString(cursor.getColumnIndex(LibrariesColumns.LIBRARY_ACCOUNT));

        return library;
    }

    /**
     * Updates library data
     *
     * @param library the Library object containing the updated data
     */
    public static void updateLibrary(Library library) {
        Uri uri = Libraries.buildLibrariesIdUri(library.id);

        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        contentResolver.update(uri, getContentValues(library), null, null);
    }

    /**
     * Returns a ContentValues instance (a map) for this Library instance. This is useful for inserting a Book into a database.
     *
     * @param library The Library object for which we want ContentValues
     * @return the ContentValues object
     */
    public static ContentValues getContentValues(Library library) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(LibrariesColumns.LIBRARY_CREATE_DATE, library.date_created);
        contentValues.put(LibrariesColumns.LIBRARY_SYNC_DATE, library.date_library_last_sync);
        contentValues.put(LibrariesColumns.LIBRARY_BOOKMARK_SYNC_DATE, library.date_bookmark_last_sync);
        contentValues.put(LibrariesColumns.LIBRARY_ACCOUNT, library.account);

        return contentValues;
    }
}
