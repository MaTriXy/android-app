// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;

/**
 * All the possible sort options
 */
public enum SortOption {

    POPULARITY(R.string.shop_sort_popularity, BBBApiConstants.ORDER_POPULARITY, true),
    RELEVANCE(R.string.shop_sort_relevance, BBBApiConstants.ORDER_RELEVANCE, true),
    BESTSELLING(R.string.shop_sort_bestselling, BBBApiConstants.ORDER_SALES_RANK, true),
    TITLE_ASCENDING(R.string.shop_sort_title_ascending, BBBApiConstants.ORDER_TITLE, false),
    TITLE_DESCENDING(R.string.shop_sort_title_descending, BBBApiConstants.ORDER_TITLE, true),
    PRICE_ASCENDING(R.string.shop_sort_price_ascending, BBBApiConstants.ORDER_PRICE, false),
    PRICE_DESCENDING(R.string.shop_sort_price_descending, BBBApiConstants.ORDER_PRICE, true),
    PUBLICATION_DATE(R.string.shop_sort_publication_date, BBBApiConstants.ORDER_PUBLICATION_DATE, true),
    AUTHOR_ASCENDING(R.string.shop_sort_author_ascending, BBBApiConstants.ORDER_AUTHOR, false),
    AUTHOR_DESCENDING(R.string.shop_sort_author_descending, BBBApiConstants.ORDER_AUTHOR, true),
    // The user can't understand what sequential means so we call it popularity
    SEQUENTIAL(R.string.shop_sort_popularity, BBBApiConstants.ORDER_SEQUENTIAL, false);

    public int displayNameResourceId;
    public String sortParameter;
    public boolean desc;

    SortOption(int displayNameResourceId, String sortParameter, boolean desc) {
        this.displayNameResourceId = displayNameResourceId;
        this.sortParameter = sortParameter;
        this.desc = desc;
    }

    public String toString() {
        return BBBApplication.getApplication().getString(displayNameResourceId);
    }

}
