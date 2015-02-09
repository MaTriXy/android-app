// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model.helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.api.model.BBBImage;
import com.blinkboxbooks.android.api.model.BBBLink;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.provider.BBBAsyncQueryHandler;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.sync.Synchroniser;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.NotificationUtil;
import com.blinkboxbooks.android.util.StringUtils;

import java.io.File;
import java.util.Calendar;

/**
 * Helper class for converting a Book object to and from a Cursor
 */
public class BookHelper {

    /**
     * Check whether the user has made any changes to their library
     *
     * @param userId
     */
    public static boolean hasChangedBookStatus(String userId) {
        Uri uri = BBBContract.Books.buildBookAccountUri(userId);
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, Books.BOOK_DOWNLOAD_STATUS + "=? OR " + Books.BOOK_STATE + "!=?", new String[]{
                String.valueOf(Books.NOT_DOWNLOADED), String.valueOf(Books.BOOK_STATE_UNREAD)
        }, null);
        boolean hasChanges = (cursor.getCount() > 0);
        cursor.close();
        return hasChanges;
    }

    /**
     * Delete the physical book for a given book object, only if it is not used by any other accounts on the device
     *
     * @param book
     */
    public static void deletePhysicalBook(Book book) {

        // Hide the notification
        NotificationUtil.hideNotification(BBBApplication.getApplication(), book.isbn.hashCode());

        if (book.embedded_book || book.media_path == null) {
            return;
        }
        String existingPath = BookHelper.getExistingFilePath(book);
        if (!TextUtils.isEmpty(book.file_path) && existingPath == null) {
            File file = new File(book.file_path);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Gets the file path of a book if it exists on another users account, or null if the book
     * has not been downloaded on this device
     *
     * @param book The book
     * @return existPath or null if the book does not exist
     */
    public static String getExistingFilePath(Book book) {

        if (book.media_path == null) {
            return null;
        }

        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();

        Cursor cursor = contentResolver.query(Books.CONTENT_URI, null, Books.BOOK_MEDIA_PATH + "=? AND " + Books.BOOK_DOWNLOAD_STATUS + "=?", new String[]{
                book.media_path, String.valueOf(Books.DOWNLOADED)
        }, null);
        String existingPath = null;
        if (cursor.moveToFirst()) {
            existingPath = BookHelper.createBook(cursor).file_path;
        }
        cursor.close();
        return existingPath;
    }

    /**
     * Updates a book in the db
     *
     * @param bookUri
     * @param book
     */
    public static void updateBook(Uri bookUri, Book book, boolean synchronous) {

        if (book == null) {
            return;
        }

        if (bookUri == null) {
            bookUri = BBBContract.Books.buildBookIdUri(book.id);
        }

        ContentValues values = getContentValues(book);

        if (synchronous) {
            ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
            contentResolver.update(bookUri, values, null, null);
        } else {
            BBBAsyncQueryHandler.getInstance().startUpdate(0, null, bookUri, values, null, null);
        }
    }

    /**
     * Update the books reading status
     *
     * @param id
     * @param state
     */
    public static void updateBookReadingStatus(long id, int state) {
        Uri bookUri = Books.buildBookReadingStatusIdUri(id);

        if (state == Books.BOOK_STATE_FINISHED) {
            Book book = getBookFromUri(bookUri);

            if (book != null) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READING,
                        book.sample_book ? AnalyticsHelper.GA_EVENT_FINISHED_READING_SAMPLE_BOOK : AnalyticsHelper.GA_EVENT_FINISHED_READING_FULL_BOOK, book.isbn, null);
            }
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(BBBContract.BooksColumns.BOOK_UPDATE_DATE, System.currentTimeMillis());
        contentValues.put(BBBContract.BooksColumns.BOOK_STATE, state);
        BBBAsyncQueryHandler.getInstance().startUpdate(0, null, bookUri, contentValues, null, null);

        AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_LIBRARY);
    }

    /**
     * Update a books description
     *
     * @param bookUri
     * @param description
     */
    public static void updateBookDescription(Uri bookUri, String description) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(BBBContract.BooksColumns.BOOK_DESCRIPTION, description);

        BBBAsyncQueryHandler.getInstance().startUpdate(0, null, bookUri, contentValues, null, null);
    }

    /**
     * Get the book at a given Uri
     *
     * @param bookUri
     * @return book
     */
    public static Book getBookFromUri(Uri bookUri) {
        Book book = null;
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        if (bookUri != null && contentResolver != null) {
            Cursor cursor = contentResolver.query(bookUri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    book = BookHelper.createBook(cursor);
                }
                cursor.close();
            }
        }
        return book;
    }

    /**
     * Get the book for a given ISBN and embedded status
     *
     * @param accountId
     * @param isbn
     * @param is_embedded
     * @return
     */
    public static Book getBookFromISBN(String accountId, String isbn, Boolean is_embedded) {
        ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();
        Uri uri = Books.buildBookAccountUri(accountId);

        Cursor cursor;

        if (is_embedded != null) {
            cursor = contentResolver.query(uri, null, Books.BOOK_ISBN + "=? AND " + Books.BOOK_IS_EMBEDDED + "=?", new String[]{isbn, is_embedded ? "1" : "0"}, null);
        } else {
            cursor = contentResolver.query(uri, null, Books.BOOK_ISBN + "=?", new String[]{isbn}, null);
        }

        Book book = null;
        if (cursor.moveToFirst()) {
            book = BookHelper.createBook(cursor);
        }
        cursor.close();
        return book;
    }

    /**
     * Create Book object from Cursor.
     *
     * @param cursor the Cursor to read from
     * @return the Book object or null if the cursor does not contain book data
     */
    public static Book createBook(Cursor cursor) {

        if (cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_LIBRARY_ID) == -1) {
            return null;
        }

        Book book = new Book();
        book.id = (cursor.getLong(0));// cursor.getColumnIndex(BBBContract.Books._ID)));
        book.library_id = cursor.getLong(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_LIBRARY_ID));
        book.sync_state = cursor.getInt(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_SYNC_STATE));
        book.server_id = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_SERVER_ID));
        book.author = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_AUTHOR));
        book.isbn = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_ISBN));
        book.title = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_TITLE));
        book.tags = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_TAGS));
        book.cover_url = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_COVER_URL));
        book.offer_price = cursor.getFloat(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_OFFER_PRICE));
        book.normal_price = cursor.getFloat(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_NORMAL_PRICE));
        book.description = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_DESCRIPTION));
        book.publisher = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_PUBLISHER));
        book.purchase_date = cursor.getLong(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_PURCHASE_DATE));
        book.update_date = cursor.getLong(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_UPDATE_DATE));
        book.sync_date = cursor.getLong(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_SYNC_DATE));
        book.publication_date = cursor.getLong(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_PUBLICATION_DATE));
        book.state = cursor.getInt(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_STATE));
        book.download_count = cursor.getInt(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_DOWNLOAD_COUNT));
        book.in_device_library = cursor.getInt(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_IN_DEVICE_LIBRARY)) > 0;
        book.embedded_book = cursor.getInt(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_IS_EMBEDDED)) > 0;
        book.sample_book = cursor.getInt(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_IS_SAMPLE)) > 0;
        book.format = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_FORMAT));
        book.file_path = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_FILE_PATH));
        book.key_path = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_KEY_PATH));
        book.media_path = cursor.getString(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_MEDIA_PATH));
        book.file_size = cursor.getLong(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_SIZE));
        book.download_offset = cursor.getLong(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_DOWNLOAD_OFFSET));
        book.download_status = cursor.getInt(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_DOWNLOAD_STATUS));
        book.enc_key = cursor.getBlob(cursor.getColumnIndex(BBBContract.BooksColumns.BOOK_ENCRYPTION_KEY));

        return book;
    }

    /**
     * Returns a ContentValues instance (a map) for this Book instance. This is useful for inserting a Book into a database.
     *
     * @param book The Book object for which we want ContentValues
     * @return the ContentValues object
     */
    public static ContentValues getContentValues(Book book) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(BBBContract.BooksColumns.BOOK_SYNC_STATE, book.sync_state);
        contentValues.put(BBBContract.BooksColumns.BOOK_SERVER_ID, book.server_id);
        contentValues.put(BBBContract.BooksColumns.BOOK_LIBRARY_ID, book.library_id);
        contentValues.put(BBBContract.BooksColumns.BOOK_AUTHOR, book.author);
        contentValues.put(BBBContract.BooksColumns.BOOK_ISBN, book.isbn);
        contentValues.put(BBBContract.BooksColumns.BOOK_TITLE, book.title);
        contentValues.put(BBBContract.BooksColumns.BOOK_TAGS, book.tags);
        contentValues.put(BBBContract.BooksColumns.BOOK_COVER_URL, book.cover_url);
        contentValues.put(BBBContract.BooksColumns.BOOK_OFFER_PRICE, book.offer_price);
        contentValues.put(BBBContract.BooksColumns.BOOK_NORMAL_PRICE, book.normal_price);
        contentValues.put(BBBContract.BooksColumns.BOOK_DESCRIPTION, book.description);
        contentValues.put(BBBContract.BooksColumns.BOOK_PUBLISHER, book.publisher);
        contentValues.put(BBBContract.BooksColumns.BOOK_PURCHASE_DATE, book.purchase_date);
        contentValues.put(BBBContract.BooksColumns.BOOK_UPDATE_DATE, book.update_date);
        contentValues.put(BBBContract.BooksColumns.BOOK_SYNC_DATE, book.sync_date);
        contentValues.put(BBBContract.BooksColumns.BOOK_PUBLICATION_DATE, book.publication_date);
        contentValues.put(BBBContract.BooksColumns.BOOK_STATE, book.state);
        contentValues.put(BBBContract.BooksColumns.BOOK_DOWNLOAD_COUNT, book.download_count);
        contentValues.put(BBBContract.BooksColumns.BOOK_IN_DEVICE_LIBRARY, book.in_device_library);
        contentValues.put(BBBContract.BooksColumns.BOOK_IS_EMBEDDED, book.embedded_book);
        contentValues.put(BBBContract.BooksColumns.BOOK_IS_SAMPLE, book.sample_book);
        contentValues.put(BBBContract.BooksColumns.BOOK_FORMAT, book.format);
        contentValues.put(BBBContract.BooksColumns.BOOK_FILE_PATH, book.file_path);
        contentValues.put(BBBContract.BooksColumns.BOOK_KEY_PATH, book.key_path);
        contentValues.put(BBBContract.BooksColumns.BOOK_MEDIA_PATH, book.media_path);
        contentValues.put(BBBContract.BooksColumns.BOOK_SIZE, book.file_size);
        contentValues.put(BBBContract.BooksColumns.BOOK_DOWNLOAD_OFFSET, book.download_offset);
        contentValues.put(BBBContract.BooksColumns.BOOK_DOWNLOAD_STATUS, book.download_status);
        contentValues.put(BBBContract.BooksColumns.BOOK_ENCRYPTION_KEY, book.enc_key);

        return contentValues;
    }

    public static Book createBook(BBBBookInfo bookInfo) {
        Book book = new Book();

        book.title = bookInfo.title;
        book.sample_eligible = bookInfo.sampleEligible;

        BBBLink authorLink = bookInfo.getLinkData(BBBApiConstants.URN_CONTRIBUTOR);

        if(authorLink != null) {
            book.author = authorLink.title;
        }

        book.download_status = BBBContract.Books.DOWNLOADED;
        book.isbn = bookInfo.id;

        if (bookInfo.links != null && bookInfo.links.length > 0) {
            for (BBBLink link : bookInfo.links) {
                if (BBBApiConstants.URN_SAMPLE_MEDIA.equals(link.rel)) {
                    book.sample_uri = StringUtils.injectIntoResourceUrl(link.href, "/params;v=0") + "/";
                } else if (BBBApiConstants.URN_CONTRIBUTOR.equals(link.rel)) {
                    book.authorId = StringUtils.getLastPathSegment(link.href);
                }
            }
        }

        BBBImage[] images = bookInfo.images;

        for (int i = 0; i < images.length; i++) {

            if (BBBApiConstants.URN_IMAGE_COVER.equals(images[i].rel)) {
                String coverUrl = images[i].src;
                if ("Unknown".equals(coverUrl)) {
                    coverUrl = null;
                }
                book.cover_url = coverUrl;
                break;
            }
        }

        Calendar publishedDate = BBBCalendarUtil.attemptParse(bookInfo.publicationDate, BBBCalendarUtil.FORMAT_YEAR_MONTH_DAY, BBBCalendarUtil.FORMAT_YEAR, BBBCalendarUtil.FORMAT_TIME_STAMP);

        if (publishedDate != null) {
            book.publication_date = publishedDate.getTimeInMillis();
        }

        return book;
    }
}
