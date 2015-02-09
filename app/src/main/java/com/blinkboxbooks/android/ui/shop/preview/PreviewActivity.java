// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.preview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.ui.BaseActivity;

/**
 * Activity for displaying shop preview
 */
public class PreviewActivity extends BaseActivity implements ComponentClickedListener {

    private static final String TAG_FRAGMENT = "fragment";

    public static final String ARG_SHOP_ITEM = "shop_item";
    public static final String ARG_START_ON_AUTHOR = "start_on_author";

    private boolean mStartOnAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_shop_preview);
        setTitle(R.string.book_description);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        ShopItem shopItem = (ShopItem)intent.getSerializableExtra(ARG_SHOP_ITEM);
        mStartOnAuthor = intent.getBooleanExtra(ARG_START_ON_AUTHOR, false);

        if(savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            if (shopItem != null) {
                PreviewFragment fragment = PreviewFragment.newInstance(shopItem);
                fragmentTransaction.add(R.id.container, fragment, TAG_FRAGMENT);
            }

            fragmentTransaction.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        PurchaseController.getInstance().setBaseActivity(this);

        // If the start on author field is set then we force a switch to the author tab
        if (mStartOnAuthor) {
            authorPressed();
            mStartOnAuthor = false;
        }
    }

    @Override
    public void backFromAuthorPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);

        if(fragment instanceof PreviewFragment) {
            ((PreviewFragment)fragment).setTab(PreviewFragment.TAB_SHOP_ITEM);
        }
    }

    @Override
    public void authorPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);

        if(fragment instanceof PreviewFragment) {
            ((PreviewFragment)fragment).setTab(PreviewFragment.TAB_AUTHOR);
        }
    }

    @Override
    public void promotedBookClicked(ShopItem shopItem) {
    }

    @Override
    public void relatedBookClicked(ShopItem shopItem) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);

        if(fragment instanceof PreviewFragment) {
            PreviewFragment previewFragment = ((PreviewFragment)fragment);
            previewFragment.setTab(PreviewFragment.TAB_SHOP_ITEM);
            previewFragment.setShopItem(shopItem);
        } else {
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra(PreviewActivity.ARG_SHOP_ITEM, shopItem);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
        if (item.getItemId() == android.R.id.home) {
            if(fragment instanceof PreviewFragment) {
                if (((PreviewFragment) fragment).getTab() == PreviewFragment.TAB_SHOP_ITEM) {
                    finish();
                } else {
                    backFromAuthorPressed();
                }
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
