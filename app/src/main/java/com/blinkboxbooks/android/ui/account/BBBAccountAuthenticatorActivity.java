// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBAuthenticationError;
import com.blinkboxbooks.android.api.model.BBBClientInformation;
import com.blinkboxbooks.android.api.model.BBBTokenResponse;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.authentication.AccountAuthenticatorActivity;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.DeviceUtils;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.ValidationUtil;
import com.blinkboxbooks.android.widget.BBBAutoCompleteTextView;
import com.blinkboxbooks.android.widget.BBBEditText;
import com.google.gson.Gson;

import java.net.HttpURLConnection;

/**
 * This class implements most of the functionality expected of an AccountAuthenticatorActivity. Subclasses need only override the methods to return the correct layout/view resource
 * ids and methods to show/hide errors/dialogs. The class takes care of doing the actual requests, validating the data and returning the correct Result to the caller
 */
public abstract class BBBAccountAuthenticatorActivity extends AccountAuthenticatorActivity {

    private static String TAG = BBBAccountAuthenticatorActivity.class.getSimpleName();

    private final static String AUTH_RESPONSE_HANDLER_ID = "login_handler";
    private final static String CLIENT_INFORMATION_RESPONSE_HANDLER_ID = "client_info_handler";

    protected static final int ERROR_CONNECTION = 0;
    protected static final int ERROR_LOGIN = 1;
    protected static final int ERROR_NO_EMAIL = 2;
    protected static final int ERROR_NO_PASSWORD = 3;
    protected static final int ERROR_TOO_MANY_DEVICES = 4;
    protected static final int ERROR_NO = 5;
    protected static final int ERROR_UNKNOWN = 6;
    protected static final int ERROR_EMPTY_FORM = 7;
    protected static final int ERROR_INVALID_EMAIL = 8;

    protected AccountManager mAccountManager;
    protected BBBAutoCompleteTextView mUsernameEditText;
    protected BBBEditText mPasswordEditText;
    protected boolean mRequestNewAccount = false;

    private String mAccessToken, mRefreshToken;
    private String mClientId, mClientSecret;

    private final BBBBasicResponseHandler<BBBClientInformation> clientInformationHandler = new BBBBasicResponseHandler<BBBClientInformation>() {

        public void receivedData(BBBResponse response, BBBClientInformation clientInformation) {

            if (clientInformation != null) {
                mClientId = clientInformation.client_id;
                mClientSecret = clientInformation.client_secret;

                String username = mUsernameEditText.getText().toString().trim();

                if (!TextUtils.isEmpty(mClientId) && !TextUtils.isEmpty(mClientSecret)) {
                    BBBRequest request = BBBRequestFactory.getInstance().createAuthenticateRequest(username, mPasswordEditText.getText().toString().trim(),
                            mClientId, mClientSecret);

                    BBBRequestManager.getInstance().executeRequest(AUTH_RESPONSE_HANDLER_ID, request);
                }
            }
        }

        public void receivedError(BBBResponse response) {

            if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {

                try {
                    BBBAuthenticationError error = new Gson().fromJson(response.getResponseData(), BBBAuthenticationError.class);

                    if (error != null && BBBApiConstants.ERROR_CLIENT_LIMIT_REACHED.equals(error.error_reason)) {
                        hideProgress();
                        showError(ERROR_TOO_MANY_DEVICES);
                    }

                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                }
            }
        }
    };

    private final BBBBasicResponseHandler<BBBTokenResponse> authenticationHandler = new BBBBasicResponseHandler<BBBTokenResponse>() {

        public void receivedData(BBBResponse response, BBBTokenResponse data) {
            mAccessToken = data.access_token;
            mRefreshToken = data.refresh_token;

            String username = mUsernameEditText.getText().toString().trim();

            if (!TextUtils.isEmpty(mAccessToken) && !TextUtils.isEmpty(mRefreshToken)) {
                if (!TextUtils.isEmpty(mClientId) && !TextUtils.isEmpty(mClientSecret)) {
                    AccountController.getInstance().finishLogin(data.user_id, username, data.user_first_name, mAccessToken, mRefreshToken, mClientId, mClientSecret, false);
                    return;
                }

                Account account = AccountController.getInstance().getAccountForEmail(username.trim());

                if (account != null) {
                    String clientId = mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_ID);
                    String clientSecret = mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET);

                    if (!TextUtils.isEmpty(clientSecret)) {
                        AccountController.getInstance().finishLogin(data.user_id, username, data.user_first_name, mAccessToken, mRefreshToken, clientId, clientSecret, false);
                        return;
                    }
                }

                AccountController.getInstance().setAccessToken(mAccessToken);

                BBBRequest request = BBBRequestFactory.getInstance().createRegisterClientRequest(DeviceUtils.getClientName(BBBAccountAuthenticatorActivity.this), DeviceUtils.getClientBrand(),
                        DeviceUtils.getClientModel(), DeviceUtils.getClientOs());
                BBBRequestManager.getInstance().executeRequest(CLIENT_INFORMATION_RESPONSE_HANDLER_ID, request);

            } else {
                showError(ERROR_LOGIN);
            }
        }

        public void receivedError(BBBResponse response) {

            if (response != null) {

                if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                    BBBAuthenticationError error = new Gson().fromJson(response.getResponseData(), BBBAuthenticationError.class);
                    String username = mUsernameEditText.getText().toString().trim();

                    if (error != null && BBBApiConstants.ERROR_INVALID_CLIENT.equals(error.error)) {
                        Account account = AccountController.getInstance().getAccountForEmail(username.trim());

                        if (account != null) {
                            mAccountManager.setUserData(account, BBBApiConstants.PARAM_CLIENT_ID, null);
                            mAccountManager.setUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET, null);
                        }

                        String password = mPasswordEditText.getText().toString().trim();
                        BBBRequest request = BBBRequestFactory.getInstance().createAuthenticateRequest(username, password, null, null);
                        BBBRequestManager.getInstance().executeRequest(AUTH_RESPONSE_HANDLER_ID, request);
                    } else {
                        hideProgress();
                        showError(ERROR_LOGIN);

                        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_INCORRECT_LOGIN, null);
                    }
                } else if(response.getResponseCode() == BBBApiConstants.ERROR_CONNECTION_FAILED) {
                    hideProgress();
                    showError(ERROR_CONNECTION);
                } else {
                    hideProgress();
                    showError(ERROR_UNKNOWN);
                }

            } else {
                hideProgress();
                showError(ERROR_CONNECTION);
            }
        }
    };

    private BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishLogin(intent);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(getLayoutResourceId());

        mUsernameEditText = (BBBAutoCompleteTextView) findViewById(getUsernameEdittextResourceId());
        mPasswordEditText = (BBBEditText) findViewById(getPasswordEdittextResourceId());

        String[] names = AccountController.getInstance().getAccountUsernames();

        if (names != null && names.length > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, names);
            mUsernameEditText.setAdapter(adapter);
            mUsernameEditText.setThreshold(1);
        }

        mAccountManager = AccountManager.get(this);

        Intent intent = getIntent();
        String username = intent.getStringExtra(BBBApiConstants.PARAM_USERNAME);
        String password = intent.getStringExtra(BBBApiConstants.PARAM_PASSWORD);
        mRequestNewAccount = username == null;

        if (!TextUtils.isEmpty(username)) {
            mUsernameEditText.setText(username.trim());
        }

        if (!TextUtils.isEmpty(password)) {
            mPasswordEditText.setText(password.trim());
        }

        mPasswordEditText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handleLogin();
                }

                return false;
            }
        });

        Button submitButton = (Button) findViewById(getSubmitButtonResourceId());

        submitButton.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                handleLogin();
            }
        });
    }

    private void handleLogin() {

        showError(ERROR_NO);
        final String username = mUsernameEditText.getText().toString().trim();

        if (!ValidationUtil.validateEmail(username)) {

            if (!TextUtils.isEmpty(username)) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_WRONG_EMAIL_FORMAT, null);
            } else {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_NO_EMAIL, null);
            }

            showError(ERROR_INVALID_EMAIL);
            return;
        }

        final String password = mPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {

            if (TextUtils.isEmpty(username) && TextUtils.isEmpty(password)) {
                showError(ERROR_EMPTY_FORM);
            } else if (TextUtils.isEmpty(username)) {
                showError(ERROR_NO_EMAIL);
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_NO_EMAIL, null);
            } else if (TextUtils.isEmpty(password)) {
                showError(ERROR_NO_PASSWORD);
            }
        } else {
            String clientId = null;
            String clientSecret = null;

            Account account = AccountController.getInstance().getAccountForEmail(username);

            if (account != null) {
                clientId = mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_ID);
                clientSecret = mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET);
            }

            if(!NetworkUtils.hasInternetConnectivity(this)) {
                showError(ERROR_CONNECTION);
                return;
            }

            showProgress();

            BBBRequest request = BBBRequestFactory.getInstance().createAuthenticateRequest(username, password, clientId, clientSecret);
            BBBRequestManager.getInstance().executeRequest(AUTH_RESPONSE_HANDLER_ID, request);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final BBBRequestManager requestManager = BBBRequestManager.getInstance();
        requestManager.addResponseHandler(AUTH_RESPONSE_HANDLER_ID, authenticationHandler);
        requestManager.addResponseHandler(CLIENT_INFORMATION_RESPONSE_HANDLER_ID, clientInformationHandler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        final BBBRequestManager requestManager = BBBRequestManager.getInstance();
        requestManager.removeResponseHandler(AUTH_RESPONSE_HANDLER_ID);
        requestManager.removeResponseHandler(CLIENT_INFORMATION_RESPONSE_HANDLER_ID);
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mLoginReceiver, new IntentFilter(AccountController.ACTION_LOGGED_IN));

    }

    @Override
    protected void onStop() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mLoginReceiver);

        super.onStop();
    }

    private void finishLogin(Intent intent) {
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Overriding class should implement this to show a loading dialog when called
     */
    public abstract void showProgress();

    /**
     * Overriding class should implement this to hide a loading dialog when called
     */
    public abstract void hideProgress();

    /**
     * Overriding class should implement this to show an error indicating there was a problem with the inputed data
     */
    public abstract void showError(int errorCode);

    /**
     * Should return the resource id of the layout file to use
     */
    public abstract int getLayoutResourceId();

    /**
     * Should return the resource id of an EditText field representing the user name
     */
    public abstract int getUsernameEdittextResourceId();

    /**
     * Should return the resource id of an EditText field representing the user password
     */
    public abstract int getPasswordEdittextResourceId();

    /**
     * Should return the resource id of the login/submit Button
     */
    public abstract int getSubmitButtonResourceId();
}
