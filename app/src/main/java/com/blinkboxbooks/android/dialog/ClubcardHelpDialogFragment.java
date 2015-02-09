// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Displays the clubcard help dialog
 */
public class ClubcardHelpDialogFragment extends DialogFragment {

    /**
     * Creates a new ClubcardHelpDialogFragment
     *
     * @return an ClubcardHelpDialogFragment
     */
    public static ClubcardHelpDialogFragment newInstance() {
        ClubcardHelpDialogFragment dialogFragment = new ClubcardHelpDialogFragment();

        return dialogFragment;
    }

    public ClubcardHelpDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        View layout = View.inflate(getActivity(), R.layout.fragment_dialog_clubcard_help, null);
        builder.setView(layout);

        builder.setTitle(R.string.title_where_is_my_clubcard_number);
        builder.setPositiveButton(R.string.button_close, null);

        return builder.create();
    }
}