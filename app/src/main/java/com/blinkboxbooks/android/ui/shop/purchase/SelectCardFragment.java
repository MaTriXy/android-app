// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.purchase;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.api.model.BBBCredit;
import com.blinkboxbooks.android.api.model.BBBCreditCard;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.controller.PurchaseController.DataUpdatedListener;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.StringUtils;

import java.util.List;

/**
 * Fragment allows user to select a card for completing purchase.
 */
@SuppressLint("InflateParams")
public class SelectCardFragment extends BaseDialogFragment implements DataUpdatedListener {

    private final PurchaseController shopController = PurchaseController.getInstance();

    private ListView mListViewCards;

    private CardAdapter mAdapter;

    private LayoutInflater mInflater;

    /**
     * Creates a new instance of this dialog fragment
     *
     * @return the Fragment
     */
    public static SelectCardFragment newInstance() {
        return new SelectCardFragment();
    }

    public SelectCardFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentName = AnalyticsHelper.GA_SCREEN_Shop_PaymentScreen_AddNewCard;

        // Because we reference a statically stored item from a class that lives within the mythical Android lifecycle bad
        // things can happen. If the app is backgrounded with this dialog present and the bbb process dies (i.e. the OS kills
        // it after a while or I can manually simulate using a task killer), when the app is opened again it tries to restore
        // this dialog and lo and behold the statically stored item is gone. So another band-aid is applied over the PurchaseController
        // logic to just clear this dialog if this happens (before it gets chance to blow up).
        ShopItem shopItem = PurchaseController.getInstance().getShopItem();
        if (shopItem == null) {
            dismiss();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        PurchaseController.getInstance().removeUpdateListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        PurchaseController.getInstance().addUpdateListener(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mInflater = getActivity().getLayoutInflater();

        View view = mInflater.inflate(R.layout.fragment_select_card, null, false);

        mListViewCards = (ListView) view.findViewById(R.id.listview);

        OnClickListener listener = new OnClickListener() {

            public void onClick(View v) {
                dismiss();
                PurchaseController.getInstance().showAddNewCardFragment();
            }
        };

        List<BBBCreditCard> cards = shopController.getCards();

        View footerView = mInflater.inflate(R.layout.list_item_add_card, null);
        footerView.findViewById(R.id.button_add_new_card).setOnClickListener(listener);

        if(cards == null || cards.size() == 0) {
            footerView.findViewById(R.id.textview_add_new_card).setVisibility(View.GONE);
        }

        mListViewCards.addFooterView(footerView);

        mAdapter = new CardAdapter();
        mListViewCards.setAdapter(mAdapter);
        mAdapter.setData(cards);

        // A bit of a hack to add the line above the cards required by design (we put in an empty header)
        if (cards != null && cards.size() > 0) {
            View headerView = new View(getActivity());
            mListViewCards.addHeaderView(headerView);
        }

        mListViewCards.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // We subtract 1 because the header counts as an item
                final BBBCreditCard creditCard = mAdapter.getItem(position - 1);
                PurchaseController.getInstance().setSelectedCard(creditCard);

                dismiss();
                PurchaseController.getInstance().showPurchaseConfirmationFragment();
            }
        });

        TextView textViewSubtext = (TextView) view.findViewById(R.id.textview_subtext);
        String email = AccountController.getInstance().getDataForLoggedInUser(BBBApiConstants.PARAM_USERNAME);

        if (!TextUtils.isEmpty(email)) {
            String text = getResources().getString(cards == null || cards.size() == 0 ? R.string.payment_details_subtext_1 : R.string.payment_details_subtext_2);
            textViewSubtext.setText(String.format(text, email));
        } else {
            textViewSubtext.setVisibility(View.GONE);
        }

        BBBCredit credit = PurchaseController.getInstance().getCurrentCredit();
        ShopItem shopItem = PurchaseController.getInstance().getShopItem();

        if (credit != null && shopItem != null) {

            BBBBookPrice price = shopItem.price;

            if (price != null && credit.amount > 0 && price.getPriceToPay() > credit.amount) {
                view.findViewById(R.id.layout_credit).setVisibility(View.VISIBLE);

                TextView textViewInsufficientCredit = (TextView) view.findViewById(R.id.textview_insufficient_credit);

                if (!TextUtils.isEmpty(email)) {
                    textViewInsufficientCredit.setText(String.format(getResources().getString(R.string.insufficient_credit_message), email));
                } else {
                    textViewInsufficientCredit.setVisibility(View.GONE);
                }

                TextView textViewCurrentCredit = (TextView) view.findViewById(R.id.textview_current_credit);
                TextView textViewAmountLeftToPay = (TextView) view.findViewById(R.id.textview_amount_left_to_pay);

                textViewCurrentCredit.setText(StringUtils.formatPrice(credit.currency, credit.amount, null));
                textViewAmountLeftToPay.setText(StringUtils.formatPrice(price.currency, price.getPriceToPay() - credit.amount, null));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        return builder.create();
    }

    @Override
    public void updated() {

        if (mListViewCards != null && mListViewCards.getAdapter() != null) {
            mAdapter.setData(shopController.getCards());
        }
    }

    private class CardAdapter extends BaseAdapter {

        private List<BBBCreditCard> cards;

        public void setData(List<BBBCreditCard> cards) {
            this.cards = cards;

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return cards == null ? 0 : cards.size();
        }

        @Override
        public BBBCreditCard getItem(int position) {

            if (cards == null || position >= cards.size()) {
                return null;
            }

            return cards.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_credit_card, null);

                ViewHolderCard holder = new ViewHolderCard();
                holder.containerDelete = convertView.findViewById(R.id.container_delete);

                holder.textViewType = (TextView) convertView.findViewById(R.id.textview_cc_type);
                holder.textViewNumber = (TextView) convertView.findViewById(R.id.textview_cc_number);
                holder.textViewExpired = (TextView) convertView.findViewById(R.id.textview_expired);

                holder.imageViewIcon = (ImageView) convertView.findViewById(R.id.imageview_cc_image);

                holder.buttonDelete = (ImageButton) convertView.findViewById(R.id.button_delete);
                convertView.setTag(holder);
            }

            final BBBCreditCard card = getItem(position);

            ViewHolderCard holder = (ViewHolderCard) convertView.getTag();

            String lastDigits = card.maskedNumber.substring(card.maskedNumber.length() - 4);

            holder.textViewType.setText(getString(R.string.card_ending, card.cardType));
            holder.textViewNumber.setText(lastDigits);

            if (BBBApiConstants.PARAM_CARDTYPE_VISA.equals(card.cardType)) {
                holder.imageViewIcon.setImageResource(R.drawable.ic_cardtype_visa);
            } else if (BBBApiConstants.PARAM_CARDTYPE_MASTERCARD.equals(card.cardType)) {
                holder.imageViewIcon.setImageResource(R.drawable.ic_cardtype_mastercard);
            }

            if (card.expired) {
                holder.textViewExpired.setText(getActivity().getString(R.string.expired, card.expirationMonth, card.expirationYear));
                holder.containerDelete.setVisibility(View.VISIBLE);
                holder.buttonDelete.setOnClickListener(!card.expired ? null : new OnClickListener() {

                    public void onClick(View v) {
                        PurchaseController.getInstance().deleteCardPressed(card.id);
                    }
                });
            } else {
                holder.containerDelete.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    static class ViewHolderCard {
        View containerDelete;

        TextView textViewType;
        TextView textViewNumber;
        TextView textViewExpired;

        ImageView imageViewIcon;

        ImageButton buttonDelete;
    }
}
