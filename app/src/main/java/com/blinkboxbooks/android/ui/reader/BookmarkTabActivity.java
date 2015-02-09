// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.ui.reader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.widget.BBBViewPager;
import com.blinkboxbooks.android.widget.SlidingTabLayout;

/**
 * The Dialog Fragment that hosts the bookmarks, highlights and notes
 */
public class BookmarkTabActivity extends BaseActivity {

    public static final String PARAM_BOOK = "book";

    public static final String EXTRA_KEY_TAB_BOOKMARKS = "bookmarks";
    public static final String EXTRA_KEY_TAB_HIGHLIGHTS = "highlights";
    public static final String EXTRA_KEY_TAB_NOTES = "notes";

    public static final int TAB_BOOKMARKS = 0;
    public static final int TAB_HIGHLIGHTS = 1;
    public static final int TAB_NOTES = 2;

    private SlidingTabLayout mSlidingTabLayout;
    private BBBViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndToolbarState(R.layout.activity_bookmarks_tabs,true);

        final ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (BBBViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case TAB_BOOKMARKS:
                         return BookmarkListFragment.instantiate(BookmarkTabActivity.this, BookmarkListFragment.class.getName(), getIntent().getExtras());
                    case TAB_HIGHLIGHTS:
                        return HighlightListFragment.instantiate(BookmarkTabActivity.this, HighlightListFragment.class.getName(), getIntent().getExtras());
                    case TAB_NOTES:
                        return NoteListFragment.instantiate(BookmarkTabActivity.this, NoteListFragment.class.getName(), getIntent().getExtras());
                }
                return null;
            }

            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case TAB_BOOKMARKS:
                        return getString(R.string.bookmarks);
                    case TAB_HIGHLIGHTS:
                        return getString(R.string.highlights);
                }
                return null;
            }
        });

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setCustomTabView(R.layout.tab_view,android.R.id.text1);
        mSlidingTabLayout.setTabsEqualsWeight(true);

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
        mScreenName = AnalyticsHelper.GA_SCREEN_Reader_MyBookmarksScreen;
    }
}
