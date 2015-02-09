// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.view.MotionEvent;

import com.blinkboxbooks.android.model.reader.CFI;

/**
 * Interface for allowing a reader to notify of various text selection events
 */
public interface TextSelectionListener {

    /**
     * Handle some text being highlighted
     * @param text the text that was highlighted
     * @param lastTouch the motion event associated with the last touch
     * @return true if the dictionary is being displayed/updated for the supplied text
     */
    public boolean textSelected(String text, MotionEvent lastTouch);
    public boolean highlightSelected(CFI highlight, MotionEvent lastTouch);

    public void actionModeCancelled();
    public void actionModeCreated();
}
