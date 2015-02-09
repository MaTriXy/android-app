// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;

import com.blinkbox.java.book.json.BBBSpineItem;
import com.blinkboxbooks.android.model.Bookmark;

import java.util.ArrayList;

/**
 * Interface for notifying a ReaderFragment container of certain events
 */
public interface EPub2ReaderCallback {

    /**
     * notify the overlay visibility should toggle it's current state
     */
    public void toggleReaderOverlayVisiblity();

    /**
     * notify the overlay visibility should be set to the specified state
     * @param overlayOn set to true to enable the overlay (and false to hide it)
     */
    public void setReaderOverlayVisibility(boolean overlayOn);

    /**
     * notifiy the reader the user has turned past the last page of the book
     */
    public void bookFinished();

    /**
     * the current reading position has changed
     */
    public void currentReadingPositionUpdated(Bookmark readingPosition, String chapter);

    /**
     * the current page bookmark status has changed
     */
    public void bookmarkStatus(boolean bookmarked);

    /**
     * set the spine list for this particular book
     * @param spineItems spine list for given book
     */
    public void setSpine(ArrayList<BBBSpineItem> spineItems);
}
