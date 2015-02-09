package com.blinkboxbooks.android.ui.library;

import com.blinkboxbooks.android.model.BookItem;

/**
 * A special case extension of BookItem that is used to display a "Find more ebooks" placeholder. We extend
 * BookItem so that we can pass an object into the adapter using the existing interface.
 */
public class FindMoreEbooksBookItem extends BookItem {

    /**
     * Default constructor, just creates a BookItem with all parameters set to null
     */
    public FindMoreEbooksBookItem() {
        super(null, null, null, null,null);
    }
}
