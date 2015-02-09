// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.widget.AboutBookItem;
import com.blinkboxbooks.android.widget.ShopItemView;

import java.util.ArrayList;

/**
 * Utility class for performing common operations on UI components
 */
public class BBBUIUtils {

    private static final float GAP_WEIGHT = 0.08f;
    private static final float BOOK_WEIGHT = 0.92f;

    public static final float WIDTH_HEIGHT_RATIO = 1.53f;

    /**
     * Sets the text for the price of a book. Formats the textviews correctly with strike through text for original price
     *
     * @param price
     * @param res
     * @param textViewPrice
     * @param textViewPriceOriginal
     * @param clubcardPointsTextView
     * @param clubcardImageView
     * @param twoLineDiscount
     */
    public static void setPriceText(BBBBookPrice price, Resources res, TextView textViewPrice, TextView textViewPriceOriginal, TextView clubcardPointsTextView, ImageView clubcardImageView, boolean twoLineDiscount) {

        if (price != null) {
            if (price.currency == null) {
                //there is no price set for this book, so exit early and make all invisible
                if (textViewPrice != null) {
                    textViewPrice.setVisibility(View.INVISIBLE);
                }

                if (textViewPriceOriginal != null) {
                    textViewPriceOriginal.setVisibility(View.INVISIBLE);
                }

                if (clubcardPointsTextView != null) {
                    clubcardPointsTextView.setVisibility(View.INVISIBLE);
                }

                if (clubcardImageView != null) {
                    clubcardImageView.setVisibility(View.INVISIBLE);
                }

                return;
            }

            if (price.discountPrice == -1) {
                textViewPrice.setText(StringUtils.formatPrice(price.currency, price.price, res.getString(R.string.free)));

                textViewPriceOriginal.setVisibility(View.INVISIBLE);
            } else {
                textViewPrice.setText(StringUtils.formatPrice(price.currency, price.discountPrice, res.getString(R.string.free)));

                if(price.discountPrice < price.price) {
                    textViewPriceOriginal.setVisibility(View.VISIBLE);
                    textViewPriceOriginal.setText(StringUtils.formatDiscount(res, twoLineDiscount ? R.string.shop_discount_2_lines : R.string.shop_discount, price.currency, price.price, price.discountPrice));
                } else {
                    textViewPriceOriginal.setVisibility(View.INVISIBLE);
                }
            }

            if (price.clubcardPointsAward > 0) {

                if(clubcardPointsTextView != null) {
                    clubcardPointsTextView.setVisibility(View.VISIBLE);
                    String pointsText = res.getString(R.string.shop_s_clubcard_points, price.clubcardPointsAward);
                    clubcardPointsTextView.setText(pointsText);
                }

                if(clubcardImageView != null) {
                    clubcardImageView.setVisibility(View.VISIBLE);
                }
            } else {

                if(clubcardImageView != null) {
                    clubcardImageView.setVisibility(View.INVISIBLE);
                }

                if(clubcardPointsTextView != null) {
                    clubcardPointsTextView.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    /**
     * Creates a row of about books
     *
     * @param context
     * @param columns
     * @param bookViews
     * @return the width of an individual book
     */
    public static int createAboutBookRow(Context context, LinearLayout layout, int columns, ArrayList<AboutBookItem> bookViews, int totalWidth) {
        layout.setWeightSum(columns + (GAP_WEIGHT * (columns+1)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams gap_params = new LinearLayout.LayoutParams(0, 1, GAP_WEIGHT);

        View view;
        int j = 0;
        AboutBookItem aboutBookItem;

        view = new View(context);
        layout.addView(view, j++, gap_params);

        PointF itemDimens = calculateRowItemDimens(context, columns, BOOK_WEIGHT, GAP_WEIGHT, totalWidth);
        int width = (int) itemDimens.x;

        for (int i = 0; i < columns; i++, j++) {
            aboutBookItem = new AboutBookItem(context);
            aboutBookItem.setTag(String.valueOf(i));

            if(bookViews != null) {
                bookViews.add(aboutBookItem);
            }

            aboutBookItem.setBookCoverWidth(width);

            layout.addView(aboutBookItem, j, params);
            j++;

            if (i != columns - 1) {
                view = new View(context);
                layout.addView(view, j, gap_params);
            }
        }

        return width;
    }

    /**
     * Creates a row of shop items
     *
     * @param context
     * @param columns
     * @param shopViews
     * @param screenName the screen that this shop row is within
     * @return
     */
    public static View createShopItemRow(Context context, int columns, ArrayList<ShopItemView> shopViews, int totalWidth, String screenName) {
        LinearLayout layout = new LinearLayout(context);

        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setWeightSum(columns + (GAP_WEIGHT * (columns+1)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.bottomMargin = (int) context.getResources().getDimension(R.dimen.gap_large);

        LinearLayout.LayoutParams gap_params = new LinearLayout.LayoutParams(0, 1, GAP_WEIGHT);

        View view;
        int j = 0;
        ShopItemView shopItemView;

        view = new View(context);
        layout.addView(view, j++, gap_params);

        PointF itemDimens = calculateRowItemDimens(context, columns, BOOK_WEIGHT, GAP_WEIGHT, totalWidth);
        int width = (int)itemDimens.x;

        for (int i = 0; i < columns; i++, j++) {

            shopItemView = new ShopItemView(context, screenName);
            shopItemView.setTag(String.valueOf(i));

            if(shopViews != null) {
                shopViews.add(shopItemView);
            }

            shopItemView.setViewWidth(width);

            layout.addView(shopItemView, j, params);
            j++;

            if (i != columns - 1) {
                view = new View(context);
                layout.addView(view, j, gap_params);
            }
        }

        return layout;
    }

    /**
     * Calculates the dimensions of an item in a grid
     *
     * @param context
     * @param numberOfColumns
     * @param itemWeight
     * @param gapWeight
     * @return
     */
    public static PointF calculateRowItemDimens(Context context, int numberOfColumns, float itemWeight, float gapWeight, int width) {
        float totalWeight = (numberOfColumns * itemWeight) + (numberOfColumns * gapWeight);

        PointF point = new PointF();
        point.x = (width * (itemWeight / totalWeight));
        point.y = (point.x * WIDTH_HEIGHT_RATIO);
        return point;
    }

    @SuppressLint("NewApi")
    public static final int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        Point screenSize = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(screenSize);
        } else {
            display.getSize(screenSize);
        }

        return screenSize.x;
    }

    /**
     * Calculates the width of an item in a grid view
     *
     * @param context
     * @param mGridView
     * @return
     */
    public static int getGridItemWidth(Context context, GridView mGridView) {
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);

        int side = Math.min(size.x, size.y);

        return side/mGridView.getNumColumns();
    }

    /**
     * Gets the width of a view based on its layout weight
     *
     * @param view
     * @return
     */
    public static int getItemWidthBasedOnWeight(View view) {

        if(view == null) {
            return 0;
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
        return (int)(params.weight * getScreenWidth(view.getContext()));
    }

    /**
     * Get the current screen orientation
     * @param activity
     * @return ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
     */
    public static int getScreenOrientation(Activity activity)
    {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Default to portrait orientation
        int orientation = Configuration.ORIENTATION_PORTRAIT;

        if(size.x > size.y) {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
        }
        return orientation;
    }
}