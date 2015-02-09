// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.BookDownloadController;
import com.blinkboxbooks.android.controller.DrawerMenuController;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.dialog.UpdateInfoDialogFragment;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.MenuListItem;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.LibraryHelper;
import com.blinkboxbooks.android.model.updateinfo.UpdateInfo;
import com.blinkboxbooks.android.model.updateinfo.VersionInfo;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.sync.Synchroniser;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.WebContentActivity;
import com.blinkboxbooks.android.ui.account.LoginActivity;
import com.blinkboxbooks.android.ui.shop.ShopActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.AppUpgradeHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.BBBUriManager;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.PreferenceManager;
import com.blinkboxbooks.android.util.UpdateInfoHelper;
import com.blinkboxbooks.android.widget.BBBViewPager;
import com.blinkboxbooks.android.widget.InfoPanel;
import com.blinkboxbooks.android.widget.SlidingTabLayout;
import com.crashlytics.android.Crashlytics;

/**
 * The landing screen for the app. A {@link DrawerLayout} is used to show the Drawer menu. This activity will
 * show the users current library of books divided into two sections: Books they are reading and all book in the
 * library.
 */
public class LibraryActivity extends BaseActivity implements DrawerMenuController.OnMenuItemSelectedListener, BookCountHandler, AppUpgradeHelper.AppUpgradeCallback  {

    public static final String ACTION_HELP = "help";
    public static final String ACTION_CURRENT = "current";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_CURRENTLY_READING = "currently_reading";
    public static final String ACTION_MY_LIBRARY = "my_library";

    public static final int TAB_READING = 0;
    public static final int TAB_MY_LIBRARY = 1;

    private SlidingTabLayout mSlidingTabLayout;
    private BBBViewPager mViewPager;
    private LibraryPagerAdapter mLibraryPagerAdapter;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private PreferenceManager mPreferenceManager;
    private View mRegisterPanel;
    private LinearLayout mInfoPanelContainer;
    private boolean mIsLoggedIn;
    private DrawerMenuFragment mDrawerFragment;

    private BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null) {

                LibraryBooksFragment readingFragment = (LibraryBooksFragment) mLibraryPagerAdapter.getFragment(TAB_READING);
                LibraryBooksFragment myLibraryFragment = (LibraryBooksFragment) mLibraryPagerAdapter.getFragment(TAB_MY_LIBRARY);

                if (action.equals(Synchroniser.ACTION_SYNC_STARTED)) {
                    if (readingFragment != null) { readingFragment.startSync(); }
                    if (myLibraryFragment != null) { myLibraryFragment.startSync(); }
                } else if (action.equals(Synchroniser.ACTION_SYNC_STOPPED)) {
                    if (readingFragment != null) { readingFragment.stopSync(); }
                    if (myLibraryFragment != null) { myLibraryFragment.stopSync(); }
                }

                // To handle the situation where the user logs in via the buy full book route, we check the logged in status
                // here and hide the register panel if they are logged in.
                mIsLoggedIn = AccountController.getInstance().isLoggedIn();
                if (mIsLoggedIn) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRegisterPanel.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onRefresh();

        setContentViewAndToolbarState(R.layout.activity_library,false);

        mPreferenceManager = PreferenceManager.getInstance();

        mInfoPanelContainer = (LinearLayout) findViewById(R.id.info_panel_container);
        mRegisterPanel = findViewById(R.id.why_register_panel);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(Color.WHITE);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, (Toolbar) findViewById(R.id.toolbar), R.string.drawer_open, R.string.drawer_close);

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View view, float v) {
                mDrawerToggle.onDrawerSlide(view, v);
            }

            @Override
            public void onDrawerOpened(View view) {
                mDrawerToggle.onDrawerOpened(view);
            }

            @Override
            public void onDrawerClosed(View view) {
                mDrawerToggle.onDrawerClosed(view);
            }

            @Override
            public void onDrawerStateChanged(int i) {
                mDrawerToggle.onDrawerStateChanged(i);
            }
        });

        Button registerButton = (Button) findViewById(R.id.library_register_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("bbb://app/register/")));
            }
        });

        TextView signIn = (TextView) findViewById(R.id.library_register_sign_in);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("bbb://app/login/")));
            }
        });

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (BBBViewPager) findViewById(R.id.viewpager);
        mLibraryPagerAdapter = new LibraryPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mLibraryPagerAdapter);

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

        mDrawerFragment = (DrawerMenuFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_drawermenu);

        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                if (mViewPager.getCurrentItem() == TAB_READING) {
                    AnalyticsHelper.getInstance().stopTrackingUIComponent(AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_MyLibrary);
                    AnalyticsHelper.getInstance().startTrackingUIComponent(AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_Reading);
                    mPreferenceManager.setPreference(PreferenceManager.PREF_KEY_LIBRARY_TAB_SELECTED, TAB_READING);
                } else if (mViewPager.getCurrentItem() == TAB_MY_LIBRARY) {
                    AnalyticsHelper.getInstance().stopTrackingUIComponent(AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_Reading);
                    AnalyticsHelper.getInstance().startTrackingUIComponent(AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_MyLibrary);
                    mPreferenceManager.setPreference(PreferenceManager.PREF_KEY_LIBRARY_TAB_SELECTED, TAB_MY_LIBRARY);
                }
                mDrawerFragment.createMenuItems();
            }
        });

        // Reinstate the last tab the user looked at (which persists between application sessions)
        int selectedTab = mPreferenceManager.getInt(PreferenceManager.PREF_KEY_LIBRARY_TAB_SELECTED, 1);
        mViewPager.setCurrentItem(selectedTab);

        try {
            // At this point the stored app version will refer to the previous version that was installed if its the first run after the install.
            // We default to a version number of all zeros if there is no prior version stored.
            String oldVersion = VersionInfo.stripBuildNumber(PreferenceManager.getInstance().getString(PreferenceManager.PREF_KEY_STORED_APP_VERSION, "0.0.0-0"));
            String currentVersion = VersionInfo.stripBuildNumber(BuildConfig.VERSION_NAME);

            // Update the stored app version at this point so we wont ever show the app version again (at least until another upgrade occurs)
            PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_STORED_APP_VERSION, BuildConfig.VERSION_NAME);

            // The version strings above should now be in the format x.y.z
            UpdateInfo updateInfo = UpdateInfoHelper.getUpdateInfo(this, new VersionInfo(oldVersion, "\\."), new VersionInfo(currentVersion, "\\."));
            if (updateInfo != null) {
                int dialogWidth = getResources().getDimensionPixelSize(R.dimen.update_info_dialog_width);
                final UpdateInfoDialogFragment updateInfoDialogFragment = UpdateInfoDialogFragment.newInstance(updateInfo.updates, dialogWidth);
                showDialog(updateInfoDialogFragment, "update_info", false);
            }
        } catch (Exception e) {
            // Just in case the build number format changes or something weird happens we don't want to crash the app.
            // In this case we just swallow the exception and don't bother showing the whats new dialog.
            Crashlytics.logException(e);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mInfoPanelContainer.removeAllViews();
        PurchaseController.getInstance().setBaseActivity(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Synchroniser.ACTION_SYNC_STARTED);
        filter.addAction(Synchroniser.ACTION_SYNC_STOPPED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mSyncReceiver, filter);

        mIsLoggedIn = AccountController.getInstance().isLoggedIn();

        // If the user is not logged in then we should display the register panel (at the bottom) and the information
        // panel (if it has not been shown and dismissed previously)
        if (! mIsLoggedIn) {
            mRegisterPanel.setVisibility(View.VISIBLE);
        } else {
            mRegisterPanel.setVisibility(View.GONE);
        }

        boolean showPreloadInfo = mPreferenceManager.getBoolean(PreferenceManager.PREF_KEY_SHOW_PRELOAD_INFORMATION, true);
        if (showPreloadInfo && (! mIsLoggedIn || AccountController.getInstance().isNewUser())) {

            View.OnClickListener dismissClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPreferenceManager.setPreference(PreferenceManager.PREF_KEY_SHOW_PRELOAD_INFORMATION, false);
                }
            };

            showNewInfoPanel(getString(R.string.preload_information), getResources().getColor(R.color.message_banner_blue), null, dismissClickListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncReceiver);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.library_actionbar_options, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;

            case R.id.action_shop:
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                        .CATEGORY_LIBRARY_GESTURE, AnalyticsHelper.GA_EVENT_SHOP, "", null);
                Intent intent = new Intent(this, ShopActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {

            // In case we come in here via a marketing deep link we want to track in Ad-X
            AnalyticsHelper.handleAdXDeepLink(this, intent);

            String item = intent.getData().getLastPathSegment();

            if (ACTION_HELP.equals(item)) {
                showHelp();
            } else if (ACTION_RESET.equals(item)) {
                LibraryHelper.confirmResetAnonymousSettings(this);
            } else if (ACTION_CURRENTLY_READING.equals(item)) {
                mViewPager.setCurrentItem(TAB_READING);
            } else if (ACTION_MY_LIBRARY.equals(item)) {
                mViewPager.setCurrentItem(TAB_MY_LIBRARY);
            } else if (ACTION_CURRENT.equals(item)) {

                if (AccountController.getInstance().isLoggedIn()) {
                    String name = AccountController.getInstance().getUserId();

                    Uri uri = BBBContract.Books.buildBookStatusUri(name, String.valueOf(BBBContract.Books.BOOK_STATE_READING));
                    Cursor cursor = getContentResolver().query(uri, null, null, null, BBBContract.BooksColumns.BOOK_UPDATE_DATE + " DESC");

                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        Book book = BookHelper.createBook(cursor);

                        // If the book is in the downloaded state we open it. It its not downloaded we the user will
                        // just remain in the library.
                        if (book.download_status == BBBContract.Books.DOWNLOADED) {
                            intent = LibraryController.getOpenBookIntent(this, book);
                            startActivity(intent);
                        }
                    }
                } else {
                    intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                }
            } else {
                try {
                    long bookId = Long.parseLong(item);

                    if (AccountController.getInstance().isLoggedIn()) {
                        Uri uri = BBBContract.Books.buildBookIdUri(bookId);
                        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

                        if (cursor.moveToFirst()) {
                            Book book = BookHelper.createBook(cursor);

                            // If the book is in the downloaded state we open it. It its not downloaded we the user will
                            // just remain in the library.
                            if (book.download_status == BBBContract.Books.DOWNLOADED) {
                                intent = LibraryController.getOpenBookIntent(this, book);
                                startActivity(intent);
                            } else {
                                if (NetworkUtils.hasInternetConnectivity(this)) {
                                    BookDownloadController.getInstance(this).startDownloadBook(book);
                                } else {
                                    BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(this);
                                    builder.setTitle(R.string.title_device_offline);
                                    builder.setMessage(R.string.dialog_device_is_offline);
                                    builder.setNeutralButton(R.string.button_close, null);
                                    builder.show();
                                }
                            }
                        }

                    } else {
                        intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                    }
                } catch (NumberFormatException e) {
                    //pass through as a normal
                }
            }
        }
    }

    /**
     * Show a new information panel to appear at the top of the library screen (overlaying the viewpager). If
     * we have multiple information panels they wil stack vertically
     * @param text the text to show in the information panel
     * @param backgroundColor the background color of the information panel
     * @param textClickListener an optional OnClickListener to be notified when the user presses the text (null for none)
     * @param dismissClickListener an optional OnClickListener to be notified when the user dismisses the panel (null for none)
     */
    private void showNewInfoPanel(String text, int backgroundColor, View.OnClickListener textClickListener, View.OnClickListener dismissClickListener) {
        InfoPanel infoPanel = new InfoPanel(this);
        infoPanel.setText(text, textClickListener);
        infoPanel.setBackgroundColor(backgroundColor);
        infoPanel.setDismissClickListener(dismissClickListener);
        mInfoPanelContainer.addView(infoPanel);
    }

    /**
     * Presents a friendly upgrade reminder in the form of an information panel with clickable text that sends the user to the Play Store.
     * @param url the url to load when the user presses the text
     * @param friendlyMessageTitle the text to display in the information panel
     */
    public void friendlyUpgradeReminder(final String url, final String friendlyMessageTitle){
        if(PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_KEY_SHOW_REMINDER, true)) {

            View.OnClickListener textClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String friendlyUpgrade = String.format("userId: %s", AccountController.getInstance().getUserId());
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_CALL_TO_ACTIONS, AnalyticsHelper.GA_EVENT_FRIENDLY_UPGRADE_ACCEPTED, friendlyUpgrade, Long.valueOf(BuildConfig.VERSION_NAME.replaceAll("[^0-9]", "")));
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            };
            View.OnClickListener dismissClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AppUpgradeHelper.getInstance().showFriendlyUpgrade(false);
                    String friendlyUpgrade = String.format("userId: %s", AccountController.getInstance().getUserId());
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_CALL_TO_ACTIONS, AnalyticsHelper.GA_EVENT_FRIENDLY_UPGRADE_DISMISSED, friendlyUpgrade, Long.valueOf(BuildConfig.VERSION_NAME.replaceAll("[^0-9]", "")));
                }
            };
            showNewInfoPanel(friendlyMessageTitle, getResources().getColor(R.color.hit_state_purple), textClickListener, dismissClickListener);
        }
    }

    /**
     * Show the help menu
     */
    private void showHelp() {
        Intent intent = new Intent(this, WebContentActivity.class);
        intent.putExtra(WebContentActivity.PARAM_URL, getString(R.string.support_url));
        intent.putExtra(WebContentActivity.PARAM_TITLE, getString(R.string.faq));
        intent.putExtra(WebContentActivity.PARAM_SCREEN_NAME, AnalyticsHelper.GA_SCREEN_Library_FAQScreen);
        startActivity(intent);
    }

    /**
     * Sign in button tapped
     *
     * @param view The View that was pressed
     */
    public void signInTapped(View view) {
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LibraryController.getInstance().setLibraryActivity(this);
        if (mViewPager.getCurrentItem() == TAB_READING) {
            AnalyticsHelper.getInstance().startTrackingUIComponent(mIsLoggedIn ?
                    AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_Reading : AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_Reading_Anonymous);
        } else if (mViewPager.getCurrentItem() == TAB_MY_LIBRARY) {
            AnalyticsHelper.getInstance().startTrackingUIComponent(mIsLoggedIn ?
                    AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_MyLibrary : AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_MyLibrary_Anonymous);
        }

        AppUpgradeHelper.getInstance().init(getString(R.string.upgrade_information_url), this);
    }

    @Override
    protected void onStop() {
        LibraryController.getInstance().setLibraryActivity(null);
        AnalyticsHelper.getInstance().stopTrackingUIComponent(mIsLoggedIn ?
                AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_Reading : AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_Reading_Anonymous);
        AnalyticsHelper.getInstance().stopTrackingUIComponent(mIsLoggedIn ?
                AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_MyLibrary : AnalyticsHelper.GA_SCREEN_Library_YourLibraryScreen_MyLibrary_Anonymous);
        super.onStop();
    }

    @Override
    public void setNumberOfItems(LibraryBooksFragment fragment, int numBooks) {

        // Check if the fragment exists within our view pager and update the title to reflect the value
        for (int i = 0; i < mLibraryPagerAdapter.getCount(); i++) {
            if (mLibraryPagerAdapter.getFragment(i) == fragment) {
                mSlidingTabLayout.setCustomTabText(i, mLibraryPagerAdapter.getPageTitle(i) + " (" + numBooks + ")");

                // Inform the drawer fragment so it may update its values accordingly
                if (i == TAB_READING) {
                    mDrawerFragment.setNumberOfItemsCurrentlyReading(numBooks);
                } else if (i == TAB_MY_LIBRARY) {
                    mDrawerFragment.setNumberOfItemsMyLibrary(numBooks);
                }
                break;
            }
        }
    }

    @Override
    public void onMenuItemSelected(MenuListItem listItem) {
        if (listItem.enabled) {
            if (listItem.actionUri != null) {
                BBBUriManager.getInstance().handleUri(this, listItem.actionUri);
            }
            mDrawerLayout.closeDrawers();
        }
    }


    // Adapter for supplying fragments to sit within our ViewPager
    private class LibraryPagerAdapter extends FragmentPagerAdapter {

        // Used to store a reference to the fragments within the adapter
        private SparseArray<Fragment> mPageReferenceArray = new SparseArray<Fragment>();

        private LibraryPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0: {
                    // The featured fragment will be visible at startup so pass true to indicate this
                    return LibraryBooksFragment.newInstance(BBBContract.Books.BOOK_STATE_READING, false, new LibrarySortOption[]{LibrarySortOption.RECENTLY_READ});
                }
                case 1: {
                    return LibraryBooksFragment.newInstance(LibraryBooksFragment.NO_FILTER, true, new LibrarySortOption[]{LibrarySortOption.PURCHASE_DATE, LibrarySortOption.RECENTLY_READ, LibrarySortOption.TITLE_ASCENDING, LibrarySortOption.AUTHOR_ASCENDING, LibrarySortOption.AUTHOR_DESCENDING});
                }
            }

            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LibraryBooksFragment fragment = (LibraryBooksFragment) super.instantiateItem(container, position);
            mPageReferenceArray.put(position, fragment);
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.reading);

                case 1:
                    return getString(R.string.my_library);

                default:
                    return "";
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);

            // Clear the fragment reference at the destroyed position
            mPageReferenceArray.remove(position);
        }

        /**
         * Get the fragment at the specified position
         *
         * @param position the position within the adapter to get the fragment from
         * @return the Fragment at the position (or null if none)
         */
        public Fragment getFragment(int position) {
            return mPageReferenceArray.get(position);
        }
    }
}