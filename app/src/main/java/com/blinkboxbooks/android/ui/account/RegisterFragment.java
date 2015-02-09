// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.account;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.blinkboxbooks.android.BusinessRules;
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
import com.blinkboxbooks.android.dialog.ClubcardHelpDialogFragment;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.WebContentActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.DeviceUtils;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.ValidationUtil;
import com.blinkboxbooks.android.widget.BBBButton;
import com.blinkboxbooks.android.widget.BBBEditText;
import com.blinkboxbooks.android.widget.BBBSwitch;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.HttpURLConnection;

/**
 * Fragment for registering a new account
 */
public class RegisterFragment extends DialogFragment {

    private static final String PARAM_HAS_INTERACTED = "has_interacted";

    private static final String REGISTER_RESPONSE_HANDLER_ID = "reg_handler";

    private static final String EXTRA_SHOW_SIGIN_BUTTON_ON_DUPLICATE_EMAIL = "signin_button_duplicate_email";
    private static final String EXTRA_TRACK_GA_SCREEN_NAME = "ga_screen_name";
    private static final String EXTRA_SHOW_TOOLBAR = "show_toolbar";

    private static final String TAG_BACK_CONFIRM_DIALOG = "back_confirm_dialog";
    private static final String TAG_HELP_DIALOG = "help_dialog";

    private BBBEditText mEditTextEmailAddress;
    private BBBEditText mEditTextFirstName;
    private BBBEditText mEditTextLastName;
    private BBBEditText mEditTextTescoClubcardNumber;
    private BBBEditText mEditTextEnterPassword;
    private BBBEditText mEditTextReEnterPassword;

    private BBBSwitch mSwitchShowPassword;
    private CheckBox mCheckBoxOffers;
    private CheckBox mCheckBoxTerms;

    private View mErrorViewEmailInvalid;
    private View mErrorViewEmailTaken;
    private View mErrorViewFirstName;
    private View mErrorViewLastName;
    private View mErrorViewClubcard;
    private View mErrorViewTerms;

    private TextView mTextViewPasswordError;
    private TextView mTextViewPasswordError2;
    private TextView mTextViewTerms;
    private View mTextViewMatch;
    private TextView mTextViewErrorGeneric;

    private ScrollView mScrollView;
    private BBBButton mSignInWithThisEmailButton;
    private BBBButton mRegisterButton;
    private ImageButton mPasswordHelpButton;
    private ImageButton mClubcardHelpButton;

    private String mUsername;
    private String mPassword;

    private boolean mHasInteracted = false;
    private String mGAScreenName;

    private RegisterEventListener mRegisterEventPressed;

    /**
     * Interface that a class can implement to listen to handle events on the registration page.
     */
    public interface RegisterEventListener {

        public void signInWithEmailPressed(String emailAddress);

        public void signInComplete(Intent intent);
    }

    /**
     * Creates a new instance of this dialog fragment.
     *
     * @param showSigninButtonOnDuplicateEmail if set to true if the user enters a duplicate email they will
     *                                         be shown a sign in button
     * @param gaScreenName                     the name of the screen to track in GA (or null if none)
     * @param showToolbar                      if set to true the dialog will display a toolbar at the top of the screen
     * @return the Fragment
     */
    public static RegisterFragment newInstance(boolean showSigninButtonOnDuplicateEmail, String gaScreenName, boolean showToolbar) {
        RegisterFragment registerFragment = new RegisterFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(EXTRA_SHOW_SIGIN_BUTTON_ON_DUPLICATE_EMAIL, showSigninButtonOnDuplicateEmail);
        arguments.putString(EXTRA_TRACK_GA_SCREEN_NAME, gaScreenName);
        arguments.putBoolean(EXTRA_SHOW_TOOLBAR, showToolbar);
        registerFragment.setArguments(arguments);
        return registerFragment;
    }

    public RegisterFragment() {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mHasInteracted = savedInstanceState.getBoolean(PARAM_HAS_INTERACTED, false);
        }

        View view = inflater.inflate(R.layout.fragment_register, container, false);

        mEditTextEmailAddress = (BBBEditText) view.findViewById(R.id.editext_emailaddress);
        mEditTextFirstName = (BBBEditText) view.findViewById(R.id.editext_firstname);
        mEditTextLastName = (BBBEditText) view.findViewById(R.id.editext_lastname);
        mEditTextTescoClubcardNumber = (BBBEditText) view.findViewById(R.id.editext_clubcardno);
        mEditTextEnterPassword = (BBBEditText) view.findViewById(R.id.editext_password);
        mEditTextReEnterPassword = (BBBEditText) view.findViewById(R.id.editext_reenterpassword);

        mEditTextEmailAddress.addTextChangedListener(mTextInteractionListener);
        mEditTextFirstName.addTextChangedListener(mTextInteractionListener);
        mEditTextLastName.addTextChangedListener(mTextInteractionListener);
        mEditTextTescoClubcardNumber.addTextChangedListener(mTextInteractionListener);
        mEditTextEnterPassword.addTextChangedListener(mTextInteractionListener);
        mEditTextReEnterPassword.addTextChangedListener(mTextInteractionListener);

        mSwitchShowPassword = (BBBSwitch) view.findViewById(R.id.switch_showpassword);
        mCheckBoxOffers = (CheckBox) view.findViewById(R.id.checkbox_offers);
        mCheckBoxTerms = (CheckBox) view.findViewById(R.id.checkbox_terms);

        mErrorViewEmailInvalid = view.findViewById(R.id.layout_error_email_invalid);
        mErrorViewEmailTaken = view.findViewById(R.id.layout_error_email_taken);
        mErrorViewFirstName = view.findViewById(R.id.layout_error_firstname);
        mErrorViewLastName = view.findViewById(R.id.layout_error_lastname);
        mErrorViewClubcard = view.findViewById(R.id.layout_error_clubcard);
        mErrorViewTerms = view.findViewById(R.id.layout_error_terms);

        mTextViewErrorGeneric = (TextView) view.findViewById(R.id.textview_error_generic);
        mTextViewPasswordError = (TextView) view.findViewById(R.id.textview_error_password_error);
        mTextViewPasswordError2 = (TextView) view.findViewById(R.id.textview_error_password2);
        mTextViewTerms = (TextView) view.findViewById(R.id.textview_terms);
        mTextViewMatch = view.findViewById(R.id.textview_match);

        mTextViewTerms.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {

                if (NetworkUtils.hasInternetConnectivity(getActivity())) {
                    Intent intent = new Intent(getActivity(), WebContentActivity.class);

                    intent.putExtra(WebContentActivity.PARAM_FILE, "html/t&c.html");

                    startActivity(intent);
                } else {
                    BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());
                    builder.setTitle(R.string.title_device_offline);
                    builder.setMessage(R.string.dialog_you_need_to_be_online_to_access_the_terms_and_conditions);
                    builder.setPositiveButton(R.string.button_close, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                }
            }
        });

        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);

        mSignInWithThisEmailButton = (BBBButton) view.findViewById(R.id.signin_with_this_email_button);

        boolean shownSignInWithThisEmailButton = true;
        Bundle arguments = getArguments();
        if (arguments != null) {
            shownSignInWithThisEmailButton = arguments.getBoolean(EXTRA_SHOW_SIGIN_BUTTON_ON_DUPLICATE_EMAIL, true);
            mGAScreenName = arguments.getString(EXTRA_TRACK_GA_SCREEN_NAME, null);

            boolean showToolbar = arguments.getBoolean(EXTRA_SHOW_TOOLBAR, false);
            if (showToolbar) {
                Toolbar toolBar = (Toolbar) view.findViewById(R.id.toolbar);
                toolBar.setVisibility(View.VISIBLE);
                toolBar.setTitle(R.string.registration);
                toolBar.setLogo(R.drawable.actionbar_icon);
                toolBar.setNavigationIcon(getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));

                toolBar.setNavigationOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
            }
        }

        if (shownSignInWithThisEmailButton) {
            mSignInWithThisEmailButton.setVisibility(View.VISIBLE);
            mSignInWithThisEmailButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    signInWithThisEmailPressed();
                }
            });
        } else {
            mSignInWithThisEmailButton.setVisibility(View.GONE);
        }

        mPasswordHelpButton = (ImageButton) view.findViewById(R.id.password_help_button);
        mPasswordHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordHelpPressed();
            }
        });

        mClubcardHelpButton = (ImageButton) view.findViewById(R.id.clubcard_help_button);
        mClubcardHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clubcardHelpPressed();
            }
        });

        mRegisterButton = (BBBButton) view.findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onRegisterPressed(v);
            }
        });

        mSwitchShowPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setShowPassword(isChecked);
            }
        });

        mEditTextEnterPassword.addTextChangedListener(mPasswordChangedListener);
        mEditTextReEnterPassword.addTextChangedListener(mPasswordChangedListener);
        udpateStrengthText();

        mEditTextReEnterPassword.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onRegisterPressed(null);
                }

                return false;
            }
        });

        return view;
    }

    /**
     * Register a listener to be notified of registration screen events
     *
     * @param listener the SignInWithEmailPressedListener object to notify
     */
    public void setRegisterEventListener(RegisterEventListener listener) {
        mRegisterEventPressed = listener;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = new Dialog(getActivity(), R.style.BBBTheme);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        BBBRequestManager.getInstance().addResponseHandler(REGISTER_RESPONSE_HANDLER_ID, registrationHandler);

        // If we have a GA Screen name supplied then we track it here
        if (!TextUtils.isEmpty(mGAScreenName)) {
            AnalyticsHelper.getInstance().startTrackingUIComponent(mGAScreenName);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BBBRequestManager.getInstance().removeResponseHandler(REGISTER_RESPONSE_HANDLER_ID);

        if (!TextUtils.isEmpty(mGAScreenName)) {
            AnalyticsHelper.getInstance().stopTrackingUIComponent(mGAScreenName);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(PARAM_HAS_INTERACTED, mHasInteracted);
    }

    private void setShowPassword(boolean showPassword) {

        if (showPassword) {
            mEditTextEnterPassword.setInputType(InputType.TYPE_CLASS_TEXT);
            mEditTextReEnterPassword.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            mEditTextEnterPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            mEditTextReEnterPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        mEditTextEnterPassword.setSelection(mEditTextEnterPassword.getText().length());
        mEditTextReEnterPassword.setSelection(mEditTextReEnterPassword.getText().length());
    }

    private void clubcardHelpPressed() {
        ((BaseActivity) getActivity()).showDialog(ClubcardHelpDialogFragment.newInstance(), TAG_HELP_DIALOG, false);
    }

    private void passwordHelpPressed() {
        ((BaseActivity) getActivity()).showMessage(getString(R.string.title_password_help), getString(R.string.dialog_help_password));
    }

    private void signInWithThisEmailPressed() {
        if (mRegisterEventPressed != null) {
            mRegisterEventPressed.signInWithEmailPressed(mEditTextEmailAddress.getText().toString());
        }
    }

    public void onRegisterPressed(View view) {

        mErrorViewEmailTaken.setVisibility(View.GONE);
        mUsername = mEditTextEmailAddress.getText().toString().trim();
        String firstName = mEditTextFirstName.getText().toString().trim();
        String lastName = mEditTextLastName.getText().toString().trim();
        String clubcardNumber = mEditTextTescoClubcardNumber.getText().toString().trim();

        mPassword = mEditTextEnterPassword.getText().toString().trim();
        String password2 = mEditTextReEnterPassword.getText().toString().trim();

        boolean validated = true;
        boolean valid;

        valid = ValidationUtil.validateEmail(mUsername);

        if (TextUtils.isEmpty(mUsername)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_REGISTRATION_ERROR, AnalyticsHelper.GA_LABEL_NO_EMAIL, null);
        } else {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_REGISTRATION_ERROR, AnalyticsHelper.GA_LABEL_WRONG_EMAIL_FORMAT, null);
        }

        mErrorViewEmailInvalid.setVisibility(valid ? View.GONE : View.VISIBLE);
        mEditTextEmailAddress.setErrorState(!valid);
        validated &= valid;

        valid = ValidationUtil.validateName(firstName);
        mErrorViewFirstName.setVisibility(valid ? View.GONE : View.VISIBLE);
        mEditTextFirstName.setErrorState(!valid);
        validated &= valid;

        valid = ValidationUtil.validateName(lastName);
        mErrorViewLastName.setVisibility(valid ? View.GONE : View.VISIBLE);
        mEditTextLastName.setErrorState(!valid);
        validated &= valid;

        if (!TextUtils.isEmpty(clubcardNumber)) {
            valid = ValidationUtil.validateTescoClubcardNumber(clubcardNumber);
            mErrorViewClubcard.setVisibility(valid ? View.GONE : View.VISIBLE);
            mEditTextTescoClubcardNumber.setErrorState(!valid);
            validated &= valid;
        }

        valid = ValidationUtil.validatePassword(mUsername, mPassword);
        mTextViewPasswordError.setVisibility(valid ? View.GONE : View.VISIBLE);

        if (!valid) {

            if (TextUtils.isEmpty(mPassword)) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_REGISTRATION_ERROR, AnalyticsHelper.GA_LABEL_NO_PASSWORD, null);
            } else if (mPassword.length() < BusinessRules.PASSWORD_MIN_LENGTH) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_REGISTRATION_ERROR, AnalyticsHelper.GA_LABEL_SHORT_PASSWORD, null);
            }
        }

        mEditTextEnterPassword.setErrorState(!valid);
        validated &= valid;

        valid = ValidationUtil.validatePasswordMatch(mPassword, password2);

        if (!valid) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_REGISTRATION_ERROR, AnalyticsHelper.GA_LABEL_PASSWORD_MISMATCH, null);
        }

        mTextViewPasswordError2.setText(TextUtils.isEmpty(password2) ? R.string.error_password_blank : R.string.error_password_not_match);
        mTextViewPasswordError2.setVisibility(valid ? View.GONE : View.VISIBLE);
        mEditTextReEnterPassword.setErrorState(!valid);
        validated &= valid;

        valid = mCheckBoxTerms.isChecked();
        mErrorViewTerms.setVisibility(valid ? View.GONE : View.VISIBLE);
        validated &= valid;

        if (validated) {
            mTextViewErrorGeneric.setVisibility(View.GONE);

            BBBRequest request = BBBRequestFactory.getInstance().createRegisterAccountRequest(mUsername, firstName, lastName, mPassword, mCheckBoxOffers.isChecked(),
                    DeviceUtils.getClientName(getActivity()), DeviceUtils.getClientBrand(), DeviceUtils.getClientModel(), DeviceUtils.getClientOs());
            BBBRequestManager.getInstance().executeRequest(REGISTER_RESPONSE_HANDLER_ID, request);
            ((BaseActivity) getActivity()).showProgress(R.string.registering);
        } else {
            mTextViewErrorGeneric.setVisibility(View.VISIBLE);
            // Scroll back to the top so the user can see any error
            mScrollView.smoothScrollTo(0, 0);
        }
    }

    private TextWatcher mTextInteractionListener = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (!mHasInteracted) {
                mHasInteracted = true;
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_REGISTRATION, AnalyticsHelper.GA_EVENT_REGISTRATION_STATUS, AnalyticsHelper.GA_LABEL_FORM_INTERACTION, null);
            }

        }

        public void afterTextChanged(Editable s) {

        }
    };

    private final BBBBasicResponseHandler<BBBTokenResponse> registrationHandler = new BBBBasicResponseHandler<BBBTokenResponse>() {

        public void receivedData(BBBResponse response, BBBTokenResponse data) {

            if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                finishLogin(data.user_id, data.user_username, data.user_first_name, data.access_token, data.refresh_token, data.client_id, data.client_secret);
            } else {
                ((BaseActivity) getActivity()).hideProgress();
                ((BaseActivity) getActivity()).showMessage(getString(R.string.error_server_title), getString(R.string.error_server_message));
            }
        }

        public void receivedError(BBBResponse response) {
            ((BaseActivity) getActivity()).hideProgress();

            if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                BBBAuthenticationError error = null;

                try {
                    error = new Gson().fromJson(response.getResponseData(), BBBAuthenticationError.class);
                } catch (JsonSyntaxException e) {
                }

                if (error != null && BBBApiConstants.ERROR_USERNAME_ALREADY_TAKEN.equals(error.error_reason)) {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_REGISTRATION_ERROR, AnalyticsHelper.GA_LABEL_ALREADY_REGISTERED, null);

                    mErrorViewEmailTaken.setVisibility(View.VISIBLE);

                    // Scroll back to the top so the user can see any error
                    mScrollView.smoothScrollTo(0, 0);

                } else if (error != null && BBBApiConstants.ERROR_COUNTRY_GEOBLOCKED.equals(error.error_reason)) {
                    ((BaseActivity) getActivity()).showMessage(getString(R.string.error_server_registration_failed), getString(R.string.error_server_geoblocked));
                } else {
                    ((BaseActivity) getActivity()).showMessage(getString(R.string.error_server_title), getString(R.string.error_server_message));
                }
            } else if (response.getResponseCode() == BBBApiConstants.ERROR_CONNECTION_FAILED) {
                ((BaseActivity) getActivity()).showMessage(getString(R.string.title_device_offline), getString(R.string.error_connection_register_body));
            } else {
                ((BaseActivity) getActivity()).showMessage(getString(R.string.error_server_title), getString(R.string.error_server_message));
            }

        }
    };

    private BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishLogin(intent);
        }
    };

    private void finishLogin(final String userId, String userName, String firstname, String accessToken, String refreshToken, String clientId, String clientSecret) {
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, userName);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, BBBApiConstants.ACCOUNT_TYPE);
        intent.putExtra(BBBApiConstants.PARAM_ACCESS_TOKEN, accessToken);
        intent.putExtra(BBBApiConstants.PARAM_REFRESH_TOKEN, refreshToken);
        intent.putExtra(BBBApiConstants.PARAM_FIRST_NAME, firstname);
        intent.putExtra(BBBApiConstants.PARAM_USERNAME, userName);
        intent.putExtra(BBBApiConstants.PARAM_USER_ID, userId);
        intent.putExtra(BBBApiConstants.PARAM_CLIENT_ID, clientId);
        intent.putExtra(BBBApiConstants.PARAM_CLIENT_SECRET, clientSecret);

        String clubcardNumber = mEditTextTescoClubcardNumber.getText().toString();

        if (!TextUtils.isEmpty(clubcardNumber)) {
            intent.putExtra(BBBApiConstants.PARAM_CLUBCARD, clubcardNumber);
        }

        AccountController.getInstance().performLogin(userId, intent, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        manager.registerReceiver(mLoginReceiver, new IntentFilter(AccountController.ACTION_LOGGED_IN));
    }

    @Override
    public void onStop() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        manager.unregisterReceiver(mLoginReceiver);
        super.onStop();
    }

    private void finishLogin(Intent intent) {
        if (mRegisterEventPressed != null) {
            mRegisterEventPressed.signInComplete(intent);
        }
        dismiss();
    }

    private void udpateStrengthText() {
        String password1 = mEditTextEnterPassword.getText().toString();

        double entropy = ValidationUtil.calculatePasswordStrength(password1);

        if (entropy > 0) {
            String password2 = mEditTextReEnterPassword.getText().toString();

            if (TextUtils.equals(password1, password2)) {
                mTextViewMatch.setVisibility(View.VISIBLE);
            } else {
                mTextViewMatch.setVisibility(View.INVISIBLE);
            }
        } else {
            mTextViewMatch.setVisibility(View.INVISIBLE);
        }
    }

    private TextWatcher mPasswordChangedListener = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            udpateStrengthText();
        }
    };
}