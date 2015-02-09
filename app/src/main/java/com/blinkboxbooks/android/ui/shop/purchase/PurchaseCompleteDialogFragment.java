// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.purchase;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.ui.library.LibraryActivity;
import com.blinkboxbooks.android.ui.shop.ShopActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;

/**
 * Fragment shown upon the successful purchase of a book
 */
@SuppressLint("InflateParams")
public class PurchaseCompleteDialogFragment extends BaseDialogFragment {

    public static PurchaseCompleteDialogFragment newInstance() {
        return new PurchaseCompleteDialogFragment();
    }

    /**
     * Creates a new instance of this dialog fragment
     *
     * @return the Fragment
     */
    public PurchaseCompleteDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentName = AnalyticsHelper.GA_SCREEN_Shop_PaymentScreen_Success;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_purchase_complete, null);

        TextView textViewHeader = (TextView) view.findViewById(R.id.textview_header);
        TextView textViewBody = (TextView) view.findViewById(R.id.textview_body);

        boolean newUser = AccountController.getInstance().isNewUser();

        textViewHeader.setText(newUser ? R.string.title_welcome_to_blinkbox_books : R.string.title_purchase_complete);

        if (newUser) {
            String accountName = AccountController.getInstance().getDataForLoggedInUser(BBBApiConstants.PARAM_USERNAME);

            String text = getActivity().getString(R.string.dialog_purchase_complete_new_user, accountName);
            textViewBody.setText(text);

            AccountController.getInstance().setNewUser(false);
        } else {
            textViewBody.setText(R.string.dialog_purchase_complete);
        }

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        builder.setView(view);
        builder.setPositiveButton(R.string.button_go_to_library, new OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dismiss();

                BaseActivity activity = PurchaseController.getInstance().getBaseActivity();

                if(activity == null || !(activity instanceof LibraryActivity)) {
                    Intent intent = new Intent(getActivity(), LibraryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getActivity().startActivity(intent);
                }
            }
        });

        builder.setNegativeButton(R.string.button_find_more_books, new OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dismiss();

                BaseActivity activity = PurchaseController.getInstance().getBaseActivity();

                if(activity == null || !(activity instanceof ShopActivity)) {
                    Intent intent = new Intent(getActivity(), ShopActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getActivity().startActivity(intent);
                }
            }
        });

        return builder.create();
    }
}
