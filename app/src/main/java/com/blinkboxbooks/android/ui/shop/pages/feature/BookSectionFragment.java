// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.pages.feature;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.loader.CatalogueLoader;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.ShopHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.ui.shop.ImpressionReporterFragment;
import com.blinkboxbooks.android.ui.shop.ShopActivity;
import com.blinkboxbooks.android.ui.shop.SortOption;
import com.blinkboxbooks.android.ui.shop.preview.ShopItemFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBUIUtils;
import com.blinkboxbooks.android.widget.ShopItemView;

import java.util.ArrayList;

/**
 * Fragment shows a subset of books with a title and an optional button to show the full list
 */
public class BookSectionFragment extends ImpressionReporterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID_LOAD_BOOKS = 1;
    private static final int LOADER_ID_LOAD_PROMOTION = 2;

    private static final String ARG_TITLE = "title";
    private static final String ARG_ROWS = "rows";
    private static final String ARG_URI = "uri";
    private static final String ARG_SHOW_POSITION = "show_position";
    private static final String ARG_SHOW_FULL_LIST = "show_full_list";
    private static final String ARG_SORT_OPTION = "sortOption";
    private static final String ARG_PROMOTION_ID = "promotionId";

    private static final int MAX_NUM_PROMOTIONAL_BOOKS_TO_REQUEST = 10;

    /**
     * Create a new BookSectionFragment and loads books directly from the supplied Uri
     *
     * @param title
     * @param rows
     * @param uri
     * @param showPositions
     * @param showFullListButton
     * @param sortOption
     * @return
     */
    public static BookSectionFragment newInstance(String title, int rows, Uri uri, boolean showPositions, boolean showFullListButton, SortOption sortOption) {
        BookSectionFragment fragment = new BookSectionFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_ROWS, rows);
        args.putParcelable(ARG_URI, uri);
        args.putBoolean(ARG_SHOW_POSITION, showPositions);
        args.putBoolean(ARG_SHOW_FULL_LIST, showFullListButton);
        args.putSerializable(ARG_SORT_OPTION, sortOption);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Create a new BookSectionFragment and loads the Promotion with the supplied id and then loads books from the Promotion
     *
     * @param promotionId
     * @param rows
     * @param showPositions
     * @param showFullListButton
     * @param sortOption
     * @return
     */
    public static BookSectionFragment newInstance(int promotionId, int rows, boolean showPositions, boolean showFullListButton, SortOption sortOption) {
        BookSectionFragment fragment = new BookSectionFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_ROWS, rows);
        args.putInt(ARG_PROMOTION_ID, promotionId);
        args.putBoolean(ARG_SHOW_POSITION, showPositions);
        args.putBoolean(ARG_SHOW_FULL_LIST, showFullListButton);
        args.putSerializable(ARG_SORT_OPTION, sortOption);
        fragment.setArguments(args);

        return fragment;
    }

    private Uri mUri;
    private int mRows;
    private int mColumns;
    private Integer mPromotionId;
    private String mTitle;
    private boolean mShowPositions;
    private boolean mShowFullList;

    private ArrayList<ShopItemView> mShopViews;
    private SortOption mSortOption;

    private TextView mTextViewTitle;
    private ProgressBar mProgressBar;

    // An observer for the users library that will cause us to reload data when a change is observed.
    private ContentObserver mLibraryObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if(mUri != null) {
                getLoaderManager().restartLoader(LOADER_ID_LOAD_BOOKS, null, BookSectionFragment.this);
            } else if(mPromotionId != null) {
                getLoaderManager().restartLoader(LOADER_ID_LOAD_PROMOTION, null, BookSectionFragment.this);
            }
        }
    };

    public BookSectionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if(args.containsKey(ARG_URI)) {
            mUri = args.getParcelable(ARG_URI);
        }

        if(args.containsKey(ARG_PROMOTION_ID)) {
            mPromotionId = args.getInt(ARG_PROMOTION_ID);
        }

        mColumns = getResources().getInteger(R.integer.shop_number_columns);
        mRows = args.getInt(ARG_ROWS);

        mTitle = args.getString(ARG_TITLE);
        mShowPositions = args.getBoolean(ARG_SHOW_POSITION);
        mSortOption = (SortOption)args.getSerializable(ARG_SORT_OPTION);
        mShowFullList = args.getBoolean(ARG_SHOW_FULL_LIST);

        setScreenName(AnalyticsHelper.GA_SCREEN_Shop_Featured);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //TODO if we already have loaded data we should just get this from savedInstanceState instead of creating a loader on every rotate/Activity restore
        if(mUri != null) {
            getLoaderManager().initLoader(LOADER_ID_LOAD_BOOKS, null, this);
        } else if(mPromotionId != null) {
            getLoaderManager().initLoader(LOADER_ID_LOAD_PROMOTION, null, this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(mShopViews != null) {
            mShopViews.clear();
            mShopViews = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Add an observer for the currently signed in user's book library updating
        String userId = AccountController.getInstance().getUserId();
        String[] projection = {BBBContract.BooksColumns.BOOK_ISBN};
        Uri uri = BBBContract.Books.buildBookAccountUriAll(userId);
        getActivity().getContentResolver().registerContentObserver(uri, true, mLibraryObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().getContentResolver().unregisterContentObserver(mLibraryObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_book_section, container, false);

        mShopViews = new ArrayList<ShopItemView>(mColumns * mRows);

        mTextViewTitle = (TextView)view.findViewById(R.id.textview_title);
        mTextViewTitle.setText(mTitle);

        mProgressBar = (ProgressBar)view.findViewById(R.id.progress_loading);

        LinearLayout rowContainer = (LinearLayout)view.findViewById(R.id.container);
        View rowItem;

        for(int i=0; i<mRows; i++) {
            rowItem = BBBUIUtils.createShopItemRow(getActivity(), mColumns, mShopViews, BBBUIUtils.getScreenWidth(getActivity()), getScreenName());
            rowContainer.addView(rowItem);
        }

        View seeFullView = view.findViewById(R.id.container_seefull);

        if(mShowFullList) {
            seeFullView.setVisibility(View.VISIBLE);

            seeFullView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Activity activity = getActivity();

                    if(activity instanceof ShopActivity) {
                        ((ShopActivity)activity).showTab(mTitle);
                    }
                }
            });
        } else {
            seeFullView.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CatalogueLoader catalogueLoader = null;

        if(id == LOADER_ID_LOAD_BOOKS) {
            catalogueLoader = new CatalogueLoader(getActivity(), mUri, null, null, null, mSortOption);
        } else if(id == LOADER_ID_LOAD_PROMOTION) {
            catalogueLoader = new CatalogueLoader(getActivity(), CatalogueContract.Promotions.getPromotionWithLocationUri(mPromotionId), null, null, null, mSortOption);
        }

        if(catalogueLoader != null) {
            catalogueLoader.setErrorListener(mErrorListener);
        }

        mProgressBar.setVisibility(View.VISIBLE);

        return catalogueLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // If this completes after calling destroy view then mShopViews will be null, so in this case we just bail out here
        if (mShopViews == null) {
            return;
        }

        int id = loader.getId();

        if(id == LOADER_ID_LOAD_BOOKS) {

            mProgressBar.setVisibility(View.GONE);

            if (data != null) {
                int viewCount = mShopViews.size();

                ShopItemView shopItemView;
                ShopItem shopItem;
                data.moveToFirst();
                int i = 0;

                do {
                    shopItem = ShopHelper.getShopItem(data);

                    if (mShowPositions) {
                        shopItem.position = i + 1;
                    } else {
                        shopItem.position = null;
                    }

                    shopItemView = mShopViews.get(i++);

                    reportShopItemImpression(i, shopItem);
                    shopItemView.setData(shopItem, false);
                    shopItemView.setOnClickListener(new ShopItemFragment.ShopItemOnClickListener(getActivity(), shopItem, getScreenName()));

                    data.moveToNext();
                } while (!data.isAfterLast() && i < viewCount);
            }

            // Destroy the books loader to prevent multiple loaders if the device is rotated
            getLoaderManager().destroyLoader(LOADER_ID_LOAD_BOOKS);

        } else if(id == LOADER_ID_LOAD_PROMOTION) {

            if (data != null) {
                data.moveToFirst();

                String displayName = data.getString(data.getColumnIndex(CatalogueContract.Promotions.TITLE));
                int promotionId = data.getInt(data.getColumnIndex(CatalogueContract.Promotions._ID));

                mTextViewTitle.setText(displayName);

                mUri = CatalogueContract.Books.getBookForPromotion(promotionId, true, MAX_NUM_PROMOTIONAL_BOOKS_TO_REQUEST);
                getLoaderManager().initLoader(LOADER_ID_LOAD_BOOKS, null, this);
            }

            // Destroy the books loader to prevent multiple loaders if the device is rotated
            getLoaderManager().destroyLoader(LOADER_ID_LOAD_PROMOTION);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

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
            reportErrorToParent(R.string.no_books);
        }
    };
}