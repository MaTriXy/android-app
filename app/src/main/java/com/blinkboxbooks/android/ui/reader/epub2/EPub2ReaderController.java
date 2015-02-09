// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;

import android.os.AsyncTask;

import com.blinkbox.java.book.crc.CRCHandler;
import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;

/**
 * Reader Controller
 */
public class EPub2ReaderController {

    private EPub2ReaderJSHelper mEPub2ReaderJSHelper;
    private CRCErrorHandler mCRCErrorHandler;

    public EPub2ReaderController(EPub2ReaderWebView webView, EPub2ReaderJSCallbacks ePub2ReaderJSCallbacks, CRCErrorHandler errorHandler) {
        mEPub2ReaderJSHelper = new EPub2ReaderJSHelper(webView, ePub2ReaderJSCallbacks);
        mCRCErrorHandler = errorHandler;
    }

    /**
     * Sets the book to display in the reader at the page referenced by the lastPosition
     *
     * @param book
     * @param bookmark
     */
    public void setBook(String isbn, BBBEPubBook book, Bookmark bookmark, String bookmarks, String highlights, boolean isOnlineSample) {
        mEPub2ReaderJSHelper.setBook(isbn, book, bookmark, bookmarks, highlights, isOnlineSample);
        mCRCErrorHandler.setBookISBN(isbn);
        CRCHandler.setCRCErrorHandler(mCRCErrorHandler);
    }

    /**
     * Navigates to the next page
     */
    public void nextPage() {
        mEPub2ReaderJSHelper.nextPage();
    }

    /**
     * Navigates to the previous page
     */
    public void prevPage() {
        mEPub2ReaderJSHelper.prevPage();
    }

    /**
     * Go to the page referenced by the CFI
     */
    public void jumpToCFI(String cfi) {
        mEPub2ReaderJSHelper.goToCFI(cfi);
    }

    /**
     * Go to the page referenced by the url
     */
    public void jumpToUrl(String url) {
        mEPub2ReaderJSHelper.goToUrl(url);
    }

    public EPub2ReaderJSHelper getEPub2ReaderJSHelper() {
        return mEPub2ReaderJSHelper;
    }

    /**
     * Gets the CFI of the current page
     */
    public void getCFI() {
        mEPub2ReaderJSHelper.getCFI();
    }

    /**
     * Tells the reader to go to the specified percentage progress
     * @param progress
     */
    public void goToProgress(float progress) {
        mEPub2ReaderJSHelper.goToProgress(progress);
    }

    public void toggleReaderOverlay(boolean visible) {
        mEPub2ReaderJSHelper.setHeaderVisibility(visible);
    }

    /**
     * Asynchronously set all the bookmarks for the given book id in the js library
     *
     * @param bookId
     */
    public void asyncSetJSBookmarks(final long bookId) {
        new AsyncTask<Void, Void, Bookmark[]>() {
            @Override
            protected Bookmark[] doInBackground(Void... params) {
                return BookmarkHelper.getAllBookmarks(bookId);
            }

            @Override
            protected void onPostExecute(Bookmark[] result) {
                setBookmarks(result);
            }
        }.execute();
    }

    /**
     * Set the given bookmark array on the reader. Only new bookmarks will be added and any current bookmarks not in the array will be removed
     *
     * @param bookmarks
     */
    public void setBookmarks(Bookmark[] bookmarks) {
        String bookmarkArray = "";
        int count = bookmarks.length;
        for (int i = 0; i < count; i++) {
            String cfi = bookmarks[i].position;
            bookmarkArray += "\"" + cfi + "\"";
            if (i < count - 1) {
                bookmarkArray += ", ";
            }
        }

        mEPub2ReaderJSHelper.setBookmarks(bookmarkArray);
    }

    /**
     * Set a bookmark at the current position
     */
    public void setBookmark() {
        mEPub2ReaderJSHelper.setBookmark();
    }

    /**
     * Called when a bookmark is removed
     *
     * @param cfi
     */
    public void removeBookmark(long bookid, String cfi) {
        BookmarkHelper.deleteBookmarkByCFI(bookid, cfi);
        mEPub2ReaderJSHelper.removeBookmark(cfi);
    }

    /**
     * Set a highlight at the current position
     */
    public void setHighlight() {
        mEPub2ReaderJSHelper.setHighlight();
    }

    /**
     * Called when a highlight is removed
     *
     * @param cfi
     */
    public void removeHighlight(long bookid, String cfi) {
        BookmarkHelper.deleteHighlightByCFI(bookid, cfi);
        mEPub2ReaderJSHelper.removeHighlight(cfi);
    }

    /**
     * Asynchronously set all the highlights for the given book id in the js library
     *
     * @param bookId
     */
    public void asyncSetJSHighlights(final long bookId) {
        new AsyncTask<Void, Void, Bookmark[]>() {
            @Override
            protected Bookmark[] doInBackground(Void... params) {
                return BookmarkHelper.getAllHighlights(bookId);
            }

            @Override
            protected void onPostExecute(Bookmark[] result) {
                setHighlights(result);
            }
        }.execute();
    }

    /**
     * Set the given highlight array on the reader. Only new highlights will be added and any current highlights not in the array will be removed
     *
     * @param highlights
     */
    public void setHighlights(Bookmark[] highlights) {
        StringBuilder highlightArray = new StringBuilder();
        int count = highlights.length;
        for (int i = 0; i < count; i++) {
            String cfi = highlights[i].position;
            highlightArray.append('"');
            highlightArray.append(cfi);
            highlightArray.append('"');
            if (i < count - 1) {
                highlightArray.append(", ");
            }
        }

        mEPub2ReaderJSHelper.setHighlights(highlightArray.toString());
    }

    /**
     * Sets the content of the webview to point to the given url
     *
     * @param url
     * @param lastPosition
     */
    public void showPreview(String url, Bookmark lastPosition) {
        mEPub2ReaderJSHelper.setPreview(url, lastPosition);
    }

    /**
     * Show or hides and error view which is displayed in place of reader content
     *
     * @param visible
     */
    public void setErrorViewVisible(boolean visible) {
        mEPub2ReaderJSHelper.setErrorViewVisible(visible);
    }
}