// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.shop.preview.PreviewActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.cache.TypefaceCache;
import com.crashlytics.android.Crashlytics;

/**
 * Sub activity for shop. Displays search results, books in category, related books.
 *
 * This activity is poorly named because it shows stuff other than search results (such as books in a category etc.)
 * but we MUST NOT change it. The HUDL2 cards are hardwired to use this activity name (It really shouldn't
 * do this, but the reality is that it does and we just have to live with it).
 */
public class SearchActivity extends BaseActivity {

    public enum ViewType {
        SEARCH,
        CATEGORY,
        RELATED_BOOKS,
        BOOKS_FOR_AUTHOR,
        BOOK_DETAIL,
        ISBN;
    }

    private static final String TAG_FRAGMENT = "fragment";
    private static final String URI_SHOW_ISBN = "bbb://app/search/isbn/";

    public static final String ARG_VIEW_TYPE = "view_type";
    public static final String ARG_ISBN = "isbn";
    public static final String ARG_ID = "id";
    public static final String ARG_TITLE = "title";
    public static final String ARG_PUT_FIRST = "put_first";
    public static final String ARG_SUGGESTION_ID = "intent_extra_data_key";

    // The server will only return a maximum of 50 results when we are searching (specifically when we request the list of book information by ids)
    private static final int DEFAULT_PAGE_SIZE_SEARCH_RESULTS = 50;

    private ViewType mViewType = ViewType.SEARCH;
    private SearchView mSearchView;

    // The screen name to use for GA
    private String mScreenName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_shop_sub);

        Intent intent = getIntent();

        // In case we come in here via a marketing deep link we want to track in Ad-X
        AnalyticsHelper.handleAdXDeepLink(this, intent);

        mViewType = getViewType(intent);
        mScreenName = getScreenName(intent);

        Uri uri = createUri(intent);

        if (uri == null) {
            finish();
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        if(savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            SortOption[] sortOptions = getSortOptions();

            BooksFragment fragment = BooksFragment.newInstance(mScreenName, uri, false, getPageSize(), true,getIntent().getIntExtra(ARG_PUT_FIRST,0), sortOptions);
            fragmentTransaction.add(R.id.container, fragment, TAG_FRAGMENT);
            fragmentTransaction.commit();
            fragment.setHasBeenViewed(true);
        } else {
            BooksFragment fragment = (BooksFragment)fragmentManager.findFragmentByTag(TAG_FRAGMENT);
            fragment.setUri(uri, getPageSize());
            fragment.setHasBeenViewed(true);
        }

        String title = intent.getStringExtra(ARG_TITLE);

        if(!TextUtils.isEmpty(title)) {
            getSupportActionBar().setTitle(title);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private ViewType getViewType(Intent intent) {
        ViewType viewType = ViewType.SEARCH;

        if(intent.hasExtra(ARG_VIEW_TYPE)) {
            viewType = (ViewType) intent.getSerializableExtra(ARG_VIEW_TYPE);
        } else {

            if (intent.hasExtra(ARG_SUGGESTION_ID)){
                viewType = ViewType.BOOKS_FOR_AUTHOR;
            }

            /**
             * If searching via voice, or from Google Now search suggestions, the user query is
             * stored in the SearchManager.QUERY or SearchManager.USER_QUERY intent extra respectively
             */
            if(intent.hasExtra(SearchManager.QUERY) || intent.hasExtra(SearchManager.USER_QUERY)) {
                viewType = ViewType.SEARCH;
            }

            final Uri uri = intent.getData();
            if (uri != null) {
                if (uri.toString().startsWith(URI_SHOW_ISBN)) {
                    viewType = ViewType.ISBN;
                }
            }
        }

        return viewType;
    }

    private String getScreenName(Intent intent) {

        switch(mViewType) {
            case CATEGORY:
                return AnalyticsHelper.GA_SCREEN_Shop_Category + intent.getStringExtra(ARG_ID);

            case SEARCH:
                if(intent.hasExtra(SearchManager.QUERY)) {
                    return AnalyticsHelper.GA_SCREEN_Shop_Search_Query_Prefix + intent.getStringExtra(SearchManager.QUERY);
                } else {
                    return AnalyticsHelper.GA_SCREEN_Shop_Search_Query_Prefix + intent.getDataString();
                }

            case BOOKS_FOR_AUTHOR:
                return AnalyticsHelper.getInstance().GA_SCREEN_Shop_Author;

            case RELATED_BOOKS:
                return AnalyticsHelper.getInstance().GA_SCREEN_Shop_Related;

            case BOOK_DETAIL:
                return AnalyticsHelper.getInstance().GA_SCREEN_Shop_BookDetailsScreen;

            default:
                return "";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //this is a dodgy hack for orientation changes - we check to see if the preview activity is
        //the current base activity of the purchase controller, if it is not destoyed/finishing
        //we do not set the base activity.
        final PurchaseController purchaseController = PurchaseController.getInstance();
        final BaseActivity baseActivity = purchaseController.getBaseActivity();
        if (baseActivity != null && baseActivity instanceof PreviewActivity) {
            if (!baseActivity.isDestroyedOrFinishing()) {
                return;
            }
        }
        purchaseController.setBaseActivity(this);
    }

    private SortOption[] getSortOptions() {

        switch(mViewType) {

            case CATEGORY:
                return new SortOption[] { SortOption.BESTSELLING, SortOption.TITLE_ASCENDING, SortOption.TITLE_DESCENDING,  SortOption.PRICE_ASCENDING, SortOption.PRICE_DESCENDING,
                        SortOption.PUBLICATION_DATE };
            case BOOKS_FOR_AUTHOR:
                return new SortOption[] { SortOption.BESTSELLING, SortOption.PRICE_ASCENDING, SortOption.PRICE_DESCENDING, SortOption.PUBLICATION_DATE,
                    SortOption.AUTHOR_ASCENDING, SortOption.AUTHOR_DESCENDING};
            case SEARCH:
                return new SortOption[] { SortOption.RELEVANCE, SortOption.PRICE_ASCENDING, SortOption.PRICE_DESCENDING, SortOption.PUBLICATION_DATE,
                        SortOption.AUTHOR_ASCENDING, SortOption.AUTHOR_DESCENDING};
        }

        return null;
    }

    private int getPageSize() {
        switch (mViewType) {
            case SEARCH:
                return DEFAULT_PAGE_SIZE_SEARCH_RESULTS;

            default:
                return BooksFragment.DEFAULT_PAGE_SIZE;
        }
    }

    private Uri createUri(Intent intent) {

        if(mViewType == null) {
            return null;
        }

        switch(mViewType) {

            case CATEGORY: {
                String name = intent.getStringExtra(ARG_ID);
                //TODO change this to get books by category location when CP-1759 is fixed. will save one API call
                return CatalogueContract.Books.getBooksForCategoryNameUri(name, true, getPageSize() , 0);
            }
            case SEARCH: {
                String query;

                if(intent.hasExtra(SearchManager.QUERY)) {
                    query = intent.getStringExtra(SearchManager.QUERY);
                } else {
                    query = intent.getDataString();
                }

                return CatalogueContract.Books.getBookSearchUri(query, true, getPageSize(), 0);
            }
            case BOOKS_FOR_AUTHOR: {
                String authorId = null;

                if(intent.hasExtra(ARG_ID)) {
                    authorId = intent.getStringExtra(ARG_ID);
                } else if(intent.hasExtra(ARG_SUGGESTION_ID)) {
                    authorId = intent.getStringExtra(ARG_SUGGESTION_ID);
                }

                return CatalogueContract.Books.getBooksForAuthorIdUri(authorId, true, getPageSize(), 0);
            }
            case RELATED_BOOKS: {
                final String isbn = intent.getStringExtra(ARG_ISBN);
                return CatalogueContract.Books.getBooksRelatedToISBNUri(isbn, true, getPageSize(), 0);

            }
            case ISBN: {
                final String query = intent.getData().getLastPathSegment();
                return CatalogueContract.Books.getBookSearchUri(query, true, getPageSize(), 0);
            }
            case BOOK_DETAIL: {
                final String isbn = intent.getStringExtra(ARG_ISBN);
                return CatalogueContract.Books.getBookSearchUri(isbn, true, getPageSize(), 0);
            }
        }

        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.shop_actionbar_options, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem menuItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        final boolean isPhone = !getResources().getBoolean(R.bool.isTablet);
        // If we are a search view then we always show the search field as expanded
        switch (mViewType) {
            case SEARCH:
                mSearchView.setIconified(false);
                break;

            case ISBN:
                mSearchView.setIconified(false);
                break;

            default:
                // Show the search view
                mSearchView.setIconified(isPhone);
                break;
        }

        if (isPhone) {
            if (! mSearchView.isIconified()) {
                getSupportActionBar().setIcon(null);
            }
            mSearchView.setOnSearchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSupportActionBar().setIcon(null);
                }
            });
            mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    getSupportActionBar().setIcon(R.drawable.actionbar_icon);
                    return false;
                }
            });
        }

        mSearchView.clearFocus();

        //Sets typeface for searchView
        EditText searchText = ((EditText) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text));
        searchText.setTypeface(TypefaceCache.getInstance().getTypeface(this, R.string.font_lola_regular));

        populateSearchView(getIntent());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            navigateToParent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewIntent(Intent intent) {

        // In case we come in here via a marketing deep link we want to track in Ad-X
        AnalyticsHelper.handleAdXDeepLink(this, intent);

        mViewType = getViewType(intent);
        mScreenName = getScreenName(intent);

        Uri uri = createUri(intent);

        // To prevent a very rare crash if we ever get a null uri at this point we just close this activity. This is consistent
        // with how we handle the same behaviour in onCreate
        if (uri == null) {

            // Log the failure to Crashlytics so we can get a handle on it
            String error = String.format("Null URI in onNewIntent (SearchActivity) - viewType = " + mViewType + " screenName = " + mScreenName);
            Crashlytics.logException(new Exception(error));

            finish();
            return;
        }

        BooksFragment fragment = (BooksFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);

        // Force an update of the options menu to ensure we setup the mSearchView member variable before trying to populate the search view
        invalidateOptionsMenu();
        populateSearchView(intent);

        if (fragment != null) {
            fragment.setSortOptions(getSortOptions(), true);
            fragment.setUri(uri, getPageSize());
            fragment.setScreenName(mScreenName);
            fragment.setHasBeenViewed(true);
        }

        setIntent(intent);

        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
    }

    private void populateSearchView(Intent intent) {

        // Just in case the search view field is null we bail out here to prevent crashes
        if (mSearchView == null) {
            return;
        }

        if(mViewType == ViewType.SEARCH) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchView.setQuery(query, false);
        } else if (mViewType == ViewType.ISBN) {
            final String isbn = intent.getData().getLastPathSegment();
            mSearchView.setQuery(isbn, false);
        }

        if(intent.getAction() == null) {
            return;
        }

        if(intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchView.setQuery(query, false);
        }

        if ((mViewType != ViewType.ISBN)&&(intent.getAction().equals(Intent.ACTION_VIEW))) {
            String query = intent.getDataString();
            mSearchView.setQuery(query, false);
        }

        getSupportActionBar().setTitle(getString(R.string.search_result));
    }
}
