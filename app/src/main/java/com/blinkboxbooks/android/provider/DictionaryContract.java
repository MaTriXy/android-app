// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.blinkboxbooks.android.BuildConfig;

/**
 * Contract class for DictionaryContentProvider
 */
public class DictionaryContract {

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.dictionary";

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.provider.dictionary.word";

    public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static class Words implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(DictionaryContract.CONTENT_URI, "dictionary");

        public static final String _ID = "_ID";
        public static final String WORD = "WORD";
        public static final String PRONUNCIATION = "PRONUNCIATION";
        public static final String DEF_WORD_ID = "WORD_ID";
        public static final String DEF_DEFINITION = "DEFINITION";
        public static final String DEF_EXAMPLE = "EXAMPLE";
        public static final String DEF_WORD_TYPE = "WORD_TYPE";
        public static final String DER_WORD_ID = "WORD_ID";
        public static final String DER_DERIVATIVE = "DERIVATIVE";
        public static final String DER_TYPE = "DERIVATIVE_TYPE";
        public static final String WORDFORMS_WORDFORM = "WORDFORM";
        public static final String WORDFORMS_ID = "WORD_ID";

        public static final String[] PROJECTION_WORD = {WORD, PRONUNCIATION};
        public static final String[] PROJECTION_DERIVATIVES = {DER_DERIVATIVE, DER_TYPE};
        public static final String[] PROJECTION_DEFINITIONS = {DEF_WORD_ID, DEF_DEFINITION, DEF_EXAMPLE, DEF_WORD_TYPE};
        public static final String[] PROJECTION_WORDFORMS = {WORDFORMS_ID, WORDFORMS_WORDFORM};

        /**
         * Builds a Uri for getting word information for a word given its database _id
         * @param id the database id of the word
         * @return
         */
        public static Uri queryForWordOnId(String id) {
            return CONTENT_URI.buildUpon().appendPath("word").appendPath(id).appendPath("id").build();
        }

        /**
         * Builds a Uri for getting word information for a query
         * @param query word to be queried
         * @return
         */
        public static Uri queryForWord(String query) {
            return CONTENT_URI.buildUpon().appendPath("word").appendPath(query).build();
        }

        /**
         * Builds a Uri for getting definitions for a query
         * @param query word to be queried
         * @return
         */
        public static Uri queryForDefinitions(String query) {
            return CONTENT_URI.buildUpon().appendPath("word").appendPath(query).appendPath("definitions").build();
        }

        /**
         * Builds a Uri for getting derivatives for a query
         * @param query word to be queried
         * @return
         */
        public static Uri queryForDerivatives(String query) {
            return CONTENT_URI.buildUpon().appendPath("word").appendPath(query).appendPath("derivatives").build();
        }

        /**
         * Gets the query element of a Uri
         * @param uri the Uri
         * @return the query String
         */
        public static String getQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }
}
