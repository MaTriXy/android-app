// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Dialog that forces the user to upgrade the app or kill it
 */
public class ForceUpgradeDialogFragment extends DialogFragment {

    private static final String TAG = ForceUpgradeDialogFragment.class.getSimpleName();

    private String mUrl;
    private String mTitle;
    private String mMessage;

    public static ForceUpgradeDialogFragment newInstance(String url, String title, String message) {
        ForceUpgradeDialogFragment dialogFragment = new ForceUpgradeDialogFragment();

        dialogFragment.mUrl = url;
        dialogFragment.mTitle = title;
        dialogFragment.mMessage = message;


        return dialogFragment;
    }

    public ForceUpgradeDialogFragment() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        if (mTitle != null) {
            builder.setTitle(mTitle);
        }
        if (mMessage != null) {
            builder.setMessage(mMessage);
        }

        builder.setPositiveButton("Upgrade", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String forcedUpgrade = String.format("userId: %s", AccountController.getInstance().getUserId());
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_CALL_TO_ACTIONS, AnalyticsHelper.GA_EVENT_FORCED_UPGRADE_ACCEPTED, forcedUpgrade, Long.valueOf(BuildConfig.VERSION_NAME.replaceAll("[^0-9]", "")));
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mUrl)));
            }
        });
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                getActivity().finish();
                String forcedUpgrade = String.format("userId: %s", AccountController.getInstance().getUserId());
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_CALL_TO_ACTIONS, AnalyticsHelper.GA_EVENT_FORCED_UPGRADE_DISMISSED, forcedUpgrade, Long.valueOf(BuildConfig.VERSION_NAME.replaceAll("[^0-9]", "")));
            }
        });

        return builder.create();
    }

    @Override
    public void onStart() {
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().setOnKeyListener(new Dialog.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK) {
                    getActivity().finish();
                    String forcedUpgrade = String.format("userId: %s", AccountController.getInstance().getUserId());
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_CALL_TO_ACTIONS, AnalyticsHelper.GA_EVENT_FORCED_UPGRADE_DISMISSED, forcedUpgrade, Long.valueOf(BuildConfig.VERSION_NAME.replaceAll("[^0-9]", "")));
                }
            return true;
            }
        });
        super.onStart();
    }
}
