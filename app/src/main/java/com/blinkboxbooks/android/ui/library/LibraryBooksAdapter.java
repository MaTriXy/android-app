package com.blinkboxbooks.android.ui.library;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.BookDownloadController;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.model.BookItem;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.shop.ShopActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBUIUtils;
import com.blinkboxbooks.android.widget.BookCover;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom adapter for displaying a list of book items with a RecyclerView
 */
public class LibraryBooksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_FIND_EBOOKS = 0;
    private static final int VIEW_TYPE_BOOK = 1;

    private List<BookItem> mItems = new ArrayList<>();
    private int mColumns;
    private BaseActivity mActivity;
    private LibraryBookOptionsHelper mOptionsHelper;
    private int mBookPaddingSide;
    private int mFilterType;

    final Comparator<BookItem> mComparator = new Comparator<BookItem>() {
        @Override
        public int compare(BookItem lhs, BookItem rhs) {
            long rhsId = rhs.book == null ? 0L : rhs.book.id;
            long lhsId = lhs.book == null ? 0L : lhs.book.id;

            return (int) (lhsId - rhsId);
        }
    };

    public LibraryBooksAdapter(BaseActivity activity, List<BookItem> items, int columns, int filterType, LibraryBookOptionsHelper optionsHelper) {
        mItems = items;
        mActivity = activity;
        mColumns = columns;
        mOptionsHelper = optionsHelper;
        mBookPaddingSide = activity.getResources().getDimensionPixelOffset(R.dimen.gap_medium);
        mFilterType = filterType;
        setHasStableIds(true);
    }

    /**
     * Set the item at the specified index, and notify the recycler view that this index has changed
     *
     * @param index the index to udpate
     * @param item  the item to set
     */
    public void setItem(int index, BookItem item) {
        mItems.set(index, item);
        notifyItemChanged(index);
    }

    /**
     * If a BookItem with the same id exists in the adapter, overwrite it's index with the given item & notify the recycler view
     *
     * @param item the item to set
     */
    public void updateItem(BookItem item) {
        for (int index = mItems.size(); --index >= 0; ) {
            final BookItem book = mItems.get(index);
            if (mComparator.compare(book, item) == 0) {
                mItems.set(index, item);
                notifyItemChanged(index);
                break;
            }
        }
    }

    /**
     * Remove the given BookItem from teh adapter, and notify the recycler view.
     *
     * @param item the item to remove.
     * @return true if the item has been found & removed, false otherwise.
     */
    public boolean removeItem(BookItem item) {
        for (int index = mItems.size(); --index >= 0; ) {
            final BookItem book = mItems.get(index);
            if (mComparator.compare(book, item) == 0) {
                mItems.remove(index);
                notifyItemRemoved(index);
                return true;
            }
        }

        return false;
    }

    /**
     * Set the list of book items for the adapter to use
     *
     * @param items the list of BookItem objects that the adapter should use
     */
    public void setItems(List<BookItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        final BookItem bookItem = mItems.get(position);
        return bookItem.book == null ? 0L : bookItem.book.id;
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof FindMoreEbooksBookItem) {
            return VIEW_TYPE_FIND_EBOOKS;
        } else {
            return VIEW_TYPE_BOOK;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_FIND_EBOOKS:
                return new ViewHolderFindEbooks(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_find_more_ebooks, parent, false));
            case VIEW_TYPE_BOOK:
            default:
                return new ViewHolderBook(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_book_griditem, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        // Special case handling for the find ebooks view type
        if (getItemViewType(position) == VIEW_TYPE_FIND_EBOOKS) {
            ViewHolderFindEbooks viewHolder = (ViewHolderFindEbooks) holder;
            calculateBookCoverSize(viewHolder.bookFrame);

            viewHolder.bookFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_ITEM_CLICK, AnalyticsHelper.GA_EVENT_FIND_MORE_BOOKS_CLICKED, "", null);
                    Intent intent = new Intent(mActivity, ShopActivity.class);
                    mActivity.startActivity(intent);
                }
            });

        } else {
            // Standard handling for a normal book item
            final BookItem item = mItems.get(position);
            final ViewHolderBook viewHolder = (ViewHolderBook) holder;

            viewHolder.bookCover.setBook(item.book);
            viewHolder.bookCover.setDownloadProgress(item.book.download_status, item.book.download_offset);

            if (item.lastPosition.position != null) {
                viewHolder.progressBar.setProgress(item.lastPosition.percentage);
                viewHolder.progressBar.setVisibility(View.VISIBLE);
            } else {
                viewHolder.progressBar.setVisibility(View.INVISIBLE);
            }

            calculateBookCoverSize(viewHolder.bookCover);

            viewHolder.bookCover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // If the book is in the downloaded state we open it. It its not downloaded we the user will
                    // just remain in the library.
                    if (item.book.download_status == BBBContract.Books.DOWNLOADED) {
                        Intent intent = LibraryController.getOpenBookIntent(mActivity, item.book);
                        mActivity.startActivity(intent);
                    } else if (item.book.download_status == BBBContract.Books.DOWNLOADING) {
                        // If the book is currently downloading then we cancel the active download
                        BookDownloadController.getInstance(mActivity).cancelDownloadClicked(item.book);
                    } else {
                        BookDownloadController.getInstance(mActivity).startDownloadClicked(mActivity, item.book);
                    }
                }
            });

            viewHolder.optionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOptionsHelper.displayOptionsForBook(mActivity, viewHolder.optionsButton, item, mFilterType);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /**
     * ViewHolder class to store UI controls for a standard book
     */
    private static class ViewHolderBook extends RecyclerView.ViewHolder {
        private View optionsButton;
        private BookCover bookCover;
        private ProgressBar progressBar;

        public ViewHolderBook(View itemView) {
            super(itemView);
            bookCover = (BookCover) itemView.findViewById(R.id.bookcover);
            optionsButton = itemView.findViewById(R.id.btn_options);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressbar_read);
        }
    }

    /**
     * ViewHolder class to store UI conrols for find more ebooks
     */
    public static class ViewHolderFindEbooks extends RecyclerView.ViewHolder {

        public View bookFrame;

        public ViewHolderFindEbooks(View itemView) {
            super(itemView);
            bookFrame = itemView.findViewById(R.id.book_frame);
        }
    }

    /**
     * Calculate the size of the book cover based on the current screen dimensions
     *
     * @param bookCover the book cover whose size to set
     */
    private void calculateBookCoverSize(View bookCover) {
        final int width = ((BBBUIUtils.getScreenWidth(mActivity) - (mBookPaddingSide * (mColumns + 1)))) / mColumns;
        ViewGroup.LayoutParams params = bookCover.getLayoutParams();
        params.width = width;
        params.height = (int) (width * LibraryController.WIDTH_HEIGHT_RATIO);
        bookCover.setLayoutParams(params);
    }
}