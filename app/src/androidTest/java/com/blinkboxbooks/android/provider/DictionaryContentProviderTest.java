// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

    import android.database.Cursor;
    import android.net.Uri;
    import android.test.ProviderTestCase2;
    import android.test.mock.MockContentResolver;
    import com.blinkboxbooks.android.controller.DictionaryController;

public class DictionaryContentProviderTest extends ProviderTestCase2<DictionaryContentProvider> {

        private MockContentResolver mMockResolver;
        private DictionaryController mDController;

        private String abductDefinitionOutput = "take someone away, especially by force";
        private String abaseDefinitionOutput = "behave in a way that causes other people to lose their respect for you";

        public DictionaryContentProviderTest() {
            super(DictionaryContentProvider.class, DictionaryContract.CONTENT_AUTHORITY);
        }

        @Override
        protected void setUp() throws Exception {
            mDController = DictionaryController.getInstance();

            if(!mDController.dictionaryFileUnzipped()) {
                mDController.unZipDictionary(mDController.getDatabaseFileFullPathLocation());
            }

            super.setUp();

            mMockResolver = getMockContentResolver();
        }


        public void testQueryForDerivatives() {
            assertTrue(mDController.dictionaryFileUnzipped());
            String query = "play";
            Uri uri = DictionaryContract.Words.queryForDerivatives(query);
            Cursor cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DERIVATIVES, null, null, null);
            cursor.moveToFirst();
            assertEquals(2, cursor.getColumnCount());
            assertEquals(2, cursor.getCount());
            assertEquals("playability", cursor.getString(0));
            assertEquals("noun", cursor.getString(1));
            cursor.close();

            query = "flag";
            uri = DictionaryContract.Words.queryForDerivatives(query);
            cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DERIVATIVES, null, null, null);
            assertEquals(1, cursor.getCount());
            cursor.close();
        }

        public void testNormalQuery() {
            assertTrue(mDController.dictionaryFileUnzipped());

            //Queries database with specified string
            String query = "abduct";
            Uri uri = DictionaryContract.Words.queryForDefinitions(query);
            Cursor cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DEFINITIONS, null, null, null);
            cursor.moveToFirst();

            String reslt = cursor.getString(cursor.getColumnIndex(DictionaryContract.Words.DEF_DEFINITION));
            assertEquals(abductDefinitionOutput, reslt);

            cursor.close();


        }

        public void testEdgeQuery() {
            assertTrue(mDController.dictionaryFileUnzipped());

            //Queries database with 2 expected entries
            String query = "abase";
            Uri uri = DictionaryContract.Words.queryForDefinitions(query);
            Cursor cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DEFINITIONS, null, null, null);
            cursor.moveToFirst();

            int expectedEntries = 1;
            assertEquals(expectedEntries, cursor.getCount());

            String defOutput = cursor.getString(cursor.getColumnIndex(DictionaryContract.Words.DEF_DEFINITION));
            assertEquals(abaseDefinitionOutput, defOutput);
            cursor.close();

            //Queries database with 0 expected entries
            query = "nonsensicalString";
            uri = DictionaryContract.Words.queryForDefinitions(query);
            cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DEFINITIONS, null, null, null);
            assertNull(cursor);

            //Queries database with a multi-word query
            query = "Union Jack";
            uri = DictionaryContract.Words.queryForDefinitions(query);
            cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DEFINITIONS, null, null, null);
            //Queries with 0 <= words <= 3 should return query
            assertNotNull(cursor);
            cursor.close();

            query = "Too many words here";
            uri = DictionaryContract.Words.queryForDefinitions(query);
            cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DEFINITIONS, null, null, null);
            //Queries with >3 words are not supported
            assertNull(cursor);
        }

        public void testQueryUri() {
            assertTrue(mDController.dictionaryFileUnzipped());

            String query = "sample";
            Uri uri = DictionaryContract.Words.queryForDefinitions(query);
            assertEquals(query, DictionaryContract.Words.getQuery(uri));
        }


        public void testWordFormQuery() {
            assertTrue(mDController.dictionaryFileUnzipped());

            //Queries database with different forms of a base word
            String query = "flagged";
            Uri uri = DictionaryContract.Words.queryForDefinitions(query);
            Cursor cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DEFINITIONS, null, null, null);
            int expectedEntries = 5;
            assertEquals(expectedEntries, cursor.getCount());
            cursor.close();

            query = "flagging";
            uri = DictionaryContract.Words.queryForDefinitions(query);
            cursor = mMockResolver.query(uri, DictionaryContract.Words.PROJECTION_DEFINITIONS, null, null, null);
            expectedEntries = 4;
            assertEquals(expectedEntries, cursor.getCount());
            cursor.close();
        }

        public void testContentForHtml() {
            //Test the elements required for the dictionary definition web view
            String query = "inferno";

            Cursor cursorWords = mMockResolver.query(DictionaryContract.Words.queryForWord(query), null, null, null, null);
            Cursor cursorDefinitions = mMockResolver.query(DictionaryContract.Words.queryForDefinitions(query), null, null, null, null);

            cursorWords.moveToFirst(); cursorDefinitions.moveToFirst();

            assertEquals(query, cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.WORD)));
            assertEquals("ɪnˈfəːnəʊ", cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.PRONUNCIATION)));
            assertEquals("a large uncontrollable fire", cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_DEFINITION)));
            assertNull(cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_EXAMPLE)));

            cursorWords.close(); cursorDefinitions.close();
        }
    }

