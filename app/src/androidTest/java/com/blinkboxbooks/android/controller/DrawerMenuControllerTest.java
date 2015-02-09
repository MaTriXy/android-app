// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.test.AndroidTestCase;

import com.blinkboxbooks.android.controller.DrawerMenuController;
import com.blinkboxbooks.android.model.MenuListItem;
import com.blinkboxbooks.android.util.PreferenceManager;

/**
 * This class tests the {@link com.blinkboxbooks.android.controller.DrawerMenuController} class
 */
public class DrawerMenuControllerTest extends AndroidTestCase {

	private DrawerMenuController mDrawerMenuController;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mDrawerMenuController = DrawerMenuController.getInstance(getContext());
	}

	/**
	 * Test case for when the user is logged out with internet connectivity
	 */
	public void testLoggedOutWithInternet() {
		mDrawerMenuController.setInnerItemsEnabled(true);
		
		PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_PREFERRED_NAME, "Derek");
		PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_CURRENT_USER, (String) null);
		
		MenuListItem[] mainMenuList = mDrawerMenuController.getMenuItems();
		
		assertEquals("Derek's library", mainMenuList[0].title);
		assertEquals(true, mainMenuList[0].header);
		assertEquals(false, mainMenuList[1].header);
	}

	/**
	 * Test case for when the user is logged out without internet connectivity
	 */
	public void testLoggedOutWithoutInternet() {
		mDrawerMenuController.setInnerItemsEnabled(false);
		PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_CURRENT_USER, (String) null);
		
		MenuListItem[] mainMenuList = mDrawerMenuController.getMenuItems();

		assertEquals("Derek's library", mainMenuList[0].title);
		assertEquals(true, mainMenuList[0].header);
		assertEquals(false, mainMenuList[1].header);
	}

	/**
	 * Test case for when the user is logged in with internet connectivity
	 */
	public void testLoggedInWithInternet() {
		mDrawerMenuController.setInnerItemsEnabled(true);
		PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_CURRENT_USER, "name");
		
		MenuListItem[] mainMenuList = mDrawerMenuController.getMenuItems();
		
		int signout = 4;
		assertEquals("Sign out", mainMenuList[signout].title);
		assertEquals(false, mainMenuList[signout].header);
		assertEquals(true, mainMenuList[signout].enabled);
	}

	/**
	 * Test case for when the user is logged in without internet connectivity
	 */
	public void testLoggedInWithoutInternet() {
		mDrawerMenuController.setInnerItemsEnabled(false);
		PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_CURRENT_USER, "name");
		
		MenuListItem[] mainMenuList = mDrawerMenuController.getMenuItems();
		
		int signout = 4;
		assertEquals("Sign out", mainMenuList[signout].title);
		assertEquals(false, mainMenuList[signout].header);
		assertEquals(true, mainMenuList[signout].enabled);
	}
}