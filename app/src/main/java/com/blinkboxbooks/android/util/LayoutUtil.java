// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.os.Build;
import android.view.ViewTreeObserver;

public class LayoutUtil {
    /**
     * Utility method for handling the API change to fix a method name typo in API 16.
     * <p/>
     * This removes the specified OnGlobalLayoutListener from the ViewTreeObserver using
     * the correct API call for the API level it is being executed upon.
     *
     * @param observer The ViewTreeObserver from which to remove the OnGlobalLayoutListener
     * @param listener The OnGlobalLayoutListener to be removed
     */
    public static void removeOnGlobalLayoutListener(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (observer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                removeOnGlobalLayoutListenerApi16(observer, listener);
            } else {
                removeOnGlobalLayoutListenerLegacy(observer, listener);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void removeOnGlobalLayoutListenerLegacy(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
        observer.removeGlobalOnLayoutListener(listener);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void removeOnGlobalLayoutListenerApi16(ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
        observer.removeOnGlobalLayoutListener(listener);
    }

    public static int getStatusBarHeight(Resources resources) {
        int result = 0;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }
}