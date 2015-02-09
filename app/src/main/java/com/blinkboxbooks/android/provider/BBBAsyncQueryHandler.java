// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;

import com.blinkboxbooks.android.BBBApplication;

/**
 * Class to perform db queries on a worker thread
 */
public class BBBAsyncQueryHandler extends AsyncQueryHandler {

    private static BBBAsyncQueryHandler sInstance;

    /**
     * Static singleton getInstance
     *
     * @return the {@link BBBAsyncQueryHandler} singleton object
     */
    public static BBBAsyncQueryHandler getInstance() {

        if (sInstance == null) {
            sInstance = new BBBAsyncQueryHandler(BBBApplication.getApplication().getContentResolver());
        }

        return sInstance;
    }

    private BBBAsyncQueryHandler(ContentResolver cr) {
        super(cr);
    }
}
