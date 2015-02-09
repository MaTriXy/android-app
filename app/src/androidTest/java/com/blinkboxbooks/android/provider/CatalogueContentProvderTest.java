// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;


import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

import android.test.mock.MockContentResolver;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;

public class CatalogueContentProvderTest extends ProviderTestCase2<CatalogueContentProvider> {

    private static final int BESTSELLERS_FICTION_LOCATION_ID = 100;

    private MockContentResolver mMockResolver;

    public CatalogueContentProvderTest() {
        super(CatalogueContentProvider.class, CatalogueContract.CONTENT_AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        BBBRequestFactory.getInstance().setHostDefault(getContext().getResources().getString(R.string.rest_server_host));
        mMockResolver = getMockContentResolver();
    }

    public void testGetBooksByCategoryName() {
        Uri uri = CatalogueContract.Books.getBooksForCategoryNameUri("bestsellers-fiction", false, 100, 0);
        Cursor cursor = mMockResolver.query(uri, null, null, null, null);

        assertEquals(100, cursor.getCount());
        cursor.moveToPosition(50);

        String points = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.CLUBCARD_POINTS_AWARDED));
        assertTrue(points != "");

        double price = cursor.getDouble(cursor.getColumnIndex(CatalogueContract.Books.PRICE));
        assertTrue(price > 0);

        cursor.close();
    }

    public void testSearchForBooks() {
        Uri uri = CatalogueContract.Books.getBookSearchUri("Harry Potter", false, 100, 0);
        Cursor cursor = mMockResolver.query(uri, null, null, null, null);
        assertTrue(cursor.getCount() > 10);

        cursor.moveToFirst();
        double price = cursor.getDouble(cursor.getColumnIndex(CatalogueContract.Books.PRICE));
        assertTrue(price > 0);

        cursor.close();
    }

    public void testGetSearchSuggestions() {
        Uri uri = CatalogueContract.SearchSuggestion.getSearchSuggestionUri("Harry Potter");
        Cursor cursor = mMockResolver.query(uri, null, null, null, null);

        assertTrue(cursor.getCount() > 5);

        cursor.close();
    }

    public void testGetCategories() {
        Uri uri = CatalogueContract.Category.getCategories();
        Cursor cursor = mMockResolver.query(uri, null, null, null, null);

        assertTrue(cursor.getCount() > 5);

        cursor.close();
    }

    public void testSearchQuery() {
        Uri uri = CatalogueContract.Books.getBookSearchUri("Inferno", false, 100, 0);
        String query = CatalogueContract.Books.getSearchQuery(uri);

        assertEquals(query, "Inferno");

        Cursor cursor = mMockResolver.query(uri, null, null, null, null);
        assertTrue(cursor.getCount() > 0);

        cursor.close();
    }

    //Tests searchSuggestion library functions and asserts 10 suggestions are found
    public void testGetSearchSuggestionQuery() {
        String query = "Inferno";
        Uri uri = CatalogueContract.SearchSuggestion.getSearchSuggestionUri(query);

        String getQuery = CatalogueContract.SearchSuggestion.getSearchSuggestionQuery(uri);

        assertEquals(query, getQuery);

        Cursor cursor = mMockResolver.query(uri, null, null, null, null);
        assertTrue(cursor.getCount() == 10);

    }

    public void testCategoryFunctions() {
        Uri uri = CatalogueContract.Category.getCategories();
        assertEquals(CatalogueContract.Category.CONTENT_URI, uri);
    }

    /**
     * Tests an externally used uri (used by Huddl devices).
     */
    public void testExternalAPIBookSearch() {
        Uri uri = Uri.parse("content://com.blinkboxbooks.android.provider.catalogue/books/search/9781444761191");
        Cursor cursor = mMockResolver.query(uri, null, null, null, null);
        assertTrue(cursor.getCount() > 0);

        cursor.close();

    }

    /**
     * Tests the externally used uri (used by Huddl devices).
     */
    public void testExternalAPIBooksByCategory() {
        Uri uri = Uri.parse("content://com.blinkboxbooks.android.provider.catalogue/books/caategory_name/bestsellers-fiction");
        Cursor cursor = mMockResolver.query(uri, null, null, null, null);

        assertTrue(cursor.getCount() > 0);

        cursor.close();
    }
}