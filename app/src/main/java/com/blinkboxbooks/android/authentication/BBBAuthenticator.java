// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBTokenResponse;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.ui.account.LoginActivity;
import com.blinkboxbooks.android.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Implementation of AbstractAccountAuthenticator. Subclasses only need to override the getLaunchAuthenticatorActivityIntent() method to return an Intent which will launch
 * an Activity which is a subclass of AccountAuthenticatorActivity
 */
public class BBBAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = BBBAuthenticator.class.getSimpleName();

    private final Context mContext;

    public BBBAuthenticator(Context context) {
        super(context);

        mContext = context;
    }

    /**
     * {inheritDoc}
     */
    public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    /**
     * {inheritDoc}
     */
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    /**
     * {inheritDoc}
     */
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String accountType) {
        throw new UnsupportedOperationException();
    }

    /**
     * {inheritDoc}
     */
    public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        AccountManager am = AccountManager.get(mContext);

        //first check to see if we already have an access token in the AccountManager cache
        String accessToken = am.peekAuthToken(account, authTokenType);

        if (accessToken != null) {
            Bundle result = new Bundle();

            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);

            return result;
        }

        //if we don't have a valid access token we try and get a new one with the refresh token
        String refreshToken = am.getUserData(account, BBBApiConstants.PARAM_REFRESH_TOKEN);
        String clientId = am.getUserData(account, BBBApiConstants.PARAM_CLIENT_ID);
        String clientSecret = am.getUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET);

        if (!TextUtils.isEmpty(refreshToken)) {
            BBBRequest request = BBBRequestFactory.getInstance().createGetRefreshAuthTokenRequest(refreshToken, clientId, clientSecret);
            BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

            if (response == null) {
                throw new NetworkErrorException("Could not get auth token with refresh token");
            }

            String json = response.getResponseData();

            BBBTokenResponse authenticationResponse = null;

            if (json != null) {

                try {
                    authenticationResponse = new Gson().fromJson(json, BBBTokenResponse.class);
                } catch (JsonSyntaxException e) {
                    LogUtils.d(TAG, e.getMessage(), e);
                }
            }

            if (authenticationResponse != null) {
                accessToken = authenticationResponse.access_token;
                refreshToken = authenticationResponse.refresh_token;

                if (!TextUtils.isEmpty(accessToken)) {
                    Bundle result = new Bundle();

                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, BBBApiConstants.AUTHTOKEN_TYPE);
                    result.putString(AccountManager.KEY_AUTHTOKEN, authenticationResponse.access_token);

                    AccountController.getInstance().setAccessToken(account, accessToken);
                    AccountController.getInstance().setRefreshToken(account, refreshToken);

                    return result;
                }
            }
        }

        //if we can't get an access token via the cache or by using the refresh token we must return an Intent which will launch an Activity allowing the user to perform manual authentication
        Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        intent.putExtra(BBBApiConstants.PARAM_USERNAME, account.name);
        intent.putExtra(BBBApiConstants.PARAM_AUTHTOKEN_TYPE, authTokenType);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    /**
     * {inheritDoc}
     */
    public String getAuthTokenLabel(String authTokenType) {
        //we don't need to display the authTokenType in the account manager so we return null
        return null;
    }

    /**
     * {inheritDoc}
     */
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] features) throws NetworkErrorException {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    /**
     * {inheritDoc}
     */
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }
}