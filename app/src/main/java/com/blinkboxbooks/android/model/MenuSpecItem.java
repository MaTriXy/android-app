// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

/**
 * Hold the content from the main menu configuration xml file
 */
public class MenuSpecItem extends MenuListItem {

    /**
     * The @string resource for this menu item
     */
    public int titleResourceId;

    /**
     * True indicates this menu entry is only visible while signed in
     */
    public boolean isVisibleSignedIn;

    /**
     * True indicates this menu entry is only visible while signed out
     */
    public boolean isVisibleSignedOut;

    /**
     * True indicates this menu entry requires network connectivity
     */
    public boolean isDependentOnNetwork;

    /**
     * True indicates this entry should include the preferred name
     */
    public boolean isMyDevice;
}