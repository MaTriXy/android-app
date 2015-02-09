// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBUIUtils;

/**
 * View class representing one of the items in a row of shop items
 */
public class ShopItemView extends LinearLayout {

    public ShopItemView(Context context, String screenName) {
        super(context);
        mScreenName = screenName;
        initialise(context);
    }

    protected ShopItem mShopItem;
    private BookCover mBookCover;

    private TextView mTextViewTitle;
    private TextView mTextViewAuthor;
    private TextView mTextViewPrice;
    private TextView mTextViewPriceOriginal;

    private Button mButtonBuy;
    private String mScreenName;

    public ShopItemView(Context context) {
        super(context);
    }

    public ShopItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShopItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void initialise(Context context) {
        View.inflate(context, R.layout.view_shop_item, this);

        mBookCover = (BookCover) findViewById(R.id.bookcover);

        mTextViewTitle = (TextView)findViewById(R.id.textview_title);
        mTextViewAuthor = (TextView)findViewById(R.id.textview_author);
        mTextViewPrice = (TextView)findViewById(R.id.textview_price);
        mTextViewPriceOriginal = (TextView)findViewById(R.id.textview_price_original);

        mButtonBuy = (Button)findViewById(R.id.button_buy);

        mButtonBuy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsHelper.getInstance().sendAddToCart(mScreenName, mShopItem);
                PurchaseController.getInstance().buyPressed(mShopItem);
            }
        });

        setVisibility(View.INVISIBLE);
    }

    public void setViewWidth(int width) {

        if (mBookCover != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mBookCover.getLayoutParams();

            params.width = width;
            params.height = (int) (width * LibraryController.WIDTH_HEIGHT_RATIO);
            params.gravity = Gravity.CENTER_HORIZONTAL;

            mBookCover.setLayoutParams(params);
        }
    }

    /**
     * Resume the image loading in the case that it was suspended
     */
    public void resumeImageLoading() {
        mBookCover.resumeImageLoading();
    }

    /**
     * Sets the data to be displayed by this library item. The view will configure itself based on the properties of the data
     *
     * @param shopItem the shop item to be displayed
     * @param suspendImageLoading set to true if we want to suspend the image loading for the book cover
     */
    public void setData(ShopItem shopItem, boolean suspendImageLoading) {

        if(shopItem != null) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.INVISIBLE);
        }

        mShopItem = shopItem;

        mBookCover.setBook(shopItem.book, suspendImageLoading, true);
        mBookCover.setPosition(shopItem.position);

        mTextViewTitle.setText(shopItem.book.title);
        mTextViewAuthor.setText(shopItem.book.author);

        if (mShopItem.price.currency == null) {
            //if there is no currency set, then there is no price, and so cannot be bought
            mButtonBuy.setVisibility(View.INVISIBLE);
        } else if(mShopItem.book.publication_date > System.currentTimeMillis()) {
            // If the book has not yet been published (ie. its coming soon) then we hide the buy button
            mButtonBuy.setVisibility(View.INVISIBLE);
        } else {
            // If the item has already been purchased then we hide the buy button
            mButtonBuy.setVisibility(shopItem.book.purchase_date != 0L ? View.INVISIBLE : View.VISIBLE);
        }

        BBBUIUtils.setPriceText(shopItem.price, getResources(), mTextViewPrice, mTextViewPriceOriginal, null, null, true);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mBookCover.setOnClickListener(l);
    }

}