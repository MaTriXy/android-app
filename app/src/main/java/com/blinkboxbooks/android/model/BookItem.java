// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

import com.blinkbox.java.book.model.BBBEPubBook;

import java.util.Comparator;

/**
 * Helper object which represents a Book, reading progress Bookmark and an optional EPubBook
 */
public class BookItem {

    /**
     * the Book object
     */
    public Book book;

    /**
     * the Last Position object
     */
    public Bookmark lastPosition;

    /**
     * the Bookmarks json String
     */
    public String bookmarks;

    /**
     * the Highlights json String
     */
    public String highlights;

    /**
     * the Epub book object
     */
    public BBBEPubBook bbbePubBook;

    /**
     * Create a bookitem object
     *
     * @param book        The book object
     * @param bookmark    The lastPosition attached to this book
     * @param bbbePubBook The epub book or null
     */
    public BookItem(Book book, Bookmark bookmark, String bookmarks, String highlights, BBBEPubBook bbbePubBook) {
        this.book = book;
        this.lastPosition = bookmark;
        this.bbbePubBook = bbbePubBook;
        this.bookmarks = bookmarks;
        this.highlights = highlights;
    }

    public static final Comparator<BookItem> purchaseDateComparator = new Comparator<BookItem>() {

        public int compare(BookItem book1, BookItem book2) {

            if (book1.book.purchase_date < book2.book.purchase_date) {
                return 1;
            } else if (book1.book.purchase_date > book2.book.purchase_date) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    public static final Comparator<BookItem> updateDateComparator = new Comparator<BookItem>() {

        public int compare(BookItem book1, BookItem book2) {

            if (book1.book.update_date < book2.book.update_date) {
                return 1;
            } else if (book1.book.update_date > book2.book.update_date) {
                return -1;
            } else {
                return 0;
            }
        }
    };
}
