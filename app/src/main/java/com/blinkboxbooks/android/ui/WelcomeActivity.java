// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.ui.account.LoginActivity;
import com.blinkboxbooks.android.ui.library.LibraryActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.PreferenceManager;

/**
 * The welcome screen shown to first time users of the application
 * <p/>
 * ALA-195 - Welcome Page (16.0.0.0)
 */
public class WelcomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mScreenName = AnalyticsHelper.GA_SCREEN_App_WelcomeScreen;
    }

    public void tryItOutPressed(View view) {
        PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_SHOWN_WELCOME_PAGE, true);

        startActivity(new Intent(WelcomeActivity.this, LibraryActivity.class));
        finish();
    }

    public void signInOrRegisterPressed(View view) {
        PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_SHOWN_WELCOME_PAGE, true);

        startActivity(new Intent(WelcomeActivity.this, LibraryActivity.class));
        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        finish();
    }
}