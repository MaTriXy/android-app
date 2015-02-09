// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Dialog fragment for when user clicks to add sample
 */
public class AddSampleDialogFragment extends BaseDialogFragment {

    /**
     * Create an new fragment
     *
     * @param downloadButtonListener
     * @param readNowButtonListener
     * @return
     */
    public static AddSampleDialogFragment newInstance(OnClickListener downloadButtonListener, OnClickListener readNowButtonListener) {

        AddSampleDialogFragment dialogFragment = new AddSampleDialogFragment();

        dialogFragment.downloadButtonListener = downloadButtonListener;
        dialogFragment.readNowButtonListener = readNowButtonListener;

        return dialogFragment;
    }

    private OnClickListener downloadButtonListener;
    private OnClickListener readNowButtonListener;

    public AddSampleDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        builder.setTitle(getString(R.string.title_add_sample_dialog));
        builder.setMessage(R.string.you_will_need_to_signin_to_download);

        builder.setPositiveButton(getString(R.string.button_download), downloadButtonListener);
        builder.setNegativeButton(getString(R.string.button_read_now), readNowButtonListener);

        return builder.create();
    }
}
