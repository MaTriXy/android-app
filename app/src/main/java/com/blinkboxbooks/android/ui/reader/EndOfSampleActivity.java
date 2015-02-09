// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.api.model.BBBBookPriceList;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.RelatedBooksFragment;
import com.blinkboxbooks.android.ui.shop.ShopActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.BBBUIUtils;
import com.blinkboxbooks.android.util.DateUtils;
import com.blinkboxbooks.android.widget.BookCover;

/**
 * Screen to show when a user has browsed past the last page of a sample book.
 */
public class EndOfSampleActivity extends BaseActivity {

    public static final String PARAM_BOOK = "book";
    private static final String PARAM_PRICE = "price";
    private static final String RESPONSE_HANDLER_ID = EndOfSampleActivity.class.getSimpleName();

    private TextView mTextViewTitle;
    private TextView mTextViewAuthor;
    private TextView mTextViewPublished;
    private TextView mClubcardPoints;
    private TextView mTextViewPriceOriginal;
    private TextView mTextViewPrice;
    private ImageView mImageViewClubcard;
    private Button mButtonBuy;
    private BookCover mBookCover;
    private Book mBook;
    private RelatedBooksFragment mRelatedBooksFragment;
    private BBBBookPrice mPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_end_of_sample);

        if (savedInstanceState != null) {
            mPrice = (BBBBookPrice) savedInstanceState.getSerializable(PARAM_PRICE);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextViewTitle = (TextView) findViewById(R.id.textview_book_title);
        mTextViewAuthor = (TextView) findViewById(R.id.textview_book_author);
        mTextViewPublished = (TextView) findViewById(R.id.textview_book_published);
        mClubcardPoints = (TextView) findViewById(R.id.textview_clubcard_points);
        mTextViewPriceOriginal = (TextView) findViewById(R.id.textview_price_original);
        mTextViewPrice = (TextView) findViewById(R.id.textview_price_discounted);
        mImageViewClubcard = (ImageView) findViewById(R.id.imageview_clubcard);

        mButtonBuy = (Button) findViewById(R.id.button_buy_the_book_now);
        mBookCover = (BookCover) findViewById(R.id.bookcover);
        mRelatedBooksFragment = (RelatedBooksFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_relatedbooks);
        if (mRelatedBooksFragment != null) {
            mRelatedBooksFragment.setHasBeenViewed(true);
            int numColumns = getResources().getInteger(R.integer.end_of_sample_related_columns);
            mRelatedBooksFragment.setNumColumsAndRows(numColumns, 1);
        }
        mScreenName = AnalyticsHelper.GA_SCREEN_Reader_EndOfSampleScreen;
    }

    @Override
    protected void onResume() {
        super.onResume();

        BBBRequestManager.getInstance().addResponseHandler(RESPONSE_HANDLER_ID, responseHandler);
        PurchaseController.getInstance().setBaseActivity(this);
        Book book = (Book) getIntent().getSerializableExtra(PARAM_BOOK);
        setBook(book);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BBBRequestManager.getInstance().removeResponseHandler(RESPONSE_HANDLER_ID);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mPrice != null) {
            outState.putSerializable(PARAM_PRICE, mPrice);
        }
    }

    private void setBook(Book book) {
        mBook = book;
        mBook.sample_book = false;

        // Some books may have a null author so in this case we just set an empty string. We also do the same for title (Just in case)
        Spanned titleText = (mBook.title != null) ? Html.fromHtml(mBook.title) : new SpannedString("");
        Spanned authorText = (mBook.author != null) ? Html.fromHtml(mBook.author) : new SpannedString("");

        mTextViewTitle.setText(titleText);
        mTextViewAuthor.setText(authorText);

        if (mBook.publication_date == 0) {
            mTextViewPublished.setText(null);
        } else {
            String date = BBBCalendarUtil.formatDate(mBook.publication_date);
            mTextViewPublished.setText(String.format(getString(R.string.date_of_publication), date));
        }

        mBookCover.setBook(mBook);

        if (mPrice == null) {
            getPrice();
        } else {
            setPrice(mPrice);
        }

        if (DateUtils.isFuture(mBook.publication_date)) {
            mButtonBuy.setEnabled(false);
        }

        if (mRelatedBooksFragment != null) {
            mRelatedBooksFragment.setBook(mBook);
        }
    }

    private void getPrice() {
        BBBRequest request = BBBRequestFactory.getInstance().createGetPricesRequest(mBook.isbn);
        BBBRequestManager.getInstance().executeRequest(RESPONSE_HANDLER_ID, request);
    }

    private void setPrice(BBBBookPrice price) {
        BBBUIUtils.setPriceText(price, getResources(), mTextViewPrice, mTextViewPriceOriginal, mClubcardPoints, mImageViewClubcard, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    public void buyNowPressed(View view) {
        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ENDOF_SAMPLE, AnalyticsHelper.GA_EVENT_CLICK_BUY_BUTTON, AnalyticsHelper.GA_LABEL_END_OF_SAMPLE_SCREEN, null);
        ShopItem item = new ShopItem(mBook);
        item.price = this.mPrice;
        AnalyticsHelper.getInstance().sendAddToCart(AnalyticsHelper.EE_SCREEN_NAME_END_OF_SAMPLE, item);
        PurchaseController.getInstance().buyPressed(item);
    }

    public void shopMoreBooksPressed(View view) {
        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ENDOF_SAMPLE, AnalyticsHelper.GA_EVENT_SHOP_MORE_BOOKS, mBook.isbn, null);
        Intent intent = new Intent(this, ShopActivity.class);
        startActivity(intent);
    }

    private BBBBasicResponseHandler<BBBBookPriceList> responseHandler = new BBBBasicResponseHandler<BBBBookPriceList>() {

        public void receivedData(BBBResponse response, BBBBookPriceList list) {
            if (list != null && list.items != null && list.items.length > 0) {
                mPrice = list.items[0];
                setPrice(mPrice);
            }
        }

        public void receivedError(BBBResponse response) {
        }
    };
}