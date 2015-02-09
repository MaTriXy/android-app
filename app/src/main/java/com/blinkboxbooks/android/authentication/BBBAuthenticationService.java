// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Authentication service allowing users to authenticate and login to accounts from outside the application
 */
public class BBBAuthenticationService extends Service {

    private BBBAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new BBBAuthenticator(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}