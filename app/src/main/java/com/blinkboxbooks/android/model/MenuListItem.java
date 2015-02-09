// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

/**
 * MenuItem POJO for items or headers on the main menu
 */
public class MenuListItem {
    /**
     * The tag identifier of the tag
     */
    public String tag;

    /**
     * The menu label entry.
     */
    public String title;

    /**
     * The menu label additional text
     */
    public String additional;

    /**
     * True if the entry is a header item in the list
     */
    public boolean header;

    /**
     * If false the item should be disabled
     */
    public boolean enabled;

    /**
     * If false we should not show the no network error
     */
    public boolean showNetworkError;

    /**
     * If true the item should be selected
     */
    public boolean selected;

    /**
     * The resource id for menu item
     */
    public int iconResourceId;

    /**
     * The action that will be executed if the menu item is tapped
     */
    public String actionUri;

    /**
     * If true, this option is only valid if the reader is displaying an epub file
     */
    public boolean epubOptionOnly;
}