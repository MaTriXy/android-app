// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.dialog.BookActionDialogFragment;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.ui.library.LibraryActivity;
import com.blinkboxbooks.android.ui.reader.ReaderActivity;
import com.blinkboxbooks.android.util.BBBUIUtils;

/**
 * Controller for the BookListFragment
 */
@SuppressLint("InflateParams")
public class LibraryController {

    private static final LibraryController instance = new LibraryController();

    public static LibraryController getInstance() {
        return instance;
    }

    /* The ratio of a book covers height compared to its width */
    public static final float WIDTH_HEIGHT_RATIO = 1.53f;

    /* the weight of the gap between books in a row of books */
    public static final float GAP_WEIGHT = 0.12f;

    /* the weight of the width of a book cover in a row of books */
    public static final float BOOK_WEIGHT = 0.88f;

    private static final String TAG_DELETE_EMBEDDED_BOOK = "tag_delete_embedded_book";
    private static final String TAG_DELETE_SAMPLE_BOOK = "tag_delete_sample_book";
    private static final String TAG_DELETE_BOOK = "tag_delete_book";

    public static int bookCoverHeight;
    public static int bookCoverWidth;

    public static int fullScreenImageWidth;

    public static int numberColumns;
    public static int rowLimit;

    private LibraryActivity mLibraryActivity;

    private boolean mHelpShowing = false;

    private LibraryController() {
    }

    @SuppressLint("NewApi")
    public void init(Context context) {
        numberColumns = context.getResources().getInteger(R.integer.library_number_columns);
        rowLimit = context.getResources().getInteger(R.integer.library_row_limit);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        Point screenSize = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(screenSize);
        } else {
            display.getSize(screenSize);
        }

        int sScreenWidth = screenSize.x;
        int sScreenHeight = screenSize.y;

        PointF coverDimens = BBBUIUtils.calculateRowItemDimens(context, numberColumns, BOOK_WEIGHT, GAP_WEIGHT, BBBUIUtils.getScreenWidth(context));
        bookCoverWidth = (int) coverDimens.x;
        bookCoverHeight = (int) coverDimens.y;

        //set once to ensure we definitely use the same value for both orientations
        if (fullScreenImageWidth == 0) {
            //so we use the same value no matter which orientation the device is in.
            fullScreenImageWidth = Math.min(sScreenWidth, sScreenHeight);
        }
    }

    /**
     * Sets the current library activity
     *
     * @param libraryActivity
     */
    public void setLibraryActivity(LibraryActivity libraryActivity) {
        mLibraryActivity = libraryActivity;
    }

    public static Intent getOpenBookIntent(Context context, Book book) {
        Intent readBookIntent = new Intent(context, ReaderActivity.class);
        readBookIntent.setData(BBBContract.Books.buildBookIdUri(book.id));
        return readBookIntent;
    }

    public void showDownloadLimitExceededError(Book book) {
        if (mLibraryActivity == null) {
            return;
        }

        String title = mLibraryActivity.getString(R.string.title_max_downloads_exceeded);
        String message = mLibraryActivity.getString(R.string.dialog_max_downloads_exceeded);
        String button = mLibraryActivity.getString(R.string.button_close);
        BookActionDialogFragment dialogFragment = BookActionDialogFragment.newInstance(book, title, message, button, null);
        mLibraryActivity.showDialog(dialogFragment, BookActionDialogFragment.class.getSimpleName(), false);
    }
}
