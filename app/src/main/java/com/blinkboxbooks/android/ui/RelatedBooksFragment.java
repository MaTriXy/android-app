// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.api.model.BBBBookInfoList;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.ui.shop.ImpressionReporterFragment;
import com.blinkboxbooks.android.ui.shop.SearchActivity;
import com.blinkboxbooks.android.ui.shop.ShopActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.widget.AboutBookItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows Books which are related to the given book, displaying them in a row.
 */
public class RelatedBooksFragment extends ImpressionReporterFragment {

    private static final String REQUEST_ID = RelatedBooksFragment.class.getSimpleName();

    private static final String PARAM_BOOKS = "books";
    private static final String PARAM_BOOK = "book";

    private Book mBook;

    private ArrayList<BBBBookInfo> mRelatedBooks;
    private List<ArrayList<AboutBookItem>> mAboutBookItemRows;

    private View mButtonOpenShop;

    private TextView mTextViewErrorNoBooks;

    private LinearLayout mBookContainer;
    private List<LinearLayout> mBookRows;

    private boolean mDownloading = false;
    private int mNumColumns;
    private int mNumRows;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mRelatedBooks = (ArrayList<BBBBookInfo>) savedInstanceState.getSerializable(PARAM_BOOKS);
            mBook = (Book) savedInstanceState.getSerializable(PARAM_BOOK);
        }
        setScreenName(AnalyticsHelper.GA_SCREEN_Shop_Related);
    }

    @Override
    public void onResume() {
        super.onResume();
        BBBRequestManager.getInstance().addResponseHandler(REQUEST_ID, bookInfoListHandler);

    }

    @Override
    public void onPause() {
        super.onPause();
        BBBRequestManager.getInstance().removeResponseHandler(REQUEST_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mRelatedBooks != null) {
            outState.putSerializable(PARAM_BOOKS, mRelatedBooks);
        }

        if (mBook != null) {
            outState.putSerializable(PARAM_BOOK, mBook);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_relatedbooks, container, false);

        mBookContainer = (LinearLayout) view.findViewById(R.id.bookcontainer);
        mButtonOpenShop = view.findViewById(R.id.button_openshop);

        mTextViewErrorNoBooks = (TextView) view.findViewById(R.id.textview_error_no_books);

        mButtonOpenShop.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ENDOF_SAMPLE, AnalyticsHelper.GA_EVENT_SHOP_MORE_BOOKS, mBook.isbn, null);
                Intent intent = new Intent(getActivity(), ShopActivity.class);
                startActivity(intent);
            }
        });

        mNumColumns = LibraryController.numberColumns;
        setNumColumsAndRows(mNumColumns, 1);
        return view;
    }

    /**
     * Populate the related books with the specified number of columns and rows. At the moment we have a hard limit
     * on 2 rows, so any value higher than 2 will be capped to 2.
     * @param columns the number of columns to populate
     * @param rows the number of row to populate (should currently be 1 or 2)
     */
    public void setNumColumsAndRows(int columns, int rows) {
        mNumColumns = columns;

        mBookContainer.removeAllViews();
        mBookRows = new ArrayList<LinearLayout>();
        int paddingTop = getResources().getDimensionPixelOffset(R.dimen.gap_medium);
        mNumRows = rows;
        mAboutBookItemRows = new ArrayList<ArrayList<AboutBookItem>>(mNumRows);
        for (int i=0 ; i<rows; i++) {
            mAboutBookItemRows.add(new ArrayList<AboutBookItem>());
            LinearLayout bookRow = new LinearLayout(getActivity());
            bookRow.setPadding(0, paddingTop, 0, 0);
            mBookRows.add(bookRow);
            mBookContainer.addView(bookRow);
            populateBookRow(bookRow, mNumColumns, mAboutBookItemRows.get(i), i);
        }
    }

    /**
     * Enables/Disables the shop button
     *
     * @param enabled
     */
    public void setShopButtonEnabled(boolean enabled) {
        mButtonOpenShop.setEnabled(enabled);
    }

    /**
     * Show/Hide the shop button
     *
     * @param visible
     */
    public void setShowButtonVisible(boolean visible) {
        mButtonOpenShop.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /*
     * Creates a row of GridViewBookItems to display a row of books
     */
    private void populateBookRow(LinearLayout layout, int numberColumns, ArrayList<AboutBookItem> aboutBookItems, int rowNumber) {
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setWeightSum(numberColumns + (LibraryController.GAP_WEIGHT * (numberColumns - 1)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams gap_params = new LinearLayout.LayoutParams(0, 1, LibraryController.GAP_WEIGHT);

        View view;
        int j = 0;

        for (int i = 0; i < numberColumns; i++, j++) {
            AboutBookItem item = new AboutBookItem(getActivity());
            item.setBookCoverWidth(LibraryController.bookCoverWidth);
            aboutBookItems.add(i, item);

            item.setOnClickListener(createRelatedBookOnClickListener(null,0));

            if (mRelatedBooks != null && i < mRelatedBooks.size()) {
                BBBBookInfo book = mRelatedBooks.get((numberColumns * rowNumber) + i);
                item.setBook(BookHelper.createBook(book), false);
                reportBookInfoImpression(i, book);
            } else {
                item.setBook(null, false);
            }

            layout.addView(item, j, params);
            j++;

            if (i != numberColumns - 1) {
                view = new View(getActivity());
                layout.addView(view, j, gap_params);
            }
        }
    }

    private OnClickListener createRelatedBookOnClickListener(final BBBBookInfo bookInfo, final int position) {

        return new OnClickListener() {

            public void onClick(View v) {

                if (!mDownloading) {

                    if (bookInfo == null) {
                        downloadRelatedBooks(mBook);
                    } else {
                        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ENDOF_SAMPLE,AnalyticsHelper.GA_EVENT_MOREBOOKS+position,bookInfo.id, null);
                        Activity context = getActivity();

                        Intent intent = new Intent(context, SearchActivity.class);
                        intent.putExtra(SearchActivity.ARG_VIEW_TYPE, SearchActivity.ViewType.RELATED_BOOKS);
                        intent.putExtra(SearchActivity.ARG_ISBN, mBook.isbn);
                        intent.putExtra(SearchActivity.ARG_PUT_FIRST, position-1);

                        AnalyticsHelper.getInstance().sendClickOnProduct(getScreenName(), bookInfo);

                        context.startActivity(intent);
                        context.overridePendingTransition(0, 0);
                    }
                }
            }
        };
    }

    private void setData(ArrayList<BBBBookInfo> books) {
        mRelatedBooks = books;

        Book book;
        BBBBookInfo bookInfo;

        for (int i = 0; i < mNumRows; i++) {
            for (int j = 0; j < mNumColumns; j++) {
                int bookIndex = (i * mNumColumns) + j;
                if (bookIndex < mRelatedBooks.size()) {
                    bookInfo = mRelatedBooks.get(bookIndex);
                    book = BookHelper.createBook(bookInfo);
                    reportBookInfoImpression(bookIndex, mRelatedBooks.get(bookIndex));
                    mAboutBookItemRows.get(i).get(j).setBook(book, false);
                    mAboutBookItemRows.get(i).get(j).setOnClickListener(createRelatedBookOnClickListener(bookInfo, bookIndex + 1));
                }
            }
        }
    }

    private void showConnecting(boolean connectionError) {
        Book book;

        for (int i = 0; i < mNumRows; i++) {
            for (int j = 0; j < mNumColumns; j++) {
                book = new Book();
                book.download_status = connectionError ? BBBContract.Books.DOWNLOAD_FAILED : BBBContract.Books.DOWNLOADING;
                mAboutBookItemRows.get(i).get(j).setBook(book, false);
            }
        }
    }

    /**
     * Sets the book object for which we want to display related books. If we don't already have this information, downloads it.
     *
     * @param book
     */
    public void setBook(Book book) {

        if (mBook == null || mRelatedBooks == null) {
            mBook = book;

            downloadRelatedBooks(mBook);
        }
    }

    private void downloadRelatedBooks(Book book) {
        mDownloading = true;
        BBBRequest request = BBBRequestFactory.getInstance().createGetRelatedBooksRequest(book.isbn, null, mNumColumns * mNumRows);
        BBBRequestManager.getInstance().executeRequest(REQUEST_ID, request);
        showConnecting(false);
    }

    private final BBBBasicResponseHandler<BBBBookInfoList> bookInfoListHandler = new BBBBasicResponseHandler<BBBBookInfoList>() {

        public void receivedData(BBBResponse response, BBBBookInfoList bookList) {
            mDownloading = false;

            if (bookList.items == null || bookList.items.length == 0) {
                mTextViewErrorNoBooks.setVisibility(View.VISIBLE);
                mBookContainer.setVisibility(View.GONE);
            } else {
                ArrayList<BBBBookInfo> items = new ArrayList<BBBBookInfo>();
                for (BBBBookInfo item : bookList.items) {
                    items.add(item);
                }
                setData(items);
            }
        }

        public void receivedError(BBBResponse response) {
            mDownloading = false;

            showConnecting(true);
        }
    };
}