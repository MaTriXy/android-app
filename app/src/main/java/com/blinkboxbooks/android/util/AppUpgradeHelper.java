// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Sends appropriate forced/friendly upgrade reminder via callbacks if required;
 * Fetches upgrade information if time elapsed since last check exceeds 24 hours
 * and then compares the current app version code with the upgrade information.
 */
public class AppUpgradeHelper {

    //JSON file element identifiers and Preference Keys
    private static final String MIN_VERSION = "minVersion";
    private static final String RECOMMENDED_VERSION = "recommendedVersion";
    private static final String STORE_LINK = "storeLink";
    private static final String FRIENDLY_MESSAGE_TITLE = "friendlyMessageTitle";
    private static final String FORCE_MESSAGE_TITLE = "forceMessageTitle";
    private static final String FORCE_MESSAGE = "forceMessage";

    private static final String ANDROID = "android";
    private static final String DEFAULT = "default";

    //Upgrade information strings
    private String mMinVersion;
    private String mRecommendedVersion;
    private String mStoreLink;
    private String mFriendlyMessageTitle;
    private String mForceMessageTitle;
    private String mForceMessage;

    private boolean mShowFriendly = true;

    //Preference keys
    private static final String PREF_KEY_TIME_SINCE_CHECK = "time_since_check";
    private static final String PREF_KEY_RECOMMENDED_VERSION = "recommended_version";

    private static final int MILLIS_IN_DAY = 86400000;

    private static AppUpgradeHelper sInstance = null;

    private AppUpgradeCallback mCallback;

    /**
     * Static singleton class structure
     */
    public static AppUpgradeHelper getInstance() {
        if (sInstance == null) {
            sInstance = new AppUpgradeHelper();
        }
        return sInstance;
    }

    /**
     * Set whether the friendly upgrade reminder show should if required
     * @param show will show friendly upgrade reminder if 'show' is true (if applicable) and vice versa
     */
    public void showFriendlyUpgrade(boolean show) {
        mShowFriendly = show;
    }

    /**
     * Initiates a series of methods that will send appropriate upgrade callbacks as required; Downloads
     * the upgrade information file if the last download was in excess of 24hours ago. Checks the stored
     *  minimum and recommended version codes and sends appropriate reminder callbacks
     * @param urlString
     * @param callback
     */
    public void init(String urlString, AppUpgradeCallback callback) {
        if(urlString != null && callback != null) {
            mCallback = callback;

            if(shouldFetchUpgradeData()) {
                new FetchUpgradeDataAsyncTask().execute(getURL(urlString));
            }

            if (mMinVersion == null){
                readUpgradeInfoFromPrefs();
            }

            processVersionCodes();
        }
    }

    private boolean shouldFetchUpgradeData(){
        long lastTime = PreferenceManager.getInstance().getLong(PREF_KEY_TIME_SINCE_CHECK, 0);
        return (System.currentTimeMillis() - lastTime) > (MILLIS_IN_DAY);
    }

    private void storeLastFetchTime(){
        PreferenceManager.getInstance().setPreference(PREF_KEY_TIME_SINCE_CHECK, System.currentTimeMillis());
    }

    private String fetchUpgradeData(URL url){

        BBBRequest request = new BBBRequest(url.getProtocol() + "://" + url.getHost(), url.getPath(), false);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        return (response.getResponseCode() == HttpURLConnection.HTTP_OK) ? response.getResponseData() : "";
    }

    /**
     * Returns true if data successfully processed
     * @param json
     * @return
     */
    private boolean processUpgradeData(String json){
        boolean processSuccess  = false;
        try {
            JSONObject upgradeJson = new JSONObject(json);
            upgradeJson = upgradeJson.getJSONObject(ANDROID);

            mMinVersion = upgradeJson.getString(MIN_VERSION);
            mRecommendedVersion = upgradeJson.getString(RECOMMENDED_VERSION);
            mStoreLink = upgradeJson.getString(STORE_LINK);

            PreferenceManager.getInstance().setPreference(MIN_VERSION, mMinVersion);
            PreferenceManager.getInstance().setPreference(RECOMMENDED_VERSION, mRecommendedVersion);
            PreferenceManager.getInstance().setPreference(STORE_LINK, mStoreLink);

            String locale = BBBApplication.getApplication().getResources().getConfiguration().locale.toString();
            processSuccess = (getUpgradeInfoMessages(upgradeJson, locale)) ? true : getUpgradeInfoMessages(upgradeJson, DEFAULT);

        } catch (JSONException e) {}

        return processSuccess;
    }

    private boolean getUpgradeInfoMessages(JSONObject json, String locale) {
        try {
            mFriendlyMessageTitle = json.getJSONObject(FRIENDLY_MESSAGE_TITLE).getString(locale);
            mForceMessageTitle = json.getJSONObject(FORCE_MESSAGE_TITLE).getString(locale);
            mForceMessage = json.getJSONObject(FORCE_MESSAGE).getString(locale);

            PreferenceManager.getInstance().setPreference(FRIENDLY_MESSAGE_TITLE, mFriendlyMessageTitle);
            PreferenceManager.getInstance().setPreference(FORCE_MESSAGE_TITLE, mForceMessageTitle);
            PreferenceManager.getInstance().setPreference(FORCE_MESSAGE, mForceMessage);

        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    /**
     * Compares the given version codes and returns true if the first version code is smaller than the second
     * @return true if first versionCode is smaller than the second versionCode
     */
    private boolean compareVersion(String versionCode1, String versionCode2) {
        int major = 0;
        int minor = 1;
        int patch = 2;

        try {
            String[] v1Array = versionCode1.split("\\.");
            String[] v2Array = versionCode2.split("\\.");

            if (v1Array.length >= 3 && v2Array.length >= 3) {
                v1Array[2] = v1Array[2].split("[+-]")[0];
                v2Array[2] = v2Array[2].split("[+-]")[0];

                int major1 = Integer.valueOf((v1Array[major]));
                int minor1 = Integer.valueOf((v1Array[minor]));
                int patch1 = Integer.valueOf((v1Array[patch]));

                int major2 = Integer.valueOf((v2Array[major]));
                int minor2 = Integer.valueOf((v2Array[minor]));
                int patch2 = Integer.valueOf((v2Array[patch]));

                if (major1 != major2) {
                    return major1 < major2;
                } else if (minor1 != minor2) {
                    return minor1 < minor2;
                } else if (patch1 != patch2) {
                    return patch1 < patch2;
                }
            }
        } catch (NumberFormatException e) {}

        return false;
    }

    private URL getURL(String url) {
        URL newUrl = null;
        try {
            newUrl = new URL(url);
        } catch (MalformedURLException e) {}
        return newUrl;
    }

    private class FetchUpgradeDataAsyncTask extends AsyncTask<URL, Void, String> {
        @Override
        protected String doInBackground(URL... params) {

            if (params[0] != null) {

                return fetchUpgradeData(params[0]);
            }
            return "";
        }

        @Override
        protected void onPostExecute(String json) {
            if (processUpgradeData(json)) {
                storeLastFetchTime();
                storeRecommendedVersion();
            }
        }
    }
    private void storeRecommendedVersion() {
        if (compareVersion(
            PreferenceManager.getInstance().getString(PREF_KEY_RECOMMENDED_VERSION, mRecommendedVersion), mRecommendedVersion) ) {
            PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_SHOW_REMINDER, true);
        }
        PreferenceManager.getInstance().setPreference(PREF_KEY_RECOMMENDED_VERSION, PreferenceManager.getInstance().getString(RECOMMENDED_VERSION, ""));
    }

    private void processVersionCodes() {
        String minVersion = PreferenceManager.getInstance().getString(MIN_VERSION, "");
        if (TextUtils.isEmpty(minVersion) || mCallback == null) { return; }

        if (compareVersion(BuildConfig.VERSION_NAME, mMinVersion)) {
            mCallback.forceUpgradeReminder(mStoreLink, mForceMessageTitle, mForceMessage);

        } else if (compareVersion(BuildConfig.VERSION_NAME, mRecommendedVersion) && mShowFriendly) {
            mCallback.friendlyUpgradeReminder(mStoreLink, mFriendlyMessageTitle);
        }
    }

    private void readUpgradeInfoFromPrefs() {
        mMinVersion = PreferenceManager.getInstance().getString(MIN_VERSION, "");
        mRecommendedVersion = PreferenceManager.getInstance().getString(RECOMMENDED_VERSION, "");
        mStoreLink = PreferenceManager.getInstance().getString(STORE_LINK, "");
        mFriendlyMessageTitle = PreferenceManager.getInstance().getString(FRIENDLY_MESSAGE_TITLE, "");
        mForceMessageTitle = PreferenceManager.getInstance().getString(FORCE_MESSAGE_TITLE, "");
        mForceMessage = PreferenceManager.getInstance().getString(FORCE_MESSAGE, "");
    }

    /**
     * Implementations of this interface handle forced/friendly upgrade reminder callbacks
     */
    public interface AppUpgradeCallback {
        /**
         * Initiates friendly prompt to the user to upgrade app
         * @param url
         * @param friendlyMessageTitle
         */
        public void friendlyUpgradeReminder(String url, String friendlyMessageTitle);

        /**
         * Initiates forced prompt to the user to upgrade app
         * @param url
         * @param forceMessageTitle
         * @param forceMessage
         */
        public void forceUpgradeReminder(String url, String forceMessageTitle, String forceMessage);
    }
}


