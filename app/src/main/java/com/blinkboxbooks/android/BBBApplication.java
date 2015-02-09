// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.StrictMode;
import android.text.TextUtils;

import com.AdX.tag.AdXConnect;
import com.android.volley.VolleyLog;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.net.ApiConnector;
import com.blinkboxbooks.android.provider.BBBAsyncQueryHandler;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBImageLoader;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.PRNGFixes;
import com.blinkboxbooks.android.util.PreferenceManager;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.Logger;

import java.io.File;
import java.io.IOException;

import io.fabric.sdk.android.Fabric;

/**
 * Custom Application object so we can perform some basic initialization on startup
 */
public class BBBApplication extends Application {

    private static final String TAG = BBBApplication.class.getSimpleName();

    private static final long HTTP_CACHE_SIZE = 10 * 1024 * 1024; //10 MB

    private static BBBApplication sInstance;

    public BBBApplication() {
        sInstance = this;
    }

    public static BBBApplication getApplication() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics.Builder().disabled(BuildConfig.DEBUG).build());
        LogUtils.setLoggingEnabled(BuildConfig.DEBUG);
        LogUtils.i(TAG,"application onCreate");

        VolleyLog.DEBUG = BuildConfig.DEBUG;
        ApiConnector.getInstance();

        try {
            File httpCacheDir = new File(getCacheDir(), "https");

            HttpResponseCache.install(httpCacheDir, HTTP_CACHE_SIZE);
        } catch (IOException e) {
            LogUtils.i(TAG, "HTTP response cache installation failed:" + e);
        }

        // set to StrictMode to catch accidental disk or network access on the
        // main thread
        if (getResources().getBoolean(R.bool.strictMode)) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
//					.penaltyDeath()
                    .build());
        }



        BBBImageLoader.getInstance().setContext(this);
        BBBAsyncQueryHandler.getInstance();

        try {
            PRNGFixes.apply();
        } catch (Exception e) {
            Crashlytics.logException(e);
        }

        boolean hasLaunched = PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_KEY_HAS_LAUNCHED, false);
        boolean updateFromOldAppWithoutAdx = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_KEY_EMBEDDED_BOOKS_VERSION, 0) > 0;
        LogUtils.i(TAG,"hasLaunched---"+ hasLaunched +"updateFromOldAppWithoutAdx---"+ updateFromOldAppWithoutAdx);
        boolean updateFlag = false;

        if (!hasLaunched && updateFromOldAppWithoutAdx) {
            updateFlag = true;
            LogUtils.i(TAG,"updateFromOldAppWithoutAdx!");
        }

        //specific AdX tracking for Hudl 2
        if(Build.MODEL.contains("Hudl 2")) {
            AdXConnect.setAttribution(getApplicationContext(), "Tesco_Hudl_2", "PreBurn");
        }

        AdXConnect.getAdXConnectInstance(getApplicationContext(),updateFlag, BuildConfig.DEBUG ? Logger.LogLevel.INFO : 0);
        AdXConnect.getAdXConnectEventInstance(getApplicationContext(), AnalyticsHelper.ADX_EVENT_LAUNCH, "", "" );

        PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_HAS_LAUNCHED, true);

        AccountController.getInstance().updateBugSenserUser();
        BBBRequestManager.getInstance().setInterface(AccountController.getInstance());

        String userAgent = System.getProperty("http.agent");

        if(!TextUtils.isEmpty(userAgent)) {

            userAgent = userAgent + ", version: " + BuildConfig.VERSION_NAME;
            BBBRequestManager.getInstance().setUserAgent(userAgent);
        }

        new Loader().start();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        LibraryController.getInstance().init(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Crashlytics.logException(new RuntimeException("onLowMemory"));
    }

    /**
     * Show an activity on top of whatever the current activity is
     *
     * @param clazz the Class of the Activity you want to show
     */
    public void showActivity(Class<?> clazz) {
        Application application = BBBApplication.getApplication();

        Intent dialogIntent = new Intent(application.getApplicationContext(), clazz);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        application.startActivity(dialogIntent);
    }

    /*
     * Ensure we are not doing disk reads on the main thread while loading preferences
     */
    private class Loader extends Thread {

        @Override
        public void run() {
            LibraryController.getInstance().init(getApplicationContext());
        }
    }
}