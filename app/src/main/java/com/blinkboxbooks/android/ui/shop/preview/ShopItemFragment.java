// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.preview;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBSynopsis;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.dialog.AddSampleDialogFragment;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.reader.ReaderActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.BBBUIUtils;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.StringUtils;
import com.blinkboxbooks.android.widget.BookCover;

/**
 * Fragment for showing information about a single book
 */
public class ShopItemFragment extends Fragment {

    private static final String TAG_DIALOG_FRAGMENT = "dialog_fragment";
    private static final String ARG_SHOP_ITEM = "shop_item";

    private static final String REQUEST_ID = ShopItemFragment.class.getName();

    /**
     * Creates a new instance of this fragment
     *
     * @param shopItem the shop item
     * @return
     */
    public static ShopItemFragment newInstance(ShopItem shopItem) {
        ShopItemFragment fragment = new ShopItemFragment();

        Bundle args = new Bundle();
        args.putSerializable(ARG_SHOP_ITEM, shopItem);
        fragment.setArguments(args);

        return fragment;
    }

    private ShopItem mShopItem;

    private TextView mTextViewTitle;
    private TextView mTextViewAuthor;
    private TextView mTextViewPublished;
    private TextView mTextViewDescription;

    private TextView mTextViewPrice;
    private TextView mTextViewDiscountPrice;
    private TextView mTextViewClubcardPoints;
    private TextView mTextViewPurchasedOn;
    private ImageView mImageViewClubcard;
    private View mViewPreview;
    private View mViewPrice;
    private View mReadNowDivider;
    private TextView mTextViewCantPurchaseYet;

    private BookCover mBookCover;
    private Button mButtonBuy;
    private View mButtonGetSample;

    public ShopItemFragment() { }

    /**
     * Sets the shop item to display and update the UI
     * @param shopItem
     */
    public void setShopItem(ShopItem shopItem) {
        mShopItem = shopItem;

        mTextViewTitle.setText(mShopItem.book.title);
        mTextViewAuthor.setText(mShopItem.book.author);
        mTextViewAuthor.setPaintFlags(mTextViewAuthor.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        if (mShopItem.book.publication_date == 0) {
            mTextViewPublished.setVisibility(View.INVISIBLE);
        } else {
            String date = BBBCalendarUtil.formatDate(mShopItem.book.publication_date);
            mTextViewPublished.setText(String.format(getString(R.string.date_of_publication), date));
        }

        mBookCover.setBook(mShopItem.book);

        if (mShopItem.book.description == null || mShopItem.book.description.length() == 0) {
            BBBRequest request = BBBRequestFactory.getInstance().createGetSynopsisRequest(mShopItem.book.isbn);
            BBBRequestManager.getInstance().executeRequest(REQUEST_ID, request);
        } else {
            mTextViewDescription.setText(Html.fromHtml(mShopItem.book.description));
        }

        if(!mShopItem.book.sample_eligible) {
            mButtonGetSample.setVisibility(View.INVISIBLE);
            mViewPreview.setVisibility(View.GONE);
        } else {
            mButtonGetSample.setVisibility(View.VISIBLE);
            mViewPreview.setVisibility(View.VISIBLE);
        }

        if (mShopItem.book.purchase_date != 0L) {
            mViewPrice.setVisibility(View.GONE);
            if (mReadNowDivider != null) {
                mReadNowDivider.setVisibility(View.VISIBLE);
            }
            mTextViewPurchasedOn.setVisibility(View.VISIBLE);
            mButtonGetSample.setVisibility(View.INVISIBLE);
            mButtonBuy.setText(R.string.button_read_now);
            mViewPreview.setVisibility(View.GONE);

            final String purchaseDate = BBBCalendarUtil.formatDate(mShopItem.book.purchase_date);
            final String purchaseDateString = getString(R.string.you_purchased_this_book_about, purchaseDate);
            mTextViewPurchasedOn.setText(purchaseDateString);
        } else {
            mViewPrice.setVisibility(View.VISIBLE);
            if (mReadNowDivider != null) {
                mReadNowDivider.setVisibility(View.GONE);
            }
            mTextViewPurchasedOn.setVisibility(View.GONE);
            mButtonBuy.setText(R.string.button_buy_now);

            if (mShopItem.book.publication_date < System.currentTimeMillis()) {
                //if there is no currency set, then there is no price, and so this book is not buyable
                if (mShopItem.price.currency == null) {
                    mButtonBuy.setVisibility(View.INVISIBLE);
                } else {
                    mButtonBuy.setVisibility(View.VISIBLE);
                }
                mButtonGetSample.setVisibility(View.VISIBLE);
                mViewPreview.setVisibility(View.VISIBLE);
                mTextViewCantPurchaseYet.setVisibility(View.GONE);
            } else {
                mButtonBuy.setVisibility(View.GONE);
                mButtonGetSample.setVisibility(View.GONE);
                mViewPreview.setVisibility(View.GONE);
                mViewPrice.setVisibility(View.GONE);
                String formattedDate = BBBCalendarUtil.formatDate(mShopItem.book.publication_date);
                String message = getString(R.string.error_book_not_yet_published_date, formattedDate);
                mTextViewCantPurchaseYet.setText(message);
                mTextViewCantPurchaseYet.setVisibility(View.VISIBLE);
            }
        }

        BBBUIUtils.setPriceText(mShopItem.price, getResources(), mTextViewDiscountPrice, mTextViewPrice, mTextViewClubcardPoints, mImageViewClubcard, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_item_preview, container, false);

        mTextViewTitle = (TextView)view.findViewById(R.id.textview_title);
        mTextViewAuthor = (TextView)view.findViewById(R.id.textview_author);
        mTextViewPublished = (TextView)view.findViewById(R.id.textview_published);
        mTextViewDescription = (TextView)view.findViewById(R.id.textview_description);
        mTextViewPrice = (TextView)view.findViewById(R.id.textview_price_original);
        mTextViewDiscountPrice = (TextView)view.findViewById(R.id.textview_price_discounted);
        mTextViewClubcardPoints = (TextView)view.findViewById(R.id.textview_clubcard_points);
        mImageViewClubcard = (ImageView)view.findViewById(R.id.imageview_clubcard);
        mButtonBuy = (Button) view.findViewById(R.id.button_buy);
        mButtonGetSample = view.findViewById(R.id.button_add_sample);
        mTextViewPurchasedOn = (TextView) view.findViewById(R.id.textview_purchased_on);
        mBookCover = (BookCover)view.findViewById(R.id.bookcover);
        mViewPreview = view.findViewById(R.id.layout_preview);
        mViewPrice = view.findViewById(R.id.layout_price);
        mReadNowDivider = view.findViewById(R.id.read_now_divider);
        mTextViewCantPurchaseYet = (TextView) view.findViewById(R.id.textview_cant_purchase_yet);

        mTextViewAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();

                if(activity instanceof ComponentClickedListener) {
                    ((ComponentClickedListener)activity).authorPressed();
                }
            }
        });

        mButtonBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mShopItem != null) {
                    if (mShopItem.book.purchase_date == 0L) {
                        if(NetworkUtils.hasInternetConnectivity(getActivity())) {
                            AnalyticsHelper.getInstance().sendAddToCart(AnalyticsHelper.GA_SCREEN_Shop_BookDetailsScreen + mShopItem.book.isbn, mShopItem);
                            PurchaseController.getInstance().buyPressed(mShopItem);
                        } else {
                            ((BaseActivity)getActivity()).showMessage(null, getString(R.string.error_no_internet_buy));
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("bbb://app/library/"+mShopItem.book.id));
                        startActivity(intent);
                    }
                }
            }
        });

        mButtonGetSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AddSampleDialogFragment addSampleDialogFragment = AddSampleDialogFragment.newInstance(new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        if (NetworkUtils.hasInternetConnectivity(getActivity())) {
                            PurchaseController.getInstance().addSample(mShopItem.book.isbn);
                        } else {
                            ((BaseActivity) getActivity()).showMessage(null, getString(R.string.error_no_internet_opening_shop_sample));
                        }
                    }
                }, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        if (NetworkUtils.hasInternetConnectivity(getActivity())) {
                            Intent readBookIntent = new Intent(getActivity(), ReaderActivity.class);
                            readBookIntent.putExtra(ReaderActivity.PARAM_SHOP_ITEM, mShopItem);
                            startActivity(readBookIntent);
                        } else {
                            ((BaseActivity) getActivity()).showMessage(null, getString(R.string.error_no_internet_opening_shop_sample));
                        }
                    }
                });

                addSampleDialogFragment.show(getFragmentManager(), TAG_DIALOG_FRAGMENT);
            }
        });

        view.findViewById(R.id.layout_preview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(NetworkUtils.hasInternetConnectivity(getActivity())) {
                    Intent readBookIntent = new Intent(getActivity(), ReaderActivity.class);
                    readBookIntent.putExtra(ReaderActivity.PARAM_SHOP_ITEM, mShopItem);
                    startActivity(readBookIntent);
                } else {
                    ((BaseActivity)getActivity()).showMessage(null, getString(R.string.error_no_internet_opening_shop_sample));
                }
            }
        });

        ShopItem item;

        if(savedInstanceState == null) {
            item = (ShopItem)getArguments().getSerializable(ARG_SHOP_ITEM);
        } else {
            item = (ShopItem)savedInstanceState.getSerializable(ARG_SHOP_ITEM);
        }

        setShopItem(item);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_SHOP_ITEM, mShopItem);
    }

    @Override
    public void onResume() {
        super.onResume();
        BBBRequestManager.getInstance().addResponseHandler(REQUEST_ID, mSynopsisHandler);
    }

    @Override
    public void onPause() {
        super.onPause();
        BBBRequestManager.getInstance().removeResponseHandler(REQUEST_ID);
    }

    private BBBBasicResponseHandler<BBBSynopsis> mSynopsisHandler = new BBBBasicResponseHandler<BBBSynopsis>() {

        public void receivedData(BBBResponse response, BBBSynopsis synopsis) {

            if (synopsis != null) {
                Uri uri = BBBContract.Books.buildBookIdUri(mShopItem.book.id);
                BookHelper.updateBookDescription(uri, synopsis.text);
                Spanned spanned = Html.fromHtml(synopsis.text);
                CharSequence trimmed = StringUtils.trimAllWhiteSpace(spanned, 0, spanned.length());
                mTextViewDescription.setText(trimmed);

            } else {
                mTextViewDescription.setText(R.string.error_no_network_shop);
            }
        }

        public void receivedError(BBBResponse response) {
            mTextViewDescription.setText(R.string.error_no_network_shop);
        }
    };

    public static class ShopItemOnClickListener implements View.OnClickListener {

        private ShopItem mShopItem;
        private Activity mActivity;
        private String mScreenName;

        public ShopItemOnClickListener(Activity activity, ShopItem shopItem, String screenName) {
            mShopItem = shopItem;
            mActivity = activity;
            mScreenName = screenName;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mActivity, PreviewActivity.class);
            intent.putExtra(PreviewActivity.ARG_SHOP_ITEM, mShopItem);
            mActivity.startActivity(intent);
            AnalyticsHelper.getInstance().sendClickOnProduct(mScreenName, mShopItem);
        }
    }
}