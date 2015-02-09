// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.shop.preview.ComponentClickedListener;
import com.blinkboxbooks.android.ui.shop.preview.PreviewActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.cache.TypefaceCache;

public class ShopActivity extends BaseActivity implements ComponentClickedListener {

    private static final String TAG_SHOP_FRAGMENT = "shop_fragment";

    public static final int[] tabTitles = {R.string.title_featured, R.string.title_categories, R.string.title_bestsellers_fiction, R.string.title_bestsellers_nonfiction, R.string.title_freebooks, R.string.title_newreleases};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case we come in here via a marketing deep link we want to track in Ad-X
        AnalyticsHelper.handleAdXDeepLink(this, getIntent());

        setContentView(R.layout.activity_shop);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            ShopFragment shopFragment = new ShopFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container, shopFragment, TAG_SHOP_FRAGMENT).commit();
        }

        // Prevent the keyboard opening by default on the search view
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //this is a dodgy hack for orientation changes - we check to see if the preview activity is
        //the current base activity of the purchase controller, if it is not destoyed/finishing
        //we do not set the base activity.
        final PurchaseController purchaseController = PurchaseController.getInstance();
        final BaseActivity baseActivity = purchaseController.getBaseActivity();
        if (baseActivity != null && baseActivity instanceof PreviewActivity) {
            if (!baseActivity.isDestroyedOrFinishing()) {
                return;
            }
        }
        purchaseController.setBaseActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.shop_actionbar_options, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem menuItem = menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(getApplicationContext(), SearchActivity.class)));
        final boolean isPhone = !getResources().getBoolean(R.bool.isTablet);
        searchView.setIconified(isPhone);

        if (isPhone) {
            searchView.setOnSearchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSupportActionBar().setIcon(null);
                }
            });
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    getSupportActionBar().setIcon(R.drawable.actionbar_icon);
                    return false;
                }
            });
        }

        // Theme the SearchView's AutoCompleteTextView drop down.
        searchView.clearFocus();

        //Sets typeface for searchView
        EditText searchText = ((EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text));
        searchText.setTypeface(TypefaceCache.getInstance().getTypeface(this, R.string.font_lola_regular));

        return true;
    }

    /**
     * Shows the tab with the given title
     *
     * @param title
     */
    public void showTab(String title) {

        if(title != null) {
            ShopFragment shopFragment = (ShopFragment)getSupportFragmentManager().findFragmentByTag(TAG_SHOP_FRAGMENT);
            shopFragment.showPage(findTabIndex(title));
        }
    }

    private int findTabIndex(String tabName) {
        Resources resources = getResources();

        for(int i=0; i<tabTitles.length; i++) {

            if(tabName.equals(resources.getString(tabTitles[i]))) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void backFromAuthorPressed() {
    }

    @Override
    public void authorPressed() {
    }

    @Override
    public void relatedBookClicked(ShopItem shopItem) {
    }

    @Override
    public void promotedBookClicked(ShopItem shopItem) {
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra(PreviewActivity.ARG_SHOP_ITEM, shopItem);
        startActivity(intent);
    }
}