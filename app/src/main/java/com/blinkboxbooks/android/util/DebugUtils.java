// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.widget.Toast;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.crashlytics.android.Crashlytics;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for performing debug functions
 */
public class DebugUtils {

    private static HashMap<String, String> mExceptionData = new HashMap<String, String>();

    public static void setJavascriptInit(String javascriptInit) {
        mExceptionData.put("javascript init", javascriptInit);
    }

    public static void setBook(Book book) {
        mExceptionData.clear();
        mExceptionData.put("book isbn", book.isbn);
        mExceptionData.put("book media_path", book.media_path);
    }

    public static void setLastPositionData(Bookmark lastPosition) {
        mExceptionData.put("last position cfi", lastPosition.position);
        mExceptionData.put("last position %", String.valueOf(lastPosition.percentage));
        mExceptionData.put("last position content", lastPosition.content);
    }

    public static void handleCPRException(String message) {
        if (BuildConfig.DEBUG) {
            LogUtils.e("CPR", message);
            Toast.makeText(BBBApplication.getApplication(), message, Toast.LENGTH_LONG).show();
        }

        Exception e = new Exception(message);

        StringBuilder builder = new StringBuilder();
        for (Map.Entry entry : mExceptionData.entrySet()) {
            builder.append(entry.getKey());
            builder.append('=');
            builder.append(entry.getValue());
            builder.append(';');
            builder.append('\n');
        }

        Crashlytics.log("dataMap: "+ builder.toString());
        Crashlytics.logException(e);
    }

    public static void handleException(String message) {
        Exception e = new Exception(message);
        Crashlytics.logException(e);
    }

}
