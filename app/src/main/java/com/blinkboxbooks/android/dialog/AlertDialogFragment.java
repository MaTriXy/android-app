// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Dialog fragment for showing a simple popup message
 */
public class AlertDialogFragment extends DialogFragment {
    private static final String PARAM_TITLE = "PARAM_TITLE";
    private static final String PARAM_MESSAGE = "PARAM_MESSAGE";

    /**
     * Creates a new AlertDialogFragment
     *
     * @param title   the text of the title you want to show
     * @param message the text the message you want to show
     * @return an AlertDialogFragment
     */
    public static AlertDialogFragment newInstance(String title, String message) {
        AlertDialogFragment dialogFragment = new AlertDialogFragment();

        dialogFragment.title = title;
        dialogFragment.message = message;

        return dialogFragment;
    }

    private String title;
    private String message;

    public AlertDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (title == null && message == null && savedInstanceState != null) {
            title = savedInstanceState.getString(PARAM_TITLE, null);
            message = savedInstanceState.getString(PARAM_MESSAGE, null);
            if (title == null && message == null) {
                return null;
            }
        }

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        if (title != null) {
            builder.setTitle(title);
        }

        if (message != null) {
            builder.setMessage(message);
        }

        builder.setPositiveButton(R.string.button_close, null);

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putString(PARAM_TITLE, title);
            outState.putString(PARAM_MESSAGE, message);
        }
    }
}