// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Dialog fragment for any kind of message
 */
public class GenericDialogFragment extends BaseDialogFragment {

    public static final String TAG_CLIENT_LIMIT_EXCEEDED = "tag_client_limit_exceeded";
    public static final String TAG_INSUFFICIENT_SPACE = "tag_insufficient_space";

    /**
     * Creates a dialog fragment with the given parameters all of which are optional.
     *
     * @param title
     * @param message
     * @param positiveButton
     * @param negativeButton
     * @param neutralButton
     * @param positiveButtonListener
     * @param neutralButtonListener
     * @param negativeButtonListener
     * @param fragmentName
     * @return
     */
    public static GenericDialogFragment newInstance(String title, String message, String positiveButton, String negativeButton, String neutralButton, OnClickListener positiveButtonListener,
        OnClickListener neutralButtonListener, OnClickListener negativeButtonListener, String fragmentName) {

        GenericDialogFragment dialogFragment = new GenericDialogFragment();

        dialogFragment.title = title;
        dialogFragment.message = message;
        dialogFragment.positiveButton = positiveButton;
        dialogFragment.negativeButton = negativeButton;
        dialogFragment.neutralButton = neutralButton;
        dialogFragment.positiveButtonListener = positiveButtonListener;
        dialogFragment.neutralButtonListener = neutralButtonListener;
        dialogFragment.negativeButtonListener = negativeButtonListener;
        dialogFragment.mFragmentName = fragmentName;

        return dialogFragment;
    }

    private String title;
    private String message;
    private String positiveButton;
    private String negativeButton;
    private String neutralButton;

    private OnClickListener positiveButtonListener;
    private OnClickListener neutralButtonListener;
    private OnClickListener negativeButtonListener;

    public GenericDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroyView() {
        // Due to a bug with the compatibility library we need this check to prevent the dialog being dismissed on rotation
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setOnDismissListener(null);
        }
        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        if (title != null) {
            builder.setTitle(title);
        }

        if (message != null) {
            builder.setMessage(message);
        }

        if (positiveButton != null) {
            builder.setPositiveButton(positiveButton, positiveButtonListener);
        }

        if (neutralButton != null) {
            builder.setNeutralButton(neutralButton, neutralButtonListener);
        }

        if (negativeButton != null) {
            builder.setNegativeButton(negativeButton, negativeButtonListener);
        }

        return builder.create();
    }
}
