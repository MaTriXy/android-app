// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

/**
 * A DialogFragment that indicates something is being loaded
 */
public class ProgressDialogFragment extends DialogFragment {

    private static final String PARAM_MESSAGE = "message";

    /**
     * Creates a new ProgressDialogFragment
     *
     * @param messageId the text resource id of the message you want to display
     * @return an ProgressDialogFragment
     */
    public static ProgressDialogFragment newInstance(int messageId) {
        ProgressDialogFragment dialogFragment = new ProgressDialogFragment();

        Bundle args = new Bundle();
        args.putInt(PARAM_MESSAGE, messageId);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    private ProgressDialog mDialog;

    public ProgressDialogFragment() {
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int messageId = getArguments().getInt(PARAM_MESSAGE);

        mDialog = new ProgressDialog(getActivity());

        mDialog.setMessage(getString(messageId));
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);

        OnKeyListener keyListener = new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }

                return false;
            }
        };

        mDialog.setOnKeyListener(keyListener);

        return mDialog;
    }

    /**
     * Updates the messages being showing by the dialog
     *
     * @param textResourceId
     */
    public boolean setMessage(int textResourceId) {
        if (mDialog == null || !mDialog.isShowing()) {
            return false;
        }
        mDialog.setMessage(getString(textResourceId));
        return true;
    }
}