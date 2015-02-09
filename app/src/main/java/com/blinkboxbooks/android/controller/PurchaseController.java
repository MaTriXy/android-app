// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.AdX.tag.AdXConnect;
import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookPriceList;
import com.blinkboxbooks.android.api.model.BBBBusinessError;
import com.blinkboxbooks.android.api.model.BBBCredit;
import com.blinkboxbooks.android.api.model.BBBCreditCard;
import com.blinkboxbooks.android.api.model.BBBCreditCardList;
import com.blinkboxbooks.android.api.model.BBBCreditResponse;
import com.blinkboxbooks.android.api.model.BBBPaymentReceipt;
import com.blinkboxbooks.android.api.model.BBBSynopsis;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.api.net.responsehandler.BBBResponseHandler;
import com.blinkboxbooks.android.model.CreditCard;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.net.ApiConnector;
import com.blinkboxbooks.android.sync.Synchroniser;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.account.ConfirmPasswordFragment;
import com.blinkboxbooks.android.ui.library.LibraryActivity;
import com.blinkboxbooks.android.ui.shop.purchase.AddNewCardFragment;
import com.blinkboxbooks.android.ui.shop.purchase.ConfirmPurchaseFragment;
import com.blinkboxbooks.android.ui.shop.purchase.PurchaseCompleteDialogFragment;
import com.blinkboxbooks.android.ui.shop.purchase.PurchaseWithCreditFragment;
import com.blinkboxbooks.android.ui.shop.purchase.SelectCardFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.LogUtils;
import com.google.gson.Gson;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for shop activity
 */
public class PurchaseController {

    private static PurchaseController instance;

    public static PurchaseController getInstance() {

        if (instance == null) {
            instance = new PurchaseController();
        }

        return instance;
    }

    private static final String TAG = PurchaseController.class.getSimpleName();
    private static final String TAG_SELECT_CARD_FRAGMENT = "select_card_fragment";
    private static final String TAG_ADD_NEW_CARD_FRAGMENT = "add_new_card_fragment";
    private static final String TAG_PURCHASE_WITH_CREDIT_FRAGMENT = "purchase_with_credit_fragment";
    private static final String TAG_CONFIRM_PASSWORD_FRAGMENT = "confirm_password_fragment";
    private static final String TAG_CONFIRM_PURCHASE_FRAGMENT = "confirm_purchase_fragment";
    private static final String TAG_PURCHASE_COMPLETE_FRAGMENT = "purchase_complete_fragment";

    private static final String RESPONSE_HANDLER_BOOK_SYNOPSIS = "sa_book_synopsis";

    private static final int STATE_INITIALISING_BOOK_PURCHASE = 0;
    private static final int STATE_PURCHASING_WITH_CREDIT = 1;
    private static final int STATE_PURCHASING_WITH_CARD = 2;
    private static final int STATE_ADDING_SAMPLE = 3;

    private BaseActivity mBaseActivity;
    private FragmentManager mFragmentManager;

    private ShopItem mShopItem;

    private String mISBN;

    private ArrayList<BBBCreditCard> mCards;

    private CreditCard mCreditCard;

    private BBBCreditCard mSelectedCard;

    private BBBCredit mCurrentCredit;

    private final ArrayList<DataUpdatedListener> listeners;

    private final Map<String, List<SynopsisCallback>> mSynopsisRequests = new HashMap<String, List<SynopsisCallback>>();

    private int mState;

    private String mLoadingBaseActivity = null;

    private Application.ActivityLifecycleCallbacks mActivityCallbackListener;

    private final Gson mGson = new Gson();

    private PurchaseController() {
        final BBBRequestManager requestManager = BBBRequestManager.getInstance();
        requestManager.addResponseHandler(RESPONSE_HANDLER_BOOK_SYNOPSIS, new SynopsisHandler());

        listeners = new ArrayList<DataUpdatedListener>();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(BBBApplication.getApplication());
        manager.registerReceiver(mLoginReceiver, new IntentFilter(AccountController.ACTION_LOGGED_IN));
    }

    /**
     * Sets the current BaseActivity for the controller
     */
    public void setBaseActivity(@NonNull BaseActivity baseActivity) {
        if (mLoadingBaseActivity != null) {
            if (!baseActivity.getLocalClassName().equals(mLoadingBaseActivity)) {
                return;
            }
        }

        mBaseActivity = baseActivity;
        mFragmentManager = mBaseActivity.getSupportFragmentManager();
        registerForActivityCallbacks(mBaseActivity.getApplication());
    }

    /**
     * Gets the base activity for the controller
     *
     * @return the Activty
     */
    public BaseActivity getBaseActivity() {
        return mBaseActivity;
    }


    /**
     * Gets the shop item we are currently purchasing
     *
     * @return the ShopItem
     */
    public ShopItem getShopItem() {
        return mShopItem;
    }

    /**
     * Gets a list of cards we have saved for the current users
     *
     * @return an array of BBBCreditCards
     */
    public List<BBBCreditCard> getCards() {
        return mCards;
    }

    /**
     * Gets the credit card the user has selected to use
     *
     * @return the credit card
     */
    public BBBCreditCard getSelectedCard() {
        return mSelectedCard;
    }

    /**
     * Sets the credit card the user wants to use to complete the purchase
     *
     * @param selectedCard the credit card
     */
    public void setSelectedCard(BBBCreditCard selectedCard) {
        this.mSelectedCard = selectedCard;
    }

    /**
     * Sets the new credit card the user wants to use to complete the purchase
     *
     * @param newCard the credit card
     */
    public void setCreditCard(CreditCard newCard) {
        this.mCreditCard = newCard;
    }

    /**
     * Gets the users current credits
     *
     * @return
     */
    public BBBCredit getCurrentCredit() {
        return mCurrentCredit;
    }

    /**
     * Completes the purchase with an existing card
     */
    public void completePurchase() {
        new Purchaser().execute();
    }

    /**
     * Completes a purchase with a new card
     *
     * @param creditCard
     */
    public void completePurchase(CreditCard creditCard) {
        mCreditCard = creditCard;
        new Purchaser().execute();
    }

    /**
     * Initialises the buy flow if the user is logged in and opens the login flow if they are not.
     *
     * @param item the ShopItem we are purchasing
     */
    public void buyPressed(ShopItem item) {

        if(mLoadingBaseActivity != null) {
            return;
        }
        if (mBaseActivity == null) {
           return;
        }

        mState = STATE_INITIALISING_BOOK_PURCHASE;
        mShopItem = item;

        if (!AccountController.getInstance().isLoggedIn()) {
            AccountController.getInstance().showLoginScreen(mBaseActivity, null);
        } else if(mShopItem != null) {
            mLoadingBaseActivity = mBaseActivity.getLocalClassName();
            new PurchaseInitialisation().execute();
        }
    }

    /**
     * Deletes the credit card
     */
    public void deleteCardPressed(final String token) {

        if (TextUtils.isEmpty(token)) {
            return;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createDeleteCreditCardRequest(token);
        showProgress();

        BBBRequestManager.getInstance().executeRequest(request, new BBBResponseHandler() {

            public void receivedResponse(BBBResponse response) {
                hideProgress();

                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    int size = mCards.size();

                    for (int i = 0; i < size; i++) {

                        if (token.equals(mCards.get(i).id)) {
                            mCards.remove(i);
                            notifyUpdated();

                            return;
                        }
                    }
                }
            }

            public void connectionError(BBBRequest request) {
                hideProgress();
            }
        });
    }

    /**
     * Attempts to add a sample to the users library
     *
     * @param isbn the ISBN of the book you want to add as a sample
     */
    public void addSample(String isbn) {
        new AddSampleTask(isbn).execute();
    }

    private class AddSampleTask extends AsyncTask<Void, Void, Boolean> {

        public AddSampleTask(String isbn) {
            mISBN = isbn;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress();
            LogUtils.d(TAG, "Adding Sample: " + mISBN);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            mState = STATE_ADDING_SAMPLE;

            if (!AccountController.getInstance().isLoggedIn()) {
                AccountController.getInstance().showLoginScreen(mBaseActivity, null);
                return false;
            } else {

                BBBRequest request = BBBRequestFactory.getInstance().createAddSampleRequest(mISBN);
                BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                // Due to a bug in the server (which they will not/cannot fix), we will get this error returned if the user adds the
                // book to their basket (and does not complete purchase) and then tries to download the sample.
                if (response.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                    // To workaround the issue we request that the basket is cleared before repeating the add sample request
                    request = BBBRequestFactory.getInstance().createClearBasketRequest();
                    response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                    // If the request above fails then we just fall through to the error handling below
                    if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        request = BBBRequestFactory.getInstance().createAddSampleRequest(mISBN);
                        response = BBBRequestManager.getInstance().executeRequestSynchronously(request);
                    }
                }

                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_BOOK_PURCHASE, AnalyticsHelper.GA_EVENT_SAVE_SAMPLE_BOOK, mISBN, null);
                    return true;
                } else if (response.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                    // If get the conflict error after clearing the basket then it must be a real conflict error
                    mBaseActivity.showMessage(null, mBaseActivity.getString(R.string.you_already_have_this_book_in_your_library));
                } else if (isUnverifiedUserError(response)) {
                    showPasswordConfirmationFragmentOnUiThread();
                } else {
                    showErrorOnUiThread(response.getResponseCode());
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            hideProgress();

            // If the download worked then we request the book is downloaded and return the user back to their library
            if (result && isBaseActivityInForeground()) {
                AccountController.getInstance().requestSynchronisationAndDownloadBook(mISBN);
                Intent intent = new Intent(mBaseActivity, LibraryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mBaseActivity.startActivity(intent);
            }
        }
    }

    private void showProgress() {
        if (isBaseActivityInForeground()) {
            mLoadingBaseActivity = mBaseActivity.getLocalClassName();
            mBaseActivity.showProgress(R.string.loading);
        }
    }

    private void hideProgress() {
        mLoadingBaseActivity = null;
        if (isBaseActivityInForeground()) {
            mBaseActivity.hideProgress();
        }
    }

    /**
     * Shows the card selection fragment
     */
    public void showSelectCardFragment() {
        if (isBaseActivityInForeground()) {
            mState = STATE_PURCHASING_WITH_CARD;
            SelectCardFragment fragment = SelectCardFragment.newInstance();
            mBaseActivity.showDialog(fragment, TAG_SELECT_CARD_FRAGMENT, true);
        }
    }

    /**
     * Show the add new card fragment
     */
    public void showAddNewCardFragment() {
        if (isBaseActivityInForeground()) {
            mState = STATE_PURCHASING_WITH_CARD;
            final AddNewCardFragment fragment = AddNewCardFragment.newInstance();
            mBaseActivity.showDialog(fragment, TAG_ADD_NEW_CARD_FRAGMENT, true);
        }
    }

    /**
     * Show the purchase with credit fragment
     */
    public void showPurchaseWithCreditFragment() {
        if (isBaseActivityInForeground()) {
            mState = STATE_PURCHASING_WITH_CREDIT;
            PurchaseWithCreditFragment fragment = PurchaseWithCreditFragment.newInstance(mShopItem);
            mBaseActivity.showDialog(fragment, TAG_PURCHASE_WITH_CREDIT_FRAGMENT, true);
        }
    }

    /**
     * Shows the password confirmation fragment
     */
    public void showPasswordConfirmationFragmentOnUiThread() {
        if (mBaseActivity != null) {
            mBaseActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isBaseActivityInForeground()) {
                        ConfirmPasswordFragment fragment = ConfirmPasswordFragment.newInstance();
                        mBaseActivity.showDialog(fragment, TAG_CONFIRM_PASSWORD_FRAGMENT, true);
                    }
                }
            });
        }
    }

    /**
     * Shows the purchase confirm fragment
     */
    public void showPurchaseConfirmationFragment() {
        if (isBaseActivityInForeground()) {
            ConfirmPurchaseFragment fragment = ConfirmPurchaseFragment.newInstance(mShopItem, mSelectedCard, mCurrentCredit, mCreditCard);
            mBaseActivity.showDialog(fragment, TAG_CONFIRM_PASSWORD_FRAGMENT, true);
        }
    }

    /**
     * Shows the order complete dialog
     */
    private void showOrderCompleteDialogFragment() {
        if (isBaseActivityInForeground()) {
            PurchaseCompleteDialogFragment fragment = PurchaseCompleteDialogFragment.newInstance();
            mBaseActivity.showDialog(fragment, TAG_PURCHASE_COMPLETE_FRAGMENT, true);
        }
    }

    public void requestSynopsis(String isbn, SynopsisCallback callback) {
        List<SynopsisCallback> callbacks = mSynopsisRequests.get(isbn);
        if (callbacks == null) {
            callbacks = new ArrayList<SynopsisCallback>();
            BBBRequest request = BBBRequestFactory.getInstance().createGetSynopsisRequest(isbn);
            BBBRequestManager.getInstance().executeRequest(RESPONSE_HANDLER_BOOK_SYNOPSIS, request);

            mSynopsisRequests.put(isbn, callbacks);
        }
        callbacks.add(callback);
    }

    private void showPurchaseErrorOnUiThread(final BBBResponse response) {
        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_BOOK_PURCHASE, AnalyticsHelper.GA_EVENT_PURCHASE_STATUS, AnalyticsHelper.GA_LABEL_PURCHASE_FAILED, null);

        if (mBaseActivity != null) {
            mBaseActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isBaseActivityInForeground()) {
                        try {
                            BBBBusinessError error = mGson.fromJson(response.getResponseData(), BBBBusinessError.class);

                            if (BBBApiConstants.ERROR_UNDISTRIBUTED_BOOK.equals(error.code)) {
                                mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_book_undistributed));
                                return;
                            } else if (BBBApiConstants.ERROR_FUTURE_PUBLICATION_DATE.equals(error.code)) {

                                String message;

                                if (mShopItem.book.publication_date > 0) {
                                    String formattedDate = BBBCalendarUtil.formatDate(mShopItem.book.publication_date);
                                    message = mBaseActivity.getString(R.string.error_book_not_yet_published_date, formattedDate);
                                } else {
                                    message = mBaseActivity.getString(R.string.error_book_not_yet_published);
                                }

                                mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), message);
                                return;
                            } else if (BBBApiConstants.ERROR_INVALID_EXPIRY_DATE.equals(error.code)) {
                                mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_invalid_expiry));
                                return;
                            } else if (BBBApiConstants.ERROR_MISSING_CREDIT_CARD_DATA.equals(error.code)) {
                                mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_missing_cc_data));
                                return;
                            } else if (BBBApiConstants.ERROR_MISSING_ADDRESS.equals(error.code)) {
                                mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_missing_address));
                                return;
                            } else if (BBBApiConstants.ERROR_NO_VALID_PAYMENT_METHODS.equals(error.code)) {
                                mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_no_valid_payment_methods));
                                return;
                            }

                        } catch (Exception e) {
                            LogUtils.e(TAG, e.getMessage(), e);
                        }

                        if (response.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                            mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_payment_declined));
                            return;
                        }

                        showErrorOnUiThread(response.getResponseCode());
                    }
                }
            });
        }
    }

    private void showErrorOnUiThread(final int code) {
        if (mBaseActivity != null) {
            mBaseActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isBaseActivityInForeground()) {
                        if (code == BBBApiConstants.ERROR_CONNECTION_FAILED) {
                            mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_no_network_generic));
                        } else {
                            mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), mBaseActivity.getString(R.string.error_server_message));
                        }
                    }
                }
            });
        }
    }

    private class SynopsisHandler extends BBBBasicResponseHandler<BBBSynopsis> {
        @Override
        public void receivedData(BBBResponse response, BBBSynopsis synopsis) {
            if (synopsis != null && synopsis.text != null && synopsis.id != null) {
                List<SynopsisCallback> callbacks = mSynopsisRequests.remove(synopsis.id);
                for (SynopsisCallback callback : callbacks) {
                    callback.updateSynopsis(synopsis);
                }
            } else {
                showErrorOnUiThread(response.getResponseCode());
                LogUtils.e(TAG, "Unexpected synopsis response: " + response.getResponseData());
            }
        }

        @Override
        public void receivedError(BBBResponse response) {
            showErrorOnUiThread(response.getResponseCode());
            LogUtils.e(TAG, "Error retrieving synopsis: " + response.getResponseData());
        }
    }


    /*	Initialisation flow:
     *
     *  add to basket and replace basket item if basket already contains a book
     *  get a list of credit cards
     *  show the Purchase confirmation screen
     */
    private class PurchaseInitialisation extends AsyncTask<Void, Void, Integer> {

        private static final int RESULT_STOP = -1;
        private static final int RESULT_SHOW_CREDIT = 1;
        private static final int RESULT_SELECT_CARD = 2;
        private static final int RESULT_ADD_CARD = 3;
        private static final int RESULT_PURCHASING_FREE_BOOK = 4;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showProgress();

            LogUtils.d(TAG, "Initialising purchase");
        }

        @Override
        protected Integer doInBackground(Void... params) {
            BBBRequest request;
            BBBResponse response = null;

            if(mShopItem == null) {
                return RESULT_STOP;
            }

            if (mShopItem.price == null) {
                LogUtils.d(TAG, "Price was null. Retrieving...");

                request = BBBRequestFactory.getInstance().createGetPricesRequest(mShopItem.book.isbn);
                response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    try {
                        BBBBookPriceList priceList = mGson.fromJson(response.getResponseData(), BBBBookPriceList.class);

                        //TODO if priceList.items is null we need to tell the user this book is not available for sale
                        if (priceList.items != null && priceList.items.length > 0) {
                            LogUtils.d(TAG, "Got price");

                            mShopItem.price = priceList.items[0];
                        }
                    } catch (Exception e) {
                        LogUtils.e(TAG, e.getMessage(), e);
                    }
                }
            }

            if (mShopItem.price == null) {
                LogUtils.e(TAG, "Could not get price: " + response.getResponseCode());

                showErrorOnUiThread(response.getResponseCode());
                return RESULT_STOP;
            }

            request = BBBRequestFactory.getInstance().createAddBasketItemRequest(mShopItem.book.isbn);
            response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

            if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                boolean retry = true;

                try {
                    BBBBusinessError error = mGson.fromJson(response.getResponseData(), BBBBusinessError.class);

                    if (BBBApiConstants.ERROR_ITEM_ALREADY_IN_THE_BASKET.equals(error.code)) {
                        retry = false;
                    } else if (BBBApiConstants.ERROR_BOOK_ALREADY_PURCHASED.equals(error.code)) {
                        String accountName = AccountController.getInstance().getDataForLoggedInUser(BBBApiConstants.PARAM_USERNAME);

                        String title = mBaseActivity.getString(R.string.title_you_already_have_this_book);
                        String body = mBaseActivity.getString(R.string.dialog_your_account_already_has_this_book, accountName);
                        showMessageOnUiThread(title, body);

                        return RESULT_STOP;
                    }

                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                }

                if (retry) {
                    request = BBBRequestFactory.getInstance().createClearBasketRequest();
                    response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                    if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        request = BBBRequestFactory.getInstance().createAddBasketItemRequest(mShopItem.book.isbn);
                        response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                        if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            showErrorOnUiThread(response.getResponseCode());
                            LogUtils.e(TAG, "error adding item to basket " + response.getResponseCode());
                            return RESULT_STOP;
                        }
                    }
                }
            } else if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
                showErrorOnUiThread(response.getResponseCode());
                LogUtils.e(TAG, "error adding item to basket " + response.getResponseCode());
                return RESULT_STOP;
            }

            if (mShopItem.price.getPriceToPay() == 0) {
                LogUtils.i(TAG, "Purchasing free book");
                return RESULT_PURCHASING_FREE_BOOK;
            }

            //Attempts to approve purchase viability by checking user credit
            try {
                response = ApiConnector.getInstance().getCreditResponse();
                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    BBBCreditResponse creditResponse = mGson.fromJson(response.getResponseData(), BBBCreditResponse.class);
                    mCurrentCredit = creditResponse.credit;

                    if (mCurrentCredit.amount >= mShopItem.price.getPriceToPay()) {
                        LogUtils.d(TAG, String.format("Sufficient credit on account found: %s. Cost of book: %s", creditResponse.credit.amount, mShopItem.price.price));
                        return RESULT_SHOW_CREDIT;

                    } else {
                        LogUtils.d(TAG, "Insufficient credit on account");
                    }

                } else {
                    showErrorOnUiThread(response.getResponseCode());
                    LogUtils.e(TAG, "error getting account credit" + response.getResponseCode());
                    return RESULT_STOP;
                }

            } catch (Exception e) {
                showErrorOnUiThread(BBBApiConstants.ERROR_UNKNOWN);
                LogUtils.e(TAG, e.getMessage(), e);
                return RESULT_STOP;
            }

            request = BBBRequestFactory.getInstance().createGetCreditCardsRequest();
            response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

            if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BBBCreditCardList list = mGson.fromJson(response.getResponseData(), BBBCreditCardList.class);

                if (list != null) {
                    if (list.items != null) {
                        mCards = new ArrayList<BBBCreditCard>((List<BBBCreditCard>) Arrays.asList(list.items));
                    } else {
                        mCards = new ArrayList<BBBCreditCard>();
                    }
                }

            } else if (isUnverifiedUserError(response)) {
                showPasswordConfirmationFragmentOnUiThread();
                return RESULT_STOP;
            } else {
                showErrorOnUiThread(response.getResponseCode());
                LogUtils.e(TAG, "error retrieving cards" + response.getResponseCode());
                return RESULT_STOP;
            }

            return RESULT_SELECT_CARD;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            switch (result) {
                case RESULT_SHOW_CREDIT:
                    hideProgress();
                    showPurchaseWithCreditFragment();
                    break;
                case RESULT_ADD_CARD:
                    hideProgress();
                    showAddNewCardFragment();
                    break;
                case RESULT_SELECT_CARD:
                    hideProgress();
                    showSelectCardFragment();
                    break;
                case RESULT_PURCHASING_FREE_BOOK:
                    mState = STATE_PURCHASING_WITH_CREDIT;
                    completePurchase();
                    break;
                case RESULT_STOP:
                    hideProgress();
                    break;
                default:
                    hideProgress();
                    break;
            }
        }
    }

    private void showMessageOnUiThread(final String title, final String body) {
        if (mBaseActivity != null) {
            mBaseActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isBaseActivityInForeground()) {
                        mBaseActivity.showMessage(title, body);
                    }
                }
            });
        }
    }

    /*
     * Purchase flow:
     *
     * add card if required
     * work out which card we are using for the purchase
     * purchase item in basket and show Order complete screen
     */
    private class Purchaser extends AsyncTask<Void, Void, Boolean> {

        public Purchaser() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showProgress();

            LogUtils.d(TAG, "Completing purchase with " + (mCreditCard == null ? "existing" : "new") + " card.");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String cardToken = null;

            BBBRequest request = null;
            BBBResponse response = null;

            if (mState == STATE_PURCHASING_WITH_CREDIT) {
                request = BBBRequestFactory.getInstance().createPurchaseBasketRequest(null);
                response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    AnalyticsHelper.getInstance().sendPurchase(AnalyticsHelper.EE_SCREEN_NAME_PURCHASE_WITH_CREDIT, mShopItem);
                    LogUtils.d(TAG, "book purchased with credit");
                    Synchroniser.performLibrarySyncForCurrentAccount(mBaseActivity, mShopItem.book.isbn);
                    logTransaction(response);

                    return true;
                } else if (isUnverifiedUserError(response)) {
                    showPasswordConfirmationFragmentOnUiThread();
                    return false;
                } else {
                    LogUtils.e(TAG, "error trying to purchase book with credit: " + response.getResponseCode());
                    showPurchaseErrorOnUiThread(response);
                    return false;
                }
            }

            if (mCreditCard != null) {

                if (mCreditCard.saveNewCard) {
                    LogUtils.d(TAG, "saving card before purchase");

                    request = BBBRequestFactory.getInstance().createAddCreditCardRequest(mBaseActivity.getString(R.string.braintree_key), false, mCreditCard.number, mCreditCard.cvv,
                            String.valueOf(mCreditCard.expirationMonth), String.valueOf(mCreditCard.expirationYear), mCreditCard.cardholderName,
                            mCreditCard.billingAddress.line1, mCreditCard.billingAddress.line2, mCreditCard.billingAddress.locality, mCreditCard.billingAddress.postcode);

                    response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                    if (response.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                        BBBCreditCard card = mGson.fromJson(response.getResponseData(), BBBCreditCard.class);
                        cardToken = card.id;
                    } else if (isUnverifiedUserError(response)) {
                        showPasswordConfirmationFragmentOnUiThread();
                        return false;
                    } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                        String message = mBaseActivity.getString(R.string.error_add_card_failed, response.getResponseData());
                        mBaseActivity.showMessage(mBaseActivity.getString(R.string.title_error), message);
                        LogUtils.e(TAG, "problem adding card, reason: " + response.getResponseData());
                        return false;
                    } else {
                        showErrorOnUiThread(response.getResponseCode());
                        LogUtils.e(TAG, "problem adding card, code: " + response.getResponseCode());
                        return false;
                    }
                } else {
                    request = BBBRequestFactory.getInstance().createPurchaseBasketRequest(mBaseActivity.getString(R.string.braintree_key), mCreditCard.number,
                            mCreditCard.cvv, String.valueOf(mCreditCard.expirationMonth), String.valueOf(mCreditCard.expirationYear), mCreditCard.cardholderName,
                            mCreditCard.billingAddress.line1, mCreditCard.billingAddress.line2, mCreditCard.billingAddress.locality, mCreditCard.billingAddress.postcode);

                    response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                    if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        AnalyticsHelper.getInstance().sendPurchase(AnalyticsHelper.EE_SCREEN_NAME_PURCHASE_WITH_CARD, mShopItem);
                        Synchroniser.performLibrarySyncForCurrentAccount(mBaseActivity, mShopItem.book.isbn);
                        logTransaction(response);

                        return true;
                    } else {
                        showPurchaseErrorOnUiThread(response);
                        LogUtils.e(TAG, "error trying to purchase book with one off card payment: " + response.getResponseCode());
                        return false;
                    }
                }
            }

            if (cardToken == null && mSelectedCard != null) {
                cardToken = mSelectedCard.id;
            }

            if (cardToken == null) {
                showErrorOnUiThread(response.getResponseCode());
                LogUtils.e(TAG, "could not get card for payment " + response.getResponseCode());
                return false;
            }

            request = BBBRequestFactory.getInstance().createPurchaseBasketRequest(cardToken);
            response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

            if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
                if (isUnverifiedUserError(response)) {
                    showPasswordConfirmationFragmentOnUiThread();
                } else {
                    showPurchaseErrorOnUiThread(response);
                    LogUtils.e(TAG, "error trying to purchase book with preexisting card: " + response.getResponseCode());
                }
                return false;
            } else {
                AnalyticsHelper.getInstance().sendPurchase(AnalyticsHelper.EE_SCREEN_NAME_PURCHASE_WITH_CARD, mShopItem);
                Synchroniser.performLibrarySyncForCurrentAccount(mBaseActivity, mShopItem.book.isbn);
                logTransaction(response);

                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                mCreditCard = null;
            }

            hideProgress();

            if (result) {
                LogUtils.d(TAG, "Book has been successfully purchased");

                showOrderCompleteDialogFragment();

                if (mShopItem.price != null && mShopItem.price.price == 0) {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_BOOK_PURCHASE, AnalyticsHelper.GA_EVENT_FREE_BOOK, mShopItem.book.isbn, null);
                }
            }
        }

        private void logTransaction(BBBResponse response) {
            BBBPaymentReceipt receipt = mGson.fromJson(response.getResponseData(), BBBPaymentReceipt.class);

            if (mBaseActivity != null && mShopItem != null && mShopItem.book != null && receipt != null && receipt.price != null) {
                AnalyticsHelper.getInstance().sendTransaction(receipt.id, receipt.price.amount, receipt.price.currency, mShopItem.book.title, receipt.isbn);
                AdXConnect.getAdXConnectEventInstance(BBBApplication.getApplication(), AnalyticsHelper.ADX_EVENT_SALE, String.valueOf(receipt.price.amount), receipt.price.currency);

                // The marketing team requested a separate (in addition to the existing) Ad-x ping to indicate a free or paid sale, so here it is...
                String extraAdxMessage = (receipt.price.amount > 0) ? AnalyticsHelper.ADX_EVENT_SALE_PAID : AnalyticsHelper.ADX_EVENT_SALE_FREE;
                AdXConnect.getAdXConnectEventInstance(BBBApplication.getApplication(), extraAdxMessage, String.valueOf(receipt.price.amount), receipt.price.currency);
            }
        }
    }

    /*
     * Helper method for checking if the response represents an unverified user error
     */
    private boolean isUnverifiedUserError(BBBResponse response) {

        if (response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {

            try {
                String fieldValue = response.getAuthenticateHeaderField(BBBApiConstants.PARAM_ERROR_REASON);

                if (BBBApiConstants.ERROR_UNVERIFIED_IDENTITY.equals(fieldValue)) {
                    LogUtils.e(TAG, "user is unverified");
                    return true;
                }

            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }

        return false;
    }

    /**
     * Adds a DataUpdatedListener to the list of listeners
     *
     * @param listener the listener you want to add
     */
    public void addUpdateListener(DataUpdatedListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given DataUpdatedListener from the list of listeners
     *
     * @param listener the listener you want to remove
     */
    public void removeUpdateListener(DataUpdatedListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners that data has changed
     */
    public void notifyUpdated() {

        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).updated();
        }
    }

    /**
     * Implement this interface and add to list of listeners via addUpdateListener to be notified of changes to data
     */
    public interface DataUpdatedListener {
        public void updated();
    }

    public interface SynopsisCallback {
        public void updateSynopsis(BBBSynopsis synopsis);
    }

    private BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (mBaseActivity != null && mBaseActivity.isInForeground()) {

                if (mState == STATE_PURCHASING_WITH_CREDIT || mState == STATE_PURCHASING_WITH_CARD) {
                    completePurchase();
                } else if (mState == STATE_INITIALISING_BOOK_PURCHASE) {

                    if(mShopItem != null) {
                        new PurchaseInitialisation().execute();
                    }
                } else if (mState == STATE_ADDING_SAMPLE) {
                    addSample(mISBN);
                }
            }
        }
    };

    private boolean isBaseActivityInForeground() {
        return (mBaseActivity != null) && (!mBaseActivity.hasSavedInstanceState());
    }

    private void registerForActivityCallbacks(Application application) {
        if (mActivityCallbackListener == null) {
            mActivityCallbackListener = new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    //do nothing
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    //do nothing
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    //do nothing
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    if (activity == mBaseActivity) {
                        mBaseActivity = null;
                        mActivityCallbackListener = null;
                        activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    //do nothing
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    //do nothing
                }
            };
            application.registerActivityLifecycleCallbacks(mActivityCallbackListener);
        }
    }
}
