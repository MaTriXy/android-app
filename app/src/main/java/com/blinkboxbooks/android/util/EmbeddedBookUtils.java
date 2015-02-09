// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;

import com.blinkbox.java.book.exceptions.BBBEPubException;
import com.blinkbox.java.book.factory.BBBEPubFactory;
import com.blinkbox.java.book.json.BBBEPubBookInfo;
import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkbox.java.book.model.BBBEPubConstants;
import com.blinkbox.java.book.model.BBBEPubMetaData;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Library;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.LibraryHelper;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.provider.BBBContract.Libraries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class EmbeddedBookUtils {

    private static final String ASSET_PATH_EMBEDDED_BOOKS = "books";
    private static final long LIBRARY_ID_TEMPLATE = 1;

    /**
     * Check whether the embedded books should be extracted
     *
     * @param context
     * @return true if the embedded books should be extracted
     */
    public static boolean shouldExtractBooks(Context context) {
        int versionCode = -1;
        try {
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
        }

        int lastVersion = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_KEY_EMBEDDED_BOOKS_VERSION, -2);
        if (lastVersion == versionCode) {
            return false;
        }

        PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_EMBEDDED_BOOKS_VERSION, versionCode);
        return true;
    }

    private static String extractBook(Context context, File localBookDir, String fileName) throws IOException {
        File file = new File(localBookDir, fileName);
        String fullPath = file.getAbsolutePath();
        if (file.exists()) {
            return null;
        }

        // Extract the embedded book
        InputStream myInput = context.getAssets().open(ASSET_PATH_EMBEDDED_BOOKS + "/" + fileName);
        OutputStream myOutput = new FileOutputStream(file);

        // transfer bytes from the input file to the output file
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
        return fullPath;
    }

    /**
     * Load new embedded books into the template library
     *
     * @param context
     */
    public static void loadEmbeddedBooks(Context context) {
        try {
            // Create the template library account (will be index 1);
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Libraries.buildLibrariesAccountUri(LibraryHelper.TEMPLATE_ACCOUNT);
            Library library = new Library();
            library.id = LIBRARY_ID_TEMPLATE;
            library.account = LibraryHelper.TEMPLATE_ACCOUNT;
            contentResolver.insert(uri, LibraryHelper.getContentValues(library));

            // Scan for existing libraries to add the new books to
            Cursor cursor = contentResolver.query(Libraries.CONTENT_URI, null, null, null, null);
            Library[] libraries = new Library[cursor.getCount()];
            for (int i = 0; i < libraries.length; i++) {
                cursor.moveToNext();
                libraries[i] = LibraryHelper.createLibrary(cursor);
            }
            cursor.close();

            // Find and extract new embedded books
            File localBookDir = context.getDir(ASSET_PATH_EMBEDDED_BOOKS, Context.MODE_PRIVATE);
            String[] embeddedBooks = context.getAssets().list(ASSET_PATH_EMBEDDED_BOOKS);
            for (String bookPath : embeddedBooks) {
                String fullPath = extractBook(context, localBookDir, bookPath);
                if (fullPath == null) {
                    // File already exists
                    continue;
                }

                BBBEPubBook bbbePubBook = BBBEPubFactory.getInstance().createFromURL(context, fullPath, null);
                BBBEPubMetaData bbbePubMetaData = bbbePubBook.getMetaData();
                BBBEPubBookInfo bbbePubBookInfo = bbbePubBook.getBookInfo();

                // Store the embedded book in the template database
                Book book = new Book();
                book.state = Books.BOOK_STATE_UNREAD;
                book.embedded_book = true;
                book.in_device_library = true;
                book.format = bbbePubBook.getMimeType();
                book.file_path = fullPath;
                book.download_status = Books.DOWNLOADED;
                book.author = bbbePubBook.getAuthor();
                book.title = bbbePubBook.getTitle();
                book.cover_url = bbbePubBook.getCoverUrl();
                book.sample_book = bbbePubBookInfo != null && bbbePubBookInfo.isSample();

                book.publisher = bbbePubMetaData == null ? "" : bbbePubMetaData.getAttribute(BBBEPubConstants.METADATA_PUBLISHER);

                // Get the ISBN from the asset path
                book.isbn = bookPath.substring(0, bookPath.lastIndexOf('.'));

                String publication_date = bbbePubMetaData == null ? "" : bbbePubMetaData.getAttribute(BBBEPubConstants.METADATA_DATE);
                book.description = bbbePubMetaData == null ? "" : bbbePubMetaData.getAttribute(BBBEPubConstants.METADATA_DESCRIPTION);

                if (book.author == null) {
                    book.author = context.getString(R.string.unknown_author);
                }
                if (publication_date != null) {
                    Calendar timeStamp = BBBCalendarUtil.attemptParse(publication_date, BBBCalendarUtil.FORMAT_TIME_STAMP, BBBCalendarUtil.FORMAT_YEAR_MONTH_DAY, BBBCalendarUtil.FORMAT_YEAR);

                    if (timeStamp != null) {
                        book.publication_date = timeStamp.getTimeInMillis();
                    }
                }
                book.cover_url = fullPath + "/" + book.isbn + "_cover_image.png";

                for (Library existingLibrary : libraries) {
                    Uri libraryUri = Books.buildBookAccountUri(existingLibrary.account);

                    // Only insert if book doesn't already exist
                    if (BookHelper.getBookFromISBN(existingLibrary.account, book.isbn, null) == null) {
                        book.library_id = existingLibrary.id;
                        contentResolver.insert(libraryUri, BookHelper.getContentValues(book));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BBBEPubException e) {
            e.printStackTrace();
        }
    }
}
