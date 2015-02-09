// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.account;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBAuthenticationError;
import com.blinkboxbooks.android.api.model.BBBTokenResponse;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.google.gson.Gson;

import java.net.HttpURLConnection;

/**
 * Fragment for allowing a user to confirm their password so that they are in an elevated state
 */
public class ConfirmPasswordFragment extends BaseDialogFragment {

    private static final String RESPONSE_HANDLER_ID = "cpf_response_handler";

    private EditText mEditTextPassword;

    private String mUsername;

    private BaseActivity mOwnerActivity;

    private AlertDialog mDialog;

    /**
     * Creates a new instance of this dialog fragment
     *
     * @return the Fragment
     */
    public static ConfirmPasswordFragment newInstance() {
        return new ConfirmPasswordFragment();
    }

    public ConfirmPasswordFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentName = AnalyticsHelper.GA_SCREEN_Shop_SoftLoginScreen_PasswordConfirmation;

        // If either of these values are null then we should not display the dialog as it will cause a crash when the user tries
        // to confirm the password. This can happen when the confirm password dialog is displayed and the app is backgrounded. When the app is
        // restored the dialog is recreated and when the user hits the buy button it tries to use the data from the AccountController. I was able
        // to repro the issue by killing the app process and deleting the user account (but there may be other ways it could happen as well)
        if (AccountController.getInstance().getClientId() == null || AccountController.getInstance().getClientSecret() == null) {
            dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        BBBRequestManager.getInstance().addResponseHandler(RESPONSE_HANDLER_ID, authenticationHandler);
    }

    @Override
    public void onPause() {
        super.onPause();

        BBBRequestManager.getInstance().removeResponseHandler(RESPONSE_HANDLER_ID);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mOwnerActivity = (BaseActivity) getActivity();

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(mOwnerActivity);
        View layout = View.inflate(mOwnerActivity, R.layout.fragment_confirm_password, null);

        mUsername = AccountController.getInstance().getDataForLoggedInUser(BBBApiConstants.PARAM_USERNAME);

        mEditTextPassword = (EditText) layout.findViewById(R.id.edittext_password);

        Button button = (Button) layout.findViewById(R.id.button_confirm_password);

        button.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String password = mEditTextPassword.getText().toString();

                if (TextUtils.isEmpty(password)) {
                    mOwnerActivity.showMessage(null, getString(R.string.error_password_blank));
                } else {
                    mOwnerActivity.showProgress(R.string.loading);

                    BBBRequest request = BBBRequestFactory.getInstance().createAuthenticateRequest(mUsername, password,
                            AccountController.getInstance().getClientId(), AccountController.getInstance().getClientSecret());

                    BBBRequestManager.getInstance().executeRequest(RESPONSE_HANDLER_ID, request);
                }
            }
        });


        TextView textView = (TextView) layout.findViewById(R.id.textview_forgotten_password);
        textView.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                dismiss();
                AccountController.getInstance().showForgottenPasswordScreen((BaseActivity) getActivity(), mUsername);
            }
        });

        builder.setView(layout);

        mDialog = builder.create();
        return mDialog;
    }

    private final BBBBasicResponseHandler<BBBTokenResponse> authenticationHandler = new BBBBasicResponseHandler<BBBTokenResponse>() {

        public void receivedData(BBBResponse response, BBBTokenResponse data) {
            String mAccessToken = data.access_token;
            String mRefreshToken = data.refresh_token;

            if (!TextUtils.isEmpty(mAccessToken) && !TextUtils.isEmpty(mRefreshToken)) {
                String clientId = AccountController.getInstance().getClientId();
                String clientSecret = AccountController.getInstance().getClientSecret();

                AccountController.getInstance().finishLogin(data.user_id, mUsername, data.user_first_name, mAccessToken, mRefreshToken, clientId, clientSecret, false);

                dismiss();
            }
        }

        public void receivedError(BBBResponse response) {
            if (mOwnerActivity != null && !mOwnerActivity.isDestroyedOrFinishing()) {
                mOwnerActivity.hideProgress();
            }

            if (response != null && response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                BBBAuthenticationError error = new Gson().fromJson(response.getResponseData(), BBBAuthenticationError.class);

                if (error != null && (BBBApiConstants.ERROR_INVALID_CLIENT.equals(error.error))) {
                    dismiss();//simply dismiss the dialog as this is handled elsewhere
                } else {
                    if (mOwnerActivity != null && !mOwnerActivity.isDestroyedOrFinishing()) {
                        mOwnerActivity.showMessage(getString(R.string.error_server_title), getString(R.string.we_cant_find_a_password));
                    }
                }

            } else {
                if (mOwnerActivity != null && !mOwnerActivity.isDestroyedOrFinishing()) {
                    mOwnerActivity.showMessage(getString(R.string.error_server_title), getString(R.string.error_server_message));
                }
            }
        }
    };
}