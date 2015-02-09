// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.reader.TableOfContentsFragment.TableOfContentsListener;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * 18.2.3.5 - The table of contents page
 */
public class TableOfContentsActivity extends BaseActivity implements TableOfContentsListener {

    public static final String PARAM_BOOK = "book";

    private Book mBook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tableofcontents);

        mBook = (Book) getIntent().getSerializableExtra(PARAM_BOOK);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.table_of_contents);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mScreenName = AnalyticsHelper.GA_SCREEN_Reader_TOCScreen;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Book getBook() {
        return mBook;
    }

    @Override
    public void onTableOfContentsItemSelected(final String url, boolean hasLastReadingPosition) {

        if (!hasLastReadingPosition) {
            goToUrl(url, false);
            return;
        }

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(this);
        builder.setTitle(R.string.title_save_your_reading_position);
        builder.setMessage(getString(R.string.dialog_keep_reading_position));
        builder.setNegativeButton(R.string.dont_save, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                goToUrl(url, false);
            }
        });

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                goToUrl(url, true);
            }
        }).show();
    }

    private void goToUrl(String url, boolean keepReadingPosition) {

        Intent intent = new Intent(this, ReaderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setData(Books.buildBookIdUri(mBook.id));
        intent.putExtra(ReaderActivity.PARAM_NEW_POSITION, url);
        intent.putExtra(ReaderActivity.PARAM_KEEP_READING_POSITION, keepReadingPosition);
        startActivity(intent);
        finish();
    }
}