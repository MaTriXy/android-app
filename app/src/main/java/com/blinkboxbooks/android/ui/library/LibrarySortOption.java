package com.blinkboxbooks.android.ui.library;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.provider.BBBContract;

/**
 * All the possible sort options for the library
 */
public enum LibrarySortOption {

    RECENTLY_READ(R.string.library_sort_recently_read, BBBContract.Books.LAST_READ_SORT, true),
    TITLE_ASCENDING(R.string.library_sort_title_ascending, BBBContract.Books.TITLE_SORT, false),
    PURCHASE_DATE(R.string.library_sort_purchase_date, BBBContract.Books.PURCHASE_DATE_SORT, true),
    AUTHOR_ASCENDING(R.string.library_sort_author_ascending, null, true),
    AUTHOR_DESCENDING(R.string.library_sort_author_descending, null, false);

    public int displayNameResourceId;
    public String sortParameter;
    public boolean desc;

    private LibrarySortOption(int displayNameResourceId, String sortParameter, boolean desc) {
        this.displayNameResourceId = displayNameResourceId;
        this.sortParameter = sortParameter;
        this.desc = desc;
    }
}