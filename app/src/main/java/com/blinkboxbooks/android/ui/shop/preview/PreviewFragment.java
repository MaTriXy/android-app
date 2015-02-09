// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.preview;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.util.AnalyticsHelper;

/**
 * Fragment for showing a shop item and author information in a ViewPager
 */
public class PreviewFragment extends Fragment {

    public static final int TAB_SHOP_ITEM = 0;
    public static final int TAB_AUTHOR = 1;

    private static final String ARG_SHOP_ITEM = "shop_item";

    public static PreviewFragment newInstance(ShopItem shopItem) {
        PreviewFragment fragment = new PreviewFragment();

        Bundle args = new Bundle();
        args.putSerializable(ARG_SHOP_ITEM, shopItem);
        fragment.setArguments(args);

        AnalyticsHelper.getInstance().sendViewProductPage(AnalyticsHelper.GA_SCREEN_Shop_BookDetailsScreen + shopItem.book.isbn, shopItem);
        return fragment;
    }

    private ViewPager mViewPager;
    private ShopItem mShopItem;

    private ShopItemFragment mShopItemFragment;
    private AuthorFragment mAuthorFragment;
    private boolean mSetAuthorFragmentToViewedOnInit;

    public void setShopItem(ShopItem shopItem) {
        mShopItem = shopItem;

        if(mShopItemFragment != null) {
            mShopItemFragment.setShopItem(mShopItem);
        }

        if(mAuthorFragment != null) {
            mAuthorFragment.setAuthorId(mShopItem.book.authorId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(savedInstanceState == null) {
            mShopItem = (ShopItem)getArguments().getSerializable(ARG_SHOP_ITEM);
        } else {
            mShopItem = (ShopItem)savedInstanceState.getSerializable(ARG_SHOP_ITEM);
        }

        View view = inflater.inflate(R.layout.fragment_preview, container, false);

        mViewPager = (ViewPager)view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new PreviewPagerAdapter(getFragmentManager()));
        mViewPager.setOffscreenPageLimit(2);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {}

            @Override
            public void onPageSelected(int position) {
                // If we are displaying the author tab then set its viewed state so we report impressions to GA
                if (position == TAB_AUTHOR) {
                    getActivity().setTitle(R.string.about_this_author);
                    if (mAuthorFragment != null){
                        mAuthorFragment.setHasBeenViewed(true);
                    }
                } else {
                    getActivity().setTitle(R.string.book_description);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {}
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_SHOP_ITEM, mShopItem);
    }

    private class PreviewPagerAdapter extends FragmentPagerAdapter {

        private PreviewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch(position) {

                case TAB_SHOP_ITEM: {
                    mShopItemFragment = ShopItemFragment.newInstance(mShopItem);
                    return mShopItemFragment;
                }
                case TAB_AUTHOR: {
                    mAuthorFragment = AuthorFragment.newInstance(mShopItem.book.authorId);
                    if (mSetAuthorFragmentToViewedOnInit) {
                        mAuthorFragment.setHasBeenViewed(true);
                    }
                    return mAuthorFragment;
                }
            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Bundle saveState() {
            // Prevent the adapter from saving state. This is required because on a device rotation the fragments
            // would normally be retained. Our references to mShopItemFragment and mAuthorFragment would not be restored
            // so updating the shop item would fail after rotation.
            return null;
        }
    }

    public void setTab(int tab) {

        if(mViewPager != null) {
            mViewPager.setCurrentItem(tab);

            // If we are displaying the author tab then set its viewed state so we report impressions to GA
            if (tab == TAB_AUTHOR) {
                if (mAuthorFragment != null) {
                    mAuthorFragment.setHasBeenViewed(true);
                } else {
                    // Because the author fragment might not be created at this stage, we set a flag to set the viewed state when it is created
                    mSetAuthorFragmentToViewedOnInit = true;
                }
            }
        }
    }

    /**
     * Get the currently selected tab.
     * @return the index of the current tab
     */
    public int getTab() {
       return mViewPager.getCurrentItem();
    }
}