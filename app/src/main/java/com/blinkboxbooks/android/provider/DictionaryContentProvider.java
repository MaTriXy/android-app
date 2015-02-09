// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.controller.DictionaryController;

/**
 * Provider for dictionary data
 */
public class DictionaryContentProvider extends ContentProvider {

    private static final String TABLE_WORDS = "WORDS";
    private static final String TABLE_DEFINITIONS = "DEFINITIONS";
    private static final String TABLE_DERIVATIVES = "DERIVATIVES";
    private static final String TABLE_WORDFORMS = "WORDFORMS";

    private static final int ID = 0;
    private static final int WORD = 1;
    private static final int DEFINITION = 2;
    private static final int DERIVATIVE = 3;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private int mWordId;
    private static SQLiteDatabase mDatabase;

    static {
        URI_MATCHER.addURI(DictionaryContract.CONTENT_AUTHORITY, "dictionary/word/*/id", ID);
        URI_MATCHER.addURI(DictionaryContract.CONTENT_AUTHORITY, "dictionary/word/*", WORD);
        URI_MATCHER.addURI(DictionaryContract.CONTENT_AUTHORITY, "dictionary/word/*/definitions/", DEFINITION);
        URI_MATCHER.addURI(DictionaryContract.CONTENT_AUTHORITY, "dictionary/word/*/derivatives/", DERIVATIVE);
    }

    /**
     * Returns the dictionary SQLiteDatabase
     * @return dictionary SQLiteDatabase
     */
    public static SQLiteDatabase getDatabase(){
        if (mDatabase == null || !mDatabase.isOpen()) {
            String path = DictionaryController.getInstance().getDatabaseFileFullPathLocation();
            mDatabase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
        }
        return mDatabase;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String query = DictionaryContract.Words.getQuery(uri);
        String[] args = {query};

        if(URI_MATCHER.match(uri) == ID) {
            return getDatabase().query(
                    TABLE_WORDS, DictionaryContract.Words.PROJECTION_WORD, DictionaryContract.Words._ID + "=?", args, null, null, null);
        }

        Cursor wordFormCursor = getDatabase().query(TABLE_WORDFORMS, DictionaryContract.Words.PROJECTION_WORDFORMS, DictionaryContract.Words.WORDFORMS_WORDFORM + "=?", args, null, null, null, null);

        int entries;
        String[] ids;

        if(wordFormCursor != null && wordFormCursor.moveToFirst()) {

            if (getCorrectWordEntryId(wordFormCursor, query)) {
                ids = new String[1]; ids[0] = String.valueOf(mWordId);
                entries = 1;
            } else {
                wordFormCursor.moveToFirst();
                entries = wordFormCursor.getCount();
                ids = new String[entries];
                for (int i = 0; i < entries; i++) {
                    ids[i] = Integer.toString(wordFormCursor.getInt(wordFormCursor.getColumnIndex(DictionaryContract.Words.WORDFORMS_ID)));
                    wordFormCursor.moveToNext();
                }
            }

            Cursor cursor = null;

            switch (URI_MATCHER.match(uri)) {
                case WORD:
                    cursor = getDatabase().query(
                            TABLE_WORDS, DictionaryContract.Words.PROJECTION_WORD, getSelection(DictionaryContract.Words._ID, entries), ids, null, null, sortOrder);
                    break;
                case DEFINITION:
                    cursor = getDatabase().query(
                            TABLE_DEFINITIONS, DictionaryContract.Words.PROJECTION_DEFINITIONS, getSelection(DictionaryContract.Words.DEF_WORD_ID, entries), ids, null, null, sortOrder);
                    break;
                case DERIVATIVE:
                    cursor = getDatabase().query(
                            TABLE_DERIVATIVES, DictionaryContract.Words.PROJECTION_DERIVATIVES, getSelection(DictionaryContract.Words.DER_WORD_ID, entries), ids, null, null, sortOrder);
                    break;
                default:
                    break;
            }
            return cursor;
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return DictionaryContract.CONTENT_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("This provider does not support the insert operation");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider does not support the delete operation");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider does not support the update operation");
    }

    private String getSelection(String where, int n) {
        StringBuilder mBuilder = new StringBuilder();
        if (n > 0) {
            mBuilder.append(where + "=? ");
            for (int i = 1; i < n; i++) {
                mBuilder.append("OR " + where + "=? ");
            }
        }
        return mBuilder.toString();
    }

    //Exploratory check to find more appropriate word entry id
    private boolean getCorrectWordEntryId(Cursor wordFormsCursor, String query) {
        if(wordFormsCursor.moveToFirst()) {
            int entries = wordFormsCursor.getCount();
            for (int i = 0; i < entries; i++) {

                String id = wordFormsCursor.getString(wordFormsCursor.getColumnIndex(DictionaryContract.Words.WORDFORMS_ID));

                Cursor wordCursor = BBBApplication.getApplication().getContentResolver().query(
                        DictionaryContract.Words.queryForWordOnId(id), null, null, null, null);

                if(wordCursor.moveToFirst() && wordCursor.getString(wordCursor.getColumnIndex(DictionaryContract.Words.WORD)).startsWith(query)) {
                    mWordId = Integer.valueOf(id);
                    wordCursor.close();
                    return true;
                }
                wordFormsCursor.moveToNext();
            }
        }
        return false;
    }
}