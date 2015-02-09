// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBAuthenticationError;
import com.blinkboxbooks.android.api.model.BBBClientInformation;
import com.blinkboxbooks.android.api.model.BBBTokenResponse;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.dialog.AlertDialogFragment;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.DeviceUtils;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.ValidationUtil;
import com.blinkboxbooks.android.widget.BBBAutoCompleteTextView;
import com.blinkboxbooks.android.widget.BBBEditText;
import com.google.gson.Gson;

import java.net.HttpURLConnection;

/**
 * Fragment allowing user to login with username and password
 */
public class LoginFragment extends BaseDialogFragment {

    private static final String PARAM_USERNAME = "username";

    /**
     * Creates a new instance of this dialog fragment
     *
     * @return the Fragment
     */
    public static LoginFragment newInstance(String username) {
        LoginFragment fragment = new LoginFragment();

        if (!TextUtils.isEmpty(username)) {
            Bundle bundle = new Bundle();
            bundle.putString(PARAM_USERNAME, username);
            fragment.setArguments(bundle);
        }

        return fragment;
    }

    private static final String TAG = LoginFragment.class.getSimpleName();

    private static final String TAG_ERROR_DIALOG = "error_dialog";

    private final static String AUTH_RESPONSE_HANDLER_ID = "login_handler";
    private final static String CLIENT_INFORMATION_RESPONSE_HANDLER_ID = "client_info_handler";

    private static final int ERROR_CONNECTION = 0;
    private static final int ERROR_LOGIN = 1;
    private static final int ERROR_NO_EMAIL = 2;
    private static final int ERROR_NO_PASSWORD = 3;
    private static final int ERROR_TOO_MANY_DEVICES = 4;
    private static final int ERROR_NO = 5;
    private static final int ERROR_UNKNOWN = 6;
    private static final int ERROR_EMPTY_FORM = 7;
    private static final int ERROR_INVALID_EMAIL = 8;

    private AccountManager mAccountManager;

    private BBBAutoCompleteTextView mUsernameEditText;
    private BBBEditText mPasswordEditText;
    private View mViewForgottenPassword;
    private TextView mTextViewError;
    private ProgressBar mProgressBar;
    private LinearLayout mLinearLayoutPasswordError;

    private String mAccessToken, mRefreshToken;
    private String mClientId, mClientSecret;

    public LoginFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentName = AnalyticsHelper.GA_SCREEN_Shop_SignInScreen;
    }

    @Override
    public void onResume() {
        super.onResume();
        final BBBRequestManager requestManager = BBBRequestManager.getInstance();
        requestManager.addResponseHandler(AUTH_RESPONSE_HANDLER_ID, authenticationHandler);
        requestManager.addResponseHandler(CLIENT_INFORMATION_RESPONSE_HANDLER_ID, clientInformationHandler);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        manager.registerReceiver(mLoginReceiver, new IntentFilter(AccountController.ACTION_LOGGED_IN));
    }

    @Override
    public void onPause() {
        super.onPause();
        final BBBRequestManager requestManager = BBBRequestManager.getInstance();
        requestManager.removeResponseHandler(AUTH_RESPONSE_HANDLER_ID);
        requestManager.removeResponseHandler(CLIENT_INFORMATION_RESPONSE_HANDLER_ID);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        manager.unregisterReceiver(mLoginReceiver);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAccountManager = AccountManager.get(getActivity());

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity(), true);

        View view = View.inflate(getActivity(), R.layout.fragment_login, null);

        mUsernameEditText = (BBBAutoCompleteTextView) view.findViewById(R.id.edittext_email);
        mPasswordEditText = (BBBEditText) view.findViewById(R.id.edittext_password);
        mViewForgottenPassword = view.findViewById(R.id.textview_forgotten_password);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        mTextViewError = (TextView) view.findViewById(R.id.textview_error);

        mLinearLayoutPasswordError = (LinearLayout) view.findViewById(R.id.layout_password_error);

        String[] names = AccountController.getInstance().getAccountUsernames();

        if (names != null && names.length > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, names);
            mUsernameEditText.setAdapter(adapter);
            mUsernameEditText.setThreshold(1);
        }

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(PARAM_USERNAME)) {
            mUsernameEditText.setText(bundle.getString(PARAM_USERNAME).trim());
        }

        mViewForgottenPassword.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                dismiss();
                AccountController.getInstance().showForgottenPasswordScreen((BaseActivity) getActivity(), mUsernameEditText.getText().toString().trim());
            }
        });

        Button buttonSignin = (Button) view.findViewById(R.id.button_signin);
        buttonSignin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                signInPressed();
            }
        });

        Button buttonRegister = (Button) view.findViewById(R.id.button_register);
        buttonRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                dismiss();
                AccountController.getInstance().showRegisterScreen((BaseActivity) getActivity());
            }
        });

        mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    signInPressed();
                    return true;
                }
                return false;
            }
        });

        builder.setView(view);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return alertDialog;
    }

    private void signInPressed() {
        showError(ERROR_NO);

        String username = mUsernameEditText.getText().toString().trim();

        mUsernameEditText.setErrorState(false);

        if (!ValidationUtil.validateEmail(username)) {

            if (!TextUtils.isEmpty(username)) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_WRONG_EMAIL_FORMAT, null);
            } else {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_NO_EMAIL, null);
            }

            showError(ERROR_INVALID_EMAIL);
            return;
        }

        String password = mPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            boolean usernameValid = !TextUtils.isEmpty(username);
            mUsernameEditText.setErrorState(!usernameValid);

            boolean passwordValid = !TextUtils.isEmpty(password);
            mPasswordEditText.setErrorState(!passwordValid);

            if (!usernameValid && !passwordValid) {
                showError(ERROR_EMPTY_FORM);
            } else if (!usernameValid) {
                showError(ERROR_NO_EMAIL);
            } else if (!passwordValid) {
                showError(ERROR_NO_PASSWORD);
            }
        } else {
            String clientId = null;
            String clientSecret = null;

            Account account = AccountController.getInstance().getAccountForEmail(username.trim());

            if (account != null) {
                clientId = mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_ID);
                clientSecret = mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET);
            }

            if(!NetworkUtils.hasInternetConnectivity(getActivity())) {
                showError(ERROR_CONNECTION);
                return;
            }

            showProgress();

            BBBRequest request = BBBRequestFactory.getInstance().createAuthenticateRequest(username, password, clientId, clientSecret);
            BBBRequestManager.getInstance().executeRequest(AUTH_RESPONSE_HANDLER_ID, request);
        }
    }

    private void showError(int errorCode) {
        mLinearLayoutPasswordError.setVisibility(View.GONE);
        mPasswordEditText.setErrorState(false);

        if(errorCode != ERROR_CONNECTION && errorCode != ERROR_UNKNOWN) {
            mTextViewError.setVisibility(View.VISIBLE);
            mUsernameEditText.setErrorState(true);
        } else {
            mTextViewError.setVisibility(View.INVISIBLE);
            mUsernameEditText.setErrorState(false);
        }

        if (errorCode == ERROR_NO) {
            mTextViewError.setVisibility(View.INVISIBLE);
        }
        if (errorCode == ERROR_LOGIN) {
            mTextViewError.setText(R.string.we_cant_find_a_password);
        } else if (errorCode == ERROR_NO_EMAIL || errorCode == ERROR_INVALID_EMAIL) {
            mTextViewError.setText(R.string.error_blank_email);
        } else if (errorCode == ERROR_NO_PASSWORD) {
            mLinearLayoutPasswordError.setVisibility(View.VISIBLE);
            mPasswordEditText.setErrorState(true);
        } else if (errorCode == ERROR_UNKNOWN) {
            final AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.error_server_title), getString(R.string.error_server_message));
            ((BaseActivity) getActivity()).showDialog(alertDialogFragment, TAG_ERROR_DIALOG, false);
        } else if (errorCode == ERROR_TOO_MANY_DEVICES) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_EXCEEDED_DEVICE_LIMIT, null);
            mTextViewError.setText(R.string.youve_already_signed_in_on);
        } else if (errorCode == ERROR_CONNECTION) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_NO_NETWORK, null);

            final AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.title_device_offline), getString(R.string.error_connection_login_body));
            ((BaseActivity) getActivity()).showDialog(alertDialogFragment, TAG_ERROR_DIALOG, false);
        } else if (errorCode == ERROR_EMPTY_FORM) {
            mLinearLayoutPasswordError.setVisibility(View.VISIBLE);
            mPasswordEditText.setErrorState(true);
            mTextViewError.setText(R.string.error_blank_email);
        }
    }

    private void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
        mLinearLayoutPasswordError.setVisibility(View.GONE);
    }

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
    }

    private final BBBBasicResponseHandler<BBBClientInformation> clientInformationHandler = new BBBBasicResponseHandler<BBBClientInformation>() {

        public void receivedData(BBBResponse response, BBBClientInformation clientInformation) {

            if (clientInformation != null) {
                mClientId = clientInformation.client_id;
                mClientSecret = clientInformation.client_secret;

                if (!TextUtils.isEmpty(mClientId) && !TextUtils.isEmpty(mClientSecret)) {
                    String username = mUsernameEditText.getText().toString().trim();
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

            if (!TextUtils.isEmpty(mAccessToken) && !TextUtils.isEmpty(mRefreshToken)) {
                String username = mUsernameEditText.getText().toString().trim();

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

                BBBRequest request = BBBRequestFactory.getInstance().createRegisterClientRequest(DeviceUtils.getClientName(getActivity()), DeviceUtils.getClientBrand(),
                        DeviceUtils.getClientModel(), DeviceUtils.getClientOs());
                BBBRequestManager.getInstance().executeRequest(CLIENT_INFORMATION_RESPONSE_HANDLER_ID, request);

            } else {
                showError(ERROR_LOGIN);
            }
        }

        public void receivedError(BBBResponse response) {

            if (response != null) {
                String username = mUsernameEditText.getText().toString().trim();

                if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                    BBBAuthenticationError error = new Gson().fromJson(response.getResponseData(), BBBAuthenticationError.class);

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

    private void finishLogin(Intent intent) {
        dismiss();
    }
}
