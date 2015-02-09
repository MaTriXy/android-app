// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppUpgradeHelperTest extends AndroidTestCase {

    private AppUpgradeHelper mHelper;
    private final static String TAG = AppUpgradeHelperTest.class.getSimpleName();

    @Override
    protected void setUp() throws Exception {
        // Calls the base class implementation of this method.
        super.setUp();
        // Gets the context for this test.
        mHelper = new AppUpgradeHelper();
    }

    public void testVersionComparison() {
        String v1a = "1.1.8";
        String v1b = "1.3.2";
        String v2a = "0.5.6";
        String v2b = "533.25.32+at325fk";
        String v3a = "8.23.6-sb34";
        String v3b = "8.24.34";
        String v4a = "5.0.0+34bbdf";
        String v4b = "3.234.23-bcdf43";
        String v5a = "25.34.23";
        String v5b = "25.34.24";
        String v6a = "2.04.25";
        String v6b = "2.3.514+b34";
        String v7a = "1.5.0-alpha";
        String v7b = "1.0.0-x.7.z.92";

        boolean result1 = false, result2 = false, result3 = false, result4 = false, result5 = false, result6 = false, result7 = false;
        try {
            Method compareVersion = AppUpgradeHelper.class.getDeclaredMethod("compareVersion", String.class, String.class);
            compareVersion.setAccessible(true);

            result1 = (Boolean) compareVersion.invoke(mHelper, v1a, v1b);
            result2 = (Boolean) compareVersion.invoke(mHelper, v2a, v2b);
            result3 = (Boolean) compareVersion.invoke(mHelper, v3a, v3b);
            result4 = (Boolean) compareVersion.invoke(mHelper, v4b, v4a);
            result5 = (Boolean) compareVersion.invoke(mHelper, v5a, v5b);
            result6 = (Boolean) compareVersion.invoke(mHelper, v6b, v6a);
            result7 = (Boolean) compareVersion.invoke(mHelper, v7b, v7a);

        } catch (NoSuchMethodException e) {}
          catch (IllegalAccessException e) {}
          catch (InvocationTargetException e) {}

        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        assertTrue(result4);
        assertTrue(result5);
        assertTrue(result6);
        assertTrue(result7);
    }

    public void testJsonProcess() {
        String json1 = "{\n" +
                "    \"ios\": {\n" +
                "        \"minVersion\": \"1.0.0\",\n" +
                "        \"recommendedVersion\": \"1.3.0\",\n" +
                "        \"storeLink\": \"https://itunes.apple.com/gb/app/blinkbox-books-ebook-reader/id815037307?mt=8\",\n" +
                "        \"forceMessageTitle\": {\n" +
                "            \"default\": \"English title\",\n" +
                "            \"en_GB\": \"British title\",\n" +
                "            \"pl_PL\": \"Polski tytuł\"\n" +
                "        },\n" +
                "        \"forceMessage\": {\n" +
                "            \"default\": \"English forced upgrade message\"\n" +
                "        },\n" +
                "        \"friendlyMessageTitle\": {\n" +
                "            \"default\": \"English friendly title\"\n" +
                "        },\n" +
                "        \"friendlyMessage\": {\n" +
                "            \"default\": \"English friendly message\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"android\": {\n" +
                "        \"minVersion\": \"1.0.0\",\n" +
                "        \"recommendedVersion\": \"1.3.0\",\n" +
                "        \"storeLink\": \"https://play.google.com/store/apps/details?id=com.blinkboxbooks.android\",\n" +
                "        \"forceMessageTitle\": {\n" +
                "            \"default\": \"English android title\",\n" +
                "            \"en_GB\": \"British android title\",\n" +
                "            \"pl_PL\": \"Polski android tytuł\"\n" +
                "        },\n" +
                "        \"forceMessage\": {\n" +
                "            \"default\": \"English force message\"\n" +
                "        },\n" +
                "        \"friendlyMessageTitle\": {\n" +
                "            \"default\": \"English friendly title\"\n" +
                "        },\n" +
                "        \"friendlyMessage\": {\n" +
                "            \"default\": \"English message\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        String minVersion = null, recommendedVersion = null, storeLink = null, forceMessageTitle = null, forceMessage = null, friendlyMessageTitle = null;

        try {

            Method processUpgradeData = AppUpgradeHelper.class.getDeclaredMethod("processUpgradeData", String.class);
            processUpgradeData.setAccessible(true);

            processUpgradeData.invoke(mHelper, json1);

            Field minVersionField =  mHelper.getClass().getDeclaredField("mMinVersion"); minVersionField.setAccessible(true);
            Field recommendedVersionField =  mHelper.getClass().getDeclaredField("mRecommendedVersion"); recommendedVersionField.setAccessible(true);
            Field storeLinkField =  mHelper.getClass().getDeclaredField("mStoreLink"); storeLinkField.setAccessible(true);
            Field forceMessageTitleField =  mHelper.getClass().getDeclaredField("mForceMessageTitle"); forceMessageTitleField.setAccessible(true);
            Field forceMessageField =  mHelper.getClass().getDeclaredField("mForceMessage"); forceMessageField.setAccessible(true);
            Field friendlyMessageTitleField =  mHelper.getClass().getDeclaredField("mFriendlyMessageTitle"); friendlyMessageTitleField.setAccessible(true);

            minVersion = (String) minVersionField.get(mHelper);
            recommendedVersion = (String) recommendedVersionField.get(mHelper);
            storeLink = (String) storeLinkField.get(mHelper);
            forceMessageTitle = (String) forceMessageTitleField.get(mHelper);
            forceMessage = (String) forceMessageField.get(mHelper);
            friendlyMessageTitle = (String) friendlyMessageTitleField.get(mHelper);

        } catch (NoSuchMethodException e) {LogUtils.d(TAG, e.toString());}
        catch (IllegalAccessException e) {LogUtils.d(TAG, e.toString());}
        catch (InvocationTargetException e) {LogUtils.d(TAG, e.toString());}
        catch (NoSuchFieldException e) {LogUtils.d(TAG, e.toString());}

        assertEquals("1.0.0", minVersion);
        assertEquals("1.3.0", recommendedVersion);
        assertEquals("https://play.google.com/store/apps/details?id=com.blinkboxbooks.android", storeLink);
        assertEquals("English android title", forceMessageTitle);
        assertEquals("English force message", forceMessage);
        assertEquals("English friendly title", friendlyMessageTitle);
    }

}
