// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.helper.LibraryHelper;
import com.blinkboxbooks.android.ui.library.LibraryActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.EmbeddedBookUtils;
import com.blinkboxbooks.android.util.PreferenceManager;

/**
 * The application splash loading screen that displays an animated logo
 */
public class SplashActivity extends BaseActivity {

    /**
     * The maximum amount of time in ms that the splash screen should be displayed for
     */
    private static final long SPLASH_SCREEN_DELAY = 1000;

    private static final int STATE_ANIMATING = 0;
    private static final int STATE_ANIMATED = 1;
    private static final int STATE_LOADED = 2;

    /**
     * Indicates the application has already loaded and the splash screen should be skipped
     */
    private static boolean sApplicationLoaded = false;

    /**
     * Indicates the loading status
     */
    private int mLoadingState = STATE_ANIMATING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case we come in here via a marketing deep link we want to track in Ad-X
        AnalyticsHelper.handleAdXDeepLink(this, getIntent());

        if (sApplicationLoaded) {
            launchApplication();
            return;
        }

        setContentView(R.layout.activity_splash);

        ImageView bbLogo = (ImageView) findViewById(R.id.splash_bb_logo);
        ImageView fromTesco = (ImageView) findViewById(R.id.splash_from_tesco);

        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(SPLASH_SCREEN_DELAY);
        fadeIn.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mLoadingState == STATE_LOADED) {
                    launchApplication();
                } else {
                    mLoadingState = STATE_ANIMATED;
                }
            }
        });
        mLoadingState = STATE_ANIMATING;
        bbLogo.startAnimation(fadeIn);

        // Just create a separate fade animation with no listener so we don't try and launch the app twice
        Animation fadeIn2 = new AlphaAnimation(0.0f, 1.0f);
        fadeIn2.setDuration(SPLASH_SCREEN_DELAY);
        fromTesco.startAnimation(fadeIn2);

        if (EmbeddedBookUtils.shouldExtractBooks(this)) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    EmbeddedBookUtils.loadEmbeddedBooks(SplashActivity.this);
                    LibraryHelper.createAnonymousLibrary();
                    return null;
                }

                protected void onPostExecute(Void result) {
                    if (mLoadingState == STATE_ANIMATED) {
                        launchApplication();
                    } else {
                        mLoadingState = STATE_LOADED;
                    }
                }
            }.execute();
        } else {
            mLoadingState = STATE_LOADED;
        }
    }

    private void launchApplication() {
        sApplicationLoaded = true;

        final Intent intent;

        PreferenceManager pm = PreferenceManager.getInstance();

        boolean shownWelcomePage = pm.getBoolean(PreferenceManager.PREF_KEY_SHOWN_WELCOME_PAGE, false);
        if (!shownWelcomePage ) {
            intent = new Intent(this, WelcomeActivity.class);
            // If we have not previously shown the welcome screen then we know this run is not the first run after an upgrade, therefore we update
            // the stored app version at this point, so we will not display any version info when we get to the library.
            pm.setPreference(PreferenceManager.PREF_KEY_STORED_APP_VERSION, BuildConfig.VERSION_NAME);
        } else {
            // To prevent the user being shown the preload info when they upgrade from an old version we set the persisted value to false
            pm.setPreference(PreferenceManager.PREF_KEY_SHOW_PRELOAD_INFORMATION, false);
            intent = new Intent(this, LibraryActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
