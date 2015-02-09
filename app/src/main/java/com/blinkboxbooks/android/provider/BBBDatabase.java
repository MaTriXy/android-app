// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.blinkboxbooks.android.provider.BBBContract.Authors;
import com.blinkboxbooks.android.provider.BBBContract.AuthorsColumns;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.provider.BBBContract.BookmarksColumns;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.provider.BBBContract.BooksColumns;
import com.blinkboxbooks.android.provider.BBBContract.Libraries;
import com.blinkboxbooks.android.provider.BBBContract.LibrariesColumns;
import com.blinkboxbooks.android.provider.BBBContract.NavPoints;
import com.blinkboxbooks.android.provider.BBBContract.NavPointsColumns;
import com.blinkboxbooks.android.provider.BBBContract.ReaderSettings;
import com.blinkboxbooks.android.provider.BBBContract.ReaderSettingsColumns;
import com.blinkboxbooks.android.provider.BBBContract.Sections;
import com.blinkboxbooks.android.provider.BBBContract.SectionsColumns;

import java.io.File;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for {@link BBBProvider}
 */
@SuppressWarnings("unused")
public class BBBDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bbb.db";

    private static final int DATABASE_VERSION_NO_KEY = 1;
    private static final int DATABASE_VERSION_NO_CFI = 2;
    private static final int DATABASE_VERSION = 8;

    public BBBDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /*
     * (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Books.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                BooksColumns.BOOK_LIBRARY_ID + " INTEGER NOT NULL," +
                BooksColumns.BOOK_SERVER_ID + " TEXT," +
                BooksColumns.BOOK_SYNC_STATE + " INTEGER DEFAULT 0," +
                BooksColumns.BOOK_AUTHOR + " TEXT NOT NULL," +
                BooksColumns.BOOK_ISBN + " TEXT NOT NULL," +
                BooksColumns.BOOK_TITLE + " TEXT NOT NULL," +
                BooksColumns.BOOK_TAGS + " TEXT," +
                BooksColumns.BOOK_COVER_URL + " TEXT," +
                BooksColumns.BOOK_OFFER_PRICE + " REAL," +
                BooksColumns.BOOK_NORMAL_PRICE + " REAL," +
                BooksColumns.BOOK_DESCRIPTION + " TEXT," +
                BooksColumns.BOOK_PUBLISHER + " TEXT," +
                BooksColumns.BOOK_PURCHASE_DATE + " INTEGER NOT NULL," +
                BooksColumns.BOOK_UPDATE_DATE + " INTEGER NOT NULL," +
                BooksColumns.BOOK_SYNC_DATE + " INTEGER NOT NULL," +
                BooksColumns.BOOK_PUBLICATION_DATE + " INTEGER NOT NULL," +
                BooksColumns.BOOK_STATE + " INTEGER NOT NULL," +
                BooksColumns.BOOK_DOWNLOAD_COUNT + " INTEGER," +
                BooksColumns.BOOK_IN_DEVICE_LIBRARY + " INTEGER DEFAULT 1," +
                BooksColumns.BOOK_IS_EMBEDDED + " INTEGER DEFAULT 0," +
                BooksColumns.BOOK_IS_SAMPLE + " INTEGER NOT NULL," +
                BooksColumns.BOOK_FORMAT + " TEXT NOT NULL," +
                BooksColumns.BOOK_FILE_PATH + " TEXT," +
                BooksColumns.BOOK_MEDIA_PATH + " TEXT," +
                BooksColumns.BOOK_KEY_PATH + " TEXT," +
                BooksColumns.BOOK_SIZE + " INTEGER," +
                BooksColumns.BOOK_DOWNLOAD_STATUS + " INTEGER," +
                BooksColumns.BOOK_DOWNLOAD_OFFSET + " INTEGER," +
                BooksColumns.BOOK_ENCRYPTION_KEY + " BLOB )");

        db.execSQL("CREATE TABLE " + Libraries.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                LibrariesColumns.LIBRARY_CREATE_DATE + " INTEGER," +
                LibrariesColumns.LIBRARY_SYNC_DATE + " TEXT," +
                LibrariesColumns.LIBRARY_BOOKMARK_SYNC_DATE + " TEXT," +
                LibrariesColumns.LIBRARY_ACCOUNT + " TEXT NOT NULL )");

        db.execSQL("CREATE TABLE " + Bookmarks.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                BookmarksColumns.BOOKMARK_CLOUD_ID + " TEXT," +
                BookmarksColumns.BOOKMARK_BOOK_ID + " INTEGER NOT NULL," +
                BookmarksColumns.BOOKMARK_NAME + " TEXT," +
                BookmarksColumns.BOOKMARK_CONTENT + " TEXT," +
                BookmarksColumns.BOOKMARK_TYPE + " INTEGER NOT NULL," +
                BookmarksColumns.BOOKMARK_ANNOTATION + " TEXT," +
                BookmarksColumns.BOOKMARK_STYLE + " TEXT," +
                BookmarksColumns.BOOKMARK_UPDATE_BY + " TEXT," +
                BookmarksColumns.BOOKMARK_UPDATE_DATE + " INTEGER ," +
                BookmarksColumns.BOOKMARK_PERCENTAGE + " INTEGER ," +
                BookmarksColumns.BOOKMARK_STATE + " INTEGER DEFAULT 0," +
                BookmarksColumns.BOOKMARK_COLOR + " TEXT," +
                BookmarksColumns.BOOKMARK_ISBN + " TEXT NOT NULL," +
                BookmarksColumns.BOOKMARK_POSITION + " TEXT NOT NULL )");

        db.execSQL("CREATE TABLE " + Sections.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SectionsColumns.SECTION_BOOK_ID + " INTEGER," +
                SectionsColumns.SECTION_PATH + " TEXT," +
                SectionsColumns.SECTION_MEDIA_TYPE + " TEXT," +
                SectionsColumns.SECTION_INDEX + " INTEGER," +
                SectionsColumns.SECTION_FILE_SIZE + " INTEGER )");

        db.execSQL("CREATE TABLE " + NavPoints.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                NavPointsColumns.NAVPOINT_BOOK_ID + " INTEGER," +
                NavPointsColumns.NAVPOINT_LABEL + " TEXT," +
                NavPointsColumns.NAVPOINT_LINK + " TEXT," +
                NavPointsColumns.NAVPOINT_INDEX + " INTEGER," +
                NavPointsColumns.NAVPOINT_DEPTH + " INTEGER )");

        db.execSQL("CREATE TABLE " + Authors.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                AuthorsColumns.AUTHOR_BOOK_ID + " INTEGER," +
                AuthorsColumns.AUTHOR_ID + " TEXT," +
                AuthorsColumns.AUTHOR_NAME + " TEXT  )");

        db.execSQL("CREATE TABLE " + ReaderSettings.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                ReaderSettingsColumns.READER_BACKGROUND_COLOR + " INTEGER," +
                ReaderSettingsColumns.READER_FOREGROUND_COLOR + " INTEGER," +
                ReaderSettingsColumns.READER_FONT_SIZE + " REAL," +
                ReaderSettingsColumns.READER_BRIGHTNESS + " REAL," +
                ReaderSettingsColumns.READER_FONT_TYPEFACE + " TEXT," +
                ReaderSettingsColumns.READER_LINE_SPACE + " REAL," +
                ReaderSettingsColumns.READER_ORIENTATION_LOCK + " INTEGER," +
                ReaderSettingsColumns.READER_SHOW_HEADER + " INTEGER," +
                ReaderSettingsColumns.READER_SHOW_FOOTER + " INTEGER," +
                ReaderSettingsColumns.READER_CLOUD_BOOKMARK + " INTEGER," +
                ReaderSettingsColumns.READER_TEXT_ALIGN + " TEXT," +
                ReaderSettingsColumns.READER_MARGIN_TOP + " REAL," +
                ReaderSettingsColumns.READER_MARGIN_BOTTOM + " REAL," +
                ReaderSettingsColumns.READER_MARGIN_LEFT + " REAL," +
                ReaderSettingsColumns.READER_MARGIN_RIGHT + " REAL," +
                ReaderSettingsColumns.READER_ORIENTATION + " INTEGER," +
                ReaderSettingsColumns.READER_PUBLISHER_STYLES + " INTEGER," +
                ReaderSettingsColumns.READER_ACCOUNT + " TEXT  )");

        db.execSQL("CREATE TRIGGER delete_books BEFORE DELETE ON " + Libraries.TABLE_NAME + " " +
                "BEGIN " +
                "DELETE FROM " + Books.TABLE_NAME + " WHERE " + Books.BOOK_LIBRARY_ID + " = OLD." + BaseColumns._ID + "; " +
                "END;");

        db.execSQL("CREATE TRIGGER delete_bookmarks BEFORE DELETE ON " + Books.TABLE_NAME + " " +
                "BEGIN " +
                "DELETE FROM " + Bookmarks.TABLE_NAME + " WHERE " + Bookmarks.BOOKMARK_BOOK_ID + " = OLD." + BaseColumns._ID + "; " +
                "END;");
    }

    /*
     * (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite .SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion <= 7) {

            try {
                db.execSQL("ALTER TABLE " + ReaderSettings.TABLE_NAME + " ADD COLUMN " + ReaderSettingsColumns.READER_PUBLISHER_STYLES + " INTEGER DEFAULT 1");
            } catch(Exception e){}

            try {
                db.execSQL("ALTER TABLE "+Libraries.TABLE_NAME+" ADD COLUMN "+LibrariesColumns.LIBRARY_BOOKMARK_SYNC_DATE+" TEXT");
            } catch(Exception e){}
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void dropAllTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Books.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Libraries.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Bookmarks.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Sections.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + NavPoints.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Authors.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ReaderSettings.TABLE_NAME);
    }

    /**
     * Delete the database
     *
     * @param context
     */
    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Check whether the database exists
     *
     * @param context
     * @return true, if the database exists
     */
    public static boolean databaseExists(Context context) {
        File databaseFile = context.getDatabasePath(DATABASE_NAME);
        return databaseFile.exists();
    }
}