// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop;

import android.app.Activity;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.dialog.RadioOptionsDialogFragment;
import com.blinkboxbooks.android.loader.CatalogueLoader;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.ShopHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.shop.preview.ShopItemFragment;
import com.blinkboxbooks.android.util.BBBUIUtils;
import com.blinkboxbooks.android.widget.ShopItemView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Fragment for displaying books from a curated list e.g. best sellers, free books, new releases
 */
public class BooksFragment extends ImpressionReporterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 12;

    private static final String TAG_SORT_OPTIONS = "SortOptions";

    /** The default page size to use to display items */
    public static final int DEFAULT_PAGE_SIZE = 100;

    private static final String ARG_DATA = "data";
    private static final String ARG_SHOW_POSITION = "show_position";
    private static final String ARG_SORT_OPTIONS = "sortOptions";
    private static final String ARG_CURRENT_SORT_OPTION = "currentSortOption";
    private static final String ARG_PUT_FIRST_INDEX = "firstIndex";
    private static final String ARG_PAGE_SIZE = "pageSize";
    private static final String ARG_SUPPORT_PAGING = "supportPaging";
    private static final String ARG_CLEAR_DATA = "clearData";
    private static final String ARG_SCREEN_NAME = "screenName";

    /**
     * Creates a new instance of this fragment
     *
     * @param screenName the name of the screen that this list of books belongs to
     * @param data the base URI of the data to be requested
     * @param showPosition set to true to show a numerical indicator of the books position
     * @param pageSize the number of items in a single page of data
     * @param supportPaging set to true to allow loading of multiple pages of data
     * @param sortOptions an array of sort options. If the array is null sort options will not be shown. If the array is of size one, then the sort order will be set to that
     *                    one element and sort options will not be shown. If the array is greater than one then the sort order will default to the first option and sort options
     *                    will be shown in the header of the ListView.
     * @return
     */
    public static BooksFragment newInstance(String screenName, Uri data, boolean showPosition, int pageSize, boolean supportPaging,SortOption... sortOptions) {
        return newInstance(screenName,data,showPosition,pageSize,supportPaging,0,sortOptions);
    }

    /**
     * Creates a new instance of this fragment
     *
     * @param screenName the name of the screen that this list of books belongs to
     * @param data the base URI of the data to be requested
     * @param showPosition set to true to show a numerical indicator of the books position
     * @param pageSize the number of items in a single page of data
     * @param supportPaging set to true to allow loading of multiple pages of data
     * @param putIndexFirst reorder the search so this index is first.
     * @param sortOptions an array of sort options. If the array is null sort options will not be shown. If the array is of size one, then the sort order will be set to that
     *                    one element and sort options will not be shown. If the array is greater than one then the sort order will default to the first option and sort options
     *                    will be shown in the header of the ListView.
     * @return
     */
    public static BooksFragment newInstance(String screenName, Uri data, boolean showPosition, int pageSize, boolean supportPaging,int putIndexFirst,SortOption... sortOptions) {
        BooksFragment fragment = new BooksFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA, data);
        args.putBoolean(ARG_SHOW_POSITION, showPosition);
        args.putSerializable(ARG_SORT_OPTIONS, sortOptions);
        args.putInt(ARG_PAGE_SIZE, pageSize);
        args.putBoolean(ARG_SUPPORT_PAGING, supportPaging);
        args.putInt(ARG_PUT_FIRST_INDEX, putIndexFirst);
        args.putString(ARG_SCREEN_NAME, screenName);
        fragment.setArguments(args);

        return fragment;
    }

    private Uri mUri;

    private int mColumns;
    private boolean mShowPosition;

    private ListView mListView;
    private TextView mTextViewError;
    private ProgressBar mProgressBar;
    private SortOption[] mSortOptions;
    private SortOption mCurrentSortOption;

    private View mFooterView;
    private View mFooterSpinner;
    private int mTotalRequested = 0;
    private Cursor mData;
    private int mPageSize;
    private int mPutFirstIndex;
    private boolean mSupportPaging;
    private boolean mSuspendImageLoading;
    private int mTopLineGap;
    private Queue<ShopItemView> mSuspendedShopItemImageLoadingQueue = new LinkedList<ShopItemView>();

    // An observer for the users library that will cause us to reload data when a change is observed.
    private ContentObserver mLibraryObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Bundle args = new Bundle();
            args.putBoolean(ARG_CLEAR_DATA, true);
            getLoaderManager().restartLoader(LOADER_ID, args, BooksFragment.this);
        }
    };

    /**
     * Set the a Uri to be requested
     * @param uri the Uri to request
     * @param pageSize the page size
     */
    public void setUri(Uri uri, int pageSize) {
        mUri = uri;
        mPageSize = pageSize;

        if (mProgressBar != null) { //only search if view has been initialised
            Bundle args = new Bundle();
            args.putBoolean(ARG_CLEAR_DATA, true);
            getLoaderManager().restartLoader(LOADER_ID, args, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_DATA, mUri);
        outState.putSerializable(ARG_CURRENT_SORT_OPTION, mCurrentSortOption);
        outState.putSerializable(ARG_SORT_OPTIONS, mSortOptions);
    }

    /**
     * Updates the sort options
     * @param sortOptions
     * @param clearCurrentOption
     */
    public void setSortOptions(SortOption[] sortOptions, boolean clearCurrentOption) {
        mSortOptions = sortOptions;

        if(mSortOptions != null && mSortOptions.length > 0) {
            if (mCurrentSortOption == null || clearCurrentOption) {
                // Always reset the current sort option when we change the sort options else we could leave a sort option selected that
                // is not supported in the new option set.
                mCurrentSortOption = mSortOptions[0];
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        final Resources resources = getResources();
        mColumns = resources.getInteger(R.integer.shop_number_columns);
        mTopLineGap = resources.getDimensionPixelOffset(R.dimen.gap_large);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mSortOptions != null && mSortOptions.length > 1) {
            inflater.inflate(R.menu.library_sort_options,menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                displaySortOptions();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displaySortOptions() {
        int selectedIndex = 0;
        String[] sortOptionStrings = new String[mSortOptions.length];

        for (int i = 0; i < mSortOptions.length; i++) {
            sortOptionStrings[i] = getString(mSortOptions[i].displayNameResourceId);
            if (mCurrentSortOption.equals(mSortOptions[i])) {
                selectedIndex = i;
            }
        }

        int dialogWidth = getResources().getDimensionPixelSize(R.dimen.sort_options_dialog_width);
        RadioOptionsDialogFragment dialog = RadioOptionsDialogFragment.newInstance(getString(R.string.sort_by), sortOptionStrings, selectedIndex, dialogWidth);
        dialog.setOptionSelectedListener(new RadioOptionsDialogFragment.OptionSelectedListener() {
            @Override
            public void onOptionSelected(int selectionIndex, String option) {
                mCurrentSortOption = mSortOptions[selectionIndex];
                Bundle args = new Bundle();
                args.putBoolean(ARG_CLEAR_DATA, true);
                getLoaderManager().restartLoader(LOADER_ID, args, BooksFragment.this);
            }
        });

        ((BaseActivity) getActivity()).showDialog(dialog, TAG_SORT_OPTIONS, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        View view = inflater.inflate(R.layout.fragment_shop_books, container, false);

        mListView = (ListView)view.findViewById(R.id.listview);
        mTextViewError = (TextView)view.findViewById(R.id.textview_error);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progress_loading);
        View footerView = View.inflate(getActivity(), R.layout.list_item_shop_loading_more, null);
        mFooterSpinner = footerView.findViewById(R.id.progress_loading);
        mListView.addFooterView(footerView);

        mShowPosition = arguments.getBoolean(ARG_SHOW_POSITION);
        mPageSize = arguments.getInt(ARG_PAGE_SIZE);
        mPutFirstIndex = arguments.getInt(ARG_PUT_FIRST_INDEX);
        setScreenName(arguments.getString(ARG_SCREEN_NAME));
        mSupportPaging = arguments.getBoolean(ARG_SUPPORT_PAGING);

        Object[] options;

        mTotalRequested = mPageSize;

        if(savedInstanceState != null) {
            mUri = savedInstanceState.getParcelable(ARG_DATA);
            mCurrentSortOption = (SortOption)savedInstanceState.getSerializable(ARG_CURRENT_SORT_OPTION);
            options = (Object[])savedInstanceState.getSerializable(ARG_SORT_OPTIONS);
        } else {
            mUri = arguments.getParcelable(ARG_DATA);
            options = (Object[])arguments.getSerializable(ARG_SORT_OPTIONS);
        }

        if(options != null) {
            SortOption[] sortOptions = new SortOption[options.length];

            for(int i=0; i<options.length; i++) {
                sortOptions[i] = (SortOption)options[i];
            }

            setSortOptions(sortOptions, false);
        }

        Bundle args = new Bundle();
        args.putBoolean(ARG_CLEAR_DATA, true);
        getLoaderManager().initLoader(LOADER_ID, args, this);

        // If we are supporting paging we add a scroll listener so we can request the next page when the user scrolls
        // near the end of the list
        if (mSupportPaging) {
            mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView absListView, int scrollState) {

                    // When we fling a list that supports paging we can fire of a large number of pointless image requests
                    // that will not hit the cache and result in a lot of memory allocations. This in turns leads to a lot
                    // of garbage collection which causes major stuttering on older slower device such as the Hudl1. To avoid
                    // this stuttering we suspend image loading for the duration of a fling.
                    if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                        mSuspendImageLoading = true;
                    } else {
                        mSuspendImageLoading = false;
                        // Now that flinging has finished we will attempt to resume any images that were suspended
                        resumeImageLoading();
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {

                    // We only consider loading a new page if we currently have full pages of data. When we hit
                    // a point where we don't get a full page that means there is no more data to request.
                    if (mData != null && mData.getCount() == mTotalRequested) {

                        // The total rows in the entire list view. The (columns + 1) mod is to take into account the fact
                        // that the final row may not be full (else we would round down to the previous full row).
                        int totalRows = (mTotalRequested + (mColumns - 1)) / mColumns;

                        // The number of rows per page.
                        int rowsPerPage = (mPageSize + (mColumns - 1)) / mColumns;

                        // The trigger point for requesting the next page is when we are within 1/2 of a page of data away
                        // from the end of the list
                        int triggerPoint = totalRows - (rowsPerPage / 2);

                        if (firstVisible >= triggerPoint) {
                            mUri = CatalogueContract.Books.refreshUriCountAndOffset(mUri, mPageSize, mTotalRequested);
                            mTotalRequested += mPageSize;

                            // Show the spinner in the footer while we are loading the next page
                            mFooterSpinner.setVisibility(View.VISIBLE);
                            getLoaderManager().restartLoader(LOADER_ID, null, BooksFragment.this);
                        }
                    }
                }
            });
        }

        return view;
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

    private void showLoading(boolean loading) {
        mProgressBar.setVisibility(loading ? View.VISIBLE:View.GONE);
        mListView.setVisibility(loading ? View.GONE:View.VISIBLE);

        if(loading) {
            mTextViewError.setVisibility(View.GONE);
        }
    }

    private void showError(int errorMessageResourceId) {
        showLoading(false);
        mListView.setVisibility(View.GONE);
        mTextViewError.setVisibility(View.VISIBLE);
        mTextViewError.setText(errorMessageResourceId);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Check if we need to clear the data before starting the loader
        if (args != null && args.getBoolean(ARG_CLEAR_DATA)) {
            mData = null;
            mTotalRequested = mPageSize;
            mUri = CatalogueContract.Books.refreshUriCountAndOffset(mUri, mPageSize, 0);
            BooksAdapter booksAdapter = new BooksAdapter(mData);
            mListView.setAdapter(booksAdapter);
        }

        // Only show the loading page for the first page of results
        if (mData == null) {
            showLoading(true);
        }

        CatalogueLoader loader = new CatalogueLoader(getActivity(), mUri, null, null, null, mCurrentSortOption);
        loader.setErrorListener(mErrorListener);

        return loader;
    }

    /**
     * A simple cursor wrapper that will move the given position to the beginning. All other
     * rows are moved to the next row.
     */
    private class ReorderCursorWrapper extends CursorWrapper {
        private int mPutFirst;
        private int mPosition;

        public ReorderCursorWrapper(Cursor cursor, int putFirst) {
            super(cursor);
            mPutFirst = putFirst;
            mPosition = -1;
        }

        @Override
        public boolean moveToPosition(int position) {
            mPosition = position;
            if (mPosition == 0) {
                return super.moveToPosition(mPutFirst);
            } else if (mPosition <= mPutFirst) {
                return super.moveToPosition(mPosition - 1);
            } else {
                return super.moveToPosition(mPosition);
            }
        }

        @Override
        public boolean move(int offset) {
            return moveToPosition(mPosition + offset);
        }

        @Override
        public boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override
        public boolean moveToNext() {
            return moveToPosition(mPosition + 1);
        }

        @Override
        public boolean moveToLast() {
            return moveToPosition(getCount()-1);
        }

        @Override
        public boolean moveToPrevious() {
            return moveToPosition(mPosition - 1);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ListAdapter adapter = mListView.getAdapter();

        // Sanity check that we have a footer view set
        mFooterSpinner.setVisibility(View.GONE);

        if (data != null) {
            data = new ReorderCursorWrapper(data,mPutFirstIndex);
        }

        if (mData != null) {
            mData = new MergeCursor(new Cursor[] {mData, data});
        } else {
            mData = data;
        }

        if (mData != null) {
            // We only call show loading if we have valid data because it makes the listview visible.
            showLoading(false);

            if (mData.getCount() > 0) {
                if (adapter == null) {
                    adapter = new BooksAdapter(mData);
                    mListView.setAdapter(adapter);
                } else {

                    if (adapter instanceof HeaderViewListAdapter) {
                        ((BooksAdapter) (((HeaderViewListAdapter) adapter).getWrappedAdapter())).setData(mData);
                    } else if (adapter instanceof BooksAdapter) {
                        ((BooksAdapter) adapter).setData(mData);
                    }
                }
            }
        } else {
            // Ensure that the progress bar gets hidden
            mProgressBar.setVisibility(View.GONE);
        }

        // Destroy the loader to prevent multiple loaders if the device is rotated
        getLoaderManager().destroyLoader(LOADER_ID);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void resumeImageLoading() {

        // Attempt to resume every suspended image that is in the queue.
        for (ShopItemView shopItemView : mSuspendedShopItemImageLoadingQueue) {
            shopItemView.resumeImageLoading();
        }

        mSuspendedShopItemImageLoadingQueue.clear();
    }

    private class BooksAdapter extends BaseAdapter {

        private final ArrayList<ShopItem> mShopItems;
        private int mRows;

        private BooksAdapter(Cursor data) {
            mShopItems = new ArrayList<ShopItem>();
            setData(data);
        }

        private void setData(Cursor data) {
            mShopItems.clear();

            if(data == null) {
                return;
            }

            data.moveToFirst();

            do {
                mShopItems.add(ShopHelper.getShopItem(data));

                data.moveToNext();
            } while(!data.isAfterLast());

            mRows = mShopItems.size() / mColumns;

            if(mShopItems.size() % mColumns != 0) {
                mRows++;
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mRows;
        }

        @Override
        public Object getItem(int position) {
            return mShopItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView == null) {
                convertView = BBBUIUtils.createShopItemRow(getActivity(), mColumns, null, BBBUIUtils.getScreenWidth(getActivity()), getScreenName());
            }

            convertView.setPadding(0,position == 0 ? mTopLineGap : 0, 0, 0);

            int shopItemsSize = mShopItems.size();
            ShopItemView shopItemView;

            int startIndex = position * mColumns;
            ShopItem shopItem;

            for(int i=0, j; i<mColumns; i++) {
                shopItemView = (ShopItemView)convertView.findViewWithTag(String.valueOf(i));

                j = startIndex + i;

                if(j<shopItemsSize) {
                    shopItem = mShopItems.get(j);

                    // Add this shop item to the set of Google Analytics impressions that we will send out later
                    reportShopItemImpression(j, shopItem);

                    if(mShowPosition) {
                        shopItem.position = j+1;
                    } else {
                        shopItem.position = null;
                    }

                    shopItemView.setVisibility(View.VISIBLE);
                    shopItemView.setData(shopItem, mSuspendImageLoading);

                    // Add the suspended shop item view to the back of the queue
                    if (mSuspendImageLoading) {
                        if (mSuspendedShopItemImageLoadingQueue.contains(shopItemView)) {
                            mSuspendedShopItemImageLoadingQueue.remove(shopItemView);
                        }
                        mSuspendedShopItemImageLoadingQueue.add(shopItemView);
                    }

                    shopItemView.setOnClickListener(new ShopItemFragment.ShopItemOnClickListener(getActivity(), shopItem, getScreenName()));
                } else {
                    shopItemView.setVisibility(View.INVISIBLE);
                }
            }

            return convertView;
        }
    }

    private final CatalogueLoader.ErrorListener mErrorListener = new CatalogueLoader.ErrorListener() {

        @Override
        public void internalServerError() {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        // A server error will always result in displaying the full screen error message
                        mData = null;
                        showError(R.string.error_server_message);
                    }
                }
            };

            Activity activity = getActivity();
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
                        // If its the first page of data that failed then show the full screen error message
                        if (mData == null) {
                            showError(R.string.error_no_network_shop);
                        } else {
                            // We are already displaying at least one page of books so just keep retrying the loader in case the network returns
                            getLoaderManager().restartLoader(LOADER_ID, null, BooksFragment.this);
                        }
                    }
                }
            };

            Activity activity = getActivity();
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
                        // If its the first page of data that failed then show the no books message
                        if (mData == null) {
                            showError(R.string.no_books);
                        }
                        // Otherwise we expect no results in the case that we request an additional page beyond the
                        // total amount of data. So just do nothing.
                    }
                }
            };

            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        }
    };
}