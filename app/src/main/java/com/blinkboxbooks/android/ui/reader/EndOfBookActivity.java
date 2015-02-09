// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.RelatedBooksFragment;
import com.blinkboxbooks.android.util.BBBUIUtils;

/**
 * Screen to show when a user has browsed past the last page of a book
 */
public class EndOfBookActivity extends BaseActivity {

    public static final String PARAM_BOOK = "book";

    RelatedBooksFragment mRelatedBooksFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_end_of_book);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRelatedBooksFragment = (RelatedBooksFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_relatedbooks);
        mRelatedBooksFragment.setHasBeenViewed(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Book book = (Book) getIntent().getSerializableExtra(PARAM_BOOK);
        mRelatedBooksFragment.setBook(book);
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        if (isTablet && BBBUIUtils.getScreenOrientation(this) == Configuration.ORIENTATION_PORTRAIT) {
            mRelatedBooksFragment.setNumColumsAndRows(LibraryController.numberColumns, 2);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}