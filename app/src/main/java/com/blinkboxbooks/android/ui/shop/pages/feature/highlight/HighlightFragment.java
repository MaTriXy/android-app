// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.pages.feature.highlight;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBSynopsis;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.loader.CatalogueLoader;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.ShopHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.shop.ImpressionReporterFragment;
import com.blinkboxbooks.android.ui.shop.SortOption;
import com.blinkboxbooks.android.ui.shop.pages.feature.SectionErrorHandler;
import com.blinkboxbooks.android.ui.shop.preview.PreviewActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.StringUtils;
import com.blinkboxbooks.android.widget.BBBButton;
import com.blinkboxbooks.android.widget.BookCover;

import java.util.ArrayList;

/**
 * The highlight fragment reveals a carousel of books and information regarding the
 * book in focus. The user can elect to scroll through books, view a book preview,
 * view the author information and buy the book
 */
public class HighlightFragment extends ImpressionReporterFragment implements LoaderManager.LoaderCallbacks<Cursor>, FancyCoverFlow.OnSingleTapListener {

    private static final int LOADER_ID_LOAD_HIGHLIGHTS = 13;
    private static final int LOADER_ID_LIBRARY_CHANGE = 14;
    private static final float SCALE_UP_BUTTON = 1.5f;
    private static final String REQUEST_ID = HighlightFragment.class.getSimpleName();
    private static final String ARG_URI = "uri";

    /**
     * Create a new highlights fragment fragment
     *
     * @param uri the uri to request 'highlights' books via the catalogue provider
     * @return an instance of the highlights fragment
     */
    public static HighlightFragment newInstance(Uri uri) {
        HighlightFragment fragment = new HighlightFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        fragment.setArguments(args);

        return fragment;
    }

    private Uri mUri;
    private FancyCoverFlow mCarousel;
    private CarouselAdapter mCarouselAdapter;
    private ArrayList<ShopItem> mShopItems;
    private ShopItem mCurrentShopItem;
    private int mCarouselPosition;

    private TextView mTitleText;
    private TextView mAuthorText;
    private TextView mSynopsis;
    private TextView mDiscountText;
    private TextView mPriceText;
    private TextView mClubcardText;
    private View mLeftSelection;
    private View mRightSelection;
    private BBBButton mBuyButton;
    private View mBookInformation;
    private ProgressBar mProgressBar;

    // An observer for the users library that will cause us to reload data when a change is observed.
    private ContentObserver mLibraryObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            getLoaderManager().restartLoader(LOADER_ID_LIBRARY_CHANGE, null, HighlightFragment.this);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle args = getArguments();
        if (args.containsKey(ARG_URI)) {
            mUri = args.getParcelable(ARG_URI);
        }
        setScreenName(AnalyticsHelper.GA_SCREEN_Shop_Highlights);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mUri != null) {
            getLoaderManager().initLoader(LOADER_ID_LOAD_HIGHLIGHTS, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflate, ViewGroup contains, Bundle savedInstanceState) {
        View view = inflate.inflate(R.layout.fragment_shop_highlights, contains, false);

        mTitleText = (TextView) view.findViewById(R.id.title_text);
        mAuthorText = (TextView) view.findViewById(R.id.author_name);
        mSynopsis = (TextView) view.findViewById(R.id.description);
        mDiscountText = (TextView) view.findViewById(R.id.discount_text);
        mPriceText = (TextView) view.findViewById(R.id.price);
        mClubcardText = (TextView) view.findViewById(R.id.clubcard_points_text);
        mBuyButton = (BBBButton) view.findViewById(R.id.button_buy);
        mCarousel = (FancyCoverFlow) view.findViewById(R.id.carousel);
        mBookInformation = view.findViewById(R.id.book_information);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        mLeftSelection = view.findViewById(R.id.left_selection);
        mRightSelection = view.findViewById(R.id.right_selection);

        mLeftSelection.setOnTouchListener(new View.OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                if (mCarousel != null) {
                    if (mCarousel.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null)) {
                        v.setScaleX(SCALE_UP_BUTTON);
                        v.setScaleY(SCALE_UP_BUTTON);
                        v.animate().scaleY(1.0f).scaleX(1.0f).setInterpolator(new AnticipateInterpolator());
                    }
                }
                return false;
            }
        });

        mRightSelection.setOnTouchListener(new View.OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                if (mCarousel != null) {
                    if (mCarousel.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null)) {
                        v.setScaleX(SCALE_UP_BUTTON);
                        v.setScaleY(SCALE_UP_BUTTON);
                        v.animate().scaleY(1.0f).scaleX(1.0f).setInterpolator(new AnticipateInterpolator());
                    }
                }
                return false;
            }
        });

        mAuthorText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentShopItem != null) {
                    startPreviewActivity(true);
                }
            }
        });

        if (mSynopsis != null) {
            mSynopsis.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreviewActivity(false);
                }
            });
        }

        showLoading(true);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mCarouselPosition = mCarousel.getSelectedItemPosition();
        BBBRequestManager.getInstance().removeResponseHandler(REQUEST_ID);

        getActivity().getContentResolver().unregisterContentObserver(mLibraryObserver);
    }

    @Override
    public void onResume() {
        super.onResume();

        BBBRequestManager.getInstance().addResponseHandler(REQUEST_ID, synopsisHandler);

        //Resume to previous carousel position if known
        if (mCarousel != null && mCarouselPosition != -1) {
            mCarousel.setSelection(mCarouselPosition);
        }

        if (mCurrentShopItem != null) {
            setBookInformation(mCurrentShopItem);
        }


        // Add an observer for the currently signed in user's book library updating
        String userId = AccountController.getInstance().getUserId();
        String[] projection = {BBBContract.BooksColumns.BOOK_ISBN};
        Uri uri = BBBContract.Books.buildBookAccountUriAll(userId);
        getActivity().getContentResolver().registerContentObserver(uri, true, mLibraryObserver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_LIBRARY_CHANGE:
            case LOADER_ID_LOAD_HIGHLIGHTS: {
                CatalogueLoader catalogueLoader = new CatalogueLoader(getActivity(), mUri, null, null, null, SortOption.SEQUENTIAL);
                catalogueLoader.setErrorListener(mErrorListener);
                return catalogueLoader;
            }
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_ID_LOAD_HIGHLIGHTS: {

                if (data != null) {
                    data.moveToFirst();

                    int bookCount = data.getCount();

                    ShopItem shopItem;
                    mShopItems = new ArrayList<ShopItem>(bookCount);

                    for (int i = 0; i < bookCount; i++) {
                        shopItem = ShopHelper.getShopItem(data);
                        mShopItems.add(shopItem);

                        // For simplicity we just report all carousel items as being impressions
                        reportShopItemImpression(i, shopItem);
                        data.moveToNext();
                    }

                    int spacingPixels = Math.round(getResources().getDimensionPixelSize(R.dimen.coverflow_spacing));
                    mBookInformation.setVisibility(View.VISIBLE);

                    mCarousel.setListener(this);
                    mCarouselAdapter = new CarouselAdapter();
                    mCarousel.setAdapter(mCarouselAdapter);
                    mCarousel.setSpacing(spacingPixels);
                    mCarousel.setActionDistance(mCarouselAdapter.getBookWidth() * 12);

                    mCarouselPosition = mCarouselAdapter.getCount() / 2;
                    mCarousel.setSelection(mCarouselPosition);

                    mCarousel.setCallbackDuringFling(false);
                    mCarousel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            setBookInformation((ShopItem) mCarousel.getSelectedItem());
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }
                getLoaderManager().destroyLoader(LOADER_ID_LOAD_HIGHLIGHTS);
            }
            break;
            case LOADER_ID_LIBRARY_CHANGE: {
                if ((data != null) && (mShopItems != null)) {
                    data.moveToFirst();

                    final int bookCount = data.getCount();
                    mShopItems.clear();

                    for (int i = 0; i < bookCount; i++) {
                        ShopItem shopItem = ShopHelper.getShopItem(data);
                        mShopItems.add(shopItem);

                        if ((mCurrentShopItem != null) && (mCurrentShopItem.book.isbn.equals(shopItem.book.isbn))) {
                            mCurrentShopItem = shopItem;
                            setBookInformation(mCurrentShopItem);
                        }
                        data.moveToNext();
                    }

                    mCarouselAdapter.notifyDataSetChanged();
                }
                getLoaderManager().destroyLoader(LOADER_ID_LIBRARY_CHANGE);
            }
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void setBookInformation(final ShopItem shopItem) {
        showLoading(false);

        if (getActivity() == null) {
            return;
        }

        mCurrentShopItem = shopItem;

        mTitleText.setText(shopItem.book.title);

        if (mCurrentShopItem.book.author != null) {
            getActivity().findViewById(R.id.by).setVisibility(View.VISIBLE);
            mAuthorText.setVisibility(View.VISIBLE);
            mAuthorText.setText(mCurrentShopItem.book.author);
        } else {
            getActivity().findViewById(R.id.by).setVisibility(View.INVISIBLE);
            mAuthorText.setVisibility(View.INVISIBLE);
        }

        if (mSynopsis != null) {
            mSynopsis.setText(mCurrentShopItem.book.description);
        }

        mDiscountText.setText(StringUtils.formatDiscount(getResources(), R.string.shop_discount_2_lines, mCurrentShopItem.price.currency, mCurrentShopItem.price.price, mCurrentShopItem.price.discountPrice));

        mPriceText.setText(StringUtils.getCurrencySymbol(mCurrentShopItem.price.currency) + String.format("%.2f", mCurrentShopItem.price.getPriceToPay()));
        mClubcardText.setText(Html.fromHtml(String.format(getString(R.string.shop_s_clubcard_points), mCurrentShopItem.price.clubcardPointsAward)));
        SpannableString spannableString = new SpannableString(mClubcardText.getText().toString());
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, 1, 0);
        mClubcardText.setText(spannableString);

        if (mCurrentShopItem.book.purchase_date == 0L) {
            mBuyButton.setEnabled(true);

            mBuyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (NetworkUtils.hasInternetConnectivity(getActivity())) {
                        PurchaseController.getInstance().buyPressed(shopItem);
                        AnalyticsHelper.getInstance().sendAddToCart(getScreenName(), shopItem);
                    } else {
                        ((BaseActivity) getActivity()).showMessage(null, getString(R.string.error_no_internet_buy));
                    }
                }
            });

        } else {
            mBuyButton.setEnabled(false);
        }

        if (mCurrentShopItem.book.description == null) {
            BBBRequest request = BBBRequestFactory.getInstance().createGetSynopsisRequest(mCurrentShopItem.book.isbn);
            BBBRequestManager.getInstance().executeRequest(REQUEST_ID, request);
        } else {
            setSynopsisText(mCurrentShopItem.book.description);
        }
    }

    private void setSynopsisText(CharSequence synopsis) {
        // Since the Synopsis can be set from an async request its possible to get into a state where the
        // shop item is null (ie. on rotation while the async request is in progress).
        if (mCurrentShopItem != null) {
            mCurrentShopItem.book.description = synopsis.toString().replace('\n', ' ');
            if (mSynopsis != null) {
                mSynopsis.setText(mCurrentShopItem.book.description);
            }
        }
    }

    /**
     * Respond appropriately to touch event on carousel.
     * If book in focus is tapped, bring preview fragment up. Otherwise, switch to book selected (by touch)
     *
     * @param e
     */
    public void onSingleTap(MotionEvent e) {
        int x = (int) e.getRawX();
        int bucketIndex = x / (getScreenWidth() / mCarouselAdapter.getVisibleBooks());
        int positionsToMove = bucketIndex - (mCarouselAdapter.getVisibleBooks() / 2);

        if (positionsToMove < 0) {
            mCarousel.setSelection(Math.max(mCarousel.getSelectedItemPosition() + positionsToMove + 1, 0));
            mCarousel.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
        } else if (positionsToMove > 0) {
            mCarousel.setSelection(Math.min(mCarousel.getSelectedItemPosition() + positionsToMove - 1, mShopItems.size() * CarouselAdapter.CAROUSEL_INFINITE_FUDGE_MULTIPLIER - 1));
            mCarousel.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
        } else {

            // Force an update of the current shop item. It's important to do this here because we don't get notifications until a
            // fling fully completes therefore if the user clicks while a fling has not finished the wrong book preview can be displayed
            mCurrentShopItem = (ShopItem) mCarousel.getSelectedItem();

            if (mCurrentShopItem != null) {
                AnalyticsHelper.getInstance().sendClickOnProduct(getScreenName(), mCurrentShopItem);
            }
            startPreviewActivity(false);
        }
    }

    //boolean parameter determines whether we show the author tab in the preview activity first
    private void startPreviewActivity(boolean showAuthor) {
        if (mCurrentShopItem != null) {
            Intent intent = new Intent(getActivity(), PreviewActivity.class);
            intent.putExtra(PreviewActivity.ARG_START_ON_AUTHOR, showAuthor);
            intent.putExtra(PreviewActivity.ARG_SHOP_ITEM, mCurrentShopItem);
            getActivity().startActivity(intent);
        }
    }

    private void showLoading(boolean visible) {
        if (visible) {
            mCarousel.setVisibility(View.INVISIBLE);
            mBookInformation.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mCarousel.setVisibility(View.VISIBLE);
            mBookInformation.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private int getScreenWidth() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    private void reportErrorToParent(final int errorStringResource) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Fragment parentFragment = getParentFragment();
                if (parentFragment != null && parentFragment instanceof SectionErrorHandler) {
                    ((SectionErrorHandler) parentFragment).reportError(errorStringResource);
                }
            }
        };

        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(runnable);
        }
    }

    private final CatalogueLoader.ErrorListener mErrorListener = new CatalogueLoader.ErrorListener() {

        @Override
        public void internalServerError() {
            reportErrorToParent(R.string.error_server_message);
        }

        @Override
        public void noNetworkError() {
            reportErrorToParent(R.string.error_no_network_shop);
        }

        @Override
        public void noResults() {
            reportErrorToParent(R.string.error_no_related_books);
        }
    };

    private class CarouselAdapter extends FancyCoverFlowAdapter {

        // In order to fake an infinite carousel (without tons of code) we simply pretend there are 10000 lots of the books.
        // By starting in the middle the user will have to scroll for a LONG time to ever hit either end, thus the illusion of
        // an infinite carousel is achieved
        public static final int CAROUSEL_INFINITE_FUDGE_MULTIPLIER = 10000;
        private static final double BOOK_HEIGHT_WIDTH_RATIO = 1.48f;

        private final int mWidth;
        private final int mHeight;
        private final int mVisibleBooks;

        public CarouselAdapter() {
            mVisibleBooks = getActivity().getResources().getInteger(R.integer.visible_carousel_books);
            mHeight = getResources().getDimensionPixelSize(R.dimen.carousel_height);
            mWidth = (int) (mHeight / BOOK_HEIGHT_WIDTH_RATIO);
        }

        @Override
        public int getCount() {
            return mShopItems.size() * CAROUSEL_INFINITE_FUDGE_MULTIPLIER;
        }

        @Override
        public ShopItem getItem(int i) {
            return mShopItems.get(getCarouselIndex(i % mShopItems.size()));
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getCoverFlowItem(int position, View reuseableView, ViewGroup viewGroup) {
            BookCover bookCover = null;
            ShopItem shopItem = getItem(position % mShopItems.size());

            if (reuseableView != null) {
                bookCover = (BookCover) reuseableView;
            } else {
                bookCover = new BookCover(viewGroup.getContext());
                bookCover.setImageFadeIn(false);
                bookCover.setLayoutParams(new FancyCoverFlow.LayoutParams(mWidth, mHeight));
                bookCover.setBookImageSize(mWidth, mHeight);
            }
            bookCover.setBook(shopItem.book);

            return bookCover;
        }

        /**
         * The carousel persists the highlights' books ranking by setting the
         * number 1 ranked book to the centre of the carousel and then alternating
         * left and right from the center, setting the books so as to keep the
         * higher ranked books closer to the centre of the carousel i.e. for a 5 book carousel,
         * the books will appear (number indicates rank): [5, 3, 1, 2, 4]
         */
        private int getCarouselIndex(int i) {
            final int bookCount = mShopItems.size();
            return (float) i < (bookCount / 2.0f) ? (bookCount - (2 * i) - 1) : (2 * i) - bookCount;
        }

        /**
         * Returns the specified number of visible books for current layout
         *
         * @return number of books visible on screen
         */
        private int getVisibleBooks() {
            return mVisibleBooks;
        }

        private int getBookWidth() {
            return mWidth;
        }
    }

    private BBBBasicResponseHandler<BBBSynopsis> synopsisHandler = new BBBBasicResponseHandler<BBBSynopsis>() {
        @Override
        public void receivedData(BBBResponse response, BBBSynopsis synopsis) {

            if (synopsis != null && mCurrentShopItem != null) {
                // Because we might fire multiple simultaneous async requests its essential that we make sure that this
                // response is for the currently selected shop item. If its not we just ignore the response.
                if (synopsis.id.equals(mCurrentShopItem.book.isbn)) {
                    setSynopsisText(Html.fromHtml(synopsis.text));
                }
            }
        }

        @Override
        public void receivedError(BBBResponse bbbResponse) {
            reportErrorToParent(R.string.error_no_network_shop);
        }
    };
}


