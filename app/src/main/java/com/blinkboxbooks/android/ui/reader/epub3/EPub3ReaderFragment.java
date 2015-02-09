// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub3;

import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.ui.reader.ReaderFragment;
import com.blinkboxbooks.android.ui.reader.TextSelectionListener;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingsFragment;

public class EPub3ReaderFragment extends ReaderFragment {

    public EPub2ReaderSettingsFragment getReaderSettingFragment() {
        return null;
    }

    @Override
    public void setTextSelectionListener(TextSelectionListener textSelectionListener) {
    }

    @Override
    public void setBook(BBBEPubBook ebook, Book book, Bookmark lastPosition, String bookmarks, String highlights, boolean isOnlineSample) {
    }

    @Override
    public void setBook(Book book){
    }

    @Override
    public void bookmarkPage() {
    }

    @Override
    public void goToBookmark(Bookmark bookmark, boolean keepCurrentReadingPosition) {
    }

    @Override
    public void highlightSelection(String content) {
    }

    @Override
    public void removeHighlight(String cfi) {
    }

    @Override
    public void toggleReaderOverlay(boolean visible) {
    }

    @Override
    public void displayVersion() {
    }

    @Override
    public void goToChapter(String url, boolean keepCurrentReadingPosition) {
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void setInteractionAllowed(boolean allowInteraction) {
    }

    @Override
    public void hideTextSelector() {
    }

    @Override
    public void goToProgress(float progress) {

    }

    @Override
    public void setPreview(String url, Bookmark lastReadingPosition)  {

    }
}