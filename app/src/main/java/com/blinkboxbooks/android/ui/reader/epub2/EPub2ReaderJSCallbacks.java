// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;


import com.blinkboxbooks.android.model.reader.Event;

/**
 * Interface defining all the callbacks that can be received from the Javascript library
 */
public interface EPub2ReaderJSCallbacks {

    public void event(Event event);

}