// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.preview;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.loader.CatalogueLoader;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.ShopHelper;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.ui.shop.BooksFragment;
import com.blinkboxbooks.android.ui.shop.ImpressionReporterFragment;
import com.blinkboxbooks.android.ui.shop.SearchActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBImageLoader;
import com.blinkboxbooks.android.util.BBBUIUtils;
import com.blinkboxbooks.android.util.StringUtils;
import com.blinkboxbooks.android.widget.AboutBookItem;

import java.util.ArrayList;

/**
 * Fragment for showing information about an author
 */
public class AuthorFragment extends ImpressionReporterFragment {

    private static final int LOADER_ID_AUTHOR = 1;
    private static final int LOADER_ID_BOOKS_FOR_AUTHOR = 2;

    private static final String ARG_AUTHOR_ID = "AUTHOR_ID";

    public static final float AUTHOR_IMAGE_WIDTH_HEIGHT_RATIO = 1.4f;

    /**
     * Creates a new instance of the fragment
     *
     * @param authorId
     * @return
     */
    public static AuthorFragment newInstance(String authorId) {
        AuthorFragment fragment = new AuthorFragment();

        Bundle args = new Bundle();
        args.putString(ARG_AUTHOR_ID, authorId);
        fragment.setArguments(args);

        return fragment;
    }

    private TextView mTextViewAuthorName;
    private TextView mTextViewAuthorBiography;

    private LinearLayout mBookContainer;

    private ImageView mImageView;

    private String mAuthorId;
    private String mAuthorName;

    private ArrayList<AboutBookItem> mBookItems;
    private View mViewOtherBooks;
    private boolean mIsTablet;

    public AuthorFragment() {
        setScreenName(AnalyticsHelper.GA_SCREEN_Shop_Author);
    }

    public void setAuthorId(String authorId) {
        mAuthorId = authorId;

        if (mAuthorId != null) {
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(LOADER_ID_AUTHOR, null, mLoaderCallbacksAuthor);
            loaderManager.initLoader(LOADER_ID_BOOKS_FOR_AUTHOR, null, mLoaderCallbacksBooksForAuthor);
        } else {
            mTextViewAuthorName.setText(R.string.no_author_information);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            mAuthorId = getArguments().getString(ARG_AUTHOR_ID);
        } else {
            mAuthorId = savedInstanceState.getString(ARG_AUTHOR_ID);
        }

        mIsTablet = getResources().getBoolean(R.bool.isTablet);

        View view = inflater.inflate(R.layout.fragment_shop_author, container, false);

        mTextViewAuthorName = (TextView) view.findViewById(R.id.textview_author_name);
        if (mIsTablet) {
            mTextViewAuthorBiography = (TextView) view.findViewById(R.id.textview_author_biography_tablet);
        } else {
            mTextViewAuthorBiography = (TextView) view.findViewById(R.id.textview_author_biography_phone);
        }
        mTextViewAuthorBiography.setVisibility(View.VISIBLE);

        mBookContainer = (LinearLayout) view.findViewById(R.id.bookcontainer);
        mImageView = (ImageView) view.findViewById(R.id.imageview_author);
        ScrollView scrollView = (ScrollView) view.findViewById(R.id.scrollview);

        mViewOtherBooks = view.findViewById(R.id.layout_other_books);

        int numOfColumns = getResources().getInteger(R.integer.shop_number_columns);
        mBookItems = new ArrayList<AboutBookItem>(numOfColumns);

        int aboutBookWidth = BBBUIUtils.createAboutBookRow(getActivity(), mBookContainer, numOfColumns, mBookItems, BBBUIUtils.getScreenWidth(getActivity()));

        if (mIsTablet) {
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(aboutBookWidth, (int) (aboutBookWidth * AUTHOR_IMAGE_WIDTH_HEIGHT_RATIO));
            mImageView.setLayoutParams(buttonLayoutParams);
        }

        view.findViewById(R.id.button_see_all_books).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);

                intent.putExtra(SearchActivity.ARG_VIEW_TYPE, SearchActivity.ViewType.BOOKS_FOR_AUTHOR);
                intent.putExtra(SearchActivity.ARG_ID, mAuthorId);

                if (!TextUtils.isEmpty(mAuthorName)) {
                    intent.putExtra(SearchActivity.ARG_TITLE, mAuthorName);
                }

                getActivity().startActivity(intent);
            }
        });

        if (mAuthorId != null) {
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(LOADER_ID_AUTHOR, null, mLoaderCallbacksAuthor);
            loaderManager.initLoader(LOADER_ID_BOOKS_FOR_AUTHOR, null, mLoaderCallbacksBooksForAuthor);
        } else {
            mTextViewAuthorName.setText(R.string.no_author_information);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_AUTHOR_ID, mAuthorId);
    }

    private void populateBookRow(Cursor data) {
        data.moveToFirst();

        ShopItem shopItem;
        int size = mBookItems.size();

        AboutBookItem aboutBookItem;

        for(int i=0; i<size; i++) {

            if(!data.isAfterLast()) {
                shopItem = ShopHelper.getShopItem(data);
                reportShopItemImpression(i, shopItem);
            } else {
                shopItem = null;
            }

            aboutBookItem = mBookItems.get(i);

            aboutBookItem.setBook(shopItem == null ? null : shopItem.book, false);
            aboutBookItem.setOnClickListener(createRelatedBookOnClickListener(shopItem));

            data.moveToNext();
        }
    }

    private View.OnClickListener createRelatedBookOnClickListener(final ShopItem item) {

        return new View.OnClickListener() {

            public void onClick(View v) {
                Activity activity = getActivity();

                AnalyticsHelper.getInstance().sendClickOnProduct(getScreenName(), item);

                if(activity instanceof ComponentClickedListener) {
                    ((ComponentClickedListener)activity).relatedBookClicked(item);
                }
            }
        };
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacksAuthor = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri uri = CatalogueContract.Author.getAuthorUri(mAuthorId);

            CatalogueLoader catalogueLoader = new CatalogueLoader(getActivity(), uri, null, null, null, null);
            catalogueLoader.setErrorListener(mErrorListener);

            return catalogueLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

            if(data != null) {
                data.moveToFirst();

                mAuthorName = data.getString(data.getColumnIndex(CatalogueContract.Author.DISPLAY_NAME));
                mTextViewAuthorName.setText(mAuthorName);

                String biography = data.getString(data.getColumnIndex(CatalogueContract.Author.BIOGRAPHY));

                if(!TextUtils.isEmpty(biography)) {
                    Spanned spanned = Html.fromHtml(biography);
                    CharSequence trimmed = StringUtils.trimAllWhiteSpace(spanned, 0, spanned.length());
                    mTextViewAuthorBiography.setText(trimmed);
                } else {
                    mTextViewAuthorBiography.setText(R.string.error_no_author_biography);
                }

                String imageUrl = data.getString(data.getColumnIndex(CatalogueContract.Author.IMAGE_URL));
                if(!TextUtils.isEmpty(imageUrl)) {
                    imageUrl = BBBImageLoader.injectWidthIntoCoverUrl(imageUrl, mImageView.getWidth());

                    BBBImageLoader.getInstance().get(imageUrl, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            // Keep the existing image width but allow the height to wrap content so images that are landscape don't take up loads of vertical space
                            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(mImageView.getLayoutParams().width, ActionBar.LayoutParams.WRAP_CONTENT);
                            mImageView.setLayoutParams(buttonLayoutParams);
                            mImageView.setImageBitmap(imageContainer.getBitmap());
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                        }

                    }, Request.Priority.HIGH);
                }
            } else {
                mTextViewAuthorBiography.setText(R.string.error_no_internet_author_biography);
            }

            // Destroy the loader on completion to avoid multiple loaders firing on device rotate
            getLoaderManager().destroyLoader(LOADER_ID_AUTHOR);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacksBooksForAuthor = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri uri = CatalogueContract.Books.getBooksForAuthorIdUri(mAuthorId, false, BooksFragment.DEFAULT_PAGE_SIZE, 0);

            CatalogueLoader catalogueLoader = new CatalogueLoader(getActivity(), uri, null, null, null, null);
            catalogueLoader.setErrorListener(mErrorListener);

            return catalogueLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

            if(data != null) {
                mViewOtherBooks.setVisibility(View.VISIBLE);
                populateBookRow(data);
            } else {
                mViewOtherBooks.setVisibility(View.GONE);
            }

            // Destroy the loader on completion to avoid multiple loaders firing on device rotate
            getLoaderManager().destroyLoader(LOADER_ID_BOOKS_FOR_AUTHOR);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    private final CatalogueLoader.ErrorListener mErrorListener = new CatalogueLoader.ErrorListener() {

        @Override
        public void internalServerError() {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        mViewOtherBooks.setVisibility(View.GONE);
                    }
                }
            };

            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        }

        @Override
        public void noNetworkError() {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        mViewOtherBooks.setVisibility(View.GONE);
                    }
                }
            };

            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        }

        @Override
        public void noResults() {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        mViewOtherBooks.setVisibility(View.GONE);
                    }
                }
            };

            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        }
    };
}
