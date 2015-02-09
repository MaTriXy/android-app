// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.ui.shop.pages.CategoriesFragment;
import com.blinkboxbooks.android.ui.shop.pages.feature.FeaturedFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.widget.BBBViewPager;
import com.blinkboxbooks.android.widget.SlidingTabLayout;

/**
 * Display the main shop view. It contains all tabs.
 */
public class ShopFragment extends Fragment {

    private static final String CATEGORY_BESTSELLERS_FICTION = "bestsellers-fiction";
    private static final String CATEGORY_BESTSELLERS_NON_FICTION = "bestsellers-non-fiction";
    private static final String CATEGORY_FREE = "top-free";
    private static final String CATEGORY_NEW_RELEASES = "new-releases";


    private SlidingTabLayout mSlidingTabLayout;

    private BBBViewPager mViewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (BBBViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new ShopPagerAdapter(getFragmentManager()));
        mViewPager.setSwipeEnabled(true);

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setCustomTabView(R.layout.tab_view,android.R.id.text1);
        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.brand_purple);
            }

            @Override
            public int getDividerColor(int position) {
                return getResources().getColor(R.color.button_outline_grey);
            }
        });

        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {}

            @Override
            public void onPageSelected(int i) {
                Fragment fragment = ((ShopPagerAdapter) mViewPager.getAdapter()).getFragment(i);
                if (fragment != null && fragment instanceof ImpressionReporterFragment) {
                    // Inform the fragment it has been viewed so it will report any book impressions
                    ((ImpressionReporterFragment) fragment).setHasBeenViewed(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {}
        });
    }

    /**
     * Shows a page
     *
     * @param page
     */
    public void showPage(int page) {
        mViewPager.setCurrentItem(page);
    }

    private class ShopPagerAdapter extends FragmentStatePagerAdapter {

        // Used to store a reference to the fragments within the adapter
        private SparseArray<Fragment> mPageReferenceArray = new SparseArray<Fragment>();

        private ShopPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch(position) {
                case 0: {
                    // The featured fragment will be visible at startup so pass true to indicate this
                    Fragment fragment = FeaturedFragment.newInstance(true);
                    mPageReferenceArray.put(position, fragment);
                    return fragment;
                }
                case 1: {
                    Fragment fragment = CategoriesFragment.newInstance();
                    mPageReferenceArray.put(position, fragment);
                    return fragment;
                }
                case 2: {
                    Fragment fragment = BooksFragment.newInstance(AnalyticsHelper.GA_SCREEN_Shop_Bestsellers_Fiction, CatalogueContract.Books.getBooksForCategoryNameUri(CATEGORY_BESTSELLERS_FICTION, true, 100, 0), true, 100, false, SortOption.SEQUENTIAL);
                    mPageReferenceArray.put(position, fragment);
                    return fragment;
                }
                case 3: {
                    Fragment fragment = BooksFragment.newInstance(AnalyticsHelper.GA_SCREEN_Shop_Bestsellers_Non_Fiction, CatalogueContract.Books.getBooksForCategoryNameUri(CATEGORY_BESTSELLERS_NON_FICTION, true, 100, 0), true, 100, false, SortOption.SEQUENTIAL);
                    mPageReferenceArray.put(position, fragment);
                    return fragment;
                }
                case 4: {
                    Fragment fragment = BooksFragment.newInstance(AnalyticsHelper.GA_SCREEN_Shop_Free, CatalogueContract.Books.getBooksForCategoryNameUri(CATEGORY_FREE, true, BooksFragment.DEFAULT_PAGE_SIZE, 0), false, BooksFragment.DEFAULT_PAGE_SIZE, true,
                            SortOption.SEQUENTIAL, SortOption.PUBLICATION_DATE, SortOption.TITLE_ASCENDING, SortOption.TITLE_DESCENDING);
                    mPageReferenceArray.put(position, fragment);
                    return fragment;
                }
                case 5: {
                    Fragment fragment = BooksFragment.newInstance(AnalyticsHelper.GA_SCREEN_Shop_New_Released, CatalogueContract.Books.getBooksForCategoryNameUri(CATEGORY_NEW_RELEASES, true, BooksFragment.DEFAULT_PAGE_SIZE, 0), false, BooksFragment.DEFAULT_PAGE_SIZE, true,
                            SortOption.SEQUENTIAL, SortOption.BESTSELLING, SortOption.TITLE_ASCENDING, SortOption.TITLE_DESCENDING,
                            SortOption.PRICE_ASCENDING, SortOption.PRICE_DESCENDING, SortOption.PUBLICATION_DATE);
                    mPageReferenceArray.put(position, fragment);
                    return fragment;
                }
            }

            return null;
        }

        @Override
        public int getCount() {
            return ShopActivity.tabTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(ShopActivity.tabTitles[position]);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);

            // Clear the fragment reference at the destroyed position
            mPageReferenceArray.remove(position);
        }

        /**
         * Get the fragment at the specified position
         * @param position the position within the adapter to get the fragment from
         * @return the Fragment at the position (or null if none)
         */
        public Fragment getFragment(int position) {
            return mPageReferenceArray.get(position);
        }
    }
}
