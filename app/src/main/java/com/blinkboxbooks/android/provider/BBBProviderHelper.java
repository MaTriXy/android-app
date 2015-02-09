// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.net.Uri;

import com.blinkboxbooks.android.BusinessRules;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.api.model.BBBImage;
import com.blinkboxbooks.android.api.model.BBBLibraryItem;
import com.blinkboxbooks.android.api.model.BBBLink;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.LibraryItem;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.LogUtils;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Helper class to simplify ContentProvider operations
 */
public class BBBProviderHelper {

    private static final String TAG = BBBProviderHelper.class.getSimpleName();

    /**
     * Converts an array of BBBLibraryItems into a List of ContentProviderOperations we can apply to the ContentResolver
     *
     * @param userId    the user id
     * @param libraryId the library id
     * @param items     the list of items that need to be converted
     * @return a list of ContentProviderOperations.
     */
    public static ArrayList<ContentProviderOperation> convertBookInfoToContentValues(String userId, long libraryId, LibraryItem[] items, long syncTimestamp) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(items.length);
        ContentProviderOperation operation;

        for (int i = 0; i < items.length; i++) {
            operation = convertBookInfoToContentValues(userId, libraryId, items[i], syncTimestamp);

            if (operation != null) {
                operations.add(operation);
            }
        }

        return operations;
    }

    /**
     * Creates the correct ContentProviderOperation for the given BBBLibraryItem
     *
     * @param userId      the user id
     * @param libraryId   the library id
     * @param libraryItem the BBBLibraryItem you want to convert
     * @return the ContentProviderOperation
     */
    public static ContentProviderOperation convertBookInfoToContentValues(String userId, long libraryId, LibraryItem libraryItem, long syncTimestamp) {
        BBBLibraryItem item = libraryItem.libraryItem;
        BBBBookInfo book = libraryItem.bookInfo;

        Uri uri = BBBContract.Books.buildBookServerIdUri(String.valueOf(userId), item.id);

        Book existingBook = BookHelper.getBookFromISBN(userId, book.id, null);

        if (BBBLibraryItem.STATUS_ARCHIVED.equals(item.visibilityStatus)) {
            return ContentProviderOperation.newDelete(uri).build();
        } else if (BBBLibraryItem.STATUS_DELETED.equals(item.visibilityStatus)) {
            return ContentProviderOperation.newDelete(uri).build();
        }

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);

        builder.withValue(BBBContract.BooksColumns.BOOK_LIBRARY_ID, libraryId);
        builder.withValue(BBBContract.BooksColumns.BOOK_SERVER_ID, item.id);

        BBBImage[] images = book.images;
        BBBImage image = book.getImageData(BBBApiConstants.URN_IMAGE_COVER);

        if(image != null) {
            builder.withValue(BBBContract.BooksColumns.BOOK_COVER_URL, image.src);
        }


        if (book.publicationDate != null && book.publicationDate.length() > 0) {
            Calendar publishedDate = BBBCalendarUtil.attemptParse(book.publicationDate, BBBCalendarUtil.FORMAT_YEAR_MONTH_DAY, BBBCalendarUtil.FORMAT_YEAR, BBBCalendarUtil.FORMAT_TIME_STAMP);

            if (publishedDate != null) {
                builder.withValue(BBBContract.BooksColumns.BOOK_PUBLICATION_DATE, publishedDate.getTimeInMillis());
            } else {
                builder.withValue(BBBContract.BooksColumns.BOOK_PUBLICATION_DATE, 0);
            }
        } else {
            builder.withValue(BBBContract.BooksColumns.BOOK_PUBLICATION_DATE, 0);
        }

        long time = 0;
        long purchaseTime = -1;

        if (item.purchasedDate != null && item.purchasedDate.length() > 0) {
            Calendar purchaseDate = BBBCalendarUtil.parseDate(item.purchasedDate, BBBCalendarUtil.FORMAT_TIME_STAMP);
            time = purchaseDate.getTimeInMillis();
            purchaseTime = time;
            builder.withValue(BBBContract.BooksColumns.BOOK_PURCHASE_DATE, time);
        } else if (item.sampledDate != null && item.sampledDate.length() > 0) {
            Calendar sampledDate = BBBCalendarUtil.parseDate(item.sampledDate, BBBCalendarUtil.FORMAT_TIME_STAMP);
            time = sampledDate.getTimeInMillis();
            builder.withValue(BBBContract.BooksColumns.BOOK_PURCHASE_DATE, time);
        } else {
            LogUtils.e(BBBProviderHelper.class.getSimpleName(), "Book with id " + book.id + " has no purchasedDate or sampledDated");
            builder.withValue(BBBContract.BooksColumns.BOOK_PURCHASE_DATE, 0);
        }

        if (existingBook != null || time == 0) {
            time = System.currentTimeMillis();
        }

        builder.withValue(BBBContract.BooksColumns.BOOK_TITLE, book.title);
        builder.withValue(BBBContract.BooksColumns.BOOK_ISBN, book.id);

        BBBLink authorLink = book.getLinkData(BBBApiConstants.URN_CONTRIBUTOR);

        if(authorLink != null) {
            builder.withValue(BBBContract.BooksColumns.BOOK_AUTHOR, authorLink.title);
        } else {
            builder.withValue(BBBContract.BooksColumns.BOOK_AUTHOR, "");
        }

        BBBLink publisherLink = book.getLinkData(BBBApiConstants.URN_PUBLISHER);

        if(publisherLink != null) {
            builder.withValue(BBBContract.BooksColumns.BOOK_PUBLISHER, publisherLink.title);
        }

        builder.withValue(BBBContract.BooksColumns.BOOK_SYNC_DATE, syncTimestamp);
        builder.withValue(BBBContract.BooksColumns.BOOK_UPDATE_DATE, time);

        if (BBBLibraryItem.READING_STATUS_FINISHED.equals(item.readingStatus)) {
            builder.withValue(BBBContract.BooksColumns.BOOK_STATE, BBBContract.Books.BOOK_STATE_FINISHED);
        } else if (BBBLibraryItem.READING_STATUS_READING.equals(item.readingStatus)) {
            builder.withValue(BBBContract.BooksColumns.BOOK_STATE, BBBContract.Books.BOOK_STATE_READING);
        } else if (BBBLibraryItem.READING_STATUS_UNREAD.equals(item.readingStatus)) {

            if (existingBook == null && purchaseTime != -1 && BBBCalendarUtil.isTimeWithinTimePeriodFromNow(purchaseTime, BusinessRules.RECENT_BOOKS_TIME_PERIOD)) {
                builder.withValue(BBBContract.BooksColumns.BOOK_STATE, BBBContract.Books.BOOK_STATE_RECENTLY_PURCHASED);
            } else {

                if (existingBook != null && existingBook.state == BBBContract.Books.BOOK_STATE_RECENTLY_PURCHASED && purchaseTime != -1 && BBBCalendarUtil.isTimeWithinTimePeriodFromNow(purchaseTime, BusinessRules.RECENT_BOOKS_TIME_PERIOD)) {
                    builder.withValue(BBBContract.BooksColumns.BOOK_STATE, BBBContract.Books.BOOK_STATE_RECENTLY_PURCHASED);
                } else {
                    builder.withValue(BBBContract.BooksColumns.BOOK_STATE, BBBContract.Books.BOOK_STATE_UNREAD);
                }
            }
        }

        boolean is_sample = BBBLibraryItem.PURCHASE_STATUS_SAMPLED.equals(item.purchaseStatus);
        builder.withValue(BBBContract.BooksColumns.BOOK_IS_SAMPLE, is_sample);

        builder.withValue(BBBContract.BooksColumns.BOOK_FORMAT, "epub2");

        BBBLink[] links = item.links;

        if (links != null) {

            for (int i = 0; i < links.length; i++) {

                if (BBBApiConstants.URN_FULL_MEDIA.equals(links[i].rel)) {
                    builder.withValue(BBBContract.BooksColumns.BOOK_MEDIA_PATH, links[i].href);
                } else if (BBBApiConstants.URN_MEDIA_KEY.equals(links[i].rel)) {
                    builder.withValue(BBBContract.BooksColumns.BOOK_KEY_PATH, links[i].href);
                }
            }
        }

        links = book.links;

        if (links != null) {

            for (int i = 0; i < links.length; i++) {

                if (BBBApiConstants.URN_SAMPLE_MEDIA.equals(links[i].rel) && BBBLibraryItem.PURCHASE_STATUS_SAMPLED.equals(item.purchaseStatus)) {
                    builder.withValue(BBBContract.BooksColumns.BOOK_MEDIA_PATH, links[i].href);
                }
            }
        }

        // ALA-318 - Handle sample/embedded -> full book and vice versa
        builder.withValue(BBBContract.BooksColumns.BOOK_IS_EMBEDDED, false);

        if (existingBook != null) {

            if (existingBook.sample_book != is_sample || existingBook.embedded_book) {

                if (item.id != existingBook.server_id) {
                    LogUtils.e(TAG, "Replacing server_id " + existingBook.server_id + " with " + item.id + " on book " + existingBook.id);
                }

                existingBook.server_id = item.id;
                existingBook.download_status = BBBContract.Books.NOT_DOWNLOADED;

                BookHelper.updateBook(BBBContract.Books.buildBookIdUri(existingBook.id), existingBook, true);
                BookHelper.deletePhysicalBook(existingBook);
            }

            if (existingBook.sync_state == BBBContract.SyncState.STATE_NORMAL) {
                builder.withValue(BBBContract.BooksColumns.BOOK_SYNC_STATE, BBBContract.SyncState.STATE_NORMAL);
            } else if (existingBook.embedded_book && existingBook.sync_state == BBBContract.SyncState.STATE_DELETED) {
                //ALA-1750 covers the case when we have deleted the embedded book & we buy it at a later time.
                builder.withValue(BBBContract.BooksColumns.BOOK_SYNC_STATE, BBBContract.SyncState.STATE_NORMAL);
            } else { // Local changes always win
                builder.withValue(BBBContract.BooksColumns.BOOK_STATE, existingBook.state);
            }
        } else {
            builder.withValue(BBBContract.BooksColumns.BOOK_SYNC_STATE, BBBContract.SyncState.STATE_NORMAL);
        }

        return builder.build();
    }

    public static String getString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);

        if (index == -1) {
            return null;
        } else {
            return cursor.getString(index);
        }
    }

    public static long getLong(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);

        if (index == -1) {
            return 0;
        } else {
            return cursor.getLong(index);
        }
    }

    public static int getInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);

        if (index == -1) {
            return 0;
        } else {
            return cursor.getInt(index);
        }
    }
}