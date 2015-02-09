package com.blinkboxbooks.android.ui.library;

/**
 * Interface that a class can implement to handle callbacks to be notified about a book count value from
 * a LibraryBooksFragment.
 */
public interface BookCountHandler {

    /**
     * Set the number of books that are available in a LibraryBooksFragment
     * @param fragment the fragment that is making this request
     * @param numBooks the number of books
     */
    public void setNumberOfItems(LibraryBooksFragment fragment, int numBooks);
}
