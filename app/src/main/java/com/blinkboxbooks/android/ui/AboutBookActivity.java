// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.api.model.BBBBookPriceList;
import com.blinkboxbooks.android.api.model.BBBSynopsis;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.dialog.GenericDialogFragment;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.BBBUIUtils;
import com.blinkboxbooks.android.util.StringUtils;
import com.blinkboxbooks.android.widget.BookCover;

import java.net.HttpURLConnection;

/**
 * Activity shows a books title/author/publish date and cover image. Also downloads and displays the description and related books.
 */
public class AboutBookActivity extends BaseActivity {

    public static final String PARAM_BOOK = "book";
    private static final String REQUEST_ID = AboutBookActivity.class.getSimpleName();
    private static final String PRICE_REQUEST_ID = "price_request";
    private static final String ERROR_DIALOG_TAG = "error_dialog";

    private TextView mTextViewTitle;
    private TextView mTextViewAuthor;
    private TextView mTextViewPublished;
    private TextView mTextViewDescription;
    private TextView mTextViewPurchaseDate;
    private TextView mTextViewPrice;
    private TextView mTextViewPriceOriginal;
    private TextView mTextViewClubcardPoints;
    private ImageView mClubcardImageView;
    private Button mButtonBuy;
    private ViewGroup mPurchasePanel;
    private ProgressBar mProgressBar;

    private BookCover mBookCover;
    private Book mBook;

    private RelatedBooksFragment mRelatedBooksFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextViewTitle = (TextView) findViewById(R.id.textview_title);
        mTextViewAuthor = (TextView) findViewById(R.id.textview_author);
        mTextViewPublished = (TextView) findViewById(R.id.textview_published);
        mTextViewDescription = (TextView) findViewById(R.id.textview_description);
        mTextViewPurchaseDate = (TextView) findViewById(R.id.textview_purchase_date);
        mTextViewPrice = (TextView) findViewById(R.id.textview_price);
        mTextViewPriceOriginal = (TextView) findViewById(R.id.textview_price_original);
        mTextViewClubcardPoints = (TextView) findViewById(R.id.textview_clubcard_points);
        mPurchasePanel = (ViewGroup) findViewById(R.id.sample_book_purchase_panel);
        mClubcardImageView = (ImageView) findViewById(R.id.clubcard_logo);
        mButtonBuy = (Button) findViewById(R.id.button_buy);

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

        mBookCover = (BookCover) findViewById(R.id.bookcover);

        // If it's a tablet we adjust the size of the book cover so it lines up with the first image in the "More books..." section.
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            int dimenGapMedium = getResources().getDimensionPixelOffset(R.dimen.gap_medium);
            int dimenGapSmall = getResources().getDimensionPixelOffset(R.dimen.gap_small);
            RelativeLayout.LayoutParams imageLayoutParams = new RelativeLayout.LayoutParams(LibraryController.bookCoverWidth, LibraryController.bookCoverHeight);
            imageLayoutParams.setMargins(0, 0, dimenGapMedium, 0);
            mBookCover.setLayoutParams(imageLayoutParams);

            // In portrait mode we resize the buy button to be the same size as the image
            if (BBBUIUtils.getScreenOrientation(this) == Configuration.ORIENTATION_PORTRAIT) {
                RelativeLayout.LayoutParams buttonLayoutParams = new RelativeLayout.LayoutParams(LibraryController.bookCoverWidth, ActionBar.LayoutParams.WRAP_CONTENT);
                buttonLayoutParams.setMargins(0, dimenGapSmall, dimenGapMedium, 0);
                buttonLayoutParams.addRule(RelativeLayout.BELOW, R.id.textview_price);
                mButtonBuy.setLayoutParams(buttonLayoutParams);
            }
        }

        mRelatedBooksFragment = (RelatedBooksFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_relatedbooks);
        mRelatedBooksFragment.setHasBeenViewed(true);

        mScreenName = AnalyticsHelper.GA_SCREEN_Reader_AboutThisBookScreen;
    }

    @Override
    protected void onResume() {
        super.onResume();
        PurchaseController.getInstance().setBaseActivity(this);
        BBBRequestManager.getInstance().addResponseHandler(REQUEST_ID, synopsisHandler);
        BBBRequestManager.getInstance().addResponseHandler(PRICE_REQUEST_ID, priceHandler);
        Book book = (Book) getIntent().getSerializableExtra(PARAM_BOOK);
        setBook(book);
    }

    @Override
    protected void onPause() {
        super.onPause();

        BBBRequestManager.getInstance().removeResponseHandler(REQUEST_ID);
        BBBRequestManager.getInstance().removeResponseHandler(PRICE_REQUEST_ID);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDescriptionLoadingVisible(boolean visible) {

        if (visible) {
            mProgressBar.setVisibility(View.VISIBLE);
            mTextViewDescription.setVisibility(View.GONE);
        } else {
            mProgressBar.setVisibility(View.GONE);
            mTextViewDescription.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the book to be displayed. Downloads the description if it has not already been set.
     *
     * @param book the Book you want to display
     */
    private void setBook(Book book) {
        mBook = book;

        // Some books may have a null author so in this case we just set an empty string. We also do the same for title (Just in case)
        Spanned titleText = (mBook.title != null) ? Html.fromHtml(mBook.title) : new SpannedString("");
        Spanned authorText = (mBook.author != null) ? Html.fromHtml(mBook.author) : new SpannedString("");

        mTextViewTitle.setText(titleText);
        mTextViewAuthor.setText(authorText);

        if (book.sample_book) {
            mTextViewPurchaseDate.setVisibility(View.GONE);

            // We should only show the purchase panel if the book publication date is before the current time. (In theory) this should always be the case
            // but we check here to be sure before requesting the price information.
            if(mBook.publication_date < System.currentTimeMillis()) {

                BBBRequest request = BBBRequestFactory.getInstance().createGetPricesRequest(mBook.isbn);
                BBBRequestManager.getInstance().executeRequest(PRICE_REQUEST_ID, request);

                mButtonBuy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_ABOUT_THIS_BOOK, AnalyticsHelper.GA_EVENT_BUY_FULL_BOOK, mBook.isbn, null);
                        ShopItem shopItem = new ShopItem(mBook);
                        PurchaseController.getInstance().buyPressed(shopItem);
                    }
                });
            }
        } else {
            mTextViewPurchaseDate.setVisibility(View.VISIBLE);
            final String purchaseDate = BBBCalendarUtil.formatDate(book.purchase_date);
            final String purchaseDateString = getString(R.string.you_purchased_this_book_about, purchaseDate);

            mTextViewPurchaseDate.setText(purchaseDateString);
        }

        if (mBook.publication_date == 0) {
            mTextViewPublished.setVisibility(View.GONE);
        } else {
            String date = BBBCalendarUtil.formatDate(mBook.publication_date);
            mTextViewPublished.setText(String.format(getString(R.string.date_of_publication), date));
        }

        mBookCover.setBook(mBook);

        if (mBook.description == null || mBook.description.length() == 0) {
            BBBRequest request = BBBRequestFactory.getInstance().createGetSynopsisRequest(mBook.isbn);
            BBBRequestManager.getInstance().executeRequest(REQUEST_ID, request);
            showDescriptionLoadingVisible(true);
        } else {
            setHtmlFormattedDescription(mBook.description);
            showDescriptionLoadingVisible(false);
        }

        mRelatedBooksFragment.setBook(book);
    }

    private void setHtmlFormattedDescription(String htmlDescription) {
        // The HTML </p> tag ends up appending new lines and messing with our padding so we perform a little trimming to solve this
        Spanned spanned = Html.fromHtml(htmlDescription);
        CharSequence trimmed = StringUtils.trimAllWhiteSpace(spanned, 0, spanned.length());
        mTextViewDescription.setText(trimmed);
    }

    /*
     * Calculates the number of lines that can fit into the given height in pixels
     */
    private int calculateNumberOfLines(TextView textView, int height) {
        Rect bounds = new Rect();

        int totalLines = textView.getLineCount();

        for (int i = 0; i < totalLines; i++) {
            textView.getLineBounds(i, bounds);

            if (bounds.bottom > height) {
                return i;
            }
        }

        return totalLines;
    }

    private BBBBasicResponseHandler<BBBSynopsis> synopsisHandler = new BBBBasicResponseHandler<BBBSynopsis>() {

        public void receivedData(BBBResponse response, BBBSynopsis synopsis) {
            showDescriptionLoadingVisible(false);

            if (synopsis != null) {
                Uri uri = Books.buildBookIdUri(mBook.id);
                BookHelper.updateBookDescription(uri, synopsis.text);

                setHtmlFormattedDescription(synopsis.text);
            } else {
                showErrorDialog(getString(R.string.title_error), getString(R.string.error_server_message));
            }
        }

        public void receivedError(BBBResponse response) {
            showDescriptionLoadingVisible(false);

            if (response.getResponseCode() == BBBApiConstants.ERROR_CONNECTION_FAILED) {
                showErrorDialog(getString(R.string.title_device_offline), getString(R.string.about_page_no_internet));
            } else {
                showErrorDialog(getString(R.string.title_error), getString(R.string.error_server_message));
            }
        }
    };

    private BBBBasicResponseHandler<BBBBookPriceList> priceHandler = new BBBBasicResponseHandler<BBBBookPriceList>() {

        public void receivedData(BBBResponse response, BBBBookPriceList priceList) {

            if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {

                // If we don't have a price for the item then do nothing as we just won't display the purchase panel
                if (priceList.items != null && priceList.items.length > 0) {
                    BBBBookPrice price = priceList.items[0];

                    if (price.currency != null) {
                        BBBUIUtils.setPriceText(price, getResources(), mTextViewPrice, mTextViewPriceOriginal, mTextViewClubcardPoints, mClubcardImageView, true);
                        mPurchasePanel.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        public void receivedError(BBBResponse response) {
            // Just ignore the error as the purchase panel will not be visible in any case.
        }
    };

    private void showErrorDialog(String title, String errorMessage) {
        GenericDialogFragment.newInstance(title, errorMessage, getString(R.string.button_retry), getString(R.string.button_cancel), null,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Retry all async requests that could of failed if the internet connection was not available
                        BBBRequest request = BBBRequestFactory.getInstance().createGetSynopsisRequest(mBook.isbn);
                        BBBRequestManager.getInstance().executeRequest(REQUEST_ID, request);
                        mRelatedBooksFragment.setBook(mBook);
                        mBookCover.setBook(mBook);
                    }
                }, null, null, null).show(getSupportFragmentManager(), ERROR_DIALOG_TAG);
    }
}