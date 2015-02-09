// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.content.Context;

import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.MenuListItem;
import com.blinkboxbooks.android.model.MenuSpecItem;
import com.blinkboxbooks.android.ui.library.LibraryActivity;
import com.blinkboxbooks.android.util.EmailUtil;
import com.blinkboxbooks.android.util.MainMenuLoader;
import com.blinkboxbooks.android.util.PreferenceManager;

import java.util.LinkedList;
import java.util.List;

/**
 * The controller class for the sliding menu
 */
public class DrawerMenuController {

    private static DrawerMenuController sInstance;

    /**
     * Static singleton getInstance
     *
     * @param context
     * @return the {@link DrawerMenuController} singleton object
     */
    public static DrawerMenuController getInstance(Context context) {

        if (sInstance == null) {
            sInstance = new DrawerMenuController(context);
        }

        return sInstance;
    }

    private final Context mContext;
    private final List<MenuSpecItem> mMainMenuSpecItems;
    private boolean mInnerItemsEnabled;

    private String mCreditText = null;

    /**
     * This listener will be invoked when a menu item is selected
     */
    public interface OnMenuItemSelectedListener {
        public void onMenuItemSelected(MenuListItem listItem);
    }

    /**
     * Private constructor
     *
     * @param context
     */
    private DrawerMenuController(Context context) {
        this.mContext = context;
        this.mMainMenuSpecItems = MainMenuLoader.loadMainMenu(context, R.xml.main_menu);
    }

    /**
     * Set false if items on the list that require connectivity should be
     * disabled
     *
     * @param innerItemsEnabled
     */
    public void setInnerItemsEnabled(boolean innerItemsEnabled) {
        this.mInnerItemsEnabled = innerItemsEnabled;
    }

    /**
     * Get a current list of {@link MenuListItem} objects that should be
     * displayed on the main menu
     *
     * @return
     */
    public MenuListItem[] getMenuItems() {
        boolean signInStatus = AccountController.getInstance().isLoggedIn();

        String preferredName = PreferenceManager.getInstance().getString(PreferenceManager.PREF_KEY_PREFERRED_NAME, null);

        if (preferredName != null) {
            preferredName = mContext.getString(R.string.s_apostrophe, preferredName);
        } else {
            preferredName = mContext.getString(R.string.your);
        }

        List<MenuListItem> returnList = new LinkedList<MenuListItem>();

        for (MenuSpecItem item : mMainMenuSpecItems) {
            item.enabled = (mInnerItemsEnabled || !item.isDependentOnNetwork);
            item.showNetworkError = !mInnerItemsEnabled;

            if (item.titleResourceId == R.string.s_library) {
                item.title = mContext.getString(item.titleResourceId, preferredName);
                item.selected = true;
            } else if (item.titleResourceId == R.string.version) {
                item.title = mContext.getString(item.titleResourceId, EmailUtil.getVersionString(mContext));
            } else if (item.isMyDevice && signInStatus) {
                item.title = mContext.getString(item.titleResourceId);// + mContext.getString(R.string.colon_s_s_device, preferredName, Build.MODEL);
            } else {
                item.title = mContext.getString(item.titleResourceId);
            }

            boolean visible = true;

            if (signInStatus) {
                visible = item.isVisibleSignedIn;
            } else {
                visible = item.isVisibleSignedOut;
            }

            int selectedTab = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_KEY_LIBRARY_TAB_SELECTED, LibraryActivity.TAB_MY_LIBRARY);

            if (item.titleResourceId == R.string.currently_reading) {
                item.enabled = (selectedTab != LibraryActivity.TAB_READING);
            } else if (item.titleResourceId == R.string.my_library_lower_l) {
                item.enabled = (selectedTab != LibraryActivity.TAB_MY_LIBRARY);
            } else if (item.titleResourceId == R.string.refresh_your_library) {
                item.enabled = item.enabled && signInStatus && !AccountController.getInstance().isSyncActive();
            } else if (item.titleResourceId == R.string.version) {
                visible = BuildConfig.DEBUG;
            } else if(item.tag != null && item.tag.equals(mContext.getString(R.string.shop))) {
                item.additional = mCreditText;
            }

            if (visible) {
                returnList.add(item);
            }
        }

        return returnList.toArray(new MenuListItem[returnList.size()]);
    }

    /**
     * Sets the text to appear in the credit item in the drawer menu
     * @param creditText String to appear
     */
    public void setCreditText(String creditText) {
        mCreditText = creditText;
    }

}
