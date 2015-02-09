// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.purchase;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBCredit;
import com.blinkboxbooks.android.api.model.BBBCreditCard;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.model.CreditCard;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.StringUtils;
import com.blinkboxbooks.android.widget.BookCover;

/**
 * Fragment allows user to review the details of a purchase (book/price) before submitting.
 */
public class ConfirmPurchaseFragment extends BaseDialogFragment {

    /**
     * Creates a new instance of this dialog fragment for purchasing with a credit card
     *
     * @param shopItem the ShopItem we want to purchase
     * @param card     the card to use to complete the purchase
     * @param credit   users current credit
     * @return the Fragment
     */
    public static ConfirmPurchaseFragment newInstance(ShopItem shopItem, BBBCreditCard card, BBBCredit credit, CreditCard creditCard) {
        ConfirmPurchaseFragment fragment = new ConfirmPurchaseFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable(ShopItem.class.getSimpleName(), shopItem);
        bundle.putSerializable(BBBCreditCard.class.getSimpleName(), card);
        bundle.putSerializable(BBBCredit.class.getSimpleName(), credit);
        bundle.putSerializable(CreditCard.class.getSimpleName(), creditCard);

        fragment.setArguments(bundle);

        return fragment;
    }

    private ShopItem mShopItem;
    private BBBCreditCard mExistingCreditCard;
    private BBBCredit mCredit;
    private CreditCard mNewCreditCard;

    public ConfirmPurchaseFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        mShopItem = (ShopItem) bundle.getSerializable(ShopItem.class.getSimpleName());
        mExistingCreditCard = (BBBCreditCard) bundle.getSerializable(BBBCreditCard.class.getSimpleName());
        mCredit = (BBBCredit) bundle.getSerializable(BBBCredit.class.getSimpleName());
        mNewCreditCard = (CreditCard) bundle.getSerializable(CreditCard.class.getSimpleName());
        mFragmentName = AnalyticsHelper.GA_SCREEN_Shop_PaymentScreen_ConfirmOrder;
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_confirm_purchase, null);

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
                priceTextView.setText(StringUtils.formatPrice(mShopItem.price.currency, mShopItem.price.discountPrice, getActivity().getString(R.string.free)));

                TextView originalPriceTextView = (TextView) view.findViewById(R.id.textview_price_original);
                if (mShopItem.price.discountPrice < mShopItem.price.price) {
                    originalPriceTextView.setVisibility(View.VISIBLE);
                    originalPriceTextView.setPaintFlags(originalPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    originalPriceTextView.setText(StringUtils.formatPrice(mShopItem.price.currency, mShopItem.price.price, getActivity().getString(R.string.free)));
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

        View cardInfoView = view.findViewById(R.id.layout_card_info);

        BBBCreditCard card = mExistingCreditCard != null ? mExistingCreditCard : mNewCreditCard;

        if (card != null) {
            cardInfoView.setVisibility(View.VISIBLE);

            TextView typeTextView = (TextView) view.findViewById(R.id.textview_cc_type);
            TextView numberTextView = (TextView) view.findViewById(R.id.textview_cc_number);

            String lastDigits = card.maskedNumber.length() < 6 ? card.maskedNumber : card.maskedNumber.substring(card.maskedNumber.length() - 4);
            typeTextView.setText(getString(R.string.new_card_ending));

            numberTextView.setText(lastDigits);

        } else if(mNewCreditCard != null) {
            cardInfoView.setVisibility(View.GONE);
        }

        View changeButton = view.findViewById(R.id.button_change);

        if (changeButton != null) {
            changeButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    dismiss();
                    PurchaseController.getInstance().showSelectCardFragment();
                }
            });
        }

        TextView creditTextView = (TextView) view.findViewById(R.id.textview_credit);

        if (mCredit != null && mCredit.amount > 0) {
            creditTextView.setVisibility(View.VISIBLE);
        } else {
            creditTextView.setVisibility(View.GONE);
        }

        Button payNowButton = (Button) view.findViewById(R.id.button_pay_now);

        payNowButton.setOnClickListener(new OnClickListener() {

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
