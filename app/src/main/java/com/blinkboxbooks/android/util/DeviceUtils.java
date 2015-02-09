// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Helper class for performing device related functions
 */
public class DeviceUtils {

    /**
     * Gets a friendly device name as set by the bluetooth name. If the device does not support bluetooth or a name has not been set, this method will return the device model.
     *
     * @param context any Context object
     * @return the friendly name
     */
    public static String getClientName(Context context) {
        return Build.MODEL;
    }

    /**
     * Returns the device model name
     *
     * @return the device model name
     */
    public static String getClientModel() {
        return Build.MODEL;
    }

    /**
     * Returns the device brand
     *
     * @return the device brand
     */
    public static String getClientBrand() {
        return Build.BRAND;
    }

    public static String getClientOs() {
        return "Android " + Build.VERSION.SDK_INT;
    }

    /*
     * Checks whether the device has a usable camera for barcode scanning
     * return true, if the device has a camera
     */
    public static boolean hasCameraFeature(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
}
