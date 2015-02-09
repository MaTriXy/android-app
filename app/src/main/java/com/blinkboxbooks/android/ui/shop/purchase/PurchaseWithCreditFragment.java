// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.purchase;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.api.model.BBBCredit;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.util.StringUtils;
import com.blinkboxbooks.android.widget.BookCover;

/**
 * Fragment showing the amount of credit a user has
 */
@SuppressLint("InflateParams")
public class PurchaseWithCreditFragment extends DialogFragment {

    private static final String ARG_SHOP_ITEM = "ArgShopItem";
    private static final String ARG_CREDIT = "credit";

    /**
     * Creates a new instance of this dialog fragment
     *
     * @return the Fragment
     */
    public static PurchaseWithCreditFragment newInstance(ShopItem shopItem) {
        PurchaseWithCreditFragment fragment = new PurchaseWithCreditFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable(ShopItem.class.getSimpleName(), shopItem);

        fragment.setArguments(bundle);

        return fragment;
    }

    private ShopItem mShopItem;
    private BBBCredit mCredit;

    public PurchaseWithCreditFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (savedInstanceState != null) {
            mShopItem = (ShopItem) savedInstanceState.getSerializable(ARG_SHOP_ITEM);
            mCredit = (BBBCredit) savedInstanceState.getSerializable(ARG_CREDIT);
        } else {
            mShopItem = (ShopItem) bundle.getSerializable(ShopItem.class.getSimpleName());
            mCredit = PurchaseController.getInstance().getCurrentCredit();
        }

        // Because we reference a statically stored item from a class that lives within the mythical Android lifecycle bad
        // things can happen. If the app is backgrounded with this dialog present and the bbb process dies (i.e. the OS kills
        // it after a while or I can manually simulate using a task killer), when the app is opened again the purchase controller
        // no longer has the data it needs about what is being purchased. So another band-aid is applied over the PurchaseController
        // logic to just clear this dialog if this happens (before it gets chance to blow up).
        if (PurchaseController.getInstance().getShopItem() == null) {
            dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_SHOP_ITEM, mShopItem);
        outState.putSerializable(ARG_CREDIT, mCredit);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_purchase_with_credit, null);

        BookCover bookCover = (BookCover) view.findViewById(R.id.bookcover);
        bookCover.setBook(mShopItem.book);

        TextView titleTextView = (TextView) view.findViewById(R.id.textview_title);
        TextView authorTextView = (TextView) view.findViewById(R.id.textview_author);
        TextView priceTextView = (TextView) view.findViewById(R.id.textview_price);

        titleTextView.setText(mShopItem.book.title);
        authorTextView.setText(mShopItem.book.author);

        if (mShopItem.price != null) {

            if (mShopItem.price.discountPrice == -1) {
                priceTextView.setText(StringUtils.formatPrice(mShopItem.price.currency, mShopItem.price.price, getActivity().getString(R.string.free)));
            } else {
                priceTextView.setText(StringUtils.formatPrice(mShopItem.price.currency, mShopItem.price.discountPrice, null));

                TextView originalPriceTextView = (TextView) view.findViewById(R.id.textview_price_original);
                if (mShopItem.price.discountPrice < mShopItem.price.price) {
                    originalPriceTextView.setVisibility(View.VISIBLE);
                    originalPriceTextView.setPaintFlags(originalPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    originalPriceTextView.setText(StringUtils.formatPrice(mShopItem.price.currency, mShopItem.price.price, null));
                }
            }

            if (mShopItem.price.clubcardPointsAward > 0) {
                TextView clubcardPointsTextView = (TextView) view.findViewById(R.id.textview_clubcard_points);
                ImageView clubcardImage = (ImageView) view.findViewById(R.id.tesco_clubcard_image);
                clubcardImage.setVisibility(View.VISIBLE);
                clubcardPointsTextView.setVisibility(View.VISIBLE);

                String pointsText = getActivity().getString(R.string.purchase_dialog_s_clubcard_points, mShopItem.price.clubcardPointsAward);
                clubcardPointsTextView.setText(pointsText);
            }
        }

        TextView textViewCurrentCredit = (TextView) view.findViewById(R.id.textview_current_credit);
        TextView textViewCreditRemaining = (TextView) view.findViewById(R.id.textview_credit_remaining);
        TextView textViewCreditToUse = (TextView) view.findViewById(R.id.textview_credit_to_use);

        BBBBookPrice price = mShopItem.price;

        double priceToPay = price.getPriceToPay();

        textViewCurrentCredit.setText(StringUtils.formatPrice(mCredit.currency, mCredit.amount, null));
        textViewCreditRemaining.setText(StringUtils.formatPrice(mCredit.currency, mCredit.amount - priceToPay, null));
        textViewCreditToUse.setText(StringUtils.formatPrice(price.currency, priceToPay, null));

        Button buttonPayNow = (Button) view.findViewById(R.id.button_pay_now);

        buttonPayNow.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                dismiss();
                PurchaseController.getInstance().completePurchase();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        return builder.create();
    }
}
