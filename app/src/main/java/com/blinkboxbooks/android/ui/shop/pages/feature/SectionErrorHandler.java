// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.pages.feature;

/**
 * A very simple interface that the top level featured fragment should implement to handle error calls from
 * individual fragments.
 */
public interface SectionErrorHandler {

    /**
     * Called to report an error that occurred within a section
     * @param errorStringResource the resource id of the error string to display
     */
    public void reportError(int errorStringResource);
}