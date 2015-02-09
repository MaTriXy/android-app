// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.account;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBResponseHandler;
import com.blinkboxbooks.android.dialog.GenericDialogFragment;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.ValidationUtil;
import com.blinkboxbooks.android.widget.BBBEditText;

import java.net.HttpURLConnection;

/**
 * Fragment for allowing the user to request a password reset email
 */
public class ForgottenPasswordFragment extends BaseDialogFragment {

    private static final String RESPONSE_HANDLER_ID = "fpqResponseHandler";

    private static final String TAG_SUCCESS_DIALOG = "tag_success_dialog";

    private static final String EXTRA_EMAIL = "email";

    /**
     * Creates a new instance of this dialog fragment
     *
     * @return the Fragment
     */
    public static ForgottenPasswordFragment newInstance(String email) {
        ForgottenPasswordFragment fragment = new ForgottenPasswordFragment();

        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_EMAIL, email);

        fragment.setArguments(bundle);

        return fragment;
    }

    private BBBEditText mEditTextEmail;

    private View mLayoutErrorEmail;

    private ProgressBar mProgressBar;

    private String mEmail;

    public ForgottenPasswordFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        View view = View.inflate(getActivity(), R.layout.fragment_forgotten_password, null);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressbar);

        mEditTextEmail = (BBBEditText) view.findViewById(R.id.editext_emailaddress);
        mLayoutErrorEmail = view.findViewById(R.id.layout_error_email);

        String email = getArguments().getString(EXTRA_EMAIL);
        mEditTextEmail.setText(email);

        mEditTextEmail.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendPressed();
                }

                return false;
            }
        });

        view.findViewById(R.id.button_send).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendPressed();
            }
        });

        TextView textViewFaq = ((TextView) view.findViewById(R.id.textview_go_to_faq));
        textViewFaq.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                faqPressed();
            }
        });

        builder.setView(view);

        final AlertDialog alertDialog = builder.create();

        return alertDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentName = AnalyticsHelper.GA_SCREEN_App_ResetPasswordScreen;
    }

    @Override
    public void onResume() {
        super.onResume();
        BBBRequestManager.getInstance().addResponseHandler(RESPONSE_HANDLER_ID, passwordResetResponseHandler);
    }

    @Override
    public void onPause() {
        super.onPause();
        BBBRequestManager.getInstance().removeResponseHandler(RESPONSE_HANDLER_ID);
    }

    public void sendPressed() {
        mEmail = mEditTextEmail.getText().toString();

        boolean valid = ValidationUtil.validateEmail(mEmail);

        if (!valid) {
            mEditTextEmail.setErrorState(true);
            mLayoutErrorEmail.setVisibility(View.VISIBLE);
            return;
        } else {
            mEditTextEmail.setErrorState(false);
            mLayoutErrorEmail.setVisibility(View.GONE);
        }

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditTextEmail.getWindowToken(), 0);

        BBBRequest request = BBBRequestFactory.getInstance().createPasswordResetRequest(mEmail);
        BBBRequestManager.getInstance().executeRequest(RESPONSE_HANDLER_ID, request);

        showProgress();
    }

    public void faqPressed() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(R.string.support_url)));
        startActivity(intent);
    }

    private BBBResponseHandler passwordResetResponseHandler = new BBBResponseHandler() {

        public void connectionError(BBBRequest request) {
            hideProgress();

            showMessage(getString(R.string.title_device_offline), getString(R.string.error_connection_register_body));
        }

        public void receivedResponse(BBBResponse response) {
            hideProgress();

            if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String message = getString(R.string.text_password_reset_sent);

                final GenericDialogFragment genericDialogFragment = GenericDialogFragment.newInstance(getString(R.string.title_password_reset_sent), message, getString(R.string.ok), null, null, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }

                }, null, null, AnalyticsHelper.GA_SCREEN_App_ResetPasswordScreen_EmailConfirmation);
                ((BaseActivity) getActivity()).showDialog(genericDialogFragment, TAG_SUCCESS_DIALOG, false);
            } else {
                showMessage(getString(R.string.error_server_title), getString(R.string.error_server_message));
            }
        }
    };

    private void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
        mLayoutErrorEmail.setVisibility(View.GONE);
    }

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
    }

    private void showMessage(String title, String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}
