package com.blinkboxbooks.android.ui.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.BookDownloadController;
import com.blinkboxbooks.android.dialog.RadioOptionsDialogFragment;
import com.blinkboxbooks.android.list.LibraryLoader;
import com.blinkboxbooks.android.list.LibrarySortByAuthorLoader;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.BookItem;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.Query;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for displaying a grid view of books that are within the user library. The fragment is initialised with a filter
 * type that is used to limit what books are shown.
 */
public class LibraryBooksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<BookItem>>, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = LibraryBooksFragment.class.getSimpleName();

    private static final String ARG_FILTER_TYPE = "filter_type";
    private static final String ARG_SORT_OPTIONS = "sort_options";
    private static final String ARG_ALWAYS_SHOW_FIND_MORE = "always_show_find_more";

    private static final int VIEW_LOADING = 0;
    private static final int VIEW_CONTENT = 1;

    private static final String TAG_SORT_OPTIONS = "SortOptions";

    private static final int MIN_BOOKS_TO_SHOW_SORT = 2;

    /**
     * Constant value to indicate that we do not want to apply any filter to the user list of books in this fragment
     */
    public static final int NO_FILTER = -1;

    private RecyclerView mRecyclerView;
    private int mColumns;
    private LibraryBooksAdapter mAdapter;
    private List<BookItem> mBookList;
    private int mStatusFilterType;
    private ViewFlipper mViewFlipper;
    private LibrarySortOption mCurrentSortOption;
    private LibraryBookOptionsHelper mOptionsHelper;
    private LibrarySortOption[] mSortOptions;
    private boolean mAlwaysShowFindMore;
    private SwipeRefreshLayout mSwipeRefresh;

    // Observer for the data store so we can update when the user's library changes
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }


        @Override
        public void onChange(boolean selfChange, Uri uri) {
            LogUtils.i(TAG, "ContentObserver change - " + uri);
            if (isAdded()) {
                boolean processed = false;
                if (uri != null) {
                    String lastPathSegment = uri.getLastPathSegment();

                    try {
                        long id = Long.parseLong(lastPathSegment);

                        Book book = BookHelper.getBookFromUri(BBBContract.Books.buildBookIdUri(id));
                        Bookmark bookmark = BookmarkHelper.createBookmark(id);

                        final BookItem bookItem = new BookItem(book, bookmark, "", "", null);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Embedded books that are not in the device library are removed from the list
                                if ((bookItem.book.embedded_book)&&(!bookItem.book.in_device_library)) {
                                    mAdapter.removeItem(bookItem);
                                } else {
                                    mAdapter.updateItem(bookItem);
                                }
                            }
                        });

                        processed = true;
                    } catch (NumberFormatException e) {
                        //If this isn't a single book - then update all
                    }
                }
                if (!processed) {
                    Loader<Cursor> loader = getLoaderManager().getLoader(0);
                    if (loader != null) {
                        loader.forceLoad();
                    }
                }
            }
        }

        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    };

    // Observer for the data store so we can update when the user's library changes
    private final ContentObserver mReadingStatusObserver = new ContentObserver(new Handler()) {

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            LogUtils.i(TAG, "ContentObserver change (ReadingStatus) - " + uri);
            if (isAdded()) {
                Loader<Cursor> loader = getLoaderManager().getLoader(0);
                if (loader != null) {
                    loader.forceLoad();
                }
            }
        }

        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    };

    // Broadcast Receiver for receiving notifications of download updates
    private BroadcastReceiver mDownloadUpdateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            long bookId = intent.getLongExtra(BookDownloadController.BOOK_DOWNLOAD_BOOK_ID_EXTRA, 0);
            double download_progress = intent.getDoubleExtra(BookDownloadController.BOOK_DOWNLOAD_PROGRESS_EXTRA, 0);
            updateDownloadStatus(bookId, download_progress);
            LogUtils.i(TAG, "Download Progress Update for book id: " + bookId + " - progress = " + download_progress);
        }
    };

    /**
     * Create a new instance of the Library Books Fragment
     *
     * @param statusFilterType        the type of status filter to apply to this books fragment (or NO_FILTER for all books with no filter)
     * @param alwaysShowFindMoreBooks set to true to always display the find more books entry as the last item in the view
     * @param sortOptions             an array of sort options. If the array is of size one, then the sort order will be set to that
     *                                one element and sort options will not be shown. If the array is greater than one then the sort order will default to the first option and sort options
     *                                will be shown in the header of the ListView.
     * @return a new LibraryBooksFragment object
     */
    public static LibraryBooksFragment newInstance(int statusFilterType, boolean alwaysShowFindMoreBooks, LibrarySortOption... sortOptions) {
        LibraryBooksFragment libraryBooksFragment = new LibraryBooksFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FILTER_TYPE, statusFilterType);
        args.putSerializable(ARG_SORT_OPTIONS, sortOptions);
        args.putBoolean(ARG_ALWAYS_SHOW_FIND_MORE, alwaysShowFindMoreBooks);
        libraryBooksFragment.setArguments(args);
        return libraryBooksFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mStatusFilterType = getArguments().getInt(ARG_FILTER_TYPE);
        mAlwaysShowFindMore = getArguments().getBoolean(ARG_ALWAYS_SHOW_FIND_MORE);
        mOptionsHelper = new LibraryBookOptionsHelper();

        Object[] sortOptions = (Object[]) getArguments().getSerializable(ARG_SORT_OPTIONS);

        if (sortOptions != null && sortOptions.length > 0) {
            mSortOptions = new LibrarySortOption[sortOptions.length];

            for (int i = 0; i < sortOptions.length; i++) {
                mSortOptions[i] = (LibrarySortOption) sortOptions[i];
            }
        }

        // The selected sort option is persisted using a key derived from the status filter type that is used on this fragment
        int sortOptionSelected = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_KEY_MY_LIBRARY_SORT_OPTION + String.valueOf(mStatusFilterType), 0);
        mCurrentSortOption = mSortOptions[sortOptionSelected];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_books, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mViewFlipper = (ViewFlipper) view.findViewById(R.id.view_flipper);
        mColumns = getResources().getInteger(R.integer.library_number_columns);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), mColumns));


        final int dimensionPixelOffset = getResources().getDimensionPixelOffset(R.dimen.gap_medium);
        final int halfDimensionPixelOffset = dimensionPixelOffset / 2;

        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                super.onDraw(c, parent, state);
            }

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                final int childPosition = parent.getChildPosition(view);
                final int positionInRow = childPosition % mColumns;
                final int row = childPosition / mColumns;

                outRect.set(positionInRow == 0 ? dimensionPixelOffset : halfDimensionPixelOffset,
                        row == 0 ? dimensionPixelOffset : 0,
                        positionInRow == mColumns - 1 ? dimensionPixelOffset : halfDimensionPixelOffset,
                        dimensionPixelOffset);
            }
        });

        mRecyclerView.setHasFixedSize(true);

        mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        mSwipeRefresh.setColorSchemeResources(R.color.brand_purple);

        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_GESTURE, AnalyticsHelper.GA_EVENT_REFRESH_YOUR_LIBRARY, "", null);
                ((BaseActivity) getActivity()).onRefresh();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mBookList = new ArrayList<>();
        mAdapter = new LibraryBooksAdapter((BaseActivity) getActivity(), new ArrayList<BookItem>(), mColumns, mStatusFilterType, mOptionsHelper);
        mRecyclerView.setAdapter(mAdapter);
        mViewFlipper.setDisplayedChild(VIEW_LOADING);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getActivity().getPackageName() + BookDownloadController.BOOK_DOWNLOAD_PROGRESS_ACTION);
        getActivity().registerReceiver(mDownloadUpdateReceiver, intentFilter);

        if (mStatusFilterType == NO_FILTER) {
            getActivity().getContentResolver().registerContentObserver(BBBContract.Books.ACCOUNT_BOOKS_URI, true, mObserver);
        } else {
            getActivity().getContentResolver().registerContentObserver(BBBContract.Books.READING_STATUS_URI, true, mReadingStatusObserver);
            getActivity().getContentResolver().registerContentObserver(BBBContract.Books.ACCOUNT_BOOKS_URI, true, mObserver);
        }
        PreferenceManager.getInstance().registerOnPreferenceChangeListener(this);

        boolean isLoggedIn = AccountController.getInstance().isLoggedIn();

        getLoaderManager().restartLoader(0, null, this);

        // The swipe to refresh behaviour is only enabled when a user is logged in
        mSwipeRefresh.setEnabled(isLoggedIn);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mDownloadUpdateReceiver);
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
        getActivity().getContentResolver().unregisterContentObserver(mReadingStatusObserver);
        PreferenceManager.getInstance().unregisterOnPreferenceChangeListener(this);

        // The popup menu has weird behaviour where it appears at the top left when the activity is paused/resumed. To prevent
        // this we just dismiss the popup menu in case its being displayed when the activity is paused.
        mOptionsHelper.hidePopupMenu();

        // Make sure the swipe refresh can never get stuck in the constantly refreshing state. E.g. if we
        // rotate and miss a callback that syncing has finished.
        mSwipeRefresh.setRefreshing(false);
    }

    public void updateDownloadStatus(long bookId, double progress) {
        for (int i = 0; i < mBookList.size(); i++) {

            final BookItem bookItem = mBookList.get(i);

            if (!(bookItem instanceof FindMoreEbooksBookItem) && bookItem.book.id == bookId) {
                if (progress < 100) {
                    bookItem.book.download_status = BBBContract.Books.DOWNLOADING;
                }
                bookItem.book.download_offset = (int) progress;
                mAdapter.setItem(i, bookItem);
                break;
            }
        }
    }

    // LoaderCallbacks
    @Override
    public Loader<List<BookItem>> onCreateLoader(int id, Bundle data) {
        List<Query> queryList = new ArrayList<Query>();

        Uri uri;

        if (mStatusFilterType == NO_FILTER) {
            uri = BBBContract.Books.buildBookAccountUriAll(AccountController.getInstance().getUserId());
        } else {
            uri = BBBContract.Books.buildBookStatusUri(AccountController.getInstance().getUserId(), String.valueOf(BBBContract.Books.BOOK_STATE_READING));
        }
        queryList.add(new Query(uri, null, null, null, mCurrentSortOption != null ? mCurrentSortOption.sortParameter : null));

        if (mCurrentSortOption == LibrarySortOption.AUTHOR_ASCENDING) {
            return new LibrarySortByAuthorLoader(getActivity(), queryList, false);
        } else if (mCurrentSortOption == LibrarySortOption.AUTHOR_DESCENDING) {
            return new LibrarySortByAuthorLoader(getActivity(), queryList, true);
        } else {
            return new LibraryLoader(getActivity(), queryList);
        }
    }

    @Override
    public synchronized void onLoadFinished(Loader<List<BookItem>> loader, List<BookItem> bookItems) {

        final int numBooks = bookItems.size();

        // For the case where we have no books then we display a special placeholder "Find more ebooks"
        if (numBooks == 0) {
            mBookList = new ArrayList<BookItem>();
            mBookList.add(new FindMoreEbooksBookItem());
        } else {
            mBookList = bookItems;
            // Add a new 'find more' tile if we need more books and the last tile is not already a find more books tile
            if (mAlwaysShowFindMore && !(mBookList.get(numBooks - 1) instanceof FindMoreEbooksBookItem)) {
                mBookList.add(new FindMoreEbooksBookItem());
            }
        }
        mAdapter.setItems(mBookList);

        ((LibraryActivity) getActivity()).setNumberOfItems(this, numBooks);
        mViewFlipper.setDisplayedChild(VIEW_CONTENT);


        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LogUtils.i(TAG, "sharedPreferences" + sharedPreferences.toString() + "key" + key);
        if (PreferenceManager.PREF_KEY_CURRENT_USER.equals(key)) { // account change
            mViewFlipper.setDisplayedChild(VIEW_LOADING);
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<BookItem>> arg0) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        if (mSortOptions.length > 1 && mBookList != null && mBookList.size() > MIN_BOOKS_TO_SHOW_SORT) {
            inflater.inflate(R.menu.library_sort_options, menu);
        }

        super.onCreateOptionsMenu(menu, inflater);
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
                PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_MY_LIBRARY_SORT_OPTION + String.valueOf(mStatusFilterType), selectionIndex);
                getLoaderManager().restartLoader(0, null, LibraryBooksFragment.this);
            }
        });
        ((BaseActivity) getActivity()).showDialog(dialog, TAG_SORT_OPTIONS, false);
    }

    /**
     * Animate the Sync icon while the
     * library is syncing
     */
    public void startSync() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefresh.setRefreshing(true);
                }
            });
        }
    }

    /**
     * Stop the sync animation once sync completes
     */
    public void stopSync() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefresh.setRefreshing(false);
                }
            });
        }
    }
}
