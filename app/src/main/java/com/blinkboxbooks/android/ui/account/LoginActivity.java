// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.account;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.dialog.AlertDialogFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.BBBUriManager;
import com.blinkboxbooks.android.util.BBBUriManager.BBBUri;

/**
 * Sign in 16.2.0.0
 * <p/>
 * This activity allows an existing user to sign in to an existing account
 */
public class LoginActivity extends BBBAccountAuthenticatorActivity {
    public static final String EXTRA_LOGGED_OUT = "logged_out";
    public static final String ACTION_LOGGED_OUT = "logged_out";

    public static final String EXTRA_PREPOPULATE_EMAIL = "prepopulate_email";

    private static final String TAG_ERROR_DIALOG = "error_dialog";
    private static final String KEY_ERROR_CODE = "error_code";
    private static final String KEY_PROGRESS = "progress";
    private static final int NO_ERROR_STATE = -1;
    private int mErrorCode = NO_ERROR_STATE;
    private boolean mProgressShown;

    private LinearLayout mLinearLayoutSignInError;
    private LinearLayout mLinearLayoutPasswordError;
    private TextView mButtonSendReset;
    private SwitchCompat mSwitchShowPassword;
    private TextView mTextViewForgottenPassword;
    private TextView mTextViewError;
    private TextView mTextViewErrorGeneric;
    private Button mButtonRegister;
    private View mProgressBar;
    private EditText mEditTextUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case we come in here via a marketing deep link we want to track in Ad-X
        AnalyticsHelper.handleAdXDeepLink(this, getIntent());

        mEditTextUsername = (EditText) findViewById(R.id.edittext_email);

        mLinearLayoutSignInError = (LinearLayout) findViewById(R.id.layout_sign_in_error);
        mLinearLayoutPasswordError = (LinearLayout) findViewById(R.id.layout_password_error);

        mButtonSendReset = (TextView) findViewById(R.id.button_send_reset);

        mSwitchShowPassword = (SwitchCompat) findViewById(R.id.switch_show_password);
        mSwitchShowPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPasswordEditText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    mPasswordEditText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        mTextViewErrorGeneric = (TextView) findViewById(R.id.textview_error_generic);
        mTextViewError = (TextView) findViewById(R.id.textview_error);
        mTextViewForgottenPassword = (TextView) findViewById(R.id.textview_forgotten_password);
        mTextViewForgottenPassword.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        mTextViewForgottenPassword.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AccountController.getInstance().showForgottenPasswordScreen(LoginActivity.this, mEditTextUsername.getText().toString().trim());
            }
        });

        mButtonRegister = (Button) findViewById(R.id.button_register);
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        mProgressBar = findViewById(R.id.progressbar);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(KEY_PROGRESS)) {
                showProgress();
            }

            final int errorState = savedInstanceState.getInt(KEY_ERROR_CODE, NO_ERROR_STATE);
            if (errorState != NO_ERROR_STATE) {
                showError(errorState);
            }
        }

        if (getIntent().hasExtra(ACTION_LOGGED_OUT)) {
            BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(this);
            builder.setTitle(getString(R.string.dialog_please_sign_in));
            builder.setMessage(getString(R.string.dialog_you_have_been_signed_out_of_blinkbox_books));
            builder.setPositiveButton(getString(R.string.button_close), null);
            builder.show();
        }

        String prePopulateEmail = getIntent().getStringExtra(EXTRA_PREPOPULATE_EMAIL);

        if (prePopulateEmail != null) {
            mEditTextUsername.setText(prePopulateEmail);
        }

        if (savedInstanceState == null || (savedInstanceState != null && !savedInstanceState.getBoolean(EXTRA_LOGGED_OUT))) {
            AccountController.getInstance().performLogout();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mScreenName = AnalyticsHelper.GA_SCREEN_Library_SignInScreen;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:

                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                    upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(upIntent);
                }
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ERROR_CODE, mErrorCode);
        outState.putBoolean(KEY_PROGRESS, mProgressShown);
        outState.putBoolean(EXTRA_LOGGED_OUT, true);
    }

    @Override
    public int getLayoutResourceId() {
        return R.layout.activity_signin;
    }

    @Override
    public int getPasswordEdittextResourceId() {
        return R.id.edittext_password;
    }

    @Override
    public int getSubmitButtonResourceId() {
        return R.id.button_signin;
    }

    @Override
    public int getUsernameEdittextResourceId() {
        return R.id.edittext_email;
    }

    @Override
    public void showProgress() {
        mProgressShown = true;
        mProgressBar.setVisibility(View.VISIBLE);
        mLinearLayoutSignInError.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress() {
        mProgressShown = false;
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showError(int errorCode) {
        mErrorCode = errorCode;

        mUsernameEditText.setErrorState(false);
        mPasswordEditText.setErrorState(false);
        mLinearLayoutSignInError.setVisibility(View.GONE);
        mLinearLayoutPasswordError.setVisibility(View.GONE);

        if(errorCode != ERROR_CONNECTION && errorCode != ERROR_UNKNOWN) {
            mTextViewErrorGeneric.setVisibility(View.VISIBLE);
        } else {
            mTextViewErrorGeneric.setVisibility(View.GONE);
        }

        if (errorCode == ERROR_NO) {
            mTextViewErrorGeneric.setVisibility(View.GONE);
        } else if (errorCode == ERROR_LOGIN) {

            mLinearLayoutSignInError.setVisibility(View.VISIBLE);
            mButtonSendReset.setVisibility(View.VISIBLE);
            mButtonSendReset.setText(R.string.send_reset_link);

            mButtonSendReset.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                    AccountController.getInstance().showForgottenPasswordScreen(LoginActivity.this, mEditTextUsername.getText().toString().trim());
            }
        });

            mTextViewError.setText(R.string.we_cant_find_a_password);
            mUsernameEditText.setErrorState(true);
            mPasswordEditText.setErrorState(true);
        } else if (errorCode == ERROR_NO_EMAIL || errorCode == ERROR_INVALID_EMAIL) {
            mLinearLayoutSignInError.setVisibility(View.VISIBLE);
            mButtonSendReset.setVisibility(View.GONE);
            mTextViewError.setText(R.string.error_blank_email);
            mUsernameEditText.setErrorState(true);
        } else if (errorCode == ERROR_NO_PASSWORD) {
            mLinearLayoutPasswordError.setVisibility(View.VISIBLE);
            mPasswordEditText.setErrorState(true);
        } else if (errorCode == ERROR_TOO_MANY_DEVICES) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_EXCEEDED_DEVICE_LIMIT, null);

            mLinearLayoutSignInError.setVisibility(View.VISIBLE);
            mButtonSendReset.setVisibility(View.VISIBLE);
            mButtonSendReset.setText(R.string.button_go_to_devices);

            mButtonSendReset.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                    BBBUriManager.getInstance().handleUri(LoginActivity.this, BBBUri.DEVICES);
            }
        });

            mTextViewError.setText(R.string.youve_already_signed_in_on);
        } else if (errorCode == ERROR_CONNECTION) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_SIGN_IN_ERROR, AnalyticsHelper.GA_LABEL_NO_NETWORK, null);
            final AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.title_device_offline), getString(R.string.error_connection_login_body));
            showDialog(alertDialogFragment, TAG_ERROR_DIALOG, false);
        } else if (errorCode == ERROR_UNKNOWN) {
            final AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(getString(R.string.error_server_title), getString(R.string.error_server_message));
            showDialog(alertDialogFragment, TAG_ERROR_DIALOG, false);
        } else if (errorCode == ERROR_EMPTY_FORM) {
            mButtonSendReset.setVisibility(View.GONE);
            mLinearLayoutPasswordError.setVisibility(View.VISIBLE);
            mLinearLayoutSignInError.setVisibility(View.VISIBLE);
            mTextViewError.setText(R.string.error_blank_email);
            mUsernameEditText.setErrorState(true);
            mPasswordEditText.setErrorState(true);
        }
    }
}