package com.blinkboxbooks.android.list;

import android.content.Context;
import android.text.TextUtils;

import com.blinkboxbooks.android.model.BookItem;
import com.blinkboxbooks.android.model.Query;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/*
 * A loader that queries the {@link ContentResolver} and returns a {@link Cursor}.
 * This class implements the {@link Loader} protocol in a standard way for
 * querying cursors, building on {@link AsyncTaskLoader} to perform the cursor
 * query on a background thread so that it does not block the application's UI.
 *
 * <p>An extension of LibraryLoader that sorts the book author by last name.
 */
public class LibrarySortByAuthorLoader extends LibraryLoader {
    boolean mSortAuthorDescending;

    private class SortByAuthorComparator implements Comparator<BookItem> {

        @Override
        public int compare(BookItem lhs, BookItem rhs) {
            final String leftString = getAuthorLastName(lhs);
            final String rightString = getAuthorLastName(rhs);

            //Always put authors with blank name last, regardless of whether this is ordered A-Z or A-Z
            if (TextUtils.isEmpty(leftString)) {
                return 1;
            } else if (TextUtils.isEmpty(rightString)) {
                return -1;
            } else {
                return leftString.compareToIgnoreCase(rightString) * (mSortAuthorDescending ? -1 : 1);
            }

        }

        @Override
        public boolean equals(Object object) {
            return false;
        }

        private String getAuthorLastName(BookItem item) {
            final String author = item.book.author;
            final String[] split = author.split(" ");
            return split[split.length-1];
        }
    }

    public LibrarySortByAuthorLoader(Context context, List<Query> queryList, boolean sortAuthorDescending) {
        super(context, queryList);
        mSortAuthorDescending = sortAuthorDescending;
    }

    @Override
    public List<BookItem> loadInBackground() {
        List<BookItem> bookItems = super.loadInBackground();

        Collections.sort(bookItems,  new SortByAuthorComparator());

        return bookItems;
    }
}
