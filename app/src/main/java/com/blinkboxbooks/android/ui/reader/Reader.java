// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import com.blinkbox.java.book.model.BBBEPubBook;

//interface from footer
public interface Reader {

    /**
     * Gets the BBBEPubBook from the Reader
     * @return
     */
    public BBBEPubBook getEPubBook();

    /**
     * Tells the reader to go to a chapter
     *
     * @param url
     * @param keepReadingPosition
     */
    public void goToChapter(String url, boolean keepReadingPosition);

    /**
     * Tells the reader to go to the specified percentage progress
     * @param progress
     */
    public void goToProgress(float progress);
}
